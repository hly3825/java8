package io.terminus.doctor.web.front.warehouseV2;

import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import io.terminus.doctor.basic.enums.WarehouseMaterialHandleType;
import io.terminus.doctor.basic.model.DoctorWareHouse;
import io.terminus.doctor.basic.model.warehouseV2.*;
import io.terminus.doctor.basic.service.DoctorWareHouseReadService;
import io.terminus.doctor.basic.service.warehouseV2.*;
import io.terminus.doctor.common.enums.WareHouseType;
import io.terminus.doctor.common.utils.DateUtil;
import io.terminus.doctor.common.utils.RespHelper;
import io.terminus.doctor.event.model.DoctorGroup;
import io.terminus.doctor.event.service.DoctorGroupReadService;
import io.terminus.doctor.user.model.DoctorFarm;
import io.terminus.doctor.user.service.DoctorFarmReadService;
import io.terminus.doctor.web.core.export.Exporter;
import io.terminus.doctor.web.front.warehouseV2.vo.StockHandleExportVo;
import io.terminus.doctor.web.front.warehouseV2.vo.StockHandleVo;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.propertyeditors.CustomDateEditor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;


/**
 * ??????????????????
 * ??????
 * ??????
 * ??????
 * ??????
 * Created by sunbo@terminus.io on 2017/10/31.
 */
@Slf4j
@RestController
@RequestMapping("api/doctor/warehouse/receipt")
public class StockHandleController {

    @Autowired
    private Exporter exporter;

    @RpcConsumer
    private DoctorWarehouseStockHandleReadService doctorWarehouseStockHandleReadService;
    @RpcConsumer
    private DoctorWarehouseStockHandleWriteService doctorWarehouseStockHandleWriteService;
    @RpcConsumer
    private DoctorWarehouseMaterialHandleWriteService doctorWarehouseMaterialHandleWriteService;
    @RpcConsumer
    private DoctorWarehouseMaterialHandleReadService doctorWarehouseMaterialHandleReadService;
    @RpcConsumer
    private DoctorFarmReadService doctorFarmReadService;
    @RpcConsumer
    private DoctorWareHouseReadService doctorWareHouseReadService;
    @RpcConsumer
    private DoctorWarehouseSkuReadService doctorWarehouseSkuReadService;
    @RpcConsumer
    private DoctorWarehouseVendorReadService doctorWarehouseVendorReadService;
    @RpcConsumer
    private DoctorWarehouseMaterialApplyReadService doctorWarehouseMaterialApplyReadService;

    @RpcConsumer
    private DoctorWarehouseSettlementService doctorWarehouseSettlementService;

    @RpcConsumer
    private DoctorGroupReadService doctorGroupReadService;

    @InitBinder
    public void init(WebDataBinder webDataBinder) {
        webDataBinder.registerCustomEditor(Date.class, new CustomDateEditor(new SimpleDateFormat("yyyy-MM-dd"), true));
    }

    //????????????
    //??????????????????????????????????????????????????????????????????
    @RequestMapping(method = RequestMethod.GET, value = "/getFarmData")
    public List<Map> getFarmData(@RequestParam Long id) {
        List<Map> maps = RespHelper.or500(doctorWarehouseMaterialHandleReadService.getFarmData(id));
        maps.forEach( mp ->{
            DoctorWarehouseMaterialApply materialApply = RespHelper.or500(doctorWarehouseMaterialApplyReadService.findByMaterialHandleAndFarmId((Long) mp.get("material_handle_id"),(Long)mp.get("farm_id")));
            //????????????????????????
            if((materialApply.getPigGroupId()!=null&&!materialApply.getPigGroupId().equals(""))&&materialApply.getApplyType()==1){
                DoctorGroup doctorGroup = RespHelper.or500(doctorGroupReadService.findGroupById(materialApply.getPigGroupId()));
                if(doctorGroup.getStatus()==-1){
                    mp.put("status",-1);
                }else{
                    mp.put("status",1);
                }
            }else{
                mp.put("status",0);
            }
        });
        return maps;
    }

    //?????????????????????????????????
    @RequestMapping(method = RequestMethod.GET, value = "/getMaterialNameByID")
    public List<Map> getMaterialNameByID(@RequestParam Long id) {
        List<Map> maps = RespHelper.or500(doctorWarehouseMaterialHandleReadService.getMaterialNameByID(id));
        return maps;
    }


    //???????????????????????? ??????????????????????????????????????????????????????????????????????????????
    @RequestMapping(method = RequestMethod.GET, value = "/getDataByMaterialName")
    public List<Map> getDataByMaterialName(@RequestParam Long id) {
        List<Map> maps = RespHelper.or500(doctorWarehouseMaterialHandleReadService.getDataByMaterialName(id));
        return maps;
    }

    //??????????????????????????????
    @RequestMapping(method = RequestMethod.GET, value = "/getRetreatingData")
    public List<Map> getRetreatingData(@RequestParam Long id) {

        List<Map> maps=new ArrayList<Map>();
        //???????????????????????? ??????????????????????????????????????????????????????????????????????????????
        List<Map> mp = RespHelper.or500(doctorWarehouseMaterialHandleReadService.getDataByMaterialName(id));
        //??????????????????????????????????????????
        for(Map mpp :mp){
            //??????????????????
            BigDecimal RefundableNumber = new BigDecimal(0);
            //???????????????????????????
            BigDecimal LibraryQuantity = RespHelper.or500(doctorWarehouseMaterialHandleReadService.findLibraryById((Long) mpp.get("material_handle_id"),(String) mpp.get("material_name")));
            //??????????????????????????????????????????
            BigDecimal RetreatingQuantity = RespHelper.or500(doctorWarehouseMaterialHandleReadService.findRetreatingById((Long) mpp.get("material_handle_id"),(String) mpp.get("material_name"),id));
            RefundableNumber = LibraryQuantity.add(RetreatingQuantity);
            mpp.put("refundableQuantity",RefundableNumber.doubleValue());

            DoctorWarehouseMaterialApply materialApply = RespHelper.or500(doctorWarehouseMaterialApplyReadService.findByMaterialHandleAndFarmId((Long) mpp.get("material_handle_id"),(Long)mpp.get("farm_id")));
            mpp.put("applyBarnId",materialApply.getPigBarnId());
            mpp.put("applyGroupId",materialApply.getPigGroupId());
            if(materialApply.getPigBarnName()==null)
                mpp.put("applyBarnName","--");
            else
                mpp.put("applyBarnName",materialApply.getPigBarnName());
            if(materialApply.getPigGroupName()==null)
                mpp.put("applyGroupName","--");
            else
                mpp.put("applyGroupName",materialApply.getPigGroupName());

            //????????????????????????????????????????????????,????????????????????????????????????????????????

            if((materialApply.getPigGroupId()!=null&&!materialApply.getPigGroupId().equals(""))&&materialApply.getApplyType()==1){
                DoctorGroup doctorGroup = RespHelper.or500(doctorGroupReadService.findGroupById(materialApply.getPigGroupId()));
                if(doctorGroup.getStatus()!=-1){
                    maps.add(mpp);
                }
            }
            //??????
            if(materialApply.getPigGroupId()!=null&&materialApply.getApplyType()==2){
                maps.add(mpp);
            }
            //??????
            if(materialApply.getPigGroupId()==null&&materialApply.getApplyType()==0){
                maps.add(mpp);
            }
        };

        return maps;
    }

    //??????????????????
    @RequestMapping(value = "/paging", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public Paging<DoctorWarehouseStockHandle> paging(@RequestParam(required = false) Long farmId,
                                                     @RequestParam(required = false) Integer pageNo,
                                                     @RequestParam(required = false) Integer pageSize,
                                                     @RequestParam(required = false) Date startDate,
                                                     @RequestParam(required = false) Date endDate,
                                                     @RequestParam(required = false) Date updatedAt,
                                                     @RequestParam(required = false) Long operatorId,
                                                     @RequestParam(required = false) Long warehouseId,
                                                     @RequestParam(required = false) Integer type,
                                                     @RequestParam(required = false) Integer subType) {

        if (null != startDate && null != endDate && startDate.after(endDate))
            throw new JsonResponseException("start.date.after.end.date");

        Map<String, Object> params = new HashMap<>();
        params.put("farmId", farmId);
        params.put("warehouseId", warehouseId);
        params.put("operatorId", operatorId);
        params.put("handleType", type);
        params.put("handleSubType", subType);
        params.put("startDate", startDate);
        params.put("endDate", endDate);
        params.put("updatedAt", updatedAt);
        return RespHelper.or500(doctorWarehouseStockHandleReadService.paging(pageNo, pageSize, params));
    }


    //??????
    @RequestMapping(method = RequestMethod.GET, value = "{id:\\d+}")
    public StockHandleVo query(@PathVariable Long id,
                               @RequestParam(required = false) Long orgId,
                               @RequestParam(required = false) String date) {

        //?????????
        DoctorWarehouseStockHandle stockHandle = RespHelper.or500(doctorWarehouseStockHandleReadService.findById(id));
        if (null == stockHandle)
            return null;

        StockHandleVo vo = new StockHandleVo();
        BeanUtils.copyProperties(stockHandle, vo);

        vo.setDetails(
                //???????????????
                RespHelper.or500(doctorWarehouseMaterialHandleReadService.findByStockHandle(stockHandle.getId()))
                        .stream()
                        .map(mh -> {
                            StockHandleVo.Detail detail = new StockHandleVo.Detail();
                            //???????????????????????????????????????detail?????????
                            BeanUtils.copyProperties(mh, detail);

                            //  ????????????????????????????????????????????????????????????????????????????????? ????????? 2018-09-19???
                            String desc=new String();
                            DoctorWarehouseMaterialHandle material = RespHelper.or500(doctorWarehouseMaterialHandleReadService.getMaxInventoryDate(stockHandle.getWarehouseId(), mh.getMaterialId(), stockHandle.getHandleDate()));
                            if(material!=null){
                                if(material!=null){
                                    if(!material.getStockHandleId().equals(stockHandle.getId())){
                                        if(stockHandle.getUpdatedAt().compareTo(material.getHandleDate())<0){
                                            detail.setIsInventory(1);
                                            vo.setHasInventory(1);
                                            desc = desc + "?????????????????????,???????????????;";
                                        }
                                    }
                                }
                            }

                            if ((!stockHandle.getHandleSubType().equals(WarehouseMaterialHandleType.IN.getValue()))&&(!stockHandle.getHandleSubType().equals(WarehouseMaterialHandleType.INVENTORY_PROFIT.getValue()))) {
                                try {
                                    //??????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
                                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM");
                                    boolean b;
                                    if (orgId != null && date != null) {
                                        b = doctorWarehouseSettlementService.isSettled(orgId, sdf.parse(date));
                                    } else {
                                        b = doctorWarehouseSettlementService.isSettled(mh.getOrgId(), mh.getSettlementDate());
                                    }
                                    if (!b) {
                                        if (!stockHandle.getHandleSubType().equals(WarehouseMaterialHandleType.IN.getValue()) || stockHandle.getHandleSubType() != WarehouseMaterialHandleType.IN.getValue()) {
                                            detail.setUnitPrice(BigDecimal.ZERO);
                                            detail.setAmount(BigDecimal.ZERO);
                                        }
                                    }
                                } catch (ParseException e) {
                                    e.printStackTrace();
                                }
                            }

                            //?????????
                            DoctorWarehouseSku sku = RespHelper.or500(doctorWarehouseSkuReadService.findById(mh.getMaterialId()));
                            if (null != sku) {
                                detail.setVendorName(RespHelper.or500(doctorWarehouseVendorReadService.findNameById(sku.getVendorId())));
                                detail.setMaterialCode(sku.getCode());
                                //??????????????????
                                String nameByUnit = RespHelper.or500(doctorWarehouseStockHandleReadService.getNameByUnit(Long.parseLong(sku.getUnit())));
                                detail.setUnit(nameByUnit);
                                detail.setUnitId(sku.getUnit());
                                detail.setMaterialSpecification(sku.getSpecification());
                            } else {
                                log.warn("sku not found,{}", mh.getMaterialId());
                            }

                            //????????????
                            if (stockHandle.getHandleSubType().equals(WarehouseMaterialHandleType.OUT.getValue())) {
                                DoctorWarehouseMaterialApply apply = RespHelper.or500(doctorWarehouseMaterialApplyReadService.findByMaterialHandleAndFarmId(mh.getId(),mh.getFarmId()));
                                if (null != apply) {
                                    detail.setApplyPigBarnName(apply.getPigBarnName());
                                    detail.setApplyPigBarnId(apply.getPigBarnId());
                                    if(apply.getPigGroupName()!=null){
                                        detail.setApplyPigGroupName(apply.getPigGroupName());
                                    }else{
                                        detail.setApplyPigGroupName("--");
                                    }
                                    detail.setApplyPigGroupId(apply.getPigGroupId());
                                    detail.setApplyStaffName(apply.getApplyStaffName());
                                    detail.setApplyStaffId(apply.getApplyStaffId());
                                } else
                                    log.warn("material apply not found,by material handle {}", mh.getId());

                                //????????????????????????

                                if((apply.getPigGroupId()!=null&&!apply.getPigGroupId().equals(""))&&apply.getApplyType()==1){

                                    DoctorGroup doctorGroup = RespHelper.or500(doctorGroupReadService.findGroupById(apply.getPigGroupId()));
                                    detail.setGroupStatus(doctorGroup.getStatus());
                                    if(doctorGroup.getStatus()==-1){
                                        vo.setStatus(doctorGroup.getStatus());
                                        desc = desc + "?????????????????????,???????????????;";
                                    }
                                }

                                //?????????????????????????????????????????????
                                Integer count = RespHelper.or500(doctorWarehouseMaterialHandleReadService.findCountByRelMaterialHandleId(mh.getId(), mh.getFarmId()));
                                detail.setRetreatingCount(count);
                                if(count>=1){
                                    desc = desc + "???????????????????????????,???????????????;";
                                }

                            }

                            //????????????-->????????????
                            if (stockHandle.getHandleSubType().equals(WarehouseMaterialHandleType.RETURN.getValue())) {
                                DoctorWarehouseMaterialApply apply = RespHelper.or500(doctorWarehouseMaterialApplyReadService.findByMaterialHandleAndFarmId(mh.getRelMaterialHandleId(),mh.getFarmId()));
                                if (null != apply) {
                                    detail.setApplyPigBarnName(apply.getPigBarnName());
                                    detail.setApplyPigBarnId(apply.getPigBarnId());
                                    if(apply.getPigGroupName()!=null){
                                        detail.setApplyPigGroupName(apply.getPigGroupName());
                                    }else{
                                        detail.setApplyPigGroupName("--");
                                    }
                                    detail.setApplyPigGroupId(apply.getPigGroupId());
                                } else {
                                    log.warn("material apply not found,by material handle {}", mh.getId());
                                }
                                BigDecimal RefundableNumber = new BigDecimal(0);
                                //???????????????????????????
                                BigDecimal LibraryQuantity = RespHelper.or500(doctorWarehouseMaterialHandleReadService.findLibraryById(mh.getRelMaterialHandleId(),mh.getMaterialName()));
                                //??????????????????????????????????????????
                                BigDecimal RetreatingQuantity = RespHelper.or500(doctorWarehouseMaterialHandleReadService.findRetreatingById(mh.getRelMaterialHandleId(),mh.getMaterialName(),stockHandle.getId()));
                                RefundableNumber = LibraryQuantity.add(RetreatingQuantity);
                                detail.setRefundableQuantity(RefundableNumber.doubleValue());

                                //????????????????????????

                                if((apply.getPigGroupId()!=null&&!apply.getPigGroupId().equals(""))&&apply.getApplyType()==1){

                                    DoctorGroup doctorGroup = RespHelper.or500(doctorGroupReadService.findGroupById(apply.getPigGroupId()));
                                    detail.setGroupStatus(doctorGroup.getStatus());
                                    if(doctorGroup.getStatus()==-1){
                                        vo.setStatus(doctorGroup.getStatus());
                                        desc = desc + "?????????????????????,???????????????;";
                                    }
                                }
                            }

                            //??????
                            if (stockHandle.getHandleSubType().equals(WarehouseMaterialHandleType.TRANSFER_OUT.getValue())) {
                                //???????????????
                                DoctorWarehouseMaterialHandle transferInHandle = RespHelper.or500(doctorWarehouseMaterialHandleReadService.findById(mh.getRelMaterialHandleId()));
                                if (transferInHandle != null) {
                                    DoctorWareHouse wareHouse = RespHelper.or500(doctorWareHouseReadService.findById(transferInHandle.getWarehouseId()));
                                    if (wareHouse != null) {
                                        detail.setTransferInWarehouseName(wareHouse.getWareHouseName());
                                        detail.setTransferInWarehouseId(wareHouse.getId());
                                        detail.setTransferInFarmName(wareHouse.getFarmName());
                                        detail.setTransferInFarmId(wareHouse.getFarmId());
                                    } else
                                        log.warn("warehouse not found,{}", transferInHandle.getWarehouseId());
                                } else
                                    log.warn("other transfer in handle not found,{}", mh.getRelMaterialHandleId());
                            }

                            //??????????????????
                            if (stockHandle.getHandleSubType().equals( WarehouseMaterialHandleType.FORMULA_OUT.getValue())) {
                                DoctorWarehouseStockHandle sh = RespHelper.or500(doctorWarehouseStockHandleReadService.findwarehouseName(stockHandle.getRelStockHandleId()));
                                detail.setStorageWarehouseIds(sh.getWarehouseId());
                                detail.setStorageWarehouseNames(sh.getWarehouseName());
                            }

                            // ???????????? ????????? 2018-09-19???
                            detail.setDesc(desc);

                            return detail;
                        })
                        .collect(Collectors.toList()));

        //??????????????????
        if (stockHandle.getHandleSubType().equals( WarehouseMaterialHandleType.FORMULA_OUT.getValue())) {
            DoctorWarehouseStockHandle sh = RespHelper.or500(doctorWarehouseStockHandleReadService.findwarehouseName(stockHandle.getRelStockHandleId()));
            vo.setStorageWarehouseId(sh.getWarehouseId());
            vo.setStorageWarehouseName(sh.getWarehouseName());
        }


        DoctorFarm farm = RespHelper.or500(doctorFarmReadService.findFarmById(vo.getFarmId()));
        if (farm != null) {
            vo.setFarmName(farm.getName());
            vo.setOrgName(farm.getOrgName());
        }

        DoctorWareHouse wareHouse = RespHelper.or500(doctorWareHouseReadService.findById(vo.getWarehouseId()));
        if (wareHouse != null) {
            vo.setWarehouseManagerName(wareHouse.getManagerName());
        }

        if (!vo.getDetails().isEmpty()) {
            vo.setWarehouseType(vo.getDetails().get(0).getWarehouseType());
        }

        BigDecimal totalQuantity = new BigDecimal(0);
        BigDecimal totalUnitPrice = new BigDecimal(0);
        double totalAmount = 0L;
        for (StockHandleVo.Detail detail : vo.getDetails()) {
            totalQuantity = totalQuantity.add(detail.getQuantity());
            totalUnitPrice = totalUnitPrice.add(null == detail.getUnitPrice() ? new BigDecimal(0) : detail.getUnitPrice());
            totalAmount += detail.getQuantity().multiply(detail.getUnitPrice()).doubleValue();
        }

        vo.setTotalQuantity(totalQuantity.doubleValue());
        vo.setTotalAmount(totalAmount);
        //vo.setTotalAmount(totalQuantity.multiply(totalUnitPrice).doubleValue());
        return vo;
    }

    //?????????????????????????????????????????????
    @RequestMapping(method = RequestMethod.DELETE, value = "{id:\\d+}")
    public Response<String> delete(@PathVariable Long id,@RequestParam(required = false) Long orgId,@RequestParam(required = false) String settlementDate) {
        //??????????????????????????????
        if (doctorWarehouseSettlementService.isUnderSettlement(orgId))
            throw new JsonResponseException("under.settlement");
        SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM");
        Date date = null;
        try {
           date  = sdf.parse(settlementDate);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        if (doctorWarehouseSettlementService.isSettled(orgId,date))
            throw new JsonResponseException("already.settlement");

        //  ????????????????????????????????????????????? ????????? 2018-09-18???
        String str = RespHelper.or500(doctorWarehouseMaterialHandleReadService.deleteCheckInventory(id));
        if(str!=null&&!str.equals("")){
            throw new JsonResponseException(str);
        }

        return doctorWarehouseStockHandleWriteService.delete(id);
           /*if (!response.isSuccess())
               throw new JsonResponseException(response.getError());
           return true;*/
    }

    //??????id???????????????????????????
    @RequestMapping(method = RequestMethod.GET, value = "/findByRelMaterialHandleId")
    public Response<Integer> findByRelMaterialHandleId(@RequestParam Long id,@RequestParam Long farmId) {
        Response<Integer> count = doctorWarehouseMaterialHandleReadService.findCountByRelMaterialHandleId(id, farmId);
        return count;
    }

    //?????????????????????
    @RequestMapping(method = RequestMethod.DELETE, value = "/deleteById/{id:\\d+}")
    public Response<String> deleteById(@PathVariable Long id,@RequestParam(required = false) Long orgId) {
        //??????????????????????????????
        if (doctorWarehouseSettlementService.isUnderSettlement(orgId))
            throw new JsonResponseException("under.settlement");

        return doctorWarehouseMaterialHandleWriteService.delete(id);
    }

    //??????
    @RequestMapping(method = RequestMethod.GET, value = "{id:\\d+}/export")
    public void export(@PathVariable Long id,
                       HttpServletRequest request,
                       HttpServletResponse response) {

        //?????????
        DoctorWarehouseStockHandle stockHandle = RespHelper.or500(doctorWarehouseStockHandleReadService.findById(id));
        if (null == stockHandle)
            throw new JsonResponseException("warehouse.stock.handle.not.found");

        //?????????Model???
        DoctorFarm farm = RespHelper.or500(doctorFarmReadService.findFarmById(stockHandle.getFarmId()));
        if (null == farm)
            throw new JsonResponseException("farm.not.found");

        //??????
        DoctorWareHouse wareHouse = RespHelper.or500(doctorWareHouseReadService.findById(stockHandle.getWarehouseId()));
        if (null == wareHouse)
            throw new JsonResponseException("warehouse.not.found");

        String farmName = farm.getName();
        String operatorTypeName = "";
        switch (stockHandle.getHandleType()) {
            case 1:
                operatorTypeName = "?????????";
                break;
            case 2:
                operatorTypeName = "?????????";
                break;
            case 3:
                operatorTypeName = "?????????";
                break;
            case 4:
                operatorTypeName = "?????????";
                break;
        }

        List<StockHandleExportVo> exportVos = RespHelper.or500(doctorWarehouseMaterialHandleReadService.findByStockHandle(id))
                .stream()
                .map(mh -> {
                    StockHandleExportVo vo = new StockHandleExportVo();
                    BeanUtils.copyProperties(mh, vo);
                    vo.setHandleType(mh.getType());
                    vo.setBeforeInventoryQuantity(mh.getBeforeStockQuantity());

                    //??????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
                    boolean b = doctorWarehouseSettlementService.isSettled(mh.getOrgId(), mh.getSettlementDate());
                    if(!b){
                        if (!stockHandle.getHandleSubType().equals(WarehouseMaterialHandleType.IN.getValue())||stockHandle.getHandleSubType()!=WarehouseMaterialHandleType.IN.getValue()) {
                            vo.setUnitPrice(0.0);
                            vo.setAmount(0.0);
                        }
                    }

                    //?????????
                    DoctorWarehouseSku sku = RespHelper.or500(doctorWarehouseSkuReadService.findById(mh.getMaterialId()));
                    if (null != sku) {
                        vo.setVendorName(RespHelper.or500(doctorWarehouseVendorReadService.findNameById(sku.getVendorId())));
                        vo.setMaterialCode(sku.getCode());
                        //??????????????????
                        String nameByUnit = RespHelper.or500(doctorWarehouseStockHandleReadService.getNameByUnit(Long.parseLong(sku.getUnit())));
                        vo.setUnit(nameByUnit);
                        vo.setMaterialSpecification(sku.getSpecification());
                    }else
                        log.warn("DoctorWarehouseSku found", mh.getMaterialId());

                    //????????????
                    if (stockHandle.getHandleSubType().equals(WarehouseMaterialHandleType.OUT.getValue())) {
                        //???????????????
                        DoctorWarehouseMaterialApply apply = RespHelper.or500(doctorWarehouseMaterialApplyReadService.findByMaterialHandleAndFarmId(mh.getId(),mh.getFarmId()));
                        if (null != apply) {
                            vo.setApplyPigBarnName(apply.getPigBarnName());
                            if(apply.getPigGroupName()!=null){
                                vo.setApplyPigGroupName(apply.getPigGroupName());
                            }else{
                                vo.setApplyPigGroupName("--");
                            }
                            vo.setApplyStaffName(apply.getApplyStaffName());
                        } else
                            log.warn("material apply not found,by material handle {}", mh.getId());
                    }

                    //??????
                    if (stockHandle.getHandleSubType().equals(WarehouseMaterialHandleType.TRANSFER_OUT.getValue())) {
                        DoctorWarehouseMaterialHandle transferInHandle = RespHelper.or500(doctorWarehouseMaterialHandleReadService.findById(mh.getRelMaterialHandleId()));
                        if (transferInHandle != null) {
                            //???????????????
                            DoctorWareHouse transferInWarehouse = RespHelper.or500(doctorWareHouseReadService.findById(transferInHandle.getWarehouseId()));
                            if (transferInWarehouse != null) {
                                vo.setTransferInWarehouseName(transferInWarehouse.getWareHouseName());
                                vo.setTransferInFarmName(transferInWarehouse.getFarmName());
                            } else
                                log.warn("warehouse not found,{}", transferInHandle.getWarehouseId());
                        } else
                            log.warn("other transfer in handle not found,{}", mh.getRelMaterialHandleId());
                    }

                    //????????????-->????????????
                    if (stockHandle.getHandleSubType().equals(WarehouseMaterialHandleType.RETURN.getValue())) {
                        DoctorWarehouseMaterialApply apply = RespHelper.or500(doctorWarehouseMaterialApplyReadService.findByMaterialHandleAndFarmId(mh.getRelMaterialHandleId(),mh.getFarmId()));
                        if (null != apply) {
                            vo.setApplyPigBarnName(apply.getPigBarnName());
                            if(apply.getPigGroupName()!=null){
                                vo.setApplyPigGroupName(apply.getPigGroupName());
                            }else{
                                vo.setApplyPigGroupName("--");
                            }
                        }
                        BigDecimal RefundableNumber = new BigDecimal(0);
                        //???????????????????????????
                        BigDecimal LibraryQuantity = RespHelper.or500(doctorWarehouseMaterialHandleReadService.findLibraryById(mh.getRelMaterialHandleId(),mh.getMaterialName()));
                        //??????????????????????????????????????????
                        BigDecimal RetreatingQuantity = RespHelper.or500(doctorWarehouseMaterialHandleReadService.findRetreatingById(mh.getRelMaterialHandleId(),mh.getMaterialName(),stockHandle.getId()));
                        RefundableNumber = LibraryQuantity.add(RetreatingQuantity);
                        vo.setBeforeInventoryQuantity(RefundableNumber);
                    }

                    //??????????????????
                    if (stockHandle.getHandleSubType().equals( WarehouseMaterialHandleType.FORMULA_OUT.getValue())) {
                        DoctorWarehouseStockHandle sh = RespHelper.or500(doctorWarehouseStockHandleReadService.findwarehouseName(stockHandle.getRelStockHandleId()));
                        vo.setTransferInWarehouseName(sh.getWarehouseName());
                    }

                    vo.setUnitPrice(mh.getUnitPrice().divide(new BigDecimal(100), 2, BigDecimal.ROUND_HALF_UP).doubleValue());
                    vo.setAmount(mh.getUnitPrice().multiply(vo.getQuantity()).divide(new BigDecimal(100), 2, BigDecimal.ROUND_HALF_UP).doubleValue());
                    vo.setUnitPrice(mh.getUnitPrice().doubleValue());
                    vo.setAmount(mh.getAmount()!=null?mh.getAmount().doubleValue():0);

                    return vo;
                })
                .collect(Collectors.toList());


        //????????????
        try {
            //????????????
            exporter.setHttpServletResponse(request, response, "????????????");

            try (XSSFWorkbook workbook = new XSSFWorkbook()) {
                //???
                Sheet sheet = workbook.createSheet();
                sheet.createRow(0).createCell(0).setCellValue(farmName + operatorTypeName);

                Row head = sheet.createRow(1);
                head.createCell(0).setCellValue(operatorTypeName + "??????");
                head.createCell(1).setCellValue(DateUtil.toDateString(stockHandle.getHandleDate()));
                head.createCell(2).setCellValue("????????????");
                head.createCell(3).setCellValue(WareHouseType.from(stockHandle.getWarehouseType()).getDesc() + "??????");
                head.createCell(4).setCellValue("????????????");
                head.createCell(5).setCellValue(stockHandle.getWarehouseName());
                head.createCell(6).setCellValue("????????????");
                if(stockHandle.getSettlementDate()!=null&&!stockHandle.getSettlementDate().equals("")){
                    Date settlementDate = stockHandle.getSettlementDate();
                    SimpleDateFormat format=new SimpleDateFormat("yyyy???MM???");
                    String ss = format.format(settlementDate);
                    head.createCell(7).setCellValue(ss);
                }else{
                    head.createCell(7).setCellValue("");
                }
                head.createCell(8).setCellValue("????????????");
                head.createCell(9).setCellValue(stockHandle.getSerialNo());

                Row title = sheet.createRow(2);
                int pos = 3;

                //?????????-->????????????
                if (stockHandle.getHandleSubType().equals(WarehouseMaterialHandleType.IN.getValue())) {
                    title.createCell(0).setCellValue("????????????");
                    title.createCell(1).setCellValue("??????");
                    title.createCell(2).setCellValue("????????????");
                    title.createCell(3).setCellValue("??????");
                    title.createCell(4).setCellValue("??????");
                    title.createCell(5).setCellValue("??????");
                    title.createCell(6).setCellValue("???????????????");
                    title.createCell(7).setCellValue("???????????????");
                    title.createCell(8).setCellValue("??????");

                    BigDecimal totalQuantity = new BigDecimal(0);
                    double totalAmount = 0L;
                    for (StockHandleExportVo vo : exportVos) {
                        Row row = sheet.createRow(pos++);
                        row.createCell(0).setCellValue(vo.getMaterialName());
                        row.createCell(1).setCellValue(vo.getVendorName());
                        row.createCell(2).setCellValue(vo.getMaterialCode());
                        row.createCell(3).setCellValue(vo.getMaterialSpecification());
                        row.createCell(4).setCellValue(vo.getUnit());
                        row.createCell(5).setCellValue(vo.getQuantity().doubleValue());
                        row.createCell(6).setCellValue(vo.getUnitPrice());
                        row.createCell(7).setCellValue(vo.getAmount());
                        row.createCell(8).setCellValue(vo.getRemark());

                        totalQuantity = totalQuantity.add(vo.getQuantity());
                        totalAmount += vo.getAmount();
                    }

                    Row countRow = sheet.createRow(pos);
                    //????????????
                    CellRangeAddress countRange = new CellRangeAddress(pos, pos, 0, 4);
                    //????????????
                    sheet.addMergedRegion(countRange);

                    Cell countCell = countRow.createCell(0);
                    CellStyle style = workbook.createCellStyle();
                    //??????
                    style.setAlignment(HSSFCellStyle.ALIGN_CENTER);
                    countCell.setCellStyle(style);
                    countCell.setCellValue("??????");

                    countRow.createCell(5).setCellValue(totalQuantity.doubleValue());
                    countRow.createCell(7).setCellValue(totalAmount);
                    pos++;

                    //?????????-->????????????
                } else if (stockHandle.getHandleSubType().equals(WarehouseMaterialHandleType.OUT.getValue())) {
                    title.createCell(0).setCellValue("????????????");
                    title.createCell(1).setCellValue("????????????");
                    title.createCell(2).setCellValue("??????");
                    title.createCell(3).setCellValue("??????");
                    title.createCell(4).setCellValue("??????");
                    title.createCell(5).setCellValue("????????????");
                    title.createCell(6).setCellValue("????????????");
                    title.createCell(7).setCellValue("?????????");
                    title.createCell(8).setCellValue("??????");
                    title.createCell(9).setCellValue("???????????????");
                    title.createCell(10).setCellValue("???????????????");
                    title.createCell(11).setCellValue("??????");

                    BigDecimal totalQuantity = new BigDecimal(0);
                    double totalAmount = 0L;
                    for (StockHandleExportVo vo : exportVos) {
                        Row row = sheet.createRow(pos++);
                        row.createCell(0).setCellValue(vo.getMaterialName());
                        row.createCell(2).setCellValue(vo.getVendorName());
                        row.createCell(1).setCellValue(vo.getMaterialCode());
                        row.createCell(3).setCellValue(vo.getMaterialSpecification());
                        row.createCell(4).setCellValue(vo.getUnit());
                        row.createCell(5).setCellValue(vo.getApplyPigBarnName());
                        row.createCell(6).setCellValue(vo.getApplyPigGroupName());
                        row.createCell(7).setCellValue(vo.getApplyStaffName());
                        row.createCell(8).setCellValue(vo.getQuantity().doubleValue());
                        CellStyle style = workbook.createCellStyle();
                        //??????
                        style.setAlignment(HSSFCellStyle.ALIGN_CENTER);
                        row.createCell(9).setCellStyle(style);
                        row.createCell(10).setCellStyle(style);
                        if(vo.getUnitPrice()==0.0){
                            row.createCell(9).setCellValue("--");
                        }else{
                            row.createCell(9).setCellValue(vo.getUnitPrice());
                        }

                        if(vo.getAmount()==0.0){
                            row.createCell(10).setCellValue("--");
                        }else{
                            row.createCell(10).setCellValue(vo.getAmount());
                        }
                        row.createCell(11).setCellValue(vo.getRemark());

                        totalQuantity = totalQuantity.add(vo.getQuantity());
                        totalAmount += vo.getAmount();
                    }

                    Row countRow = sheet.createRow(pos);
                    CellRangeAddress cra = new CellRangeAddress(pos, pos, 0, 7);
                    sheet.addMergedRegion(cra);

                    Cell countCell = countRow.createCell(0);
                    CellStyle style = workbook.createCellStyle();
                    style.setAlignment(HSSFCellStyle.ALIGN_CENTER);
                    countCell.setCellStyle(style);
                    countRow.createCell(10).setCellStyle(style);
                    countCell.setCellValue("??????");

                    countRow.createCell(8).setCellValue(totalQuantity.doubleValue());
                    if(totalAmount==0.0){
                        countRow.createCell(10).setCellValue("--");
                    }else{
                        countRow.createCell(10).setCellValue(totalAmount);
                    }

                    pos++;

                }
                //??????
                else if (stockHandle.getHandleSubType() .equals(WarehouseMaterialHandleType.INVENTORY_PROFIT.getValue())) {
                    title.createCell(0).setCellValue("????????????");
                    title.createCell(1).setCellValue("????????????");
                    title.createCell(2).setCellValue("??????");
                    title.createCell(3).setCellValue("??????");
                    title.createCell(4).setCellValue("??????");
                    title.createCell(5).setCellValue("????????????");
                    title.createCell(6).setCellValue("????????????");
                    title.createCell(7).setCellValue("??????");
                    title.createCell(8).setCellValue("???????????????");
                    title.createCell(9).setCellValue("??????");

                    BigDecimal totalQuantity = new BigDecimal(0);
                    for (StockHandleExportVo vo : exportVos) {

                        Row row = sheet.createRow(pos++);
                        row.createCell(0).setCellValue(vo.getMaterialName());
                        row.createCell(2).setCellValue(vo.getVendorName());
                        row.createCell(1).setCellValue(vo.getMaterialCode());
                        row.createCell(3).setCellValue(vo.getMaterialSpecification());
                        row.createCell(4).setCellValue(vo.getUnit());
                        row.createCell(5).setCellValue(vo.getBeforeInventoryQuantity().doubleValue());
                        row.createCell(6).setCellValue(vo.getQuantity().doubleValue());
                        if(vo.getUnitPrice()==0.0&&vo.getAmount()==0.0){
                            CellStyle style = workbook.createCellStyle();
                            //??????
                            style.setAlignment(HSSFCellStyle.ALIGN_CENTER);
                            row.createCell(7).setCellStyle(style);
                            row.createCell(8).setCellStyle(style);
                            row.createCell(7).setCellValue("--");
                            row.createCell(8).setCellValue("--");
                        }else{
                            row.createCell(7).setCellValue(vo.getUnitPrice());
                            row.createCell(8).setCellValue(vo.getAmount());
                        }
                        row.createCell(9).setCellValue(vo.getRemark());

                        totalQuantity = vo.getQuantity();
                    }

                    Row countRow = sheet.createRow(pos);
                    //????????????
                    CellRangeAddress countRange = new CellRangeAddress(pos, pos, 0, 5);
                    //????????????
                    sheet.addMergedRegion(countRange);

                    Cell countCell = countRow.createCell(0);
                    CellStyle style = workbook.createCellStyle();
                    //??????
                    style.setAlignment(HSSFCellStyle.ALIGN_CENTER);
                    countCell.setCellStyle(style);
                    countCell.setCellValue("??????");

                    countRow.createCell(6).setCellValue(totalQuantity.doubleValue());
                    pos++;

                }
                //??????
                else if (stockHandle.getHandleSubType() .equals(WarehouseMaterialHandleType.INVENTORY_DEFICIT.getValue())) {
                        title.createCell(0).setCellValue("????????????");
                        title.createCell(1).setCellValue("????????????");
                        title.createCell(2).setCellValue("??????");
                        title.createCell(3).setCellValue("??????");
                        title.createCell(4).setCellValue("??????");
                        title.createCell(5).setCellValue("????????????");
                        title.createCell(6).setCellValue("????????????");
                        title.createCell(7).setCellValue("??????");
                        title.createCell(8).setCellValue("???????????????");
                        title.createCell(9).setCellValue("??????");

                        BigDecimal totalQuantity = new BigDecimal(0);
                       // BigDecimal inventoryQuantity = new BigDecimal(0);
                        double totalUnitPrice = 0L;
                        double totalAmount = 0L;

                        for (StockHandleExportVo vo : exportVos) {

                            Row row = sheet.createRow(pos++);
                            row.createCell(0).setCellValue(vo.getMaterialName());
                            row.createCell(2).setCellValue(vo.getVendorName());
                            row.createCell(1).setCellValue(vo.getMaterialCode());
                            row.createCell(3).setCellValue(vo.getMaterialSpecification());
                            row.createCell(4).setCellValue(vo.getUnit());
                            row.createCell(5).setCellValue(vo.getBeforeInventoryQuantity().doubleValue());
                            row.createCell(6).setCellValue(vo.getQuantity().doubleValue());
                            if(vo.getUnitPrice()==0.0&&vo.getAmount()==0.0){
                                CellStyle style = workbook.createCellStyle();
                                //??????
                                style.setAlignment(HSSFCellStyle.ALIGN_CENTER);
                                row.createCell(7).setCellStyle(style);
                                row.createCell(8).setCellStyle(style);
                                row.createCell(7).setCellValue("--");
                                row.createCell(8).setCellValue("--");
                            }else{
                                row.createCell(7).setCellValue(vo.getUnitPrice());
                                row.createCell(8).setCellValue(vo.getAmount());
                            }
                            row.createCell(9).setCellValue(vo.getRemark());

                            totalQuantity = totalQuantity.add(vo.getQuantity());
                            //inventoryQuantity = vo.getBeforeInventoryQuantity();
                            totalUnitPrice += vo.getUnitPrice();
                            totalAmount += vo.getAmount();
                        }

                        Row countRow = sheet.createRow(pos);
                        //????????????
                        CellRangeAddress countRange = new CellRangeAddress(pos, pos, 0, 5);
                        //????????????
                        sheet.addMergedRegion(countRange);

                        Cell countCell = countRow.createCell(0);
                        CellStyle style = workbook.createCellStyle();
                        //??????
                        style.setAlignment(HSSFCellStyle.ALIGN_CENTER);
                        countCell.setCellStyle(style);
                        countCell.setCellValue("??????");

                        countRow.createCell(6).setCellValue(totalQuantity.doubleValue());

                        if(totalUnitPrice==0.0){
                            countRow.createCell(7).setCellValue("--");
                        }else{
                            countRow.createCell(7).setCellValue(totalUnitPrice);
                        }

                        if(totalAmount==0.0){
                            countRow.createCell(8).setCellValue("--");
                        }else{
                            countRow.createCell(8).setCellValue(totalAmount);
                        }
                        pos++;

                    }
                //????????????
                else if (stockHandle.getHandleSubType().equals(WarehouseMaterialHandleType.TRANSFER_OUT.getValue())) {
                    title.createCell(0).setCellValue("????????????");
                    title.createCell(1).setCellValue("????????????");
                    title.createCell(2).setCellValue("??????");
                    title.createCell(3).setCellValue("??????");
                    title.createCell(4).setCellValue("??????");
                    title.createCell(5).setCellValue("????????????");
                    title.createCell(6).setCellValue("????????????");
                    title.createCell(7).setCellValue("????????????");
                    title.createCell(8).setCellValue("????????????");
                    title.createCell(9).setCellValue("??????");
                    title.createCell(10).setCellValue("???????????????");
                    title.createCell(11).setCellValue("??????");

                    BigDecimal totalQuantity = new BigDecimal(0);
                    double totalAmount = 0L;

                    for (StockHandleExportVo vo : exportVos) {
                        Row row = sheet.createRow(pos++);
                        row.createCell(0).setCellValue(vo.getMaterialName());
                        row.createCell(1).setCellValue(vo.getMaterialCode());
                        row.createCell(2).setCellValue(vo.getVendorName());
                        row.createCell(3).setCellValue(vo.getMaterialSpecification());
                        row.createCell(4).setCellValue(vo.getUnit());
                        row.createCell(5).setCellValue(vo.getBeforeInventoryQuantity().doubleValue());
                        row.createCell(6).setCellValue(vo.getTransferInFarmName());
                        row.createCell(7).setCellValue(vo.getTransferInWarehouseName());
                        row.createCell(8).setCellValue(vo.getQuantity().doubleValue());
                        if(vo.getUnitPrice()==0.0&&vo.getAmount()==0.0){
                            CellStyle style = workbook.createCellStyle();
                            //??????
                            style.setAlignment(HSSFCellStyle.ALIGN_CENTER);
                            row.createCell(9).setCellStyle(style);
                            row.createCell(10).setCellStyle(style);
                            row.createCell(9).setCellValue("--");
                            row.createCell(10).setCellValue("--");
                        }else{
                            row.createCell(9).setCellValue(vo.getUnitPrice());
                            row.createCell(10).setCellValue(vo.getAmount());
                        }

                        row.createCell(11).setCellValue(vo.getRemark());

                        totalQuantity = totalQuantity.add(vo.getQuantity());
                        totalAmount += vo.getAmount();
                    }
                    Row countRow = sheet.createRow(pos);
                    //????????????
                    CellRangeAddress countRange = new CellRangeAddress(pos, pos, 0, 5);
                    //????????????
                    sheet.addMergedRegion(countRange);

                    Cell countCell = countRow.createCell(0);
                    CellStyle style = workbook.createCellStyle();
                    //??????
                    style.setAlignment(HSSFCellStyle.ALIGN_CENTER);
                    countCell.setCellStyle(style);
                    countCell.setCellValue("?????????");

                    countRow.createCell(8).setCellValue(totalQuantity.doubleValue());

                    if(totalAmount==0.0){
                        countRow.createCell(10).setCellValue("--");
                    }else{
                        countRow.createCell(10).setCellValue(totalAmount);
                    }
                    pos++;
                }
                //????????????
                else if (stockHandle.getHandleSubType().equals(WarehouseMaterialHandleType.TRANSFER_IN.getValue())) {
                    title.createCell(0).setCellValue("????????????");
                    title.createCell(1).setCellValue("????????????");
                    title.createCell(2).setCellValue("??????");
                    title.createCell(3).setCellValue("??????");
                    title.createCell(4).setCellValue("??????");
                    title.createCell(5).setCellValue("????????????");
                    title.createCell(6).setCellValue("??????");
                    title.createCell(7).setCellValue("???????????????");
                    title.createCell(8).setCellValue("??????");

                    BigDecimal totalQuantity = new BigDecimal(0);
                    double totalAmount = 0L;

                    for (StockHandleExportVo vo : exportVos) {
                        Row row = sheet.createRow(pos++);
                        row.createCell(0).setCellValue(vo.getMaterialName());
                        row.createCell(1).setCellValue(vo.getMaterialCode());
                        row.createCell(2).setCellValue(vo.getVendorName());
                        row.createCell(3).setCellValue(vo.getMaterialSpecification());
                        row.createCell(4).setCellValue(vo.getUnit());
                        row.createCell(5).setCellValue(vo.getQuantity().doubleValue());
                        if(vo.getUnitPrice()==0.0&&vo.getAmount()==0.0){
                           CellStyle style = workbook.createCellStyle();
                           //??????
                           style.setAlignment(HSSFCellStyle.ALIGN_CENTER);
                           row.createCell(6).setCellStyle(style);
                           row.createCell(7).setCellStyle(style);
                           row.createCell(6).setCellValue("--");
                           row.createCell(7).setCellValue("--");
                       }else{
                           row.createCell(6).setCellValue(vo.getUnitPrice());
                           row.createCell(7).setCellValue(vo.getAmount());
                       }
                        row.createCell(8).setCellValue(vo.getRemark());

                        totalQuantity = totalQuantity.add(vo.getQuantity());
                        totalAmount += vo.getAmount();
                    }

                    Row countRow = sheet.createRow(pos);
                    //????????????
                    CellRangeAddress countRange = new CellRangeAddress(pos, pos, 0, 4);
                    //????????????
                    sheet.addMergedRegion(countRange);

                    Cell countCell = countRow.createCell(0);
                    CellStyle style = workbook.createCellStyle();
                    //??????
                    style.setAlignment(HSSFCellStyle.ALIGN_CENTER);
                    countCell.setCellStyle(style);
                    countCell.setCellValue("??????");

                    countRow.createCell(5).setCellValue(totalQuantity.doubleValue());
                    countRow.createCell(7).setCellValue(totalAmount);

                    pos++;
                }
                //??????????????????
                else if (stockHandle.getHandleSubType().equals(WarehouseMaterialHandleType.FORMULA_OUT.getValue())) {
                    title.createCell(0).setCellValue("????????????");
                    title.createCell(1).setCellValue("????????????");
                    title.createCell(2).setCellValue("??????");
                    title.createCell(3).setCellValue("??????");
                    title.createCell(4).setCellValue("??????");
                    title.createCell(5).setCellValue("????????????");
                    title.createCell(6).setCellValue("??????");
                    title.createCell(1).setCellValue("???????????????");
                    title.createCell(8).setCellValue("??????");

                    for (StockHandleExportVo vo : exportVos) {
                        Row row = sheet.createRow(pos++);
                        row.createCell(0).setCellValue(vo.getMaterialName());
                        row.createCell(1).setCellValue(vo.getMaterialCode());
                        row.createCell(2).setCellValue(vo.getVendorName());
                        row.createCell(3).setCellValue(vo.getMaterialSpecification());
                        row.createCell(4).setCellValue(vo.getUnit());
                        row.createCell(5).setCellValue(vo.getQuantity().doubleValue());
                        if(vo.getUnitPrice()==0.0&&vo.getAmount()==0.0){
                            CellStyle style = workbook.createCellStyle();
                            //??????
                            style.setAlignment(HSSFCellStyle.ALIGN_CENTER);
                            row.createCell(6).setCellStyle(style);
                            row.createCell(7).setCellStyle(style);
                            row.createCell(6).setCellValue("--");
                            row.createCell(7).setCellValue("--");
                        }else{
                            row.createCell(6).setCellValue(vo.getUnitPrice());
                            row.createCell(7).setCellValue(vo.getAmount());
                        }
                        row.createCell(8).setCellValue(vo.getRemark());
                    }

                }
                //??????????????????
                else if (stockHandle.getHandleSubType().equals(WarehouseMaterialHandleType.FORMULA_IN.getValue())) {
                    title.createCell(0).setCellValue("????????????");
                    title.createCell(1).setCellValue("????????????");
                    title.createCell(2).setCellValue("??????");
                    title.createCell(3).setCellValue("??????");
                    title.createCell(4).setCellValue("??????");
                    title.createCell(5).setCellValue("????????????");
                    title.createCell(6).setCellValue("??????");
                    title.createCell(7).setCellValue("???????????????");
                    title.createCell(8).setCellValue("??????");

                    for (StockHandleExportVo vo : exportVos) {
                        Row row = sheet.createRow(pos++);
                        row.createCell(0).setCellValue(vo.getMaterialName());
                        row.createCell(1).setCellValue(vo.getMaterialCode());
                        row.createCell(2).setCellValue(vo.getVendorName());
                        row.createCell(3).setCellValue(vo.getMaterialSpecification());
                        row.createCell(4).setCellValue(vo.getUnit());
                        row.createCell(5).setCellValue(vo.getQuantity().doubleValue());
                        if(vo.getUnitPrice()==0.0&&vo.getAmount()==0.0){
                            CellStyle style = workbook.createCellStyle();
                            //??????
                            style.setAlignment(HSSFCellStyle.ALIGN_CENTER);
                            row.createCell(6).setCellStyle(style);
                            row.createCell(7).setCellStyle(style);
                            row.createCell(6).setCellValue("--");
                            row.createCell(7).setCellValue("--");
                        }else{
                            row.createCell(6).setCellValue(vo.getUnitPrice());
                            row.createCell(7).setCellValue(vo.getAmount());
                        }
                        row.createCell(8).setCellValue(vo.getRemark());
                    }
                }
                //????????????
                else if (stockHandle.getHandleSubType().equals(WarehouseMaterialHandleType.RETURN.getValue())) {
                    title.createCell(0).setCellValue("????????????");
                    title.createCell(1).setCellValue("????????????");
                    title.createCell(2).setCellValue("??????");
                    title.createCell(3).setCellValue("??????");
                    title.createCell(4).setCellValue("??????");
                    title.createCell(5).setCellValue("????????????");
                    title.createCell(6).setCellValue("????????????");
                    title.createCell(7).setCellValue("????????????");
                    title.createCell(8).setCellValue("????????????");
                    title.createCell(9).setCellValue("??????(???)");
                    title.createCell(10).setCellValue("??????(???)");
                    title.createCell(11).setCellValue("??????");

                    BigDecimal totalQuantity = new BigDecimal(0);
                    double totalAmount = 0L;
                    for (StockHandleExportVo vo : exportVos) {
                        Row row = sheet.createRow(pos++);
                        row.createCell(0).setCellValue(vo.getMaterialName());
                        row.createCell(2).setCellValue(vo.getVendorName());
                        row.createCell(1).setCellValue(vo.getMaterialCode());
                        row.createCell(3).setCellValue(vo.getMaterialSpecification());
                        row.createCell(4).setCellValue(vo.getUnit());
                        row.createCell(5).setCellValue(vo.getApplyPigBarnName());
                        row.createCell(6).setCellValue(vo.getApplyPigGroupName());
                        row.createCell(7).setCellValue(vo.getBeforeInventoryQuantity().doubleValue());
                        row.createCell(8).setCellValue(vo.getQuantity().doubleValue());
                        CellStyle style = workbook.createCellStyle();
                        //??????
                        style.setAlignment(HSSFCellStyle.ALIGN_CENTER);
                        row.createCell(9).setCellStyle(style);
                        row.createCell(10).setCellStyle(style);
                        if(vo.getUnitPrice()==0.0){
                            row.createCell(9).setCellValue("--");
                        }else{
                            row.createCell(9).setCellValue(vo.getUnitPrice());
                        }

                        if(vo.getAmount()==0.0){
                            row.createCell(10).setCellValue("--");
                        }else{
                            row.createCell(10).setCellValue(vo.getAmount());
                        }
                        row.createCell(11).setCellValue(vo.getRemark());

                        totalQuantity = totalQuantity.add(vo.getQuantity());
                        totalAmount += vo.getAmount();
                    }

                    Row countRow = sheet.createRow(pos);
                    CellRangeAddress cra = new CellRangeAddress(pos, pos, 0, 5);
                    sheet.addMergedRegion(cra);

                    Cell countCell = countRow.createCell(0);
                    CellStyle style = workbook.createCellStyle();
                    style.setAlignment(HSSFCellStyle.ALIGN_CENTER);
                    countCell.setCellStyle(style);
                    countRow.createCell(8).setCellStyle(style);
                    countCell.setCellValue("??????");

                    countRow.createCell(8).setCellValue(totalQuantity.doubleValue());

                    if(totalAmount==0.0){
                        countRow.createCell(10).setCellValue("--");
                    }else{
                        countRow.createCell(10).setCellValue(totalAmount);
                    }

                    pos++;

                }

                Row foot = sheet.createRow(pos);
                foot.createCell(0).setCellValue("?????????");
                foot.createCell(1).setCellValue(wareHouse.getManagerName());
                foot.createCell(2).setCellValue("?????????");
                foot.createCell(3).setCellValue(stockHandle.getOperatorName());
                foot.createCell(4).setCellValue("????????????");
                foot.createCell(5).setCellValue(farm.getOrgName());

                workbook.write(response.getOutputStream());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }


    }
    @RequestMapping(method = RequestMethod.GET, value = "/stockPage")
    public Paging<DoctorWarehouseStockHandle> stockPage(
            @RequestParam(required = false) Integer pageNo,
            @RequestParam(required = false) Integer pageSize,
            @RequestParam(required = false) Integer farmId,
            @RequestParam(required = false,value = "warehouseId") String warehouseId,
            @RequestParam(required = false,value = "operatorId") String operatorId,
            @RequestParam(required = false,value = "handleSubType") Integer handleSubType,
            @RequestParam(required = false,value = "handleDateStart") Date handleDateStart,
            @RequestParam(required = false,value = "handleDateEnd") Date handleDateEnd,
            @RequestParam(required = false,value = "updatedAtStart") Date updatedAtStart,
            @RequestParam(required = false,value = "updatedAtEnd") Date updatedAtEnd
    ) {

        if (null != handleDateStart && null != handleDateEnd && handleDateStart.after(handleDateEnd))
            throw new JsonResponseException("start.date.after.end.date");

        if (null != updatedAtStart && null != updatedAtEnd && updatedAtStart.after(updatedAtEnd))
            throw new JsonResponseException("start.date.after.end.date");

        Map<String, Object> params = new HashMap<>();
        if(warehouseId!=null&&!"".equals(warehouseId))
        params.put("warehouseId", warehouseId);
        if(operatorId!=null&&!"".equals(operatorId))
        params.put("operatorId", operatorId);
        params.put("handleSubType", handleSubType);
        params.put("handleDateStart", handleDateStart);
        params.put("handleDateEnd", handleDateEnd);
        params.put("updatedAtStart", updatedAtStart);
        params.put("farmId",farmId);
        params.put("updatedAtEnd", updatedAtEnd);
        return RespHelper.or500(doctorWarehouseStockHandleReadService.paging(pageNo, pageSize, params));
    }

}
