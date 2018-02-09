package io.terminus.doctor.user.service;

import io.terminus.common.model.Response;
import io.terminus.doctor.user.dto.DoctorUserInfoDto;
import io.terminus.doctor.user.dto.DoctorUserUnfreezeDto;
import io.terminus.parana.user.model.User;
import io.terminus.parana.user.service.UserReadService;

import java.util.Date;
import java.util.List;

/**
 * Desc: 用户读服务
 * Mail: yangzl@terminus.io
 * author: DreamYoung
 * Date: 16/5/18
 */

public interface DoctorUserReadService extends UserReadService<User>{

    /**
     * 根据用户id查询用户前台角色类型
     * @param userId 用户id
     * @return 角色类型
     * @see io.terminus.doctor.user.enums.RoleType
     */
    Response<Integer> findUserRoleTypeByUserId(Long userId);

    /**
     * 查询用户基本信息
     * @param userId 用户id
     * @return 用户信息
     */
    Response<DoctorUserInfoDto> findUserInfoByUserId(Long userId);

    /**
     * 检查子账号是否存在
     * @param loginId
     * @return
     */
    Response<User> subAccountCheck(String loginId);

    /**
     * 查询指定时间之后创建的用户
     * @param since
     * @return
     */
    Response<List<User>> listCreatedUserSince(Date since);

    /**
     * 解冻由于删除猪场导致的用户的冻结
     * @param doctorUserUnfreezeDto
     * @return
     */
    Response<Boolean> unfreeze(DoctorUserUnfreezeDto doctorUserUnfreezeDto);

    /**
     * 校验手机号和用户是否已存在
     * @param mobile
     * @param name
     * @return
     */
    Response<Boolean> checkExist(String mobile, String name);
}
