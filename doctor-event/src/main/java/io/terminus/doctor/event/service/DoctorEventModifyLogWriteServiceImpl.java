package io.terminus.doctor.event.service;

import com.google.common.base.Throwables;
import io.terminus.common.model.Response;
import io.terminus.doctor.event.dao.DoctorEventModifyLogDao;
import io.terminus.doctor.event.model.DoctorEventModifyLog;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Code generated by terminus code gen
 * Desc: 写服务实现类
 * Date: 2017-04-05
 */
@Slf4j
@Service
public class DoctorEventModifyLogWriteServiceImpl implements DoctorEventModifyLogWriteService {

    private final DoctorEventModifyLogDao doctorEventModifyLogDao;

    @Autowired
    public DoctorEventModifyLogWriteServiceImpl(DoctorEventModifyLogDao doctorEventModifyLogDao) {
        this.doctorEventModifyLogDao = doctorEventModifyLogDao;
    }

    @Override
    public Response<Long> createDoctorEventModifyLog(DoctorEventModifyLog doctorEventModifyLog) {
        try {
            doctorEventModifyLogDao.create(doctorEventModifyLog);
            return Response.ok(doctorEventModifyLog.getId());
        } catch (Exception e) {
            log.error("create doctorEventModifyLog failed, doctorEventModifyLog:{}, cause:{}", doctorEventModifyLog, Throwables.getStackTraceAsString(e));
            return Response.fail("doctorEventModifyLog.create.fail");
        }
    }

    @Override
    public Response<Boolean> updateDoctorEventModifyLog(DoctorEventModifyLog doctorEventModifyLog) {
        try {
            return Response.ok(doctorEventModifyLogDao.update(doctorEventModifyLog));
        } catch (Exception e) {
            log.error("update doctorEventModifyLog failed, doctorEventModifyLog:{}, cause:{}", doctorEventModifyLog, Throwables.getStackTraceAsString(e));
            return Response.fail("doctorEventModifyLog.update.fail");
        }
    }

    @Override
    public Response<Boolean> deleteDoctorEventModifyLogById(Long doctorEventModifyLogId) {
        try {
            return Response.ok(doctorEventModifyLogDao.delete(doctorEventModifyLogId));
        } catch (Exception e) {
            log.error("delete doctorEventModifyLog failed, doctorEventModifyLogId:{}, cause:{}", doctorEventModifyLogId, Throwables.getStackTraceAsString(e));
            return Response.fail("doctorEventModifyLog.delete.fail");
        }
    }
}
