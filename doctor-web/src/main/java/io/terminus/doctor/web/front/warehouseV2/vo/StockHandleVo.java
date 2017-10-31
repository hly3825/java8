package io.terminus.doctor.web.front.warehouseV2.vo;

import io.terminus.doctor.basic.model.warehouseV2.DoctorWarehouseMaterialHandle;
import io.terminus.doctor.basic.model.warehouseV2.DoctorWarehouseStockHandle;
import lombok.Data;

import java.util.List;

/**
 * Created by sunbo@terminus.io on 2017/10/31.
 */
@Data
public class StockHandleVo extends DoctorWarehouseStockHandle {

    private List<Detail> details;


    public static class Detail extends DoctorWarehouseMaterialHandle {

    }
}
