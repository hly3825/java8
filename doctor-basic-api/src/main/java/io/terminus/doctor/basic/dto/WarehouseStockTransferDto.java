package io.terminus.doctor.basic.dto;

import lombok.Data;
import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * Created by sunbo@terminus.io on 2017/8/20.
 */
@Data
public class WarehouseStockTransferDto extends AbstractWarehouseStockDto implements Serializable {


    private static final long serialVersionUID = 3044734405148417477L;

    @Valid
    @NotEmpty(message = "stock.detail.empty")
    private List<WarehouseStockTransferDetail> details;

    @Data
    public static class WarehouseStockTransferDetail extends AbstractWarehouseStockDetail implements Serializable {

        private static final long serialVersionUID = 2339146227421186614L;

        @NotNull(message = "stock.target.warehouse.id.null")
        private Long transferInWarehouseId;

    }

}
