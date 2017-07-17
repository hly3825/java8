package io.terminus.doctor.user.service;

import io.terminus.common.model.Response;
import io.terminus.doctor.user.model.DoctorOrg;

import javax.validation.constraints.NotNull;
import java.util.List;

public interface DoctorOrgReadService {

    /**
     * 根据公司id查询公司信息
     * @param orgId 公司id
     * @return 公司信息
     */
    Response<DoctorOrg> findOrgById(Long orgId);

    /**
     * 根据公司id查询公司信息
     * @param orgIds 公司ids
     * @return 公司信息
     */
    Response<List<DoctorOrg>> findOrgByIds(List<Long> orgIds);

    /**
     * 根据用户id查询有权限的公司
     * @param userId 用户id
     * @return 公司列表
     */
    Response<List<DoctorOrg>> findOrgsByUserId(@NotNull(message = "userId.not.null") Long userId);

    /**
     * 查询所有公司
     * @return 所有公司
     */
    Response<List<DoctorOrg>> findAllOrgs();

    /**
     * 查询父公司下所有子公司
     * @param parentId 父公司id
     * @return 子公司列表
     */
    Response<List<DoctorOrg>> findOrgByParentId(Long parentId);
}
