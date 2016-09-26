package io.terminus.doctor.warehouse.service;

import io.terminus.common.model.Response;
import io.terminus.doctor.warehouse.model.FeedFormula;

import java.util.List;

/**
 * Code generated by terminus code gen
 * Desc: 饲料配方表读服务
 * Date: 2016-09-26
 */

public interface FeedFormulaReadService {

    /**
     * 根据id查询饲料配方表
     * @param feedFormulaId 主键id
     * @return 饲料配方表
     */
    Response<FeedFormula> findFeedFormulaById(Long feedFormulaId);

    /**
     * 查询配方
     * @param feedId 饲料(即物料)的id
     * @param farmId
     * @return
     */
    Response<FeedFormula> findFeedFormulaById(Long feedId, Long farmId);
}
