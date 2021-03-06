package io.terminus.doctor.event.reportBi.synchronizer;

import io.terminus.doctor.common.enums.IsOrNot;
import io.terminus.doctor.event.dao.reportBi.DoctorReportSowDao;
import io.terminus.doctor.event.dto.DoctorDimensionCriteria;
import io.terminus.doctor.event.dto.reportBi.DoctorFiledUrlCriteria;
import io.terminus.doctor.event.dto.reportBi.DoctorPigDailyExtend;
import io.terminus.doctor.event.enums.DateDimension;
import io.terminus.doctor.event.enums.OrzDimension;
import io.terminus.doctor.event.model.DoctorReportSow;
import io.terminus.doctor.event.reportBi.helper.FieldHelper;
import io.terminus.doctor.event.util.EventUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Objects;

import static io.terminus.doctor.event.reportBi.helper.DateHelper.dateCN;
import static io.terminus.doctor.event.reportBi.helper.DateHelper.withDateStartDay;
import static java.util.Objects.isNull;

/**
 * Created by xjn on 18/1/13.
 * email:xiaojiannan@terminus.io
 */
@Component
public class DoctorSowSynchronizer {
    private final DoctorReportSowDao doctorReportSowDao;
    private final FieldHelper fieldHelper;

    @Autowired
    public DoctorSowSynchronizer(DoctorReportSowDao doctorReportSowDao, FieldHelper fieldHelper) {
        this.doctorReportSowDao = doctorReportSowDao;
        this.fieldHelper = fieldHelper;
    }

    public void synchronize(DoctorPigDailyExtend pigDaily,
                            DoctorDimensionCriteria dimensionCriteria){
        DoctorReportSow reportBI;
        if (isNull(dimensionCriteria.getSumAt()) || isNull(reportBI = doctorReportSowDao.findByDimension(dimensionCriteria))) {
            reportBI= new DoctorReportSow();
            reportBI.setOrzType(dimensionCriteria.getOrzType());
            reportBI.setDateType(dimensionCriteria.getDateType());
        }
        insertOrUpdate(build(pigDaily, reportBI, dimensionCriteria.getIsRealTime()));
    }

    private void insertOrUpdate(DoctorReportSow reportBi){
        if (isNull(reportBi.getId())) {
            doctorReportSowDao.create(reportBi);
            return;
        }
        doctorReportSowDao.update(reportBi);
    }

    public DoctorReportSow build(DoctorPigDailyExtend pigDaily, DoctorReportSow reportBi, Integer isRealTime) {
        if (Objects.equals(reportBi.getOrzType(), OrzDimension.FARM.getValue())) {
            reportBi.setOrzId(pigDaily.getFarmId());
            reportBi.setOrzName(pigDaily.getFarmName());
        } else if (Objects.equals(reportBi.getOrzType(), OrzDimension.ORG.getValue())){
            reportBi.setOrzId(pigDaily.getOrgId());
            reportBi.setOrzName(pigDaily.getOrgName());
        } else{
            reportBi.setOrzId(pigDaily.getGroupId());
            reportBi.setOrzName(pigDaily.getGroupName());
        }
        DateDimension dateDimension = DateDimension.from(reportBi.getDateType());
        reportBi.setSumAt(withDateStartDay(pigDaily.getSumAt(), dateDimension));
        reportBi.setSumAtName(dateCN(pigDaily.getSumAt(), dateDimension));
        buildRealTime(pigDaily, reportBi);
        if (!Objects.equals(isRealTime, IsOrNot.YES.getKey())) {
            buildDelay(pigDaily, reportBi);
        }
        return reportBi;
    }

    private void buildRealTime(DoctorPigDailyExtend pigDaily, DoctorReportSow reportBi) {
        DoctorFiledUrlCriteria filedUrlCriteria = new DoctorFiledUrlCriteria();
        fieldHelper.fillPigFiledUrl(filedUrlCriteria, pigDaily, reportBi.getOrzType(), reportBi.getDateType());

        reportBi.setStart(pigDaily.getSowStart());
        reportBi.setDead(fieldHelper.filedUrl(filedUrlCriteria, EventUtil.plusInt(pigDaily.getSowPhDead(), pigDaily.getSowCfDead()), "sowDead"));
        reportBi.setWeedOut(fieldHelper.filedUrl(filedUrlCriteria, EventUtil.plusInt(pigDaily.getSowPhWeedOut(), pigDaily.getSowCfWeedOut()), "sowWeedOut"));
        reportBi.setSale(fieldHelper.filedUrl(filedUrlCriteria, EventUtil.plusInt(pigDaily.getSowPhSale(), pigDaily.getSowCfSale()), "sowSale"));
        reportBi.setChgFarmOut(EventUtil.plusInt(pigDaily.getSowPhChgFarm(), pigDaily.getSowCfChgFarm()));
        reportBi.setOtherChange(EventUtil.plusInt(pigDaily.getSowPhOtherOut(), pigDaily.getSowCfOtherOut()));
        reportBi.setHoubeiIn(pigDaily.getSowPhReserveIn());
        reportBi.setOtherIn(otherIn(pigDaily, reportBi.getOrzType()));
        reportBi.setEnd(pigDaily.getSowEnd());
    }

    private void buildDelay(DoctorPigDailyExtend pigDaily, DoctorReportSow reportBi) {
        if (DateDimension.YEARLY.contains(reportBi.getDateType())) {
            reportBi.setDeadWeedOutRate(sowDeadWeedOutRate(pigDaily, reportBi.getOrzType()));
            reportBi.setDailyLivestockOnHand(pigDaily.getSowDailyPigCount());
        }
    }

    /**
     * ????????????+????????????
     * @param pigDaily
     * @param orzType
     * @return
     */
    private Integer otherIn(DoctorPigDailyExtend pigDaily, Integer orzType) {
        Integer otherIn = pigDaily.getSowPhEntryIn() - pigDaily.getSowPhReserveIn();
        if (Objects.equals(orzType, OrzDimension.FARM.getValue())) {
            otherIn = otherIn + pigDaily.getSowPhChgFarmIn() + pigDaily.getSowCfInFarmIn();
        }
        return otherIn;
    }
    private Double sowDeadWeedOutRate(DoctorPigDailyExtend pigDaily, Integer orzType) {
        Integer sowDeadWeedOut = pigDaily.getSowPhDead() + pigDaily.getSowCfDead()
                + pigDaily.getSowPhWeedOut() + pigDaily.getSowCfWeedOut();
        Integer sowIn = pigDaily.getSowPhEntryIn();

        if (Objects.equals(orzType, OrzDimension.FARM.getValue())) {
            sowIn = sowIn + pigDaily.getSowPhChgFarmIn()
                    + pigDaily.getSowCfInFarmIn();
        }
        return fieldHelper.get(sowDeadWeedOut, pigDaily.getSowStart() + sowIn);
    }

    public void deleteAll() {
        doctorReportSowDao.deleteAll();
    }
}
