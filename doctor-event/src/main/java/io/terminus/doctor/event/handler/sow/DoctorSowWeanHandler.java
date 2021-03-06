package io.terminus.doctor.event.handler.sow;

import com.google.common.base.MoreObjects;
import com.google.common.collect.Maps;
import io.terminus.doctor.common.exception.InvalidException;
import io.terminus.doctor.event.dao.DoctorBarnDao;
import io.terminus.doctor.event.dao.DoctorGroupTrackDao;
import io.terminus.doctor.event.dto.DoctorBasicInputInfoDto;
import io.terminus.doctor.event.dto.event.BasePigEventInputDto;
import io.terminus.doctor.event.dto.event.DoctorEventInfo;
import io.terminus.doctor.event.dto.event.group.input.BaseGroupInput;
import io.terminus.doctor.event.dto.event.group.input.DoctorWeanGroupInput;
import io.terminus.doctor.event.dto.event.sow.DoctorWeanDto;
import io.terminus.doctor.event.dto.event.usual.DoctorChgLocationDto;
import io.terminus.doctor.event.editHandler.pig.DoctorModifyPigWeanEventHandler;
import io.terminus.doctor.event.enums.PigEvent;
import io.terminus.doctor.event.enums.PigStatus;
import io.terminus.doctor.event.handler.DoctorAbstractEventHandler;
import io.terminus.doctor.event.handler.group.DoctorCommonGroupEventHandler;
import io.terminus.doctor.event.handler.usual.DoctorChgLocationHandler;
import io.terminus.doctor.event.model.DoctorBarn;
import io.terminus.doctor.event.model.DoctorGroupTrack;
import io.terminus.doctor.event.model.DoctorPigEvent;
import io.terminus.doctor.event.model.DoctorPigTrack;
import io.terminus.doctor.event.util.EventUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import static io.terminus.common.utils.Arguments.notNull;
import static io.terminus.doctor.common.utils.Checks.expectTrue;

/**
 * Created by .
 * Date:2016-05-27
 * Email:yaoqj@terminus.io
 * Descirbe:
 */
@Slf4j
@Component
public class DoctorSowWeanHandler extends DoctorAbstractEventHandler {

    @Autowired
    private DoctorChgLocationHandler chgLocationHandler;
    @Autowired
    private DoctorGroupTrackDao doctorGroupTrackDao;
    @Autowired
    private DoctorBarnDao doctorBarnDao;
    @Autowired
    private DoctorCommonGroupEventHandler doctorCommonGroupEventHandler;
    @Autowired
    private DoctorModifyPigWeanEventHandler doctorModifyPigWeanEventHandler;

    @Override
    public void handleCheck(DoctorPigEvent executeEvent, DoctorPigTrack fromTrack) {
        super.handleCheck(executeEvent, fromTrack);
        expectTrue(Objects.equals(fromTrack.getStatus(), PigStatus.FEED.getKey())
                ,"pig.status.failed", PigEvent.from(executeEvent.getType()).getName(), PigStatus.from(fromTrack.getStatus()).getName());
        DoctorWeanDto weanDto = JSON_MAPPER.fromJson(executeEvent.getExtra(), DoctorWeanDto.class);
        expectTrue(MoreObjects.firstNonNull(weanDto.getQualifiedCount(), 0) + MoreObjects.firstNonNull(weanDto.getNotQualifiedCount(), 0) <= weanDto.getFarrowingLiveCount(), "qualified.add.noQualified.over.live", weanDto.getPigCode());
        if (Objects.equals(weanDto.getFarrowingLiveCount(), 0) && !Objects.equals(weanDto.getPartWeanAvgWeight(), 0d)) {
            throw new InvalidException("wean.avg.weight.not.zero", weanDto.getPigCode());
        }
        if (!Objects.equals(weanDto.getFarrowingLiveCount(), 0) && weanDto.getPartWeanAvgWeight() > 9) {
            throw new InvalidException("wean.avg.weight.range.error", weanDto.getPigCode());
        }
    }

    @Override
    public DoctorPigEvent buildPigEvent(DoctorBasicInputInfoDto basic, BasePigEventInputDto inputDto) {
        DoctorPigEvent doctorPigEvent = super.buildPigEvent(basic, inputDto);
        DoctorWeanDto weanDto = (DoctorWeanDto) inputDto;
        DoctorPigTrack pigTrack = doctorPigTrackDao.findByPigId(doctorPigEvent.getPigId());

        //????????????
        doctorPigEvent.setFeedDays(doctorModifyPigWeanEventHandler.getWeanAvgAge(pigTrack.getPigId(), doctorPigEventDao.findLastParity(pigTrack.getPigId()), weanDto.eventAt()));

        //???????????????????????????
        doctorPigEvent.setWeanCount(weanDto.getPartWeanPigletsCount());
        doctorPigEvent.setWeanAvgWeight(weanDto.getPartWeanAvgWeight());

//        Integer quaQty = doctorPigEvent.getWeanCount();
//        if (weanDto.getQualifiedCount() != null) {
//            quaQty = weanDto.getQualifiedCount();
//        }
        doctorPigEvent.setHealthCount(weanDto.getQualifiedCount());
 //       doctorPigEvent.setHealthCount(quaQty);    //??? ?????????????????????????????????
     //   doctorPigEvent.setWeakCount(doctorPigEvent.getWeanCount() - quaQty);
        doctorPigEvent.setWeakCount(weanDto.getNotQualifiedCount());
        Map<String, Object> extraMap = doctorPigEvent.getExtraMap();
    //    extraMap.put("qualifiedCount", quaQty);
        extraMap.put("qualifiedCount", weanDto.getQualifiedCount());
        doctorPigEvent.setExtraMap(extraMap);

        expectTrue(notNull(pigTrack), "pig.track.not.null", inputDto.getPigId());
        expectTrue(notNull(pigTrack.getGroupId()), "farrow.groupId.not.null", weanDto.getPigId());
        doctorPigEvent.setGroupId(pigTrack.getGroupId());   //????????????????????????groupId
        return doctorPigEvent;
    }

    @Override
    public DoctorPigTrack buildPigTrack(DoctorPigEvent executeEvent, DoctorPigTrack fromTrack) {
        DoctorPigTrack toTrack = super.buildPigTrack(executeEvent, fromTrack);
        expectTrue(Objects.equals(toTrack.getStatus(), PigStatus.FEED.getKey()), "sow.status.not.feed", PigStatus.from(toTrack.getStatus()).getName());

        //????????????
        Integer unweanCount = toTrack.getUnweanQty();    //???????????????
        Integer weanCount = toTrack.getWeanQty();        //????????????
        Integer toWeanCount = executeEvent.getWeanCount();
        expectTrue(Objects.equals(toWeanCount,unweanCount), "need.all.wean", toWeanCount, unweanCount);
        toTrack.setUnweanQty(unweanCount - toWeanCount); //???????????????
        toTrack.setWeanQty(weanCount + toWeanCount);     //????????????

        //????????????
        Double toWeanAvgWeight = executeEvent.getWeanAvgWeight();
        Double weanAvgWeight = ((MoreObjects.firstNonNull(toTrack.getWeanAvgWeight(), 0D) * weanCount) + toWeanAvgWeight * toWeanCount ) / (weanCount + toWeanCount);
        toTrack.setWeanAvgWeight(weanAvgWeight);

        Map<String, Object> newExtraMap = Maps.newHashMap();
        //??????extra??????
        newExtraMap.put("hasWeanToMating", true);

        fromTrack.setExtraMap(newExtraMap);
        //????????????????????????id??????????????????????????????
        //basic.setWeanGroupId(doctorPigTrack.getGroupId());

        //???????????????, ????????????????????????????????????
        if (toTrack.getUnweanQty() == 0) {
            toTrack.setStatus(PigStatus.Wean.getKey());
            toTrack.setGroupId(-1L);  //groupId = -1 ?????? NULL
            toTrack.setFarrowAvgWeight(0D);
            toTrack.setFarrowQty(0);  //????????? 0
            toTrack.setWeanAvgWeight(0D);
        }
        return toTrack;
    }

    @Override
    protected void updateDailyForNew(DoctorPigEvent newPigEvent) {
        BasePigEventInputDto inputDto = JSON_MAPPER.fromJson(newPigEvent.getExtra(), DoctorWeanDto.class);
        doctorModifyPigWeanEventHandler.updateDailyOfNew(newPigEvent, inputDto);
    }

    @Override
    protected void triggerEvent(List<DoctorEventInfo> doctorEventInfoList, DoctorPigEvent doctorPigEvent, DoctorPigTrack doctorPigTrack) {
        DoctorWeanDto partWeanDto = JSON_MAPPER.fromJson(doctorPigEvent.getExtra(), DoctorWeanDto.class);
        //????????????????????????
        DoctorWeanGroupInput input = (DoctorWeanGroupInput) buildTriggerGroupEventInput(doctorPigEvent);
        doctorCommonGroupEventHandler.sowWeanGroupEvent(doctorEventInfoList, input);
        //??????????????????
        if (Objects.equals(partWeanDto.getPartWeanPigletsCount(), partWeanDto.getFarrowingLiveCount()) && partWeanDto.getChgLocationToBarnId() != null) {
            DoctorBarn doctorBarn = doctorBarnDao.findById(partWeanDto.getChgLocationToBarnId());
            DoctorChgLocationDto chgLocationDto = DoctorChgLocationDto.builder()
                    .changeLocationDate(doctorPigEvent.getEventAt())
                    .chgLocationFromBarnId(doctorPigEvent.getBarnId())
                    .chgLocationFromBarnName(doctorPigEvent.getBarnName())
                    .chgLocationToBarnId(partWeanDto.getChgLocationToBarnId())
                    .chgLocationToBarnName(doctorBarn.getName())
                    .build();
            buildAutoEventCommonInfo(partWeanDto, chgLocationDto, PigEvent.CHG_LOCATION, doctorPigEvent.getId());
            //??????basic
            DoctorBasicInputInfoDto basic = DoctorBasicInputInfoDto.builder()
                    .orgId(doctorPigEvent.getOrgId())
                    .orgName(doctorPigEvent.getOrgName())
                    .farmId(doctorPigEvent.getFarmId())
                    .farmName(doctorPigEvent.getFarmName())
                    .staffId(doctorPigEvent.getOperatorId())
                    .staffName(doctorPigEvent.getOperatorName())
                    .build();
            chgLocationHandler.handle(doctorEventInfoList, chgLocationHandler.buildPigEvent(basic, chgLocationDto), doctorPigTrack);
        }

        //??????????????????
//        updateGroupInfo(doctorPigEvent);
    }

    @Override
    public BaseGroupInput buildTriggerGroupEventInput(DoctorPigEvent pigEvent) {
        return doctorModifyPigWeanEventHandler.buildTriggerGroupEventInput(pigEvent);
    }


    private void updateGroupInfo(DoctorPigEvent doctorPigEvent) {
        Long farrowGroupId = doctorPigEvent.getGroupId();
        log.info("updateGroupInfo farrow group track, groupId:{}, event:{}", farrowGroupId, doctorPigEvent);

        //?????????????????????????????????
        DoctorGroupTrack groupTrack = doctorGroupTrackDao.findByGroupId(farrowGroupId);
        expectTrue(notNull(groupTrack), "farrow.group.track.not.null", farrowGroupId);
        groupTrack.setQuaQty(EventUtil.plusInt(groupTrack.getQuaQty(), doctorPigEvent.getHealthCount()));
        groupTrack.setUnqQty(EventUtil.plusInt(groupTrack.getUnqQty(), doctorPigEvent.getWeakCount()));
        groupTrack.setWeanQty(EventUtil.plusInt(groupTrack.getWeanQty(), doctorPigEvent.getWeanCount()));
        groupTrack.setUnweanQty(EventUtil.plusInt(groupTrack.getUnweanQty(), -doctorPigEvent.getWeanCount()));
        groupTrack.setWeanWeight(EventUtil.plusDouble(groupTrack.getWeanWeight(), doctorPigEvent.getWeanAvgWeight() * doctorPigEvent.getWeanCount()));
        doctorGroupTrackDao.update(groupTrack);
    }
}
