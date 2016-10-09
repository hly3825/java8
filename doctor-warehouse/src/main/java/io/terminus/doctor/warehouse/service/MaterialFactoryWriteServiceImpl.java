package io.terminus.doctor.warehouse.service;

import com.google.common.base.Throwables;
import io.terminus.boot.rpc.common.annotation.RpcProvider;
import io.terminus.common.model.Response;
import io.terminus.doctor.warehouse.dao.MaterialFactoryDao;
import io.terminus.doctor.warehouse.model.MaterialFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Code generated by terminus code gen
 * Desc: 物资入库时可选择的厂家写服务实现类
 * Date: 2016-09-29
 */
@Slf4j
@Service
@RpcProvider
public class MaterialFactoryWriteServiceImpl implements MaterialFactoryWriteService {

    private final MaterialFactoryDao materialFactoryDao;

    @Autowired
    public MaterialFactoryWriteServiceImpl(MaterialFactoryDao materialFactoryDao) {
        this.materialFactoryDao = materialFactoryDao;
    }

    @Override
    public Response<Long> createMaterialFactory(MaterialFactory materialFactory) {
        try {
            materialFactoryDao.create(materialFactory);
            return Response.ok(materialFactory.getId());
        } catch (Exception e) {
            log.error("create materialFactory failed, materialFactory:{}, cause:{}", materialFactory, Throwables.getStackTraceAsString(e));
            return Response.fail("materialFactory.create.fail");
        }
    }

    @Override
    public Response<Boolean> updateMaterialFactory(MaterialFactory materialFactory) {
        try {
            return Response.ok(materialFactoryDao.update(materialFactory));
        } catch (Exception e) {
            log.error("update materialFactory failed, materialFactory:{}, cause:{}", materialFactory, Throwables.getStackTraceAsString(e));
            return Response.fail("materialFactory.update.fail");
        }
    }

    @Override
    public Response<Boolean> deleteMaterialFactoryById(Long materialFactoryId) {
        try {
            return Response.ok(materialFactoryDao.delete(materialFactoryId));
        } catch (Exception e) {
            log.error("delete materialFactory failed, materialFactoryId:{}, cause:{}", materialFactoryId, Throwables.getStackTraceAsString(e));
            return Response.fail("materialFactory.delete.fail");
        }
    }
}