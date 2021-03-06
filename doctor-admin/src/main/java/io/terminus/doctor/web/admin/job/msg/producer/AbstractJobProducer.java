package io.terminus.doctor.web.admin.job.msg.producer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.MoreObjects;
import com.google.common.base.Stopwatch;
import com.google.common.collect.Lists;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.ServiceException;
import io.terminus.common.utils.Arguments;
import io.terminus.common.utils.BeanMapper;
import io.terminus.common.utils.JsonMapper;
import io.terminus.doctor.common.utils.RespHelper;
import io.terminus.doctor.event.dto.DoctorGroupDetail;
import io.terminus.doctor.event.dto.DoctorPigInfoDto;
import io.terminus.doctor.event.dto.msg.DoctorMessageSearchDto;
import io.terminus.doctor.event.dto.msg.RuleValue;
import io.terminus.doctor.event.dto.msg.SubUser;
import io.terminus.doctor.event.enums.Category;
import io.terminus.doctor.event.enums.PigEvent;
import io.terminus.doctor.event.enums.PigStatus;
import io.terminus.doctor.event.enums.PregCheckResult;
import io.terminus.doctor.event.model.DoctorGroupEvent;
import io.terminus.doctor.event.model.DoctorMessage;
import io.terminus.doctor.event.model.DoctorMessageRule;
import io.terminus.doctor.event.model.DoctorMessageRuleRole;
import io.terminus.doctor.event.model.DoctorMessageRuleTemplate;
import io.terminus.doctor.event.model.DoctorMessageUser;
import io.terminus.doctor.event.model.DoctorPig;
import io.terminus.doctor.event.model.DoctorPigEvent;
import io.terminus.doctor.event.service.DoctorGroupReadService;
import io.terminus.doctor.event.service.DoctorMessageReadService;
import io.terminus.doctor.event.service.DoctorMessageRuleReadService;
import io.terminus.doctor.event.service.DoctorMessageRuleRoleReadService;
import io.terminus.doctor.event.service.DoctorMessageRuleTemplateReadService;
import io.terminus.doctor.event.service.DoctorMessageTemplateReadService;
import io.terminus.doctor.event.service.DoctorMessageUserWriteService;
import io.terminus.doctor.event.service.DoctorMessageWriteService;
import io.terminus.doctor.event.service.DoctorPigEventReadService;
import io.terminus.doctor.event.service.DoctorPigReadService;
import io.terminus.doctor.event.service.DoctorPigWriteService;
import io.terminus.doctor.user.model.DoctorUserDataPermission;
import io.terminus.doctor.user.service.DoctorUserDataPermissionReadService;
import io.terminus.doctor.user.service.PrimaryUserReadService;
import io.terminus.doctor.web.admin.job.msg.dto.DoctorMessageInfo;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Desc: Job?????????Producer
 * Mail: chk@terminus.io
 * Created by IceMimosa
 * Date: 16/7/6
 */
@Slf4j
public abstract class AbstractJobProducer {

    protected ObjectMapper MAPPER = JsonMapper.JSON_NON_DEFAULT_MAPPER.getMapper();

    @Autowired
    protected DoctorMessageTemplateReadService doctorMessageTemplateReadService;
    @Autowired
    protected DoctorMessageRuleTemplateReadService doctorMessageRuleTemplateReadService;
    @Autowired
    protected DoctorMessageRuleReadService doctorMessageRuleReadService;
    @Autowired
    protected DoctorMessageRuleRoleReadService doctorMessageRuleRoleReadService;
    @Autowired
    protected DoctorMessageReadService doctorMessageReadService;
    @Autowired
    protected DoctorMessageWriteService doctorMessageWriteService;
    @Autowired
    protected DoctorUserDataPermissionReadService doctorUserDataPermissionReadService;
    @Autowired
    protected DoctorPigReadService doctorPigReadService;
    @RpcConsumer
    protected DoctorPigEventReadService doctorPigEventReadService;
    @RpcConsumer
    protected DoctorGroupReadService doctorGroupReadService;
    @Autowired
    protected DoctorPigWriteService doctorPigWriteService;
    @Autowired
    protected DoctorMessageUserWriteService doctorMessageUserWriteService;

    @Autowired
    private PrimaryUserReadService primaryUserReadService;

    @Value("${msg.jumpUrl.pig.sow}")
    protected String sowPigDetailUrl;

    @Value("${msg.jumpUrl.pig.boar}")
    protected String boarPigDetailUrl;

    @Value("${msg.jumpUrl.group}")
    protected String groupDetailUrl;

    protected Category category;
    @Autowired
    public AbstractJobProducer(Category category){
        this.category = category;
    }

    /**
     * ????????????
     */
    public void produce() {
        //??????????????????
        List<DoctorMessageRuleTemplate> ruleTemplates = RespHelper.orServEx(doctorMessageRuleTemplateReadService.findByCategory(category.getKey()));
        for (int i = 0; ruleTemplates != null && i < ruleTemplates.size(); i++) {
            DoctorMessageRuleTemplate ruleTemplate = ruleTemplates.get(i);
            // ???????????????, ??????????????????
            if (ruleTemplate == null || !Objects.equals(ruleTemplate.getStatus(), DoctorMessageRuleTemplate.Status.NORMAL.getValue())) {
                return;
            }

            // 1. ?????????????????????
            if (Objects.equals(DoctorMessageRuleTemplate.Type.SYSTEM.getValue(), ruleTemplate.getType())) {
                Stopwatch stopwatch = Stopwatch.createStarted();
                log.info("[AbstractJobProducer] {} -> ??????????????????, starting......", ruleTemplate.getName());
                DoctorMessageRuleRole ruleRole = DoctorMessageRuleRole.builder()
                        .templateId(ruleTemplate.getId())
                        .ruleValue(ruleTemplate.getRuleValue())
                        .build();
                // ???????????? (?????????????????????/user)
                message(ruleRole, getUsersHasFarm(null));
                stopwatch.stop();
                log.info("[AbstractJobProducer] {} -> ??????????????????????????????, ?????? {}ms end......", ruleTemplate.getName(), stopwatch.elapsed(TimeUnit.MILLISECONDS));
            }

            // 2. ??????????????????????????????
            else {
                Stopwatch stopWatch = Stopwatch.createStarted();
                log.info("[AbstractJobProducer] {} -> ??????????????????, starting......", ruleTemplate.getName());

                // > ?????????????????????????????????
                List<DoctorMessageRule> messageRules = RespHelper.orServEx(doctorMessageRuleReadService.findMessageRulesByTplId(ruleTemplate.getId()));
                messageRules.forEach(this::createWarnMessageByMessageRule);
                stopWatch.stop();
                log.info("[AbstractJobProducer] {} -> ????????????????????????, ?????? {}ms, ending......", ruleTemplate.getName(), stopWatch.elapsed(TimeUnit.MILLISECONDS));
            }
        }
    }

    /**
     * ??????????????????????????????
     * @param messageRule
     */
    public void createWarnMessageByMessageRule(DoctorMessageRule messageRule) {
        //?????????????????????
        DoctorMessageSearchDto dto = new DoctorMessageSearchDto();
        dto.setRuleId(messageRule.getId());
        deleteMessages(dto);

        if (!Objects.equals(messageRule.getStatus(), DoctorMessageRule.Status.NORMAL.getValue())) {
            return;
        }
        DoctorMessageRuleRole ruleRole = DoctorMessageRuleRole.builder()
                .ruleId(messageRule.getId())
                .templateId(messageRule.getTemplateId())
                .farmId(messageRule.getFarmId())
                .ruleValue(messageRule.getRuleValue())
                .build();
        message(ruleRole, getUsersHasFarm(messageRule.getFarmId()));
    }

    /**
     * ???????????????????????????????????????(farmId???null,????????????????????????)
     * @param farmId ??????Id
     * @return
     */
    private List<SubUser> getUsersHasFarm(Long farmId){
        List<SubUser> subUsers = Lists.newArrayList();
        List<DoctorUserDataPermission> permissionList = RespHelper.orServEx(doctorUserDataPermissionReadService.listAll());
        permissionList.forEach(dataPermission -> {
            SubUser subUser = SubUser.builder()
                    .userId(dataPermission.getUserId())
                    .farmIds(Lists.newArrayList())
                    .barnIds(Lists.newArrayList())
                    .build();
            if (farmId == null){
                subUsers.add(subUser);
            } else {
                // ??????????????????
                dataPermission.setFarmIds(dataPermission.getFarmIds());
                if (dataPermission.getFarmIdsList().contains(farmId)) {
                    dataPermission.setBarnIds(dataPermission.getBarnIds());
                    subUser.getFarmIds().addAll(dataPermission.getFarmIdsList());
                    subUser.getBarnIds().addAll(dataPermission.getBarnIdsList());
                    subUsers.add(subUser);
                }
            }

        });
        return subUsers;
    }


    /**
     * ??????DoctorMessage??????
     * @param subUsers ?????????(????????????roleId??????)
     * @param ruleRole ????????????
     * @param messageInfo ????????????
     */
    protected void createMessage(List<SubUser> subUsers, DoctorMessageRuleRole ruleRole, DoctorMessageInfo messageInfo) {
        DoctorMessageRuleTemplate template = RespHelper.orServEx(doctorMessageRuleTemplateReadService.findMessageRuleTemplateById(ruleRole.getTemplateId()));
        //1.???????????????????????????????????????
        if (Arguments.isNullOrEmpty(subUsers)){
            return;
        }
        //2.???????????????
        DoctorMessage message = BeanMapper.map(messageInfo, DoctorMessage.class);
        message.setFarmId(ruleRole.getFarmId());
        message.setRuleId(ruleRole.getRuleId());
        message.setRoleId(ruleRole.getRoleId());
        message.setTemplateName(template.getName());
        message.setTemplateId(ruleRole.getTemplateId());
        message.setMessageTemplate(template.getMessageTemplate());
        message.setType(template.getType());
        message.setCategory(template.getCategory());
        message.setCreatedBy(template.getUpdatedBy());
        // ????????????
//        try {
//            Map<String, Serializable> jsonContext = MAPPER.readValue(jsonData, JacksonType.MAP_OF_STRING);
//            String content = RespHelper.orServEx(doctorMessageTemplateReadService.getMessageContentWithCache(message.getMessageTemplate(), jsonContext));
//            message.setContent(content != null ? content.trim() : "");
//        } catch (exception e) {
//            log.error("compile message template failed,cause by {}, template name is {}, json map is {}", Throwables.getStackTraceAsString(e), message.getMessageTemplate(), jsonData);
//        }
        Long messageId = RespHelper.orServEx(doctorMessageWriteService.createMessage(message));

        //3.????????????
        subUsers.forEach(subUser -> {
            DoctorMessageUser doctorMessageUser = DoctorMessageUser.builder()
                    .userId(subUser.getUserId())
                    .messageId(messageId)
                    .businessId(messageInfo.getBusinessId())
                    .ruleValueId(messageInfo.getRuleValueId())
                    .farmId(ruleRole.getFarmId())
                    .templateId(ruleRole.getTemplateId())
                    .statusSys(DoctorMessageUser.Status.NORMAL.getValue())
                    .statusSms(DoctorMessageUser.Status.NORMAL.getValue())
                    .statusEmail(DoctorMessageUser.Status.NORMAL.getValue())
                    .statusApp(DoctorMessageUser.Status.NORMAL.getValue())
                    .build();
            doctorMessageUserWriteService.createDoctorMessageUser(doctorMessageUser);
        });
    }

    /**
     * ???????????????
     *
     * @param total ?????????
     * @param size  ?????????????????????
     * @return Long
     */
    protected Long getPageSize(Long total, Long size) {
        size = MoreObjects.firstNonNull(size, 100L);
        Long page = 0L;
        if (total != null) {
            if (total % size == 0) {
                page = total / size;
            } else {
                page = total / size + 1;
            }
        }
        return page;
    }

    /**
     * ????????????????????????
     * @param doctorMessageSearchDto ????????????
     */
    private void deleteMessages(DoctorMessageSearchDto doctorMessageSearchDto) {
        while (true) {
            List<Long> messageIds = RespHelper.orServEx(doctorMessageReadService.pagingWarnMessages(doctorMessageSearchDto, 1, 1000)).getData().stream().map(DoctorMessage::getId).collect(Collectors.toList());
            if (!Arguments.isNullOrEmpty(messageIds)){
                doctorMessageWriteService.deleteMessagesByIds(messageIds);
                doctorMessageUserWriteService.deletesByMessageIds(messageIds);
            }
            if (messageIds.size() < 1000) {
                break;
            }
        }
    }

    /**
     * ????????????????????????????????????
     *
     * @param pigDto
     * @return
     */
    protected DoctorPigEvent getMatingPigEvent(DoctorPigInfoDto pigDto) {
        try {
//            List<DoctorPigEvent> eventList = pigDto.getDoctorPigEvents().stream().filter(doctorPigEvent -> doctorPigEvent.getEventAt() != null).sorted(this::pigEventCompare).collect(Collectors.toList());
//            DoctorPigEvent doctorPigEvent = null;
//            Boolean flag = false;
//            for (DoctorPigEvent event : eventList) {
//                if (flag && !Objects.equals(event.getType(), PigEvent.MATING.getKey())) {
//                    break;
//                }
//                if (Objects.equals(event.getType(), PigEvent.MATING.getKey())) {
//                    flag = true;
//                    doctorPigEvent = event;
//                }
//            }
            return RespHelper.orServEx(doctorPigEventReadService.findLastFirstMateEvent(pigDto.getPigId()));
        } catch (Exception e) {
            log.error("get mating date fail");
        }
        return null;
    }

    /**
     * ?????????????????????????????????
     *
     * @param pigDto
     * @return
     */
    protected DoctorPigEvent getStatusEvent(DoctorPigInfoDto pigDto) {
        try {
            PigStatus STATUS = PigStatus.from(pigDto.getStatus());
            DoctorPigEvent doctorPigEvent;
            if (STATUS != null) {
                switch (STATUS) {
                    case Entry:    // ??????
                        return getPigEventByEventType(pigDto.getPigId(), PigEvent.ENTRY.getKey());
                    case Wean:     //??????
                        return getPigEventByEventType(pigDto.getPigId(), PigEvent.WEAN.getKey());
                    case KongHuai: // ??????
                        doctorPigEvent = getPigEventByEventType(pigDto.getPigId(), PigEvent.PREG_CHECK.getKey());
                        pigDto.setStatusName(PregCheckResult.from(doctorPigEvent.getPregCheckResult()).getDesc());
                        return doctorPigEvent;
                }
            }
        } catch (Exception e) {
            log.error("SowPregCheckProducer get status date failed, pigDto is {}", pigDto);
        }
        return null;
    }

    /**
     * ????????????????????????
     *
     * @param subUsers
     * @param barnId
     * @return
     */
    protected List<SubUser> filterSubUserBarnId(List<SubUser> subUsers, Long barnId) {
        if (Arguments.isNullOrEmpty(subUsers)) {
            return Collections.emptyList();
        }
        return subUsers.stream().filter(subUser -> filterCondition(subUser, barnId)).collect(Collectors.toList());
    }

    /**
     * ??????????????????
     *
     * @param subUser
     * @param barnId
     * @return
     */
    private Boolean filterCondition(SubUser subUser, Long barnId) {
        return !Arguments.isNullOrEmpty(subUser.getBarnIds()) && subUser.getBarnIds().contains(barnId);
    }

    /**
     * ???????????????????????????????????????????????????
     *
     * @param pigId
     * @param type
     * @return DoctorPigEvent
     */
    protected DoctorPigEvent getPigEventByEventType(Long pigId, Integer type) {
        try {
            return RespHelper.orServEx(doctorPigEventReadService.findLastEventByType(pigId, type));
        } catch (Exception e) {
            log.error("get.pig.event.by.event.type.failed");
        }
        return null;
    }

    /**
     * ???????????????????????????
     * @param groupId
     * @param type
     * @return
     */
    protected DoctorGroupEvent getLastGroupEventByEventType(Long groupId, Integer type) {
        try {
            return RespHelper.orServEx(doctorGroupReadService.findLastGroupEventByType(groupId, type));
        } catch (Exception e) {
            log.error("get.last.group.event.by.event.type.failed");
        }
        return null;
    }

    /**
     * ??????????????????url
     * @param pigDto ?????????
     * @return url
     */
    protected String getPigJumpUrl(DoctorPigInfoDto pigDto) {
        if (Objects.equals(pigDto.getPigType(), DoctorPig.PigSex.SOW.getKey())) {
            return sowPigDetailUrl.concat("?pigId=" + pigDto.getPigId() + "&farmId=" + pigDto.getFarmId());
        } else if (Objects.equals(pigDto.getPigType(), DoctorPig.PigSex.BOAR.getKey())) {
            return boarPigDetailUrl.concat("?pigId=" + pigDto.getPigId() + "&farmId=" + pigDto.getFarmId());
        }
        throw new ServiceException("pigSex.failed");

    }

    /**
     * ????????????????????????url
     * @param doctorGroupDetail ????????????
     * @param ruleRole ????????????
     * @return url
     */
    protected String getGroupJumpUrl(DoctorGroupDetail doctorGroupDetail, DoctorMessageRuleRole ruleRole) {
        return groupDetailUrl.concat("?groupId=" + doctorGroupDetail.getGroup().getId() + "&farmId=" + ruleRole.getFarmId());
    }
    /**
     * ??????????????????????????????????????????
     * @param eventTime
     * @return Double
     */
    protected  Double getTimeDiff(DateTime eventTime) {
        try {
            Long timeDiff = (DateTime.now().getMillis() + 28800000) / 86400000 - (eventTime.getMillis() + 28800000) / 86400000;
            return timeDiff.doubleValue();
        } catch (Exception e) {
            log.error("get.timeDiff.failed, eventTime {}", eventTime);
        }
        return null;
    }

//    /**
//     * ???????????????
//     */
//    protected void getMessage(DoctorPigInfoDto pigDto, DoctorMessageRuleRole ruleRole, List<SubUser> subUsers, DoctorMessageInfo messageInfo) {
//        // ????????????
//        String jumpUrl = url.concat("?pigId=" + pigDto.getPigId() + "&farmId=" + ruleRole.getFarmId());
//        Map<String, Object> jsonData = PigDtoFactory.getInstance().createPigMessage(pigDto, timeDiff, ruleTimeDiff, url);
//        try {
//            createMessage(subUsers, ruleRole, MAPPER.writeValueAsString(jsonData), eventType, pigDto.getPigId(), DoctorMessage.BUSINESS_TYPE.PIG.getValue(), ruleValueId, jumpUrl);
//        } catch (JsonProcessingException e) {
//            log.error("message produce error, cause by {}", Throwables.getStackTraceAsString(e));
//        }
//    }

    /**
     * ????????????????????????
     * @param ruleValue
     * @param timeDiff
     * @return
     */
    protected Double getRuleTimeDiff(RuleValue ruleValue, Double timeDiff) {
        if (Objects.equals(ruleValue.getRuleType(), RuleValue.RuleType.VALUE.getValue())) {
            return ruleValue.getValue() - timeDiff;
        } else if (Objects.equals(ruleValue.getRuleType(), RuleValue.RuleType.VALUE_RANGE.getValue())) {
            return ruleValue.getLeftValue() - timeDiff;
        }
        return null;
    }



    /**
     * ????????????,??????????????????
     * @param ruleRole
     * @param subUsers
     */
    protected abstract void message(DoctorMessageRuleRole ruleRole, List<SubUser> subUsers);

    /**
     * ????????????????????????RuleValue
     *
     * @param ruleValue ??????
     * @param value     ?????????
     * @return
     */
    protected boolean checkRuleValue(RuleValue ruleValue, Double value) {
        if (ruleValue == null) {
            return true;
        }
        // 1. ?????????
        if (Objects.equals(RuleValue.RuleType.VALUE.getValue(), ruleValue.getRuleType())) {
            if (value >= ruleValue.getValue()) {
                return true;
            }
        }
        // 2. ???????????????
        if (Objects.equals(RuleValue.RuleType.VALUE_RANGE.getValue(), ruleValue.getRuleType())) {
            if (ruleValue.getLeftValue() != null && value < ruleValue.getLeftValue()) {
                return false;
            }
            if (ruleValue.getRightValue() != null && value > ruleValue.getRightValue()) {
                return false;
            }
            return true;
        }
        return false;
    }

    /**
     * ????????????????????????RuleValue
     *
     * @param ruleValue ??????
     * @param date      ????????????
     * @return
     */
    protected boolean checkRuleValue(RuleValue ruleValue, Date date) {
        // 1. ????????????
        if (Objects.equals(RuleValue.RuleType.DATE.getValue(), ruleValue.getRuleType())) {
            return new DateTime(ruleValue.getDate()).minus(date.getTime()).getMillis() == 0;
        }
        // 2. ??????????????????
        if (Objects.equals(RuleValue.RuleType.DATE_RANGE.getValue(), ruleValue.getRuleType())) {
            if (ruleValue.getLeftDate() != null && new DateTime(date).isBefore(new DateTime(ruleValue.getLeftDate()))) {
                return false;
            }
            if (ruleValue.getRightDate() != null && new DateTime(date).isAfter(new DateTime(ruleValue.getRightDate()))) {
                return false;
            }
            return true;
        }
        return false;
    }

    /**
     * ?????????????????????
     * @param event1
     * @param event2
     * @return
     */
    private int pigEventCompare(DoctorPigEvent event1, DoctorPigEvent event2) {
        if (Objects.equals(event1.getEventAt(), event2.getEventAt())) {
            return event2.getId().compareTo(event1.getId());
        } else {
            return event2.getEventAt().compareTo(event1.getEventAt());
        }
    }

    /**
     * ????????????????????????
     * @param event1
     * @param event2
     * @return
     */
    private int groupEventCompare(DoctorGroupEvent event1, DoctorGroupEvent event2) {
        if (Objects.equals(event1.getEventAt(), event2.getEventAt())) {
            return event1.getId().compareTo(event2.getId());
        } else {
            return event1.getEventAt().compareTo(event2.getEventAt());
        }
    }
}
