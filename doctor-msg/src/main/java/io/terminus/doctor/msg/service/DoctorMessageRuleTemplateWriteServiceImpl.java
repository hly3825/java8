package io.terminus.doctor.msg.service;

import com.google.common.base.Throwables;
import io.terminus.common.model.Response;
import io.terminus.doctor.msg.dao.DoctorMessageRuleTemplateDao;
import io.terminus.doctor.msg.model.DoctorMessageRuleTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Code generated by terminus code gen
 * Desc: 猪场软件消息规则模板表写服务实现类
 * Date: 2016-05-31
 * Author: chk@terminus.io
 */
@Slf4j
@Service
public class DoctorMessageRuleTemplateWriteServiceImpl implements DoctorMessageRuleTemplateWriteService {

    private final DoctorMessageRuleTemplateDao doctorMessageRuleTemplateDao;

    @Autowired
    public DoctorMessageRuleTemplateWriteServiceImpl(DoctorMessageRuleTemplateDao doctorMessageRuleTemplateDao) {
        this.doctorMessageRuleTemplateDao = doctorMessageRuleTemplateDao;
    }

    @Override
    public Response<Long> createMessageRuleTemplate(DoctorMessageRuleTemplate messageRuleTemplate) {
        try {
            doctorMessageRuleTemplateDao.create(messageRuleTemplate);
            return Response.ok(messageRuleTemplate.getId());
        } catch (Exception e) {
            log.error("create messageRuleTemplate failed, messageRuleTemplate:{}, cause:{}", messageRuleTemplate, Throwables.getStackTraceAsString(e));
            return Response.fail("messageRuleTemplate.create.fail");
        }
    }

    @Override
    public Response<Boolean> updateMessageRuleTemplate(DoctorMessageRuleTemplate messageRuleTemplate) {
        try {
            return Response.ok(doctorMessageRuleTemplateDao.update(messageRuleTemplate));
        } catch (Exception e) {
            log.error("update messageRuleTemplate failed, messageRuleTemplate:{}, cause:{}", messageRuleTemplate, Throwables.getStackTraceAsString(e));
            return Response.fail("messageRuleTemplate.update.fail");
        }
    }

    @Override
    public Response<Boolean> deleteMessageRuleTemplateById(Long messageRuleTemplateId) {
        try {
            return Response.ok(doctorMessageRuleTemplateDao.delete(messageRuleTemplateId));
        } catch (Exception e) {
            log.error("delete messageRuleTemplate failed, messageRuleTemplateId:{}, cause:{}", messageRuleTemplateId, Throwables.getStackTraceAsString(e));
            return Response.fail("messageRuleTemplate.delete.fail");
        }
    }
}
