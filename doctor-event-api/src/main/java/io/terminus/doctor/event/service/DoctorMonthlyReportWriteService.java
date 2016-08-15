package io.terminus.doctor.event.service;

import io.terminus.common.model.Response;
import io.terminus.doctor.event.model.DoctorMonthlyReport;

import javax.validation.constraints.NotNull;
import java.util.Date;
import java.util.List;

/**
 * Desc: 猪场月报表写服务
 * Mail: yangzl@terminus.io
 * author: DreamYoung
 * Date: 2016-08-11
 */

public interface DoctorMonthlyReportWriteService {

    /**
     * 批量创建DoctorMonthlyReport
     * @param farmIds 猪场ids
     * @param sumAt 结算日期(天初)
     * @return 主键id
     */
    Response<Boolean> createMonthlyReports(@NotNull(message = "farmId.not.null") List<Long> farmIds,
                                           @NotNull(message = "date.not.null") Date sumAt);

    /**
     * 根据日期和猪场id获取初始化的月报统计
     *
     * @param farmId 猪场id
     * @param date   日期
     * @return 日报统计
     */
    Response<DoctorMonthlyReport> initMonthlyReportByFarmIdAndDate(@NotNull(message = "farmId.not.null") Long farmId,
                                                                   @NotNull(message = "date.not.null") Date date);
}