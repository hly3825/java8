package io.terminus.doctor.basic.service.warehouseV2;

import com.google.common.base.Throwables;
import io.terminus.boot.rpc.common.annotation.RpcProvider;
import io.terminus.common.exception.ServiceException;
import io.terminus.common.model.Response;
import io.terminus.doctor.basic.dao.*;
import io.terminus.doctor.basic.enums.WarehouseMaterialHandleDeleteFlag;
import io.terminus.doctor.basic.enums.WarehouseMaterialHandleType;
import io.terminus.doctor.basic.enums.WarehousePurchaseHandleFlag;
import io.terminus.doctor.basic.manager.*;
import io.terminus.doctor.basic.model.warehouseV2.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Desc:
 * Mail: [ your email ]
 * Date: 2017-08-21 08:56:13
 * Created by [ your name ]
 */
@Slf4j
@Service
@RpcProvider
public class DoctorWarehouseMaterialHandleWriteServiceImpl implements DoctorWarehouseMaterialHandleWriteService {

    @Autowired
    private DoctorWarehouseMaterialHandleDao doctorWarehouseMaterialHandleDao;
    @Autowired
    private DoctorWarehouseStockDao doctorWarehouseStockDao;

    @Autowired
    private DoctorWarehouseHandleDetailDao doctorWarehouseHandleDetailDao;

    @Autowired
    private DoctorWarehousePurchaseDao doctorWarehousePurchaseDao;

    @Autowired
    private DoctorWarehouseHandlerManager doctorWarehouseHandlerManager;
    @Autowired
    private DoctorWarehouseStockMonthlyManager doctorWarehouseStockMonthlyManager;
    @Autowired
    private DoctorWarehouseMaterialHandleManager doctorWarehouseMaterialHandleManager;
    @Autowired
    private WarehouseOutManager warehouseOutManager;
    @Autowired
    private WarehouseInManager warehouseInManager;

    @Override
    public Response<Long> create(DoctorWarehouseMaterialHandle doctorWarehouseMaterialHandle) {
        try {
            doctorWarehouseMaterialHandleDao.create(doctorWarehouseMaterialHandle);
            return Response.ok(doctorWarehouseMaterialHandle.getId());
        } catch (Exception e) {
            log.error("failed to create doctor warehouse material handle, cause:{}", Throwables.getStackTraceAsString(e));
            return Response.fail("doctor.warehouse.material.handle.create.fail");
        }
    }

    @Override
    public Response<Boolean> update(DoctorWarehouseMaterialHandle doctorWarehouseMaterialHandle) {
        try {
            return Response.ok(doctorWarehouseMaterialHandleDao.update(doctorWarehouseMaterialHandle));
        } catch (Exception e) {
            log.error("failed to update doctor warehouse material handle, cause:{}", Throwables.getStackTraceAsString(e));
            return Response.fail("doctor.warehouse.material.handle.update.fail");
        }
    }

    @Override
    @Transactional
    @ExceptionHandle("doctor.warehouse.material.handle.delete.fail")
    public Response<String> delete(Long id) {

        DoctorWarehouseMaterialHandle handle = doctorWarehouseMaterialHandleDao.findById(id);
        if (null == handle) {
            log.info("???????????????????????????,??????????????????????????????,id[{}]", id);
            return Response.fail("????????????????????????????????????");
        }

        Boolean flag=false;
        if(flag==false) {
            int type = handle.getType();
            if (type != 1 && type != 2 && type != 3 && type != 4 && type != 5 && type != 10 && type != 7 && type != 8 && type != 9
                    && type != 11 && type != 12 && type != 13) {
                return Response.fail("????????????");
            }
            //????????????
            if (type == 5) {
                return Response.fail("??????????????????????????????????????????");
            }
            //????????????
            if (type == 9) {
                return Response.fail("??????????????????????????????????????????");
            }
            //????????????,????????????,????????????
            if (type == 1 || type == 13 || type == 7) {
                warehouseInManager.recalculate(handle);
                warehouseInManager.delete(handle);
            }
            //????????????
            if (type == 8) {
                warehouseOutManager.delete(handle);
            }
            //????????????
            if (type == 2) {
                Integer countByRelMaterialHandleId = doctorWarehouseMaterialHandleDao.getCountByRelMaterialHandleId(handle.getId(), 13);
                if (countByRelMaterialHandleId > 0) {
                    return Response.fail("?????????????????????,???????????????");
                } else {
                    warehouseOutManager.delete(handle);
                }
            }
            //????????????,????????????
            if (type == 12 || type == 10) {
                int tt=0;
                if(type == 12){
                    tt=11;
                }else if(type==10){
                    tt=9;
                }
                DoctorWarehouseMaterialHandle byRelMaterialHandleId = doctorWarehouseMaterialHandleDao.findByRelMaterialHandleId(handle.getId(), tt);
                if (byRelMaterialHandleId != null) {
                    warehouseInManager.recalculate(byRelMaterialHandleId);//???????????????????????????
                    warehouseInManager.delete(byRelMaterialHandleId);//?????????????????????????????????
                }
                warehouseOutManager.delete(handle);//??????????????????????????????
            }
            flag=true;
        }
        if(flag==true){
            doctorWarehouseMaterialHandleManager.delete(handle);
            return Response.ok("????????????");
        }else{
            return Response.fail("????????????");
        }
    }


}