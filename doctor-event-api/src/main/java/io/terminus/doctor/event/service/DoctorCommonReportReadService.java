package io.terminus.doctor.event.service;

import io.terminus.common.model.Response;
import io.terminus.doctor.event.dto.report.common.DoctorCliqueReportDto;
import io.terminus.doctor.event.dto.report.common.DoctorCommonReportDto;
import io.terminus.doctor.event.dto.report.common.DoctorCommonReportTrendDto;
import io.terminus.doctor.event.dto.report.common.DoctorGroupLiveStockDetailDto;
import io.terminus.doctor.event.dto.report.daily.DoctorFarmLiveStockDto;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Desc: 猪场报表读服务
 * Mail: yangzl@terminus.io
 * author: DreamYoung
 * Date: 2016-08-11
 */

public interface DoctorCommonReportReadService {

    /**
     * 根据farmId和统计日期查询猪场月报表和趋势图
     *
     * @param farmId 猪场id
     * @param sumAt  统计日期 yyyy-MM-dd
     * @param index  趋势月份数
     * @return 猪场月报表
     */
    Response<DoctorCommonReportTrendDto> findMonthlyReportTrendByFarmIdAndSumAt(@NotNull(message = "farmId.not.null") Long farmId,
                                                                                @NotNull(message = "date.not.null") String sumAt,
                                                                                @Nullable Integer index);

    /**
     * 根据farmId和日期段查询猪场月报表
     * @param farmId 猪场id
     * @param startDate 开始日期 yyyy-MM-dd
     * @param endDate 结束日期 yyyy-MM-dd
     * @return 猪场月报表
     */
     Response<List<DoctorCommonReportTrendDto>> findMonthlyReportTrendByFarmIdAndDuration( Long farmId, String startDate, String endDate);

    /**
     * 根据farmId和统计日期查询猪场周报表和趋势图
     *
     * @param farmId 猪场id
     * @param year   年份，默认今年
     * @param week   统计日期 当年的第几周，默认今天所在周
     * @param index  趋势图
     * @return 猪场周报表
     */
    Response<DoctorCommonReportTrendDto> findWeeklyReportTrendByFarmIdAndSumAt(@NotNull(message = "farmId.not.null") Long farmId,
                                                                               @Nullable Integer year,
                                                                               @Nullable Integer week,
                                                                               @Nullable Integer index);

    /**
     * 根据farmId和周段查询猪场周报表
     * @param farmId 猪场id
     * @param year   年份 如2016
     * @param startWeek 开始周
     * @param endWeek   结束周
     * @return 猪场周报报表
     */
    Response<List<DoctorCommonReportTrendDto>> findWeeklyReportTrendByFarmIdAndDuration(
            Long farmId, Integer year, Integer startWeek, Integer endWeek);


        /**
         * 根据统计时间查询所有猪场月报数据列
         * @param sumAt 统计时间
         * @return
         */
    Response<List<DoctorCommonReportDto>> findMonthlyReports(@NotNull(message = "date.not.null") String sumAt);

    /**
     * 根据日期获取当时猪群的情况
     */
    Response<List<DoctorGroupLiveStockDetailDto>> findEveryGroupInfo(@NotNull(message = "date.not.null") String sumAt);

    Response<Map<String, Integer>> findBarnLiveStock(Long barnId, Date date, Integer index);

    /**
     * 查询猪场当前存栏
     * @param farmId 猪场id
     * @return 存栏
     */
    Response<DoctorFarmLiveStockDto> findFarmCurrentLiveStock(Long farmId);

    /**
     * 查询过个猪场的存栏
     * @param farmIdList 猪场id列表
     * @return
     */
    Response<List<DoctorFarmLiveStockDto>> findFarmsLiveStock(List<Long> farmIdList);

    /**
     * 获取横向报表
     * @param farmIdToName 猪场id与farmName映射
     * @param startDate 开始日期 yyyy-MM-dd
     * @param endDate 结束时间 yyyy-MM-dd
     * @return 横向报表
     */
    Response<List<DoctorCliqueReportDto>> getTransverseCliqueReport(Map<Long, String> farmIdToName, String startDate, String endDate);

    /**
     * 获取纵向报表
     * @param farmIds 猪场id列表
     * @param startDate 开始日期 yyyy-MM-dd
     * @param endDate 结束时间 yyyy-MM-dd
     * @return 纵向报表
     */
    Response<List<DoctorCliqueReportDto>> getPortraitCliqueReport(List<Long> farmIds, String startDate, String endDate);
}
