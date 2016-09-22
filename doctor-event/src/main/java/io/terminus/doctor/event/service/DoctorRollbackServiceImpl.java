package io.terminus.doctor.event.service;

import com.google.common.base.Throwables;
import io.terminus.boot.rpc.common.annotation.RpcProvider;
import io.terminus.common.exception.ServiceException;
import io.terminus.common.model.Response;
import io.terminus.common.utils.Dates;
import io.terminus.doctor.common.utils.DateUtil;
import io.terminus.doctor.event.dao.DoctorGroupBatchSummaryDao;
import io.terminus.doctor.event.dao.DoctorGroupEventDao;
import io.terminus.doctor.event.dao.DoctorKpiDao;
import io.terminus.doctor.event.dao.DoctorPigEventDao;
import io.terminus.doctor.event.dao.redis.DailyReport2UpdateDao;
import io.terminus.doctor.event.dao.redis.DailyReportHistoryDao;
import io.terminus.doctor.event.dto.DoctorRollbackDto;
import io.terminus.doctor.event.dto.report.daily.DoctorDailyReportDto;
import io.terminus.doctor.event.dto.report.daily.DoctorDeadDailyReport;
import io.terminus.doctor.event.dto.report.daily.DoctorDeliverDailyReport;
import io.terminus.doctor.event.dto.report.daily.DoctorLiveStockDailyReport;
import io.terminus.doctor.event.dto.report.daily.DoctorMatingDailyReport;
import io.terminus.doctor.event.dto.report.daily.DoctorSaleDailyReport;
import io.terminus.doctor.event.dto.report.daily.DoctorWeanDailyReport;
import io.terminus.doctor.event.enums.RollbackType;
import io.terminus.doctor.event.handler.rollback.DoctorRollbackHandlerChain;
import io.terminus.doctor.event.model.DoctorGroupEvent;
import io.terminus.doctor.event.model.DoctorPigEvent;
import io.terminus.doctor.event.search.barn.BarnSearchWriteService;
import io.terminus.doctor.event.search.group.GroupSearchWriteService;
import io.terminus.doctor.event.search.pig.PigSearchWriteService;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;

import static io.terminus.common.utils.Arguments.notEmpty;

/**
 * Desc:
 * Mail: yangzl@terminus.io
 * author: DreamYoung
 * Date: 16/9/20
 */
@Slf4j
@Service
@RpcProvider
public class DoctorRollbackServiceImpl implements DoctorRollbackService {

    private final DoctorRollbackHandlerChain doctorRollbackHandlerChain;
    private final DoctorGroupEventDao doctorGroupEventDao;
    private final DoctorPigEventDao doctorPigEventDao;
    private final PigSearchWriteService pigSearchWriteService;
    private final GroupSearchWriteService groupSearchWriteService;
    private final BarnSearchWriteService barnSearchWriteService;
    private final DoctorGroupBatchSummaryDao doctorGroupBatchSummaryDao;
    private final DailyReport2UpdateDao dailyReport2UpdateDao;
    private final DailyReportHistoryDao dailyReportHistoryDao;
    private final DoctorKpiDao doctorKpiDao;

    @Autowired
    public DoctorRollbackServiceImpl(DoctorRollbackHandlerChain doctorRollbackHandlerChain,
                                     DoctorGroupEventDao doctorGroupEventDao,
                                     DoctorPigEventDao doctorPigEventDao,
                                     PigSearchWriteService pigSearchWriteService,
                                     GroupSearchWriteService groupSearchWriteService,
                                     BarnSearchWriteService barnSearchWriteService,
                                     DoctorGroupBatchSummaryDao doctorGroupBatchSummaryDao,
                                     DailyReport2UpdateDao dailyReport2UpdateDao,
                                     DailyReportHistoryDao dailyReportHistoryDao,
                                     DoctorKpiDao doctorKpiDao) {
        this.doctorRollbackHandlerChain = doctorRollbackHandlerChain;
        this.doctorGroupEventDao = doctorGroupEventDao;
        this.doctorPigEventDao = doctorPigEventDao;
        this.pigSearchWriteService = pigSearchWriteService;
        this.groupSearchWriteService = groupSearchWriteService;
        this.barnSearchWriteService = barnSearchWriteService;
        this.doctorGroupBatchSummaryDao = doctorGroupBatchSummaryDao;
        this.dailyReport2UpdateDao = dailyReport2UpdateDao;
        this.dailyReportHistoryDao = dailyReportHistoryDao;
        this.doctorKpiDao = doctorKpiDao;
    }

    @Override
    public Response<Boolean> rollbackGroupEvent(Long eventId, Long operatorId, String operatorName) {
        try {
            DoctorGroupEvent groupEvent = doctorGroupEventDao.findById(eventId);
            if (groupEvent != null) {
                //获取拦截器链, 判断能否回滚,执行回滚操作, 更新报表
                doctorRollbackHandlerChain.getRollbackGroupEventHandlers().stream()
                        .filter(handler -> handler.canRollback(groupEvent))
                        .forEach(handler -> {
                            handler.rollback(groupEvent, operatorId, operatorName);
                            handler.updateReport(groupEvent);
                        });
            }
            return Response.ok(Boolean.TRUE);
        } catch (ServiceException e) {
            return Response.fail(e.getMessage());
        } catch (Exception e) {
            log.error("rollack group event failed, eventId:{}, cause:{}", eventId, Throwables.getStackTraceAsString(e));
            return Response.fail("rollback.event.failed");
        }
    }

    @Override
    public Response<Boolean> rollbackPigEvent(Long eventId, Long operatorId, String operatorName) {
        try {
            DoctorPigEvent pigEvent = doctorPigEventDao.findById(eventId);
            if (pigEvent != null) {
                //获取拦截器链, 判断能否回滚, 执行回滚操作, 如果成功, 更新报表
                doctorRollbackHandlerChain.getRollbackPigEventHandlers().stream()
                        .filter(handler -> handler.canRollback(pigEvent))
                        .forEach(handler -> {
                            handler.rollback(pigEvent, operatorId, operatorName);
                            handler.updateReport(pigEvent);
                        });
            }
            return Response.ok(Boolean.TRUE);
        } catch (ServiceException e) {
            return Response.fail(e.getMessage());
        } catch (Exception e) {
            log.error("rollack pig event failed, eventId:{}, cause:{}", eventId, Throwables.getStackTraceAsString(e));
            return Response.fail("rollback.event.failed");
        }
    }

    @Override
    public Response<Boolean> rollbackReportAndES(DoctorRollbackDto rollbackDto) {
        try {
            if (rollbackDto == null || !notEmpty(rollbackDto.getRollbackTypes()) ||
                    rollbackDto.getFarmId() == null || rollbackDto.getEventAt() == null) {
                return Response.ok(Boolean.TRUE);
            }
            reportAndES(rollbackDto);
            return Response.ok(Boolean.TRUE);
        } catch (Exception e) {
            log.error("rollback report and es failed, rollbackDto:{}, cause:{}", rollbackDto, Throwables.getStackTraceAsString(e));
            return Response.fail("rollback.report.fail");
        }
    }

    //遍历回滚类型，执行相应操作
    private synchronized void reportAndES(DoctorRollbackDto dto) {
        Date startAt = Dates.startOfDay(dto.getEventAt());
        Date endAt = DateUtil.getDateEnd(new DateTime(dto.getEventAt())).toDate();
        Long farmId = dto.getFarmId();

        DoctorDailyReportDto report = dailyReportHistoryDao.getDailyReportWithRedis(farmId, startAt);

        for (RollbackType type : dto.getRollbackTypes()) {
            switch (type) {
                //搜索实时更新
                case SEARCH_BARN:
                    if (dto.getEsBarnId() != null) {
                        barnSearchWriteService.update(dto.getEsBarnId());
                    }
                    break;
                case SEARCH_GROUP:
                    if (dto.getEsGroupId() != null) {
                        groupSearchWriteService.update(dto.getEsGroupId());
                    }
                    break;
                case SEARCH_PIG:
                    if (dto.getEsPigId() != null) {
                        pigSearchWriteService.update(dto.getEsPigId());
                    }
                    break;

                //日报实时更新
                case DAILY_DEAD:
                    report.setDead(getDeadDailyReport(farmId, startAt, endAt));
                    break;
                case DAILY_FARROW:
                    report.setDeliver(getDeliverDailyReport(farmId, startAt, endAt));
                    break;
                case DAILY_MATE:
                    report.setMating(getMatingDailyReport(farmId, startAt, endAt));
                    break;
                case DAILY_SALE:
                    report.setSale(getSaleDailyReport(farmId, startAt, endAt));
                    break;
                case DAILY_WEAN:
                    report.setWean(getWeanDailyReport(farmId, startAt, endAt));
                    break;

                //直接删除
                case GROUP_BATCH:
                    if (dto.getEsGroupId() != null) {
                        doctorGroupBatchSummaryDao.deleteByGroupId(dto.getEsGroupId());
                    }
                    break;
                default:
                    break;
            }
        }
        dailyReportHistoryDao.saveDailyReport(report, farmId, startAt);
        dailyReport2UpdateDao.saveDailyReport2Update(startAt, farmId);  //记录事件，// TODO: 16/9/21 晚上job更新月报

        updateLiveStockUntilNow(dto);   //更新从回滚日期到今天的存栏
    }

    //更新从回滚日期到今天的存栏
    private void updateLiveStockUntilNow(DoctorRollbackDto dto) {
        if (!dto.getRollbackTypes().contains(RollbackType.DAILY_LIVESTOCK)) {
            return;
        }
        Date startAt = Dates.startOfDay(dto.getEventAt());
        Date endAt = Dates.startOfDay(new Date());
        Long farmId = dto.getFarmId();

        while (!startAt.after(endAt)) {
            //猪群存栏
            DoctorLiveStockDailyReport liveStock = new DoctorLiveStockDailyReport();
            liveStock.setHoubeiBoar(doctorKpiDao.realTimeLiveStockHoubeiBoar(farmId, startAt));
            liveStock.setHoubeiSow(doctorKpiDao.realTimeLiveStockHoubeiSow(farmId, startAt));  //后备母猪
            liveStock.setFarrow(doctorKpiDao.realTimeLiveStockFarrow(farmId, startAt));
            liveStock.setNursery(doctorKpiDao.realTimeLiveStockNursery(farmId, startAt));
            liveStock.setFatten(doctorKpiDao.realTimeLiveStockFatten(farmId, startAt));

            //猪存栏
            liveStock.setBuruSow(doctorKpiDao.realTimeLiveStockFarrowSow(farmId, startAt));    //产房母猪
            liveStock.setPeihuaiSow(doctorKpiDao.realTimeLiveStockSow(farmId, startAt) - liveStock.getBuruSow());    //配怀 = 总存栏 - 产房母猪
            liveStock.setKonghuaiSow(0);                                                       //空怀猪作废, 置成0
            liveStock.setBoar(doctorKpiDao.realTimeLiveStockBoar(farmId, startAt));            //公猪

            DoctorDailyReportDto everyRedis = dailyReportHistoryDao.getDailyReportWithRedis(farmId, startAt);
            everyRedis.setLiveStock(liveStock);
            dailyReportHistoryDao.saveDailyReport(everyRedis, farmId, startAt);
            startAt = new DateTime(startAt).plusDays(1).toDate();
        }
    }

    private DoctorDeadDailyReport getDeadDailyReport(Long farmId, Date startAt, Date endAt) {
        DoctorDeadDailyReport dead = new DoctorDeadDailyReport();
        dead.setBoar(doctorKpiDao.getDeadBoar(farmId, startAt, endAt));
        dead.setSow(doctorKpiDao.getDeadSow(farmId, startAt, endAt));
        dead.setFarrow(doctorKpiDao.getDeadFarrow(farmId, startAt, endAt));
        dead.setNursery(doctorKpiDao.getDeadNursery(farmId, startAt, endAt));
        dead.setFatten(doctorKpiDao.getDeadFatten(farmId, startAt, endAt));
        return dead;
    }

    private DoctorDeliverDailyReport getDeliverDailyReport(Long farmId, Date startAt, Date endAt) {
        DoctorDeliverDailyReport deliver = new DoctorDeliverDailyReport();
        deliver.setNest(doctorKpiDao.getDelivery(farmId, startAt, endAt));
        deliver.setLive(doctorKpiDao.getDeliveryLive(farmId, startAt, endAt));
        deliver.setHealth(doctorKpiDao.getDeliveryHealth(farmId, startAt, endAt));
        deliver.setWeak(doctorKpiDao.getDeliveryWeak(farmId, startAt, endAt));
        deliver.setBlack(doctorKpiDao.getDeliveryDeadBlackMuJi(farmId, startAt, endAt));
        deliver.setAvgWeight(doctorKpiDao.getFarrowWeightAvg(farmId, startAt, endAt));
        return deliver;
    }

    private DoctorMatingDailyReport getMatingDailyReport(Long farmId, Date startAt, Date endAt) {
        DoctorMatingDailyReport mating = new DoctorMatingDailyReport();
        mating.setHoubei(doctorKpiDao.firstMatingCounts(farmId, startAt, endAt));
        mating.setPregCheckResultYing(doctorKpiDao.yinMatingCounts(farmId, startAt, endAt));
        mating.setDuannai(doctorKpiDao.weanMatingCounts(farmId, startAt, endAt));
        mating.setFanqing(doctorKpiDao.fanQMatingCounts(farmId, startAt, endAt));
        mating.setLiuchan(doctorKpiDao.abortionMatingCounts(farmId, startAt, endAt));
        return mating;
    }

    private DoctorSaleDailyReport getSaleDailyReport(Long farmId, Date startAt, Date endAt) {
        DoctorSaleDailyReport sale = new DoctorSaleDailyReport();
        sale.setBoar(doctorKpiDao.getSaleBoar(farmId, startAt, endAt));
        sale.setSow(doctorKpiDao.getSaleSow(farmId, startAt, endAt));
        sale.setNursery(doctorKpiDao.getSaleNursery(farmId, startAt, endAt));
        sale.setFatten(doctorKpiDao.getSaleFatten(farmId, startAt, endAt));
        sale.setFattenPrice(doctorKpiDao.getGroupSaleFattenPrice(farmId, startAt, endAt));
        sale.setBasePrice10(doctorKpiDao.getGroupSaleBasePrice10(farmId, startAt, endAt));
        sale.setBasePrice15(doctorKpiDao.getGroupSaleBasePrice15(farmId, startAt, endAt));
        return sale;
    }

    private DoctorWeanDailyReport getWeanDailyReport(Long farmId, Date startAt, Date endAt) {
        DoctorWeanDailyReport wean = new DoctorWeanDailyReport();
        wean.setCount(doctorKpiDao.getWeanPiglet(farmId, startAt, endAt));
        wean.setWeight(doctorKpiDao.getWeanPigletWeightAvg(farmId, startAt, endAt));
        wean.setNest(doctorKpiDao.getWeanSow(farmId, startAt, endAt));
        wean.setAvgDayAge(doctorKpiDao.getWeanDayAgeAvg(farmId, startAt, endAt));
        return wean;
    }
}
