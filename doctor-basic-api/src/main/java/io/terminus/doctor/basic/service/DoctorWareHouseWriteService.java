package io.terminus.doctor.basic.service;

import io.terminus.common.model.Response;
import io.terminus.doctor.basic.model.DoctorWareHouse;

import javax.validation.constraints.NotNull;

/**
 * Created by yaoqijun.
 * Date:2016-05-13
 * Email:yaoqj@terminus.io
 * Descirbe:
 */
public interface DoctorWareHouseWriteService {

    /**
     * 创建WareHouse
     * @param doctorWareHouse
     * @return
     */
    Response<Long> createWareHouse(@NotNull(message = "input.wareHouse.empty") DoctorWareHouse doctorWareHouse);

    /**
     * 修改warehouse 信息, 修改ManagerId, ManagerName, address 地址信息， WareHouseName 仓库名称
     * @param wareHouse
     * @return
     */
    Response<Boolean> updateWareHouse(@NotNull(message = "input.warehouse.empty") DoctorWareHouse wareHouse);


    /**
     * 删除WareHouse
     * @param doctorWareHouse
     * @return
     */
    Response<Boolean> deleteWareHouse(DoctorWareHouse doctorWareHouse);

}
