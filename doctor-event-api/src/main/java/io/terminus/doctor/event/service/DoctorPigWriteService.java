package io.terminus.doctor.event.service;

import io.terminus.common.model.Response;
import io.terminus.doctor.event.model.DoctorPig;

import java.util.List;

/**
 * Created by yaoqijun.
 * Date:2016-05-13
 * Email:yaoqj@terminus.io
 * Descirbe: pig 信息内容
 */
public interface DoctorPigWriteService {

    /**
     * 批量修改猪的耳号
     */
    Response<Boolean> updatePigCodes(List<DoctorPig> pigs);

    /**
     * 更新当前猪舍下猪的猪舍名
     * @param currentBarnId 当前猪舍id
     * @param currentBarnName 新猪舍名
     * @return
     */
    Response<Boolean> updateCurrentBarnName(Long currentBarnId, String currentBarnName);
}
