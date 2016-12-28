package io.terminus.doctor.user.dao;

import com.google.common.collect.Maps;
import io.terminus.common.model.Paging;
import io.terminus.common.mysql.dao.MyBatisDao;
import io.terminus.doctor.user.model.DoctorServiceReviewExt;
import io.terminus.doctor.user.model.DoctorServiceStatus;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Code generated by terminus code gen
 * Desc: 用户服务状态表Dao类
 * Date: 2016-06-03
 */
@Repository
public class DoctorServiceStatusDao extends MyBatisDao<DoctorServiceStatus> {

    public DoctorServiceStatus findByUserId(Long userId){
        return sqlSession.selectOne(sqlId("findByUserId"), userId);
    }

    public boolean updateWithNull(DoctorServiceStatus doctorServiceStatus){
        return sqlSession.update(sqlId("updateWithNull"), doctorServiceStatus) == 1;
    }

    public Paging<DoctorServiceReviewExt> pagingExt(Map<String, Object> criteria) {
        if (criteria == null) {    //如果查询条件为空
            criteria = Maps.newHashMap();
        }

        Long total = sqlSession.selectOne(sqlId(COUNT), criteria);
        if (total <= 0){
            return new Paging<>(0L, Collections.<DoctorServiceReviewExt>emptyList());
        }

        List<DoctorServiceReviewExt> datas = sqlSession.selectList(sqlId(PAGING), criteria);
        return new Paging<>(total, datas);
    }
}
