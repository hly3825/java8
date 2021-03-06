package io.terminus.doctor.event.reportBi;

import com.google.common.base.Stopwatch;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.utils.Arguments;
import io.terminus.common.utils.BeanMapper;
import io.terminus.doctor.common.enums.IsOrNot;
import io.terminus.doctor.common.enums.PigType;
import io.terminus.doctor.common.utils.DateUtil;
import io.terminus.doctor.common.utils.RespHelper;
import io.terminus.doctor.event.cache.DoctorDepartmentCache;
import io.terminus.doctor.event.dao.DoctorGroupDailyDao;
import io.terminus.doctor.event.dao.DoctorPigDailyDao;
import io.terminus.doctor.event.dao.DoctorReportNpdDao;
import io.terminus.doctor.event.dto.DoctorDimensionCriteria;
import io.terminus.doctor.event.dto.reportBi.DoctorGroupDailyExtend;
import io.terminus.doctor.event.dto.reportBi.DoctorPigDailyExtend;
import io.terminus.doctor.event.enums.DateDimension;
import io.terminus.doctor.event.enums.OrzDimension;
import io.terminus.doctor.event.model.DoctorGroupDaily;
import io.terminus.doctor.event.model.DoctorPigDaily;
import io.terminus.doctor.event.reportBi.helper.DateHelper;
import io.terminus.doctor.event.reportBi.helper.FieldHelper;
import io.terminus.doctor.event.reportBi.synchronizer.DoctorBoarSynchronizer;
import io.terminus.doctor.event.reportBi.synchronizer.DoctorDeliverSynchronizer;
import io.terminus.doctor.event.reportBi.synchronizer.DoctorEfficiencySynchronizer;
import io.terminus.doctor.event.reportBi.synchronizer.DoctorFattenSynchronizer;
import io.terminus.doctor.event.reportBi.synchronizer.DoctorMatingSynchronizer;
import io.terminus.doctor.event.reportBi.synchronizer.DoctorNurserySynchronizer;
import io.terminus.doctor.event.reportBi.synchronizer.DoctorReserveSynchronizer;
import io.terminus.doctor.event.reportBi.synchronizer.DoctorSowSynchronizer;
import io.terminus.doctor.event.reportBi.synchronizer.DoctorWarehouseSynchronizer;
import io.terminus.doctor.user.model.DoctorFarm;
import io.terminus.doctor.user.service.DoctorFarmReadService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.DateUtils;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.TimeUnit;

import static io.terminus.doctor.common.utils.Checks.expectNotNull;

/**
 * Created by xjn on 18/1/9.
 * email:xiaojiannan@terminus.io
 */
@Slf4j
@Component
public class DoctorReportBiDataSynchronize {
    private final DoctorPigDailyDao doctorPigDailyDao;
    private final DoctorGroupDailyDao doctorGroupDailyDao;
    private final DoctorReportNpdDao doctorReportNpdDao;
    private final DoctorReserveSynchronizer reserveSynchronizer;
    private final DoctorFattenSynchronizer fattenSynchronizer;
    private final DoctorNurserySynchronizer nurserySynchronizer;
    private final DoctorBoarSynchronizer boarSynchronizer;
    private final DoctorSowSynchronizer sowSynchronizer;
    private final DoctorMatingSynchronizer matingSynchronizer;
    private final DoctorDeliverSynchronizer deliverSynchronizer;
    private final DoctorWarehouseSynchronizer warehouseSynchronizer;
    private final DoctorEfficiencySynchronizer efficiencySynchronizer;

    private final Integer DELTA_DAY = 600;
    private final Integer REAL_TIME_INTERVAL = 1;

    @RpcConsumer
    private DoctorFarmReadService doctorFarmReadService;
    @RpcConsumer
    private DoctorDepartmentCache cache;

    @Autowired
    public DoctorReportBiDataSynchronize(DoctorPigDailyDao doctorPigDailyDao,
                                         DoctorGroupDailyDao doctorGroupDailyDao,
                                         DoctorReportNpdDao doctorReportNpdDao,
                                         DoctorReserveSynchronizer reserveSynchronizer,
                                         DoctorFattenSynchronizer fattenSynchronizer,
                                         DoctorNurserySynchronizer nurserySynchronizer,
                                         DoctorBoarSynchronizer boarSynchronizer,
                                         DoctorSowSynchronizer sowSynchronizer,
                                         DoctorMatingSynchronizer matingSynchronizer,
                                         DoctorDeliverSynchronizer deliverSynchronizer,
                                         DoctorWarehouseSynchronizer warehouseSynchronizer,
                                         DoctorEfficiencySynchronizer efficiencySynchronizer) {
        this.doctorPigDailyDao = doctorPigDailyDao;
        this.doctorGroupDailyDao = doctorGroupDailyDao;
        this.doctorReportNpdDao = doctorReportNpdDao;
        this.reserveSynchronizer = reserveSynchronizer;
        this.fattenSynchronizer = fattenSynchronizer;
        this.nurserySynchronizer = nurserySynchronizer;
        this.boarSynchronizer = boarSynchronizer;
        this.sowSynchronizer = sowSynchronizer;
        this.matingSynchronizer = matingSynchronizer;
        this.deliverSynchronizer = deliverSynchronizer;
        this.warehouseSynchronizer = warehouseSynchronizer;
        this.efficiencySynchronizer = efficiencySynchronizer;
    }

    /**
     * ?????????????????????????????????????????????????????????????????????
     */
    @Deprecated
    public void synchronizeFullBiData() {
        log.info("synchronize full bi data starting");
        Stopwatch stopwatch = Stopwatch.createStarted();

//        cleanFullBiData();
        synchronizeBiDataImpl();
        log.info("synchronize full bi data end, minute:{}m", stopwatch.elapsed(TimeUnit.MINUTES));
    }

    /**
     * ??????????????????
     */
    public void synchronizeDeltaDayBiData(Map<Long, Date> farmIdToSumAt) {
        log.info("synchronize delta day bi data starting");
        Stopwatch stopwatch = Stopwatch.createStarted();

        farmIdToSumAt.entrySet().parallelStream().forEach(entry -> {
            synchronizeDeltaBiData(entry.getKey(), OrzDimension.FARM.getValue(), entry.getValue(), 1);
            log.info("synchronize delta farm ending: farmId:{}, date:{}", entry.getKey(), entry.getValue());
        });
        Map<Long, Date> orgIdToSumAt = Maps.newHashMap();
        farmIdToSumAt.forEach((key, value) -> {
            Long orgId = cache.getUnchecked(key).getOrgId();
            if (!orgIdToSumAt.containsKey(orgId) || value.before(orgIdToSumAt.get(orgId))) {
                orgIdToSumAt.put(orgId, value);
            }
        });
        orgIdToSumAt.entrySet().parallelStream().forEach(entry -> {
            synchronizeDeltaBiData(entry.getKey(), OrzDimension.ORG.getValue(), entry.getValue(), 1);
            log.info("synchronize delta orgId ending: orgId:{}, date:{}", entry.getKey(), entry.getValue());
        });

        //???????????????????????????
        Map<Long, Date> groupIdToSumAt = Maps.newHashMap();
        farmIdToSumAt.forEach((key, value) -> {
            Long groupId = cache.getUnchecked(key).getCliqueId();
            if (!groupIdToSumAt.containsKey(groupId) || value.before(groupIdToSumAt.get(groupId))) {
                groupIdToSumAt.put(groupId, value);
            }
        });
        groupIdToSumAt.entrySet().parallelStream().forEach(entry -> {
            if(entry.getKey() != 0) {
                synchronizeDeltaBiData(entry.getKey(), OrzDimension.CLIQUE.getValue(), entry.getValue(), 1);
                log.info("synchronize delta groupId ending: groupId:{}, date:{}", entry.getKey(), entry.getValue());
            }
        });


        //??????????????????????????????????????????????????????????????????????????????????????????
        List<Date> dates = warehouseSynchronizer.getChangedDate(new Date());
        for (Date d : dates) {
            if (DateUtils.isSameDay(d, new Date()))
                continue;

            warehouseSynchronizer.sync(d);
        }

        warehouseSynchronizer.sync(new Date());
        efficiencySynchronizer.sync(new Date());

        log.info("synchronize delta day bi data end, minute:{}m", stopwatch.elapsed(TimeUnit.MINUTES));
    }

    /**
     * ????????????
     *
     * @param orzId ??????id
     * @param start  ????????????????????? ????????????sumAt??????
     */
    public void synchronizeDeltaDayBiData(Long orzId, Date start, Integer orzType) {
        log.info("synchronize delta day bi data starting, farmId:{}, start:{}", orzId, start);
        Stopwatch stopwatch = Stopwatch.createStarted();
        synchronizeDeltaBiData(orzId, orzType, start, 1);
        log.info("synchronize delta day bi data end, minute:{}m", stopwatch.elapsed(TimeUnit.MINUTES));
    }

    /**
     * ??????????????????
     */
    public void synchronizeRealTimeBiData(Long orzId, Integer orzType) {
        log.info("synchronize real time bi data starting");
        Date updatedAt = DateTime.now().minusMinutes(REAL_TIME_INTERVAL).toDate();
        Date pigSumAt = doctorPigDailyDao.minSumAtForUpdated(orzId, orzType, updatedAt);
        Date groupSumAt = doctorGroupDailyDao.minSumAtForUpdated(orzId, orzType, updatedAt);
        synchronizeDelta(orzId, orzType, pigSumAt, groupSumAt, 1, IsOrNot.YES.getKey());
        log.info("synchronize real time bi data end");
    }

    /**
     * ????????????????????????
     * @param orzId
     * @param orzType
     * @param date
     * @param type
     */
    public void synchronizeDelta(Long orzId, Integer orzType, Date date, Integer type) {
        synchronizeDelta(orzId, orzType, date, date, type, IsOrNot.NO.getKey());
    }

    /**
     * ????????????
     * @param orzId
     * @param orzType
     * @param pigDate
     * @param groupDate
     * @param type
     * @param isRealTime
     */
    public void synchronizeDelta(Long orzId, Integer orzType, Date pigDate, Date groupDate, Integer type, Integer isRealTime) {

        //???????????????????????????????????????????????????????????????????????????????????????
        if (Objects.equals(orzType, OrzDimension.FARM.getValue())) {
            DoctorFarm doctorFarm = RespHelper.orServEx(doctorFarmReadService.findFarmById(orzId));
            synchronizeDeltaBiData(orzId, orzType, pigDate, groupDate, type, isRealTime);
            synchronizeDeltaBiData(doctorFarm.getOrgId(), OrzDimension.ORG.getValue(), pigDate, groupDate, type, isRealTime);
            //????????????id?????????id
            Long groupId = doctorPigDailyDao.getGroupIdByOrgId(doctorFarm.getOrgId());
            if(groupId !=null && groupId!= 0L) {
                synchronizeDeltaBiData(groupId, OrzDimension.CLIQUE.getValue(), pigDate, groupDate, type, isRealTime);
            }
        } else if (Objects.equals(orzType, OrzDimension.ORG.getValue())) {
            //?????????????????????????????????????????????????????????????????????????????????
            List<DoctorFarm> farmList = RespHelper.orServEx(doctorFarmReadService.findFarmsByOrgId(orzId));
            farmList.parallelStream().forEach(doctorFarm -> {
                synchronizeDeltaBiData(doctorFarm.getId(), OrzDimension.FARM.getValue(), pigDate, groupDate, type, isRealTime);
            });
            synchronizeDeltaBiData(orzId, orzType, pigDate, groupDate, type, isRealTime);
        }else if (Objects.equals(orzType, OrzDimension.CLIQUE.getValue())){
            //?????????????????????????????????????????????????????????
            synchronizeDeltaBiData(orzId, OrzDimension.CLIQUE.getValue(), pigDate, groupDate, type, isRealTime);
        }
    }

    /**
     * ????????????????????????????????????
     * @param orzId
     * @param orzType
     * @param date
     * @param type
     */
    private void synchronizeDeltaBiData(Long orzId, Integer orzType, Date date, Integer type) {
        synchronizeDeltaBiData(orzId, orzType, date, date, type, IsOrNot.NO.getKey());
    }

        /**
         * ?????????????????????
         * @param orzId ??????id
         * @param orzType ????????????
         *                @see OrzDimension
         * @param pigDate ?????????date ?????????????????????date
         * @param groupDate ????????????date ?????????????????????date
         * @param type  ?????????????????????????????????date???????????????1-???????????????sumAt??? 2-???????????????updatedAt
         * @param isRealTime ???????????????????????????
         *
         */
    private void synchronizeDeltaBiData(Long orzId, Integer orzType, Date pigDate, Date groupDate, Integer type, Integer isRealTime) {

        //??????????????????????????????
        List<DoctorGroupDaily> groupDailyList = doctorGroupDailyDao.findByAfter(orzId, orzType, groupDate, type);
        if (!Arguments.isNullOrEmpty(groupDailyList)) {
            List<DoctorDimensionCriteria> dimensionCriteriaList = Lists.newArrayList();
            dimensionCriteriaList.addAll(doctorGroupDailyDao.findByDateType(orzId, groupDate, type, DateDimension.DAY.getValue(), orzType));
            dimensionCriteriaList.addAll(doctorGroupDailyDao.findByDateType(orzId, groupDate, type, DateDimension.WEEK.getValue(), orzType));
            dimensionCriteriaList.addAll(doctorGroupDailyDao.findByDateType(orzId, groupDate, type, DateDimension.MONTH.getValue(), orzType));
            dimensionCriteriaList.addAll(doctorGroupDailyDao.findByDateType(orzId, groupDate, type, DateDimension.QUARTER.getValue(), orzType));
            dimensionCriteriaList.addAll(doctorGroupDailyDao.findByDateType(orzId, groupDate, type, DateDimension.YEAR.getValue(), orzType));
            dimensionCriteriaList.parallelStream().forEach(dimensionCriteria -> {
                DateDimension dateDimension = DateDimension.from(dimensionCriteria.getDateType());
                dimensionCriteria.setStartAt(DateHelper.withDateStartDay(dimensionCriteria.getSumAt(), dateDimension));
                dimensionCriteria.setEndAt(DateHelper.withDateEndDay(dimensionCriteria.getSumAt(), dateDimension));
                dimensionCriteria.setIsRealTime(isRealTime);
                synchronizeGroupBiData(doctorGroupDailyDao.selectOneSumForDimension(dimensionCriteria), dimensionCriteria);
            });
        }

        //????????????????????????
        List<DoctorPigDaily> pigDailyList = doctorPigDailyDao.findByAfter(orzId, orzType, pigDate, type);
        if (!Arguments.isNullOrEmpty(pigDailyList)) {
            List<DoctorDimensionCriteria> criteriaList = Lists.newArrayList();
            criteriaList.addAll(doctorPigDailyDao.findByDateType(orzId, pigDate, type, DateDimension.DAY.getValue(), orzType));
            criteriaList.addAll(doctorPigDailyDao.findByDateType(orzId, pigDate, type, DateDimension.WEEK.getValue(), orzType));
            criteriaList.addAll(doctorPigDailyDao.findByDateType(orzId, pigDate, type, DateDimension.MONTH.getValue(), orzType));
            criteriaList.addAll(doctorPigDailyDao.findByDateType(orzId, pigDate, type, DateDimension.QUARTER.getValue(), orzType));
            criteriaList.addAll(doctorPigDailyDao.findByDateType(orzId, pigDate, type, DateDimension.YEAR.getValue(), orzType));
            criteriaList.parallelStream().forEach(dimensionCriteria -> {
                DateDimension dateDimension = DateDimension.from(dimensionCriteria.getDateType());
                dimensionCriteria.setStartAt(DateHelper.withDateStartDay(dimensionCriteria.getSumAt(), dateDimension));
                dimensionCriteria.setEndAt(DateHelper.withDateEndDay(dimensionCriteria.getSumAt(), dateDimension));
                dimensionCriteria.setIsRealTime(isRealTime);
                synchronizePigBiData(doctorPigDailyDao.selectOneSumForDimension(dimensionCriteria), dimensionCriteria);
            });
        }

    }

    /**
     * ?????????????????????????????????
     * @param farmIdToSumAt
     */
    public void synchronizeDeltaDeliverRate(Map<Long, Date> farmIdToSumAt) {
        log.info("synchronize delta day bi data starting");
        farmIdToSumAt.entrySet().parallelStream().forEach(entry -> {
            synchronizeDeliverRate(entry.getKey(), OrzDimension.FARM.getValue(), entry.getValue(), 1);
            log.info("synchronize delta farm ending: farmId:{}, date:{}", entry.getKey(), entry.getValue());
        });
        Map<Long, Date> orgIdToSumAt = Maps.newHashMap();
        farmIdToSumAt.forEach((key, value) -> {
            Long orgId = cache.getUnchecked(key).getOrgId();
            if (!orgIdToSumAt.containsKey(orgId) || value.before(orgIdToSumAt.get(orgId))) {
                orgIdToSumAt.put(orgId, value);
            }
        });
        orgIdToSumAt.entrySet().parallelStream().forEach(entry -> {
            synchronizeDeliverRate(entry.getKey(), OrzDimension.ORG.getValue(), entry.getValue(), 1);
            log.info("synchronize delta orgId ending: orgId:{}, date:{}", entry.getKey(), entry.getValue());
        });
        log.info("synchronize delta day bi data end");

    }

    /**
     * ???????????????
     * @param orzId
     * @param orzType
     * @param pigDate
     * @param type
     */
    public void synchronizeDeliverRate(Long orzId, Integer orzType, Date pigDate, Integer type) {
        List<DoctorDimensionCriteria> criteriaList = Lists.newArrayList();
        criteriaList.addAll(doctorPigDailyDao.findByDateType(orzId, pigDate, type, DateDimension.DAY.getValue(), orzType));
        criteriaList.addAll(doctorPigDailyDao.findByDateType(orzId, pigDate, type, DateDimension.WEEK.getValue(), orzType));
        criteriaList.addAll(doctorPigDailyDao.findByDateType(orzId, pigDate, type, DateDimension.MONTH.getValue(), orzType));
        criteriaList.addAll(doctorPigDailyDao.findByDateType(orzId, pigDate, type, DateDimension.QUARTER.getValue(), orzType));
        criteriaList.addAll(doctorPigDailyDao.findByDateType(orzId, pigDate, type, DateDimension.YEAR.getValue(), orzType));
        criteriaList.parallelStream().forEach(deliverSynchronizer::synchronizeDeliverRate);
    }

    /**
     * ???????????????????????????
     *
     * @param groupDailyList
     */
    private void synchronizeGroupForDay(List<DoctorGroupDaily> groupDailyList, Integer isRealTime) {
        if (Arguments.isNullOrEmpty(groupDailyList)) {
            return;
        }
        DoctorGroupDailyExtend extend = new DoctorGroupDailyExtend();
        DoctorDimensionCriteria doctorDimensionCriteria = new DoctorDimensionCriteria();
        doctorDimensionCriteria.setOrzType(OrzDimension.FARM.getValue());
        doctorDimensionCriteria.setDateType(DateDimension.DAY.getValue());
        doctorDimensionCriteria.setIsRealTime(isRealTime);
        groupDailyList.forEach(groupDaily -> {
            BeanMapper.copy(groupDaily, extend);
            extend.setDailyLivestockOnHand(groupDaily.getEnd());
            extend.setStart(groupDaily.getStart());
            extend.setEnd(groupDaily.getEnd());
            synchronizeGroupBiData(extend, doctorDimensionCriteria);
        });
    }

    /**
     * ???????????????????????????
     *
     * @param pigDailyList
     */
    public void synchronizePigForDay(List<DoctorPigDaily> pigDailyList, Integer isRealTime) {
        if (Arguments.isNullOrEmpty(pigDailyList)) {
            return;
        }
        DoctorPigDailyExtend extend = new DoctorPigDailyExtend();
        DoctorDimensionCriteria doctorDimensionCriteria = new DoctorDimensionCriteria();
        doctorDimensionCriteria.setOrzType(OrzDimension.FARM.getValue());
        doctorDimensionCriteria.setDateType(DateDimension.DAY.getValue());
        doctorDimensionCriteria.setIsRealTime(isRealTime);
        pigDailyList.forEach(pigDaily -> {
            BeanMapper.copy(pigDaily, extend);
            extend.setSowStart(pigDaily.getSowPhStart() + pigDaily.getSowCfStart());
            extend.setSowEnd(pigDaily.getSowPhEnd() + pigDaily.getSowCfEnd());
            extend.setSowDailyPigCount(extend.getSowEnd());
            extend.setBoarDailyPigCount(pigDaily.getBoarEnd());
            synchronizePigBiData(extend, doctorDimensionCriteria);
        });
    }

    /**
     * ??????bi????????????????????????
     */
    public void cleanFullBiData() {
        deliverSynchronizer.deleteAll();
        fattenSynchronizer.deleteAll();
        nurserySynchronizer.deleteAll();
        reserveSynchronizer.deleteAll();
        boarSynchronizer.deleteAll();
        sowSynchronizer.deleteAll();
//        matingSynchronizer.deleteAll();
        efficiencySynchronizer.deleteAll();
        warehouseSynchronizer.deleteAll();
    }

    /**
     * ??????????????????
     */
    private void synchronizeBiDataImpl() {
        //???????????????
//        synchronizeFullBiDataForDay();
        List<DoctorDimensionCriteria> dimensionCriteriaList = Lists.newArrayList();
        DoctorDimensionCriteria dimensionCriteria;
        //???????????????
        dimensionCriteria = new DoctorDimensionCriteria();
        dimensionCriteria.setOrzType(OrzDimension.FARM.getValue());
        dimensionCriteria.setDateType(DateDimension.WEEK.getValue());
        dimensionCriteriaList.add(dimensionCriteria);
        //???????????????
        dimensionCriteria = new DoctorDimensionCriteria();
        dimensionCriteria.setOrzType(OrzDimension.FARM.getValue());
        dimensionCriteria.setDateType(DateDimension.MONTH.getValue());
        dimensionCriteriaList.add(dimensionCriteria);
        //???????????????
        dimensionCriteria = new DoctorDimensionCriteria();
        dimensionCriteria.setOrzType(OrzDimension.FARM.getValue());
        dimensionCriteria.setDateType(DateDimension.QUARTER.getValue());
        dimensionCriteriaList.add(dimensionCriteria);

        //???????????????
        dimensionCriteria = new DoctorDimensionCriteria();
        dimensionCriteria.setOrzType(OrzDimension.FARM.getValue());
        dimensionCriteria.setDateType(DateDimension.YEAR.getValue());
        dimensionCriteriaList.add(dimensionCriteria);

        //???????????????
        dimensionCriteria = new DoctorDimensionCriteria();
        dimensionCriteria.setOrzType(OrzDimension.ORG.getValue());
        dimensionCriteria.setDateType(DateDimension.DAY.getValue());
        dimensionCriteriaList.add(dimensionCriteria);

        //???????????????
        dimensionCriteria = new DoctorDimensionCriteria();
        dimensionCriteria.setOrzType(OrzDimension.ORG.getValue());
        dimensionCriteria.setDateType(DateDimension.WEEK.getValue());
        dimensionCriteriaList.add(dimensionCriteria);

        //???????????????
        dimensionCriteria = new DoctorDimensionCriteria();
        dimensionCriteria.setOrzType(OrzDimension.ORG.getValue());
        dimensionCriteria.setDateType(DateDimension.MONTH.getValue());
        dimensionCriteriaList.add(dimensionCriteria);

        //???????????????
        dimensionCriteria = new DoctorDimensionCriteria();
        dimensionCriteria.setOrzType(OrzDimension.ORG.getValue());
        dimensionCriteria.setDateType(DateDimension.QUARTER.getValue());
        dimensionCriteriaList.add(dimensionCriteria);

        //???????????????
        dimensionCriteria = new DoctorDimensionCriteria();
        dimensionCriteria.setOrzType(OrzDimension.ORG.getValue());
        dimensionCriteria.setDateType(DateDimension.YEAR.getValue());
        dimensionCriteriaList.add(dimensionCriteria);

        dimensionCriteriaList.parallelStream().forEach(this::synchronizeFullBiDataForDimension);
    }

    /**
     * ?????????????????????????????????
     *
     * @param dimensionCriteria
     */
    private void synchronizeFullBiDataForDimension(DoctorDimensionCriteria dimensionCriteria) {
        log.info("========dimension starting:orz:{}, dateType:{}",
                dimensionCriteria.getOrzType(), dimensionCriteria.getDateType());
        Stopwatch stopwatch = Stopwatch.createStarted();
        List<DoctorGroupDailyExtend> groupDailyList = doctorGroupDailyDao.sumForDimension(dimensionCriteria);
        log.info("========group daily size:{}, orz:{}, dateType:{}", groupDailyList.size(), dimensionCriteria.getOrzId(), dimensionCriteria.getDateType());
        groupDailyList.forEach(groupDaily -> synchronizeGroupBiData(groupDaily, dimensionCriteria));
        List<DoctorPigDailyExtend> pigDailyList = doctorPigDailyDao.sumForDimension(dimensionCriteria);
        pigDailyList.forEach(pigDaily -> synchronizePigBiData(pigDaily, dimensionCriteria));
        log.info("========pig daily size:{}, orz:{}, dateType:{}", pigDailyList.size(), dimensionCriteria.getOrzId(), dimensionCriteria.getDateType());

//        efficiencySynchronizer.sync(dimensionCriteria);
//        warehouseSynchronizer.sync(dimensionCriteria);

        log.info("========dimension ending orzId:{}, dateType:{},consume:{}m", stopwatch.elapsed(TimeUnit.MINUTES),
                dimensionCriteria.getOrzId(), dimensionCriteria.getDateType());
    }

    /**
     * ???????????????????????????
     */
    private void synchronizeFullBiDataForDay() {
        List<DoctorGroupDaily> groupDailyList;
        Integer pageSize = 5000;
        Integer pageNo = 0;
        while (true) {
            groupDailyList = doctorGroupDailyDao.paging(pageNo, pageSize).getData();
            synchronizeGroupForDay(groupDailyList, IsOrNot.NO.getKey());
            if (groupDailyList.size() < 5000) break;
            pageNo += 5000;
        }

        List<DoctorPigDaily> pigDailyList;
        pageNo = 0;
        while (true) {
            pigDailyList = doctorPigDailyDao.paging(pageNo, pageSize).getData();
            synchronizePigForDay(pigDailyList, IsOrNot.NO.getKey());
            if (pigDailyList.size() < 5000) break;
            pageNo += 5000;
        }

        DoctorDimensionCriteria criteria = new DoctorDimensionCriteria();
        criteria.setDateType(DateDimension.DAY.getValue());
        criteria.setOrzType(OrzDimension.FARM.getValue());
        warehouseSynchronizer.sync(criteria);
    }

    /**
     * ??????????????????????????????????????????
     *
     * @param groupDaily
     * @param dimensionCriteria
     */
    private void synchronizeGroupBiData(DoctorGroupDailyExtend groupDaily, DoctorDimensionCriteria dimensionCriteria) {
        log.debug("dimension:orgId{}, farmId:{}, dimensionCriteria:{}", groupDaily.getOrgId(), groupDaily.getFarmId(), dimensionCriteria);
        PigType pigType = expectNotNull(PigType.from(groupDaily.getPigType()), "pigType.is.illegal");
        if (Objects.equals(dimensionCriteria.getOrzType(), OrzDimension.ORG.getValue())) {
            dimensionCriteria.setOrzId(groupDaily.getOrgId());
        } else if (Objects.equals(dimensionCriteria.getOrzType(), OrzDimension.FARM.getValue())){
            dimensionCriteria.setOrzId(groupDaily.getFarmId());
        } else if(Objects.equals(dimensionCriteria.getOrzType(), OrzDimension.CLIQUE.getValue())){
            dimensionCriteria.setOrzId(groupDaily.getGroupId());
        }
        dimensionCriteria.setSumAt(groupDaily.getSumAt());
        dimensionCriteria.setPigType(groupDaily.getPigType());
//        if (!Objects.equals(dimensionCriteria.getDateType(), DateDimension.DAY.getValue())
//                || !Objects.equals(dimensionCriteria.getOrzType(), OrzDimension.FARM.getValue())) {
            Integer start;
            Integer end;
            if (Objects.equals(dimensionCriteria.getOrzType(), OrzDimension.FARM.getValue())) {
                start = doctorGroupDailyDao.farmStart(dimensionCriteria);
                end = doctorGroupDailyDao.farmEnd(dimensionCriteria);
            } else if(Objects.equals(dimensionCriteria.getOrzType(), OrzDimension.ORG.getValue())){
                Date minDate = doctorGroupDailyDao.minDate(dimensionCriteria);
                Date maxDate = doctorGroupDailyDao.maxDate(dimensionCriteria);
                start = doctorGroupDailyDao.orgDayStartStock(dimensionCriteria.getOrzId(), minDate, dimensionCriteria.getPigType());
                end = doctorGroupDailyDao.orgDayEndStock(dimensionCriteria.getOrzId(), maxDate, dimensionCriteria.getPigType());
                groupDaily.setDailyLivestockOnHand(FieldHelper.getInteger(doctorGroupDailyDao.orgDayAvgLiveStock(dimensionCriteria), DateUtil.getDeltaDaysAbs(maxDate, minDate)));
            } else{
                Date minDate = doctorGroupDailyDao.minDate(dimensionCriteria);
                Date maxDate = doctorGroupDailyDao.maxDate(dimensionCriteria);
                start = doctorGroupDailyDao.groupDayStartStock(dimensionCriteria.getOrzId(), minDate, dimensionCriteria.getPigType());
                end = doctorGroupDailyDao.groupDayEndStock(dimensionCriteria.getOrzId(), maxDate, dimensionCriteria.getPigType());
            }
            groupDaily.setStart(start);
            groupDaily.setEnd(end);
//        }
        switch (pigType) {
            case DELIVER_SOW:
                deliverSynchronizer.synchronize(groupDaily, dimensionCriteria);
                break;
            case NURSERY_PIGLET:
                nurserySynchronizer.synchronize(groupDaily, dimensionCriteria);
                break;
            case FATTEN_PIG:
                fattenSynchronizer.synchronize(groupDaily, dimensionCriteria);
                break;
            case RESERVE:

                reserveSynchronizer.synchronize(groupDaily, dimensionCriteria);
                break;
        }
    }

    /**
     * ???????????????????????????????????????
     *
     * @param dailyExtend
     * @param dimensionCriteria
     */
    private void synchronizePigBiData(DoctorPigDailyExtend dailyExtend, DoctorDimensionCriteria dimensionCriteria) {
        log.debug("dimension:orgId{}, farmId:{}, dimensionCriteria:{}", dailyExtend.getOrgId(), dailyExtend.getFarmId(), dimensionCriteria);
        if (Objects.equals(dimensionCriteria.getOrzType(), OrzDimension.ORG.getValue())) {
            dimensionCriteria.setOrzId(dailyExtend.getOrgId());
        } else if(Objects.equals(dimensionCriteria.getOrzType(), OrzDimension.FARM.getValue())){
            dimensionCriteria.setOrzId(dailyExtend.getFarmId());
        } else if(Objects.equals(dimensionCriteria.getOrzType(), OrzDimension.CLIQUE.getValue())){
            dimensionCriteria.setOrzId(dailyExtend.getGroupId());
        }
        dimensionCriteria.setSumAt(dailyExtend.getSumAt());
//        if (!Objects.equals(dimensionCriteria.getDateType(), DateDimension.DAY.getValue())
//                || !Objects.equals(dimensionCriteria.getOrzType(), OrzDimension.FARM.getValue())) {
            DoctorPigDailyExtend start;
            DoctorPigDailyExtend end;
            if (Objects.equals(dimensionCriteria.getOrzType(), OrzDimension.FARM.getValue())) {
                start = doctorPigDailyDao.farmStart(dimensionCriteria);
                end = doctorPigDailyDao.farmEnd(dimensionCriteria);
            } else if(Objects.equals(dimensionCriteria.getOrzType(), OrzDimension.ORG.getValue())){
                Date minDate = doctorPigDailyDao.minDate(dimensionCriteria);
                Date maxDate = doctorPigDailyDao.maxDate(dimensionCriteria);
                start = doctorPigDailyDao.orgStart(dimensionCriteria.getOrzId(), minDate);
                end = doctorPigDailyDao.orgEnd(dimensionCriteria.getOrzId(), maxDate);
                DoctorPigDailyExtend dayAvgLiveStock = doctorPigDailyDao.orgSumDimension(dimensionCriteria);
                int count = DateUtil.getDeltaDaysAbs(maxDate, minDate);
                dailyExtend.setBoarDailyPigCount(FieldHelper.getInteger(dayAvgLiveStock.getBoarDailyPigCount(), count));
                dailyExtend.setSowDailyPigCount(FieldHelper.getInteger(dayAvgLiveStock.getSowDailyPigCount(), count));
            }else{//(?????????)
                Date minDate = doctorPigDailyDao.minDate(dimensionCriteria);
                Date maxDate = doctorPigDailyDao.maxDate(dimensionCriteria);
                start = doctorPigDailyDao.groupStart(dimensionCriteria.getOrzId(), minDate);
                end = doctorPigDailyDao.groupEnd(dimensionCriteria.getOrzId(), maxDate);
            }
            dailyExtend.setSowCfStart(start.getSowCfStart());
            dailyExtend.setSowPhStart(start.getSowPhStart());
            dailyExtend.setSowStart(start.getSowStart());
            dailyExtend.setBoarStart(start.getBoarStart());
            dailyExtend.setSowCfEnd(end.getSowCfEnd());
            dailyExtend.setSowPhEnd(end.getSowPhEnd());
            dailyExtend.setSowEnd(end.getSowEnd());
            dailyExtend.setBoarEnd(end.getBoarEnd());
            dailyExtend.setSowPhMating(end.getSowPhMating());
            dailyExtend.setSowPhKonghuai(end.getSowPhKonghuai());
            dailyExtend.setSowPhPregnant(end.getSowPhPregnant());
//        }
        boarSynchronizer.synchronize(dailyExtend, dimensionCriteria);
        sowSynchronizer.synchronize(dailyExtend, dimensionCriteria);
        matingSynchronizer.synchronize(dailyExtend, dimensionCriteria);
        deliverSynchronizer.synchronize(dailyExtend, dimensionCriteria);
    }
}
