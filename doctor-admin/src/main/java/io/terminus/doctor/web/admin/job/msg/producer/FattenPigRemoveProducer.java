package io.terminus.doctor.web.admin.job.msg.producer;

import com.google.api.client.util.Maps;
import io.terminus.doctor.common.enums.PigType;
import io.terminus.doctor.common.utils.RespHelper;
import io.terminus.doctor.event.dto.DoctorGroupDetail;
import io.terminus.doctor.event.dto.DoctorGroupSearchDto;
import io.terminus.doctor.event.dto.msg.Rule;
import io.terminus.doctor.event.dto.msg.RuleValue;
import io.terminus.doctor.event.dto.msg.SubUser;
import io.terminus.doctor.event.enums.Category;
import io.terminus.doctor.event.model.DoctorGroup;
import io.terminus.doctor.event.model.DoctorMessage;
import io.terminus.doctor.event.model.DoctorMessageRuleRole;
import io.terminus.doctor.event.service.DoctorGroupReadService;
import io.terminus.doctor.web.admin.job.msg.dto.DoctorMessageInfo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Created by xiao on 16/8/31.
 */
@Slf4j
@Component
public class FattenPigRemoveProducer extends AbstractJobProducer {

    @Autowired
    private DoctorGroupReadService doctorGroupReadService;

    public FattenPigRemoveProducer() {
        super(Category.FATTEN_PIG_REMOVE);
    }

    @Override
    protected void message(DoctorMessageRuleRole ruleRole, List<SubUser> subUsers) {

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
            doctorGroupSearchDto.setStatus(DoctorGroup.Status.CREATED.getValue());
            List<DoctorGroupDetail> groupDetails = RespHelper.or500(doctorGroupReadService.findGroupDetail(doctorGroupSearchDto));
            groupDetails.forEach(doctorGroupDetail -> {
                try {
                    //?????????????????????????????????????????????user
                    RuleValue ruleValue = ruleValueMap.get(1);
                    List<SubUser> sUsers = filterSubUserBarnId(subUsers, doctorGroupDetail.getGroup().getCurrentBarnId());
                    if (checkRuleValue(ruleValue, (double) doctorGroupDetail.getGroupTrack().getAvgDayAge())) {
                        DoctorMessageInfo messageInfo = DoctorMessageInfo.builder()
                                .code(doctorGroupDetail.getGroup().getGroupCode())
                                .url(getGroupJumpUrl(doctorGroupDetail, ruleRole))
                                .reason(ruleValue.getDescribe() + ruleValue.getValue().toString())
                                .barnId(doctorGroupDetail.getGroup().getCurrentBarnId())
                                .barnName(doctorGroupDetail.getGroup().getCurrentBarnName())
                                .businessType(DoctorMessage.BUSINESS_TYPE.GROUP.getValue())
                                .businessId(doctorGroupDetail.getGroup().getId())
                                .ruleValueId(ruleValue.getId())
                                .quantity(doctorGroupDetail.getGroupTrack().getQuantity())
                                .avgDayAge(doctorGroupDetail.getGroupTrack().getAvgDayAge())
                                .build();
                        createMessage(sUsers, ruleRole, messageInfo);
                    }
                } catch (Exception e) {
                    log.error("[FattenPigRemoveProducer]->message.failed");
                }

            });
        }
    }

//    /**
//     * ????????????
//     */
//    private void getMessage(DoctorGroupDetail doctorGroupDetail, DoctorMessageRuleRole ruleRole, List<SubUser> subUsers, String url, Integer eventType, Integer ruleValueId) {
//        // ????????????
//        String jumpUrl = groupDetailUrl.concat("?groupId=" + doctorGroupDetail.getGroup().getId() + "&farmId=" + ruleRole.getFarmId());
//        Map<String, Object> jsonData = GroupDetailFactory.getInstance().createGroupMessage(doctorGroupDetail, url);
//            try {
//                createMessage(subUsers, ruleRole, MAPPER.writeValueAsString(jsonData), eventType, doctorGroupDetail.getGroup().getId(), DoctorMessage.BUSINESS_TYPE.GROUP.getValue(), ruleValueId, jumpUrl);
//            } catch (JsonProcessingException e) {
//                log.error("message produce error, cause by {}", Throwables.getStackTraceAsString(e));
//            }
//    }
}
