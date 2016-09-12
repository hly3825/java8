package io.terminus.doctor.schedule.msg.producer;

import com.google.api.client.util.Maps;
import com.google.common.collect.Lists;
import io.terminus.doctor.common.utils.RespHelper;
import io.terminus.doctor.event.dto.DoctorPigInfoDto;
import io.terminus.doctor.event.enums.DataRange;
import io.terminus.doctor.event.enums.PigEvent;
import io.terminus.doctor.event.enums.PigStatus;
import io.terminus.doctor.event.enums.PregCheckResult;
import io.terminus.doctor.event.model.DoctorPig;
import io.terminus.doctor.event.model.DoctorPigEvent;
import io.terminus.doctor.event.service.DoctorPigReadService;
import io.terminus.doctor.event.service.DoctorPigWriteService;
import io.terminus.doctor.msg.dto.Rule;
import io.terminus.doctor.msg.dto.RuleValue;
import io.terminus.doctor.msg.dto.SubUser;
import io.terminus.doctor.msg.enums.Category;
import io.terminus.doctor.msg.model.DoctorMessage;
import io.terminus.doctor.msg.model.DoctorMessageRule;
import io.terminus.doctor.msg.model.DoctorMessageRuleRole;
import io.terminus.doctor.msg.model.DoctorMessageRuleTemplate;
import io.terminus.doctor.msg.service.DoctorMessageReadService;
import io.terminus.doctor.msg.service.DoctorMessageRuleReadService;
import io.terminus.doctor.msg.service.DoctorMessageRuleRoleReadService;
import io.terminus.doctor.msg.service.DoctorMessageRuleTemplateReadService;
import io.terminus.doctor.msg.service.DoctorMessageTemplateReadService;
import io.terminus.doctor.msg.service.DoctorMessageWriteService;
import io.terminus.doctor.user.service.DoctorUserDataPermissionReadService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Desc: 母猪应淘汰提示
 * <p>
 * 1. 母猪胎次
 * <p>
 * Mail: chk@terminus.io
 * Created by icemimosa
 * Date: 16/6/6
 */
@Component
@Slf4j
public class SowEliminateProducer extends AbstractJobProducer {

    @Autowired
    public SowEliminateProducer(DoctorMessageRuleTemplateReadService doctorMessageRuleTemplateReadService,
                                DoctorMessageRuleReadService doctorMessageRuleReadService,
                                DoctorMessageRuleRoleReadService doctorMessageRuleRoleReadService,
                                DoctorMessageReadService doctorMessageReadService,
                                DoctorMessageWriteService doctorMessageWriteService,
                                DoctorPigReadService doctorPigReadService,
                                DoctorPigWriteService doctorPigWriteService,
                                DoctorMessageTemplateReadService doctorMessageTemplateReadService,
                                DoctorUserDataPermissionReadService doctorUserDataPermissionReadService) {
        super(doctorMessageTemplateReadService,
                doctorMessageRuleTemplateReadService,
                doctorMessageRuleReadService,
                doctorMessageRuleRoleReadService,
                doctorMessageReadService,
                doctorMessageWriteService,
                doctorPigReadService,
                doctorPigWriteService,
                doctorUserDataPermissionReadService,
                Category.SOW_ELIMINATE);
    }

    @Override
    protected List<DoctorMessage> message(DoctorMessageRuleRole ruleRole, List<SubUser> subUsers) {

        log.info("母猪应淘汰消息产生 --- SowEliminateProducer 开始执行");
        List<DoctorMessage> messages = Lists.newArrayList();
        handleMessages(ruleRole.getRule(), ruleRole.getTemplateId(), ruleRole.getFarmId(), true, messages, ruleRole, subUsers);
        log.info("母猪应淘汰消息产生 --- SowEliminateProducer 结束执行, 产生 {} 条消息", messages.size());
        return messages;
    }

    @Override
    protected void recordPigMessages(DoctorMessageRule messageRule) {
        handleMessages(messageRule.getRule(), messageRule.getTemplateId(), messageRule.getFarmId(), false, null, null, null);
    }

    private void handleMessages(Rule rule, Long tplId, Long farmId, boolean isMessage, List<DoctorMessage> messages, DoctorMessageRuleRole ruleRole, List<SubUser> subUsers) {
        // ruleValue map
        Map<Integer, RuleValue> ruleValueMap = Maps.newHashMap();
        for (int i = 0; rule.getValues() != null && i < rule.getValues().size(); i++) {
            RuleValue ruleValue = rule.getValues().get(i);
            ruleValueMap.put(ruleValue.getId(), ruleValue);
        }
        DoctorMessageRuleTemplate ruleTemplate = RespHelper.orServEx(doctorMessageRuleTemplateReadService.findMessageRuleTemplateById(tplId));
        if (StringUtils.isNotBlank(rule.getChannels())) {
            // 批量获取猪信息
            Long total = RespHelper.orServEx(doctorPigReadService.queryPigCount(
                    DataRange.FARM.getKey(), farmId, DoctorPig.PIG_TYPE.SOW.getKey()));
            // 计算size, 分批处理
            Long page = getPageSize(total, 100L);
            DoctorPig pig = DoctorPig.builder()
                    .farmId(farmId)
                    .pigType(DoctorPig.PIG_TYPE.SOW.getKey())
                    .build();
            for (int i = 1; i <= page; i++) {
                List<DoctorPigInfoDto> pigs = RespHelper.orServEx(doctorPigReadService.pagingDoctorInfoDtoByPig(pig, i, 100)).getData();
                // 过滤出未离场的猪
                pigs = pigs.stream().filter(pigDto ->
                        !Objects.equals(PigStatus.Removal.getKey(), pigDto.getStatus())
                ).collect(Collectors.toList());
                // 处理每个猪
                for (int j = 0; pigs != null && j < pigs.size(); j++) {
                    try {
                        DoctorPigInfoDto pigDto = pigs.get(j);
                        //根据用户拥有的猪舍权限过滤拥有user
                        List<SubUser> sUsers = filterSubUserBarnId(subUsers, pigDto.getBarnId());
                        // 母猪的updatedAt与当前时间差 (天)
                        Double timeDiff = (double) (DateTime.now().minus(pigDto.getUpdatedAt().getTime()).getMillis() / 86400000);
                        ruleValueMap.keySet().forEach(key -> {
                            //是否需要发送消息
                            Boolean isSend = false;
                            RuleValue ruleValue = ruleValueMap.get(key);
                            if (key == 1) {
                                if (pigDto.getParity() != null) {
                                    //当前胎次大于或等于预定值
                                    isSend = pigDto.getParity() > ruleValue.getValue().intValue() - 1;
                                }
                            } else if (key == 2) {
                                List<DoctorPigEvent> doctorPigEvents = pigDto.getDoctorPigEvents().stream().filter(doctorPigEvent -> Objects.equals(doctorPigEvent.getType(), PigEvent.FARROWING.getKey())).sorted(Comparator.comparing(DoctorPigEvent::getId).reversed()).collect(Collectors.toList());
                                if (doctorPigEvents.size() > 1) {
                                    //最近两胎产仔数小于或等于预定值
                                    isSend = doctorPigEvents.get(0).getLiveCount() + doctorPigEvents.get(1).getLiveCount() < ruleValue.getValue().intValue() + 1;
                                }
                            } else if (key == 3) {
                                Long count = pigDto.getDoctorPigEvents().stream().filter(doctorPigEvent -> Objects.equals(doctorPigEvent.getType(), PigEvent.PREG_CHECK.getKey())
                                        && !Objects.equals(doctorPigEvent.getPregCheckResult(), PregCheckResult.YANG.getKey())).count();
                                //累计返情、流产、阴性大于或等于预定值
                                isSend = count > ruleValue.getValue().intValue() - 1;

                            } else if (key == 4) {
                                //连续返情、阴性、流产的次数
                                Integer count = 0;
                                List<DoctorPigEvent> events = pigDto.getDoctorPigEvents().stream().filter(doctorPigEvent -> Objects.equals(doctorPigEvent.getType(), PigEvent.MATING.getKey()) || Objects.equals(doctorPigEvent.getType(), PigEvent.PREG_CHECK.getKey())).collect(Collectors.toList());
                                List<List<DoctorPigEvent>> lists = getPigList(events, PigEvent.MATING.getKey());
                                for (List<DoctorPigEvent> list : lists) {
                                    DoctorPigEvent doctorPigEvent;
                                    if (list.isEmpty()) {
                                        break;
                                    }
                                    if (list.size() > 1) {
                                        doctorPigEvent = list.get(list.size() - 1);
                                    } else {
                                        doctorPigEvent = list.get(0);
                                    }
                                    if (!Objects.equals(doctorPigEvent.getPregCheckResult(), PregCheckResult.YANG.getKey())) {
                                        count++;
                                    } else {
                                        count = 0;
                                    }
                                }
                                //连续返情、流产、阴性数大于或等于预定值
                                isSend = count > ruleValue.getValue().intValue() - 1;
                            }
                            if (isSend) {
                                if (!isMessage && Objects.equals(ruleTemplate.getType(), DoctorMessageRuleTemplate.Type.WARNING.getValue())) {
                                    recordPigMessage(pigDto, PigEvent.REMOVAL, null, ruleValue.getValue().intValue(), null);
                                } else if (isMessage) {
                                    pigDto.setReason(ruleValue.getDescribe() + ruleValue.getValue().intValue());
                                    messages.addAll(getMessage(pigDto, rule.getChannels(), ruleRole, sUsers, timeDiff, rule.getUrl()));
                                }
                            }
                        });
                    } catch (Exception e) {
                        log.error("[sowEliminateProduce]-handle.message.failed");
                    }
                }
            }
        }

    }

    private List<List<DoctorPigEvent>> getPigList(List<DoctorPigEvent> events, Integer type) {
        List<List<DoctorPigEvent>> results = Lists.newArrayList();
        List<DoctorPigEvent> tempList = Lists.newArrayList();
        events = events.stream().sorted((a, b) -> b.getEventAt().compareTo(a.getEventAt())).collect(Collectors.toList());
        for (DoctorPigEvent event : events) {
            if (Objects.equals(event.getType(), type)) {
                results.add(tempList);
                tempList = Lists.newArrayList();
            } else {
                tempList.add(event);
            }
        }
        return results;
    }
}
