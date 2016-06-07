package io.terminus.doctor.msg.service;

import com.google.common.base.Throwables;
import io.terminus.common.model.Response;
import io.terminus.doctor.msg.dao.DoctorMessageRuleDao;
import io.terminus.doctor.msg.dao.DoctorMessageRuleTemplateDao;
import io.terminus.doctor.msg.model.DoctorMessageRule;
import io.terminus.doctor.msg.model.DoctorMessageRuleTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Code generated by terminus code gen
 * Desc: 猪场软件消息规则表写服务实现类
 * Date: 2016-05-31
 * Author: chk@terminus.io
 */
@Slf4j
@Service
public class DoctorMessageRuleWriteServiceImpl implements DoctorMessageRuleWriteService {

    private final DoctorMessageRuleDao doctorMessageRuleDao;
    private final DoctorMessageRuleTemplateDao doctorMessageRuleTemplateDao;


    @Autowired
    public DoctorMessageRuleWriteServiceImpl(DoctorMessageRuleDao doctorMessageRuleDao,
                                             DoctorMessageRuleTemplateDao doctorMessageRuleTemplateDao) {
        this.doctorMessageRuleDao = doctorMessageRuleDao;
        this.doctorMessageRuleTemplateDao = doctorMessageRuleTemplateDao;
    }

    @Override
    public Response<Long> createMessageRule(DoctorMessageRule messageRule) {
        try {
            doctorMessageRuleDao.create(messageRule);
            return Response.ok(messageRule.getId());
        } catch (Exception e) {
            log.error("create messageRule failed, messageRule:{}, cause:{}", messageRule, Throwables.getStackTraceAsString(e));
            return Response.fail("messageRule.create.fail");
        }
    }

    @Override
    public Response<Boolean> updateMessageRule(DoctorMessageRule messageRule) {
        try {
            // 如果是选择默认
            if (1 == messageRule.getUseDefault()) {
                DoctorMessageRuleTemplate template = doctorMessageRuleTemplateDao.findById(messageRule.getTemplateId());
                messageRule.setRuleValue(template.getRuleValue());
            }
            return Response.ok(doctorMessageRuleDao.update(messageRule));
        } catch (Exception e) {
            log.error("update messageRule failed, messageRule:{}, cause:{}", messageRule, Throwables.getStackTraceAsString(e));
            return Response.fail("messageRule.update.fail");
        }
    }

    @Override
    public Response<Boolean> deleteMessageRuleById(Long messageRuleId) {
        try {
            // 逻辑删除
            DoctorMessageRule rule = doctorMessageRuleDao.findById(messageRuleId);
            if (rule != null) {
                rule.setStatus(DoctorMessageRule.Status.DELETE.getValue());
                return Response.ok(doctorMessageRuleDao.update(rule));
            }
            return Response.ok(Boolean.TRUE);
        } catch (Exception e) {
            log.error("delete messageRule failed, messageRuleId:{}, cause:{}", messageRuleId, Throwables.getStackTraceAsString(e));
            return Response.fail("messageRule.delete.fail");
        }
    }

    @Override
    public Response<Boolean> initTemplate(Long farmId) {
        try{
            if (farmId == null) {
                log.error("init template rule for farm failed, farm id can not be null");
                return Response.fail("message.template.rule.fail");
            }
            List<DoctorMessageRuleTemplate> ruleTemplates = doctorMessageRuleTemplateDao.findAllWarnMessageTpl();
            for (int i = 0; ruleTemplates != null && i < ruleTemplates.size(); i++) {
                DoctorMessageRuleTemplate ruleTemplate = ruleTemplates.get(i);
                // 1. 判断模板与farm的关系是否存在
                DoctorMessageRule messageRules = doctorMessageRuleDao.findByTplAndFarm(ruleTemplate.getId(), farmId);
                if (messageRules != null) {
                    continue;
                }
                // 2. 将模板与farm建立关系
                DoctorMessageRule rule = DoctorMessageRule.builder()
                        .farmId(farmId)
                        .templateId(ruleTemplate.getId())
                        .templateName(ruleTemplate.getName())
                        .type(ruleTemplate.getType())
                        .category(ruleTemplate.getCategory())
                        .ruleValue(ruleTemplate.getRuleValue())
                        .useDefault(1) // 使用默认配置
                        .status(DoctorMessageRule.Status.NORMAL.getValue())
                        .describe(ruleTemplate.getDescribe())
                        .build();
                doctorMessageRuleDao.create(rule);
            }
            return Response.ok(Boolean.TRUE);
        } catch (Exception e) {
            log.error("init msg template to farm failed, farm id is {}, cause by {}", farmId, Throwables.getStackTraceAsString(e));
            return Response.fail("init.msg.template.fail");
        }
    }
}
