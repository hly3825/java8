package io.terminus.doctor.user.service;

import io.terminus.common.model.Response;
import io.terminus.doctor.user.model.DoctorUserDataPermission;

import java.util.List;

/**
 * Desc: 用户数据权限读接口
 * Mail: yangzl@terminus.io
 * author: DreamYoung
 * Date: 16/5/18
 */

public interface DoctorUserDataPermissionReadService {

    /**
     * 根据用户id查询所属数据权限
     * @param userId  用户id
     * @return 数据权限
     */
    Response<DoctorUserDataPermission> findDataPermissionByUserId(Long userId);

    /**
     * 根据用户id 批量查询所属数据权限
     * @param userIds  用户id
     * @return 数据权限
     */
    Response<List<DoctorUserDataPermission>> findDataPermissionByUserIds(List<Long> userIds);

    /**
     * 根据id查询所属数据权限
     * @param permissionId  id
     * @return 数据权限
     */
    Response<DoctorUserDataPermission> findDataPermissionById(Long permissionId);

    /**
     * 查询所有权限
     * @return
     */
    Response<List<DoctorUserDataPermission>> listAll();

    /**
     * 查询所有拥有猪场权限的账户的权限
     * @param farmId 猪场id
     * @param userIds 账户id
     * @return 权限列表
     */
    Response<List<DoctorUserDataPermission>> findByFarmAndPrimary(Long farmId, List<Long> userIds);
}
