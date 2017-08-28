package io.terminus.doctor.basic.service.warehouseV2;

import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import io.terminus.doctor.basic.model.warehouseV2.DoctorWarehousePurchase;

import java.util.Map;
import java.util.List;

/**
 * Desc:
 * Mail: [ your email ]
 * Date: 2017-08-21 00:18:50
 * Created by [ your name ]
 */
public interface DoctorWarehousePurchaseReadService {

    /**
     * 查询
     *
     * @param id
     * @return doctorWarehousePurchase
     */
    Response<DoctorWarehousePurchase> findById(Long id);

    /**
     * 分页
     *
     * @param pageNo
     * @param pageSize
     * @param criteria
     * @return Paging<DoctorWarehousePurchase>
     */
    Response<Paging<DoctorWarehousePurchase>> paging(Integer pageNo, Integer pageSize, Map<String, Object> criteria);

    /**
     * 列表
     *
     * @param criteria
     * @return List<DoctorWarehousePurchase>
     */
    Response<List<DoctorWarehousePurchase>> list(Map<String, Object> criteria);


    /**
     * 列表
     *
     * @param criteria
     * @return
     */
    Response<List<DoctorWarehousePurchase>> list(DoctorWarehousePurchase criteria);

    /**
     * 统计猪厂下每个仓库余额
     *
     * @return
     */
    Response<Map<Long, Long>> countWarehouseBalanceAmount(Long farmId);
}