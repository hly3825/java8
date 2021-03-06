package io.terminus.doctor.move.controller.material;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.doctor.basic.enums.WarehouseMaterialApplyType;
import io.terminus.doctor.basic.model.DoctorMaterialConsumeProvider;
import io.terminus.doctor.basic.model.warehouseV2.DoctorWarehouseMaterialApply;
import io.terminus.doctor.basic.service.DoctorMaterialConsumeProviderReadService;
import io.terminus.doctor.basic.service.warehouseV2.DoctorWarehouseMaterialApplyReadService;
import io.terminus.doctor.common.enums.WareHouseType;
import io.terminus.doctor.common.utils.DateUtil;
import io.terminus.doctor.common.utils.RespHelper;
import io.terminus.doctor.event.dto.DoctorProfitExportDto;
import io.terminus.doctor.event.model.DoctorProfitMaterialOrPig;
import io.terminus.doctor.event.service.DoctorPigEventReadService;
import io.terminus.doctor.event.service.DoctorProfitMaterOrPigWriteServer;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.DateUtils;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Created by terminus on 2017/4/20.
 */
@Component
@Slf4j
public class DoctorGroupProfitManage {

    @Autowired
    private DoctorProfitMaterOrPigWriteServer doctorProfitMaterOrPigWriteServer;
    @Autowired
    private DoctorPigEventReadService doctorPigEventReadService;
    @Autowired
    private DoctorMaterialConsumeProviderReadService doctorMaterialConsumeProviderReadService;

    @RpcConsumer
    private DoctorWarehouseMaterialApplyReadService doctorWarehouseMaterialApplyReadService;


    public static final List<Long> materialType = Lists.newArrayList(1L, 2L, 3L, 4L, 5L);
    public static final List<String> pigType = Lists.newArrayList("3", "4", "5", "7_2");

    public Double feedAmount = 0.0;
    public Double materialAmount = 0.0;
    public Double vaccineAmount = 0.0;
    public Double medicineAmount = 0.0;
    public Double consumablesAmount = 0.0;
    public Double amount = 0.0;
    public Map<String, Double> barnAmount = new HashMap<>();
    public Map<String, Double> yearBarnAmount = new HashMap<>();

    public void sumDoctorProfitMaterialOrPig(List<Long> farmIds, Date dates) {

        Date startDate = DateUtil.monthStart(dates);
        Date endDate = DateUtil.getMonthEnd(new DateTime(startDate)).toDate();
        DateTime date;
        Date startDates;
        Date endDates;
        doctorProfitMaterOrPigWriteServer.deleteDoctorProfitMaterialOrPig(startDate);
        Map<String, Object> map = Maps.newHashMap();
        List<DoctorProfitMaterialOrPig> doctorProfitMaterialOrPigList = Lists.newArrayList();
        List<DoctorProfitExportDto> profitExportDto;
        DoctorProfitMaterialOrPig doctorProfitMaterialOrPig;
        List<DoctorProfitExportDto> profitYearExportDto;
        map.put("startDate", startDate);
        map.put("endDate", endDate);
        for (Long farmId : farmIds) {
            try {
                map.put("farmId", farmId);
                for (String pigs : pigType) {
                    map.put("pigTypeId", pigs);
                    profitExportDto = RespHelper.or500(doctorPigEventReadService.sumProfitAmount(map));
                    doctorProfitMaterialOrPig = new DoctorProfitMaterialOrPig();
                    Double amountPig = 0.0;
                    feedAmount = 0.0;
                    materialAmount = 0.0;
                    vaccineAmount = 0.0;
                    medicineAmount = 0.0;
                    consumablesAmount = 0.0;
                    amount = 0.0;
                    for (DoctorProfitExportDto doctorProfitExportDto : profitExportDto) {
                        doctorProfitMaterialOrPig = sumMaterialAmount(startDate, endDate, farmId, doctorProfitExportDto.getBarnId(), doctorProfitMaterialOrPig, true, pigs);
                        amountPig += doctorProfitExportDto.getAmount();
                    }
                    date = new DateTime(startDate);
                    startDates = DateUtils.addDays(startDate, 1 - date.dayOfYear().get());
                    endDates = DateUtils.addSeconds(DateUtils.addYears(startDates, 1), -1);
                    map.put("startDate", startDates);
                    map.put("endDate", endDates);
                    profitYearExportDto = RespHelper.or500(doctorPigEventReadService.sumProfitAmount(map));
                    Double amountPigYear = 0.0;
                    amount = 0.0;
                    for (DoctorProfitExportDto doctorProfitExportDto : profitYearExportDto) {
                        doctorProfitMaterialOrPig = sumMaterialAmount(startDates, endDates, farmId, doctorProfitExportDto.getBarnId(), doctorProfitMaterialOrPig, false, pigs);
                        amountPigYear += doctorProfitExportDto.getAmount();
                    }
                    if (!profitExportDto.isEmpty()) {
                        doctorProfitMaterialOrPig.setFarmId(farmId);
                        doctorProfitMaterialOrPig.setPigTypeNameId(pigs);
                        doctorProfitMaterialOrPig.setPigTypeName(profitExportDto.get(0).getPigTypeName());
                        doctorProfitMaterialOrPig.setAmountPig(amountPig);
                        doctorProfitMaterialOrPig.setSumTime(startDate);
                        doctorProfitMaterialOrPig.setRefreshTime(DateUtil.toDateTimeString(new Date()));
                        doctorProfitMaterialOrPig.setAmountYearPig(amountPigYear);
                        doctorProfitMaterialOrPig.setFeedAmount(feedAmount);
                        doctorProfitMaterialOrPig.setMaterialAmount(materialAmount);
                        doctorProfitMaterialOrPig.setMedicineAmount(medicineAmount);
                        doctorProfitMaterialOrPig.setConsumablesAmount(consumablesAmount);
                        doctorProfitMaterialOrPig.setAmountYearMaterial(amount);
                        doctorProfitMaterialOrPigList.add(doctorProfitMaterialOrPig);
                    }
                }
            } catch (Exception e) {
                log.error("doctor group profit fail, farmid:{}", farmId);
            } finally {
                barnAmount.clear();
                yearBarnAmount.clear();
            }
        }
        if (!doctorProfitMaterialOrPigList.isEmpty())
            doctorProfitMaterOrPigWriteServer.insterDoctorProfitMaterialOrPig(doctorProfitMaterialOrPigList);
    }

    private final DoctorProfitMaterialOrPig sumMaterialAmount(Date startDate, Date endDate, Long farmId, Long barnId, DoctorProfitMaterialOrPig doctorProfitMaterialOrPig, Boolean tag, String pigType) {

        List<DoctorMaterialConsumeProvider> doctorMaterialConsumeProviders = Lists.newArrayList();
        if (tag) {
            for (Long type : materialType) {

                String key = farmId + "|" + barnId + "|" + type;
                if (barnAmount.containsKey(key)) //????????????????????????????????????????????????????????????????????????map???????????????
                    continue;

                Map<String, Object> params = new HashMap<>();
                params.put("farmId", farmId);
                params.put("pigBarnId", barnId);
                params.put("type", type);
                params.put("applyType", WarehouseMaterialApplyType.BARN.getValue());
                params.put("startDate", startDate);
                params.put("endDate", endDate);
                List<DoctorWarehouseMaterialApply> applies = RespHelper.or500(doctorWarehouseMaterialApplyReadService.list(params));
                double totalAmount = applies.stream().mapToDouble(a -> a.getQuantity().multiply((a.getUnitPrice())).doubleValue()).sum();
                if (type == WareHouseType.FEED.getKey().intValue()) {
                    doctorProfitMaterialOrPig.setFeedTypeName("??????");
                    doctorProfitMaterialOrPig.setFeedTypeId(type);
                    doctorProfitMaterialOrPig.setFeedAmount(totalAmount);
                    feedAmount += totalAmount;
                } else if (type == WareHouseType.MATERIAL.getKey().intValue()) {
                    doctorProfitMaterialOrPig.setMaterialTypeName("??????");
                    doctorProfitMaterialOrPig.setMaterialTypeId(type);
                    doctorProfitMaterialOrPig.setMaterialAmount(totalAmount);
                    materialAmount += totalAmount;
                } else if (type == WareHouseType.VACCINATION.getKey().intValue()) {
                    doctorProfitMaterialOrPig.setVaccineTypeName("??????");
                    doctorProfitMaterialOrPig.setVaccineTypeId(type);
                    doctorProfitMaterialOrPig.setVaccineAmount(totalAmount);
                    vaccineAmount += totalAmount;
                } else if (type == WareHouseType.MEDICINE.getKey().intValue()) {
                    doctorProfitMaterialOrPig.setMedicineTypeName("??????");
                    doctorProfitMaterialOrPig.setMedicineTypeId(type);
                    doctorProfitMaterialOrPig.setMedicineAmount(totalAmount);
                    medicineAmount += totalAmount;
                } else if (type == WareHouseType.CONSUME.getKey().intValue()) {
                    doctorProfitMaterialOrPig.setConsumablesTypeName("?????????");
                    doctorProfitMaterialOrPig.setConsumablesTypeId(type);
                    doctorProfitMaterialOrPig.setConsumablesAmount(totalAmount);
                    consumablesAmount += totalAmount;
                }

                barnAmount.put(key, totalAmount);
//                doctorMaterialConsumeProviders = RespHelper.or500(doctorMaterialConsumeProviderReadService.findMaterialProfit(farmId,
//                        type,
//                        barnId,
//                        startDate,
//                        endDate));
//                if (type == 1L) {
//                    doctorProfitMaterialOrPig.setFeedTypeName("??????");
//                    doctorProfitMaterialOrPig.setFeedTypeId(type);
//                    feedAmount += builderDoctorMaterialConumeProvider(doctorMaterialConsumeProviders);
//                    doctorProfitMaterialOrPig.setFeedAmount(feedAmount);
//
//                } else if (type == 2L) {
//                    doctorProfitMaterialOrPig.setMaterialTypeName("??????");
//                    doctorProfitMaterialOrPig.setMaterialTypeId(type);
//                    materialAmount += builderDoctorMaterialConumeProvider(doctorMaterialConsumeProviders);
//
//                } else if (type == 3L) {
//                    doctorProfitMaterialOrPig.setVaccineTypeName("??????");
//                    doctorProfitMaterialOrPig.setVaccineTypeId(type);
//                    vaccineAmount += builderDoctorMaterialConumeProvider(doctorMaterialConsumeProviders);
//
//                } else if (type == 4L) {
//                    doctorProfitMaterialOrPig.setMedicineTypeName("??????");
//                    doctorProfitMaterialOrPig.setMedicineTypeId(type);
//                    medicineAmount += builderDoctorMaterialConumeProvider(doctorMaterialConsumeProviders);
//
//                } else if (type == 5L) {
//                    doctorProfitMaterialOrPig.setConsumablesTypeName("?????????");
//                    doctorProfitMaterialOrPig.setConsumablesTypeId(type);
//                    consumablesAmount += builderDoctorMaterialConumeProvider(doctorMaterialConsumeProviders);
//
//                }
            }
        } else {
//            doctorMaterialConsumeProviders = RespHelper.or500(doctorMaterialConsumeProviderReadService.findMaterialProfit(farmId,
//                    null,
//                    barnId,
//                    startDate,
//                    endDate));
//            amount += builderDoctorMaterialConumeProvider(doctorMaterialConsumeProviders);
            String key = farmId + "|" + barnId;
            if (yearBarnAmount.containsKey(key))
                return doctorProfitMaterialOrPig;

            Map<String, Object> params = new HashMap<>();
            params.put("farmId", farmId);
            params.put("pigBarnId", barnId);
            params.put("applyType", WarehouseMaterialApplyType.BARN.getValue());
            params.put("startDate", startDate);
            params.put("endDate", endDate);
            List<DoctorWarehouseMaterialApply> applies = RespHelper.or500(doctorWarehouseMaterialApplyReadService.list(params));
            amount += applies.stream().mapToDouble(a -> a.getQuantity().multiply((a.getUnitPrice())).doubleValue()).sum();

            yearBarnAmount.put(key, amount);
        }
        return doctorProfitMaterialOrPig;
    }

    private final Double builderDoctorMaterialConumeProvider(List<DoctorMaterialConsumeProvider> doctorMaterialConsumeProviders) {

        Double acmunt = 0.0;
        List<Map<String, Object>> priceCompose;
        for (int i = 0, length = doctorMaterialConsumeProviders.size(); i < length; i++) {

            if (doctorMaterialConsumeProviders.get(i).getExtra() != null && doctorMaterialConsumeProviders.get(i).getExtraMap().containsKey("consumePrice")) {
                priceCompose = (ArrayList) doctorMaterialConsumeProviders.get(i).getExtraMap().get("consumePrice");
                for (Map<String, Object> eachPrice : priceCompose) {

                    Long unitPrice = Long.valueOf(eachPrice.get("unitPrice").toString());
                    Double count = Double.valueOf(eachPrice.get("count").toString());
                    acmunt += unitPrice * count;
                }
            } else {
                Long unitPrice = doctorMaterialConsumeProviders.get(i).getUnitPrice();
                Double count = doctorMaterialConsumeProviders.get(i).getEventCount();
                acmunt += unitPrice * count;
            }

        }
        return acmunt;
    }
}
