package io.terminus.doctor.event.handler.group;

import com.google.common.base.MoreObjects;
import io.terminus.common.exception.ServiceException;
import io.terminus.doctor.common.enums.PigType;
import io.terminus.doctor.common.enums.SourceType;
import io.terminus.doctor.common.exception.InvalidException;
import io.terminus.doctor.common.utils.DateUtil;
import io.terminus.doctor.common.utils.JsonMapperUtil;
import io.terminus.doctor.common.utils.ToJsonMapper;
import io.terminus.doctor.event.dao.DoctorBarnDao;
import io.terminus.doctor.event.dao.DoctorGroupDao;
import io.terminus.doctor.event.dao.DoctorGroupEventDao;
import io.terminus.doctor.event.dao.DoctorGroupTrackDao;
import io.terminus.doctor.event.dao.DoctorTrackSnapshotDao;
import io.terminus.doctor.event.dto.event.DoctorEventInfo;
import io.terminus.doctor.event.dto.event.group.input.BaseGroupInput;
import io.terminus.doctor.event.dto.event.group.input.DoctorTransGroupInput;
import io.terminus.doctor.event.enums.EventStatus;
import io.terminus.doctor.event.enums.GroupEventType;
import io.terminus.doctor.event.enums.InType;
import io.terminus.doctor.event.enums.IsOrNot;
import io.terminus.doctor.event.event.DoctorGroupEventListener;
import io.terminus.doctor.event.event.DoctorGroupPublishDto;
import io.terminus.doctor.event.handler.DoctorGroupEventHandler;
import io.terminus.doctor.event.helper.DoctorConcurrentControl;
import io.terminus.doctor.event.helper.DoctorEventBaseHelper;
import io.terminus.doctor.event.model.DoctorBarn;
import io.terminus.doctor.event.model.DoctorEventModifyRequest;
import io.terminus.doctor.event.model.DoctorGroup;
import io.terminus.doctor.event.model.DoctorGroupEvent;
import io.terminus.doctor.event.model.DoctorGroupTrack;
import io.terminus.doctor.event.model.DoctorTrackSnapshot;
import io.terminus.doctor.event.util.EventUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import javax.validation.Valid;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import static io.terminus.common.utils.Arguments.*;
import static io.terminus.doctor.common.enums.PigType.*;
import static io.terminus.doctor.common.utils.Checks.expectTrue;

/**
 * Desc:
 * Mail: yangzl@terminus.io
 * author: DreamYoung
 * Date: 16/6/18
 */
@Slf4j
public abstract class DoctorAbstractGroupEventHandler implements DoctorGroupEventHandler {

    protected static final JsonMapperUtil JSON_MAPPER = JsonMapperUtil.JSON_NON_EMPTY_MAPPER;



    private final DoctorGroupTrackDao doctorGroupTrackDao;
    private final DoctorGroupEventDao doctorGroupEventDao;
    private final DoctorBarnDao doctorBarnDao;

    @Autowired
    private DoctorGroupDao doctorGroupDao;

    @Autowired
    private DoctorGroupEventListener doctorGroupEventListener;
    @Autowired
    private DoctorConcurrentControl doctorConcurrentControl;
    @Autowired
    private DoctorTrackSnapshotDao doctorTrackSnapshotDao;
    @Autowired
    private DoctorEventBaseHelper doctorEventBaseHelper;

    protected static final ToJsonMapper TO_JSON_MAPPER = ToJsonMapper.JSON_NON_EMPTY_MAPPER;


    @Autowired
    public DoctorAbstractGroupEventHandler(DoctorGroupTrackDao doctorGroupTrackDao,
                                           DoctorGroupEventDao doctorGroupEventDao,
                                           DoctorBarnDao doctorBarnDao) {
        this.doctorGroupTrackDao = doctorGroupTrackDao;
        this.doctorGroupEventDao = doctorGroupEventDao;
        this.doctorBarnDao = doctorBarnDao;
    }

    @Override
    public <I extends BaseGroupInput> DoctorGroupEvent buildGroupEvent(DoctorGroup group, DoctorGroupTrack groupTrack, @Valid I input) {
        return null;
    }

    @Override
    public <I extends BaseGroupInput> void handle(List<DoctorEventInfo> eventInfoList, DoctorGroup group, DoctorGroupTrack groupTrack, I input) {
        if (isNull(input.getEventSource()) || Objects.equals(input.getEventSource(), SourceType.INPUT.getValue())) {
            String key = "group" + group.getId().toString();
            expectTrue(doctorConcurrentControl.setKey(key),
                    "event.concurrent.error", group.getGroupCode());
        }
        handleEvent(eventInfoList, group, groupTrack, input);
        DoctorEventInfo eventInfo = DoctorEventInfo.builder()
                .businessId(group.getId())
                .businessType(DoctorEventInfo.Business_Type.GROUP.getValue())
                .eventAt(DateUtil.toDate(input.getEventAt()))
                .eventType(input.getEventType())
                .code(group.getGroupCode())
                .farmId(group.getFarmId())
                .orgId(group.getOrgId())
                .pigType(group.getPigType())
                .build();
        eventInfoList.add(eventInfo);
    }

    /**
     * ?????????????????????track snapshot
     * @param newEvent ????????????
     */
    protected void createTrackSnapshot(DoctorGroupEvent newEvent, DoctorGroupTrack currentTrack) {
        DoctorTrackSnapshot snapshot = DoctorTrackSnapshot.builder()
                .farmId(newEvent.getFarmId())
                .farmName(newEvent.getFarmName())
                .businessId(newEvent.getGroupId())
                .businessCode(newEvent.getGroupCode())
                .businessType(DoctorEventModifyRequest.TYPE.GROUP.getValue())
                .eventId(newEvent.getId())
                .eventSource(DoctorTrackSnapshot.EventSource.EVENT.getValue())
                .trackJson(TO_JSON_MAPPER.toJson(currentTrack))
                .build();
        doctorTrackSnapshotDao.create(snapshot);
    }

    @Override
    public DoctorGroupTrack elicitGroupTrack(DoctorGroupEvent event, DoctorGroupTrack track){
        updateAvgDayAge(event, track);
        track = updateTrackOtherInfo(event, track);
        track.setRelEventId(event.getId());
        return track;
    }


    /**
     * ??????????????????
     * @param newGroupEvent ?????????
     */
    protected void updateDailyForNew(DoctorGroupEvent newGroupEvent){}

    protected void updateAvgDayAge(DoctorGroupEvent event, DoctorGroupTrack track) {
        track.setAvgDayAge(DateUtil.getDeltaDaysAbs(event.getEventAt(), MoreObjects.firstNonNull(track.getBirthDate(), event.getEventAt())));
    }

    protected abstract DoctorGroupTrack updateTrackOtherInfo(DoctorGroupEvent event, DoctorGroupTrack track);

    /**
     * ???????????????????????????, ???????????????????????????
     * @param eventInfoList ?????????????????? ?????????????????????????????????
     * @param group       ??????
     * @param groupTrack  ????????????
     * @param input       ????????????
     * @param <I>         ??????????????????
     */
    protected abstract <I extends BaseGroupInput> void handleEvent(List<DoctorEventInfo> eventInfoList, DoctorGroup group, DoctorGroupTrack groupTrack, I input);

    //???????????????????????????
    protected DoctorGroupEvent dozerGroupEvent(DoctorGroup group, GroupEventType eventType, BaseGroupInput baseInput) {
        DoctorGroupEvent event = new DoctorGroupEvent();
        event.setEventAt(getEventAt(baseInput.getEventAt()));
        event.setOrgId(group.getOrgId());       //????????????
        event.setOrgName(group.getOrgName());
        event.setFarmId(group.getFarmId());     //????????????
        event.setFarmName(group.getFarmName());
        event.setGroupId(group.getId());        //????????????
        event.setGroupCode(group.getGroupCode());
        event.setType(eventType.getValue());    //????????????
        event.setName(eventType.getDesc());
        event.setBarnId(group.getCurrentBarnId());      //??????????????????
        event.setBarnName(group.getCurrentBarnName());
        event.setPigType(group.getPigType());           //??????
        event.setIsAuto(baseInput.getIsAuto());
        event.setCreatorId(baseInput.getCreatorId());   //?????????
        event.setCreatorName(baseInput.getCreatorName());
        event.setOperatorId(baseInput.getCreatorId());
        event.setOperatorName(baseInput.getCreatorName());
        event.setDesc(baseInput.generateEventDesc());
        event.setRemark(baseInput.getRemark());
        event.setRelGroupEventId(baseInput.getRelGroupEventId());
        event.setRelPigEventId(baseInput.getRelPigEventId());
        event.setStatus(EventStatus.VALID.getValue());
        event.setEventSource(notNull(baseInput.getEventSource()) ? baseInput.getEventSource()
                : SourceType.INPUT.getValue());
        return event;
    }

    //????????????, ???????????????
    private static Date getEventAt(String eventAt) {
        return eventAt.equals(DateUtil.toDateString(new Date())) ? new Date() : DateUtil.toDate(eventAt);
    }

    //??????????????????
    protected void updateGroupTrack(DoctorGroupTrack groupTrack, DoctorGroupEvent event) {
        groupTrack.setRelEventId(event.getId());    //?????????????????????id
        groupTrack.setUpdatorId(event.getCreatorId());
        groupTrack.setUpdatorName(event.getCreatorName());
        groupTrack.setSex(DoctorGroupTrack.Sex.MIX.getValue());

        //??????track
        doctorEventBaseHelper.validTrackAfterUpdate(groupTrack);

        //??????track
        doctorGroupTrackDao.update(groupTrack);

        //????????????track??????
        createTrackSnapshot(event, groupTrack);
    }

    //????????????
    protected static void checkQuantity(Integer max, Integer actual) {
        if (actual > max) {
            log.error("maxQty:{}, actualQty:{}", max, actual);
            throw new InvalidException("quantity.over.max", actual, max);
        }
    }

    /**
     * ??????????????????????????????????????????
     * @param weanQuantity
     * @param changeQuantity
     */
    protected static void checkWeanQuantity(Integer weanQuantity, Integer changeQuantity) {

        if (changeQuantity > weanQuantity) {
            throw new InvalidException("quantity.over.wean", changeQuantity, weanQuantity);
        }
    }

    //?????? ??? + ??? = ??????
    protected static void checkQuantityEqual(Integer all, Integer boar, Integer sow) {
        if (EventUtil.plusInt(boar, sow) > all) {
            log.error("allQty:{}, boarQty:{}, sowQty:{}", all, boar, sow);
            throw new InvalidException("boarQty.and.sowQty.over.allQty", all, boar + sow);
        }
    }

    private static DoctorGroupPublishDto getPublishGroup(DoctorGroupEvent event) {
        DoctorGroupPublishDto dto = new DoctorGroupPublishDto();
        dto.setGroupId(event.getGroupId());
        dto.setEventId(event.getId());
        dto.setEventAt(event.getEventAt());
        dto.setPigType(event.getPigType());
        return dto;
    }

    //????????????, ?????????????????????????????????, ???????????????????????????????????????????????????
    protected static void checkBreed(Long groupBreedId, Long breedId) {
        if (notNull(groupBreedId) && notNull(breedId) && !groupBreedId.equals(breedId)) {
            log.error("groupBreed:{}, inBreed:{}", groupBreedId, breedId);
            throw new InvalidException("breed.not.equal", groupBreedId, breedId);
        }
    }

    //????????????, ????????????????????????100???, ??????????????????
    protected void checkDayAge(Integer dayAge, DoctorTransGroupInput input) {
        if (!Objects.equals(input.getIsCreateGroup(), IsOrNot.YES.getValue())) {
            DoctorGroupTrack groupTrack = doctorGroupTrackDao.findByGroupId(input.getToGroupId());
            if (Math.abs(dayAge - groupTrack.getAvgDayAge()) > 100) {
                log.error("dayAge:{}, inDayAge:{}", dayAge, Math.abs(dayAge - groupTrack.getAvgDayAge()));
                throw new InvalidException("delta.dayAge.over.100", Math.abs(dayAge - groupTrack.getAvgDayAge()));
            }
        }
    }

    //??????(???????????????)????????????????????????
    protected void  checkFarrowGroupUnique(Integer isCreateGroup, Long barnId) {
        if (isCreateGroup.equals(IsOrNot.YES.getValue())) {
            DoctorBarn doctorBarn = doctorBarnDao.findById(barnId);
            Integer barnType = doctorBarn.getPigType();
            //??????????????????????????????
            if (barnType.equals(PigType.DELIVER_SOW.getValue())) {
                List<DoctorGroup> groups = doctorGroupDao.findByCurrentBarnId(barnId);
                if (notEmpty(groups)) {
                    throw new InvalidException("group.count.over.1", doctorBarn.getName());
                }
            }
        }
    }

    //????????????????????????(?????? => ??????(???????????????)/????????????????????? => ?????????/?????????/?????????????????????????????????)
    protected void checkCanTransBarn(Integer pigType, Long barnId) {
        Integer barnType = doctorBarnDao.findById(barnId).getPigType();

        //?????? => ??????(???????????????)/?????????
        if (Objects.equals(pigType, PigType.DELIVER_SOW.getValue())) {
            if (!FARROW_ALLOW_TRANS.contains(barnType)) {
                log.error("check can trans barn pigType:{}, barnType:{}", pigType, barnType);
                throw new InvalidException("farrow.can.not.trans", PigType.from(barnType).getDesc());
            }
            return;
        }
        //????????? => ?????????/?????????/?????????/?????????(??????)
        if (Objects.equals(pigType, PigType.NURSERY_PIGLET.getValue())) {
            if (!NURSERY_ALLOW_TRANS.contains(barnType)) {
                log.error("check can trans barn pigType:{}, barnType:{}", pigType, barnType);
                throw new InvalidException("nursery.can.not.trans", PigType.from(barnType).getDesc());
            }
            return;
        }
        //????????? => ?????????/?????????(??????)
        if (Objects.equals(pigType, PigType.FATTEN_PIG.getValue())) {
            if (!FATTEN_ALLOW_TRANS.contains(barnType)) {
                log.error("check can trans barn pigType:{}, barnType:{}", pigType, barnType);
                throw new InvalidException("fatten.can.not.trans", PigType.from(barnType).getDesc());
            }
            return;
        }
        // ????????? => ?????????/?????????
        if (Objects.equals(pigType, PigType.RESERVE.getValue())) {
            if(barnType != PigType.RESERVE.getValue() && barnType != PigType.FATTEN_PIG.getValue()){
                throw new InvalidException("reserve.can.not.trans", PigType.from(barnType).getDesc());
            }
            return;
        }

        //?????? => ?????????
        if(!Objects.equals(pigType, barnType)) {
            log.error("check can trans barn pigType:{}, barnType:{}", pigType, barnType);
            throw new InvalidException("no.equal.type.can.not.trans", PigType.from(barnType).getDesc());
        }
    }

    //???????????????????????????id???????????????????????????
    protected void checkCanTransGroup(Long toGroupId, Long toBarnId, Integer isCreate) {
        if (Objects.equals(isCreate, IsOrNot.YES.getValue())) {
            return;
        }
        if (toGroupId != null) {
            DoctorGroup toGroup = doctorGroupDao.findById(toGroupId);
            if (toGroup == null || !Objects.equals(toGroup.getCurrentBarnId(), toBarnId)) {
                log.error("check can trans group toGroupId:{}, toBarnId:{}", toGroupId, toBarnId);
                throw new ServiceException("group.toBarn.not.equal");
            }
        }
    }

    //????????????????????????
    protected static DoctorGroupEvent.TransGroupType getTransType(Integer inType, Integer pigType, DoctorBarn toBarn) {
        if (inType != null && !Objects.equals(inType, InType.GROUP.getValue())) {
            return DoctorGroupEvent.TransGroupType.OUT;
        }
        return Objects.equals(pigType, toBarn.getPigType()) || (FARROW_TYPES.contains(pigType) && FARROW_TYPES.contains(toBarn.getPigType())) ?
                DoctorGroupEvent.TransGroupType.IN : DoctorGroupEvent.TransGroupType.OUT;
    }

    //???????????? < 0  ??????0
    protected static double getDeltaWeight(Double weight) {
        return weight == null || weight < 0 ? 0 : weight;
    }

    protected DoctorBarn getBarnById(Long barnId) {
        return doctorBarnDao.findById(barnId);
    }

    //????????????0???????????????????????????????????????????????????????????????/?????????????????????
    protected void checkUnweanTrans(Integer pigType, Integer toType, DoctorGroupTrack groupTrack, Integer eventQty) {
        log.error("checkUnweanTrans:pigType="+pigType+",toType:"+toType+",eventQty:"+eventQty);
        if (!Objects.equals(pigType, PigType.DELIVER_SOW.getValue()) || Objects.equals(pigType, toType)) {
            return;
        }
        log.error("checkUnweanTrans:groupTrack="+groupTrack.toString());
        Integer unwean = MoreObjects.firstNonNull(groupTrack.getUnweanQty(), 0);
        if (eventQty > (groupTrack.getQuantity() - unwean)) {
            throw new InvalidException("group.has.unwean", eventQty, groupTrack.getQuantity() - unwean);
        }
    }
    //???????????????????????????????????????
    protected static int getGroupEventAge(int groupAge, int deltaDays) {
        int eventAge = groupAge - deltaDays;
        if (eventAge < 0) {
            //// TODO: 17/3/11 ?????????????????? 
            //throw new InvalidException("day.age.error");
            eventAge = 0;
        }
        return eventAge;
    }

    protected boolean checkFarrowFirstMoveIn(DoctorGroupEvent event){
        if(notNull(event.getRelGroupEventId())){
            DoctorGroupEvent newEvent = doctorGroupEventDao.findById(event.getRelGroupEventId());
            if(Objects.nonNull(newEvent) && Objects.equals(newEvent.getType(), GroupEventType.NEW.getValue()) && notNull(newEvent.getRelPigEventId())){
                return true;
            }
        }
        return false;
    }


    /**
     * ??????????????????
     * @param groupTrack
     * @param sowQtyIn
     * @return
     */
    protected Integer getSowQty(DoctorGroupTrack groupTrack, Integer sowQtyIn) {
        Integer sowQty = EventUtil.plusInt(groupTrack.getSowQty(), sowQtyIn);
        sowQty = sowQty > groupTrack.getQuantity() ? groupTrack.getQuantity() : sowQty;
        return sowQty < 0 ? 0 : sowQty;
    }

    /**
     * ??????????????????
     * @param groupTrack
     * @param boarQtyIn ????????????
     * @return
     */
    protected Integer getBoarQty(DoctorGroupTrack groupTrack, Integer boarQtyIn) {
        Integer boarQty = EventUtil.plusInt(groupTrack.getBoarQty(), boarQtyIn);
        boarQty = boarQty > groupTrack.getQuantity() ? groupTrack.getQuantity() : boarQty;
        return boarQty < 0 ? 0 : boarQty;
    }

}
