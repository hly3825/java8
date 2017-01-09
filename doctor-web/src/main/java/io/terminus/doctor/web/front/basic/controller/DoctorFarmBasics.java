package io.terminus.doctor.web.front.basic.controller;

import com.google.common.base.Throwables;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.doctor.basic.model.DoctorBasic;
import io.terminus.doctor.basic.model.DoctorChangeReason;
import io.terminus.doctor.basic.model.DoctorFarmBasic;
import io.terminus.doctor.basic.service.DoctorBasicReadService;
import io.terminus.doctor.basic.service.DoctorFarmBasicReadService;
import io.terminus.doctor.basic.service.DoctorFarmBasicWriteService;
import io.terminus.parana.common.utils.RespHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;

import static io.terminus.common.utils.Arguments.isEmpty;

/**
 * Code generated by CodeGen
 * Desc: 猪场基础数据关联表 Controller
 * Date: 2016-11-22
 */
@Slf4j
@RestController
@RequestMapping("/api/doctor")
public class DoctorFarmBasics {

    @RpcConsumer
    private DoctorFarmBasicReadService doctorFarmBasicReadService;

    @RpcConsumer
    private DoctorFarmBasicWriteService doctorFarmBasicWriteService;

    @RpcConsumer
    private DoctorBasicReadService doctorBasicReadService;

    /**
     * 根据猪场id查询猪场有权限的基础数据ids
     * @param farmId 猪场id
     * @return 基础数据ids
     */
    @RequestMapping(value = "/farmBasic/basicIds", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public DoctorFarmBasic findBasicIdsByFarmId(@RequestParam("farmId") Long farmId) {
        try {
            return RespHelper.or500(doctorFarmBasicReadService.findFarmBasicByFarmId(farmId));
        } catch (Exception e) {
            log.error("find basic ids by farm id failed, farmId:{}, cause:{}",
                    farmId, Throwables.getStackTraceAsString(e));
            return null;
        }
    }

    /**
     * 创建或更新basicIds
     * @return 是否成功
     */
    @RequestMapping(value = "/farmBasic/basicIds", method = RequestMethod.POST)
    public Boolean createOrUpdateBasicIds(@RequestParam("farmId") Long farmId,
                                          @RequestParam("ids") String ids) {
        if (isEmpty(ids)) {
            return Boolean.TRUE;
        }
        DoctorFarmBasic farmBasic = RespHelper.or500(doctorFarmBasicReadService.findFarmBasicByFarmId(farmId));
        if (farmBasic == null) {
            farmBasic = new DoctorFarmBasic();
            farmBasic.setFarmId(farmId);
            farmBasic.setBasicIds(ids);
            RespHelper.or500(doctorFarmBasicWriteService.createFarmBasic(farmBasic));
        } else {
            farmBasic.setBasicIds(ids);
            RespHelper.or500(doctorFarmBasicWriteService.updateFarmBasic(farmBasic));
        }
        return Boolean.TRUE;
    }

    /**
     * 创建或更新reasonIds
     * @return 是否成功
     */
    @RequestMapping(value = "/farmBasic/reasonIds", method = RequestMethod.POST)
    public Boolean createOrUpdateReasonIds(@RequestParam("farmId") Long farmId,
                                          @RequestParam("ids") String ids) {
        if (isEmpty(ids)) {
            return Boolean.TRUE;
        }
        DoctorFarmBasic farmBasic = RespHelper.or500(doctorFarmBasicReadService.findFarmBasicByFarmId(farmId));
        if (farmBasic == null) {
            farmBasic = new DoctorFarmBasic();
            farmBasic.setFarmId(farmId);
            farmBasic.setReasonIds(ids);
            RespHelper.or500(doctorFarmBasicWriteService.createFarmBasic(farmBasic));
        } else {
            farmBasic.setReasonIds(ids);
            RespHelper.or500(doctorFarmBasicWriteService.updateFarmBasic(farmBasic));
        }
        return Boolean.TRUE;
    }

    /**
     * 创建或更新materialIds
     * @return 是否成功
     */
    @RequestMapping(value = "/farmBasic/materialIds", method = RequestMethod.POST)
    public Boolean createOrUpdateMaterialIds(@RequestParam("farmId") Long farmId,
                                           @RequestParam("ids") String ids) {
        if (isEmpty(ids)) {
            return Boolean.TRUE;
        }
        DoctorFarmBasic farmBasic = RespHelper.or500(doctorFarmBasicReadService.findFarmBasicByFarmId(farmId));
        if (farmBasic == null) {
            farmBasic = new DoctorFarmBasic();
            farmBasic.setFarmId(farmId);
            farmBasic.setMaterialIds(ids);
            RespHelper.or500(doctorFarmBasicWriteService.createFarmBasic(farmBasic));
        } else {
            farmBasic.setMaterialIds(ids);
            RespHelper.or500(doctorFarmBasicWriteService.updateFarmBasic(farmBasic));
        }
        return Boolean.TRUE;
    }
}