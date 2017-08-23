package io.terminus.doctor.basic.service;

import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import io.terminus.doctor.basic.model.warehouse.DoctorWarehouseMaterialApply;

import java.util.Map;
import java.util.List;

/**
 * Desc:
 * Mail: [ your email ]
 * Date: 2017-08-21 14:05:59
 * Created by [ your name ]
 */
public interface DoctorWarehouseMaterialApplyReadService {

    /**
     * 查询
     * @param id
     * @return doctorWarehouseMaterialApply
     */
    Response<DoctorWarehouseMaterialApply> findById(Long id);

    /**
     * 分页
     * @param pageNo
     * @param pageSize
     * @param criteria
     * @return Paging<DoctorWarehouseMaterialApply>
     */
    Response<Paging<DoctorWarehouseMaterialApply>> paging(Integer pageNo, Integer pageSize, Map<String, Object> criteria);

   /**
    * 列表
    * @param criteria
    * @return List<DoctorWarehouseMaterialApply>
    */
    Response<List<DoctorWarehouseMaterialApply>> list(Map<String, Object> criteria);

    Response<List<DoctorWarehouseMaterialApply>> list(DoctorWarehouseMaterialApply criteria);

    Response<List<DoctorWarehouseMaterialApply>> listOrderByHandleDate(DoctorWarehouseMaterialApply criteria,Integer limit);
}