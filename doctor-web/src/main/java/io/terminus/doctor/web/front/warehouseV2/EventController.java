package io.terminus.doctor.web.front.warehouseV2;

import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import io.terminus.doctor.basic.enums.WarehouseMaterialHandleDeleteFlag;
import io.terminus.doctor.basic.enums.WarehouseMaterialHandleType;
import io.terminus.doctor.basic.model.warehouseV2.DoctorWarehouseMaterialHandle;
import io.terminus.doctor.basic.model.warehouseV2.DoctorWarehouseStock;
import io.terminus.doctor.basic.service.warehouseV2.DoctorWarehouseMaterialHandleReadService;
import io.terminus.doctor.basic.service.warehouseV2.DoctorWarehouseMaterialHandleWriteService;
import io.terminus.doctor.basic.service.warehouseV2.DoctorWarehouseStockReadService;
import io.terminus.doctor.basic.service.warehouseV2.DoctorWarehouseStockWriteService;
import io.terminus.doctor.web.core.export.Exporter;
import io.terminus.doctor.web.front.warehouseV2.vo.WarehouseEventExportVo;
import io.terminus.doctor.web.front.warehouseV2.vo.WarehouseMaterialEventVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by sunbo@terminus.io on 2017/8/24.
 */
@Deprecated
@Slf4j
@RestController
@RequestMapping("api/doctor/warehouse/event")
public class EventController {

    @Autowired
    private Exporter exporter;

    @RpcConsumer
    private DoctorWarehouseMaterialHandleReadService doctorWarehouseMaterialHandleReadService;

    @RpcConsumer
    private DoctorWarehouseMaterialHandleWriteService doctorWarehouseMaterialHandleWriteService;

    @RpcConsumer
    private DoctorWarehouseStockReadService doctorWarehouseStockReadService;
    @RpcConsumer
    private DoctorWarehouseStockWriteService doctorWarehouseStockWriteService;

    @RequestMapping(method = RequestMethod.GET)
//    @JsonView(WarehouseMaterialHandleVo.MaterialHandleEventView.class)
    public Paging<WarehouseMaterialEventVo> paging(
            @RequestParam Long farmId,
            @RequestParam(required = false) Integer type,//1??????2??????
            @RequestParam(required = false) Long materialId,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date endDate,
            @RequestParam(required = false) Integer pageNo,
            @RequestParam(required = false) Integer pageSize) {


        List<Integer> types = WarehouseMaterialHandleType.getGroupType(type);

        if (null != startDate && null == endDate)
            endDate = new Date();
        if (null != startDate && null != endDate && startDate.after(endDate))
            throw new JsonResponseException("start.date.after.end.date");

        Map<String, Object> criteria = new HashMap<>();
        criteria.put("farmId", farmId);
        criteria.put("startDate", startDate);
        criteria.put("endDate", endDate);
        criteria.put("bigType", types);
        criteria.put("materialId", materialId);
        criteria.put("deleteFlag", WarehouseMaterialHandleDeleteFlag.NOT_DELETE.getValue());
        Response<Paging<DoctorWarehouseMaterialHandle>> handleResponse = doctorWarehouseMaterialHandleReadService.advPaging(pageNo, pageSize, criteria);
        if (!handleResponse.isSuccess())
            throw new JsonResponseException(handleResponse.getError());

        List<WarehouseMaterialEventVo> vos = new ArrayList<>(handleResponse.getResult().getData().size());
        for (DoctorWarehouseMaterialHandle handle : handleResponse.getResult().getData()) {

            boolean allowDelete = true;
            if (WarehouseMaterialHandleType.FORMULA_IN.getValue() == handle.getType() || WarehouseMaterialHandleType.FORMULA_OUT.getValue() == handle.getType()) {
                allowDelete = false;
            }
            if (WarehouseMaterialHandleType.IN.getValue() == handle.getType()
                    || WarehouseMaterialHandleType.INVENTORY_PROFIT.getValue() == handle.getType()
                    || WarehouseMaterialHandleType.TRANSFER_IN.getValue() == handle.getType()) {
                Response<List<DoctorWarehouseStock>> stockResponse = doctorWarehouseStockReadService.list(DoctorWarehouseStock.builder()
                        .warehouseId(handle.getWarehouseId())
                        .skuId(handle.getMaterialId())
                        .build());
                if (!stockResponse.isSuccess())
                    throw new JsonResponseException(stockResponse.getError());
                if (null == stockResponse.getResult() || stockResponse.getResult().isEmpty())
                    allowDelete = false;
                if (stockResponse.getResult().get(0).getQuantity().compareTo(handle.getQuantity()) < 0)
                    allowDelete = false;
            }

            vos.add(WarehouseMaterialEventVo.builder()
                    .id(handle.getId())
                    .materialName(handle.getMaterialName())
                    .warehouseName(handle.getWarehouseName())
                    .handleDate(handle.getHandleDate())
                    .quantity(handle.getQuantity())
                    .type(handle.getType())
                    .unit(handle.getUnit())
                    .unitPrice(handle.getUnitPrice())
                    .amount(handle.getQuantity().multiply(handle.getUnitPrice()))
                    .vendorName(handle.getVendorName())
                    .allowDelete(allowDelete)
                    .operatorId(handle.getOperatorId())
                    .operatorName(handle.getOperatorName())
                    .build());
        }

        Paging<WarehouseMaterialEventVo> warehouseMaterialHandleVoPaging = new Paging<>();
        warehouseMaterialHandleVoPaging.setTotal(handleResponse.getResult().getTotal());
        warehouseMaterialHandleVoPaging.setData(vos);
        return warehouseMaterialHandleVoPaging;
    }

    @RequestMapping(method = RequestMethod.GET, value = "export")
    public void export(@RequestParam Long farmId,
                       @RequestParam(required = false) Integer type,//1??????2??????
                       @RequestParam(required = false) Long materialId,
                       @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date startDate,
                       @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date endDate,
                       HttpServletRequest request,
                       HttpServletResponse response) {

        if (null != startDate && null == endDate)
            endDate = new Date();
        if (null != startDate && null != endDate && startDate.after(endDate))
            throw new JsonResponseException("start.date.after.end.date");

        List<Integer> types = WarehouseMaterialHandleType.getGroupType(type);

        Map<String, Object> criteria = new HashMap<>();
        criteria.put("farmId", farmId);
        criteria.put("startDate", startDate);
        criteria.put("endDate", endDate);
        criteria.put("bigType", types);
        criteria.put("materialId", materialId);

        Response<List<DoctorWarehouseMaterialHandle>> handleResponse = doctorWarehouseMaterialHandleReadService.advList(criteria);
        if (!handleResponse.isSuccess())
            throw new JsonResponseException(handleResponse.getError());

        exporter.export(handleResponse.getResult().stream().map(handle -> {
            WarehouseEventExportVo eventExportVo = new WarehouseEventExportVo();
            eventExportVo.setMaterialName(handle.getMaterialName());
            eventExportVo.setWareHouseName(handle.getWarehouseName());
            eventExportVo.setProviderFactoryName(handle.getVendorName());
            eventExportVo.setUnitName(handle.getUnit());
            eventExportVo.setUnitPrice(handle.getUnitPrice());
            eventExportVo.setEventTime(handle.getHandleDate());
            eventExportVo.setAmount(handle.getQuantity().multiply(handle.getUnitPrice()));
            return eventExportVo;
        }).collect(Collectors.toList()), "web-wareHouse-event", request, response);
    }

    @RequestMapping(method = RequestMethod.DELETE, value = "{id}")
    public boolean delete(@PathVariable Long id) {


        Response<String> response = doctorWarehouseMaterialHandleWriteService.delete(id);
        if (!response.isSuccess())
            throw new JsonResponseException(response.getError());
        return true;
    }


}

