package io.terminus.doctor.event.handler.usual;

import com.google.common.base.MoreObjects;
import io.terminus.doctor.common.enums.PigType;
import io.terminus.doctor.common.utils.DateUtil;
import io.terminus.doctor.event.dao.DoctorBarnDao;
import io.terminus.doctor.event.dao.DoctorGroupDao;
import io.terminus.doctor.event.dao.DoctorGroupTrackDao;
import io.terminus.doctor.event.dto.DoctorBasicInputInfoDto;
import io.terminus.doctor.event.dto.event.BasePigEventInputDto;
import io.terminus.doctor.event.dto.event.DoctorEventInfo;
import io.terminus.doctor.event.dto.event.group.input.DoctorTransGroupInput;
import io.terminus.doctor.event.dto.event.usual.DoctorChgLocationDto;
import io.terminus.doctor.event.editHandler.pig.DoctorModifyPigChgLocationEventHandler;
import io.terminus.doctor.event.enums.IsOrNot;
import io.terminus.doctor.event.enums.PigEvent;
import io.terminus.doctor.event.enums.PigSource;
import io.terminus.doctor.event.enums.PigStatus;
import io.terminus.doctor.event.handler.DoctorAbstractEventHandler;
import io.terminus.doctor.event.handler.DoctorEventSelector;
import io.terminus.doctor.event.handler.group.DoctorTransGroupEventHandler;
import io.terminus.doctor.event.model.DoctorBarn;
import io.terminus.doctor.event.model.DoctorGroup;
import io.terminus.doctor.event.model.DoctorGroupTrack;
import io.terminus.doctor.event.model.DoctorPigEvent;
import io.terminus.doctor.event.model.DoctorPigTrack;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static io.terminus.common.utils.Arguments.notEmpty;
import static io.terminus.common.utils.Arguments.notNull;
import static io.terminus.doctor.common.enums.PigType.MATING_TYPES;
import static io.terminus.doctor.common.enums.PigType.PREG_SOW;
import static io.terminus.doctor.common.utils.Checks.expectTrue;

/**
 * Created by yaoqijun.
 * Date:2016-05-27
 * Email:yaoqj@terminus.io
 * Descirbe:
 */
@Slf4j
@Component
public class DoctorChgLocationHandler extends DoctorAbstractEventHandler{

    @Autowired
    private DoctorBarnDao doctorBarnDao;
    @Autowired
    private DoctorGroupDao doctorGroupDao;
    @Autowired
    private DoctorGroupTrackDao doctorGroupTrackDao;
    @Autowired
    private DoctorTransGroupEventHandler transGroupEventHandler;
    @Autowired
    private DoctorModifyPigChgLocationEventHandler modifyPigChgLocationEventHandler;

    @Override
    public void handleCheck(DoctorPigEvent executeEvent, DoctorPigTrack fromTrack) {
        super.handleCheck(executeEvent, fromTrack);
        DoctorChgLocationDto chgLocationDto = JSON_MAPPER.fromJson(executeEvent.getExtra(), DoctorChgLocationDto.class);
        expectTrue(!Objects.equals(chgLocationDto.getChgLocationFromBarnId(), chgLocationDto.getChgLocationToBarnId()), "same.barn.not.trans");
    }

    @Override
    public DoctorPigEvent buildPigEvent(DoctorBasicInputInfoDto basic, BasePigEventInputDto inputDto) {
        DoctorPigEvent doctorPigEvent =  super.buildPigEvent(basic, inputDto);
        DoctorChgLocationDto chgLocationDto = (DoctorChgLocationDto) inputDto;
        DoctorBarn fromBarn = doctorBarnDao.findById(chgLocationDto.getChgLocationFromBarnId());
        expectTrue(notNull(fromBarn), "barn.not.null", chgLocationDto.getChgLocationFromBarnId());
        DoctorBarn toBarn = doctorBarnDao.findById(chgLocationDto.getChgLocationToBarnId());
        expectTrue(notNull(toBarn), "barn.not.null", chgLocationDto.getChgLocationToBarnId());
        if (Objects.equals(fromBarn.getPigType(), PREG_SOW.getValue()) && Objects.equals(toBarn.getPigType(), PigType.DELIVER_SOW.getValue())) {
            doctorPigEvent.setType(PigEvent.TO_FARROWING.getKey());
            doctorPigEvent.setName(PigEvent.TO_FARROWING.getName());
        } else if (Objects.equals(fromBarn.getPigType(), PigType.DELIVER_SOW.getValue()) && MATING_TYPES.contains(toBarn.getPigType())) {
            doctorPigEvent.setType(PigEvent.TO_MATING.getKey());
            doctorPigEvent.setName(PigEvent.TO_MATING.getName());
        } else {
            doctorPigEvent.setType(PigEvent.CHG_LOCATION.getKey());
            doctorPigEvent.setName(PigEvent.CHG_LOCATION.getName());
        }
        DoctorPigTrack pigTrack = doctorPigTrackDao.findByPigId(doctorPigEvent.getPigId());
        doctorPigEvent.setGroupId(pigTrack.getGroupId());
        return doctorPigEvent;
    }

    @Override
    public DoctorPigTrack buildPigTrack(DoctorPigEvent executeEvent, DoctorPigTrack fromTrack) {
        DoctorPigTrack toTrack = super.buildPigTrack(executeEvent, fromTrack);
        DoctorChgLocationDto chgLocationDto = JSON_MAPPER.fromJson(executeEvent.getExtra(), DoctorChgLocationDto.class);
        Long toBarnId = chgLocationDto.getChgLocationToBarnId();

        //??????????????????????????????, ????????????????????????????????????
        DoctorBarn fromBarn = doctorBarnDao.findById(chgLocationDto.getChgLocationFromBarnId());
        expectTrue(notNull(fromBarn), "barn.not.null", fromTrack.getCurrentBarnId());
        DoctorBarn toBarn = doctorBarnDao.findById(toBarnId);
        expectTrue(notNull(toBarn), "barn.not.null", chgLocationDto.getChgLocationToBarnId());
        expectTrue(checkBarnTypeEqual(fromBarn.getPigType(), toTrack.getStatus(), toBarn.getPigType()), "not.trans.barn.type",
                PigType.from(fromBarn.getPigType()).getDesc(), PigType.from(toBarn.getPigType()).getDesc());
        //expectTrue(!(Objects.equals(doctorPigTrack.getStatus(), PigStatus.FEED.getKey()) && PigType.MATING_TYPES.contains(toBarn.getPigType())), "", new Object[]{chgLocationDto.getPigCode()});

        if (Objects.equals(fromBarn.getPigType(), PREG_SOW.getValue()) && Objects.equals(toBarn.getPigType(), PigType.DELIVER_SOW.getValue())) {
            toTrack.setStatus(PigStatus.Farrow.getKey());
        }
        toTrack.setCurrentBarnId(toBarnId);
        toTrack.setCurrentBarnName(toBarn.getName());
        toTrack.setCurrentBarnType(toBarn.getPigType());
        return toTrack;
    }

    @Override
    protected void triggerEvent(List<DoctorEventInfo> doctorEventInfoList, DoctorPigEvent executeEvent, DoctorPigTrack toTrack) {
        DoctorChgLocationDto chgLocationDto = JSON_MAPPER.fromJson(executeEvent.getExtra(), DoctorChgLocationDto.class);
        DoctorBarn fromBarn = doctorBarnDao.findById(chgLocationDto.getChgLocationFromBarnId());
        DoctorBarn toBarn = doctorBarnDao.findById(chgLocationDto.getChgLocationToBarnId());
        Map<String, Object> extraMap = toTrack.getExtraMap();

        // ????????????????????? 1 ??? 7 ???, ????????????????????????
        if(PigType.FARROW_TYPES.contains(fromBarn.getPigType())
                && PigType.FARROW_TYPES.contains(toBarn.getPigType())
                && Objects.equals(toTrack.getStatus(), PigStatus.FEED.getKey())
                && toTrack.getGroupId() != null){
            Long groupId = pigletTrans(doctorEventInfoList, executeEvent, toTrack, chgLocationDto, toBarn);

            toTrack.setExtraMap(extraMap);
            toTrack.setGroupId(groupId);  //????????????id
            doctorPigTrackDao.update(toTrack);
        }
    }

    @Override
    protected void updateDailyForNew(DoctorPigEvent newPigEvent) {
        BasePigEventInputDto inputDto = JSON_MAPPER.fromJson(newPigEvent.getExtra(), DoctorChgLocationDto.class);
        modifyPigChgLocationEventHandler.updateDailyOfNew(newPigEvent, inputDto);
    }

    //?????????????????????
    private Long pigletTrans(List<DoctorEventInfo> eventInfoList,DoctorPigEvent executeEvent, DoctorPigTrack pigTrack, DoctorChgLocationDto chgLocationDto, DoctorBarn doctorToBarn) {
        expectTrue(notNull(pigTrack.getGroupId()), "farrow.groupId.not.null", pigTrack.getPigId());
        //???????????????id
        DoctorTransGroupInput input = new DoctorTransGroupInput();
        input.setSowId(chgLocationDto.getPigId());
        input.setSowCode(chgLocationDto.getPigCode());
        input.setToBarnId(doctorToBarn.getId());
        input.setToBarnName(doctorToBarn.getName());
        List<DoctorGroup> groupList = doctorGroupDao.findByCurrentBarnId(doctorToBarn.getId());
        if (notEmpty(groupList)) {
            input.setIsCreateGroup(IsOrNot.NO.getValue());
            DoctorGroup toGroup = groupList.get(0);
            input.setToGroupId(toGroup.getId());
            input.setToGroupCode(toGroup.getGroupCode());
        } else {
            input.setIsCreateGroup(IsOrNot.YES.getValue());
            input.setToGroupCode(grateGroupCode(doctorToBarn.getName(), chgLocationDto.eventAt()));
        }

        DoctorGroup group = doctorGroupDao.findById(pigTrack.getGroupId());
        expectTrue(notNull(group), "group.not.null", pigTrack.getGroupId());
        DoctorGroupTrack groupTrack= doctorGroupTrackDao.findByGroupId(pigTrack.getGroupId());
        expectTrue(notNull(groupTrack), "farrow.group.track.not.null", pigTrack.getGroupId());
        input.setEventAt(DateUtil.toDateString(chgLocationDto.eventAt()));
        input.setIsAuto(IsOrNot.YES.getValue());
        input.setCreatorId(executeEvent.getOperatorId());
        input.setCreatorName(executeEvent.getOperatorName());
        input.setBreedId(group.getBreedId());
        input.setBreedName(group.getBreedName());
        input.setSource(PigSource.LOCAL.getKey());
        input.setSowEvent(true);    //??????????????????????????????

        //?????????????????? = ??? - ??????
        input.setQuantity(pigTrack.getUnweanQty());
        input.setBoarQty(0);
        input.setSowQty(input.getQuantity() - input.getBoarQty());
        input.setAvgWeight((MoreObjects.firstNonNull(pigTrack.getFarrowAvgWeight(), 0D)));
        input.setWeight(input.getAvgWeight() * input.getQuantity());
        input.setRelPigEventId(executeEvent.getId());

        transGroupEventHandler.handle(eventInfoList, group, groupTrack, input);
        if (Objects.equals(input.getIsCreateGroup(), IsOrNot.YES.getValue())) {
            //DoctorGroup toGroup = doctorGroupDao.findByFarmIdAndGroupCode(group.getFarmId(), input.getToGroupCode());
            return input.getToGroupId();
        }
        return input.getToGroupId();
    }

    /**
     * ????????????????????????
     * @param fromPigType ??????
     * @param toPigType ?????????
     * @param pigStatus ??????
     * @return ??????????????????
     */
    private Boolean checkBarnTypeEqual(Integer fromPigType, Integer pigStatus, Integer toPigType) {
        List<Integer> allows = DoctorEventSelector.selectBarn(PigStatus.from(pigStatus), PigType.from(fromPigType)).stream()
                .map(PigType::getValue)
                .collect(Collectors.toList());
        return allows.contains(toPigType);
    }
}
