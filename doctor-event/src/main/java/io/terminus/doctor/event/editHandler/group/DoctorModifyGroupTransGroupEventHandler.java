package io.terminus.doctor.event.editHandler.group;

import io.terminus.doctor.common.enums.PigType;
import io.terminus.doctor.common.exception.InvalidException;
import io.terminus.doctor.common.utils.DateUtil;
import io.terminus.doctor.event.dao.DoctorBarnDao;
import io.terminus.doctor.event.dto.event.edit.DoctorEventChangeDto;
import io.terminus.doctor.event.dto.event.group.input.BaseGroupInput;
import io.terminus.doctor.event.dto.event.group.input.DoctorMoveInGroupInput;
import io.terminus.doctor.event.dto.event.group.input.DoctorNewGroupInput;
import io.terminus.doctor.event.dto.event.group.input.DoctorTransGroupInput;
import io.terminus.doctor.event.enums.GroupEventType;
import io.terminus.doctor.event.enums.InType;
import io.terminus.doctor.event.enums.IsOrNot;
import io.terminus.doctor.event.model.DoctorBarn;
import io.terminus.doctor.event.model.DoctorDailyGroup;
import io.terminus.doctor.event.model.DoctorGroupDaily;
import io.terminus.doctor.event.model.DoctorGroupEvent;
import io.terminus.doctor.event.model.DoctorGroupTrack;
import io.terminus.doctor.event.util.EventUtil;
import org.apache.commons.lang3.time.DateUtils;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.Objects;

import static io.terminus.common.utils.Arguments.notNull;


/**
 * Created by IntelliJ IDEA.
 * Author: luoys
 * Date: 11:31 2017/4/15
 */
@Component
public class DoctorModifyGroupTransGroupEventHandler extends DoctorAbstractModifyGroupEventHandler{
    @Autowired
    private DoctorModifyGroupMoveInEventHandler modifyGroupMoveInEventHandler;
    @Autowired
    private DoctorModifyGroupNewEventHandler modifyGroupNewEventHandler;
    @Autowired
    private DoctorModifyGroupCloseEventHandler modifyGroupCloseEventHandler;
    @Autowired
    private DoctorBarnDao doctorBarnDao;

    @Override
    protected void modifyHandleCheck(DoctorGroupEvent oldGroupEvent, BaseGroupInput input) {
        super.modifyHandleCheck(oldGroupEvent, input);
        DoctorTransGroupInput newInput = (DoctorTransGroupInput) input;
        validGroupLiveStock(oldGroupEvent.getGroupId(), oldGroupEvent.getGroupCode(), oldGroupEvent.getId(),
                oldGroupEvent.getEventAt(), DateUtil.toDate(newInput.getEventAt()),
                oldGroupEvent.getQuantity(), -newInput.getQuantity(),
                EventUtil.minusInt(oldGroupEvent.getQuantity(), newInput.getQuantity()));

        DoctorTransGroupInput oldInput = JSON_MAPPER.fromJson(oldGroupEvent.getExtra(), DoctorTransGroupInput.class);
        if (!Objects.equals(oldInput.getToGroupId(), newInput.getToGroupId())) {
            throw new InvalidException("move.in.group.not.allow.modify");
        }
    }

    @Override
    public DoctorEventChangeDto buildEventChange(DoctorGroupEvent oldGroupEvent, BaseGroupInput input) {
        DoctorTransGroupInput oldInput = JSON_MAPPER.fromJson(oldGroupEvent.getExtra(), DoctorTransGroupInput.class);
        DoctorTransGroupInput newInput = (DoctorTransGroupInput) input;
        DoctorBarn toBarn = doctorBarnDao.findById(oldInput.getToBarnId());
        return DoctorEventChangeDto.builder()
                .farmId(oldGroupEvent.getFarmId())
                .pigType(oldGroupEvent.getPigType())
                .businessId(oldGroupEvent.getGroupId())
                .oldToGroupId(oldInput.getToGroupId())
                .toGroupId(newInput.getToGroupId())
                .oldEventAt(DateTime.parse(oldInput.getEventAt()).toDate())
                .newEventAt(DateTime.parse(newInput.getEventAt()).toDate())
                .quantityChange(EventUtil.minusInt(newInput.getQuantity(), oldInput.getQuantity()))
                .avgWeightChange(EventUtil.minusDouble(newInput.getAvgWeight(), oldInput.getAvgWeight()))
                .weightChange(EventUtil.minusDouble(newInput.getWeight(), oldInput.getWeight()))
                .isSowTrigger(notNull(oldGroupEvent.getSowId()))
                .transBarnType(toBarn.getPigType())
                .transGroupType(oldGroupEvent.getTransGroupType())
                .build();
    }

    @Override
    public DoctorGroupEvent buildNewEvent(DoctorGroupEvent oldGroupEvent, BaseGroupInput input) {
        DoctorGroupEvent newGroupEvent = super.buildNewEvent(oldGroupEvent, input);
        DoctorTransGroupInput newInput = (DoctorTransGroupInput) input;
        newGroupEvent.setQuantity(newInput.getQuantity());
        newGroupEvent.setWeight(newInput.getWeight());
        newGroupEvent.setAvgWeight(newInput.getAvgWeight());
        return newGroupEvent;
    }

    @Override
    public DoctorGroupTrack buildNewTrack(DoctorGroupTrack oldGroupTrack, DoctorEventChangeDto changeDto) {
        if (changeDto.getIsSowTrigger()) {
            oldGroupTrack.setLiveQty(EventUtil.minusInt(oldGroupTrack.getLiveQty(), changeDto.getQuantityChange()));
            oldGroupTrack.setHealthyQty(EventUtil.minusInt(oldGroupTrack.getLiveQty(), changeDto.getQuantityChange()));
            oldGroupTrack.setBirthWeight(EventUtil.minusDouble(oldGroupTrack.getBirthWeight(), changeDto.getWeightChange()));
            oldGroupTrack.setUnweanQty(EventUtil.minusInt(oldGroupTrack.getUnweanQty(), changeDto.getQuantityChange()));
        }
        oldGroupTrack.setQuantity(EventUtil.minusInt(oldGroupTrack.getQuantity(), changeDto.getQuantityChange()));
        return oldGroupTrack;
    }

    @Override
    protected void updateDailyForModify(DoctorGroupEvent oldGroupEvent, BaseGroupInput input, DoctorEventChangeDto changeDto) {
        if (Objects.equals(oldGroupEvent.getTransGroupType(), DoctorGroupEvent.TransGroupType.IN.getValue())) {
            return;
        }
        if (DateUtils.isSameDay(changeDto.getNewEventAt(), changeDto.getOldEventAt())) {
            DoctorGroupDaily oldDailyGroup = doctorDailyReportManager.findDoctorGroupDaily(oldGroupEvent.getFarmId(), oldGroupEvent.getPigType(), changeDto.getOldEventAt());
            doctorGroupDailyDao.update(buildDailyGroup(oldDailyGroup, changeDto));
            updateDailyGroupLiveStock(changeDto.getFarmId(), changeDto.getPigType(),
                    getAfterDay(oldGroupEvent.getEventAt()), EventUtil.minusInt(0, changeDto.getQuantityChange()));

            //??????
            DoctorDailyGroup oldDaily = oldDailyReportManager.findByGroupIdAndSumAt(oldGroupEvent.getGroupId(), changeDto.getOldEventAt());
            oldDailyReportManager.createOrUpdateDailyGroup(oldBuildDailyGroup(oldDaily, changeDto));
            oldUpdateDailyGroupLiveStock(oldGroupEvent.getGroupId(),
                    getAfterDay(oldGroupEvent.getEventAt()), EventUtil.minusInt(0, changeDto.getQuantityChange()));
        } else {
            updateDailyOfDelete(oldGroupEvent);
            updateDailyOfNew(oldGroupEvent, input);
        }
    }

    @Override
    protected void triggerEventModifyHandle(DoctorGroupEvent newEvent) {
        //1.??????????????????
        DoctorGroupEvent newCreateEvent = doctorGroupEventDao.findByRelGroupEventIdAndType(newEvent.getId(), GroupEventType.NEW.getValue());
        if (notNull(newCreateEvent)) {
            BaseGroupInput newInput = JSON_MAPPER.fromJson(newCreateEvent.getExtra(), DoctorNewGroupInput.class);
            newInput.setEventAt(DateUtil.toDateString(newEvent.getEventAt()));
            modifyGroupNewEventHandler.modifyHandle(newCreateEvent, newInput);
        }
        //2.??????????????????
        DoctorGroupEvent moveInEvent = doctorGroupEventDao.findByRelGroupEventIdAndType(newEvent.getId(), GroupEventType.MOVE_IN.getValue());
        modifyGroupMoveInEventHandler.modifyHandle(moveInEvent, buildTriggerGroupEventInput(newEvent));

        //3.??????????????????
        DoctorGroupEvent closeEvent = doctorGroupEventDao.findByRelGroupEventIdAndType(newEvent.getId(), GroupEventType.CLOSE.getValue());
        if (notNull(closeEvent)) {
            modifyGroupCloseEventHandler.modifyHandle(closeEvent, buildGroupCloseInput(newEvent));
        }
    }

    @Override
    public Boolean rollbackHandleCheck(DoctorGroupEvent deleteGroupEvent) {
        DoctorGroupEvent moveInEvent = doctorGroupEventDao.findByRelGroupEventIdAndType(deleteGroupEvent.getId(), GroupEventType.MOVE_IN.getValue());
        Boolean isRollback = modifyGroupMoveInEventHandler.rollbackHandleCheck(moveInEvent);

        DoctorGroupEvent newCreateEvent = doctorGroupEventDao.findByRelGroupEventIdAndType(deleteGroupEvent.getId(), GroupEventType.NEW.getValue());
        if (notNull(newCreateEvent)) {
            isRollback &= modifyGroupNewEventHandler.rollbackHandleCheck(newCreateEvent);
        }

        //??????????????????
        DoctorGroupEvent closeEvent = doctorGroupEventDao.findByRelGroupEventIdAndType(deleteGroupEvent.getId(), GroupEventType.CLOSE.getValue());
        if (notNull(closeEvent)) {
            isRollback &= modifyGroupCloseEventHandler.rollbackHandleCheck(closeEvent);
        }

        return isRollback;
    }

    @Override
    protected void triggerEventRollbackHandle(DoctorGroupEvent deleteGroupEvent, Long operatorId, String operatorName) {
        //1.????????????
        DoctorGroupEvent moveInEvent = doctorGroupEventDao.findByRelGroupEventIdAndType(deleteGroupEvent.getId(), GroupEventType.MOVE_IN.getValue());
        modifyGroupMoveInEventHandler.rollbackHandle(moveInEvent, operatorId, operatorName);

        //2.????????????
        DoctorGroupEvent newCreateEvent = doctorGroupEventDao.findByRelGroupEventIdAndType(deleteGroupEvent.getId(), GroupEventType.NEW.getValue());
        if (notNull(newCreateEvent)) {
            modifyGroupNewEventHandler.rollbackHandle(newCreateEvent, operatorId, operatorName);
        }

        //3.????????????
        DoctorGroupEvent closeEvent = doctorGroupEventDao.findByRelGroupEventIdAndType(deleteGroupEvent.getId(), GroupEventType.CLOSE.getValue());
        if (notNull(closeEvent)) {
            modifyGroupCloseEventHandler.rollbackHandle(closeEvent, operatorId, operatorName);
        }
    }

    @Override
    protected DoctorGroupTrack buildNewTrackForRollback(DoctorGroupEvent deleteGroupEvent, DoctorGroupTrack oldGroupTrack) {
        if (notNull(deleteGroupEvent.getSowId())) {
            oldGroupTrack.setLiveQty(EventUtil.plusInt(oldGroupTrack.getLiveQty(), deleteGroupEvent.getQuantity()));
            oldGroupTrack.setHealthyQty(EventUtil.plusInt(oldGroupTrack.getLiveQty(), deleteGroupEvent.getQuantity()));
            oldGroupTrack.setBirthWeight(EventUtil.plusDouble(oldGroupTrack.getBirthWeight(), deleteGroupEvent.getWeight()));
            oldGroupTrack.setUnweanQty(EventUtil.plusInt(oldGroupTrack.getUnweanQty(), deleteGroupEvent.getQuantity()));
        }
        oldGroupTrack.setQuantity(EventUtil.plusInt(oldGroupTrack.getQuantity(), deleteGroupEvent.getQuantity()));
        return oldGroupTrack;
    }

    @Override
    protected void updateDailyForDelete(DoctorGroupEvent deleteGroupEvent) {
        updateDailyOfDelete(deleteGroupEvent);
    }

    @Override
    public void updateDailyOfDelete(DoctorGroupEvent oldGroupEvent) {
        if (Objects.equals(oldGroupEvent.getTransGroupType(), DoctorGroupEvent.TransGroupType.IN.getValue())) {
            return;
        }
        DoctorTransGroupInput oldInput = JSON_MAPPER.fromJson(oldGroupEvent.getExtra(), DoctorTransGroupInput.class);
        DoctorBarn toBarn = doctorBarnDao.findById(oldInput.getToBarnId());
        DoctorEventChangeDto changeDto1 = DoctorEventChangeDto.builder()
                .quantityChange(EventUtil.minusInt(0, oldInput.getQuantity()))
                .weightChange(EventUtil.minusDouble(0D, oldInput.getWeight()))
                .transBarnType(toBarn.getPigType())
                .transGroupType(oldGroupEvent.getTransGroupType())
                .build();
        DoctorGroupDaily oldDailyGroup1 = doctorDailyReportManager.findDoctorGroupDaily(oldGroupEvent.getFarmId(), oldGroupEvent.getPigType(), oldGroupEvent.getEventAt());
        doctorGroupDailyDao.update(buildDailyGroup(oldDailyGroup1, changeDto1));
        updateDailyGroupLiveStock(oldGroupEvent.getFarmId(), oldGroupEvent.getPigType(),
                getAfterDay(oldGroupEvent.getEventAt()), -changeDto1.getQuantityChange());

        //??????
        DoctorDailyGroup oldDaily = oldDailyReportManager.findByGroupIdAndSumAt(oldGroupEvent.getGroupId(), oldGroupEvent.getEventAt());
        oldDailyReportManager.createOrUpdateDailyGroup(oldBuildDailyGroup(oldDaily, changeDto1));
        oldUpdateDailyGroupLiveStock(oldGroupEvent.getGroupId(),
                getAfterDay(oldGroupEvent.getEventAt()), -changeDto1.getQuantityChange());

    }

    @Override
    public void updateDailyOfNew(DoctorGroupEvent newGroupEvent, BaseGroupInput input) {
        if (Objects.equals(newGroupEvent.getTransGroupType(), DoctorGroupEvent.TransGroupType.IN.getValue())) {
            return;
        }
        Date eventAt = DateUtil.toDate(input.getEventAt());
        DoctorTransGroupInput newInput = (DoctorTransGroupInput) input;
        DoctorBarn toBarn = doctorBarnDao.findById(newInput.getToBarnId());
        DoctorEventChangeDto changeDto2 = DoctorEventChangeDto.builder()
                .quantityChange(newInput.getQuantity())
                .weightChange(newInput.getWeight())
                .transBarnType(toBarn.getPigType())
                .transGroupType(newGroupEvent.getTransGroupType())
                .build();
        DoctorGroupDaily oldDailyGroup2 = doctorDailyReportManager.findDoctorGroupDaily(newGroupEvent.getFarmId(), newGroupEvent.getPigType(), eventAt);
        doctorDailyReportManager.createOrUpdateGroupDaily(buildDailyGroup(oldDailyGroup2, changeDto2));
        updateDailyGroupLiveStock(newGroupEvent.getFarmId(), newGroupEvent.getPigType(),
                getAfterDay(eventAt), -changeDto2.getQuantityChange());

        //??????
        DoctorDailyGroup oldDaily = oldDailyReportManager.findByGroupIdAndSumAt(newGroupEvent.getGroupId(), newGroupEvent.getEventAt());
        oldDailyReportManager.createOrUpdateDailyGroup(oldBuildDailyGroup(oldDaily, changeDto2));
        oldUpdateDailyGroupLiveStock(newGroupEvent.getGroupId(),
                getAfterDay(eventAt), -changeDto2.getQuantityChange());
    }

    @Override
    protected DoctorGroupDaily buildDailyGroup(DoctorGroupDaily oldDailyGroup, DoctorEventChangeDto changeDto) {
        oldDailyGroup = super.buildDailyGroup(oldDailyGroup, changeDto);
        oldDailyGroup.setEnd(EventUtil.minusInt(oldDailyGroup.getEnd(), changeDto.getQuantityChange()));
//        oldDailyGroup.setTurnOutWeight(EventUtil.plusDouble(oldDailyGroup.getTurnOutWeight(), changeDto.getWeightChange()));

        if (Objects.equals(changeDto.getTransBarnType(), PigType.FATTEN_PIG.getValue())) {
            oldDailyGroup.setToFatten(EventUtil.plusInt(oldDailyGroup.getToFatten(), changeDto.getQuantityChange()));
//            oldDailyGroup.setToFattenWeight(EventUtil.plusDouble(oldDailyGroup.getToFattenWeight(), changeDto.getWeightChange()));
        }else if (Objects.equals(changeDto.getTransBarnType(), PigType.RESERVE.getValue())) {
            oldDailyGroup.setToHoubei(EventUtil.plusInt(oldDailyGroup.getToHoubei(), changeDto.getQuantityChange()));
//            oldDailyGroup.setToHoubeiWeight(EventUtil.plusDouble(oldDailyGroup.getToHoubeiWeight(), changeDto.getWeightChange()));
        } else if (Objects.equals(changeDto.getTransBarnType(), PigType.NURSERY_PIGLET.getValue())) {
            oldDailyGroup.setToNursery(EventUtil.plusInt(oldDailyGroup.getToNursery(), changeDto.getQuantityChange()));
//            oldDailyGroup.setToNurseryWeight(EventUtil.plusDouble(oldDailyGroup.getToNurseryWeight(), changeDto.getWeightChange()));
        }
        return oldDailyGroup;
    }

    protected DoctorDailyGroup oldBuildDailyGroup(DoctorDailyGroup oldDailyGroup, DoctorEventChangeDto changeDto) {

        oldDailyGroup.setEnd(EventUtil.minusInt(oldDailyGroup.getEnd(), changeDto.getQuantityChange()));

        if (Objects.equals(changeDto.getTransGroupType(), DoctorGroupEvent.TransGroupType.IN.getValue())) {
            oldDailyGroup.setInnerOut(EventUtil.plusInt(oldDailyGroup.getInnerOut(), changeDto.getQuantityChange()));
            return oldDailyGroup;
        }

        if (Objects.equals(changeDto.getTransBarnType(), PigType.FATTEN_PIG.getValue())) {
            oldDailyGroup.setToFatten(EventUtil.plusInt(oldDailyGroup.getToFatten(), changeDto.getQuantityChange()));
        }else if (Objects.equals(changeDto.getTransBarnType(), PigType.RESERVE.getValue())) {
            oldDailyGroup.setToHoubei(EventUtil.plusInt(oldDailyGroup.getToHoubei(), changeDto.getQuantityChange()));
        } else if (Objects.equals(changeDto.getTransBarnType(), PigType.NURSERY_PIGLET.getValue())) {
            oldDailyGroup.setToNursery(EventUtil.plusInt(oldDailyGroup.getToNursery(), changeDto.getQuantityChange()));
        }
        return oldDailyGroup;
    }

    /**
     * ??????????????????????????????
     * @param transGroupEvent ?????????
     * @return ??????????????????
     */
    public DoctorMoveInGroupInput buildTriggerGroupEventInput(DoctorGroupEvent transGroupEvent) {
        DoctorTransGroupInput transGroup = JSON_MAPPER.fromJson(transGroupEvent.getExtra(), DoctorTransGroupInput.class);
        DoctorGroupTrack groupTrack = doctorGroupTrackDao.findByGroupId(transGroupEvent.getGroupId());

        DoctorMoveInGroupInput moveIn = new DoctorMoveInGroupInput();
        moveIn.setSowId(transGroup.getSowId());
        moveIn.setSowCode(transGroup.getSowCode());
        moveIn.setEventAt(transGroup.getEventAt());
        moveIn.setEventType(GroupEventType.MOVE_IN.getValue());
        moveIn.setIsAuto(IsOrNot.YES.getValue());
        moveIn.setCreatorId(transGroup.getCreatorId());
        moveIn.setCreatorName(transGroup.getCreatorName());
        moveIn.setRelGroupEventId(transGroup.getRelGroupEventId());

        moveIn.setInType(InType.GROUP.getValue());       //????????????
        moveIn.setInTypeName(InType.GROUP.getDesc());
        moveIn.setSource(transGroup.getSource());        //?????????????????? ??????(??????), ??????(??????)
        moveIn.setBreedId(transGroup.getBreedId());
        moveIn.setBreedName(transGroup.getBreedName());
        moveIn.setFromBarnId(transGroupEvent.getBarnId());
        moveIn.setFromBarnName(transGroupEvent.getBarnName());
        moveIn.setFromGroupId(transGroupEvent.getGroupId());
        moveIn.setFromGroupCode(transGroupEvent.getGroupCode());
        moveIn.setQuantity(transGroup.getQuantity());
        moveIn.setBoarQty(transGroup.getBoarQty());
        moveIn.setSowQty(transGroup.getSowQty());
        moveIn.setAvgWeight(EventUtil.getAvgWeight(transGroup.getWeight(), transGroup.getQuantity()));  //????????????
        moveIn.setSowEvent(transGroup.isSowEvent());    //?????????????????????????????????
        moveIn.setAvgDayAge(groupTrack.getAvgDayAge());
        return moveIn;
    }
}
