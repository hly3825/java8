package io.terminus.doctor.user.dao;

import com.google.common.collect.ImmutableMap;
import io.terminus.common.mysql.dao.MyBatisDao;
import io.terminus.doctor.common.utils.Params;
import io.terminus.doctor.user.model.Sub;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

/**
 * @author houly
 */
@Repository
public class SubDao extends MyBatisDao<Sub> {

    public Sub findByUserId(Long userId) {
        return getSqlSession().selectOne(sqlId("findByUserId"), userId);
    }

    public List<Sub> findByParentUserId(Long parentUserId) {
        return getSqlSession().selectList(sqlId("findByParentUserId"), parentUserId);
    }

    public Sub findFrozenByUserId(Long userId){
        return sqlSession.selectOne(sqlId("findFrozenByUserId"), userId);
    }

    public Sub findByParentUserIdAndUserId(Long parentUserId, Long userId) {
        return getSqlSession().selectOne(sqlId("findByParentUserIdAndUserId"), ImmutableMap.of("parentUserId", parentUserId, "userId", userId));
    }

    /**
     * 获取所有审核通过的子账号
     * @return
     */
    public List<Sub> findAllActiveSubs() {
        return getSqlSession().selectList(sqlId("findAllActiveSubs"));
    }

    /**
     * 多条件筛选, 相当于分页查询去掉了分页参数
     * @param criteria
     * @param limit 限制数量, 可为空
     * @return
     */
    public List<Sub> findByConditions(Map<String, Object> criteria, Integer limit){
        if(limit != null){
            criteria.put("limit", limit);
        }
        return getSqlSession().selectList(sqlId("findByConditions"), ImmutableMap.copyOf(Params.filterNullOrEmpty(criteria)));
    }

    /**
     * 子账号角色名称更新后,此表中的冗余字段也需要跟着更新
     * @param subRoleId 表 doctor_sub_roles 的 主键id, 关联表 doctor_user_subs 的 role_id
     * @param newRoleName 新的角色名称
     * @return
     */
    public void updateRoleName(Long subRoleId, String newRoleName){
        getSqlSession().update(sqlId("updateRoleName"), ImmutableMap.of("roleId", subRoleId, "roleName", newRoleName));
    }

    /**
     * 获取子账号列表
     * @param farmId 猪场id
     * @return 子账号列表
     */
    public List<Sub> findSubsByFarmId(Long farmId) {
        return getSqlSession().selectList(sqlId("findSubsByFarmId"), farmId);
    }


    public List<Sub> findSubsByFarmIdAndStatus(Long farmId, Integer status) {
        return getSqlSession().selectList(sqlId("findSubsByFarmIdAndStatus"), ImmutableMap.of("farmId", farmId, "status", status));
    }

    /**
     * 删除猪场下的子账号
     * @param farmId 猪场id
     */
    public void deleteByFarmId(Long farmId) {
        getSqlSession().delete(sqlId("deleteByFarmId"), farmId);
    }

    public Boolean freezeByUser(Long userId) {
        return getSqlSession().update(sqlId("freezeByUser"), userId) == 1;
    }
}
