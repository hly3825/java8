package io.terminus.doctor.event.handler.group;

import io.terminus.common.utils.BeanMapper;
import io.terminus.doctor.common.enums.PigType;
import io.terminus.doctor.common.exception.InvalidException;
import io.terminus.doctor.common.utils.DateUtil;
import io.terminus.doctor.common.utils.RespHelper;
import io.terminus.doctor.event.dao.DoctorBarnDao;
import io.terminus.doctor.event.dao.DoctorGroupDao;
import io.terminus.doctor.event.dao.DoctorGroupEventDao;
import io.terminus.doctor.event.dao.DoctorGroupTrackDao;
import io.terminus.doctor.event.dao.DoctorPigTrackDao;
import io.terminus.doctor.event.dto.DoctorBasicInputInfoDto;
import io.terminus.doctor.event.dto.DoctorGroupDetail;
import io.terminus.doctor.event.dto.event.DoctorEventInfo;
import io.terminus.doctor.event.dto.event.group.input.BaseGroupInput;
import io.terminus.doctor.event.dto.event.group.input.DoctorCloseGroupInput;
import io.terminus.doctor.event.dto.event.group.input.DoctorMoveInGroupInput;
import io.terminus.doctor.event.dto.event.group.input.DoctorNewGroupInput;
import io.terminus.doctor.event.dto.event.group.input.DoctorSowMoveInGroupInput;
import io.terminus.doctor.event.dto.event.group.input.DoctorTransFarmGroupInput;
import io.terminus.doctor.event.dto.event.group.input.DoctorTransGroupInput;
import io.terminus.doctor.event.dto.event.group.input.DoctorTurnSeedGroupInput;
import io.terminus.doctor.event.dto.event.group.input.DoctorWeanGroupInput;
import io.terminus.doctor.event.dto.event.usual.DoctorFarmEntryDto;
import io.terminus.doctor.event.enums.BoarEntryType;
import io.terminus.doctor.event.enums.GroupEventType;
import io.terminus.doctor.event.enums.InType;
import io.terminus.doctor.event.enums.IsOrNot;
import io.terminus.doctor.event.enums.PigEvent;
import io.terminus.doctor.event.enums.PigSource;
import io.terminus.doctor.event.handler.usual.DoctorEntryHandler;
import io.terminus.doctor.event.manager.DoctorGroupManager;
import io.terminus.doctor.event.model.DoctorBarn;
import io.terminus.doctor.event.model.DoctorGroup;
import io.terminus.doctor.event.model.DoctorGroupBatchSummary;
import io.terminus.doctor.event.model.DoctorGroupEvent;
import io.terminus.doctor.event.model.DoctorGroupTrack;
import io.terminus.doctor.event.model.DoctorPig;
import io.terminus.doctor.event.model.DoctorPigEvent;
import io.terminus.doctor.event.model.DoctorPigTrack;
import io.terminus.doctor.event.service.DoctorGroupBatchSummaryReadService;
import io.terminus.doctor.event.service.DoctorGroupBatchSummaryWriteService;
import io.terminus.doctor.event.service.DoctorGroupReadService;
import io.terminus.doctor.event.util.EventUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.validation.Valid;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import static io.terminus.common.utils.Arguments.notEmpty;
import static io.terminus.doctor.common.utils.Checks.expectTrue;

/**
 * Desc: ?????????????????????
 * Mail: yangzl@terminus.io
 * author: DreamYoung
 * Date: 16/6/20
 */
@Slf4j
@Component
public class DoctorCommonGroupEventHandler {

    @Autowired
    private DoctorCloseGroupEventHandler doctorCloseGroupEventHandler;

    @Autowired
    private DoctorMoveInGroupEventHandler doctorMoveInGroupEventHandler;

    @Autowired
    private DoctorGroupReadService doctorGroupReadService;

    @Autowired
    private DoctorGroupManager doctorGroupManager;

    @Autowired
    private DoctorEntryHandler doctorEntryHandler;

    @Autowired
    private DoctorWeanGroupEventHandler doctorWeanGroupEventHandler;

    @Autowired
    private DoctorGroupBatchSummaryReadService doctorGroupBatchSummaryReadService;

    @Autowired
    private DoctorGroupBatchSummaryWriteService doctorGroupBatchSummaryWriteService;

    @Autowired
    private DoctorGroupDao doctorGroupDao;

    @Autowired
    private DoctorGroupTrackDao doctorGroupTrackDao;

    @Autowired
    private DoctorBarnDao doctorBarnDao;

    @Autowired
    private DoctorPigTrackDao doctorPigTrackDao;
    @Autowired
    private DoctorGroupEventDao doctorGroupEventDao;

    /**
     * ???????????????????????????????????????(???????????????????????????)
     */
    public void autoGroupEventClose(List<DoctorEventInfo> eventInfoList, DoctorGroup group, DoctorGroupTrack groupTrack, BaseGroupInput baseInput, Date eventAt, Double fcrFeed) {
        validCloseGroup(group, groupTrack);
        createGroupBatchSummaryWhenClosed(group, groupTrack, eventAt, fcrFeed);

        DoctorCloseGroupInput closeInput = new DoctorCloseGroupInput();
        DoctorGroupEvent lastEvent = doctorGroupEventDao.findLastEventByGroupId(group.getId());
        closeInput.setIsAuto(IsOrNot.NO.getValue());
        closeInput.setEventAt(DateUtil.toDateString(lastEvent.getEventAt()));
        doctorCloseGroupEventHandler.handle(eventInfoList, group, groupTrack, closeInput);
    }

    /**
     * ?????????????????????????????????????????????(????????????, ??????/????????????)
     */
    public void autoTransEventMoveIn(List<DoctorEventInfo> eventInfoList, DoctorGroup fromGroup, DoctorGroupTrack fromGroupTrack, DoctorTransGroupInput transGroup) {
        DoctorMoveInGroupInput moveIn = new DoctorMoveInGroupInput();
        moveIn.setSowId(transGroup.getSowId());
        moveIn.setSowCode(transGroup.getSowCode());
        moveIn.setEventAt(transGroup.getEventAt());
        moveIn.setEventType(GroupEventType.MOVE_IN.getValue());
        moveIn.setIsAuto(IsOrNot.YES.getValue());
        moveIn.setCreatorId(transGroup.getCreatorId());
        moveIn.setCreatorName(transGroup.getCreatorName());
        moveIn.setRelGroupEventId(transGroup.getRelGroupEventId());

        InType inType = transGroup instanceof DoctorTransFarmGroupInput ? InType.FARM : InType.GROUP;
        moveIn.setInType(inType.getValue());       //????????????
        moveIn.setInTypeName(inType.getDesc());
        moveIn.setSource(transGroup.getSource());                 //?????????????????? ??????(??????), ??????(??????)
        moveIn.setSex(fromGroupTrack.getSex());
        moveIn.setBreedId(transGroup.getBreedId());
        moveIn.setBreedName(transGroup.getBreedName());
        moveIn.setFromFarmId(fromGroup.getFarmId());
        moveIn.setFromFarmName(fromGroup.getFarmName());
        moveIn.setFromBarnId(fromGroup.getCurrentBarnId());         //????????????
        moveIn.setFromBarnName(fromGroup.getCurrentBarnName());
        moveIn.setFromGroupId(fromGroup.getId());                   //????????????
        moveIn.setFromGroupCode(fromGroup.getGroupCode());
        moveIn.setQuantity(transGroup.getQuantity());
        moveIn.setBoarQty(transGroup.getBoarQty());
        moveIn.setSowQty(transGroup.getSowQty());
        moveIn.setAvgDayAge(DateUtil.getDeltaDays(fromGroupTrack.getBirthDate(), DateUtil.toDate(transGroup.getEventAt())));     //??????
        moveIn.setAvgWeight(EventUtil.getAvgWeight(transGroup.getWeight(), transGroup.getQuantity()));  //????????????
        moveIn.setSowEvent(transGroup.isSowEvent());    //?????????????????????????????????

        //????????????????????????
        DoctorGroupDetail groupDetail = RespHelper.orServEx(doctorGroupReadService.findGroupDetailByGroupId(transGroup.getToGroupId()));
        doctorMoveInGroupEventHandler.handle(eventInfoList, groupDetail.getGroup(), groupDetail.getGroupTrack(), moveIn);
    }

    /**
     * ?????????????????????????????????????????????
     * @param input ????????????
     * @return ???????????????id
     */
    @Transactional
    public Long sowGroupEventMoveInWithNew(List<DoctorEventInfo> eventInfoList, DoctorSowMoveInGroupInput input) {
        input.setIsAuto(IsOrNot.YES.getValue());
        input.setRemark(notEmpty(input.getRemark()) ? input.getRemark() : "??????????????????????????????????????????????????????????????????");

        //1. ????????????????????????
        DoctorGroup group = BeanMapper.map(input, DoctorGroup.class);
        group.setRemark(null);  //dozer???????????????remark
        group.setStaffId(input.getCreatorId());
        group.setStaffName(input.getCreatorName());

        //2. ????????????????????????
        DoctorNewGroupInput newGroupInput = BeanMapper.map(input, DoctorNewGroupInput.class);
        newGroupInput.setBarnId(input.getToBarnId());
        newGroupInput.setBarnName(input.getToBarnName());


        //3. ??????????????????
        Long groupId = doctorGroupManager.createNewGroup(eventInfoList, group, newGroupInput);

        //4. ??????????????????
        DoctorGroupDetail groupDetail = RespHelper.orServEx(doctorGroupReadService.findGroupDetailByGroupId(groupId));

//        input.setRelPigEventId(null); //?????????????????? relPigEventId ?????????
//        input.setRelGroupEventId(groupDetail.getGroupTrack().getRelEventId());      //???????????????????????????id(??????????????????track.relEventId = ??????????????????id)
        input.setSowEvent(true);
        doctorMoveInGroupEventHandler.handle(eventInfoList, groupDetail.getGroup(), groupDetail.getGroupTrack(), input);
        return groupId;
    }

    /**
     * ?????????????????????????????????????????????(??????:??????????????????????????????, ???????????????????????????????????????????????????)
     * @param eventInfoList ?????????????????????
     * @param input ????????????
     * @return ???????????????id
     */
    public Long sowGroupEventMoveIn(List<DoctorEventInfo> eventInfoList, @Valid DoctorSowMoveInGroupInput input) {
        log.info("trigger group event start, DoctorSowMoveInGroupInput = {}", input);
        List<DoctorGroup> groups = doctorGroupDao.findByCurrentBarnId(input.getToBarnId());
        DoctorBarn doctorBarn = doctorBarnDao.findById(input.getToBarnId());
        //????????????, ??????
        if (!notEmpty(groups)) {
            return sowGroupEventMoveInWithNew(eventInfoList, input);
        }

        //????????????, ??????
        if (groups.size() > 1) {
            throw new InvalidException("group.count.over.1", doctorBarn.getName(), input.getGroupCode());
        }

        //????????????, ??????
        DoctorGroup group = groups.get(0);
        DoctorGroupTrack groupTrack = doctorGroupTrackDao.findByGroupId(group.getId());
        input.setEventType(GroupEventType.MOVE_IN.getValue());

        doctorMoveInGroupEventHandler.handle(eventInfoList, group, groupTrack, input);
        return group.getId();
    }

    /**
     * ??????????????????????????????????????????
     * @param sex    ????????????
     * @param input
     * @param group
     * @param barn
     */
    public void autoPigEntryEvent(List<DoctorEventInfo> eventInfoList, DoctorPig.PigSex sex, DoctorTurnSeedGroupInput input, DoctorGroup group, DoctorBarn barn) {
        DoctorBasicInputInfoDto basicDto = new DoctorBasicInputInfoDto();
        DoctorFarmEntryDto farmEntryDto = new DoctorFarmEntryDto();

        ///?????????????????????
        if (Objects.equals(sex, DoctorPig.PigSex.BOAR)) {
            farmEntryDto.setPigType(DoctorPig.PigSex.BOAR.getKey());
            farmEntryDto.setBoarType(BoarEntryType.HGZ.getKey());
            farmEntryDto.setBoarTypeName(BoarEntryType.HGZ.getCode());
        } else {
            farmEntryDto.setPigType(DoctorPig.PigSex.SOW.getKey());
            farmEntryDto.setParity(1);
            farmEntryDto.setEarCode(input.getEarCode());
        }

        //????????????
        farmEntryDto.setRelGroupEventId(input.getRelGroupEventId());
        farmEntryDto.setPigCode(input.getPigCode());
        farmEntryDto.setOrigin(input.getOrigin());
        farmEntryDto.setBarnId(barn.getId());
        farmEntryDto.setBarnName(barn.getName());
        farmEntryDto.setBarnType(barn.getPigType());
        basicDto.setFarmId(group.getFarmId());
        basicDto.setFarmName(group.getFarmName());
        basicDto.setOrgId(group.getOrgId());
        basicDto.setOrgName(group.getOrgName());
        farmEntryDto.setEventType(PigEvent.ENTRY.getKey());
        farmEntryDto.setEventName(PigEvent.ENTRY.getName());
        farmEntryDto.setEventDesc(PigEvent.ENTRY.getDesc());
        basicDto.setStaffId(input.getCreatorId());
        basicDto.setStaffName(input.getCreatorName());
        farmEntryDto.setIsAuto(IsOrNot.YES.getValue());

        //????????????
        //farmEntryDto.setPigType(basicDto.getPigType());
        farmEntryDto.setPigCode(input.getPigCode());
        farmEntryDto.setBirthday(DateUtil.toDate(input.getBirthDate()));
        farmEntryDto.setInFarmDate(DateUtil.toDate(input.getEventAt()));
        farmEntryDto.setBarnId(barn.getId());
        farmEntryDto.setBarnName(barn.getName());
        farmEntryDto.setSource(PigSource.LOCAL.getKey());
        farmEntryDto.setBreed(input.getBreedId());
        farmEntryDto.setBreedName(input.getBreedName());
        farmEntryDto.setBreedType(input.getGeneticId());
        farmEntryDto.setBreedTypeName(input.getGeneticName());
        farmEntryDto.setMotherCode(input.getMotherEarCode());
        farmEntryDto.setEarCode(input.getEarCode());
        farmEntryDto.setWeight(input.getWeight());
        farmEntryDto.setEventSource(input.getEventSource());

        DoctorPigEvent pigEvent = doctorEntryHandler.buildPigEvent(basicDto, farmEntryDto);
        doctorEntryHandler.handleCheck(pigEvent, null);
        doctorEntryHandler.handle(eventInfoList, pigEvent, null);
    }

    /**
     * ??????????????????, ????????????????????????(??????????????????????????????????????????)
     */
    private void createGroupBatchSummaryWhenClosed(DoctorGroup group, DoctorGroupTrack groupTrack, Date eventAt, Double fcrFeed) {
        DoctorGroupBatchSummary summary = RespHelper.orServEx(doctorGroupBatchSummaryReadService
                .getSummaryByGroupDetail(new DoctorGroupDetail(group, groupTrack), fcrFeed));

        //???????????????????????????????????????
        summary.setStatus(DoctorGroup.Status.CLOSED.getValue());
        summary.setCloseAt(eventAt);

        RespHelper.orServEx(doctorGroupBatchSummaryWriteService.createGroupBatchSummary(summary));
    }

    public void sowWeanGroupEvent(List<DoctorEventInfo> eventInfoList, DoctorWeanGroupInput input){

        DoctorGroupTrack doctorGroupTrack = doctorGroupTrackDao.findByGroupId(input.getGroupId());
        DoctorGroup doctorGroup = doctorGroupDao.findById(input.getGroupId());
        doctorWeanGroupEventHandler.handle(eventInfoList, doctorGroup, doctorGroupTrack, input);
    }

    private void validCloseGroup(DoctorGroup group, DoctorGroupTrack groupTrack) {

        expectTrue(groupTrack.getQuantity() == 0, "group.quantity.not.allow.close", group.getId());
        if (!Objects.equals(group.getPigType(), PigType.DELIVER_SOW.getValue())) {
            return;
        }

        if (Objects.equals(group.getPigType(), PigType.DELIVER_SOW.getValue())) {
            List<DoctorPigTrack> pigTrackList = doctorPigTrackDao.findFeedSowTrackByGroupId(group.getFarmId(), group.getId());
            expectTrue(pigTrackList.isEmpty(), "group.has.burusow.not.allow.close", group.getId());
        }
    }
}
