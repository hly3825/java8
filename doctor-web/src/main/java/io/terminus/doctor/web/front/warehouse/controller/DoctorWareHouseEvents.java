package io.terminus.doctor.web.front.warehouse.controller;

import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.exception.ServiceException;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import io.terminus.doctor.basic.model.DoctorBasic;
import io.terminus.doctor.basic.model.DoctorBasicMaterial;
import io.terminus.doctor.basic.service.DoctorBasicMaterialReadService;
import io.terminus.doctor.basic.service.DoctorBasicReadService;
import io.terminus.doctor.common.enums.WareHouseType;
import io.terminus.doctor.common.utils.RespHelper;
import io.terminus.doctor.event.service.DoctorBarnReadService;
import io.terminus.doctor.user.model.DoctorFarm;
import io.terminus.doctor.user.service.DoctorFarmReadService;
import io.terminus.doctor.user.service.DoctorUserProfileReadService;
import io.terminus.doctor.warehouse.dto.DoctorMaterialConsumeProviderDto;
import io.terminus.doctor.warehouse.dto.DoctorMaterialInWareHouseDto;
import io.terminus.doctor.warehouse.dto.DoctorWareHouseDto;
import io.terminus.doctor.warehouse.model.DoctorMaterialConsumeProvider;
import io.terminus.doctor.warehouse.model.DoctorMaterialInWareHouse;
import io.terminus.doctor.warehouse.model.DoctorMaterialInfo;
import io.terminus.doctor.warehouse.model.DoctorWareHouse;
import io.terminus.doctor.warehouse.service.DoctorMaterialInWareHouseReadService;
import io.terminus.doctor.warehouse.service.DoctorMaterialInWareHouseWriteService;
import io.terminus.doctor.warehouse.service.DoctorMaterialInfoReadService;
import io.terminus.doctor.warehouse.service.DoctorWareHouseReadService;
import io.terminus.doctor.web.front.controller.UserProfiles;
import io.terminus.doctor.web.front.warehouse.dto.DoctorConsumeProviderInputDto;
import io.terminus.pampas.common.UserUtil;
import io.terminus.parana.user.model.User;
import io.terminus.parana.user.model.UserProfile;
import io.terminus.parana.user.service.UserReadService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;
import java.util.Objects;

import static com.google.common.base.Preconditions.checkState;
import static java.util.Objects.isNull;

/**
 * Created by yaoqijun.
 * Date:2016-05-25
 * Email:yaoqj@terminus.io
 * Descirbe: 对应的仓库事件信息录入
 */
@Slf4j
@Controller
@RequestMapping("/api/doctor/warehouse/event")
public class DoctorWareHouseEvents {

    private final DoctorMaterialInWareHouseWriteService doctorMaterialInWareHouseWriteService;

    private final DoctorMaterialInWareHouseReadService doctorMaterialInWareHouseReadService;

    private final UserReadService<User> userReadService;

    private final DoctorUserProfileReadService doctorUserProfileReadService;

    private final DoctorBarnReadService doctorBarnReadService;

    private final DoctorWareHouseReadService doctorWareHouseReadService;

    private final DoctorBasicMaterialReadService doctorBasicMaterialReadService;

    private final DoctorBasicReadService doctorBasicReadService;

    private final DoctorFarmReadService doctorFarmReadService;

    @Autowired
    public DoctorWareHouseEvents(DoctorMaterialInWareHouseWriteService doctorMaterialInWareHouseWriteService,
                                 DoctorMaterialInWareHouseReadService doctorMaterialInWareHouseReadService,
                                 UserReadService<User> userReadService, DoctorBarnReadService doctorBarnReadService,
                                 DoctorBasicMaterialReadService doctorBasicMaterialReadService,
                                 DoctorWareHouseReadService doctorWareHouseReadService,
                                 DoctorFarmReadService doctorFarmReadService,
                                 DoctorBasicReadService doctorBasicReadService,
                                 DoctorUserProfileReadService doctorUserProfileReadService){
        this.doctorMaterialInWareHouseWriteService = doctorMaterialInWareHouseWriteService;
        this.doctorMaterialInWareHouseReadService = doctorMaterialInWareHouseReadService;
        this.userReadService = userReadService;
        this.doctorBarnReadService = doctorBarnReadService;
        this.doctorBasicMaterialReadService = doctorBasicMaterialReadService;
        this.doctorWareHouseReadService = doctorWareHouseReadService;
        this.doctorFarmReadService = doctorFarmReadService;
        this.doctorUserProfileReadService = doctorUserProfileReadService;
        this.doctorBasicReadService = doctorBasicReadService;
    }

    /**
     * 获取对应的仓库的 物料信息
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
     * 对应的仓库信息的分页查询方式
     * @param farmId
     * @param wareHouseId
     * @param pageNo
     * @param pageSize
     * @return
     */
    @RequestMapping(value = "/paging", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public Paging<DoctorMaterialInWareHouseDto> pagingDoctorMaterialInWareHouse(@RequestParam("farmId") Long farmId,
                                                                                @RequestParam("wareHouseId") Long wareHouseId,
                                                                                @RequestParam("pageNo") Integer pageNo,
                                                                                @RequestParam("pageSize") Integer pageSize){
        Paging<DoctorMaterialInWareHouseDto> result = RespHelper.or500(doctorMaterialInWareHouseReadService.pagingDoctorMaterialInWareHouse(farmId, wareHouseId, pageNo, pageSize));

        try{
            result.getData().forEach(s->{
                Response<UserProfile> response = doctorUserProfileReadService.findProfileByUserId(s.getStaffId());
                s.setRealName(RespHelper.orServEx(response).getRealName());
            });

        }catch (Exception e){
            log.error("get user data info fail, cause:{}", Throwables.getStackTraceAsString(e));
            throw new JsonResponseException(e.getMessage());
        }

        return result;
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
     * 消耗物品的领用
     * @param dto
     * @return
     */
    @RequestMapping(value = "/consume", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public Long createConsumeEvent(@RequestBody DoctorConsumeProviderInputDto dto){
        DoctorMaterialConsumeProviderDto doctorMaterialConsumeProviderDto = null;
        try{

            DoctorMaterialInWareHouse doctorMaterialInWareHouse = RespHelper.orServEx(doctorMaterialInWareHouseReadService.queryByMaterialWareHouseIds(
                    dto.getFarmId(),dto.getMaterialId(),dto.getWareHouseId()));
            checkState(!isNull(doctorMaterialInWareHouse), "input.materialInfo.error");

            Long currentUserId = UserUtil.getUserId();
            Response<User> userResponse = userReadService.findById(currentUserId);
            String userName = RespHelper.orServEx(userResponse).getName();

            DoctorBasicMaterial doctorBasicMaterial = RespHelper.orServEx(doctorBasicMaterialReadService.findBasicMaterialById(dto.getMaterialId()));

            doctorMaterialConsumeProviderDto = DoctorMaterialConsumeProviderDto.builder()
                    .actionType(DoctorMaterialConsumeProvider.EVENT_TYPE.CONSUMER.getValue()).type(doctorMaterialInWareHouse.getType())
                    .farmId(doctorMaterialInWareHouse.getFarmId()).farmName(doctorMaterialInWareHouse.getFarmName())
                    .wareHouseId(doctorMaterialInWareHouse.getWareHouseId()).wareHouseName(doctorMaterialInWareHouse.getWareHouseName())
                    .materialTypeId(doctorMaterialInWareHouse.getMaterialId()).materialName(doctorMaterialInWareHouse.getMaterialName())
                    .barnId(dto.getBarnId()).barnName(dto.getBarnName())
                    .staffId(currentUserId).staffName(userName)
                    .count(dto.getCount()).consumeDays(dto.getConsumeDays())
                    .unitId(doctorBasicMaterial.getUnitId()).unitName(doctorBasicMaterial.getUnitName())
                    .unitGroupId(doctorBasicMaterial.getUnitGroupId()).unitGroupName(doctorBasicMaterial.getUnitGroupName())
                    .build();
        }catch (Exception e){
            log.error("consume material fail, cause:{}", Throwables.getStackTraceAsString(e));
            throw new JsonResponseException(e.getMessage());
        }
        return RespHelper.or500(doctorMaterialInWareHouseWriteService.consumeMaterialInfo(doctorMaterialConsumeProviderDto));
    }

    /**
     * 录入物品数据信息
     * @param dto
     * @return
     */
    @RequestMapping(value = "/provider", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public Long createProviderEvent(@RequestBody DoctorConsumeProviderInputDto dto){
        DoctorMaterialConsumeProviderDto doctorMaterialConsumeProviderDto;
        if(dto.getUnitPrice() == null || dto.getUnitPrice() < 0){
            throw new JsonResponseException("price.invalid");
        }
        try{
            DoctorWareHouseDto doctorWareHouseDto = RespHelper.orServEx(doctorWareHouseReadService.queryDoctorWareHouseById(dto.getWareHouseId()));

            Long userId = UserUtil.getUserId();
            String userName = RespHelper.orServEx(doctorUserProfileReadService.findProfileByUserIds(Lists.newArrayList(userId))).get(0).getRealName();

            DoctorBasicMaterial doctorBasicMaterial = RespHelper.orServEx(doctorBasicMaterialReadService.findBasicMaterialById(dto.getMaterialId()));
            DoctorFarm doctorFarm = RespHelper.orServEx(doctorFarmReadService.findFarmById(dto.getFarmId()));

            doctorMaterialConsumeProviderDto = DoctorMaterialConsumeProviderDto.builder()
                    .actionType(DoctorMaterialConsumeProvider.EVENT_TYPE.PROVIDER.getValue()).type(doctorBasicMaterial.getType())
                    .farmId(doctorFarm.getId()).farmName(doctorFarm.getName())
                    .wareHouseId(doctorWareHouseDto.getWarehouseId()).wareHouseName(doctorWareHouseDto.getWarehouseName())
                    .materialTypeId(doctorBasicMaterial.getId()).materialName(doctorBasicMaterial.getName())
                    .staffId(userId).staffName(userName)
                    .count(dto.getCount())
                    //.unitId(doctorBasicMaterial.getUnitId()).unitName(doctorBasicMaterial.getUnitName())
                    .unitGroupId(doctorBasicMaterial.getUnitGroupId()).unitGroupName(doctorBasicMaterial.getUnitGroupName())
                    .unitPrice(dto.getUnitPrice())
                    .build();

            // 药品.疫苗.易耗品, 前台必须传来单位
            if(Objects.equals(doctorBasicMaterial.getType(), WareHouseType.CONSUME.getKey())
                    || Objects.equals(doctorBasicMaterial.getType(), WareHouseType.MEDICINE.getKey())
                    || Objects.equals(doctorBasicMaterial.getType(), WareHouseType.VACCINATION.getKey())){
                if(dto.getUnitId() != null){
                    DoctorBasic doctorBasic = RespHelper.or500(doctorBasicReadService.findBasicById(dto.getUnitId()));
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
        }catch (Exception e){
            log.error("provider material fail, cause:{}", Throwables.getStackTraceAsString(e));
            throw new JsonResponseException(e.getMessage());
        }
        return RespHelper.or500(doctorMaterialInWareHouseWriteService.providerMaterialInfo(doctorMaterialConsumeProviderDto));
    }
}
