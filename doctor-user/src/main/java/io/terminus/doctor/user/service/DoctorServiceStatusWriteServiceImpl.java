package io.terminus.doctor.user.service;

import com.google.common.base.Throwables;
import io.terminus.boot.rpc.common.annotation.RpcProvider;
import io.terminus.common.model.Response;
import io.terminus.doctor.user.dao.DoctorServiceStatusDao;
import io.terminus.doctor.user.model.DoctorServiceStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Code generated by terminus code gen
 * Desc: 用户服务状态表写服务实现类
 * Date: 2016-06-03
 */
@Slf4j
@Service
@RpcProvider
public class DoctorServiceStatusWriteServiceImpl implements DoctorServiceStatusWriteService {

    private final DoctorServiceStatusDao doctorServiceStatusDao;

    @Autowired
    public DoctorServiceStatusWriteServiceImpl(DoctorServiceStatusDao doctorServiceStatusDao) {
        this.doctorServiceStatusDao = doctorServiceStatusDao;
    }

    @Override
    public Response<Long> createServiceStatus(DoctorServiceStatus serviceStatus) {
        try {
            doctorServiceStatusDao.create(serviceStatus);
            return Response.ok(serviceStatus.getId());
        } catch (Exception e) {
            log.error("create serviceStatus failed, serviceStatus:{}, cause:{}", serviceStatus, Throwables.getStackTraceAsString(e));
            return Response.fail("serviceStatus.create.fail");
        }
    }

    @Override
    public Response<Boolean> updateServiceStatus(DoctorServiceStatus serviceStatus) {
        try {
            return Response.ok(doctorServiceStatusDao.update(serviceStatus));
        } catch (Exception e) {
            log.error("update serviceStatus failed, serviceStatus:{}, cause:{}", serviceStatus, Throwables.getStackTraceAsString(e));
            return Response.fail("serviceStatus.update.fail");
        }
    }

    @Override
    public Response<Boolean> deleteById(Long id) {
        try {
            return Response.ok(doctorServiceStatusDao.delete(id));
        } catch (Exception e) {
            log.error("delete serviceStatus failed, id:{}, cause:{}", id, Throwables.getStackTraceAsString(e));
            return Response.fail("serviceStatus.delete.fail");
        }
    }
}
