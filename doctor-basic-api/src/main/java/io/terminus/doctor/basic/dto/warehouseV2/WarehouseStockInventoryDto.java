package io.terminus.doctor.basic.dto.warehouseV2;

import io.terminus.doctor.basic.dto.warehouseV2.AbstractWarehouseStockDetail;
import io.terminus.doctor.basic.dto.warehouseV2.AbstractWarehouseStockDto;
import lombok.Data;
import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.Valid;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * Created by sunbo@terminus.io on 2017/8/20.
 */
@Data
public class WarehouseStockInventoryDto extends AbstractWarehouseStockDto implements Serializable {


    private static final long serialVersionUID = -70215823407762026L;


    @Valid
    @NotEmpty(message = "stock.detail.empty")
    private List<WarehouseStockInventoryDetail> details;


    @Data
    public static class WarehouseStockInventoryDetail extends AbstractWarehouseStockDetail implements Serializable {

        private static final long serialVersionUID = -3947299201390395960L;

//        @NotNull(message = "stock.quantity.null")
//        @DecimalMin.List(
//                @DecimalMin(inclusive = true, value = "0", message = "stock.quantity.small.then.zero"),
//                @DecimalMin(value = "0"))
//        private BigDecimal quantity;

    }

}
