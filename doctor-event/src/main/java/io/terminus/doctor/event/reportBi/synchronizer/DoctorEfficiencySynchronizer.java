package io.terminus.doctor.event.reportBi.synchronizer;

import com.sun.org.apache.regexp.internal.RE;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.doctor.common.utils.DateUtil;
import io.terminus.doctor.common.utils.RespHelper;
import io.terminus.doctor.event.dao.DoctorPigDailyDao;
import io.terminus.doctor.event.dao.DoctorReportNpdDao;
import io.terminus.doctor.event.dao.reportBi.DoctorReportEfficiencyDao;
import io.terminus.doctor.event.dto.DoctorDimensionCriteria;
import io.terminus.doctor.event.enums.DateDimension;
import io.terminus.doctor.event.enums.OrzDimension;
import io.terminus.doctor.event.enums.ReportTime;
import io.terminus.doctor.event.model.DoctorPigDaily;
import io.terminus.doctor.event.model.DoctorReportEfficiency;
import io.terminus.doctor.event.model.DoctorReportNpd;
import io.terminus.doctor.event.reportBi.helper.DateHelper;
import io.terminus.doctor.event.service.DoctorPigReportReadService;
import io.terminus.doctor.event.service.DoctorReportReadService;
import io.terminus.doctor.event.service.DoctorReportWriteService;
import io.terminus.doctor.user.model.DoctorFarm;
import io.terminus.doctor.user.model.DoctorOrg;
import io.terminus.doctor.user.service.DoctorFarmReadService;
import io.terminus.doctor.user.service.DoctorOrgReadService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.ListUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.validation.constraints.Min;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by sunbo@terminus.io on 2018/1/15.
 */
@Slf4j
@Component
public class DoctorEfficiencySynchronizer {

    @Autowired
    private DoctorReportNpdDao doctorReportNpdDao;
    @Autowired
    private DoctorPigDailyDao doctorPigDailyDao;
    @Autowired
    private DoctorReportEfficiencyDao doctorReportEfficiencyDao;
    @RpcConsumer
    private DoctorFarmReadService doctorFarmReadService;
    @RpcConsumer
    private DoctorOrgReadService doctorOrgReadService;
    @RpcConsumer
    private DoctorPigReportReadService doctorPigReportReadService;

    public void deleteAll() {
        doctorReportEfficiencyDao.delete();
    }


    public void delete(DoctorDimensionCriteria criteria) {
        doctorReportEfficiencyDao.delete(criteria);
    }

    /**
     * ???doctor_report_npd????????????doctor_report_efficiency
     * doctor_report_npd???????????????????????????????????????????????????????????????
     */
    public void sync(DoctorDimensionCriteria dimensionCriteria) {

        if (dimensionCriteria.getDateType().equals(DateDimension.DAY.getValue())
                || dimensionCriteria.getDateType().equals(DateDimension.WEEK.getValue())) {
            log.warn("????????????????????????????????????????????????BI");
            return;
        }
        if (dimensionCriteria.getOrzType().equals(OrzDimension.CLIQUE.getValue())) {
            log.warn("??????????????????");
            return;
        }

        log.info("start sync from npd to efficiency:{},{}", DateDimension.from(dimensionCriteria.getDateType()).getName(),
                OrzDimension.from(dimensionCriteria.getOrzType()).getName());
        List<DoctorReportNpd> npds = doctorReportNpdDao.count(dimensionCriteria, null, null);
        for (DoctorReportNpd npd : npds) {


            DoctorReportEfficiency efficiency = new DoctorReportEfficiency();
            if (dimensionCriteria.getOrzType().equals(OrzDimension.FARM.getValue()))
                efficiency.setOrzId(npd.getFarmId());
            else
                efficiency.setOrzId(npd.getOrgId());
            efficiency.setOrzType(dimensionCriteria.getOrzType());
            efficiency.setDateType(dimensionCriteria.getDateType());
            efficiency.setSumAtName(DateHelper.dateCN(npd.getSumAt(), DateDimension.from(dimensionCriteria.getDateType())));

            if (dimensionCriteria.getOrzType().equals(OrzDimension.FARM.getValue())) {
                DoctorFarm farm = RespHelper.orServEx(doctorFarmReadService.findFarmById(npd.getFarmId()));
                efficiency.setOrzName(farm == null ? "" : farm.getName());
            } else if (dimensionCriteria.getOrzType().equals(OrzDimension.ORG.getValue())) {
                DoctorOrg org = RespHelper.orServEx(doctorOrgReadService.findOrgById(npd.getOrgId()));
                efficiency.setOrzName(org == null ? "" : org.getName());
            }

            Date end;
            if (dimensionCriteria.getDateType().equals(DateDimension.MONTH.getValue()))
                end = DateUtil.monthEnd(npd.getSumAt());
            else if (dimensionCriteria.getDateType().equals(DateDimension.QUARTER.getValue())) {
                end = DateHelper.withDateEndDay(npd.getSumAt(), DateDimension.QUARTER);
            } else {
                end = DateHelper.withDateEndDay(npd.getSumAt(), DateDimension.YEAR);
            }

            DoctorPigDaily pigDaily;
            if (dimensionCriteria.getOrzType().equals(OrzDimension.ORG.getValue()))
                pigDaily = doctorPigDailyDao.countByFarm(npd.getOrgId(), npd.getSumAt(), end);
            else
                pigDaily = doctorPigDailyDao.countByFarm(npd.getFarmId(), npd.getSumAt(), end);


            efficiency.setSumAt(npd.getSumAt());

            calc(pigDaily, npd, npd.getSumAt(), end, efficiency, dimensionCriteria.getDateType());

            doctorReportEfficiencyDao.create(efficiency);
        }

        log.debug("finish sync from npd to efficiency:{},{}", DateDimension.from(dimensionCriteria.getDateType()).getName(),
                OrzDimension.from(dimensionCriteria.getOrzType()).getName());
    }

    /**
     * ??????????????????????????????????????????????????????
     * ????????????????????????????????????
     *
     * @param farmId
     * @param date
     */
    public void syncFarm(Long farmId, Date date) {

    }

    /**
     * ???????????????????????????????????????????????????
     *
     * @param orgId
     * @param date
     */
    public void syncOrg(Long orgId, Date date) {

    }

    /**
     * ???????????????????????????????????????????????????????????????
     *
     * @param date
     */
    public void sync(Date date) {
        //
        Date start = DateHelper.withDateStartDay(date, DateDimension.MONTH);
        Date end = DateHelper.withDateEndDay(date, DateDimension.MONTH);
        List<DoctorReportNpd> npds = doctorReportNpdDao.findBySumAt(start);
        create(npds, DateDimension.MONTH, start, end);

        DoctorDimensionCriteria criteria = new DoctorDimensionCriteria();
        criteria.setOrzType(OrzDimension.FARM.getValue());
        criteria.setDateType(DateDimension.QUARTER.getValue());

        DoctorPigReportReadService.DateDuration dateDuration = doctorPigReportReadService.getDuration(date, ReportTime.SEASON);
        start = dateDuration.getStart();
        end = dateDuration.getEnd();

        npds = doctorReportNpdDao.count(criteria, start, end);
        create(npds, DateDimension.QUARTER, start, end);

        criteria = new DoctorDimensionCriteria();
        criteria.setOrzType(OrzDimension.FARM.getValue());
        criteria.setDateType(DateDimension.YEAR.getValue());

        dateDuration = doctorPigReportReadService.getDuration(date, ReportTime.YEAR);
        start = dateDuration.getStart();
        end = dateDuration.getEnd();

        npds = doctorReportNpdDao.count(criteria, start, end);
        create(npds, DateDimension.YEAR, start, end);
    }

    private void create(List<DoctorReportNpd> npds, DateDimension dateDimension, Date start, Date end) {

        Set<Long> orgIds = new HashSet<>();

        //??????Farm
        Map<Long, List<DoctorFarm>> farmMap = RespHelper.orServEx(doctorFarmReadService.findFarmsByIds(npds.stream().map(DoctorReportNpd::getFarmId).collect(Collectors.toList())))
                .stream().collect(Collectors.groupingBy(DoctorFarm::getId));


        //???????????????????????????Map
        Map<Long, List<DoctorReportNpd>> npdMap = npds.stream().collect(Collectors.groupingBy(DoctorReportNpd::getFarmId));
//        RespHelper.orServEx(doctorFarmReadService.findAllFarms()).stream().forEach(f -> {
        npdMap.forEach((f, v) -> {
            DoctorReportEfficiency efficiency;
            DoctorDimensionCriteria criteria = new DoctorDimensionCriteria();
            criteria.setOrzId(f);
            criteria.setDateType(dateDimension.getValue());
            criteria.setOrzType(OrzDimension.FARM.getValue());
            criteria.setSumAt(start);
            efficiency = doctorReportEfficiencyDao.findByDimension(criteria);
            if (null == efficiency)
                efficiency = new DoctorReportEfficiency();
            if (!npdMap.containsKey(f)) {
                //???
                efficiency.setOrzId(f);
                efficiency.setOrzType(OrzDimension.FARM.getValue());
                efficiency.setDateType(dateDimension.getValue());
                efficiency.setOrzName(farmMap.containsKey(f) ? farmMap.get(f).get(0).getName() : "");
                efficiency.setSumAt(start);
                efficiency.setSumAtName(DateHelper.dateCN(start, dateDimension));

                //??????????????????????????????0
                efficiency.setPregnancy(new BigDecimal(0));
                efficiency.setLactation(new BigDecimal(0));
                if (null == efficiency.getId())
                    doctorReportEfficiencyDao.create(efficiency);
                else
                    doctorReportEfficiencyDao.update(efficiency);
            } else {
                DoctorReportNpd npd = npdMap.get(f).get(0);//??????????????????????????????????????????????????????
                //??????farmId????????????????????????
                DoctorPigDaily pigDaily = doctorPigDailyDao.countByFarm(npd.getFarmId(), start, end);

                efficiency.setOrzId(npd.getFarmId());
                efficiency.setOrzType(OrzDimension.FARM.getValue());
                efficiency.setDateType(dateDimension.getValue());
                efficiency.setOrzName(farmMap.containsKey(f) ? farmMap.get(f).get(0).getName() : "");
                efficiency.setSumAtName(DateHelper.dateCN(npd.getSumAt(), dateDimension));

                efficiency.setSumAt(npd.getSumAt());

                calc(pigDaily, npd, start, end, efficiency, dateDimension.getValue());

                if (efficiency.getId() == null)
                    doctorReportEfficiencyDao.create(efficiency);
                else
                    doctorReportEfficiencyDao.update(efficiency);
                orgIds.add(npd.getOrgId());
            }
        });

//        RespHelper.orServEx(doctorOrgReadService.findAllOrgs()).stream().forEach(o -> {

        Map<Long, List<DoctorOrg>> orgMap = RespHelper.orServEx(doctorOrgReadService.findOrgByIds(orgIds.stream().collect(Collectors.toList())))
                .stream().collect(Collectors.groupingBy(DoctorOrg::getId));
        orgIds.forEach(o -> {
            DoctorReportEfficiency efficiency;
            DoctorDimensionCriteria criteria = new DoctorDimensionCriteria();
            criteria.setOrzId(o);
            criteria.setDateType(dateDimension.getValue());
            criteria.setOrzType(OrzDimension.ORG.getValue());
            criteria.setSumAt(start);
            efficiency = doctorReportEfficiencyDao.findByDimension(criteria);
            if (null == efficiency) efficiency = new DoctorReportEfficiency();

            if (!orgIds.contains(o)) {
                //???
                efficiency.setOrzId(o);
                efficiency.setOrzType(OrzDimension.ORG.getValue());
                efficiency.setDateType(dateDimension.getValue());
                efficiency.setOrzName(orgMap.containsKey(o) ? orgMap.get(o).get(0).getName() : "");
                efficiency.setSumAt(start);
                efficiency.setSumAtName(DateHelper.dateCN(start, dateDimension));

                efficiency.setPregnancy(new BigDecimal(0));
                efficiency.setLactation(new BigDecimal(0));
                if (null == efficiency.getId())
                    doctorReportEfficiencyDao.create(efficiency);
                else
                    doctorReportEfficiencyDao.update(efficiency);
            } else {
                DoctorReportNpd npd = doctorReportNpdDao.findByOrgAndSumAt(o, start);

                DoctorPigDaily pigDaily = doctorPigDailyDao.countByOrg(o, start, end);

                efficiency.setOrzId(o);
                efficiency.setOrzType(OrzDimension.ORG.getValue());
                efficiency.setDateType(dateDimension.getValue());
                efficiency.setSumAtName(DateHelper.dateCN(start, dateDimension));
//                DoctorOrg org = RespHelper.orServEx(doctorOrgReadService.findOrgById(orgId));
                efficiency.setOrzName(orgMap.containsKey(o) ? orgMap.get(o).get(0).getName() : "");

                efficiency.setSumAt(start);

                calc(pigDaily, npd, start, end, efficiency, dateDimension.getValue());

                if (efficiency.getId() == null)
                    doctorReportEfficiencyDao.create(efficiency);
                else
                    doctorReportEfficiencyDao.update(efficiency);
            }
        });
    }


    private void calc(/*???????????????*/DoctorPigDaily pigDaily, /*??????????????????*/DoctorReportNpd npd, Date start, Date end, /*?????????*/DoctorReportEfficiency efficiency, int dateType) {

        //???????????????????????????
        int dayCount = DateUtil.getDeltaDays(start, end) + 1;

        //???????????????????????????=????????????/??????
        BigDecimal sowAvg = npd.getSowCount() == 0 ? new BigDecimal(0) :
                new BigDecimal(npd.getSowCount()).divide(new BigDecimal(dayCount), 2, BigDecimal.ROUND_HALF_UP);

        //???????????????=??????????????????/???????????????????????????
        if (sowAvg.compareTo(new BigDecimal(0)) != 0) {
            efficiency.setNpd(new BigDecimal(npd.getNpd()).divide(sowAvg, 2, BigDecimal.ROUND_HALF_UP));
        }

        //?????????????????????=365-???????????????*12/????????????/?????????
        if (null != pigDaily && pigDaily.getFarrowNest() != null && pigDaily.getFarrowNest() != 0 && efficiency.getNpd() != null
                && (npd.getPregnancy() + npd.getLactation() != 0)) {
            //?????????+??????????????????????????????
            BigDecimal pd = new BigDecimal(npd.getPregnancy()).add(new BigDecimal(npd.getLactation()));
            //????????????????????????=????????????????????????
            BigDecimal eachBirthDay = pd.divide(new BigDecimal(pigDaily.getFarrowNest()), 2, BigDecimal.ROUND_HALF_UP);

            if (DateDimension.MONTH.getValue().equals(dateType)) {//?????????????????????=365-???????????????*12/????????????/?????????
                efficiency.setBirthPerYear(new BigDecimal(365).subtract(efficiency.getNpd().multiply(new BigDecimal(12))).divide(eachBirthDay, 2, BigDecimal.ROUND_HALF_UP));
            } else if (DateDimension.QUARTER.getValue().equals(dateType)) {
                efficiency.setBirthPerYear(new BigDecimal(365).subtract(efficiency.getNpd().multiply(new BigDecimal(4))).divide(eachBirthDay, 2, BigDecimal.ROUND_HALF_UP));
            } else {
                efficiency.setBirthPerYear(new BigDecimal(365).subtract(efficiency.getNpd()).divide(eachBirthDay, 2, BigDecimal.ROUND_HALF_UP));
            }
        }

        //psy=????????????*???????????????/????????????
        if (null != pigDaily && pigDaily.getWeanNest() != null && pigDaily.getWeanNest() != 0 && efficiency.getBirthPerYear() != null)
            efficiency.setPsy(efficiency.getBirthPerYear().multiply(new BigDecimal(pigDaily.getWeanCount()).divide(new BigDecimal(pigDaily.getWeanNest()), 2, BigDecimal.ROUND_HALF_UP)));

        if (sowAvg.compareTo(new BigDecimal(0)) == 0) {
            efficiency.setPregnancy(new BigDecimal(0));
            efficiency.setLactation(new BigDecimal(0));
        } else {
            efficiency.setPregnancy(new BigDecimal(npd.getPregnancy()).divide(sowAvg, 2, BigDecimal.ROUND_HALF_UP));
            efficiency.setLactation(new BigDecimal(npd.getLactation()).divide(sowAvg, 2, BigDecimal.ROUND_HALF_UP));
        }
    }
}
