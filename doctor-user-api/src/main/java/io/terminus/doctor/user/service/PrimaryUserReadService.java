package io.terminus.doctor.user.service;

import com.google.common.base.Optional;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import io.terminus.doctor.user.model.PrimaryUser;
import io.terminus.doctor.user.model.Sub;

/**
 * 主账号读服务
 *
 * @author Effet
 */
public interface PrimaryUserReadService {

    /**
     * 通过主键查主账号信息
     *
     * @param id 主账号表主键 ID
     * @return 主账号信息
     */
    Response<PrimaryUser> findPrimaryUserById(Long id);

    /**
     * 通过用户 ID 查询主账号信息
     *
     * @param userId 用户 ID
     * @return 主账号信息
     */
    Response<Optional<PrimaryUser>> findPrimaryUserByUserId(Long userId);

    /**
     * 分页主账号信息
     *
     * @param userId 主账号用户 ID
     * @param status 主账号状态
     * @param pageNo 页码
     * @param size   查询数量
     * @return 主账号分页
     */
    Response<Paging<PrimaryUser>> primaryUserPagination(Long userId, Integer status, Integer pageNo, Integer size);

    /**
     * 通过主键查主账号子账户
     *
     * @param id 子账号关联表主键
     * @return 子账号信息
     */
    Response<Sub> findSubById(Long id);

    /**
     * 通过主账号 ID 和 子账户用户 ID 查询子账号关联
     *
     * @param parentUserId 主账号用户 ID
     * @param userId 子账户用户 ID
     * @return 子账号信息
     */
    Response<Optional<Sub>> findSubSellerByParentUserIdAndUserId(Long parentUserId, Long userId);

    /**
     * 分页子账户信息
     *
     * @param parentUserId 主账号用户 ID
     * @param roleId 角色ID ID
     * @param status 子账户绑定状态
     * @param pageNo 页号
     * @param size   查询数量
     * @return 子账户分页
     */
    Response<Paging<Sub>> subPagination(Long parentUserId, Long roleId, Integer status, Integer pageNo, Integer size);
}
