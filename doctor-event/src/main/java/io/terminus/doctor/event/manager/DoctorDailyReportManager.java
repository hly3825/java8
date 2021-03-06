package io.terminus.doctor.event.manager;

import com.google.common.base.Throwables;
import io.terminus.doctor.common.utils.DateUtil;
import io.terminus.doctor.event.cache.DoctorDailyReportCache;
import io.terminus.doctor.event.dao.DoctorDailyGroupDao;
import io.terminus.doctor.event.dao.DoctorDailyReportDao;
import io.terminus.doctor.event.dao.DoctorGroupDao;
import io.terminus.doctor.event.dao.DoctorKpiDao;
import io.terminus.doctor.event.dto.report.common.DoctorLiveStockChangeCommonReport;
import io.terminus.doctor.event.dto.report.daily.DoctorDailyReportDto;
import io.terminus.doctor.event.model.DoctorDailyGroup;
import io.terminus.doctor.event.model.DoctorDailyReport;
import io.terminus.doctor.event.model.DoctorGroup;
import io.terminus.doctor.event.util.EventUtil;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.Objects;

import static io.terminus.common.utils.Arguments.isNull;
import static io.terminus.doctor.common.utils.Checks.expectTrue;

/**
 * Desc:
 * Mail: yangzl@terminus.io
 * author: DreamYoung
 * Date: 16/7/20
 */
@Slf4j
@Component
public class DoctorDailyReportManager {

    private final DoctorDailyReportDao doctorDailyReportDao;
    private final DoctorDailyReportCache doctorDailyReportCache;
    private final DoctorKpiDao doctorKpiDao;
    private final DoctorDailyGroupDao doctorDailyGroupDao;
    @Autowired
    private DoctorGroupDao doctorGroupDao;

    @Autowired
    public DoctorDailyReportManager(DoctorDailyReportDao doctorDailyReportDao,
                                    DoctorDailyReportCache doctorDailyReportCache,
                                    DoctorKpiDao doctorKpiDao,
                                    DoctorDailyGroupDao doctorDailyGroupDao) {
        this.doctorDailyReportDao = doctorDailyReportDao;
        this.doctorDailyReportCache = doctorDailyReportCache;
        this.doctorKpiDao = doctorKpiDao;
        this.doctorDailyGroupDao = doctorDailyGroupDao;
    }

    /**
     * ?????????????????????, ?????????
     *
     * @param farmId ??????id
     * @param sumAt  ????????????
     */
    @Transactional
    public void realTimeDailyReports(Long farmId, Date sumAt) {
        DoctorDailyReport report = new DoctorDailyReport();
        report.setFarmId(farmId);
        report.setSumAt(DateUtil.toDateString(sumAt));

        DoctorDailyReportDto dto = doctorDailyReportCache.initDailyReportByFarmIdAndDate(farmId, sumAt);
//        report.setSowCount(dto.getSowCount());                      //???????????????
//        report.setFarrowCount(dto.getLiveStock().getFarrow());      //????????????
//        report.setNurseryCount(dto.getLiveStock().getNursery());    //?????????
//        report.setFattenCount(dto.getLiveStock().getFatten());      //?????????
//        report.setHoubeiCount(dto.getLiveStock().getHoubei());      //?????????
//        report.setReportData(dto);
//        doctorDailyReportDao.deleteByFarmIdAndSumAt(farmId, sumAt);
//        doctorDailyReportDao.create(report);
    }

    /**
     * ???????????????????????????, ??????????????????
     *
     * @param groupId ??????id
     * @param sumAt   ????????????
     * @return ????????????
     */
    public DoctorDailyGroup findByGroupIdAndSumAt(Long groupId, Date sumAt) {
        DoctorGroup group = doctorGroupDao.findById(groupId);
        DoctorDailyGroup doctorDailyGroup = doctorDailyGroupDao.findByGroupIdAndSumAt(groupId, sumAt);
        if (isNull(doctorDailyGroup)) {
            doctorDailyGroup = new DoctorDailyGroup();
            doctorDailyGroup.setStart(doctorKpiDao.realTimeLivetockGroup(groupId, new DateTime(sumAt).minusDays(1).toDate()));
            doctorDailyGroup.setEnd(doctorDailyGroup.getStart());
            doctorDailyGroup.setSumAt(sumAt);
            doctorDailyGroup.setGroupId(groupId);
            doctorDailyGroup.setFarmId(group.getFarmId());
            doctorDailyGroup.setType(group.getPigType());
        }
        return doctorDailyGroup;
    }

    /**
     * ????????????,????????????
     *
     * @param dailyGroup ???????????????
     */
    public void createOrUpdateDailyGroup(DoctorDailyGroup dailyGroup) {
        if (isNull(dailyGroup.getId())) {
            doctorDailyGroupDao.create(dailyGroup);
        } else {
            expectTrue(doctorDailyGroupDao.update(dailyGroup), "concurrent.error");
        }
    }

    /**
     * ????????????,????????????
     *
     * @param dailyPig ????????????
     */
    public void createOrUpdateDailyPig(DoctorDailyReport dailyPig) {
        if (isNull(dailyPig.getId())) {
            doctorDailyReportDao.create(dailyPig);
        } else {
            expectTrue(doctorDailyReportDao.update(dailyPig), "concurrent.error");
        }
    }

    /**
     * ?????????????????????
     *
     * @param farmId ??????id
     * @param date   ??????
     * @return
     */
    public Boolean createOrUpdateReport(Long farmId, Date date) {
        DoctorDailyReport newReport = null;
        try {
            newReport = getdoctorDailyReport(farmId, date, DateUtil.getDateEnd(new DateTime(date)).toDate());
            newReport = caculateIndicator(newReport);
            DoctorDailyReport oldDayReport = doctorDailyReportDao.findByFarmIdAndSumAt(farmId, date);
            if (!Objects.isNull(oldDayReport)) {
                newReport.setId(oldDayReport.getId());
                doctorDailyReportDao.update(newReport);
                return Boolean.TRUE;
            }
            doctorDailyReportDao.create(newReport);
        } catch (Exception e) {
            log.info("create or update daily report failed, report: {}, cause: {}", newReport, Throwables.getStackTraceAsString(e));
            return Boolean.FALSE;
        }
        return Boolean.TRUE;
    }

    private DoctorDailyReport getdoctorDailyReport(Long farmId, Date startAt, Date endAt) {
        DoctorDailyReport doctorDailyReport = new DoctorDailyReport();
        doctorDailyReport.setFarmId(farmId);
        doctorDailyReport.setSumAt(DateUtil.toDateString(startAt));

        //????????????id????????????id
        Long orgId = doctorKpiDao.getOrgIdByFarmId(farmId);
        //????????????????????????
        doctorDailyReport.setSowCfStart(doctorKpiDao.realTimeLiveStockFarrowSow(orgId,farmId, new DateTime(startAt).minusDays(1).toDate()));
        doctorDailyReport.setSowCfIn(doctorKpiDao.getMonthlyLiveStockChangeToFarrow(farmId, startAt, endAt));
        doctorDailyReport.setSowCfDead(doctorKpiDao.getSowCfDead(farmId, startAt, endAt));
        doctorDailyReport.setSowCfWeedOut(doctorKpiDao.getSowCfWeedOut(farmId, startAt, endAt));
        doctorDailyReport.setSowCfSale(doctorKpiDao.getSowCfSale(farmId, startAt, endAt));
        doctorDailyReport.setSowCfOtherOut(doctorKpiDao.getSowCfOtherOut(farmId, startAt, endAt));
        doctorDailyReport.setSowCfWeanOut(doctorKpiDao.getMonthlyLiveStockChangeWeanIn(farmId, startAt, endAt));
        doctorDailyReport.setSowCfChgFarm(doctorKpiDao.getSowCfChgFarm(farmId, startAt, endAt));
        doctorDailyReport.setSowCfEnd(doctorKpiDao.realTimeLiveStockFarrowSow(orgId,farmId, startAt));
        doctorDailyReport.setSowCfInFarmIn(doctorKpiDao.getSowCfInFarmIn(farmId, startAt, endAt));
        //????????????????????????
        doctorDailyReport.setSowPhStart(doctorKpiDao.realTimeLiveStockPHSow(orgId,farmId, new DateTime(startAt).minusDays(1).toDate()));
//        doctorDailyReport.setSowPhStart(EventUtil.minusInt(doctorDailyReport.getSowStart(), doctorDailyReport.getSowCfStart()));
        doctorDailyReport.setSowPhReserveIn(doctorKpiDao.getSowPhReserveIn(farmId, startAt, endAt));
        doctorDailyReport.setSowPhChgFarmIn(doctorKpiDao.getSowPhChgFarmIn(farmId, startAt, endAt));
        doctorDailyReport.setSowPhWeanIn(doctorDailyReport.getSowCfWeanOut());
        doctorDailyReport.setSowPhToCf(doctorKpiDao.getMonthlyLiveStockChangeToFarrow(farmId, startAt, endAt));
        doctorDailyReport.setSowPhInFarmIn(doctorKpiDao.getSowPhInFarm(farmId, startAt, endAt));
        doctorDailyReport.setSowPhDead(doctorKpiDao.getSowPhDead(farmId, startAt, endAt));
        doctorDailyReport.setSowPhWeedOut(doctorKpiDao.getSowPhWeedOut(farmId, startAt, endAt));
        doctorDailyReport.setSowPhSale(EventUtil.minusInt(doctorDailyReport.getSowSale(), doctorDailyReport.getSowCfSale()));
        doctorDailyReport.setSowPhOtherOut(EventUtil.minusInt(doctorDailyReport.getSowOtherOut(), doctorDailyReport.getSowCfOtherOut()));
        doctorDailyReport.setSowPhChgFarm(EventUtil.minusInt(doctorDailyReport.getSowChgFarm(), doctorDailyReport.getSowCfChgFarm()));
        doctorDailyReport.setSowPhMating(doctorKpiDao.getSowMatingCount(orgId,farmId, endAt));
        doctorDailyReport.setSowPhKonghuai(doctorKpiDao.getSowKonghuaiCount(orgId,farmId, endAt));
        doctorDailyReport.setSowPhPregnant(doctorKpiDao.getSowPregnantCount(orgId,farmId, endAt));
        doctorDailyReport.setSowPhEnd(doctorKpiDao.realTimeLiveStockPHSow(orgId,farmId, startAt));
//        doctorDailyReport.setSowPhEnd(doctorDailyReport.getSowPhEnd());

        doctorDailyReport.setSowPh(doctorDailyReport.getSowPhEnd());
        doctorDailyReport.setSowCf(doctorDailyReport.getSowCfEnd());

        //??????????????????
        doctorDailyReport.setSowStart(EventUtil.plusInt(doctorDailyReport.getSowCfStart(),doctorDailyReport.getSowPhStart()));
        doctorDailyReport.setSowIn(doctorKpiDao.getInSow(farmId, startAt, endAt));
        doctorDailyReport.setSowDead(doctorKpiDao.getDeadSow(farmId, startAt, endAt));
        doctorDailyReport.setSowWeedOut(doctorKpiDao.getWeedOutSow(farmId, startAt, endAt));
        doctorDailyReport.setSowSale(doctorKpiDao.getSaleSow(farmId, startAt, endAt));
        doctorDailyReport.setSowOtherOut(doctorKpiDao.getOtherOutSow(farmId, startAt, endAt));
        doctorDailyReport.setSowChgFarm(doctorKpiDao.getPigChgFarm(farmId, 1, startAt, endAt));
        doctorDailyReport.setSowEnd(EventUtil.plusInt(doctorDailyReport.getSowCfEnd(),doctorDailyReport.getSowPhEnd()));

        //??????????????????
        doctorDailyReport.setBoarStart(doctorKpiDao.realTimeLiveStockBoar(farmId, new DateTime(startAt).minusDays(1).toDate()));
        doctorDailyReport.setBoarIn(doctorKpiDao.getInBoar(farmId, startAt, endAt));
        doctorDailyReport.setBoarDead(doctorKpiDao.getDeadBoar(farmId, startAt, endAt));
        doctorDailyReport.setBoarWeedOut(doctorKpiDao.getWeedOutBoar(farmId, startAt, endAt));
        doctorDailyReport.setBoarSale(doctorKpiDao.getSaleBoar(farmId, startAt, endAt));
        doctorDailyReport.setBoarOtherOut(doctorKpiDao.getOtherOutBoar(farmId, startAt, endAt));
        doctorDailyReport.setBoarChgFarm(doctorKpiDao.getPigChgFarm(farmId, 2, startAt, endAt));
        doctorDailyReport.setBoarEnd(doctorKpiDao.realTimeLiveStockBoar(farmId, startAt));
        //????????????
        doctorDailyReport.setMateHb(doctorKpiDao.firstMatingCounts(farmId, startAt, endAt));
        doctorDailyReport.setMateDn(doctorKpiDao.weanMatingCounts(farmId, startAt, endAt));
        doctorDailyReport.setMateFq(doctorKpiDao.fanQMatingCounts(farmId, startAt, endAt));
        doctorDailyReport.setMateLc(doctorKpiDao.abortionMatingCounts(farmId, startAt, endAt));
        doctorDailyReport.setMateYx(doctorKpiDao.yinMatingCounts(farmId, startAt, endAt));
        //??????????????????
        doctorDailyReport.setPregPositive(doctorKpiDao.checkYangCounts(farmId, startAt, endAt));
        doctorDailyReport.setPregNegative(doctorKpiDao.checkYingCounts(farmId, startAt, endAt));
        doctorDailyReport.setPregFanqing(doctorKpiDao.checkFanQCounts(farmId, startAt, endAt));
        doctorDailyReport.setPregLiuchan(doctorKpiDao.checkAbortionCounts(farmId, startAt, endAt));
        //????????????
        doctorDailyReport.setFarrowNest(doctorKpiDao.getDelivery(farmId, startAt, endAt));
        doctorDailyReport.setFarrowAll(doctorKpiDao.getDeliveryAll(farmId, startAt, endAt));
        doctorDailyReport.setFarrowLive(doctorKpiDao.getDeliveryLive(farmId, startAt, endAt));
        doctorDailyReport.setFarrowHealth(doctorKpiDao.getDeliveryHealth(farmId, startAt, endAt));
        doctorDailyReport.setFarrowWeak(doctorKpiDao.getDeliveryWeak(farmId, startAt, endAt));
        doctorDailyReport.setFarrowDead(doctorKpiDao.getDeliveryDead(farmId, startAt, endAt));
        doctorDailyReport.setFarrowJx(doctorKpiDao.getDeliveryJx(farmId, startAt, endAt));
        doctorDailyReport.setFarrowMny(doctorKpiDao.getDeliveryMny(farmId, startAt, endAt));
        doctorDailyReport.setFarrowBlack(doctorKpiDao.getDeliveryBlack(farmId, startAt, endAt));
        doctorDailyReport.setFarrowSjmh(doctorKpiDao.getDeliveryDeadBlackMuJi(farmId, startAt, endAt));
        doctorDailyReport.setFarrowWeight(doctorKpiDao.getFarrowWeight(farmId, startAt, endAt));
        doctorDailyReport.setFarrowAvgWeight(doctorKpiDao.getFarrowWeightAvg(farmId, startAt, endAt));
        //????????????
        doctorDailyReport.setWeanNest(doctorKpiDao.getWeanSow(farmId, startAt, endAt));
        doctorDailyReport.setWeanCount(doctorKpiDao.getWeanPiglet(farmId, startAt, endAt));
        doctorDailyReport.setWeanAvgWeight(doctorKpiDao.getWeanPigletWeightAvg(farmId, startAt, endAt));
        doctorDailyReport.setWeanDayAge(doctorKpiDao.getWeanDayAgeAvg(farmId, startAt, endAt));
        return doctorDailyReport;
    }

    private DoctorDailyReport caculateIndicator(DoctorDailyReport doctorDailyReport) {
        Long farmId = doctorDailyReport.getFarmId();
        Date startAt = DateUtil.toDate(doctorDailyReport.getSumAt());
        Date endAt = DateUtil.getDateEnd(new DateTime(startAt)).toDate();

//        DoctorLiveStockChangeCommonReport changeCountReport = doctorKpiDao.getMonthlyLiveStockChangeFeedCount(farmId, startAt, endAt);
//        DoctorLiveStockChangeCommonReport changeAmountReport = doctorKpiDao.getMonthlyLiveStockChangeMaterielAmount(farmId, startAt, endAt);

        //????????????????????????
//        DoctorLiveStockChangeCommonReport changeCountReport = doctorKpiDao.getMonthlyLiveStockChangeFeedCountV2(farmId, startAt, endAt);
        //????????????????????????
//        DoctorLiveStockChangeCommonReport changeAmountReport = doctorKpiDao.getMonthlyLiveStockChangeMaterialAmountV2(farmId, startAt, endAt);

        DoctorLiveStockChangeCommonReport changeCountAndAmountReport = doctorKpiDao.getMonthlyLiveStockChangeQuantityAndAmount(farmId, startAt, endAt);

        //?????????????????????????????????????????????
        DoctorLiveStockChangeCommonReport farrowSowChange = doctorKpiDao.getFarrowSowApplyCount(farmId, startAt, endAt);
        DoctorLiveStockChangeCommonReport farrowChange = doctorKpiDao.getFarrowApplyCount(farmId, startAt, endAt);


        doctorDailyReport.setFattenPrice(doctorKpiDao.getGroupSaleFattenPrice(farmId, startAt, endAt));
        doctorDailyReport.setBasePrice10(doctorKpiDao.getGroupSaleBasePrice10(farmId, startAt, endAt));
        doctorDailyReport.setBasePrice15(doctorKpiDao.getGroupSaleBasePrice15(farmId, startAt, endAt));

        doctorDailyReport.setSowPhFeed(changeCountAndAmountReport.getPeiHuaiFeedCount());
        doctorDailyReport.setSowPhFeedAmount(changeCountAndAmountReport.getPeiHuaiFeedAmount());
        doctorDailyReport.setSowPhMedicineAmount(changeCountAndAmountReport.getPeiHuaiDrugAmount());
        doctorDailyReport.setSowPhVaccinationAmount(changeCountAndAmountReport.getPeiHuaiVaccineAmount());
        doctorDailyReport.setSowPhConsumableAmount(changeCountAndAmountReport.getPeiHuaiConsumerAmount());

        //????????????
        doctorDailyReport.setSowCfFeed(farrowSowChange.getFarrowSowFeedCount());
        doctorDailyReport.setSowCfFeedAmount(farrowSowChange.getFarrowSowFeedAmount());
        doctorDailyReport.setSowCfMedicineAmount(farrowSowChange.getFarrowSowDrugAmount());
        doctorDailyReport.setSowCfVaccinationAmount(farrowSowChange.getFarrowSowVaccineAmount());
        doctorDailyReport.setSowCfConsumableAmount(farrowSowChange.getFarrowSowConsumerAmount());

        //????????????
        doctorDailyReport.setFarrowFeed(farrowChange.getFarrowFeedCount());
        doctorDailyReport.setFarrowFeedAmount(farrowChange.getFarrowFeedAmount());
        doctorDailyReport.setFarrowMedicineAmount(farrowChange.getFarrowDrugAmount());
        doctorDailyReport.setFarrowVaccinationAmount(farrowChange.getFarrowVaccineAmount());
        doctorDailyReport.setFarrowConsumableAmount(farrowChange.getFarrowConsumerAmount());

        doctorDailyReport.setNurseryFeed(changeCountAndAmountReport.getNurseryFeedCount());
        doctorDailyReport.setNurseryFeedAmount(changeCountAndAmountReport.getNurseryFeedAmount());
        doctorDailyReport.setNurseryMedicineAmount(changeCountAndAmountReport.getNurseryDrugAmount());
        doctorDailyReport.setNurseryVaccinationAmount(changeCountAndAmountReport.getNurseryVaccineAmount());
        doctorDailyReport.setNurseryConsumableAmount(changeCountAndAmountReport.getNurseryConsumerAmount());

        doctorDailyReport.setFattenFeed(changeCountAndAmountReport.getFattenFeedCount());
        doctorDailyReport.setFattenFeedAmount(changeCountAndAmountReport.getFattenFeedAmount());
        doctorDailyReport.setFattenMedicineAmount(changeCountAndAmountReport.getFattenDrugAmount());
        doctorDailyReport.setFattenVaccinationAmount(changeCountAndAmountReport.getFattenVaccineAmount());
        doctorDailyReport.setFattenConsumableAmount(changeCountAndAmountReport.getFattenConsumerAmount());

        doctorDailyReport.setHoubeiFeed(changeCountAndAmountReport.getHoubeiFeedCount());
        doctorDailyReport.setHoubeiFeedAmount(changeCountAndAmountReport.getHoubeiFeedAmount());
        doctorDailyReport.setHoubeiMedicineAmount(changeCountAndAmountReport.getHoubeiDrugAmount());
        doctorDailyReport.setHoubeiVaccinationAmount(changeCountAndAmountReport.getHoubeiVaccineAmount());
        doctorDailyReport.setHoubeiConsumableAmount(changeCountAndAmountReport.getHoubeiConsumerAmount());

        return doctorDailyReport;
    }
}
