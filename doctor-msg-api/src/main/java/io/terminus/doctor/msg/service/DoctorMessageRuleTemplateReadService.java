package io.terminus.doctor.msg.service;

import io.terminus.common.model.Response;
import io.terminus.doctor.msg.model.DoctorMessageRuleTemplate;

/**
 * Code generated by terminus code gen
 * Desc: 猪场软件消息规则模板表读服务
 * Date: 2016-05-31
 * Author: chk@terminus.io
 */

public interface DoctorMessageRuleTemplateReadService {

    /**
     * 根据id查询猪场软件消息规则模板表
     * @param messageRuleTemplateId 主键id
     * @return 猪场软件消息规则模板表
     */
    Response<DoctorMessageRuleTemplate> findMessageRuleTemplateById(Long messageRuleTemplateId);
}
