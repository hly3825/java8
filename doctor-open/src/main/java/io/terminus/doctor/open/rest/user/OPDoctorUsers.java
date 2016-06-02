package io.terminus.doctor.open.rest.user;

import com.google.common.collect.Lists;
import io.terminus.common.model.BaseUser;
import io.terminus.doctor.common.enums.UserType;
import io.terminus.doctor.open.util.OPRespHelper;
import io.terminus.doctor.user.dto.DoctorMenuDto;
import io.terminus.doctor.user.dto.DoctorServiceApplyDto;
import io.terminus.doctor.user.dto.DoctorServiceReviewDto;
import io.terminus.doctor.user.dto.DoctorUserInfoDto;
import io.terminus.doctor.user.model.DoctorOrg;
import io.terminus.doctor.user.model.DoctorUser;
import io.terminus.doctor.user.service.DoctorOrgReadService;
import io.terminus.doctor.user.service.DoctorServiceReviewReadService;
import io.terminus.doctor.user.service.DoctorServiceReviewWriteService;
import io.terminus.doctor.user.service.DoctorUserReadService;
import io.terminus.doctor.user.service.business.DoctorServiceReviewService;
import io.terminus.pampas.common.UserUtil;
import io.terminus.pampas.openplatform.annotations.OpenBean;
import io.terminus.pampas.openplatform.annotations.OpenMethod;
import io.terminus.pampas.openplatform.exceptions.OPClientException;
import org.springframework.beans.factory.annotation.Autowired;

import javax.validation.Valid;
import java.util.List;
import java.util.Objects;

/**
 * Desc: 用户相关
 * Mail: yangzl@terminus.io
 * author: DreamYoung
 * Date: 16/5/18
 */
@OpenBean
@SuppressWarnings("unused")
public class OPDoctorUsers {

    private final DoctorServiceReviewReadService doctorServiceReviewReadService;

    private final DoctorServiceReviewWriteService doctorServiceReviewWriteService;
    private final DoctorServiceReviewService doctorServiceReviewService;
    private final DoctorOrgReadService doctorOrgReadService;
    private final DoctorUserReadService doctorUserReadService;

    @Autowired
    public OPDoctorUsers(DoctorServiceReviewReadService doctorServiceReviewReadService,
                         DoctorServiceReviewWriteService doctorServiceReviewWriteService,
                         DoctorUserReadService doctorUserReadService,
                         DoctorServiceReviewService doctorServiceReviewService,
                         DoctorOrgReadService doctorOrgReadService) {
        this.doctorServiceReviewReadService = doctorServiceReviewReadService;
        this.doctorServiceReviewWriteService = doctorServiceReviewWriteService;
        this.doctorUserReadService = doctorUserReadService;
        this.doctorServiceReviewService = doctorServiceReviewService;
        this.doctorOrgReadService = doctorOrgReadService;
    }

    /**
     * 根据用户id查询 用户服务开通情况
     * @return 服务开通情况
     */
    @OpenMethod(key = "get.user.service.status")
    public DoctorServiceReviewDto getUserServiceStatus() {
        return OPRespHelper.orOPEx(doctorServiceReviewReadService.findServiceReviewDtoByUserId(UserUtil.getUserId()));
    }

    /**
     * 申请开通服务, 首次申请和驳回后再次申请都可以用这个
     * @param serviceApplyDto 申请信息
     * @return 申请是否成功
     */
    @OpenMethod(key = "apply.open.service", paramNames = "serviceApplyDto")
    public Boolean applyOpenService(@Valid DoctorServiceApplyDto serviceApplyDto) {
        BaseUser baseUser = UserUtil.getCurrentUser();
        if(!Objects.equals(UserType.FARM_ADMIN_PRIMARY.value(), baseUser.getType())){
            //只有主账号(猪场管理员)才能申请开通服务
            throw new OPClientException("authorize.fail");
        }
        return OPRespHelper.orOPEx(doctorServiceReviewService.applyOpenService(baseUser, serviceApplyDto));
    }

    /**
     * 获取用户角色类型
     * @return 角色类型
     * @see io.terminus.doctor.user.enums.RoleType
     */
    @OpenMethod(key = "get.user.role.type")
    public Integer getUserRoleType() {
        return OPRespHelper.orOPEx(doctorUserReadService.findUserRoleTypeByUserId(UserUtil.getUserId()));
    }

    /**
     * 获取用户基本信息
     * @return 用户基本信息
     */
    @OpenMethod(key = "get.user.basic.info")
    public DoctorUserInfoDto getUserBasicInfo() {
        return OPRespHelper.orOPEx(doctorUserReadService.findUserInfoByUserId(UserUtil.getUserId()));
    }

    /**
     * 查询用户所在的公司的信息
     * @return 公司id, 公司名称, 营业执照url, 公司手机号
     */
    @OpenMethod(key = "get.org.info")
    public DoctorOrg getOrgInfo() {
        return OPRespHelper.orOPEx(doctorOrgReadService.findOrgByUserId(UserUtil.getUserId()));
    }

    @OpenMethod(key = "get.user.level.one.menu")
    public List<DoctorMenuDto> getUserLevelOneMenu() {
        return Lists.newArrayList(mockMenuDto(1L), mockMenuDto(2L), mockMenuDto(3L));
    }

    private DoctorMenuDto mockMenuDto(Long id) {
        DoctorMenuDto menuDto = new DoctorMenuDto();
        menuDto.setId(id);
        menuDto.setName("menu" + id);
        menuDto.setLevel(1);
        menuDto.setUrl("/user/info");
        menuDto.setHasIcon(0);
        menuDto.setType(3);
        menuDto.setOrderNo(id.intValue());
        menuDto.setNeedHiden(0);
        return menuDto;
    }
}
