package io.terminus.doctor.web.admin.controller;

import com.google.api.client.util.Lists;
import com.google.common.base.Throwables;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.exception.ServiceException;
import io.terminus.common.model.BaseUser;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import io.terminus.common.utils.Arguments;
import io.terminus.doctor.common.enums.UserType;
import io.terminus.doctor.common.event.CoreEventDispatcher;
import io.terminus.doctor.event.service.DoctorBarnReadService;
import io.terminus.doctor.event.service.DoctorBarnWriteService;
import io.terminus.doctor.user.event.OpenDoctorServiceEvent;
import io.terminus.doctor.user.model.*;
import io.terminus.doctor.user.service.DoctorFarmReadService;
import io.terminus.doctor.user.service.DoctorOrgReadService;
import io.terminus.doctor.user.service.DoctorServiceReviewReadService;
import io.terminus.doctor.user.service.DoctorServiceStatusReadService;
import io.terminus.doctor.user.service.business.DoctorServiceReviewService;
import io.terminus.doctor.web.admin.dto.UserApplyServiceDetailDto;
import io.terminus.doctor.web.admin.service.DoctorInitBarnService;
import io.terminus.doctor.web.admin.service.DoctorInitFarmService;
import io.terminus.doctor.web.core.component.MobilePattern;
import io.terminus.pampas.common.UserUtil;
import io.terminus.parana.common.utils.RespHelper;
import io.terminus.parana.user.model.User;
import io.terminus.parana.user.service.UserReadService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Objects;

/**
 * ????????? 16/5/30.???????????????\?????????????????????controller
 */
@Slf4j
@RestController
@RequestMapping("/api/doctor/admin/service")
public class DoctorServiceReviewController {

    private final DoctorServiceReviewService doctorServiceReviewService;
    private final DoctorServiceReviewReadService doctorServiceReviewReadService;
    private final DoctorOrgReadService doctorOrgReadService;
    private final DoctorFarmReadService doctorFarmReadService;
    private final CoreEventDispatcher coreEventDispatcher;
    private final DoctorInitBarnService doctorInitBarnService;
    private final DoctorInitFarmService doctorInitFarmService;
    private final MobilePattern mobilePattern;
    private final UserReadService userReadService;

    @RpcConsumer
    private DoctorBarnWriteService doctorBarnWriteService;
    @RpcConsumer
    private DoctorBarnReadService doctorBarnReadService;
    @RpcConsumer
    private DoctorServiceStatusReadService serviceStatusReadService;

    @Autowired
    public DoctorServiceReviewController(DoctorServiceReviewService doctorServiceReviewService,
                                         DoctorServiceReviewReadService doctorServiceReviewReadService,
                                         DoctorOrgReadService doctorOrgReadService,
                                         DoctorFarmReadService doctorFarmReadService,
                                         CoreEventDispatcher coreEventDispatcher,
                                         DoctorInitBarnService doctorInitBarnService,
                                         MobilePattern mobilePattern,
                                         UserReadService userReadService,
                                         DoctorInitFarmService doctorInitFarmService){
        this.doctorServiceReviewService = doctorServiceReviewService;
        this.doctorServiceReviewReadService = doctorServiceReviewReadService;
        this.doctorOrgReadService = doctorOrgReadService;
        this.doctorFarmReadService = doctorFarmReadService;
        this.coreEventDispatcher = coreEventDispatcher;
        this.doctorInitBarnService = doctorInitBarnService;
        this.mobilePattern = mobilePattern;
        this.userReadService = userReadService;
        this.doctorInitFarmService = doctorInitFarmService;
    }

    /**
     * ??????????????????????????????????????????????????????
     * @return
     */
    @RequestMapping(value = "/pigdoctor/open", method = RequestMethod.POST)
    @ResponseBody
    public Boolean openDoctorService(@RequestBody UserApplyServiceDetailDto dto){
        BaseUser baseUser = this.checkUserTypeOperator();
        if (dto.getUserId() == null) {
            throw new JsonResponseException(500, "user.id.invalid");
        }
        if(dto.getFarms() == null || dto.getFarms().isEmpty()){
            throw new JsonResponseException(500, "need.at.least.one.farm"); //??????????????????????????????
        }
        if(StringUtils.isBlank(dto.getLoginName()) || StringUtils.containsWhitespace(dto.getLoginName())
                || dto.getLoginName().contains("@") || mobilePattern.getPattern().matcher(dto.getLoginName()).matches()){
            throw new JsonResponseException("login.name.invalid");
        }

        // TODO: 2017/2/16 ??????????????????
        if (dto.getOrg() == null || dto.getOrg().getId() == null) {
            throw new JsonResponseException("orgId.not.null");
        }

        List<DoctorFarm> newFarms = RespHelper.or500(
                doctorServiceReviewService.openDoctorService(baseUser, dto.getUserId(), dto.getLoginName(), dto.getOrg(), dto.getFarms())
        );

        //????????????????????????????????????
        Response<List<Long>> farmResp = doctorFarmReadService.findFarmIdsByUserId(dto.getUserId());
        if(farmResp.isSuccess()){
            coreEventDispatcher.publish(new OpenDoctorServiceEvent(dto.getUserId(), farmResp.getResult()));
        }else{
            log.error("failed to post OpenDoctorServiceEvent due to findFarmsByUserId failing");
        }

        log.info("init barn start, userId:{}, farms:{}", dto.getUserId(), newFarms);
        //??????????????????????????????
        newFarms.forEach(farm -> doctorInitFarmService.initFarm(farm, dto.getUserId()));

        log.info("init barn end");
        return true;
    }

    /**
     * ??????????????????
     * @param userId ?????????????????????id, ??????????????????????????????id
     * @return
     */
    @RequestMapping(value = "/pigmall/open/{userId}", method = RequestMethod.POST)
    @ResponseBody
    public Boolean openPigmallService(@PathVariable Long userId){
        BaseUser baseUser = this.checkUserTypeOperator();
        //???????????????????????????
        return RespHelper.or500(doctorServiceReviewService.openService(baseUser, userId, DoctorServiceReview.Type.PIGMALL));
    }

    /**
     * ?????????????????????
     * @param userId ?????????????????????id, ??????????????????????????????id
     * @return
     */
    @RequestMapping(value = "/neverest/open/{userId}", method = RequestMethod.POST)
    @ResponseBody
    public Boolean openNeverestService(@PathVariable Long userId){
        BaseUser baseUser = this.checkUserTypeOperator();
        //???????????????????????????
        return RespHelper.or500(doctorServiceReviewService.openService(baseUser, userId, DoctorServiceReview.Type.NEVEREST));
    }
    /**
     * ?????????????????????????????????????????????
     * @param userId ?????????????????????id, ??????????????????????????????id
     * @param type ???????????????, ???????????? DoctorServiceReview.Type
     * @return
     */
    @RequestMapping(value = "/notopen", method = RequestMethod.POST)
    @ResponseBody
    public Boolean notOpenService(@RequestParam("userId") Long userId, @RequestParam("type") Integer type, @RequestParam("reason") String reason){
        BaseUser baseUser = this.checkUserTypeOperator();
        try {
            DoctorServiceReview.Type serviceType = DoctorServiceReview.Type.from(type);
            RespHelper.or500(doctorServiceReviewService.notOpenService(baseUser, userId, serviceType, reason));
        } catch (Exception e) {
            throw new JsonResponseException(500, e.getMessage());
        }
        return true;
    }

    /**
     * ??????????????????????????????????????????
     * @param userId ?????????????????????id, ??????????????????????????????id
     * @param type ???????????????, ???????????? DoctorServiceReview.Type
     * @param reason ?????????????????????
     * @return
     */
    @RequestMapping(value = "/froze", method = RequestMethod.POST)
    @ResponseBody
    public Boolean frozeApply(@RequestParam("userId") Long userId, @RequestParam("type") Integer type, @RequestParam("reason") String reason){
        BaseUser baseUser = this.checkUserTypeOperator();
        try {
            DoctorServiceReview.Type serviceType = DoctorServiceReview.Type.from(type);
            RespHelper.or500(doctorServiceReviewService.frozeApply(baseUser, userId, serviceType, reason));
        } catch (Exception e) {
            throw new JsonResponseException(500, e.getMessage());
        }
        return true;
    }

    /**
     * ?????????????????????????????????, ???????????????????????????
     * @param userId ????????????????????????id, ????????????, ????????????????????????id
     * @param type ????????????, ??????DoctorServiceReview.Type
     * @param userMobile ???????????????????????????
     * @param realName ??????????????????????????????
     * @param status ?????????????????????
     *               @see io.terminus.doctor.user.model.DoctorServiceReview.Status
     * @param pageNo ?????????
     * @param pageSize ????????????
     * @return
     */
    @RequestMapping(value = "/apply/page", method = RequestMethod.GET)
    @ResponseBody
    public Paging<DoctorServiceReviewExt> pageServiceApplies(@RequestParam(value = "userId", required = false) Long userId,
                                                             @RequestParam(value = "type", required = false) Integer type,
                                                             @RequestParam(value = "userMobile", required = false) String userMobile,
                                                             @RequestParam(value = "realName", required = false) String realName,
                                                             @RequestParam(value = "orgName", required = false) String orgName,
                                                             @RequestParam(required = false) Integer status,
                                                             @RequestParam(required = false) Integer pageNo,
                                                             @RequestParam(required = false) Integer pageSize){
        try {
            DoctorServiceReview.Type servicetype = null;
            if (type != null) {
                servicetype = DoctorServiceReview.Type.from(type);
            }
            DoctorServiceReview.Status statusEnum = null;
            if(status != null){
                statusEnum = DoctorServiceReview.Status.from(status);
            }
            Paging<DoctorServiceReviewExt> doctorServiceReviewExtPaging = RespHelper.or500(
                    doctorServiceReviewReadService.page(pageNo, pageSize, userId, userMobile, realName, servicetype, statusEnum, orgName)
            );
            List<DoctorServiceReviewExt> result = Lists.newArrayList();
            if(doctorServiceReviewExtPaging.getTotal() > 0){
                doctorServiceReviewExtPaging.getData().stream().forEach(doctorServiceReviewExt -> {

                    List<DoctorOrg> orgs = RespHelper.or500(doctorOrgReadService.findOrgsByUserId(doctorServiceReviewExt.getUserId()));
                    if(!Arguments.isNullOrEmpty(orgs)){
                        doctorServiceReviewExt.setOrgs(orgs);
                    }

                });
            }
            return doctorServiceReviewExtPaging;
        } catch (ServiceException e) {
            log.error("pageServiceApplies failed, cause : {}", Throwables.getStackTraceAsString(e));
            throw new JsonResponseException(500, e.getMessage());
        }
    }

    @RequestMapping(value = "/opened/page", method = RequestMethod.GET)
    @ResponseBody
    public Paging<DoctorServiceReviewExt> pageOpenedService(
            @RequestParam(value = "userId", required = false) Long userId,
            @RequestParam(value = "type", required = false, defaultValue = "1") Integer type,
            @RequestParam(value = "userMobile", required = false) String userMobile,
            @RequestParam(value = "orgName", required = false) String orgName,
            @RequestParam(required = false) Integer pageNo,
            @RequestParam(required = false) Integer pageSize){
        DoctorServiceReview.Type typeEnum = DoctorServiceReview.Type.from(type);
        DoctorServiceReview.Status statusEnum = DoctorServiceReview.Status.from(1);
        Paging<DoctorServiceReviewExt> doctorServiceReviewExtPaging = RespHelper.or500(
                doctorServiceReviewReadService.page(pageNo, pageSize, userId, userMobile, null, typeEnum, statusEnum, orgName)
        );
        List<DoctorServiceReviewExt> result = Lists.newArrayList();
        if(doctorServiceReviewExtPaging.getTotal() > 0){
            doctorServiceReviewExtPaging.getData().stream().forEach(doctorServiceReviewExt -> {

                List<DoctorOrg> orgs = RespHelper.or500(doctorOrgReadService.findOrgsByUserId(doctorServiceReviewExt.getUserId()));
                if(!Arguments.isNullOrEmpty(orgs)){
                    doctorServiceReviewExt.setOrgs(orgs);
                }

            });
        }
        return doctorServiceReviewExtPaging;
    }

    /**
     * ???????????????????????????, admin???????????????, ????????????????????????????????????
     * @param userId
     * @return
     */
    @RequestMapping(value = "/pigdoctor/detail/{userId}", method = RequestMethod.GET)
    @ResponseBody
    public UserApplyServiceDetailDto findUserApplyDetail(@PathVariable Long userId){
        UserApplyServiceDetailDto dto = new UserApplyServiceDetailDto();
        List<DoctorFarm> farms = RespHelper.or500(doctorFarmReadService.findFarmsByUserId(userId));
        List<DoctorOrg> orgs = RespHelper.or500(doctorOrgReadService.findOrgsByUserId(userId));
        dto.setFarms(farms);
        dto.setUserId(userId);
        if(!Arguments.isNullOrEmpty(orgs)){
            dto.setOrg(orgs.get(0)); // TODO: 2017/2/16 ??????????????????
        }
        return dto;
    }

    /**
     * ???????????????????????????????????????, ?????????????????????????????????
     * @return
     */
    private BaseUser checkUserTypeOperator(){
        BaseUser baseUser = UserUtil.getCurrentUser();
        if(!Objects.equals(UserType.ADMIN.value(), baseUser.getType())){
            throw new JsonResponseException("authorize.fail");
        }
        return baseUser;
    }
}
