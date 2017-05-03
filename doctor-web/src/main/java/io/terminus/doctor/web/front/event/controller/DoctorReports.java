package io.terminus.doctor.web.front.event.controller;

import com.google.common.collect.Lists;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import io.terminus.common.utils.Arguments;
import io.terminus.common.utils.BeanMapper;
import io.terminus.common.utils.Splitters;
import io.terminus.doctor.basic.model.DoctorMaterialConsumeProvider;
import io.terminus.doctor.basic.service.DoctorMaterialConsumeProviderReadService;
import io.terminus.doctor.common.utils.DateUtil;
import io.terminus.doctor.common.utils.Params;
import io.terminus.doctor.common.utils.RespHelper;
import io.terminus.doctor.event.dto.DoctorGroupDetail;
import io.terminus.doctor.event.dto.DoctorGroupSearchDto;
import io.terminus.doctor.event.dto.DoctorStockStructureDto;
import io.terminus.doctor.event.dto.report.common.DoctorCommonReportTrendDto;
import io.terminus.doctor.event.dto.report.daily.DoctorDailyReportDto;
import io.terminus.doctor.event.model.DoctorDailyReport;
import io.terminus.doctor.event.model.DoctorGroup;
import io.terminus.doctor.event.model.DoctorGroupBatchSummary;
import io.terminus.doctor.event.model.DoctorRangeReport;
import io.terminus.doctor.event.service.*;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.ArrayList;

import static io.terminus.common.utils.Arguments.notEmpty;

/**
 * Desc:
 * Mail: yangzl@terminus.io
 * author: DreamYoung
 * Date: 16/7/20
 */
@Slf4j
@RestController
@RequestMapping("/api/doctor/report")
public class DoctorReports {

    @RpcConsumer
    private DoctorDailyReportReadService doctorDailyReportReadService;

    @RpcConsumer
    private DoctorDailyReportWriteService doctorDailyReportWriteService;

    @RpcConsumer
    private DoctorCommonReportReadService doctorCommonReportReadService;

    @RpcConsumer
    private DoctorGroupBatchSummaryReadService doctorGroupBatchSummaryReadService;

    @RpcConsumer
    private DoctorMaterialConsumeProviderReadService doctorMaterialConsumeProviderReadService;

    @RpcConsumer
    private DoctorGroupReadService doctorGroupReadService;

    @RpcConsumer
    private DoctorRangeReportReadService doctorRangeReportReadService;

    /**
     * 根据farmId和日期查询猪场日报表(缓存方式)
     * @param farmId 猪场id
     * @param date   日期 yyyy-MM-dd
     * @return 猪场日报表
     */
    @RequestMapping(value = "/daily", method = RequestMethod.GET)
    public DoctorDailyReportDto findDailyReportByFarmIdAndSumAt(@RequestParam("farmId") Long farmId,
                                                                @RequestParam("date") String date) {
        DoctorDailyReportDto doctorDailyReportDto = new DoctorDailyReportDto();
        if(DateUtil.toDate(date).after(DateUtil.getDateEnd(DateTime.now()).toDate())){
            doctorDailyReportDto.setFail(true);
            return doctorDailyReportDto;
        }
        return RespHelper.or500(doctorDailyReportReadService.findDailyReportDtoByFarmIdAndSumAt(farmId, date));
    }

    /**
     * 根据farmId和日期查询猪场日报表(缓存方式)
     * @param farmId  猪场id
     * @param startAt 开始时间 yyyy-MM-dd
     * @param endAt   结束时间 yyyy-MM-dd
     * @return 猪场日报表
     */
    @RequestMapping(value = "/daily/range", method = RequestMethod.GET)
    public List<DoctorDailyReport> findDailyReportByFarmIdAndRange(@RequestParam("farmId") Long farmId,
                                                                   @RequestParam(value = "startAt", required = false) String startAt,
                                                                   @RequestParam(value = "endAt", required = false) String endAt) {
        return RespHelper.or500(doctorDailyReportReadService.findDailyReportDtoByFarmIdAndRange(farmId, startAt, endAt));
    }

    /**
     * 根据farmId和日期查询猪场月报表
     * @param farmId 猪场id
     * @param date   日期 yyyy-MM-dd
     * @return 猪场月报表
     */
    @RequestMapping(value = "/monthly", method = RequestMethod.GET)
    public DoctorCommonReportTrendDto findMonthlyReportTrendByFarmIdAndSumAt(@RequestParam("farmId") Long farmId,
                                                                             @RequestParam("date") String date,
                                                                             @RequestParam(value = "index", required = false) Integer index) {
        return RespHelper.or500(doctorCommonReportReadService.findMonthlyReportTrendByFarmIdAndSumAt(farmId, date, index));
    }

    /**
     * 根据farmId和日期查询猪场月报表
     * @param farmId 猪场id
     * @param date   日期 yyyy-MM-dd
     * @return 猪场月报表
     */
    @RequestMapping(value = "/monthly/structure", method = RequestMethod.GET)
    public DoctorStockStructureDto findMonthlyReportByFarmIdAndSumAt(@RequestParam("farmId") Long farmId,
                                                                    @RequestParam("date") String date) {
        Response<DoctorRangeReport> reportResponse = doctorRangeReportReadService.findMonthlyByFarmIdAndSumAt(farmId, date);
        if(Arguments.isNull(reportResponse) || Arguments.isNull(reportResponse.getResult())){
            return new DoctorStockStructureDto();
        }
        return reportResponse.getResult().getStockDistributeDto();
    }

    /**
     * 根据farmId和日期查询猪场周报表
     * @param farmId 猪场id
     * @param year   年份  2016
     * @param week   当年第几周 20
     * @return 猪场周报报表
     */
    @RequestMapping(value = "/weekly", method = RequestMethod.GET)
    public DoctorCommonReportTrendDto findWeeklyReportTrendByFarmIdAndSumAt(@RequestParam("farmId") Long farmId,
                                                                            @RequestParam(value = "year", required = false) Integer year,
                                                                            @RequestParam(value = "week", required = false) Integer week,
                                                                            @RequestParam(value = "index", required = false) Integer index) {
        return RespHelper.or500(doctorCommonReportReadService.findWeeklyReportTrendByFarmIdAndSumAt(farmId, year, week, index));
    }
    
    /**
     * 分页查询猪群批次总结
     * @return 批次总结
     */
    @RequestMapping(value = "/group/batch/summary", method = RequestMethod.GET)
    public Paging<DoctorGroupBatchSummary> pagingGroupBatchSummary(@RequestParam Map<String, Object> params,
                                                                   @RequestParam(required = false) Integer pageNo,
                                                                   @RequestParam(required = false) Integer pageSize) {
        params = Params.filterNullOrEmpty(params);
        if (!params.containsKey("farmId") || params.get("farmId") == null) {
            return new Paging<>(0L, Collections.emptyList());
        }

        DoctorGroupSearchDto dto = BeanMapper.map(Params.filterNullOrEmpty(params), DoctorGroupSearchDto.class);

        dto.setStartOpenAt(DateUtil.toDate(getDate(params.get("openStartAt"))));
        dto.setEndOpenAt(DateUtil.toDate(getDate(params.get("openEndAt"))));
        dto.setStartCloseAt(DateUtil.toDate(getDate(params.get("closeStartAt"))));
        dto.setEndCloseAt(DateUtil.toDate(getDate(params.get("closeEndAt"))));
        if (params.get("barnId") != null) {
            dto.setCurrentBarnId(Long.valueOf(String.valueOf(params.get("barnId"))));
        }

        if (notEmpty(dto.getPigTypeCommas())) {
            dto.setPigTypes(Splitters.splitToInteger(dto.getPigTypeCommas(), Splitters.COMMA));
        }
        Paging<DoctorGroupBatchSummary> paging = RespHelper.or500(doctorGroupBatchSummaryReadService.pagingGroupBatchSummary(dto, pageNo, pageSize));
        //如果猪群没有关闭，刷新下料肉比
        List<DoctorGroupBatchSummary> summaries = paging.getData().stream()
                .map(s -> {
                    if (Objects.equals(s.getStatus(), DoctorGroup.Status.CREATED.getValue())) {
                        Double material = RespHelper.or(doctorMaterialConsumeProviderReadService.sumConsumeFeed(null, null, null, null, null, s.getGroupId(), null, null), 0D);
                        s.setFcr(material / s.getFcr());
                    }
                    return s;
                })
                .collect(Collectors.toList());

        //对批次表进行金额的注入
        List<DoctorGroupBatchSummary> batchSummaries = summaries.stream()
                .map(s -> {
                    List<DoctorMaterialConsumeProvider> consumeProviders = Lists.newArrayList();
                    consumeProviders = RespHelper.or500(doctorMaterialConsumeProviderReadService.findMaterialByGroupId(s.getFarmId(),s.getGroupId(),null,null,null,null,1L,null,null));
                    s.setFeedAmount(getMaterialAmount(consumeProviders));
                    s.setFendNumber(getMaterialNumber(consumeProviders));
                    consumeProviders = RespHelper.or500(doctorMaterialConsumeProviderReadService.findMaterialByGroupId(s.getFarmId(),s.getGroupId(),null,null,null,null,2L,null,null));
                    s.setMedicineAmount(getMaterialAmount(consumeProviders));
                    consumeProviders = RespHelper.or500(doctorMaterialConsumeProviderReadService.findMaterialByGroupId(s.getFarmId(),s.getGroupId(),null,null,null,null,3L,null,null));
                    s.setVaccineAmount(getMaterialAmount(consumeProviders));
                    consumeProviders = RespHelper.or500(doctorMaterialConsumeProviderReadService.findMaterialByGroupId(s.getFarmId(),s.getGroupId(),null,null,null,null,4L,null,null));
                    s.setMedicineAmount(getMaterialAmount(consumeProviders));
                    consumeProviders = RespHelper.or500(doctorMaterialConsumeProviderReadService.findMaterialByGroupId(s.getFarmId(),s.getGroupId(),null,null,null,null,5L,null,null));
                    s.setConsumablesAmount(getMaterialAmount(consumeProviders));
                    return s;
                })
                .collect(Collectors.toList());

        return new Paging<>(paging.getTotal(), batchSummaries);
    }

    private static String getDate(Object o) {
        if (o == null) {
            return null;
        }
        return String.valueOf(o);
    }

    private static Double getMaterialNumber(List<DoctorMaterialConsumeProvider> doctorMaterialConsumeProviders) {
        Double number = 0.0;
        for (int i = 0; i < doctorMaterialConsumeProviders.size(); i++) {
            if (doctorMaterialConsumeProviders.get(i).getExtra() != null && doctorMaterialConsumeProviders.get(i).getExtraMap().containsKey("consumePrice")) {
                List<Map<String, Object>> priceCompose = (ArrayList) doctorMaterialConsumeProviders.get(i).getExtraMap().get("consumePrice");
                for (Map<String, Object> eachPrice : priceCompose) {
                    Double count = Double.valueOf(eachPrice.get("count").toString());
                    number += count;
                }
            } else {
                Double count = doctorMaterialConsumeProviders.get(i).getEventCount();
                number += count;
            }
        }
        return number;
    }

    private static Double getMaterialAmount(List<DoctorMaterialConsumeProvider> doctorMaterialConsumeProviders) {
        Double amount = 0.0;
        for (int i = 0; i < doctorMaterialConsumeProviders.size(); i++) {
            if (doctorMaterialConsumeProviders.get(i).getExtra() != null && doctorMaterialConsumeProviders.get(i).getExtraMap().containsKey("consumePrice")) {
                List<Map<String, Object>> priceCompose = (ArrayList) doctorMaterialConsumeProviders.get(i).getExtraMap().get("consumePrice");
                for (Map<String, Object> eachPrice : priceCompose) {
                    Long unitPrice = Long.valueOf(eachPrice.get("unitPrice").toString());
                    Double count = Double.valueOf(eachPrice.get("count").toString());
                    amount += unitPrice * count;
                }
            } else {
                Long unitPrice = doctorMaterialConsumeProviders.get(i).getUnitPrice();
                Double count = doctorMaterialConsumeProviders.get(i).getEventCount();
                amount += unitPrice * count;
            }
        }
        return amount;
    }

    /**
     * 分页查询猪群批次总结
     * @return 批次总结
     */
    @RequestMapping(value = "/group/batch", method = RequestMethod.GET)
    public DoctorGroupBatchSummary getGroupBatchSummary(@RequestParam("groupId") Long groupId, @RequestParam("fcc") Double fcc) {
        DoctorGroupDetail groupDetail = RespHelper.or500(doctorGroupReadService.findGroupDetailByGroupId(groupId));
        return RespHelper.or500(doctorGroupBatchSummaryReadService.getSummaryByGroupDetail(groupDetail, fcc));
    }

    @RequestMapping(value = "/getBarnLiveStocks", method = RequestMethod.GET)
    public Map<String, Integer> getBarnLiveStocks(@RequestParam Long barnId,
                                           @RequestParam Integer index){
        return RespHelper.or500(doctorCommonReportReadService.findBarnLiveStock(barnId, new Date(), index));
    }
}
