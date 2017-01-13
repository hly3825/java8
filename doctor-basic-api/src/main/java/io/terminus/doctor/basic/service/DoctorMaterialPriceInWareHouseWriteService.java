package io.terminus.doctor.basic.service;

import io.terminus.common.model.Response;
import io.terminus.doctor.basic.model.DoctorMaterialPriceInWareHouse;

/**
 * Code generated by terminus code gen
 * Desc: 仓库中各物料每次入库的剩余量写服务
 * Date: 2016-08-15
 */

public interface DoctorMaterialPriceInWareHouseWriteService {

    /**
     * 创建DoctorMaterialPriceInWareHouse
     * @param materialPriceInWareHouse
     * @return 主键id
     */
    Response<Long> createMaterialPriceInWareHouse(DoctorMaterialPriceInWareHouse materialPriceInWareHouse);

    /**
     * 更新DoctorMaterialPriceInWareHouse
     * @param materialPriceInWareHouse
     * @return 是否成功
     */
    Response<Boolean> updateMaterialPriceInWareHouse(DoctorMaterialPriceInWareHouse materialPriceInWareHouse);

    /**
     * 根据主键id删除DoctorMaterialPriceInWareHouse
     * @param materialPriceInWareHouseId
     * @return 是否成功
     */
    Response<Boolean> deleteMaterialPriceInWareHouseById(Long materialPriceInWareHouseId);
}