package io.terminus.doctor.event.service;

import com.google.common.base.Stopwatch;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import io.terminus.boot.rpc.common.annotation.RpcProvider;
import io.terminus.common.model.Response;
import io.terminus.common.utils.BeanMapper;
import io.terminus.doctor.common.enums.PigType;
import io.terminus.doctor.common.utils.DateUtil;
import io.terminus.doctor.event.dao.DoctorGroupEventDao;
import io.terminus.doctor.event.dao.DoctorPigEventDao;
import io.terminus.doctor.event.dto.DoctorDimensionCriteria;
import io.terminus.doctor.event.dto.DoctorFarmEarlyEventAtDto;
import io.terminus.doctor.event.dto.DoctorStatisticCriteria;
import io.terminus.doctor.event.dto.report.daily.DoctorFarmLiveStockDto;
import io.terminus.doctor.event.dto.reportBi.DoctorDimensionReport;
import io.terminus.doctor.event.enums.DateDimension;
import io.terminus.doctor.event.enums.OrzDimension;
import io.terminus.doctor.event.manager.DoctorDailyReportV2Manager;
import io.terminus.doctor.event.reportBi.DoctorReportBiDataSynchronize;
import io.terminus.doctor.event.reportBi.DoctorReportBiManager;
import io.terminus.doctor.event.reportBi.synchronizer.DoctorEfficiencySynchronizer;
import io.terminus.doctor.event.reportBi.synchronizer.DoctorWarehouseSynchronizer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static io.terminus.common.utils.Arguments.notNull;


/**
 * Created by xjn on 17/12/12.
 * email:xiaojiannan@terminus.io
 */
@Slf4j
@Service
@RpcProvider
public class DoctorDailyReportV2ServiceImpl implements DoctorDailyReportV2Service {

    private final DoctorDailyReportV2Manager doctorDailyReportV2Manager;
    private final DoctorReportBiManager doctorReportBiManager;
    private final DoctorPigEventDao doctorPigEventDao;
    private final DoctorGroupEventDao doctorGroupEventDao;

    @Autowired
    private DoctorWarehouseSynchronizer warehouseSynchronizer;
    @Autowired
    private DoctorEfficiencySynchronizer efficiencySynchronizer;

    private final DoctorReportBiDataSynchronize doctorReportBiDataSynchronize;

    @Autowired
    public DoctorDailyReportV2ServiceImpl(DoctorDailyReportV2Manager doctorDailyReportV2Manager, DoctorReportBiManager doctorReportBiManager, DoctorPigEventDao doctorPigEventDao, DoctorGroupEventDao doctorGroupEventDao, DoctorReportBiDataSynchronize doctorReportBiDataSynchronize) {
        this.doctorDailyReportV2Manager = doctorDailyReportV2Manager;
        this.doctorReportBiManager = doctorReportBiManager;
        this.doctorPigEventDao = doctorPigEventDao;
        this.doctorGroupEventDao = doctorGroupEventDao;
        this.doctorReportBiDataSynchronize = doctorReportBiDataSynchronize;
    }

    @Override
    public Response<Boolean> flushFarmDaily(Long farmId, String startAt, String endAt) {
        log.info("flush farm daily starting, farmId:{}, startAt:{}, endAt:{}", farmId, startAt, endAt);
        try {
            Stopwatch stopwatch = Stopwatch.createStarted();
            DoctorStatisticCriteria criteria = new DoctorStatisticCriteria();
            criteria.setFarmId(farmId);
            List<Date> list = DateUtil.getDates(DateUtil.toDate(startAt), DateUtil.toDate(endAt));
            if (list.isEmpty()) {
                log.error("flush farm daily startAt or endAt is illegal, startAt:{}, endAt:{}", startAt, endAt);
                return Response.fail("startAt.or.endAt.is.error");
            }

            list.parallelStream().forEach(date -> {
                DoctorStatisticCriteria criteria1 = new DoctorStatisticCriteria();
                BeanMapper.copy(criteria, criteria1);
                criteria1.setSumAt(DateUtil.toDateString(date));
                log.info("flush farm daily farmId:{}, sumAt:{}", criteria1.getFarmId(), criteria1.getSumAt());
                doctorDailyReportV2Manager.flushFarmDaily(criteria1);
            });
            log.info("flush farm daily end, consume:{}m", stopwatch.elapsed(TimeUnit.MINUTES));
            return Response.ok(Boolean.TRUE);
        } catch (Exception e) {
            log.error("flush farm daily failed, farmId:{}, startAt:{}, endAt:{}, cause:{}",
                    farmId, startAt, endAt, Throwables.getStackTraceAsString(e));
            return Response.fail("flush.farm.daily.failed");
        }
    }

    @Override
    public Response<Boolean> flushGroupDaily(Long farmId, String startAt, String endAt) {
        log.info("flush group daily starting, farmId:{}, startAt:{}, endAt:{}", farmId, startAt, endAt);
        try {
            PigType.GROUP_TYPES.forEach(pigType -> flushGroupDaily(farmId, pigType, startAt, endAt));
            log.info("flush group daily end");
            return Response.ok(Boolean.TRUE);
        } catch (Exception e) {
            log.error("flush group daily failed, farmId:{}, startAt:{}, endAt:{}, cause:{}",
                    farmId, startAt, endAt, Throwables.getStackTraceAsString(e));
            return Response.fail("flush.group.daily.failed");
        }
    }

    @Override
    public Response<Boolean> flushGroupDaily(Long farmId, Integer pigType, String startAt, String endAt) {
        log.info("flush group daily for pigType starting, farmId:{}, pigType:{}, startAt:{}, endAt:{}", farmId, pigType, startAt, endAt);
        Stopwatch stopwatch = Stopwatch.createStarted();
        try {
            if (!PigType.GROUP_TYPES.contains(pigType)) {
                log.error("flush group daily pig type is illegal,pigType:{}", pigType);
                return Response.fail("pig.type.is.illegal");
            }
            DoctorStatisticCriteria criteria = new DoctorStatisticCriteria();
            criteria.setFarmId(farmId);
            criteria.setPigType(pigType);
            List<Date> list = DateUtil.getDates(DateUtil.toDate(startAt), DateUtil.toDate(endAt));
            if (list.isEmpty()) {
                log.error("flush group daily startAt or endAt is illegal, startAt:{}, endAt:{}", startAt, endAt);
                return Response.fail("startAt.or.endAt.is.error");
            }

            list.forEach(date -> {
                criteria.setSumAt(DateUtil.toDateString(date));
                log.info("flush group daily farmId:{}, pigType:{}, sumAt:{}", criteria.getFarmId(), criteria.getPigType(), criteria.getSumAt());
                doctorDailyReportV2Manager.flushGroupDaily(criteria);
            });
            log.info("flush group daily for pigType end, consume:{}minute", stopwatch.elapsed(TimeUnit.MINUTES));
            return Response.ok(Boolean.TRUE);
        } catch (Exception e) {
            log.error("flush group daily failed, farmId:{}, pigType:{}, startAt:{}, endAt:{}, cause:{}",
                    farmId, pigType, startAt, endAt, Throwables.getStackTraceAsString(e));
            return Response.fail("flush.group.daily.failed");
        }
    }

    @Override
    public Response<Boolean> flushPigDaily(Long farmId, String startAt, String endAt) {
        try {
            Stopwatch stopwatch = Stopwatch.createStarted();
            log.info("flush pig daily starting, farmId:{}, startAt:{}, endAt:{}", farmId, startAt, endAt);
            DoctorStatisticCriteria criteria = new DoctorStatisticCriteria();
            criteria.setFarmId(farmId);
            List<Date> list = DateUtil.getDates(DateUtil.toDate(startAt), DateUtil.toDate(endAt));
            if (list.isEmpty()) {
                log.error("flush pig daily startAt or endAt is illegal, startAt:{}, endAt:{}", startAt, endAt);
                return Response.fail("startAt.or.endAt.is.error");
            }

            list.forEach(date -> {
                criteria.setSumAt(DateUtil.toDateString(date));
                log.info("flush pig daily farmId:{}, sumAt:{}", criteria.getFarmId(), criteria.getSumAt());
                doctorDailyReportV2Manager.flushPigDaily(criteria);
            });

            log.info("flush pig daily end, consume:{}minute", stopwatch.elapsed(TimeUnit.MINUTES));
            return Response.ok(Boolean.TRUE);
        } catch (Exception e) {
            log.error("flush pig daily failed, farmId:{}, startAt:{}, endAt:{}, cause:{}",
                    farmId, startAt, endAt, Throwables.getStackTraceAsString(e));
            return Response.fail("flush.pig.daily.failed");
        }
    }

    @Override
    public Response<Boolean> generateYesterdayAndToday(List<Long> farmIds, Date date) {
        try {
            log.info("generate yesterday and today starting");
            Stopwatch stopWatch = Stopwatch.createStarted();
            Map<Long, Date> longDateMap = doctorDailyReportV2Manager.generateYesterdayAndToday(farmIds, date);
            log.info("generate yesterday and today end, consume:{}minute", stopWatch.elapsed(TimeUnit.MINUTES));
            doctorReportBiManager.synchronizeDeltaDayBiData(longDateMap);
            log.info("synchronize yesterday and today end, consume:{}minute", stopWatch.elapsed(TimeUnit.MINUTES));
            return Response.ok(Boolean.TRUE);
        } catch (Exception e) {
            log.error("generate yesterday and today failed, cause:{}", Throwables.getStackTraceAsString(e));
            return Response.fail("generate.yesterday.and.today.failed");
        }
    }

    @Override
    public Response<Boolean> synchronize(List<Long> farmIds, Date date) {
        try {
            log.info("synchronize yesterday and today starting");
            Stopwatch stopWatch = Stopwatch.createStarted();
            Map<Long, Date> dateMap = doctorDailyReportV2Manager.queryFarmEarlyEventAtImpl(DateUtil.toDateString(date));
            doctorReportBiManager.synchronizeDeltaDayBiData(dateMap);
            log.info("synchronize yesterday and today end, consume:{}minute", stopWatch.elapsed(TimeUnit.MINUTES));
            return Response.ok(Boolean.TRUE);
        } catch (Exception e) {
            log.error("synchronize yesterday and today failed,cause:{}", Throwables.getStackTraceAsString(e));
            return Response.fail("synchronize.yesterday.and.today.failed");
        }
    }

    @Override
    @Deprecated
    public Response<Boolean> synchronizeFullBiData() {
        try {
            doctorReportBiManager.synchronizeFullData();
            return Response.ok(Boolean.TRUE);
        } catch (Exception e) {
            log.error("synchronize full bi data failed,cause:{}", Throwables.getStackTraceAsString(e));
            return Response.fail("synchronize.full.bi.data.failed");
        }
    }

    @Override
    public Response<Boolean> synchronizeDeltaDayBiData(Long orzId, Date start, Integer orzType) {
        try {
            log.info("synchronize Delta day bi data starting, orzId:{}, orzType:{}, start:{}", orzId, orzType, start);
            doctorReportBiManager.synchronizeDeltaDayBiData(orzId, start, orzType);
            log.info("synchronize Delta day bi data end");
            return Response.ok(Boolean.TRUE);
        } catch (Exception e) {
            log.error("synchronize delta day bi data failed,orzId:{}, start:{}, orzType:{}, cause:{}",
                    orzId, start, orzType, Throwables.getStackTraceAsString(e));
            return Response.fail("synchronize.delta.day.bi.data.failed");
        }
    }

    @Override
    public Response<Boolean> synchronizeDelta(Long orzId, Date start, Integer orzType) {
        try {
            doctorReportBiManager.synchronizeDelta(orzId, start, orzType);
            return Response.ok(Boolean.TRUE);
        } catch (Exception e) {
            log.error("synchronize delta failed,orzId:{}, start:{}, orzType:{}, cause:{}",
                    orzId, start, orzType, Throwables.getStackTraceAsString(e));
            return Response.fail("synchronize.delta.failed");
        }
    }

    @Override
    public Response<DoctorDimensionReport> dimensionReport(DoctorDimensionCriteria dimensionCriteria) {
        try {
            return Response.ok(doctorReportBiManager.dimensionReport(dimensionCriteria));
        } catch (Exception e) {
            log.error("dimension report failed,dimension:{}, cause:{}", dimensionCriteria, Throwables.getStackTraceAsString(e));
            return Response.fail("dimension.report.failed");
        }

    }

    @Override
    public Response<Boolean> syncWarehouse(Date date) {
        warehouseSynchronizer.sync(date);
        return Response.ok(true);
    }

    @Override
    public Response<Boolean> syncWarehouse(Integer dateType, Integer orgType) {
        DoctorDimensionCriteria criteria = new DoctorDimensionCriteria();
        criteria.setDateType(dateType);
        criteria.setOrzType(orgType);

        warehouseSynchronizer.delete(criteria);
        warehouseSynchronizer.sync(criteria);
        return Response.ok(true);
    }

    @Override
    public Response<Boolean> syncEfficiency(Date date) {
        efficiencySynchronizer.sync(date);

        return Response.ok(true);
    }


    @Override
    public Response<Boolean> syncEfficiency(Integer dateType, Integer orgType) {
        DoctorDimensionCriteria criteria = new DoctorDimensionCriteria();
        criteria.setDateType(dateType);
        criteria.setOrzType(orgType);

        efficiencySynchronizer.delete(criteria);
        efficiencySynchronizer.sync(criteria);
        return Response.ok(true);
    }

    @Override
    public Response<Boolean> syncEfficiency(Long farmId) {

        DoctorDimensionCriteria criteria = new DoctorDimensionCriteria();
        criteria.setDateType(DateDimension.MONTH.getValue());
        criteria.setOrzType(OrzDimension.FARM.getValue());
        criteria.setOrzId(farmId);

        efficiencySynchronizer.delete(criteria);
        efficiencySynchronizer.sync(criteria);


        criteria.setDateType(DateDimension.QUARTER.getValue());
        efficiencySynchronizer.delete(criteria);
        efficiencySynchronizer.sync(criteria);

        criteria.setDateType(DateDimension.YEAR.getValue());
        efficiencySynchronizer.delete(criteria);
        efficiencySynchronizer.sync(criteria);


        criteria.setDateType(DateDimension.MONTH.getValue());
        criteria.setOrzDimensionName(OrzDimension.ORG.getName());
        efficiencySynchronizer.delete(criteria);
        efficiencySynchronizer.sync(criteria);

        criteria.setDateType(DateDimension.QUARTER.getValue());
        efficiencySynchronizer.delete(criteria);
        efficiencySynchronizer.sync(criteria);

        criteria.setDateType(DateDimension.YEAR.getValue());
        efficiencySynchronizer.delete(criteria);
        efficiencySynchronizer.sync(criteria);

        return Response.ok(true);
    }

    @Override
    public Response<List<DoctorFarmLiveStockDto>> findFarmsLiveStock(List<Long> farmIdList) {
        try {
            Date now = new Date();
            List<DoctorFarmLiveStockDto> dtos = farmIdList.parallelStream().map(farmId -> {
                DoctorDimensionCriteria dimensionCriteria =
                        new DoctorDimensionCriteria(farmId, OrzDimension.FARM.getValue(), now, DateDimension.DAY.getValue());
                DoctorDimensionReport report = doctorReportBiManager.dimensionReport(dimensionCriteria);
                return DoctorFarmLiveStockDto.builder()
                        .farmId(farmId)
                        .boar(notNull(report.getReportBoar()) ? report.getReportBoar().getEnd() : 0)
                        .farrow(notNull(report.getReportDeliver()) ? report.getReportDeliver().getPigletEnd() : 0)
                        .deliverSow(notNull(report.getReportDeliver()) ? report.getReportDeliver().getEnd() : 0)
                        .sow(notNull(report.getReportSow()) ? report.getReportSow().getEnd() : 0)
                        .houbei(notNull(report.getReportReserve()) ? report.getReportReserve().getEnd() : 0)
                        .peihuai(notNull(report.getReportMating()) ? report.getReportMating().getEnd() : 0)
                        .nursery(notNull(report.getReportNursery()) ? report.getReportNursery().getEnd() : 0)
                        .fatten(notNull(report.getReportFatten()) ? report.getReportFatten().getEnd() : 0)
                        .build();

            }).collect(Collectors.toList());
            return Response.ok(dtos);
        } catch (Exception e) {
            log.error("find farms live stock failed, farmIdList:{}, cause:{}", farmIdList, Throwables.getStackTraceAsString(e));
            return Response.fail("find.farms.live.stock.failed");
        }

    }

    @Override
    public Response<List<DoctorFarmEarlyEventAtDto>> findEarLyAt() {
        try {
            List<DoctorFarmEarlyEventAtDto> pigList = doctorPigEventDao.findEarLyAt();
            List<DoctorFarmEarlyEventAtDto> groupList = doctorGroupEventDao.findEarLyAt();
            pigList.addAll(groupList);
            Map<Long, List<DoctorFarmEarlyEventAtDto>> map = pigList.stream().collect(Collectors.groupingBy(DoctorFarmEarlyEventAtDto::getFarmId));
            List<DoctorFarmEarlyEventAtDto> list = Lists.newArrayList();
            map.forEach((key, value) -> list.add(new DoctorFarmEarlyEventAtDto(key, value.stream().map(DoctorFarmEarlyEventAtDto::getEventAt).min(Date::compareTo).get())));
            return Response.ok(list);
        } catch (Exception e) {
            log.error("find early at failed,cause:{}", Throwables.getStackTraceAsString(e));
            return Response.fail("find.early.at.failed");
        }
    }

    @Override
    public Response<Boolean> generateDeliverRate(List<Long> farmIds, Date date) {
        try {
            log.info("generate deliver rate starting");
            Map<Long, Date> farmToDate = doctorDailyReportV2Manager.queryFarmDeliverRateDate(DateUtil.toDateString(date));
            doctorReportBiDataSynchronize.synchronizeDeltaDeliverRate(farmToDate);
            log.info("generate deliver rate end");
            return Response.ok(Boolean.TRUE);
        } catch (Exception e) {
            log.error("generate deliver rate failed, date:{},cause:{}", date, Throwables.getStackTraceAsString(e));
            return Response.fail("generate.deliver.rate.failed");
        }
    }

    @Override
    public Response<Boolean> flushDeliverRate(Long orzId, Integer orzType, Date start) {
        try {
            log.info("flush deliver rate starting, orzId:{}, orzType:{}, start:{}", orzId, orzType, start);
            doctorReportBiDataSynchronize.synchronizeDeliverRate(orzId, orzType, start, 1);
            log.info("flush deliver rate end");
            return Response.ok(Boolean.TRUE);
        } catch (Exception e) {
            log.error(",cause:{}", Throwables.getStackTraceAsString(e));
            return Response.fail("flush.deliver.rate.failed");
        }
    }

    //?????????
    @Override
    public Response<List<DoctorFarmLiveStockDto>> findFarmsLiveStock1(List<Long> farmIdList, Integer Type) {
        try {
            Date now = new Date();
            List<DoctorFarmLiveStockDto> dtos = farmIdList.parallelStream().map(farmId -> {
                DoctorDimensionCriteria dimensionCriteria =
                        new DoctorDimensionCriteria(farmId, Type, now, DateDimension.DAY.getValue());
                DoctorDimensionReport report = doctorReportBiManager.dimensionReport(dimensionCriteria);
                return DoctorFarmLiveStockDto.builder()
                        .farmId(farmId)
                        .boar(notNull(report.getReportBoar()) ? report.getReportBoar().getEnd() : 0)
                        .farrow(notNull(report.getReportDeliver()) ? report.getReportDeliver().getPigletEnd() : 0)
                        .deliverSow(notNull(report.getReportDeliver()) ? report.getReportDeliver().getEnd() : 0)
                        .sow(notNull(report.getReportSow()) ? report.getReportSow().getEnd() : 0)
                        .houbei(notNull(report.getReportReserve()) ? report.getReportReserve().getEnd() : 0)
                        .peihuai(notNull(report.getReportMating()) ? report.getReportMating().getEnd() : 0)
                        .nursery(notNull(report.getReportNursery()) ? report.getReportNursery().getEnd() : 0)
                        .fatten(notNull(report.getReportFatten()) ? report.getReportFatten().getEnd() : 0)
                        .build();

            }).collect(Collectors.toList());
            return Response.ok(dtos);
        } catch (Exception e) {
            log.error("find farms live stock failed, farmIdList:{}, cause:{}", farmIdList, Throwables.getStackTraceAsString(e));
            return Response.fail("find.farms.live.stock.failed");
        }
    }
}


