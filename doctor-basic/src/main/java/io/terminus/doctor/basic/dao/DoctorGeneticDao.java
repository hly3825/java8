package io.terminus.doctor.basic.dao;

import io.terminus.common.mysql.dao.MyBatisDao;
import io.terminus.doctor.basic.model.DoctorGenetic;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Desc: 品系表Dao类
 * Mail: yangzl@terminus.io
 * author: DreamYoung
 * Date: 2016-05-20
 */
@Repository
public class DoctorGeneticDao extends MyBatisDao<DoctorGenetic> {

    public List<DoctorGenetic> findAll() {
        return getSqlSession().selectList(sqlId("findAll"));
    }
}
