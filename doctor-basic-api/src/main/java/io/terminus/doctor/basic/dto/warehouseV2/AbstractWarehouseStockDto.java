package io.terminus.doctor.basic.dto.warehouseV2;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import javax.validation.constraints.NotNull;
import java.util.Calendar;
import java.util.Date;

/**
 * Created by sunbo@terminus.io on 2017/8/20.
 */
@Data
public class AbstractWarehouseStockDto {

    @NotNull(message = "farm.id.null",groups = AbstractWarehouseStockDetail.StockDefaultValid.class)
    private Long farmId;

    @NotNull(message = "warehouse.stock.handle.date.null",groups = AbstractWarehouseStockDetail.StockDefaultValid.class)
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private Calendar handleDate;

    @NotNull(message = "warehouse.id.null",groups = AbstractWarehouseStockDetail.StockDefaultValid.class)
    private Long warehouseId;

    @NotNull(message = "warehouse.stock.operator.id.null",groups = AbstractWarehouseStockDetail.StockDefaultValid.class)
    private Long operatorId;

    private String operatorName;

    private Long stockHandleId;
}
