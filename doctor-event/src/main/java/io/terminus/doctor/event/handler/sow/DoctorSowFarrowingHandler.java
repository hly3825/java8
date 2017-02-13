package io.terminus.doctor.event.handler.sow;

import com.google.common.base.MoreObjects;
import io.terminus.doctor.common.enums.PigType;
import io.terminus.doctor.common.utils.CountUtil;
import io.terminus.doctor.common.utils.DateUtil;
import io.terminus.doctor.event.dto.DoctorBasicInputInfoDto;
import io.terminus.doctor.event.dto.event.BasePigEventInputDto;
import io.terminus.doctor.event.dto.event.DoctorEventInfo;
import io.terminus.doctor.event.dto.event.group.DoctorMoveInGroupEvent;
import io.terminus.doctor.event.dto.event.group.input.DoctorSowMoveInGroupInput;
import io.terminus.doctor.event.dto.event.sow.DoctorFarrowingDto;
import io.terminus.doctor.event.dto.event.sow.DoctorWeanDto;
import io.terminus.doctor.event.enums.FarrowingType;
import io.terminus.doctor.event.enums.PigEvent;
import io.terminus.doctor.event.enums.PigSource;
import io.terminus.doctor.event.enums.PigStatus;
import io.terminus.doctor.event.handler.DoctorAbstractEventHandler;
import io.terminus.doctor.event.handler.group.DoctorCommonGroupEventHandler;
import io.terminus.doctor.event.model.DoctorGroupTrack;
import io.terminus.doctor.event.model.DoctorPigEvent;
import io.terminus.doctor.event.model.DoctorPigTrack;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.joda.time.Days;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.google.common.base.Preconditions.checkState;
import static io.terminus.common.utils.Arguments.notNull;
import static io.terminus.doctor.common.utils.DateUtil.stringToDate;

/**
 * Created by yaoqijun.
 * Date:2016-05-27
 * Email:yaoqj@terminus.io
 * Descirbe: 母猪分娩事件
 */
@Component
@Slf4j
public class DoctorSowFarrowingHandler extends DoctorAbstractEventHandler {

    @Autowired
    private DoctorCommonGroupEventHandler doctorCommonGroupEventHandler;

    @Autowired
    private DoctorSowWeanHandler doctorSowWeanHandler;

    @Override
    protected DoctorPigEvent buildPigEvent(DoctorBasicInputInfoDto basic, BasePigEventInputDto inputDto) {
         DoctorPigEvent doctorPigEvent = super.buildPigEvent(basic, inputDto);
        DoctorPigTrack doctorPigTrack = doctorPigTrackDao.findByPigId(inputDto.getPigId());
        DoctorFarrowingDto farrowingDto = (DoctorFarrowingDto) inputDto;
        Map<String, Object> extra = doctorPigEvent.getExtraMap();
        // 助产 信息
        extra.put("isHelp", 1);

        // 校验 是否早产信息
        //分娩时间
        DateTime farrowingDate = new DateTime(farrowingDto.eventAt());
        doctorPigEvent.setFarrowingDate(farrowingDate.toDate());
        //查找最近一次初配种事件
        DoctorPigEvent firstMate = doctorPigEventDao.queryLastFirstMate(doctorPigTrack.getPigId(), doctorPigTrack.getCurrentParity());
        DateTime pregJudgeDate = new DateTime(stringToDate(firstMate.getExtraMap().get("judgePregDate").toString()));
        DateTime mattingDate = new DateTime(firstMate.getEventAt());

        //计算孕期
        doctorPigEvent.setPregDays(Math.abs(Days.daysBetween(farrowingDate, mattingDate).getDays()));


        //分娩窝重
        doctorPigEvent.setFarrowWeight(farrowingDto.getBirthNestAvg());
        //分娩仔猪只数信息
        doctorPigEvent.setLiveCount(CountUtil.getIntegerDefault0(farrowingDto.getFarrowingLiveCount()));
        doctorPigEvent.setHealthCount(CountUtil.getIntegerDefault0(farrowingDto.getHealthCount()));
        doctorPigEvent.setWeakCount(CountUtil.getIntegerDefault0(farrowingDto.getWeakCount()));
        doctorPigEvent.setMnyCount(CountUtil.getIntegerDefault0(farrowingDto.getMnyCount()));
        doctorPigEvent.setJxCount(CountUtil.getIntegerDefault0(farrowingDto.getJxCount()));
        doctorPigEvent.setDeadCount(CountUtil.getIntegerDefault0(farrowingDto.getDeadCount()));
        doctorPigEvent.setBlackCount(CountUtil.getIntegerDefault0(farrowingDto.getBlackCount()));

        if (farrowingDate.isBefore(pregJudgeDate)) {
            extra.put("farrowingType", FarrowingType.EARLY.getKey());
        } else {
            extra.put("farrowingType", FarrowingType.USUAL.getKey());
        }
        doctorPigEvent.setExtraMap(extra);
        return doctorPigEvent;
    }

    @Override
    public DoctorPigTrack createOrUpdatePigTrack(DoctorBasicInputInfoDto basic, BasePigEventInputDto inputDto) {
        DoctorPigTrack doctorPigTrack = doctorPigTrackDao.findByPigId(inputDto.getPigId());
        DoctorFarrowingDto farrowingDto = (DoctorFarrowingDto) inputDto;
        Map<String, Object> extra = doctorPigTrack.getExtraMap();
        // 对应的 仔猪 猪舍的 信息
        extra.put("toBarnId", doctorPigTrack.getCurrentBarnId());
        extra.put("toBarnName", doctorPigTrack.getCurrentBarnName());
        //Long pigEventId = (Long) context.get("doctorPigEventId");

        //分娩时记录下 分娩数量
        doctorPigTrack.setFarrowQty(farrowingDto.getFarrowingLiveCount());
        doctorPigTrack.setUnweanQty(farrowingDto.getFarrowingLiveCount());
        doctorPigTrack.setWeanQty(0);  //分娩时 断奶数为0
        doctorPigTrack.setFarrowAvgWeight(farrowingDto.getBirthNestAvg());
        doctorPigTrack.setWeanAvgWeight(0D); //分娩时, 断奶均重置成0

        doctorPigTrack.setExtraMap(extra);
        doctorPigTrack.setStatus(PigStatus.FEED.getKey());  //母猪进入哺乳的状态

        return doctorPigTrack;
    }

    @Override
    protected void specialHandle(DoctorPigEvent doctorPigEvent, DoctorPigTrack doctorPigTrack, BasePigEventInputDto inputDto, DoctorBasicInputInfoDto basic) {
        super.specialHandle(doctorPigEvent, doctorPigTrack, inputDto, basic);
        //对应的最近一次的 周期配种的初陪 的 isDelivery 字段变成true
        DoctorPigEvent firstMate = doctorPigEventDao.queryLastFirstMate(doctorPigTrack.getPigId(), doctorPigTrack.getCurrentParity());
        if (notNull(firstMate)) {
            firstMate.setIsDelivery(1);
            doctorPigEventDao.update(firstMate);
        }
    }

    @Override
    public void triggerEvent(List<DoctorEventInfo> doctorEventInfoList, DoctorPigEvent doctorPigEvent, DoctorPigTrack doctorPigTrack, BasePigEventInputDto inputDto, DoctorBasicInputInfoDto basic) {
        DoctorFarrowingDto farrowingDto = (DoctorFarrowingDto) inputDto;
        //触发断奶事件
        if (Objects.equals(farrowingDto.getFarrowingLiveCount(), 0)) {
            DoctorWeanDto partWeanDto = DoctorWeanDto.builder()
                    .partWeanDate(farrowingDto.eventAt())
                    .partWeanPigletsCount(0)
                    .partWeanAvgWeight(0d)
                    .build();
            buildAutoEventCommonInfo(farrowingDto, partWeanDto, basic, PigEvent.WEAN, doctorPigEvent.getId());
            doctorSowWeanHandler.handle(doctorEventInfoList, partWeanDto, basic);
        }
        //触发猪群事件
        Long groupId = buildPigGroupCountInfo(doctorEventInfoList, doctorPigTrack, doctorPigEvent, farrowingDto, basic);
        checkState(notNull(groupId), "创建猪事件失败:猪号" + inputDto.getPigCode());
        doctorPigTrack.setGroupId(groupId);
        doctorPigTrackDao.update(doctorPigTrack);

        //向分娩事件事件中放入groupId
        Map<String, Object> extraMap = doctorPigEvent.getExtraMap();
        extraMap.put("farrowGroupId", doctorPigTrack.getGroupId());
        doctorPigEvent.setGroupId(doctorPigTrack.getGroupId());
        doctorPigEvent.setExtraMap(extraMap);
        doctorPigEventDao.update(doctorPigEvent);

    }

    /**
     * 创建对应的猪群
     *
     */
    protected Long buildPigGroupCountInfo(List<DoctorEventInfo> eventInfoList, DoctorPigTrack doctorPigTrack, DoctorPigEvent doctorPigEvent, DoctorFarrowingDto farrowingDto, DoctorBasicInputInfoDto basic) {

        // Build 新建猪群操作方式
        DoctorSowMoveInGroupInput input = new DoctorSowMoveInGroupInput();
        input.setOrgId(basic.getOrgId());
        input.setOrgName(basic.getOrgName());
        input.setFarmId(basic.getFarmId());
        input.setFarmName(basic.getFarmName());
        input.setGroupCode(farrowingDto.getGroupCode());

        input.setFromBarnId(farrowingDto.getBarnId());
        input.setFromBarnName(farrowingDto.getBarnName());
        input.setToBarnId(farrowingDto.getBarnId());
        input.setToBarnName(farrowingDto.getBarnName());
        input.setPigType(PigType.DELIVER_SOW.getValue());
        input.setInType(DoctorMoveInGroupEvent.InType.PIGLET.getValue());
        input.setInTypeName(DoctorMoveInGroupEvent.InType.PIGLET.getDesc());
        input.setSource(PigSource.LOCAL.getKey());

        Integer farrowingLiveCount = MoreObjects.firstNonNull(farrowingDto.getFarrowingLiveCount(), 0);
        Integer sowCount = MoreObjects.firstNonNull(farrowingDto.getLiveSowCount(), 0);
        Integer boarCount = MoreObjects.firstNonNull(farrowingDto.getLiveBoarCount(), 0);
        if (sowCount == 0 && boarCount == 0) sowCount = farrowingLiveCount;

        input.setSex(DoctorGroupTrack.Sex.MIX.getValue());
        input.setQuantity(farrowingLiveCount);
        input.setSowQty(sowCount);
        input.setBoarQty(boarCount);
        input.setAvgDayAge(1);
        input.setAvgWeight(farrowingDto.getBirthNestAvg());
        input.setEventAt(DateUtil.toDateString(doctorPigEvent.getEventAt()));
        input.setIsAuto(1);
        input.setCreatorId(doctorPigEvent.getCreatorId());
        input.setCreatorName(doctorPigEvent.getCreatorName());

        input.setSowEvent(true);  //设置为分娩转入
        input.setWeakQty(CountUtil.getIntegerDefault0(farrowingDto.getWeakCount()));
        input.setHealthyQty(CountUtil.getIntegerDefault0(farrowingDto.getHealthCount()));

        input.setRelPigEventId(doctorPigEvent.getId());
        return doctorCommonGroupEventHandler.sowGroupEventMoveIn(eventInfoList, input);
    }

    private DoctorGroupTrack.Sex judgePigSex(Integer sowCount, Integer boarCount) {
        if (sowCount == 0 && boarCount == 0) {
            return DoctorGroupTrack.Sex.MIX;
        }

        if (sowCount == 0) {
            return DoctorGroupTrack.Sex.MALE;
        }

        if (boarCount == 0) {
            return DoctorGroupTrack.Sex.FEMALE;
        }
        return DoctorGroupTrack.Sex.MIX;
    }

}
