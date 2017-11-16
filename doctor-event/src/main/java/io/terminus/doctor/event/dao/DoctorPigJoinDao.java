package io.terminus.doctor.event.dao;

import com.google.common.collect.Maps;
import io.terminus.common.model.Paging;
import io.terminus.common.mysql.dao.MyBatisDao;
import io.terminus.common.utils.Constants;
import io.terminus.doctor.event.dto.search.DoctorPigCountDto;
import io.terminus.doctor.event.dto.search.SearchedPig;
import io.terminus.doctor.event.model.DoctorPig;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Desc: 猪join
 * Mail: yangzl@terminus.io
 * author: DreamYoung
 * Date: 2016/12/22
 */
@Repository
public class DoctorPigJoinDao extends MyBatisDao<SearchedPig> {

    public Paging<SearchedPig> pigPagingWithJoin(Map<String, Object> params, Integer offset, Integer limit) {
        if (params == null) {    //如果查询条件为空
            params = Maps.newHashMap();
        }

        Long total = sqlSession.selectOne(sqlId(COUNT), params);
        if (total <= 0){
            return new Paging<>(0L, Collections.emptyList());
        }
        params.put(Constants.VAR_OFFSET, offset);
        params.put(Constants.VAR_LIMIT, limit);
        List<SearchedPig> datas = sqlSession.selectList(sqlId(PAGING), params);
        return new Paging<>(total, datas);
    }

    /**
     * 获取状态母猪数量
     * @param farmId 猪场id
     * @return 母猪数量
     */
    public DoctorPigCountDto findPigCount(Long farmId) {
        return getSqlSession().selectOne(sqlId("findPigCount"), farmId);
    }

    /**
     * 公猪存栏数量
     */
    public Integer findBoarPigCount(Long farmId) {
        return getSqlSession().selectOne(sqlId("findBoarPigCount"), farmId);
    }

    /**
     * 模糊搜索pigCode猪舍下符合
     * @param barnId 猪舍id
     * @param name 模糊搜索字段
     * @param count 返回前count
     * @return
     */
    public List<DoctorPig> suggestSowPig(Long barnId, String name, Integer count){
        Map<String, Object> map = Maps.newHashMap();
        map.put("barnId", barnId);
        map.put("name", name);
        map.put("count", count);
        return sqlSession.selectList(sqlId("suggestSowPig"), map);
    }
}
