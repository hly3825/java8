package io.terminus.doctor.event.dao;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableMap;
import io.terminus.common.mysql.dao.MyBatisDao;
import io.terminus.common.utils.MapBuilder;
import io.terminus.doctor.common.utils.Iters;
import io.terminus.doctor.event.dto.DoctorBarnCountForPigTypeDto;
import io.terminus.doctor.event.model.DoctorBarn;
import org.springframework.stereotype.Repository;

import javax.validation.constraints.NotNull;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Desc: 猪舍表Dao类
 * Mail: yangzl@terminus.io
 * author: DreamYoung
 * Date: 2016-05-20
 */
@Repository
public class DoctorBarnDao extends MyBatisDao<DoctorBarn> {

    public DoctorBarn findByOutId(String outId) {
        return getSqlSession().selectOne(sqlId("findByOutId"), outId);
    }

    public List<DoctorBarn> findByOrgId(Long orgId){
        return sqlSession.selectList(sqlId("findByOrgId"), orgId);
    }

    public List<DoctorBarn> findByFarmId(Long farmId) {
        return getSqlSession().selectList(sqlId("findByFarmId"), farmId);
    }

    public List<DoctorBarn> findByFarmIds(List<Long> farmIds) {
        if(farmIds == null || farmIds.isEmpty()){
            return Collections.emptyList();
        }
        return getSqlSession().selectList(sqlId("findByFarmIds"), farmIds);
    }

    public List<DoctorBarn> findByEnums(@NotNull Long farmId, List<Integer> pigTypes, Integer canOpenGroup, Integer status, List<Long> barnIds) {
        return getSqlSession().selectList(sqlId("findByEnums"), MapBuilder.<String, Object>newHashMap()
                .put("farmId", farmId)
                .put("pigTypes", Iters.emptyToNull(pigTypes))
                .put("canOpenGroup", canOpenGroup)
                .put("status", status)
                .put("barnIds", Iters.emptyToNull(barnIds))
                .map());
    }

    /**
     * 猪舍的当前最大的id
     */
    public Long maxId() {
        return MoreObjects.firstNonNull(getSqlSession().selectOne(sqlId("maxId")), 0L);
    }

    /**
     * 查询id小于lastId内且更新时间大于since的limit条数据
     *
     * @param lastId lastId 最大的猪舍id
     * @param since  起始更新时间 yyyy-MM-dd HH:mm:ss
     * @param limit  个数
     */
    public List<DoctorBarn> listSince(Long lastId, String since, int limit) {
        return getSqlSession().selectList(sqlId("listSince"),
                ImmutableMap.of("lastId", lastId, "limit", limit, "since", since));
    }

    /**
     * 统计每种猪舍类型猪舍数量
     * @param criteria 查询条件
     * @return
     */
    public DoctorBarnCountForPigTypeDto countForTypes(Map<String, Object> criteria){
        return getSqlSession().selectOne("countForTypes", criteria);
    }

    public DoctorBarn findBarnByFarmAndBarnName(Map<String, Object> criteria) {
        return getSqlSession().selectOne("findBarnByFarmAndBarnName", criteria);
    }

    /**
     * 更改猪场名
     * @param farmId 需要更改的猪场id
     * @param farmName 新的猪场名
     */
    public void updateFarmName(Long farmId, String farmName) {
        getSqlSession().update(sqlId("updateFarmName"), ImmutableMap.of("farmId", farmId, "farmName", farmName));
    }
}
