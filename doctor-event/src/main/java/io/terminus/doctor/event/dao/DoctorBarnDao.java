package io.terminus.doctor.event.dao;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import io.terminus.common.mysql.dao.MyBatisDao;
import io.terminus.common.utils.MapBuilder;
import io.terminus.doctor.common.utils.Iters;
import io.terminus.doctor.event.dto.DoctorBarnCountForPigTypeDto;
import io.terminus.doctor.event.model.DoctorBarn;
import org.springframework.stereotype.Repository;

import javax.validation.constraints.NotNull;
import java.util.Collections;
import java.util.Date;
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

    public DoctorBarn findByOutId(Long farmId, String outId) {
        return getSqlSession().selectOne(sqlId("findByOutId"), ImmutableMap.of("farmId", farmId, "outId", outId));
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


    /**
     * 获取默认妊娠舍
     * @param farmId 猪场id
     * @return 默认妊娠舍
     */
    public DoctorBarn getDefaultPregBarn(Long farmId) {
        return getSqlSession().selectOne(sqlId("getDefaultPregBarn"), farmId);
    }

    /**
     * 模糊搜索有效猪舍
     * @param name 模糊关键字
     * @param count 返回的个数
     * @return 返回满足条件的前count个
     */
    public List<DoctorBarn> selectBarns(Long orgId, Long farmId, String name, Integer count) {
        Map<String, Object> map = Maps.newHashMap();
        map.put("name", name);
        map.put("count", count);
        map.put("orgId", orgId);
        map.put("farmId", farmId);
        return getSqlSession().selectList(sqlId("selectBarns"), map);
    }
    public Map<String,Object> findBarnTypeById(Long barnId) {
        Map<String, Object> map = Maps.newHashMap();
        map.put("barnId", barnId);
        return getSqlSession().selectOne(sqlId("findBarnTypeById"), map);
    }
    public Integer qichucunlan(Long farmId,Long barnId,Date beginTime) {
        Map<String, Object> map = Maps.newHashMap();
        map.put("barnId", barnId);
        map.put("farmId", farmId);
        map.put("beginTime", beginTime);
        return getSqlSession().selectOne(sqlId("qichucunlan"), map);
    }
    public List<Map<Integer,Long>> jianshao(Long barnId,Date beginTime,Date endTime) {
        Map<String, Object> map = Maps.newHashMap();
        map.put("barnId", barnId);
        map.put("endTime", endTime);
        map.put("beginTime", beginTime);
        return getSqlSession().selectList(sqlId("jianshao"), map);
    }
    public Integer zhuanchu(Long barnId,Date beginTime,Date endTime) {
        Map<String, Object> map = Maps.newHashMap();
        map.put("barnId", barnId);
        map.put("endTime", endTime);
        map.put("beginTime", beginTime);
        return getSqlSession().selectOne(sqlId("zhuanchu"), map);
    }
    public List<Long> findBarnIdsByfarmId(Long farmId,String operatorName) {
        Map<String, Object> map = Maps.newHashMap();
        map.put("farmId", farmId);
        map.put("operatorName", operatorName);
        return getSqlSession().selectOne(sqlId("findBarnIdsByfarmId"), map);
    }

}
