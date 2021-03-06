package io.terminus.doctor.web.front.warehouse.controller;

import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.exception.ServiceException;
import io.terminus.common.model.BaseUser;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import io.terminus.common.utils.Arguments;
import io.terminus.common.utils.BeanMapper;
import io.terminus.doctor.basic.dto.DoctorMaterialConsumeProviderDto;
import io.terminus.doctor.basic.dto.DoctorMaterialInWareHouseDto;
import io.terminus.doctor.basic.dto.DoctorMoveMaterialDto;
import io.terminus.doctor.basic.dto.DoctorWareHouseDto;
import io.terminus.doctor.basic.model.*;
import io.terminus.doctor.basic.service.DoctorBasicMaterialReadService;
import io.terminus.doctor.basic.service.DoctorBasicReadService;
import io.terminus.doctor.basic.service.DoctorMaterialConsumeProviderReadService;
import io.terminus.doctor.basic.service.DoctorMaterialInWareHouseReadService;
import io.terminus.doctor.basic.service.DoctorMaterialInWareHouseWriteService;
import io.terminus.doctor.basic.service.DoctorMaterialPriceInWareHouseReadService;
import io.terminus.doctor.basic.service.DoctorWareHouseReadService;
import io.terminus.doctor.common.enums.WareHouseType;
import io.terminus.doctor.common.utils.DateUtil;
import io.terminus.doctor.common.utils.RespHelper;
import io.terminus.doctor.event.model.DoctorBarn;
import io.terminus.doctor.event.model.DoctorMasterialDatailsGroup;
import io.terminus.doctor.event.service.DoctorBarnReadService;
import io.terminus.doctor.event.service.DoctorGroupMaterialReadServer;
import io.terminus.doctor.event.service.DoctorGroupReadService;
import io.terminus.doctor.user.model.DoctorFarm;
import io.terminus.doctor.user.service.DoctorFarmReadService;
import io.terminus.doctor.web.core.export.Exporter;
import io.terminus.doctor.web.front.event.service.DoctorGroupWebService;
import io.terminus.doctor.web.front.warehouse.dto.*;
import io.terminus.pampas.common.UserUtil;
import io.terminus.parana.user.model.User;
import io.terminus.parana.user.service.UserReadService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.time.DateUtils;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.util.*;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkState;
import static java.util.Objects.isNull;

/**
 * Created by yaoqijun.
 * Date:2016-05-25
 * Email:yaoqj@terminus.io
 * Descirbe: ?????????????????????????????????
 */
@Slf4j
@Controller
@RequestMapping("/api/doctor/warehouse/event")
public class DoctorWareHouseEvents {

    private final DoctorMaterialInWareHouseWriteService doctorMaterialInWareHouseWriteService;

    private final DoctorMaterialInWareHouseReadService doctorMaterialInWareHouseReadService;

    private final UserReadService<User> userReadService;

    private final DoctorWareHouseReadService doctorWareHouseReadService;

    private final DoctorBasicMaterialReadService doctorBasicMaterialReadService;

    private final DoctorBasicReadService doctorBasicReadService;

    private final DoctorFarmReadService doctorFarmReadService;

    private final DoctorGroupWebService doctorGroupWebService;

    private final DoctorBarnReadService doctorBarnReadService;

    @Autowired
    private Exporter exporter;

    @RpcConsumer
    private DoctorMaterialPriceInWareHouseReadService materialPriceInWareHouseReadService;
    @RpcConsumer
    private DoctorMaterialConsumeProviderReadService materialConsumeProviderReadService;
    @RpcConsumer
    private DoctorGroupReadService doctorGroupReadService;
    @RpcConsumer
    private DoctorGroupMaterialReadServer doctorGroupMaterialReadServer;

    @Autowired
    public DoctorWareHouseEvents(DoctorMaterialInWareHouseWriteService doctorMaterialInWareHouseWriteService,
                                 DoctorMaterialInWareHouseReadService doctorMaterialInWareHouseReadService,
                                 UserReadService<User> userReadService,
                                 DoctorBasicMaterialReadService doctorBasicMaterialReadService,
                                 DoctorWareHouseReadService doctorWareHouseReadService,
                                 DoctorFarmReadService doctorFarmReadService,
                                 DoctorBasicReadService doctorBasicReadService,
                                 DoctorGroupWebService doctorGroupWebService, DoctorBarnReadService doctorBarnReadService){
        this.doctorMaterialInWareHouseWriteService = doctorMaterialInWareHouseWriteService;
        this.doctorMaterialInWareHouseReadService = doctorMaterialInWareHouseReadService;
        this.userReadService = userReadService;
        this.doctorBasicMaterialReadService = doctorBasicMaterialReadService;
        this.doctorWareHouseReadService = doctorWareHouseReadService;
        this.doctorFarmReadService = doctorFarmReadService;
        this.doctorBasicReadService = doctorBasicReadService;
        this.doctorGroupWebService = doctorGroupWebService;
        this.doctorBarnReadService = doctorBarnReadService;
    }
    private final static Integer DETAILS = 2;
    private final static Integer WHARE = 1;
    private final static Integer GROUP = 0;
    /**
     * ???????????????????????? ????????????
     * @param farmId
     * @param wareHouseId
     * @return
     */
    @RequestMapping(value = "/list", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public List<DoctorMaterialInWareHouse> listDoctorMaterialInWareHouse(@RequestParam("farmId") Long farmId,
                                                                         @RequestParam("wareHouseId") Long wareHouseId){
        return RespHelper.or500(doctorMaterialInWareHouseReadService.queryDoctorMaterialInWareHouse(farmId, wareHouseId));
    }

    /**
     * ??????????????????????????????????????????
     * @param farmId
     * @param wareHouseId
     * @param pageNo
     * @param pageSize
     * @return
     */
    @RequestMapping(value = "/paging", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public Paging<DoctorMaterialInWareHouseDto> pagingDoctorMaterialInWareHouse(@RequestParam("farmId") Long farmId,
                                                                                @RequestParam(name = "wareHouseId", required = false) Long wareHouseId,
                                                                                @RequestParam(name = "materialId", required = false) Long materialId,
                                                                                @RequestParam(name = "materialName", required = false) String materialName,
                                                                                @RequestParam("pageNo") Integer pageNo,
                                                                                @RequestParam("pageSize") Integer pageSize){

        return pagingWareHouseMaterial(farmId, wareHouseId, materialId, materialName, pageNo, pageSize);
    }

    /**
     * ??????????????????
     * @param params ????????????
     * @param request HttpServletRequest
     * @param response HttpServletResponse
     */
    @RequestMapping(value = "/material/export", method = RequestMethod.GET)
    public void wareHouseMaterialExport(@RequestParam Map<String, String> params,
                                        HttpServletRequest request,
                                        HttpServletResponse response) {
        try {
            exporter.export(wareHouseMaterialHandle(params), "web-material-export", request, response);
        } catch (Exception e) {
            log.error("ware.house.event.export.failed, cause:{}", Throwables.getStackTraceAsString(e));
            throw new JsonResponseException("ware.house.material.export.fail");
        }
    }

    /**
     * @param params
     * @return
     */

    @RequestMapping(value = "/material/message", method = RequestMethod.GET)
    @ResponseBody
    public Paging<DoctorWareHouseMaterialData> wareHouseMaterialMessage(@RequestParam Map<String, String> params) {

        List<DoctorWareHouseMaterialData> doctorWareHouseMaterialDatas = wareHouseMaterialHandle(params);

        return new Paging<>((long)doctorWareHouseMaterialDatas.size(), doctorWareHouseMaterialDatas);

    }

    public List<DoctorWareHouseMaterialData> wareHouseMaterialHandle(Map<String, String> params) {
        List<DoctorWareHouseMaterialData> doctorWareHouseMaterialData1 = materialExportData(params, 0);
        List<DoctorWareHouseMaterialData> doctorWareHouseMaterialData2 = materialExportData(params, 1);
        List<DoctorWareHouseMaterialData> doctorWareHouseMaterialData = Lists.newArrayList();
        for (DoctorWareHouseMaterialData doctorDate1 : doctorWareHouseMaterialData1) {
            int number = 0;
            for (DoctorWareHouseMaterialData doctorDate2 : doctorWareHouseMaterialData2) {
                if (doctorDate1.getMaterialName().equals(doctorDate2.getMaterialName())) {
                    doctorDate1.setCurrentAmount(doctorDate2.getMonthBeginAmount());
                    doctorDate1.setLotNumber(doctorDate2.getMonthBeginNumber());
                    doctorDate1.setInAmount(doctorDate1.getInAmount() - doctorDate2.getInAmount());
                    doctorDate1.setInCount(doctorDate1.getInCount() - doctorDate2.getInCount());
                    doctorDate1.setOutCount(doctorDate1.getOutCount() - doctorDate2.getOutCount());
                    doctorDate1.setOutAmount(doctorDate1.getOutAmount() - doctorDate2.getOutAmount());
                    doctorWareHouseMaterialData.add(doctorDate1);
                    break;
                }
                number++;
            }
            if (number == doctorWareHouseMaterialData2.size()) {
                doctorWareHouseMaterialData.add(doctorDate1);
            }
        }
        
        return doctorWareHouseMaterialData;
    }

    @RequestMapping(value = "/materialInWareHouse/delete", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public Boolean deleteMaterialInWareHouse(@RequestParam("materialInWareHouseId") Long materialInWareHouseId){
        Long userId;
        String userName;
        try{
            userId = UserUtil.getUserId();
            Response<User> userResponse = userReadService.findById(userId);
            checkState(userResponse.isSuccess(), "query.userInfo.error");
            userName = userResponse.getResult().getName();
        }catch (Exception e){
            log.error("get user info fail, cause:{}", Throwables.getStackTraceAsString(e));
            throw new JsonResponseException(e.getMessage());
        }
        return RespHelper.or500(
                doctorMaterialInWareHouseWriteService.deleteMaterialInWareHouseInfo(
                        materialInWareHouseId, userId, userName));
    }

    /**
     * ?????????????????????
     * @param dto
     * @return
     */
    @RequestMapping(value = "/consume", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public Long createConsumeEvent(@RequestBody @Valid DoctorConsumeProviderInputDto dto){
        return RespHelper.or500(doctorMaterialInWareHouseWriteService.consumeMaterialInfo(buildMaterialConsumeInfo(dto)));
    }

    /**
     * ????????????
     * @param dtoList
     * @return
     */
    @RequestMapping(value = "/batch/consume", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public Boolean batchCreateConsumeEvents(@RequestBody List<DoctorConsumeProviderInputDto> dtoList) {
        if (Arguments.isNullOrEmpty(dtoList)) {
            return Boolean.FALSE;
        }
        List<DoctorMaterialConsumeProviderDto> providerDtoList = dtoList.stream().map(this::buildMaterialConsumeInfo).collect(Collectors.toList());
        return RespHelper.or500(doctorMaterialInWareHouseWriteService.batchConsumeMaterialInfo(providerDtoList));
    }


    /**
     * ?????????????????????
     * @param materialId
     * @param warehouseId
     * @param unitId
     * @return
     */
    @RequestMapping(value = "/initMaterialInWarehouse", method = {RequestMethod.GET, RequestMethod.POST})
    @ResponseBody
    public boolean initMaterialInWarehouse(@RequestParam Long materialId, @RequestParam Long warehouseId,
                                           @RequestParam(required = false) Long unitId){
        DoctorBasicMaterial material = RespHelper.or500(doctorBasicMaterialReadService.findBasicMaterialById(materialId));
        if(material == null){
            throw new JsonResponseException("basicMaterial.not.found");
        }

        DoctorWareHouse wareHouse = RespHelper.or500(doctorWareHouseReadService.findById(warehouseId));
        if(wareHouse == null){
            throw new JsonResponseException("warehosue.not.found");
        }

        if(!wareHouse.getType().equals(material.getType())){
            throw new JsonResponseException("warehouse.material.type.not.match"); // ??????????????????????????????
        }

        DoctorMaterialInWareHouse in = RespHelper.or500(doctorMaterialInWareHouseReadService.queryByMaterialWareHouseIds(wareHouse.getFarmId(), materialId, warehouseId));
        if(in != null){
            throw new JsonResponseException("materialInWarehouse.exist");
        }

        in = new DoctorMaterialInWareHouse();
        // ??????.??????.?????????, ????????????????????????
        if(Objects.equals(material.getType(), WareHouseType.CONSUME.getKey())
                || Objects.equals(material.getType(), WareHouseType.MEDICINE.getKey())
                || Objects.equals(material.getType(), WareHouseType.VACCINATION.getKey())){
            if(unitId != null){
                DoctorBasic doctorBasic = RespHelper.or500(doctorBasicReadService.findBasicByIdFilterByFarmId(wareHouse.getFarmId(), unitId));
                if(doctorBasic == null || !Objects.equals(doctorBasic.getType(), DoctorBasic.Type.UNIT.getValue())){
                    throw new JsonResponseException("unit.miss");
                }
                in.setUnitName(doctorBasic.getName());
            }else{
                throw new JsonResponseException("unit.miss");
            }
        }else{
            in.setUnitName("??????");
        }
        in.setFarmId(wareHouse.getFarmId());
        in.setFarmName(wareHouse.getFarmName());
        in.setWareHouseId(warehouseId);
        in.setWareHouseName(wareHouse.getWareHouseName());
        in.setMaterialId(materialId);
        in.setMaterialName(material.getName());
        in.setType(material.getType());
        in.setLotNumber(0D);
        BaseUser user = UserUtil.getCurrentUser();
        in.setCreatorId(user.getId());
        in.setCreatorName(user.getName());
        in.setUpdatorId(user.getId());
        in.setUpdatorName(user.getName());
        return RespHelper.or500(doctorMaterialInWareHouseWriteService.create(in));
    }

    /**
     * ????????????????????????
     * @param dto
     * @return
     */
    @RequestMapping(value = "/provider", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public Long createProviderEvent(@RequestBody @Valid DoctorConsumeProviderInputDto dto){
        return RespHelper.or500(doctorMaterialInWareHouseWriteService.providerMaterialInfo(buildMaterialProviderInfo(dto)));
    }

    /**
     * ????????????
     * @param dtoList
     * @return
     */
    @RequestMapping(value = "/batch/provider", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public Boolean batchCreateProviderEvents(@RequestBody List<DoctorConsumeProviderInputDto> dtoList){
        if (Arguments.isNullOrEmpty(dtoList)) {
            return Boolean.FALSE;
        }
        List<DoctorMaterialConsumeProviderDto> providerDtoList = dtoList.stream().map(this::buildMaterialProviderInfo).collect(Collectors.toList());
        return RespHelper.or500(doctorMaterialInWareHouseWriteService.batchProviderMaterialInfo(providerDtoList));

    }

    /**
     * ?????????????????????????????????
     */
    @RequestMapping(value = "/moveMaterial", method = RequestMethod.POST)
    @ResponseBody
    public boolean moveMaterial(@RequestBody DoctorMoveMaterialInputDto dto){
        RespHelper.or500(doctorMaterialInWareHouseWriteService.moveMaterial(buildMoveMaterialInfo(dto)));
        return true;
    }

    /**
     * ????????????
     */
    @RequestMapping(value = "/batch/moveMaterial", method = RequestMethod.POST)
    @ResponseBody
    public Boolean moveMaterial(@RequestBody List<DoctorMoveMaterialInputDto> dtoList){
        if (Arguments.isNullOrEmpty(dtoList)) {
            return Boolean.FALSE;
        }
        List<DoctorMoveMaterialDto> moveMaterialDtoList = dtoList.stream().map(this::buildMoveMaterialInfo).collect(Collectors.toList());
        return RespHelper.or500(doctorMaterialInWareHouseWriteService.batchMoveMaterial(moveMaterialDtoList));
    }

    /**
     * ??????????????????
     * @param farmId ??????id
     * @param warehouseId ??????id
     * @param materialId ??????id
     * @param count ?????????
     * @return
     */
    @RequestMapping(value = "/inventory", method = RequestMethod.POST)
    @ResponseBody
    public boolean inventory(@RequestParam Long farmId, @RequestParam Long warehouseId, @RequestParam Long materialId,
                             @RequestParam Double count, @RequestParam(required = false) String eventAt){
        DoctorMaterialInWareHouse materialInWareHouse = RespHelper.or500(doctorMaterialInWareHouseReadService.queryByMaterialWareHouseIds(farmId, materialId, warehouseId));
        DoctorConsumeProviderInputDto dto = DoctorConsumeProviderInputDto.builder()
                .farmId(farmId).wareHouseId(warehouseId).materialId(materialId)
                .eventAt(eventAt == null ? new Date() : DateUtil.toDate(eventAt))
                .build();
        if (materialInWareHouse.getLotNumber() > count){
            // ?????? (??????)
            dto.setCount(materialInWareHouse.getLotNumber() - count);
            dto.setEventType(DoctorMaterialConsumeProvider.EVENT_TYPE.PANKUI.getValue());
            this.createConsumeEvent(dto);
        } else if(materialInWareHouse.getLotNumber() < count){
            // ??????(??????)
            dto.setCount(count - materialInWareHouse.getLotNumber());
            dto.setEventType(DoctorMaterialConsumeProvider.EVENT_TYPE.PANYING.getValue());
            this.createProviderEvent(dto);
        } else {
            return true;
        }
        return true;
    }

    /**
     * ????????????
     * @param inputDtoList
     * @return
     */
    @RequestMapping(value = "/batch/inventory", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public Boolean batchInventory(@RequestBody List<DoctorInventoryInputDto> inputDtoList) {
        List<DoctorMaterialConsumeProviderDto> dtoList = inputDtoList.stream().map(inputDto -> {
            DoctorMaterialInWareHouse materialInWareHouse = RespHelper.or500(doctorMaterialInWareHouseReadService.queryByMaterialWareHouseIds(inputDto.getFarmId(), inputDto.getMaterialId(), inputDto.getWareHouseId()));
            DoctorConsumeProviderInputDto dto = DoctorConsumeProviderInputDto.builder()
                    .farmId(inputDto.getFarmId()).wareHouseId(inputDto.getWareHouseId()).materialId(inputDto.getMaterialId())
                    .eventAt(inputDto.getEventAt() == null ? new Date() : DateUtil.toDate(inputDto.getEventAt()))
                    .build();
            Double count = inputDto.getCount();
            if (materialInWareHouse.getLotNumber() > count) {
                // ?????? (??????)
                dto.setCount(materialInWareHouse.getLotNumber() - count);
                dto.setEventType(DoctorMaterialConsumeProvider.EVENT_TYPE.PANKUI.getValue());
                return buildMaterialConsumeInfo(dto);
            } else if (materialInWareHouse.getLotNumber() < count) {
                // ??????(??????)
                dto.setCount(count - materialInWareHouse.getLotNumber());
                dto.setEventType(DoctorMaterialConsumeProvider.EVENT_TYPE.PANYING.getValue());
                return buildMaterialProviderInfo(dto);
            } else {
                return null;
            }
        }).filter(dto -> dto != null).collect(Collectors.toList());
        return RespHelper.or500(doctorMaterialInWareHouseWriteService.batchInventory(dtoList));
    }

    /**
     * ??????????????????
     * @param eventId DoctorMaterialConsumeProvider???id
     * @return
     */
    @RequestMapping(value = "/rollback", method = RequestMethod.GET)
    @ResponseBody
    public boolean rollback(@RequestParam Long eventId){
        return RespHelper.or500(doctorMaterialInWareHouseWriteService.rollback(eventId));
    }

    /**
     * ????????????????????????
     * @param dto
     * @return
     */
    private DoctorMaterialConsumeProviderDto buildMaterialProviderInfo(DoctorConsumeProviderInputDto dto) {
        DoctorMaterialConsumeProviderDto doctorMaterialConsumeProviderDto;
        DoctorMaterialConsumeProvider.EVENT_TYPE eventType = DoctorMaterialConsumeProvider.EVENT_TYPE.from(dto.getEventType());
        if(eventType == null || !eventType.isIn()){
            throw new JsonResponseException("event.type.error");
        }
        try{
            DoctorWareHouseDto doctorWareHouseDto = RespHelper.orServEx(doctorWareHouseReadService.queryDoctorWareHouseById(dto.getWareHouseId()));

            Long userId = dto.getStaffId() != null ? dto.getStaffId() : UserUtil.getUserId();

            DoctorBasicMaterial doctorBasicMaterial = RespHelper.orServEx(doctorBasicMaterialReadService.findBasicMaterialById(dto.getMaterialId()));
            DoctorFarm doctorFarm = RespHelper.orServEx(doctorFarmReadService.findFarmById(dto.getFarmId()));

            doctorMaterialConsumeProviderDto = DoctorMaterialConsumeProviderDto.builder()
                    .actionType(eventType.getValue()).type(doctorBasicMaterial.getType())
                    .farmId(doctorFarm.getId()).farmName(doctorFarm.getName())
                    .wareHouseId(doctorWareHouseDto.getWarehouseId()).wareHouseName(doctorWareHouseDto.getWarehouseName())
                    .materialTypeId(doctorBasicMaterial.getId()).materialName(doctorBasicMaterial.getName())
                    .staffId(userId).staffName(RespHelper.orServEx(doctorGroupWebService.findRealName(userId)))
                    .count(dto.getCount())
                    .eventTime(dto.getEventAt() == null ? new Date() : dto.getEventAt())
                    .providerFactoryId(dto.getFactoryId()).providerFactoryName(dto.getFactoryName())
                    .unitPrice(dto.getUnitPrice())
                    .build();

            // ??????.??????.?????????, ????????????????????????
            if(Objects.equals(doctorBasicMaterial.getType(), WareHouseType.CONSUME.getKey())
                    || Objects.equals(doctorBasicMaterial.getType(), WareHouseType.MEDICINE.getKey())
                    || Objects.equals(doctorBasicMaterial.getType(), WareHouseType.VACCINATION.getKey())){
                if(dto.getUnitId() != null){
                    DoctorBasic doctorBasic = RespHelper.or500(doctorBasicReadService.findBasicByIdFilterByFarmId(dto.getFarmId(), dto.getUnitId()));
                    doctorMaterialConsumeProviderDto.setUnitName(doctorBasic.getName());
                    doctorMaterialConsumeProviderDto.setUnitId(doctorBasic.getId());
                }else{
                    DoctorMaterialInWareHouse materialInWareHouse = RespHelper.orServEx(
                            doctorMaterialInWareHouseReadService.queryByMaterialWareHouseIds(doctorFarm.getId(), dto.getMaterialId(), dto.getWareHouseId())
                    );
                    if(materialInWareHouse == null){
                        throw new ServiceException("unit.miss");
                    }
                }
            }
            return doctorMaterialConsumeProviderDto;
        }catch (Exception e){
            log.error("provider material fail, cause:{}", Throwables.getStackTraceAsString(e));
            throw new JsonResponseException(e.getMessage());
        }
    }

    /**
     * ????????????????????????
     * @param dto ????????????????????????
     * @return
     */
    private DoctorMaterialConsumeProviderDto buildMaterialConsumeInfo(DoctorConsumeProviderInputDto dto) {
        DoctorMaterialConsumeProviderDto doctorMaterialConsumeProviderDto;
        DoctorMaterialConsumeProvider.EVENT_TYPE eventType = DoctorMaterialConsumeProvider.EVENT_TYPE.from(dto.getEventType());
        if(eventType == null || !eventType.isOut()){
            throw new JsonResponseException("event.type.error");
        }
        try{

            DoctorMaterialInWareHouse doctorMaterialInWareHouse = RespHelper.orServEx(doctorMaterialInWareHouseReadService.queryByMaterialWareHouseIds(
                    dto.getFarmId(),dto.getMaterialId(),dto.getWareHouseId()));
            checkState(!isNull(doctorMaterialInWareHouse), "input.materialInfo.error");

            Long currentUserId = dto.getStaffId() != null ? dto.getStaffId() : UserUtil.getUserId();

            DoctorBasicMaterial doctorBasicMaterial = RespHelper.orServEx(doctorBasicMaterialReadService.findBasicMaterialById(dto.getMaterialId()));

            doctorMaterialConsumeProviderDto = DoctorMaterialConsumeProviderDto.builder()
                    .actionType(eventType.getValue()).type(doctorMaterialInWareHouse.getType())
                    .farmId(doctorMaterialInWareHouse.getFarmId()).farmName(doctorMaterialInWareHouse.getFarmName())
                    .wareHouseId(doctorMaterialInWareHouse.getWareHouseId()).wareHouseName(doctorMaterialInWareHouse.getWareHouseName())
                    .materialTypeId(doctorMaterialInWareHouse.getMaterialId()).materialName(doctorMaterialInWareHouse.getMaterialName())
                    .barnId(dto.getBarnId()).barnName(dto.getBarnName())
                    .groupId(dto.getGroupId()).groupCode(dto.getGroupCode())
                    .staffId(currentUserId).staffName(RespHelper.orServEx(doctorGroupWebService.findRealName(currentUserId)))
                    .count(dto.getCount()).consumeDays(dto.getConsumeDays())
                    .eventTime(dto.getEventAt() == null ? new Date() : dto.getEventAt())
                    .unitId(doctorBasicMaterial.getUnitId()).unitName(doctorBasicMaterial.getUnitName())
                    .unitGroupId(doctorBasicMaterial.getUnitGroupId()).unitGroupName(doctorBasicMaterial.getUnitGroupName())
                    .build();
            return doctorMaterialConsumeProviderDto;
        }catch (Exception e){
            log.error("consume material fail, cause:{}", Throwables.getStackTraceAsString(e));
            throw new JsonResponseException(e.getMessage());
        }
    }

    /**
     * ????????????????????????
     * @param dto ??????????????????
     * @return ????????????
     */
    private DoctorMoveMaterialDto buildMoveMaterialInfo(DoctorMoveMaterialInputDto dto) {
        if(dto.getMoveQuantity() == null || dto.getMoveQuantity() <= 0){
            throw new JsonResponseException("quantity.invalid");
        }
        if(dto.getFromWareHouseId().equals(dto.getToWareHouseId())){
            throw new JsonResponseException("fromWareHouseId.equal.toWareHouseId"); // ?????????????????????????????????????????????
        }
        DoctorBasicMaterial material = RespHelper.or500(doctorBasicMaterialReadService.findBasicMaterialById(dto.getMaterialId()));
        DoctorFarm farm = RespHelper.or500(doctorFarmReadService.findFarmById(dto.getFarmId()));
        DoctorWareHouse consumeWarehouse = RespHelper.or500(doctorWareHouseReadService.findById(dto.getFromWareHouseId()));
        DoctorWareHouse toWarehouse = RespHelper.or500(doctorWareHouseReadService.findById(dto.getToWareHouseId()));
        DoctorMaterialInWareHouse materialInWareHouse = RespHelper.or500(doctorMaterialInWareHouseReadService.queryByMaterialWareHouseIds(dto.getFarmId(), dto.getMaterialId(), dto.getFromWareHouseId()));
        if(materialInWareHouse == null){
            throw new JsonResponseException("no.material.consume");
        }
        Long userId = UserUtil.getUserId();
        String realName = RespHelper.or500(doctorGroupWebService.findRealName(userId));

        DoctorMaterialConsumeProviderDto diaochu = DoctorMaterialConsumeProviderDto.builder()
                .actionType(DoctorMaterialConsumeProvider.EVENT_TYPE.DIAOCHU.getValue()).type(material.getType())
                .farmId(dto.getFarmId()).farmName(farm.getName())
                .materialTypeId(dto.getMaterialId()).materialName(material.getName())
                .wareHouseId(dto.getFromWareHouseId()).wareHouseName(consumeWarehouse.getWareHouseName())
                .staffId(userId).staffName(realName)
                .count(dto.getMoveQuantity())
                .eventTime(dto.getEventAt() == null ? new Date() : DateUtil.toDate(dto.getEventAt()))
                .unitName(materialInWareHouse.getUnitName()).unitGroupName(materialInWareHouse.getUnitGroupName())
                .build();
        DoctorMaterialConsumeProviderDto diaoru = DoctorMaterialConsumeProviderDto.builder()
                .actionType(DoctorMaterialConsumeProvider.EVENT_TYPE.DIAORU.getValue()).type(material.getType())
                .farmId(dto.getFarmId()).farmName(farm.getName())
                .materialTypeId(dto.getMaterialId()).materialName(material.getName())
                .wareHouseId(dto.getToWareHouseId()).wareHouseName(toWarehouse.getWareHouseName())
                .staffId(userId).staffName(realName)
                .count(dto.getMoveQuantity())
                .eventTime(dto.getEventAt() == null ? new Date() : DateUtil.toDate(dto.getEventAt()))
                .unitName(materialInWareHouse.getUnitName()).unitGroupName(materialInWareHouse.getUnitGroupName())
                .build();
        return new DoctorMoveMaterialDto(diaochu, diaoru);
    }

    /**
     * ????????????????????????
     * @param criteriaMap ????????????
     * @return ???????????????
     */
    public List<DoctorWareHouseMaterialData> materialExportData(Map<String, String> criteriaMap, Integer num) {

        Date startDate = DateUtil.toYYYYMM(criteriaMap.get("startAt"));
        Date endDate = null;
        if (num == 1) {

            startDate = DateUtils.addMonths(startDate, 1);
        }
        DoctorWareHouseMaterialCriteria criteria = BeanMapper.map(criteriaMap, DoctorWareHouseMaterialCriteria.class);
        criteria.setStartDate(DateUtil.toDateTimeString(startDate));
        criteria.setEndDate(null);
        List<DoctorWareHouseMaterialData> listDate = new ArrayList<>(500);
        double numbersOut = 0;
        double priceOut = 0;
        double numberIn = 0;
        double priceIn = 0;
        double monthEndAmount = 0;
        double monthEndNumber = 0;
        List<DoctorMaterialConsumeProvider> list = RespHelper.or500(materialConsumeProviderReadService.findMaterialConsume(

                criteria.getFarmId(),
                criteria.getWareHouseId(),
                criteria.getMaterialId(),
                criteria.getMaterialName(),
                criteria.getBarnId(),
                criteria.getMaterialType(),
                criteria.getBarnName(),
                criteria.getType(),
                DateUtil.stringToDate(criteria.getStartDate()),
                DateUtil.stringToDate(criteria.getEndDate()),
                criteria.getPageNo(), criteria.getSize()));
        //?????????????????????????????????????????????????????????????????????DoctorMaterialConsumeProvider?????????????????????????????????????????????
        List<DoctorMaterialConsumeProvider> listOverride = buildDoctorMaterialConsumeProvider(list);
        //??????dto???????????????????????????????????????
        for (int i = 0, length = listOverride.size(); i < length; i++) {
            if (i != (listOverride.size()-1) && listOverride.get(i).getMaterialId().equals(listOverride.get(i+1).getMaterialId())) {
                if (DoctorMaterialConsumeProvider.EVENT_TYPE.from(listOverride.get(i).getEventType()).isOut()) {
                    numbersOut += listOverride.get(i).getEventCount();
                    priceOut += listOverride.get(i).getUnitPrice() * listOverride.get(i).getEventCount();
                } else {
                    numberIn += listOverride.get(i).getEventCount();
                    priceIn += listOverride.get(i).getUnitPrice() * listOverride.get(i).getEventCount();
                }
            } else{
                //????????????????????????
                if (DoctorMaterialConsumeProvider.EVENT_TYPE.from(listOverride.get(i).getEventType()).isOut()) {
                    numbersOut += listOverride.get(i).getEventCount();
                    priceOut += listOverride.get(i).getUnitPrice() * listOverride.get(i).getEventCount();
                } else {
                    numberIn += listOverride.get(i).getEventCount();
                    priceIn += listOverride.get(i).getUnitPrice() * listOverride.get(i).getEventCount();
                }
                //????????????
                List<DoctorMaterialPriceInWareHouse> doctorMaterialPriceInWareHouses = RespHelper.or500(materialPriceInWareHouseReadService.findMaterialData(
                        criteria.getFarmId(),
                        listOverride.get(i).getMaterialId(),
                        criteria.getWareHouseId(),
                        null,
                        null));
                if (doctorMaterialPriceInWareHouses.size() == 0) {
                    monthEndNumber = 0;
                    monthEndAmount = 0;
                }else {
                    for (DoctorMaterialPriceInWareHouse doctorMaterialPriceInWareHouse : doctorMaterialPriceInWareHouses) {
                        monthEndNumber += doctorMaterialPriceInWareHouse.getRemainder();
                        monthEndAmount += doctorMaterialPriceInWareHouse.getRemainder() * doctorMaterialPriceInWareHouse.getUnitPrice();
                    }
                }
                DoctorMaterialInWareHouse doctorMaterialInWareHouse = RespHelper.or500(doctorMaterialInWareHouseReadService.findMaterialUnits(
                        criteria.getFarmId(),
                        listOverride.get(i).getMaterialId(),
                        criteria.getWareHouseId()));

                DoctorWareHouseMaterialData date = new DoctorWareHouseMaterialData();
                date.setMaterialName(listOverride.get(i).getMaterialName());
                date.setProviderFactoryName(listOverride.get(i).getProviderFactoryName());
                if (doctorMaterialInWareHouse != null) {
                    date.setUnitName(doctorMaterialInWareHouse.getUnitName());
                }
                date.setMonthBeginAmount(monthEndAmount - priceIn + priceOut);
                date.setMonthBeginNumber(monthEndNumber - numberIn + numbersOut);
                date.setInAmount(priceIn);
                date.setInCount(numberIn);
                date.setOutAmount(priceOut);
                date.setOutCount(numbersOut);
                date.setCurrentAmount(monthEndAmount);
                date.setLotNumber(monthEndNumber);

                listDate.add(date);
                //????????????
                numbersOut = 0;
                priceOut = 0;
                numberIn = 0;
                priceIn = 0;
                monthEndNumber = 0;
                monthEndAmount = 0;

            }
        }

        return listDate;
    }
    /**
     * ??????????????????????????????
     * @param farmId ??????id
     * @param wareHouseId ??????id
     * @param materialId ??????id
     * @param materialName ?????????
     * @param pageNo ??????
     * @param pageSize ????????????
     * @return ????????????
     */
    private Paging<DoctorMaterialInWareHouseDto> pagingWareHouseMaterial(Long farmId, Long wareHouseId, Long materialId, String materialName, Integer pageNo, Integer pageSize) {
        DateTime monthStart = DateTime.now().withTimeAtStartOfDay().withDayOfMonth(1);
        DateTime nextMonthStart = monthStart.plusMonths(1);

        Paging<DoctorMaterialInWareHouse> result = RespHelper.or500(
                doctorMaterialInWareHouseReadService.pagingDoctorMaterialInWareHouse(farmId, wareHouseId, materialId, materialName, pageNo, pageSize)
        );
        List<DoctorMaterialInWareHouseDto> list = result.getData().stream()
                .map(in -> {
                    DoctorMaterialInWareHouseDto dto = DoctorMaterialInWareHouseDto.buildDoctorMaterialInWareHouseInfo(in);
                    Double amount = RespHelper.or500(materialPriceInWareHouseReadService.findByWareHouseAndMaterialId(dto.getWarehouseId(), dto.getMaterialId())).stream()
                            .map(price -> price.getRemainder() * price.getUnitPrice())
                            .reduce((o1, o2) -> o1 + o2).orElse(0D);
                    dto.setCurrentAmount(amount);
                    RespHelper.or500(materialConsumeProviderReadService.warehouseEventReport(
                            farmId, dto.getWarehouseId(), dto.getMaterialId(), null, null, null, null, null, DateUtil.toDateString(monthStart.toDate()), DateUtil.toDateString(nextMonthStart.toDate()))
                    ).forEach(report -> {
                        if(Objects.equals(report.getEventType(), DoctorMaterialConsumeProvider.EVENT_TYPE.CONSUMER.getValue())){
                            dto.setOutAmount(dto.getOutAmount() + report.getAmount());
                            dto.setOutCount(dto.getOutCount() + report.getCount());
                        }
                        if(Objects.equals(report.getEventType(), DoctorMaterialConsumeProvider.EVENT_TYPE.PROVIDER.getValue())){
                            dto.setInAmount(dto.getInAmount() + report.getAmount());
                            dto.setInCount(dto.getInCount() + report.getCount());
                        }
                        if(Objects.equals(report.getEventType(), DoctorMaterialConsumeProvider.EVENT_TYPE.DIAORU.getValue())){
                            dto.setDiaoruAmount(dto.getDiaoruAmount() + report.getAmount());
                            dto.setDiaoruCount(dto.getDiaoruCount() + report.getCount());
                        }
                        if(Objects.equals(report.getEventType(), DoctorMaterialConsumeProvider.EVENT_TYPE.DIAOCHU.getValue())){
                            dto.setDiaochuAmount(dto.getDiaochuAmount() + report.getAmount());
                            dto.setDiaochuCount(dto.getDiaochuCount() + report.getCount());
                        }
                    });
                    return dto;
                })
                .collect(Collectors.toList());
        return new Paging<>(result.getTotal(), list);
    }

    /**
     * ???????????????????????????
     * @param params
     */

    @RequestMapping(value = "/ware/details", method = RequestMethod.GET)
    @ResponseBody
    public Paging<DoctorMasterialDatailsGroup> pagingWareHouseMaterialDetails(@RequestParam Map<String, Object> params,
                                                                              @RequestParam Long farmId,
                                                                              @RequestParam Integer pageNo,
                                                                              @RequestParam Integer pageSize,
                                                                              @RequestParam String date) {
        Date startDate = DateUtil.toYYYYMM(date);
        Date endDate = DateUtil.toYYYYMM(date);
        startDate = DateUtil.monthStart(startDate);
        endDate = DateUtil.monthEnd(endDate);
        params.put("farmId", farmId);
        params.put("flag", DETAILS);
        params.put("startDate", startDate);
        params.put("endDate", endDate);
        return RespHelper.or500(doctorGroupMaterialReadServer.findMasterialDatailsGroup(params, pageNo, pageSize));
    }
    /**
     * ??????????????????????????????
     * @param maps
     * @return
     */
    @RequestMapping(value = "/ware/use", method = RequestMethod.GET)
    @ResponseBody
    public Paging<DoctorMasterialDatailsGroup> pagingWareHouseMaterialUse(@RequestParam Map<String, Object> maps,
                                                                          @RequestParam Long farmId,
                                                                          @RequestParam Integer pageNo,
                                                                          @RequestParam Integer pageSize,
                                                                          @RequestParam String date) {

        Date startDate = DateUtil.toYYYYMM(date);
        Date endDate = DateUtil.toYYYYMM(date);
        startDate = DateUtil.monthStart(startDate);
        endDate = DateUtil.monthEnd(endDate);
        maps.put("farmId", farmId);
        maps.put("flag", WHARE);
        maps.put("startDate", startDate);
        maps.put("endDate", endDate);
        return RespHelper.or500(doctorGroupMaterialReadServer.findMasterialDatailsGroup(maps, pageNo, pageSize));
    }

    /**
     * ???????????????????????????
     * @param map
     * @param farmId
     * @return
     */
    @RequestMapping(value = "/group/material")
    @ResponseBody
    public Paging<DoctorMasterialDatailsGroup> pagingGroupMaterial(@RequestParam Map<String, Object> map,
                                                                   @RequestParam Long farmId,
                                                                   @RequestParam Integer pageNo,
                                                                   @RequestParam Integer pageSize) {
        map.put("farmId", farmId);
        map.put("flag", GROUP);
        return RespHelper.or500(doctorGroupMaterialReadServer.findMasterialDatailsGroup(map, pageNo, pageSize));
    }

    private final List<DoctorMaterialConsumeProvider> buildDoctorMaterialConsumeProvider(List<DoctorMaterialConsumeProvider> list) {

        List<DoctorMaterialConsumeProvider> listOverride = new ArrayList<>(500);
        DoctorMaterialConsumeProvider doctorMaterialConsumeProviderOverride;

        for (int i = 0, length = list.size(); i < length; i++) {

            if (list.get(i).getExtra() != null && list.get(i).getExtraMap().containsKey("consumePrice")) {

                List<Map<String, Object>> priceCompose = (ArrayList) list.get(i).getExtraMap().get("consumePrice");
                for (Map<String, Object> eachPrice : priceCompose) {
                    doctorMaterialConsumeProviderOverride = new DoctorMaterialConsumeProvider();
                    Long providerIdfd = Long.valueOf(eachPrice.get("providerId").toString());
                    if (isNull(providerIdfd)) {
                        providerIdfd = -1L;
                    }
                    Long unitPrice = Long.valueOf(eachPrice.get("unitPrice").toString());
                    Double count = Double.valueOf(eachPrice.get("count").toString());
                    doctorMaterialConsumeProviderOverride.setMaterialName(list.get(i).getMaterialName());
                    doctorMaterialConsumeProviderOverride.setUnitPrice(unitPrice);
                    doctorMaterialConsumeProviderOverride.setMaterialId(list.get(i).getMaterialId());
                    doctorMaterialConsumeProviderOverride.setWareHouseId(list.get(i).getWareHouseId());
                    doctorMaterialConsumeProviderOverride.setBarnName(list.get(i).getBarnName());
                    doctorMaterialConsumeProviderOverride.setEventTime(list.get(i).getEventTime());
                    doctorMaterialConsumeProviderOverride.setGroupCode(list.get(i).getGroupCode());
                    doctorMaterialConsumeProviderOverride.setWareHouseName(list.get(i).getWareHouseName());
                    doctorMaterialConsumeProviderOverride.setEventCount(count);
                    doctorMaterialConsumeProviderOverride.setType(list.get(i).getType());
                    doctorMaterialConsumeProviderOverride.setProvider(providerIdfd);
                    doctorMaterialConsumeProviderOverride.setEventType(list.get(i).getEventType());
                    listOverride.add(doctorMaterialConsumeProviderOverride);

                }
            } else {
                if (isNull(list.get(i).getProviderFactoryId())) {
                    list.get(i).setProvider(-1L);
                }
                listOverride.add(list.get(i));
            }
        }
        return listOverride;
    }

}
