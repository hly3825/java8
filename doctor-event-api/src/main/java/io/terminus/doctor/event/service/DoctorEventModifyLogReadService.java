package io.terminus.doctor.event.service;

import io.terminus.common.model.Response;
import io.terminus.doctor.event.model.DoctorEventModifyLog;

import java.util.List;

/**
 * Code generated by terminus code gen
 * Desc: 读服务
 * Date: 2017-04-05
 */

public interface DoctorEventModifyLogReadService {

    /**
     * 根据id查询
     * @param doctorEventModifyLogId 主键id
     * @return 
     */
    Response<DoctorEventModifyLog> findDoctorEventModifyLogById(Long doctorEventModifyLogId);
}