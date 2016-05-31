package io.terminus.doctor.web.front.basic.controller;

import io.terminus.common.utils.JsonMapper;
import io.terminus.doctor.basic.model.DoctorBreed;
import io.terminus.doctor.basic.model.DoctorChangeReason;
import io.terminus.doctor.basic.model.DoctorChangeType;
import io.terminus.doctor.basic.model.DoctorCustomer;
import io.terminus.doctor.basic.model.DoctorDisease;
import io.terminus.doctor.basic.model.DoctorGenetic;
import io.terminus.doctor.basic.service.DoctorBasicReadService;
import io.terminus.doctor.basic.service.DoctorBasicWriteService;
import io.terminus.doctor.common.utils.RespHelper;
import io.terminus.pampas.common.UserUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Desc: 基础数据Controller
 * Mail: yangzl@terminus.io
 * author: DreamYoung
 * Date: 16/5/23
 */
@Slf4j
@RestController
@RequestMapping("/api/doctor/basic")
public class DoctorBasics {

    private static final JsonMapper JSON_MAPPER = JsonMapper.nonEmptyMapper();

    private final DoctorBasicReadService doctorBasicReadService;
    private final DoctorBasicWriteService doctorBasicWriteService;

    @Autowired
    public DoctorBasics(DoctorBasicReadService doctorBasicReadService,
                        DoctorBasicWriteService doctorBasicWriteService) {
        this.doctorBasicReadService = doctorBasicReadService;
        this.doctorBasicWriteService = doctorBasicWriteService;
    }

    /************************** 品种品系相关 *************************/
    /**
     * 查询所有品种
     * @return 品种列表
     */
    @RequestMapping(value = "/breed/all", method = RequestMethod.GET)
    public List<DoctorBreed> finaAllBreed() {
        return RespHelper.or500(doctorBasicReadService.findAllBreeds());
    }

    /**
     * 查询所有品种
     * @return 品种列表
     */
    @RequestMapping(value = "/genetic/all", method = RequestMethod.GET)
    public List<DoctorGenetic> finaAllGenetic() {
        return RespHelper.or500(doctorBasicReadService.findAllGenetics());
    }

    /************************** 疾病防疫相关 **************************/
    /**
     * 查询疾病详情
     * @param diseaseId 主键id
     * @return 疾病表
     */
    @RequestMapping(value = "/disease/id", method = RequestMethod.GET)
    public DoctorDisease findDiseaseById(@RequestParam("diseaseId") Long diseaseId) {
        return RespHelper.or500(doctorBasicReadService.findDiseaseById(diseaseId));
    }

    /**
     * 根据farmId查询疾病列表
     * @param farmId 猪场id
     * @return 疾病列表
     */
    @RequestMapping(value = "/disease/farmId", method = RequestMethod.GET)
    public List<DoctorDisease> findDiseaseByfarmId(@RequestParam("farmId") Long farmId) {
        return RespHelper.or500(doctorBasicReadService.findDiseasesByFarmId(farmId));
    }

    /**
     * 创建或更新疾病表
     * @return 是否成功
     */
    @RequestMapping(value = "/disease", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public Boolean createOrUpdateDisease(@RequestBody DoctorDisease disease) {
        checkNotNull(disease, "disease.not.null");

        // TODO: 权限中心校验权限

        if (disease.getId() == null) {
            disease.setCreatorId(UserUtil.getUserId());
            disease.setCreatorName(UserUtil.getCurrentUser().getName());
            RespHelper.or500(doctorBasicWriteService.createDisease(disease));
        } else {
            disease.setUpdatorId(UserUtil.getUserId());
            disease.setUpdatorName(UserUtil.getCurrentUser().getName());
            RespHelper.or500(doctorBasicWriteService.updateDisease(disease));
        }
        return Boolean.TRUE;
    }

    /**
     * 根据主键id删除DoctorDisease
     * @return 是否成功
     */
    @RequestMapping(value = "/disease", method = RequestMethod.DELETE)
    public Boolean deleteDisease(@RequestParam("diseaseId") Long diseaseId) {
        DoctorDisease disease = RespHelper.or500(doctorBasicReadService.findDiseaseById(diseaseId));

        // TODO: 权限中心校验权限

        return RespHelper.or500(doctorBasicWriteService.deleteDiseaseById(diseaseId));
    }

    /************************** 猪群变动相关 **************************/
    /**
     * 根据id查询变动类型表
     * @param changeTypeId 主键id
     * @return 变动类型表
     */
    @RequestMapping(value = "/changeType/id", method = RequestMethod.GET)
    public DoctorChangeType findChangeTypeById(@RequestParam("changeTypeId") Long changeTypeId) {
        return RespHelper.or500(doctorBasicReadService.findChangeTypeById(changeTypeId));
    }

    /**
     * 根据farmId查询变动类型表
     * @param farmId 猪场id
     * @return 变动类型表列表
     */
    @RequestMapping(value = "/changeType/farmId", method = RequestMethod.GET)
    public List<DoctorChangeType> findChangeTypesByfarmId(@RequestParam("farmId") Long farmId) {
        return RespHelper.or500(doctorBasicReadService.findChangeTypesByFarmId(farmId));
    }

    /**
     * 创建或更新DoctorChangeType
     * @return 是否成功
     */
    @RequestMapping(value = "/changeType", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public Boolean createOrUpdateChangeType(@RequestBody DoctorChangeType changeType) {
        checkNotNull(changeType, "changeType.not.null");

        // TODO: 权限中心校验权限

        if (changeType.getId() == null) {
            changeType.setCreatorId(UserUtil.getUserId());
            changeType.setCreatorName(UserUtil.getCurrentUser().getName());
            RespHelper.or500(doctorBasicWriteService.createChangeType(changeType));
        } else {
            changeType.setUpdatorId(UserUtil.getUserId());
            changeType.setUpdatorName(UserUtil.getCurrentUser().getName());
            RespHelper.or500(doctorBasicWriteService.updateChangeType(changeType));
        }
        return Boolean.TRUE;
    }

    /**
     * 根据主键id删除DoctorChangeType
     * @return 是否成功
     */
    @RequestMapping(value = "/changeType", method = RequestMethod.DELETE)
    public Boolean deleteChangeType(@RequestParam("changeTypeId") Long changeTypeId) {
        DoctorChangeType changeType = RespHelper.or500(doctorBasicReadService.findChangeTypeById(changeTypeId));

        // TODO: 权限中心校验权限

        return RespHelper.or500(doctorBasicWriteService.deleteChangeTypeById(changeTypeId));
    }

    /**
     * 根据id查询变动原因表
     * @param changeReasonId 主键id
     * @return 变动原因表
     */
    @RequestMapping(value = "/changeReason/id", method = RequestMethod.GET)
    public DoctorChangeReason findChangeReasonById(@RequestParam("changeReasonId") Long changeReasonId) {
        return RespHelper.or500(doctorBasicReadService.findChangeReasonById(changeReasonId));
    }

    /**
     * 根据变动类型id查询变动原因表
     * @param changeTypeId 变动类型id
     * @return 变动原因表
     */
    @RequestMapping(value = "/changeReason/typeId", method = RequestMethod.GET)
    public List<DoctorChangeReason> findChangeReasonByChangeTypeId(@RequestParam("changeTypeId") Long changeTypeId) {
        return RespHelper.or500(doctorBasicReadService.findChangeReasonByChangeTypeId(changeTypeId));
    }

    /**
     * 创建或更新DoctorChangeReason
     * @return 是否成功
     */
    @RequestMapping(value = "/changeReason", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public Boolean createOrUpdateChangeReason(@RequestParam("changeTypeId") Long changeTypeId,
                                              @RequestParam("reason") String reason) {

        DoctorChangeReason changeReason = JSON_MAPPER.fromJson(reason, DoctorChangeReason.class);
        checkNotNull(changeReason, "customer.not.null");

        // TODO: 权限中心校验权限

        //设置变动类型id
        changeReason.setChangeTypeId(changeTypeId);
        if (changeReason.getId() == null) {
            changeReason.setCreatorId(UserUtil.getUserId());
            changeReason.setCreatorName(UserUtil.getCurrentUser().getName());
            RespHelper.or500(doctorBasicWriteService.createChangeReason(changeReason));
        } else {
            changeReason.setUpdatorId(UserUtil.getUserId());
            changeReason.setUpdatorName(UserUtil.getCurrentUser().getName());
            RespHelper.or500(doctorBasicWriteService.updateChangeReason(changeReason));
        }
        return Boolean.TRUE;
    }

    /**
     * 根据主键id删除DoctorChangeReason
     * @return 是否成功
     */
    @RequestMapping(value = "/changeReason", method = RequestMethod.DELETE)
    public Boolean deleteChangeReason(@RequestParam("changeReasonId") Long changeReasonId) {
        DoctorChangeReason changeReason = RespHelper.or500(doctorBasicReadService.findChangeReasonById(changeReasonId));

        // TODO: 权限中心校验权限

        return RespHelper.or500(doctorBasicWriteService.deleteChangeReasonById(changeReasonId));
    }

    /*********************** 猪场客户相关 ************************/
    /**
     * 根据id查询客户表
     * @param customerId 主键id
     * @return 客户表
     */
    @RequestMapping(value = "/customer/id", method = RequestMethod.GET)
    public DoctorCustomer findCustomerById(@RequestParam("customerId") Long customerId) {
        return RespHelper.or500(doctorBasicReadService.findCustomerById(customerId));
    }

    /**
     * 根据farmId查询客户表
     * @param farmId 猪场id
     * @return 客户表列表
     */
    @RequestMapping(value = "/customer/farmId", method = RequestMethod.GET)
    public List<DoctorCustomer> findCustomersByfarmId(@RequestParam("farmId") Long farmId) {
        return RespHelper.or500(doctorBasicReadService.findCustomersByFarmId(farmId));
    }

    /**
     * 创建或更新DoctorCustomer
     * @return 是否成功
     */
    @RequestMapping(value = "/customer", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public Boolean createOrUpdateCustomer(@RequestBody DoctorCustomer customer) {
        checkNotNull(customer, "customer.not.null");

        // TODO: 权限中心校验权限

        if (customer.getId() == null) {
            customer.setCreatorId(UserUtil.getUserId());
            customer.setCreatorName(UserUtil.getCurrentUser().getName());
            RespHelper.or500(doctorBasicWriteService.createCustomer(customer));
        } else {
            customer.setUpdatorId(UserUtil.getUserId());
            customer.setUpdatorName(UserUtil.getCurrentUser().getName());
            RespHelper.or500(doctorBasicWriteService.updateCustomer(customer));
        }
        return Boolean.TRUE;
    }

    /**
     * 根据主键id删除DoctorCustomer
     * @return 是否成功
     */
    @RequestMapping(value = "/customer", method = RequestMethod.DELETE)
    public Boolean deleteCustomer(@RequestParam("customerId") Long customerId) {
        DoctorCustomer customer = RespHelper.or500(doctorBasicReadService.findCustomerById(customerId));

        // TODO: 权限中心校验权限

        return RespHelper.or500(doctorBasicWriteService.deleteCustomerById(customerId));
    }
}
