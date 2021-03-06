package io.terminus.doctor.user.service;

import com.google.common.base.Throwables;
import io.terminus.boot.rpc.common.annotation.RpcProvider;
import io.terminus.common.exception.ServiceException;
import io.terminus.common.model.Response;
import io.terminus.doctor.common.enums.UserType;
import io.terminus.doctor.common.utils.RespHelper;
import io.terminus.doctor.user.dao.SubDao;
import io.terminus.doctor.user.dao.UserDaoExt;
import io.terminus.doctor.user.dto.DoctorUserInfoDto;
import io.terminus.doctor.user.dto.DoctorUserUnfreezeDto;
import io.terminus.doctor.user.enums.RoleType;
import io.terminus.doctor.user.manager.DoctorUserManager;
import io.terminus.doctor.user.model.DoctorUserDataPermission;
import io.terminus.doctor.user.model.Sub;
import io.terminus.parana.user.impl.dao.UserDao;
import io.terminus.parana.user.impl.service.UserReadServiceImpl;
import io.terminus.parana.user.model.LoginType;
import io.terminus.parana.user.model.User;
import io.terminus.parana.user.model.UserProfile;
import io.terminus.parana.user.service.UserProfileReadService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Objects;

import static io.terminus.common.utils.Arguments.isNull;

/**
 * Desc:
 * Mail: yangzl@terminus.io
 * author: DreamYoung
 * Date: 16/5/18
 */
@Slf4j
@Service
@Primary
@RpcProvider
public class DoctorUserReadServiceImpl extends UserReadServiceImpl implements DoctorUserReadService{

    private final UserDao userDao;
    private final UserDaoExt userDaoExt;
    private final DoctorStaffReadService doctorStaffReadService;
    private final DoctorUserDataPermissionReadService doctorUserDataPermissionReadService;
    private final UserProfileReadService userProfileReadService;
    @Autowired
    private SubDao subDao;
    private final DoctorUserManager doctorUserManager;

    @Autowired
    public DoctorUserReadServiceImpl(UserDao userDao, DoctorStaffReadService doctorStaffReadService,
                                     DoctorUserDataPermissionReadService doctorUserDataPermissionReadService,
                                     UserProfileReadService userProfileReadService,
                                     UserDaoExt userDaoExt, DoctorUserManager doctorUserManager) {
        super(userDao);
        this.userDao = userDao;
        this.doctorStaffReadService = doctorStaffReadService;
        this.doctorUserDataPermissionReadService = doctorUserDataPermissionReadService;
        this.userProfileReadService = userProfileReadService;
        this.userDaoExt = userDaoExt;
        this.doctorUserManager = doctorUserManager;
    }

    /**
     * ??????????????????????????????
     *
     * @param loginId   ????????????
     * @param loginType ??????????????????
     * @return ???????????????
     */
    @Override
    public Response<User> findBy(String loginId, LoginType loginType) {
        try {
            User user;
            switch (loginType) {
                case NAME:
                    user = userDao.findByName(loginId);
                    break;
                case EMAIL:
                    user = userDao.findByEmail(loginId);
                    break;
                case MOBILE:
                    user = userDao.findByMobile(loginId);
                    break;
                default:
                    user = RespHelper.orServEx(subAccountCheck(loginId));
                    break;
            }
            if (user == null) {
                log.error("user(loginId={}, loginType={}) not found", loginId, loginType);
                return Response.fail("user.not.found");
            }
            return Response.ok(user);
        }catch(ServiceException e){
            return Response.fail(e.getMessage());
        } catch (Exception e) {
            log.error("failed to find user(loginId={}, loginType={}), cause:{}",
                    loginId, loginType, Throwables.getStackTraceAsString(e));
            return Response.fail("user.find.fail");
        }
    }

    @Override
    public Response<User> subAccountCheck(String loginId){
        try {
            User user = userDao.findByName(loginId);
            Sub sub = subDao.findByUserId(user.getId());
            //?????????????????????????????????
            if (isNull(sub) || isNull(sub.getFarmId())) {
                throw new ServiceException("sub.relation.not.found");
            }
            return Response.ok(user);
        } catch (ServiceException e) {
            return Response.fail(e.getMessage());
        } catch (Exception e) {
            log.error("failed to check subuser(loginId={}), cause:{}",
                    loginId, Throwables.getStackTraceAsString(e));
            return Response.fail("sub.check.fail");
        }

    }

    @Override
    public Response<Integer> findUserRoleTypeByUserId(Long userId) {
        Response<Integer> response = new Response<>();
        try{
            User user = userDao.findById(userId);
            //?????????
            if(Objects.equals(UserType.ADMIN.value(), user.getType())){
                return Response.ok(RoleType.ADMIN.getValue());
            }

            //?????????
            if(Objects.equals(UserType.FARM_ADMIN_PRIMARY.value(), user.getType())){
                return Response.ok(RoleType.MAIN.getValue());
            }

            //?????????
            if(Objects.equals(UserType.FARM_SUB.value(), user.getType())){
                DoctorUserDataPermission permission = RespHelper.orServEx(doctorUserDataPermissionReadService.findDataPermissionByUserId(userId));
                if(permission != null && !permission.getFarmIdsList().isEmpty()){
                    if(permission.getFarmIdsList().size() == 1){
                        return Response.ok(RoleType.SUB_SINGLE.getValue());
                    }
                    else{
                        return Response.ok(RoleType.SUB_MULTI.getValue());
                    }
                }else{
                    return Response.ok(RoleType.SUB_NONE.getValue());
                }
            }
            //??????
            return Response.fail("user.role.not.vaild");
        }catch(ServiceException e){
            response.setError(e.getMessage());
        }catch(Exception e){
            log.error("findUserRoleTypeByUserId failed, cause : {}", Throwables.getStackTraceAsString(e));
            response.setError("find.user.role.type.by.user.id.failed");
        }
        return response;
    }

    @Override
    public Response<DoctorUserInfoDto> findUserInfoByUserId(Long userId) {
        Response<DoctorUserInfoDto> response = new Response<>();
        try {

            //????????????
            User user = userDao.findById(userId);
            user.setPassword(null);

            //??????????????????
            UserProfile userProfile = RespHelper.orServEx(userProfileReadService.findProfileByUserId(userId));

            //????????????
            Integer roleType = RespHelper.orServEx(findUserRoleTypeByUserId(userId));

            Long farmId = null;
            if(Objects.equals(roleType, RoleType.SUB_SINGLE.getValue())){
                DoctorUserDataPermission permission = RespHelper.orServEx(doctorUserDataPermissionReadService.findDataPermissionByUserId(userId));
                farmId = permission.getFarmIdsList().get(0);
            }

            return RespHelper.ok(
                    DoctorUserInfoDto.builder()
                            .user(user)
                            .userProfile(userProfile)
                            .frontRoleType(roleType)
                            .farmId(farmId)
                            .build()
            );
        }catch(ServiceException e){
            response.setError(e.getMessage());
        }catch(Exception e){
            log.error("findUserInfoByUserId failed, cause : {}", Throwables.getStackTraceAsString(e));
            response.setError("find.user.info.by.user.id.failed");
        }
        return response;
    }


    @Override
    public Response<List<User>> listCreatedUserSince(Date since){
        try{
            return Response.ok(userDaoExt.listCreatedSince(since));
        }catch(Exception e){
            log.error("list created user since {} failed, cause : {}", since, Throwables.getStackTraceAsString(e));
            return Response.fail("list.created.user.failed");
        }
    }

    @Override
    public Response<Boolean> unfreeze(DoctorUserUnfreezeDto doctorUserUnfreezeDto) {
        try {

        } catch (Exception e) {
            log.error(",cause:{}", Throwables.getStackTraceAsString(e));
        }
        return null;
    }

    @Override
    public Response<Boolean> checkExist(String mobile, String name) {
        try {
            doctorUserManager.checkExist(mobile, name);
            return Response.ok(Boolean.TRUE);
        } catch (Exception e) {
            log.error("check exist failed, mobile:{}, name:{},cause:{}", mobile, name, Throwables.getStackTraceAsString(e));
            return Response.fail(e.getMessage());
        }
    }
}
