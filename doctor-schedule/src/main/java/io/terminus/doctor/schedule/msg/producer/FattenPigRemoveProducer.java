package io.terminus.doctor.schedule.msg.producer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.api.client.util.Maps;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import io.terminus.common.utils.Splitters;
import io.terminus.doctor.common.enums.PigType;
import io.terminus.doctor.common.utils.RespHelper;
import io.terminus.doctor.event.dto.DoctorGroupDetail;
import io.terminus.doctor.event.dto.DoctorGroupSearchDto;
import io.terminus.doctor.event.dto.DoctorPigInfoDto;
import io.terminus.doctor.event.enums.DataRange;
import io.terminus.doctor.event.model.DoctorPig;
import io.terminus.doctor.event.service.DoctorGroupReadService;
import io.terminus.doctor.event.service.DoctorPigReadService;
import io.terminus.doctor.event.service.DoctorPigWriteService;
import io.terminus.doctor.msg.dto.Rule;
import io.terminus.doctor.msg.dto.RuleValue;
import io.terminus.doctor.msg.dto.SubUser;
import io.terminus.doctor.msg.enums.Category;
import io.terminus.doctor.msg.model.DoctorMessage;
import io.terminus.doctor.msg.model.DoctorMessageRuleRole;
import io.terminus.doctor.msg.service.DoctorMessageReadService;
import io.terminus.doctor.msg.service.DoctorMessageRuleReadService;
import io.terminus.doctor.msg.service.DoctorMessageRuleRoleReadService;
import io.terminus.doctor.msg.service.DoctorMessageRuleTemplateReadService;
import io.terminus.doctor.msg.service.DoctorMessageTemplateReadService;
import io.terminus.doctor.msg.service.DoctorMessageWriteService;
import io.terminus.doctor.schedule.msg.producer.factory.GroupDetailFactory;
import io.terminus.doctor.schedule.msg.producer.factory.PigDtoFactory;
import io.terminus.doctor.user.service.DoctorUserDataPermissionReadService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Created by xiao on 16/8/31.
 */
@Slf4j
@Component
public class FattenPigRemoveProducer extends AbstractJobProducer {

    private DoctorGroupReadService doctorGroupReadService;
    @Autowired
    public FattenPigRemoveProducer(DoctorMessageTemplateReadService doctorMessageTemplateReadService, DoctorMessageRuleTemplateReadService doctorMessageRuleTemplateReadService, DoctorMessageRuleReadService doctorMessageRuleReadService, DoctorMessageRuleRoleReadService doctorMessageRuleRoleReadService, DoctorMessageReadService doctorMessageReadService, DoctorMessageWriteService doctorMessageWriteService, DoctorPigReadService doctorPigReadService, DoctorPigWriteService doctorPigWriteService, DoctorGroupReadService doctorGroupReadService, DoctorUserDataPermissionReadService doctorUserDataPermissionReadService) {
        super(doctorMessageTemplateReadService, doctorMessageRuleTemplateReadService, doctorMessageRuleReadService, doctorMessageRuleRoleReadService, doctorMessageReadService, doctorMessageWriteService, doctorPigReadService, doctorPigWriteService, doctorUserDataPermissionReadService, Category.FATTEN_PIG_REMOVE);
        this.doctorGroupReadService = doctorGroupReadService;
    }

    @Override
    protected List<DoctorMessage> message(DoctorMessageRuleRole ruleRole, List<SubUser> subUsers) {
        log.info("育肥猪出栏提示消息产生 --- FattenPigRemoveProducer 开始执行");
        List<DoctorMessage> messages = Lists.newArrayList();

        Rule rule = ruleRole.getRule();
        // ruleValue map
        Map<Integer, RuleValue> ruleValueMap = Maps.newHashMap();
        for (int i = 0; rule.getValues() != null && i < rule.getValues().size(); i++) {
            RuleValue ruleValue = rule.getValues().get(i);
            ruleValueMap.put(ruleValue.getId(), ruleValue);
        }

        if (StringUtils.isNotBlank(rule.getChannels())) {
            DoctorGroupSearchDto doctorGroupSearchDto = new DoctorGroupSearchDto();
            doctorGroupSearchDto.setPigType(PigType.FATTEN_PIG.getValue());
            doctorGroupSearchDto.setFarmId(ruleRole.getFarmId());
            List<DoctorGroupDetail> groupDetails = RespHelper.or500(doctorGroupReadService.findGroupDetail(doctorGroupSearchDto));
            groupDetails.forEach(doctorGroupDetail -> {
                //根据用户拥有的猪舍权限过滤拥有user
                List<SubUser> sUsers = filterSubUserBarnId(subUsers, doctorGroupDetail.getGroup().getCurrentBarnId());
                if (ruleValueMap.get(1) != null) {
                    if (checkRuleValue(ruleValueMap.get(1), (double)doctorGroupDetail.getGroupTrack().getAvgDayAge()) ) {
                        messages.addAll(getMessage(doctorGroupDetail, rule.getChannels(), ruleRole, sUsers, rule.getUrl()));
                    }
                }
            });
        }
        log.info("育肥猪出栏提示消息产生 --- FattenPigRemoveProducer 结束执行, 产生 {} 条消息", messages.size());
        return messages;
    }

    /**
     * 创建消息
     */
    private List<DoctorMessage> getMessage(DoctorGroupDetail doctorGroupDetail, String channels, DoctorMessageRuleRole ruleRole, List<SubUser> subUsers, String url) {
        List<DoctorMessage> messages = Lists.newArrayList();
        // 创建消息
        Map<String, Object> jsonData = GroupDetailFactory.getInstance().createGroupMessage(doctorGroupDetail, url);

        Splitters.COMMA.splitToList(channels).forEach(channel -> {
            try {
                messages.addAll(createMessage(subUsers, ruleRole, Integer.parseInt(channel), MAPPER.writeValueAsString(jsonData)));
            } catch (JsonProcessingException e) {
                log.error("message produce error, cause by {}", Throwables.getStackTraceAsString(e));
            }
        });
        return messages;
    }
}