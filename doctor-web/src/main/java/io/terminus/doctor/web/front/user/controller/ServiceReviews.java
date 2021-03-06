package io.terminus.doctor.web.front.user.controller;

import com.google.common.collect.Lists;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.BaseUser;
import io.terminus.common.model.Response;
import io.terminus.doctor.common.enums.UserType;
import io.terminus.doctor.user.dto.DoctorMenuDto;
import io.terminus.doctor.user.dto.DoctorServiceApplyDto;
import io.terminus.doctor.user.dto.DoctorUserInfoDto;
import io.terminus.doctor.user.model.DoctorOrg;
import io.terminus.doctor.user.model.DoctorServiceReview;
import io.terminus.doctor.user.model.DoctorServiceStatus;
import io.terminus.doctor.user.service.DoctorOrgReadService;
import io.terminus.doctor.user.service.DoctorServiceReviewReadService;
import io.terminus.doctor.user.service.DoctorServiceReviewWriteService;
import io.terminus.doctor.user.service.DoctorServiceStatusReadService;
import io.terminus.doctor.user.service.DoctorUserReadService;
import io.terminus.doctor.user.service.PrimaryUserReadService;
import io.terminus.doctor.user.service.business.DoctorServiceReviewService;
import io.terminus.doctor.web.core.dto.ServiceBetaStatusToken;
import io.terminus.doctor.web.core.service.ServiceBetaStatusHandler;
import io.terminus.pampas.common.UserUtil;
import io.terminus.parana.common.utils.RespHelper;
import io.terminus.parana.user.model.User;
import io.terminus.parana.user.service.UserReadService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;
import java.util.Objects;

import static io.terminus.common.utils.Arguments.notEmpty;

@Slf4j
@RestController
@RequestMapping("/api/user/service")
public class ServiceReviews {
    private final DoctorServiceReviewReadService doctorServiceReviewReadService;
    private final DoctorServiceReviewWriteService doctorServiceReviewWriteService;
    private final DoctorServiceReviewService doctorServiceReviewService;
    private final DoctorOrgReadService doctorOrgReadService;
    private final DoctorUserReadService doctorUserReadService;
    private final DoctorServiceStatusReadService doctorServiceStatusReadService;
    private final PrimaryUserReadService primaryUserReadService;
    private final ServiceBetaStatusHandler serviceBetaStatusHandler;
    private final UserReadService<User> userReadService;

    @Autowired
    public ServiceReviews(DoctorServiceReviewReadService doctorServiceReviewReadService,
                          DoctorServiceReviewWriteService doctorServiceReviewWriteService,
                          DoctorUserReadService doctorUserReadService,
                          DoctorServiceReviewService doctorServiceReviewService,
                          DoctorOrgReadService doctorOrgReadService,
                          DoctorServiceStatusReadService doctorServiceStatusReadService,
                          PrimaryUserReadService primaryUserReadService,
                          ServiceBetaStatusHandler serviceBetaStatusHandler,
                          UserReadService<User> userReadService) {
        this.doctorServiceReviewReadService = doctorServiceReviewReadService;
        this.doctorServiceReviewWriteService = doctorServiceReviewWriteService;
        this.doctorUserReadService = doctorUserReadService;
        this.doctorServiceReviewService = doctorServiceReviewService;
        this.doctorOrgReadService = doctorOrgReadService;
        this.doctorServiceStatusReadService = doctorServiceStatusReadService;
        this.primaryUserReadService = primaryUserReadService;
        this.serviceBetaStatusHandler = serviceBetaStatusHandler;
        this.userReadService = userReadService;
    }

    /**
     * ????????????id?????? ????????????????????????
     * @return ??????????????????
     */
    @RequestMapping(value = "/getUserServiceStatus", method = RequestMethod.GET)
    @ResponseBody
    public DoctorServiceStatus getUserServiceStatus() {
        BaseUser baseUser = UserUtil.getCurrentUser();
        Long primaryUserId; //?????????id

        if(Objects.equals(UserType.FARM_ADMIN_PRIMARY.value(), baseUser.getType())){
            //????????????????????????,???????????????
            primaryUserId = baseUser.getId();
        }else if(Objects.equals(UserType.FARM_SUB.value(), baseUser.getType())){
            //????????????????????????, ??????????????????
            primaryUserId = RespHelper.or500(primaryUserReadService.findSubByUserId(baseUser.getId())).getParentUserId();
        }else{
            throw new JsonResponseException("authorize.fail");
        }
        return RespHelper.or500(doctorServiceStatusReadService.findByUserId(primaryUserId));
    }

    /**
     * ??????????????????, ??????????????????????????????????????????????????????
     * @param serviceApplyDto ????????????
     * @return ??????????????????
     */
    @RequestMapping(value = "/applyOpenService", method = RequestMethod.POST)
    @ResponseBody
    public Boolean applyOpenService(@Valid @RequestBody DoctorServiceApplyDto serviceApplyDto) {
        BaseUser baseUser = UserUtil.getCurrentUser();
        if(!Objects.equals(UserType.FARM_ADMIN_PRIMARY.value(), baseUser.getType())){
            //???????????????(???????????????)????????????????????????
            throw new JsonResponseException("authorize.fail");
        }
        ServiceBetaStatusToken dto = serviceBetaStatusHandler.getServiceBetaStatusToken();
        if(dto.inBeta(DoctorServiceReview.Type.from(serviceApplyDto.getType()))){
            throw new JsonResponseException("service.in.beta");
        }
        if(serviceApplyDto.getOrg() == null){
            throw new JsonResponseException("required.org.info.missing");
        }
        if(StringUtils.isBlank(serviceApplyDto.getOrg().getName())){
            throw new JsonResponseException("org.name.not.null");
        }

        Response<DoctorOrg> orgResponse = doctorOrgReadService.findByName(serviceApplyDto.getOrg().getName());
        if (orgResponse.isSuccess() && orgResponse.getResult() != null) {
            throw new JsonResponseException("org.name.has.existed");
        }

        if(StringUtils.isBlank(serviceApplyDto.getOrg().getLicense())){
            throw new JsonResponseException("org.license.not.null");
        }
        if(StringUtils.isBlank(serviceApplyDto.getOrg().getMobile())){
            throw new JsonResponseException("org.mobile.not.null");
        }
        return RespHelper.or500(doctorServiceReviewService.applyOpenService(baseUser, serviceApplyDto));
    }

    /**
     * ????????????????????????
     * @return ????????????
     * @see io.terminus.doctor.user.enums.RoleType
     */
    @RequestMapping(value = "/getUserRoleType", method = RequestMethod.GET)
    @ResponseBody
    public Integer getUserRoleType() {
        return RespHelper.or500(doctorUserReadService.findUserRoleTypeByUserId(UserUtil.getUserId()));
    }

    /**
     * ????????????????????????
     * @return ??????????????????
     */
    @RequestMapping(value = "/getUserBasicInfo", method = RequestMethod.GET)
    @ResponseBody
    public DoctorUserInfoDto getUserBasicInfo() {
        return RespHelper.or500(doctorUserReadService.findUserInfoByUserId(UserUtil.getUserId()));
    }

    /**
     * ????????????????????????????????????
     * @return ??????id, ????????????, ????????????url, ???????????????
     */
    @RequestMapping(value = "/getOrgInfo", method = RequestMethod.GET)
    @ResponseBody
    public DoctorOrg getOrgInfo() {
        List<DoctorOrg> orgs = RespHelper.or500(doctorOrgReadService.findOrgsByUserId(UserUtil.getUserId()));
        // TODO: 2017/2/16 ????????????????????????????????????
        return notEmpty(orgs) ? orgs.get(0) : null;
    }

    @RequestMapping(value = "/getUserLevelOneMenu", method = RequestMethod.GET)
    @ResponseBody
    public List<DoctorMenuDto> getUserLevelOneMenu() {
        return Lists.newArrayList();
    }

}
