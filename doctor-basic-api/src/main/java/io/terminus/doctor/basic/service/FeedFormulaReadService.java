package io.terminus.doctor.basic.service;

import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import io.terminus.doctor.basic.model.FeedFormula;

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

    /**
     * 分页查询配方列表数据
     * @param formulaName
     * @param feedId
     * @param pageNo
     * @param pageSize
     * @return
     */
    Response<Paging<FeedFormula>> pagingFormulaList(Long farmId,String formulaName,
                                                    Long feedId,
                                                    Integer pageNo,
                                                    Integer pageSize);

    /**
     * 分页查询配方
     * @param feedId
     * @param farmId
     * @param feedName
     * @param pageNo
     * @param size
     * @return
     */
    Response<Paging<FeedFormula>> paging(Long feedId, Long farmId, String feedName, Integer pageNo, Integer size);
}
