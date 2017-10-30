package io.terminus.doctor.basic.service.warehouseV2;

import io.terminus.doctor.basic.dao.DoctorWarehouseUnitOrgDao;
import io.terminus.doctor.basic.model.warehouseV2.DoctorWarehouseUnitOrg;

import io.terminus.common.model.Response;
import io.terminus.boot.rpc.common.annotation.RpcProvider;

import com.google.common.base.Throwables;
import io.terminus.doctor.basic.service.warehouseV2.DoctorWarehouseUnitOrgWriteService;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;

/**
 * Desc:
 * Mail: [ your email ]
 * Date: 2017-10-30 16:08:20
 * Created by [ your name ]
 */
@Slf4j
@Service
@RpcProvider
public class DoctorWarehouseUnitOrgWriteServiceImpl implements DoctorWarehouseUnitOrgWriteService {

    @Autowired
    private DoctorWarehouseUnitOrgDao doctorWarehouseUnitOrgDao;

    @Override
    public Response<Long> create(DoctorWarehouseUnitOrg doctorWarehouseUnitOrg) {
        try {
            doctorWarehouseUnitOrgDao.create(doctorWarehouseUnitOrg);
            return Response.ok(doctorWarehouseUnitOrg.getId());
        } catch (Exception e) {
            log.error("failed to create doctor warehouse unit org, cause:{}", Throwables.getStackTraceAsString(e));
            return Response.fail("doctor.warehouse.unit.org.create.fail");
        }
    }

    @Override
    public Response<Boolean> update(DoctorWarehouseUnitOrg doctorWarehouseUnitOrg) {
        try {
            return Response.ok(doctorWarehouseUnitOrgDao.update(doctorWarehouseUnitOrg));
        } catch (Exception e) {
            log.error("failed to update doctor warehouse unit org, cause:{}", Throwables.getStackTraceAsString(e));
            return Response.fail("doctor.warehouse.unit.org.update.fail");
        }
    }

    @Override
    public Response<Boolean> delete(Long id) {
        try {
            return Response.ok(doctorWarehouseUnitOrgDao.delete(id));
        } catch (Exception e) {
            log.error("failed to delete doctor warehouse unit org by id:{}, cause:{}", id, Throwables.getStackTraceAsString(e));
            return Response.fail("doctor.warehouse.unit.org.delete.fail");
        }
    }

    @Override
    @ExceptionHandle("doctor.warehouse.unit.bound.fail")
    public Response<Boolean> boundToOrg(Long orgId, String unitIds) {
        for (String id : unitIds.split(",")) {
            if (!doctorWarehouseUnitOrgDao.create(DoctorWarehouseUnitOrg.builder()
                    .orgId(orgId)
                    .unitId(Long.parseLong(id))
                    .build()))
                return Response.fail("doctor.warehouse.unit.bound.fail");
        }
        return Response.ok(true);
    }
}