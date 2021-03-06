package io.terminus.doctor.event.event;

import com.google.common.collect.Lists;
import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.Subscribe;
import io.terminus.common.utils.Dates;
import io.terminus.doctor.common.enums.PigType;
import io.terminus.doctor.common.event.EventListener;
import io.terminus.doctor.common.utils.DateUtil;
import io.terminus.doctor.event.cache.DoctorDailyReportCache;
import io.terminus.doctor.event.dao.DoctorKpiDao;
import io.terminus.doctor.event.dto.report.daily.DoctorDailyReportDto;
import io.terminus.doctor.event.enums.GroupEventType;
import io.terminus.doctor.event.manager.DoctorCommonReportManager;
import io.terminus.doctor.event.service.DoctorCommonReportWriteService;
import io.terminus.doctor.event.service.DoctorPigTypeStatisticWriteService;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

import static io.terminus.doctor.event.event.DoctorGroupPublishDto.filterBy;

/**
 * Desc:
 * Mail: yangzl@terminus.io
 * author: DreamYoung
 * Date: 2016/11/30
 */
@Slf4j
@Component
@SuppressWarnings("unused")
public class DoctorGroupEventListener implements EventListener {

//    @Autowired
//    private DoctorKpiDao doctorKpiDao;
//
//    @Autowired
//    private DoctorPigTypeStatisticWriteService doctorPigTypeStatisticWriteService;
//
//    @Autowired
//    private DoctorDailyReportCache doctorDailyReportCache;
//
//    @Autowired
//    private DoctorCommonReportWriteService doctorCommonReportWriteService;
//
//    @Autowired
//    private DoctorCommonReportManager doctorCommonReportManager;
//
//    private static final List<Integer> NEED_TYPES = Lists.newArrayList(
//            GroupEventType.MOVE_IN.getValue(),
//            GroupEventType.CHANGE.getValue(),
//            GroupEventType.TRANS_GROUP.getValue(),
//            GroupEventType.TURN_SEED.getValue(),
//            GroupEventType.TRANS_FARM.getValue()
//    );
//
//    @AllowConcurrentEvents
//    @Subscribe
//    public void handleGroupEvent(ListenedGroupEvent groupEvent) {
//        log.info("handle group event, event info:{}", groupEvent);
//
//        //????????????????????????????????????
//        if (!NEED_TYPES.contains(groupEvent.getEventType())) {
//            log.info("this eventType({}) no need to handle", groupEvent.getEventType());
//            return;
//        }
//
//        Function<DoctorGroupPublishDto, Date> eventAtFunc = e -> Dates.startOfDay(e.getEventAt());
//        Function<DoctorGroupPublishDto, Date> monthFunc = e -> DateUtil.monthStart(e.getEventAt());
//
//        //????????????
//        filterBy(groupEvent.getGroups(), DoctorGroupPublishDto::getPigType, eventAtFunc)
//                .forEach(event -> handleDaily(groupEvent.getOrgId(), groupEvent.getFarmId(), groupEvent.getEventType(), event));
//
//        //????????????
//        filterBy(groupEvent.getGroups(), monthFunc)
//                .forEach(event -> handlyCommon(groupEvent.getFarmId(), groupEvent.getEventType(), event));
//        log.info("handleGroupEvent ok???");
//    }
//
//    //????????????
//    private void handlyCommon(Long farmId, Integer eventType, DoctorGroupPublishDto publishDto) {
//        GroupEventType type = GroupEventType.from(eventType);
//        if (type == null) {
//            log.error("handle group event type not find, farmId:{}, eventType:{}, pigType:{}, eventAt:{}",
//                    farmId, eventType, publishDto.getPigType(), publishDto.getEventAt());
//            return;
//        }
//
//        //????????????????????????
//        doctorCommonReportManager.updateLiveStockChange(new DoctorCommonReportManager.FarmIdAndEventAt(farmId, publishDto.getEventAt()));
//
//        //???????????????????????????????????????
//        if (Objects.equals(type, GroupEventType.CHANGE)) {
//            doctorCommonReportManager.updateSaleDead(new DoctorCommonReportManager.FarmIdAndEventAt(farmId, publishDto.getEventAt()));
//        }
//    }
//
//    //????????????
//    private void handleDaily(Long orgId, Long farmId, Integer eventType, DoctorGroupPublishDto publishDto) {
//        GroupEventType type = GroupEventType.from(eventType);
//        if (type == null) {
//            log.error("handle group event type not find, farmId:{}, eventType:{}, pigType:{}, eventAt:{}",
//                    farmId, eventType, publishDto.getPigType(), publishDto.getEventAt());
//            return;
//        }
//
//        handleGroupLiveStock(orgId, farmId, publishDto.getPigType(), publishDto.getEventAt());
//
//        //???????????????????????????????????????
//        if (Objects.equals(type, GroupEventType.CHANGE)) {
//            handleChange(orgId, publishDto.getPigType(), publishDto.getEventAt());
//        }
//    }
//
//    //??????????????????(?????????????????????????????????????????????????????????)
//    private void handleChange(Long farmId, Integer pigType, Date eventAt) {
//        Date startAt = Dates.startOfDay(eventAt);
//        Date endAt = DateUtil.getDateEnd(new DateTime(eventAt)).toDate();
//
//        getDead(PigType.from(pigType), farmId, startAt, endAt);
//        getSale(PigType.from(pigType), farmId, startAt, endAt);
//    }
//
//    //???????????????????????????
//    private void handleGroupLiveStock(Long orgId, Long farmId, Integer pigType, Date eventAt) {
//        Date startAt = Dates.startOfDay(eventAt);
//        Date endAt = Dates.startOfDay(new Date());
//
//        //??????????????????????????????
//        doctorPigTypeStatisticWriteService.statisticGroup(orgId, farmId);
//
//        PigType type = PigType.from(pigType);
//        if (type == null) {
//            log.error("group event pigType({}) not support!", pigType);
//            return;
//        }
//
//        //????????????????????????
//        while (!startAt.after(endAt)) {
//            //??????startAt ???????????????????????????????????????????????????????????????????????????
//            if (!doctorDailyReportCache.reportIsFullInit(farmId, startAt)) {
//                getLiveStock(type, farmId, startAt);
//            }
//            startAt = new DateTime(startAt).plusDays(1).toDate();
//        }
//    }
//
//    //???????????????????????????????????????
//    private void getLiveStock(PigType pigType, Long farmId, Date startAt) {
////        switch (pigType) {
////            case NURSERY_PIGLET:
////                int nursery = doctorKpiDao.realTimeLiveStockNursery(farmId, startAt);
////                DoctorDailyReportDto reportDtoNursery = doctorDailyReportCache.getDailyReportDto(farmId, startAt);
////                reportDtoNursery.getLiveStock().setNursery(nursery);
////                doctorDailyReportCache.putDailyReportToMySQL(farmId, startAt, reportDtoNursery);
////                break;
////            case FATTEN_PIG:
////                int fatten = doctorKpiDao.realTimeLiveStockFatten(farmId, startAt);
////                DoctorDailyReportDto reportDtoFatten = doctorDailyReportCache.getDailyReportDto(farmId, startAt);
////                reportDtoFatten.getLiveStock().setFatten(fatten);
////                doctorDailyReportCache.putDailyReportToMySQL(farmId, startAt, reportDtoFatten);
////                break;
////            case RESERVE:
////                int houbeiSow = doctorKpiDao.realTimeLiveStockHoubeiSow(farmId, startAt);
////                int houbeiBoar = doctorKpiDao.realTimeLiveStockHoubeiBoar(farmId, startAt);
////                DoctorDailyReportDto reportDtoHoubei = doctorDailyReportCache.getDailyReportDto(farmId, startAt);
////                reportDtoHoubei.getLiveStock().setHoubeiSow(houbeiSow);
////                reportDtoHoubei.getLiveStock().setHoubeiBoar(houbeiBoar);
////                doctorDailyReportCache.putDailyReportToMySQL(farmId, startAt, reportDtoHoubei);
////                break;
////            case DELIVER_SOW:
////                int farrow = doctorKpiDao.realTimeLiveStockFarrow(farmId, startAt);
////                DoctorDailyReportDto reportDtoFarrow = doctorDailyReportCache.getDailyReportDto(farmId, startAt);
////                reportDtoFarrow.getLiveStock().setFarrow(farrow);
////                doctorDailyReportCache.putDailyReportToMySQL(farmId, startAt, reportDtoFarrow);
////                break;
////            default:
////                break;
////        }
//    }
//
//    //????????????
//    private void getDead(PigType pigType, Long farmId, Date startAt, Date endAt) {
////        switch (pigType) {
////            case NURSERY_PIGLET:
////                int nursery = doctorKpiDao.getDeadNursery(farmId, startAt, endAt);
////                int weedOutNursery = doctorKpiDao.getWeedOutNursery(farmId, startAt, endAt);
////                DoctorDailyReportDto reportDtoNursery = doctorDailyReportCache.getDailyReportDto(farmId, startAt);
////                reportDtoNursery.getDead().setNursery(nursery);
////                reportDtoNursery.getDead().setWeedOutNursery(weedOutNursery);
////                doctorDailyReportCache.putDailyReportToMySQL(farmId, startAt, reportDtoNursery);
////                break;
////            case FATTEN_PIG:
////                int fatten = doctorKpiDao.getDeadFatten(farmId, startAt, endAt);
////                int weedOutFatten = doctorKpiDao.getWeedOutFatten(farmId, startAt, endAt);
////                DoctorDailyReportDto reportDtoFatten = doctorDailyReportCache.getDailyReportDto(farmId, startAt);
////                reportDtoFatten.getDead().setFatten(fatten);
////                reportDtoFatten.getDead().setWeedOutFatten(weedOutFatten);
////                doctorDailyReportCache.putDailyReportToMySQL(farmId, startAt, reportDtoFatten);
////                break;
////            case RESERVE:
////                int houbei = doctorKpiDao.getDeadHoubei(farmId, startAt, endAt);
////                int weedOutHoubei = doctorKpiDao.getWeedOutHoubei(farmId, startAt, endAt);
////                DoctorDailyReportDto reportDtoHoubei = doctorDailyReportCache.getDailyReportDto(farmId, startAt);
////                reportDtoHoubei.getDead().setHoubei(houbei);
////                reportDtoHoubei.getDead().setWeedOutHoubei(weedOutHoubei);
////                doctorDailyReportCache.putDailyReportToMySQL(farmId, startAt, reportDtoHoubei);
////                break;
////            case DELIVER_SOW:
////                int farrow = doctorKpiDao.getDeadFarrow(farmId, startAt, endAt);
////                int weedOutFarrow = doctorKpiDao.getWeedOutFarrow(farmId, startAt, endAt);
////                DoctorDailyReportDto reportDtoFarrow = doctorDailyReportCache.getDailyReportDto(farmId, startAt);
////                reportDtoFarrow.getDead().setFarrow(farrow);
////                reportDtoFarrow.getDead().setWeedOutFarrow(weedOutFarrow);
////                doctorDailyReportCache.putDailyReportToMySQL(farmId, startAt, reportDtoFarrow);
////                break;
////            default:
////                break;
////        }
//    }
//
//    //????????????
//    private void getSale(PigType pigType, Long farmId, Date startAt, Date endAt) {
//        log.info("handle group sale type:{}, startAt:{}, endAt:{}", pigType, startAt, endAt);
//        if (Objects.equals(pigType, PigType.FATTEN_PIG)) {
//            int fatten = doctorKpiDao.getSaleFatten(farmId, startAt, endAt);
//            long fattenPrice = doctorKpiDao.getGroupSaleFattenPrice(farmId, startAt, endAt);
//            DoctorDailyReportDto reportDtoFatten = doctorDailyReportCache.getDailyReportDto(farmId, startAt);
////            reportDtoFatten.getSale().setFatten(fatten);
////            reportDtoFatten.getSale().setFattenPrice(fattenPrice);
//            doctorDailyReportCache.putDailyReportToMySQL(farmId, startAt, reportDtoFatten);
//            return;
//        }
//        if (Objects.equals(pigType, PigType.RESERVE)) {
//            int houbei = doctorKpiDao.getSaleHoubei(farmId, startAt, endAt);
//            DoctorDailyReportDto reportDtoHoubei = doctorDailyReportCache.getDailyReportDto(farmId, startAt);
////            reportDtoHoubei.getSale().setHoubei(houbei);
//            doctorDailyReportCache.putDailyReportToMySQL(farmId, startAt, reportDtoHoubei);
//            return;
//        }
//        int nursery = doctorKpiDao.getSaleNursery(farmId, startAt, endAt);
//        long base10 = doctorKpiDao.getGroupSaleBasePrice10(farmId, startAt, endAt);
//        long base15 = doctorKpiDao.getGroupSaleBasePrice15(farmId, startAt, endAt);
//
//        DoctorDailyReportDto reportDtoNursery = doctorDailyReportCache.getDailyReportDto(farmId, startAt);
////        reportDtoNursery.getSale().setNursery(nursery);
////        reportDtoNursery.getSale().setBasePrice10(base10);
////        reportDtoNursery.getSale().setBasePrice15(base15);
//        doctorDailyReportCache.putDailyReportToMySQL(farmId, startAt, reportDtoNursery);
//    }
}
