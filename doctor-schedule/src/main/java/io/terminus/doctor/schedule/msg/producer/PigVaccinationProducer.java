package io.terminus.doctor.schedule.msg.producer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import io.terminus.doctor.common.enums.PigType;
import io.terminus.doctor.common.utils.DateUtil;
import io.terminus.doctor.common.utils.RespHelper;
import io.terminus.doctor.event.dto.DoctorGroupDetail;
import io.terminus.doctor.event.dto.DoctorGroupSearchDto;
import io.terminus.doctor.event.dto.DoctorPigInfoDto;
import io.terminus.doctor.event.enums.GroupEventType;
import io.terminus.doctor.event.enums.PigEvent;
import io.terminus.doctor.event.enums.PigStatus;
import io.terminus.doctor.event.enums.VaccinationDateType;
import io.terminus.doctor.event.model.DoctorBarn;
import io.terminus.doctor.event.model.DoctorGroupEvent;
import io.terminus.doctor.event.model.DoctorGroupTrack;
import io.terminus.doctor.event.model.DoctorPig;
import io.terminus.doctor.event.model.DoctorPigEvent;
import io.terminus.doctor.event.model.DoctorVaccinationPigWarn;
import io.terminus.doctor.event.service.DoctorBarnReadService;
import io.terminus.doctor.event.service.DoctorGroupReadService;
import io.terminus.doctor.event.service.DoctorVaccinationPigWarnReadService;
import io.terminus.doctor.msg.dto.Rule;
import io.terminus.doctor.msg.dto.RuleValue;
import io.terminus.doctor.msg.dto.SubUser;
import io.terminus.doctor.msg.enums.Category;
import io.terminus.doctor.msg.model.DoctorMessage;
import io.terminus.doctor.msg.model.DoctorMessageRuleRole;
import io.terminus.doctor.schedule.msg.dto.DoctorMessageInfo;
import io.terminus.doctor.schedule.msg.producer.factory.GroupDetailFactory;
import io.terminus.doctor.schedule.msg.producer.factory.PigDtoFactory;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Desc: 猪只免疫规则
 * Mail: chk@terminus.io
 * Created by icemimosa
 * Date: 16/6/6
 */
@Component
@Slf4j
public class PigVaccinationProducer extends AbstractJobProducer {

    @Autowired
    private  DoctorGroupReadService doctorGroupReadService;
    @Autowired
    private  DoctorVaccinationPigWarnReadService doctorVaccinationPigWarnReadService;
    @Autowired
    private DoctorBarnReadService doctorBarnReadService;

    public PigVaccinationProducer() {
        super(Category.PIG_VACCINATION);
    }

    @Override
    protected void message(DoctorMessageRuleRole ruleRole, List<SubUser> subUsers) {
        try {
        Rule rule = ruleRole.getRule();
        // ruleValue map
        Map<Integer, RuleValue> ruleValueMap = Maps.newHashMap();
        for (int i = 0; rule.getValues() != null && i < rule.getValues().size(); i++) {
            RuleValue ruleValue = rule.getValues().get(i);
            ruleValueMap.put(ruleValue.getId(), ruleValue);
        }
            // 获取所有的预设免疫规则
            List<DoctorVaccinationPigWarn> vaccinationWarns = RespHelper.orServEx(
                    doctorVaccinationPigWarnReadService.findVaccinationPigWarnsByFarmId(ruleRole.getFarmId()));
            if (vaccinationWarns == null || vaccinationWarns.isEmpty()) {
                return;
            }

            List<DoctorBarn> barns = RespHelper.orServEx(doctorBarnReadService.findBarnsByFarmId(ruleRole.getFarmId()));
            Map<Long, Integer> barnTypeMap = barns.stream().collect(Collectors.toMap(k -> k.getId(), v -> v.getPigType()));
            for (DoctorVaccinationPigWarn warn : vaccinationWarns) {
                // 判断规则是否在有效期内
                if (warn.getStartDate() != null && DateTime.now().isBefore(new DateTime(warn.getStartDate()))) {
                    continue;
                }
                if (warn.getEndDate() != null && DateTime.now().isAfter(new DateTime(warn.getEndDate()))) {
                    continue;
                }
                PigType pigType = PigType.from(warn.getPigType());
                if (pigType == null) {
                    continue;
                }
                // 一次处理每种类型
                switch (pigType) {
                    // 配种母猪
                    case MATE_SOW:
                        checkMateSow(warn, ruleRole, rule, subUsers, barnTypeMap);
                        break;
                    // 妊娠母猪
                    case PREG_SOW:
                        checkPregSow(warn, ruleRole, rule, subUsers, barnTypeMap);
                        break;
                    // 分娩母猪
                    case DELIVER_SOW:
                        checkDeliverSow(warn, ruleRole, rule, subUsers, barnTypeMap);
                        checkPigGroup(warn, ruleRole, rule, subUsers, PigType.DELIVER_SOW.getValue());
                        break;
                    // 后备猪
                    case RESERVE:
                        checkReservePig(warn, ruleRole, rule, subUsers, null, barnTypeMap);
                        break;
                    // 种公猪
                    case BOAR:
                        checkReservePig(warn, ruleRole, rule, subUsers, DoctorPig.PigSex.BOAR.getKey(), barnTypeMap);
                        break;
                    // 保育猪
                    case NURSERY_PIGLET:
                        checkPigGroup(warn, ruleRole, rule, subUsers, PigType.NURSERY_PIGLET.getValue());
                        break;
                    // 育肥猪
                    case FATTEN_PIG:
                        checkPigGroup(warn, ruleRole, rule, subUsers, PigType.FATTEN_PIG.getValue());
                        break;
                    default:
                        break;
                }
            }
        } catch (Exception e) {
            log.error("[PigVaccinationProducer]-message.error");
        }
    }

    /**
     * 校验配种母猪是否需要免疫
     * 可能存在的日期类型:
     * 固定日龄, 固定日期, 固定体重, 转舍, 配种
     */
    private void checkMateSow(DoctorVaccinationPigWarn warn, DoctorMessageRuleRole ruleRole, Rule rule, List<SubUser> subUsers, Map<Long, Integer> barnTypeMap) {
        Map<Integer, List<DoctorPigInfoDto>> sowPigInfoDtos = getPigInfoDtos(ruleRole, DoctorPig.PigSex.SOW.getKey(), Lists.newArrayList(PigType.MATE_SOW.getValue()), barnTypeMap);
        Iterator<List<DoctorPigInfoDto>> iterator = sowPigInfoDtos.values().iterator();
        while (iterator.hasNext()) {
            iterator.next().forEach(sowPig -> {
                List<SubUser> sUsers = filterSubUserBarnId(subUsers, sowPig.getBarnId());
                DateTime vaccDate = getVaccinationDate(sowPig);
                // 1. 固定日龄
                if (checkFixedDayAge(warn, sowPig.getDateAge(), vaccDate)) {
                    getMessage(sowPig, ruleRole, sUsers, null, rule.getUrl(), warn, vaccDate);
                }
                // 2. 固定日期
                if (checkFixedDate(warn, vaccDate)) {
                    getMessage(sowPig, ruleRole, sUsers, null, rule.getUrl(), warn, vaccDate);
                }
                // 3. 固定体重
                if (checkFixedWeight(warn, vaccDate, sowPig.getWeight(), getCheckWeightDate(sowPig))) {
                    getMessage(sowPig, ruleRole, sUsers, null, rule.getUrl(), warn, vaccDate);
                }
                // 4. 转舍
                if (checkChangeLocation(warn, vaccDate, getChangeLocationDate(sowPig))) {
                    getMessage(sowPig, ruleRole, sUsers, null, rule.getUrl(), warn, vaccDate);
                }
                // 5. 配种, 母猪处于待配种状态
                if (Objects.equals(VaccinationDateType.BREEDING.getValue(), warn.getVaccinationDateType()) &&
                        Objects.equals(sowPig.getStatus(), PigStatus.Mate.getKey())) {
                    // (当前日期 - 配置的天数) 大于 处于待配种状态的日期
                    if (DateTime.now().minusDays(warn.getInputValue()).isAfter(new DateTime(sowPig.getUpdatedAt()))) {
                        if (vaccDate == null || vaccDate.isBefore(new DateTime(sowPig.getUpdatedAt()))) {
                            getMessage(sowPig, ruleRole, sUsers, null, rule.getUrl(), warn, vaccDate);
                        }
                    }
                }
            });
        }
    }

    /**
     * 校验妊娠母猪是否需要免疫
     * 可能存在的日期类型:
     * 固定日龄, 固定日期, 固定体重, 转舍, 妊娠检查
     */
    private void checkPregSow(DoctorVaccinationPigWarn warn, DoctorMessageRuleRole ruleRole, Rule rule, List<SubUser> subUsers, Map<Long, Integer> barnTypeMap) {
        Map<Integer, List<DoctorPigInfoDto>> sowPigInfoDtos = getPigInfoDtos(ruleRole, DoctorPig.PigSex.SOW.getKey(), Lists.newArrayList(PigType.PREG_SOW.getValue()), barnTypeMap);
        Iterator<List<DoctorPigInfoDto>> iterator = sowPigInfoDtos.values().iterator();
        while (iterator.hasNext()) {
            iterator.next().forEach(sowPig -> {
                List<SubUser> sUsers = filterSubUserBarnId(subUsers, sowPig.getBarnId());
                DateTime vaccDate = getVaccinationDate(sowPig);
                // 1. 固定日龄
                if (checkFixedDayAge(warn, sowPig.getDateAge(), vaccDate)) {
                    getMessage(sowPig, ruleRole, sUsers, null, rule.getUrl(), warn, vaccDate);
                }
                // 2. 固定日期
                if (checkFixedDate(warn, vaccDate)) {
                    getMessage(sowPig, ruleRole, sUsers, null, rule.getUrl(), warn, vaccDate);
                }
                // 3. 固定体重
                if (checkFixedWeight(warn, vaccDate, sowPig.getWeight(), getCheckWeightDate(sowPig))) {
                    getMessage(sowPig, ruleRole, sUsers, null, rule.getUrl(), warn, vaccDate);
                }
                // 4. 转舍
                if (checkChangeLocation(warn, vaccDate, getChangeLocationDate(sowPig))) {
                    getMessage(sowPig, ruleRole, sUsers, null, rule.getUrl(), warn, vaccDate);
                }
                // 5. 妊娠检测, 母猪处于阳性状态
                if (Objects.equals(VaccinationDateType.PREG_CHECK.getValue(), warn.getVaccinationDateType()) &&
                        Objects.equals(sowPig.getStatus(), PigStatus.Pregnancy.getKey())) {
                    // (当前日期 - 配置的天数) 大于 处于阳性状态的日期
                    if (DateTime.now().minusDays(warn.getInputValue()).isAfter(new DateTime(sowPig.getUpdatedAt()))) {
                        if (vaccDate == null || vaccDate.isBefore(new DateTime(sowPig.getUpdatedAt()))) {
                            getMessage(sowPig, ruleRole, sUsers, null, rule.getUrl(), warn, vaccDate);
                        }
                    }
                }

            });
        }
    }

    /**
     * 校验分娩母猪是否需要免疫
     * 可能存在的日期类型:
     * 固定日龄, 固定日期, 固定体重, 转舍, 分娩
     */
    private void checkDeliverSow(DoctorVaccinationPigWarn warn, DoctorMessageRuleRole ruleRole, Rule rule, List<SubUser> subUsers, Map<Long, Integer> barnTypeMap) {
        Map<Integer, List<DoctorPigInfoDto>> sowPigInfoDtos = getPigInfoDtos(ruleRole, DoctorPig.PigSex.SOW.getKey(), Lists.newArrayList(PigType.FATTEN_PIG.getValue()), barnTypeMap);
        Iterator<List<DoctorPigInfoDto>> iterator = sowPigInfoDtos.values().iterator();
        while (iterator.hasNext()) {
            iterator.next().forEach(sowPig -> {
                List<SubUser> sUsers = filterSubUserBarnId(subUsers, sowPig.getBarnId());
                DateTime vaccDate = getVaccinationDate(sowPig);
                // 1. 固定日龄
                if (checkFixedDayAge(warn, sowPig.getDateAge(), vaccDate)) {
                    getMessage(sowPig, ruleRole, sUsers, null, rule.getUrl(), warn, vaccDate);
                }
                // 2. 固定日期
                if (checkFixedDate(warn, vaccDate)) {
                    getMessage(sowPig, ruleRole, sUsers, null, rule.getUrl(), warn, vaccDate);
                }
                // 3. 固定体重
                if (checkFixedWeight(warn, vaccDate, sowPig.getWeight(), getCheckWeightDate(sowPig))) {
                    getMessage(sowPig, ruleRole, sUsers, null, rule.getUrl(), warn, vaccDate);
                }
                // 4. 转舍
                if (checkChangeLocation(warn, vaccDate, getChangeLocationDate(sowPig))) {
                    getMessage(sowPig, ruleRole, sUsers, null, rule.getUrl(), warn, vaccDate);
                }
                // 5. 分娩, 母猪处于哺乳状态
                if (Objects.equals(VaccinationDateType.DELIVER.getValue(), warn.getVaccinationDateType()) &&
                        Objects.equals(sowPig.getStatus(), PigStatus.FEED.getKey())) {
                    // (当前日期 - 配置的天数) 大于 处于哺乳状态的日期
                    if (DateTime.now().minusDays(warn.getInputValue()).isAfter(new DateTime(sowPig.getUpdatedAt()))) {
                        if (vaccDate == null || vaccDate.isBefore(new DateTime(sowPig.getUpdatedAt()))) {
                            getMessage(sowPig, ruleRole, sUsers, null, rule.getUrl(), warn, vaccDate);
                        }
                    }
                }
            });
        }
    }

    /**
     * 校验(后备母猪, 后备公猪, 种公猪)是否需要免疫
     * 可能存在的日期类型:
     * 固定日龄, 固定日期, 固定体重, 转舍
     */
    private void checkReservePig(DoctorVaccinationPigWarn warn, DoctorMessageRuleRole ruleRole, Rule rule, List<SubUser> subUsers, Integer pigType, Map<Long, Integer> barnTypeMap) {
        // 获取猪
        Map<Integer, List<DoctorPigInfoDto>> sowPigInfoDtos = getPigInfoDtos(ruleRole, pigType, Lists.newArrayList(PigType.RESERVE.getValue(), PigType.BOAR.getValue()), barnTypeMap);
        Iterator<List<DoctorPigInfoDto>> iterator = sowPigInfoDtos.values().iterator();
        while (iterator.hasNext()) {
            iterator.next().forEach(sowPig -> {
                List<SubUser> sUsers = filterSubUserBarnId(subUsers, sowPig.getBarnId());
                DateTime vaccDate = getVaccinationDate(sowPig);
                // 1. 固定日龄
                if (checkFixedDayAge(warn, sowPig.getDateAge(), vaccDate)) {
                    getMessage(sowPig, ruleRole, sUsers, null, rule.getUrl(), warn, vaccDate);
                }
                // 2. 固定日期
                if (checkFixedDate(warn, vaccDate)) {
                    getMessage(sowPig, ruleRole, sUsers, null, rule.getUrl(), warn, vaccDate);
                }
                // 3. 固定体重
                if (checkFixedWeight(warn, vaccDate, sowPig.getWeight(), getCheckWeightDate(sowPig))) {
                    getMessage(sowPig, ruleRole, sUsers, null, rule.getUrl(), warn, vaccDate);
                }
                // 4. 转舍
                if (checkChangeLocation(warn, vaccDate, getChangeLocationDate(sowPig))) {
                    getMessage(sowPig, ruleRole, sUsers, null, rule.getUrl(), warn, vaccDate);
                }
            });
        }
    }

    /**
     * 校验(保育猪, 育肥猪)是否需要免疫
     * 可能存在的日期类型:
     * 固定日龄, 固定日期, 固定体重, 转群
     */
    private void checkPigGroup(DoctorVaccinationPigWarn warn, DoctorMessageRuleRole ruleRole, Rule rule, List<SubUser> subUsers, Integer pigType) {
        List<DoctorGroupDetail> groupInfos = getGroupInfos(ruleRole, pigType);
        for (int i = 0; groupInfos != null && i < groupInfos.size(); i++) {
            DoctorGroupDetail groupInfo = groupInfos.get(i);
            DoctorGroupTrack groupTrack = groupInfo.getGroupTrack();
            List<SubUser> sUsers = filterSubUserBarnId(subUsers, groupInfo.getGroup().getCurrentBarnId());
            if (groupTrack != null) {
                DateTime vaccDate = getGroupVaccinationDate(groupTrack);
                // 1. 固定日龄
                if (checkFixedDayAge(warn, groupTrack.getAvgDayAge(), vaccDate)) {
                    getGroupMessage(groupInfo, ruleRole, sUsers, rule.getUrl(), warn, vaccDate);
                }
                // 2. 固定日期
                if (checkFixedDate(warn, vaccDate)) {
                    getGroupMessage(groupInfo, ruleRole, sUsers, rule.getUrl(), warn, vaccDate);
                }
                // 4. 转群
                if (checkChangeGroup(warn, vaccDate, getChangeGroupDate(groupTrack))) {
                    getGroupMessage(groupInfo, ruleRole, sUsers, rule.getUrl(), warn, vaccDate);
                }
            }
        }
    }

    /**
     * 校验是否达到固定日龄的规则
     *
     * @param warn     防疫程序
     * @param dayAge   日龄
     * @param vaccDate 免疫时间
     * @return
     */
    private boolean checkFixedDayAge(DoctorVaccinationPigWarn warn, Integer dayAge, DateTime vaccDate) {
        if (Objects.equals(VaccinationDateType.FIXED_DAY_AGE.getValue(), warn.getVaccinationDateType())) {
            if (dayAge >= warn.getInputValue()) {
                // 如果免疫时间 小于 (达到规则日龄的时间), 则生成消息
                DateTime ruleDayAgeTime = DateTime.now().minusDays(dayAge - warn.getInputValue());
                if (vaccDate == null || vaccDate.isBefore(ruleDayAgeTime)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 校验是否达到固定日期
     *
     * @param warn     防疫程序
     * @param vaccDate 免疫时间
     * @return
     */
    private boolean checkFixedDate(DoctorVaccinationPigWarn warn, DateTime vaccDate) {
        if (Objects.equals(VaccinationDateType.FIXED_DATE.getValue(), warn.getVaccinationDateType())) {
            if (DateTime.now().isAfter(new DateTime(warn.getInputDate()))) {
                // 如果免疫时间 小于 (规则固定日期), 则生成消息
                if (vaccDate == null || vaccDate.isBefore(new DateTime(warn.getInputDate()))) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 检验是否到达固定体重
     *
     * @param warn            免疫程序
     * @param vaccDate        免疫时间
     * @param weight          当前重量
     * @param checkWeightTime 检测重量的时间
     * @return
     */
    private boolean checkFixedWeight(DoctorVaccinationPigWarn warn, DateTime vaccDate, Double weight, DateTime checkWeightTime) {
        if (Objects.equals(VaccinationDateType.FIXED_WEIGHT.getValue(), warn.getVaccinationDateType())) {
            if (weight >= warn.getInputValue()) {
                // 如果免疫时间 小于 (到达规则体重的日期), 则生成消息
                // DateTime weightTime = checkWeightTime;
                if (vaccDate == null /*|| vaccDate.isBefore(weightTime)*/) { //  如果没有免疫, 就免疫
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 校验转舍后是否达到规则要求
     *
     * @param warn       免疫程序
     * @param vaccDate   免疫时间
     * @param chgLocTime 转舍时间
     * @return
     */
    private boolean checkChangeLocation(DoctorVaccinationPigWarn warn, DateTime vaccDate, DateTime chgLocTime) {
        if (Objects.equals(VaccinationDateType.CHANGE_LOC.getValue(), warn.getVaccinationDateType())) {
            // (当前日期 - 配置的天数) 大于 转舍日期
            if (DateTime.now().minusDays(warn.getInputValue()).isAfter(chgLocTime)) {
                // 如果免疫过了, 就不提醒
                if (vaccDate == null) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 校验转群后是否达到规则要求
     *
     * @param warn         免疫程序
     * @param vaccDate     免疫时间
     * @param chgGroupTime 转舍时间
     * @return
     */
    private boolean checkChangeGroup(DoctorVaccinationPigWarn warn, DateTime vaccDate, DateTime chgGroupTime) {
        if (Objects.equals(VaccinationDateType.CHANGE_GROUP.getValue(), warn.getVaccinationDateType())) {
            // (当前日期 - 配置的天数) 大于 转群日期
            if (DateTime.now().minusDays(warn.getInputValue()).isAfter(chgGroupTime)) {
                // 如果免疫过了, 就不提醒
                if (vaccDate == null) {
                    return true;
                }
            }
        }
        return false;
    }


    /**
     * 获取猪信息
     *
     * @param ruleRole
     * @param pigType  猪性别
     * @param types    猪类型
     * @see DoctorPig.PigSex
     */
    private Map<Integer, List<DoctorPigInfoDto>> getPigInfoDtos(DoctorMessageRuleRole ruleRole, Integer pigType, List<Integer> types, Map<Long, Integer> barnTypeMap) {
        Map<Integer, List<DoctorPigInfoDto>> sowPigInfoDtos = Maps.newHashMap();
        Long total = RespHelper.orServEx(doctorPigReadService.getPigCount(ruleRole.getFarmId(), DoctorPig.PigSex.from(pigType)));
        // 计算size, 和page
        Long page = getPageSize(total, 100L);
        DoctorPig pig = DoctorPig.builder()
                .farmId(ruleRole.getFarmId())
                .pigType(pigType)
                .build();
        for (int i = 1; i <= page; i++) {
            List<DoctorPigInfoDto> pigs = RespHelper.orServEx(doctorPigReadService.pagingDoctorInfoDtoByPig(pig, i, 100)).getData();
            pigs = pigs.stream().filter(pigDto ->
                    filterPigCondition(pigDto, types, barnTypeMap)
            ).collect(Collectors.toList());

            sowPigInfoDtos.put(i, pigs);
        }
        return sowPigInfoDtos;
    }

    private Boolean filterPigCondition(DoctorPigInfoDto pigDto, List<Integer> types, Map<Long, Integer> barnTypeMap){
        Integer barnType = barnTypeMap.get(pigDto.getBarnId());
        return !Objects.equals(PigStatus.Removal.getKey(), pigDto.getStatus()) && types.contains(barnType);
    }

    /**
     * 获取猪群信息
     *
     * @param ruleRole
     * @param pigType  猪种类
     * @see PigType
     */
    private List<DoctorGroupDetail> getGroupInfos(DoctorMessageRuleRole ruleRole, Integer pigType) {
        DoctorGroupSearchDto searchDto = new DoctorGroupSearchDto();
        searchDto.setFarmId(ruleRole.getFarmId());
        searchDto.setPigType(pigType);
        searchDto.setStatus(DoctorGroupSearchDto.Status.CREATED.getValue());
        return RespHelper.orServEx(doctorGroupReadService.findGroupDetail(searchDto));
    }

    /**
     * 获取猪的最新的免疫时间
     */
    private DateTime getVaccinationDate(DoctorPigInfoDto pigDto) {
        try {
            DoctorPigEvent event = getPigEventByEventType(pigDto.getPigId(), PigEvent.VACCINATION.getKey());
            if (event != null){
                return new DateTime(event.getEventAt());
            }
        } catch (Exception e) {
            log.error("[PigVaccinationProducer] get vaccination date failed");
        }
        return null;
    }

    /**
     * 获取猪群的最新免疫时间
     */
    private DateTime getGroupVaccinationDate(DoctorGroupTrack track) {
        try {
            List<DoctorGroupEvent> events = RespHelper.orServEx(doctorGroupReadService.queryAllGroupEventByGroupId(track.getGroupId()));
            DoctorGroupEvent event = getLastGroupEventByEventType(events, GroupEventType.ANTIEPIDEMIC.getValue());
            if (event != null){
                return new DateTime(event.getEventAt());
            }
        } catch (Exception e) {
            log.error("[PigVaccinationProducer] get GroupVaccinationDate failed");
        }
        return null;
    }

    /**
     * 获取转群时间
     */
    private DateTime getChangeGroupDate(DoctorGroupTrack track) {
        try {
            List<DoctorGroupEvent> events = RespHelper.orServEx(doctorGroupReadService.queryAllGroupEventByGroupId(track.getGroupId()));

            DoctorGroupEvent event = getLastGroupEventByEventType(events, GroupEventType.TRANS_GROUP.getValue());
            if (event != null){
                return new DateTime(event.getEventAt());
            }
        } catch (Exception e) {
            log.error("[PigVaccinationProducer] get trantsGroup date failed");
        }
        return null;
    }

    /**
     * 获取最新的体况检查时间
     */
    private DateTime getCheckWeightDate(DoctorPigInfoDto pigDto) {
        try {
            DoctorPigEvent event = getPigEventByEventType(pigDto.getPigId(), PigEvent.CONDITION.getKey());
            if (event != null){
                return new DateTime(event.getEventAt());
            }
        } catch (Exception e) {
            log.error("[PigVaccinationProducer] get check weight date failed");
        }
        return null;
    }

    /**
     * 获取转舍时间
     */
    private DateTime getChangeLocationDate(DoctorPigInfoDto pigDto) {
        try {
            DoctorPigEvent event = getPigEventByEventType(pigDto.getPigId(), PigEvent.CHG_LOCATION.getKey());
            if (event != null){
                return new DateTime(event.getEventAt());
            }
        } catch (Exception e) {
            log.error("[PigVaccinationProducer] get change location date failed");
        }
        return null;
    }


    /**
     * 创建消息
     */
    private void getMessage(DoctorPigInfoDto pigDto, DoctorMessageRuleRole ruleRole, List<SubUser> subUsers, Double timeDiff, String url, DoctorVaccinationPigWarn warn, DateTime vaccDate) {
        // 创建消息
        String jumpUrl = getPigJumpUrl(pigDto, ruleRole);
        Map<String, Object> jsonData = PigDtoFactory.getInstance().createPigMessage(pigDto, timeDiff, null, url);
        jsonData.put("pigType", warn.getPigType());
        jsonData.put("materialId", warn.getMaterialId());
        jsonData.put("materialName", warn.getMaterialName());
        jsonData.put("inputValue", warn.getInputValue());
        jsonData.put("inputDate", warn.getInputDate());
        jsonData.put("dose", warn.getDose());
        jsonData.put("vaccinationDateType", warn.getVaccinationDateType());
        jsonData.put("vaccDate", DateTimeFormat.forPattern("yyyy-MM-dd").print(vaccDate));

        try {
            DoctorMessageInfo messageInfo = DoctorMessageInfo.builder()
                    .data(MAPPER.writeValueAsString(jsonData))
                    .barnId(pigDto.getBarnId())
                    .barnName(pigDto.getBarnName())
                    .eventType(PigEvent.VACCINATION.getKey())
                    .url(jumpUrl)
                    .businessId(pigDto.getPigId())
                    .businessType(DoctorMessage.BUSINESS_TYPE.PIG.getValue())
                    .status(pigDto.getStatus())
                    .statusName(pigDto.getStatusName())
                    .build();

            createMessage(subUsers, ruleRole, messageInfo);
        } catch (JsonProcessingException e) {
            log.error("message produce error, cause by {}", Throwables.getStackTraceAsString(e));
        }
    }

    /**
     * 创建消息
     */
    private void getGroupMessage(DoctorGroupDetail detail, DoctorMessageRuleRole ruleRole, List<SubUser> subUsers, String url, DoctorVaccinationPigWarn warn, DateTime vaccDate) {
        // 创建消息
        String jumpUrl = groupDetailUrl.concat("?groupId=" + detail.getGroup().getId() + "&farmId=" + ruleRole.getFarmId());
        Map<String, Object> jsonData = GroupDetailFactory.getInstance().createGroupMessage(detail, url);
        jsonData.put("pigType", warn.getPigType());
        jsonData.put("materialId", warn.getMaterialId());
        jsonData.put("materialName", warn.getMaterialName());
        jsonData.put("inputValue", warn.getInputValue());
        jsonData.put("inputDate", DateUtil.toDateString(warn.getInputDate()));
        jsonData.put("currDate", DateUtil.toDateString(new Date()));
        jsonData.put("dose", warn.getDose());
        jsonData.put("vaccinationDateType", warn.getVaccinationDateType());
        jsonData.put("vaccDate", DateTimeFormat.forPattern("yyyy-MM-dd").print(vaccDate));
        try {
            DoctorMessageInfo messageInfo = DoctorMessageInfo.builder()
                    .data(MAPPER.writeValueAsString(jsonData))
                    .barnId(detail.getGroup().getCurrentBarnId())
                    .barnName(detail.getGroup().getCurrentBarnName())
                    .eventType(GroupEventType.ANTIEPIDEMIC.getValue())
                    .url(jumpUrl)
                    .businessId(detail.getGroup().getId())
                    .businessType(DoctorMessage.BUSINESS_TYPE.GROUP.getValue())
                    .build();

            createMessage(subUsers, ruleRole, messageInfo);
        } catch (JsonProcessingException e) {
            log.error("message produce error, cause by {}", Throwables.getStackTraceAsString(e));
        }
    }
}
