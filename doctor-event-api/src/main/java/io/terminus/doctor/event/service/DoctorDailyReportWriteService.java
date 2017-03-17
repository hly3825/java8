package io.terminus.doctor.event.service;

import io.terminus.common.model.Response;

import javax.validation.constraints.NotNull;
import java.util.Date;
import java.util.List;

/**
 * Desc: 猪场日报表写服务
 * Mail: yangzl@terminus.io
 * author: DreamYoung
 * Date: 2016-07-19
 */

public interface DoctorDailyReportWriteService {

    /**
     * job批量创建DoctorDailyReport(先删除, 再创建)
     * @param farmIds 猪场ids
     * @param sumAt 统计时间
     * @return 是否成功
     */
    Response<Boolean> createDailyReports(@NotNull(message = "farmId.not.null") List<Long> farmIds,
                                         @NotNull(message = "date.not.null") Date sumAt);

    /**
     * (先删除, 再创建)日报
     * @param beginDate 需要更新的日报的开始范围, 只有日期没有时间, 且包括此日期
     * @param endDate  需要更新的日报的结束范围, 只有日期没有时间, 且包括此日期
     * @param farmId 猪场
     * @return 是否成功
     */
    Response<Boolean> createDailyReports(Date beginDate, Date endDate, Long farmId);

    /**
     * 删除存入redis的需要更新的日报
     * @param farmId 猪场
     * @return 是否成功
     */
    Response<Boolean> deleteDailyReport2Update(Long farmId);

    /**
     * job daily reports,生成昨天和今天的报表
     * @param farmIds
     * @return
     */
    Response<Boolean> createYesterdayAndTodayReports(@NotNull(message = "farmId.not.null") List<Long> farmIds);
}