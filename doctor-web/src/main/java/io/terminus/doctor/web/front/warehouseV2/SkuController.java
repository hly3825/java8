package io.terminus.doctor.web.front.warehouseV2;

import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Paging;
import io.terminus.doctor.basic.model.warehouseV2.DoctorWarehouseSku;
import io.terminus.doctor.basic.service.warehouseV2.DoctorWarehouseSkuReadService;
import io.terminus.doctor.basic.service.warehouseV2.DoctorWarehouseSkuWriteService;
import io.terminus.doctor.common.utils.RespHelper;
import io.terminus.doctor.user.model.DoctorFarm;
import io.terminus.doctor.user.service.DoctorFarmReadService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.validation.Errors;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by sunbo@terminus.io on 2017/10/13.
 */
@RestController
@RequestMapping("api/doctor/warehouse/sku")
public class SkuController {

    @RpcConsumer
    private DoctorWarehouseSkuReadService doctorWarehouseSkuReadService;
    @RpcConsumer
    private DoctorWarehouseSkuWriteService doctorWarehouseSkuWriteService;
    @RpcConsumer
    private DoctorFarmReadService doctorFarmReadService;

    @RequestMapping(method = RequestMethod.GET, value = "paging")
    public Paging<DoctorWarehouseSku> query(@RequestParam(required = false) Long orgId,
                                            @RequestParam(required = false) Long farmId,
                                            @RequestParam(required = false) Integer type,
                                            @RequestParam(required = false) String srm,
                                            @RequestParam(required = false) String srmOrName,
                                            @RequestParam(required = false) Integer pageNo,
                                            @RequestParam(required = false) Integer pageSize) {

        if (null == orgId && null == farmId)
            throw new JsonResponseException("warehouse.sku.org.id.or.farm.id.not.null");

        Map<String, Object> params = new HashMap<>();
        if (null != orgId)
            params.put("orgId", orgId);
        else {
            DoctorFarm farm = RespHelper.or500(doctorFarmReadService.findFarmById(farmId));
            if (null == farm)
                throw new JsonResponseException("farm.not.found");
            params.put("orgId", farm.getOrgId());
        }

        if (StringUtils.isNotBlank(srm))
            params.put("srm", srm);
        if (null != type)
            params.put("type", type);
        if (StringUtils.isNotBlank(srmOrName))
            params.put("nameOrSrmLike", srmOrName);

        return RespHelper.or500(doctorWarehouseSkuReadService.paging(pageNo, pageSize, params));
    }

    @RequestMapping(method = RequestMethod.GET, value = "{id}")
    public DoctorWarehouseSku query(@PathVariable Long id) {
        return RespHelper.or500(doctorWarehouseSkuReadService.findById(id));
    }


    @RequestMapping(method = RequestMethod.PUT)
    public boolean edit(@RequestBody @Validated(DoctorWarehouseSku.UpdateValid.class) DoctorWarehouseSku sku, Errors errors) {
        if (errors.hasErrors())
            throw new JsonResponseException(errors.getFieldError().getDefaultMessage());

        return RespHelper.or500(doctorWarehouseSkuWriteService.update(sku));
    }


    @RequestMapping(method = RequestMethod.POST)
    public boolean save(@RequestBody @Validated(DoctorWarehouseSku.CreateValid.class) DoctorWarehouseSku sku, Errors errors) {
        if (errors.hasErrors())
            throw new JsonResponseException(errors.getFieldError().getDefaultMessage());

        return null != RespHelper.or500(doctorWarehouseSkuWriteService.create(sku));
    }
}