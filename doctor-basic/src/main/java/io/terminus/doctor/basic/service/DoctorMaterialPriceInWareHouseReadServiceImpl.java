package io.terminus.doctor.basic.service;

import com.google.common.base.Throwables;
import io.terminus.boot.rpc.common.annotation.RpcProvider;
import io.terminus.common.model.Response;
import io.terminus.doctor.common.enums.WareHouseType;
import io.terminus.doctor.basic.dao.DoctorMaterialPriceInWareHouseDao;
import io.terminus.doctor.basic.model.DoctorMaterialPriceInWareHouse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Code generated by terminus code gen
 * Desc: 仓库中各物料每次入库的剩余量读服务实现类
 * Date: 2016-08-15
 */
@Slf4j
@Service
@RpcProvider
public class DoctorMaterialPriceInWareHouseReadServiceImpl implements DoctorMaterialPriceInWareHouseReadService {

    private final DoctorMaterialPriceInWareHouseDao doctorMaterialPriceInWareHouseDao;

    @Autowired
    public DoctorMaterialPriceInWareHouseReadServiceImpl(DoctorMaterialPriceInWareHouseDao doctorMaterialPriceInWareHouseDao) {
        this.doctorMaterialPriceInWareHouseDao = doctorMaterialPriceInWareHouseDao;
    }

    @Override
    public Response<DoctorMaterialPriceInWareHouse> findById(Long id) {
        try {
            return Response.ok(doctorMaterialPriceInWareHouseDao.findById(id));
        } catch (Exception e) {
            log.error("find materialPriceInWareHouse by id failed, materialPriceInWareHouseId:{}, cause:{}", id, Throwables.getStackTraceAsString(e));
            return Response.fail("materialPriceInWareHouse.find.fail");
        }
    }

    @Override
    public Response<List<DoctorMaterialPriceInWareHouse>> findByWareHouseAndMaterialId(Long wareHouseId, Long materialId) {
        try {
            return Response.ok(doctorMaterialPriceInWareHouseDao.findByWareHouseAndMaterialId(wareHouseId, materialId));
        } catch (Exception e) {
            log.error("find materialPriceInWareHouse by id failed, wareHouseId:{}, cause:{}", wareHouseId, Throwables.getStackTraceAsString(e));
            return Response.fail("materialPriceInWareHouse.find.fail");
        }
    }

    @Override
    public Response<Map<Long, Double>> stockAmount(Long farmId, Long warehouseId, WareHouseType type) {
        try{
            return Response.ok(doctorMaterialPriceInWareHouseDao.stockAmount(farmId, warehouseId, type));
        }catch(Exception e){
            log.error("stockAmount fail, cause:{}", Throwables.getStackTraceAsString(e));
            return Response.fail("stockAmount.fail");
        }
    }
}