package io.terminus.doctor.event.handler;

import com.google.common.base.MoreObjects;
import com.google.common.collect.Lists;
import io.terminus.common.utils.Dates;
import io.terminus.doctor.common.enums.SourceType;
import io.terminus.doctor.common.exception.InvalidException;
import io.terminus.doctor.common.utils.DateUtil;
import io.terminus.doctor.common.utils.JsonMapperUtil;
import io.terminus.doctor.common.utils.ToJsonMapper;
import io.terminus.doctor.event.dao.DoctorBarnDao;
import io.terminus.doctor.event.dao.DoctorPigDao;
import io.terminus.doctor.event.dao.DoctorPigEventDao;
import io.terminus.doctor.event.dao.DoctorPigTrackDao;
import io.terminus.doctor.event.dao.DoctorRevertLogDao;
import io.terminus.doctor.event.dao.DoctorTrackSnapshotDao;
import io.terminus.doctor.event.dto.DoctorBasicInputInfoDto;
import io.terminus.doctor.event.dto.event.BasePigEventInputDto;
import io.terminus.doctor.event.dto.event.DoctorEventInfo;
import io.terminus.doctor.event.dto.event.group.input.BaseGroupInput;
import io.terminus.doctor.event.enums.EventStatus;
import io.terminus.doctor.event.enums.IsOrNot;
import io.terminus.doctor.event.enums.PigEvent;
import io.terminus.doctor.event.helper.DoctorConcurrentControl;
import io.terminus.doctor.event.helper.DoctorEventBaseHelper;
import io.terminus.doctor.event.model.DoctorBarn;
import io.terminus.doctor.event.model.DoctorEventModifyRequest;
import io.terminus.doctor.event.model.DoctorPig;
import io.terminus.doctor.event.model.DoctorPigEvent;
import io.terminus.doctor.event.model.DoctorPigTrack;
import io.terminus.doctor.event.model.DoctorTrackSnapshot;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.List;
import java.util.Objects;

import static io.terminus.common.utils.Arguments.*;
import static io.terminus.doctor.common.utils.Checks.expectTrue;
import static io.terminus.doctor.event.dto.DoctorBasicInputInfoDto.generateEventDescFromExtra;

/**
 * Created by xjn.
 * Date:2017/1/3
 */
@Slf4j
public abstract class DoctorAbstractEventHandler implements DoctorPigEventHandler {

    @Autowired
    protected  DoctorPigDao doctorPigDao;
    @Autowired
    protected  DoctorPigEventDao doctorPigEventDao;
    @Autowired
    protected  DoctorPigTrackDao doctorPigTrackDao;
    @Autowired
    protected  DoctorRevertLogDao doctorRevertLogDao;
    @Autowired
    protected DoctorBarnDao doctorBarnDao;
    @Autowired
    protected DoctorConcurrentControl doctorConcurrentControl;
    @Autowired
    protected DoctorTrackSnapshotDao doctorTrackSnapshotDao;
    @Autowired
    protected DoctorEventBaseHelper doctorEventBaseHelper;

    protected static final JsonMapperUtil JSON_MAPPER = JsonMapperUtil.JSON_NON_EMPTY_MAPPER;
    protected static final ToJsonMapper TO_JSON_MAPPER = ToJsonMapper.JSON_NON_EMPTY_MAPPER;

    public static final List<Integer> IGNORE_EVENT = Lists.newArrayList(PigEvent.CONDITION.getKey(),
            PigEvent.VACCINATION.getKey(), PigEvent.DISEASE.getKey(), PigEvent.SEMEN.getKey());

    @Override
    public void handleCheck(DoctorPigEvent executeEvent, DoctorPigTrack fromTrack) {
        DoctorPigEvent lastEvent = doctorPigEventDao.getLastStatusEvent(executeEvent.getPigId());
        checkEventAt(executeEvent, lastEvent);
    }

    @Override
    public void handle(List<DoctorEventInfo> doctorEventInfoList, DoctorPigEvent executeEvent, DoctorPigTrack fromTrack) {
        if (isNull(executeEvent.getEventSource()) || Objects.equals(executeEvent.getEventSource(), SourceType.INPUT.getValue())) {
            String key = "pig" + executeEvent.getPigId().toString();
            expectTrue(doctorConcurrentControl.setKey(key), "event.concurrent.error", executeEvent.getPigCode());
        }

        //??????
        handleCheck(executeEvent, fromTrack);

        //???????????????
        Long currentEventId = fromTrack.getCurrentEventId();
        //?????????id,????????????
        Long oldEventId = executeEvent.getId();

        //2.????????????
        executeEvent.setPigStatusBefore(fromTrack.getStatus());
        doctorPigEventDao.create(executeEvent);

        //4???????????????????????????track???????????????
        DoctorPigTrack toTrack = buildPigTrack(executeEvent, fromTrack);
        if (!IGNORE_EVENT.contains(executeEvent.getType()) || executeEvent.getType() == PigEvent.CONDITION.getKey()) {

            //??????track
            doctorEventBaseHelper.validTrackAfterUpdate(toTrack);

            //2.??????track
            doctorPigTrackDao.update(toTrack);
        }

        //5.????????????
        specialHandle(executeEvent, toTrack);

        //6.???????????????????????????
        DoctorBarn doctorBarn = doctorBarnDao.findById(toTrack.getCurrentBarnId());
        DoctorEventInfo doctorEventInfo = DoctorEventInfo.builder()
                .orgId(executeEvent.getOrgId())
                .farmId(executeEvent.getFarmId())
                .eventId(executeEvent.getId())
                .oldEventId(oldEventId)
                .isAuto(executeEvent.getIsAuto())
                .eventAt(executeEvent.getEventAt())
                .kind(executeEvent.getKind())
                .mateType(executeEvent.getDoctorMateType())
                .pregCheckResult(executeEvent.getPregCheckResult())
                .businessId(executeEvent.getPigId())
                .code(executeEvent.getPigCode())
                .status(toTrack.getStatus())
                .preStatus(executeEvent.getPigStatusBefore())
                .businessType(DoctorEventInfo.Business_Type.PIG.getValue())
                .eventType(executeEvent.getType())
                .pigType(doctorBarn.getPigType())
                .build();
        doctorEventInfoList.add(doctorEventInfo);

        if (!IGNORE_EVENT.contains(executeEvent.getType()) || executeEvent.getType() == PigEvent.CONDITION.getKey()) {
            //?????????????????????track snapshot
            createTrackSnapshot(executeEvent);
        }

        if (isNull(executeEvent.getEventSource()) || Objects.equals(executeEvent.getEventSource(), SourceType.INPUT.getValue())) {
            //7.???????????????
            updateDailyForNew(executeEvent);
        }

        if (!Objects.equals(executeEvent.getEventSource(), SourceType.MOVE.getValue())) {
            //8.????????????
            triggerEvent(doctorEventInfoList, executeEvent, toTrack);
        }
    }

    @Override
    public BaseGroupInput buildTriggerGroupEventInput(DoctorPigEvent pigEvent) {
        return null;
    }

    /**
     * ????????????????????????
     * @param basic
     * @param inputDto
     * @return
     */
    @Override
    public DoctorPigEvent buildPigEvent(DoctorBasicInputInfoDto basic, BasePigEventInputDto inputDto) {
        DoctorPigEvent doctorPigEvent = DoctorPigEvent.builder()
                .orgId(basic.getOrgId()).orgName(basic.getOrgName())
                .farmId(basic.getFarmId()).farmName(basic.getFarmName())
                .pigId(inputDto.getPigId()).pigCode(inputDto.getPigCode())
                .eventAt(generateEventAt(inputDto.eventAt())).type(inputDto.getEventType())
                .barnId(inputDto.getBarnId()).barnName(inputDto.getBarnName()).barnType(inputDto.getBarnType())
                .kind(inputDto.getPigType()).relPigEventId(inputDto.getRelPigEventId()).relGroupEventId(inputDto.getRelGroupEventId())
                .name(inputDto.getEventName()).desc(generateEventDescFromExtra(inputDto))
                .operatorId(MoreObjects.firstNonNull(inputDto.getOperatorId(), basic.getStaffId()))
                .operatorName(StringUtils.hasText(inputDto.getOperatorName()) ? inputDto.getOperatorName() : basic.getStaffName())
                .creatorId(basic.getStaffId()).creatorName(basic.getStaffName())
                .isAuto(MoreObjects.firstNonNull(inputDto.getIsAuto(), IsOrNot.NO.getValue()))
                .status(EventStatus.VALID.getValue()).eventSource(inputDto.getEventSource()).isModify(IsOrNot.NO.getValue())
                .npd(0).dpnpd(0).pfnpd(0).plnpd(0).psnpd(0).pynpd(0).ptnpd(0).jpnpd(0)
                .build();
        doctorPigEvent.setRemark(inputDto.changeRemark());
        doctorPigEvent.setExtraMap(inputDto.toMap());
        if (!Objects.equals(inputDto.getEventType(), PigEvent.ENTRY.getKey())
                && Objects.equals(inputDto.getPigType(), DoctorPig.PigSex.SOW.getKey())) {
            doctorPigEvent.setParity(doctorEventBaseHelper.getCurrentParity(inputDto.getPigId()));
        }

        if (Objects.equals(doctorPigEvent.getKind(), DoctorPig.PigSex.BOAR.getKey())) {
            DoctorPig boarPig = doctorPigDao.findById(inputDto.getPigId());
            doctorPigEvent.setBoarType(boarPig.getBoarType());
        }
        return doctorPigEvent;
    }

    /**
     * ????????????????????????track
     * @param executeEvent ??????????????????
     * @param fromTrack ??????????????????track
     * @return ???????????????track
     */
    public DoctorPigTrack buildPigTrack(DoctorPigEvent executeEvent, DoctorPigTrack fromTrack) {
        fromTrack.setCurrentEventId(executeEvent.getId());
        return fromTrack;
    }


    /**
     * ??????????????????????????????????????????
     * @param executeEvent ??????????????????
     * @param toTrack ???????????????????????????track
     */
    protected void specialHandle(DoctorPigEvent executeEvent, DoctorPigTrack toTrack){
        executeEvent.setPigStatusAfter(toTrack.getStatus());
        doctorPigEventDao.update(executeEvent);
    }

    /**
     * ??????????????????
     * @param newPigEvent ?????????
     */
    protected void updateDailyForNew(DoctorPigEvent newPigEvent){}

    /**
     * ????????????, ??????????????????????????????????????????
     * @param doctorEventInfoList ??????????????????
     * @param executeEvent ??????????????????
     * @param toTrack ???????????????????????????track
     */
    protected void triggerEvent(List<DoctorEventInfo> doctorEventInfoList, DoctorPigEvent executeEvent, DoctorPigTrack toTrack) {

    }

    /**
     * ?????????????????????????????????(??????????????????????????????????????????)
     * @param fromInputDto ???????????????
     * @param toInputDto ?????????????????????
     * @param pigEvent ??????????????????
     */
    protected void buildAutoEventCommonInfo(BasePigEventInputDto fromInputDto, BasePigEventInputDto toInputDto, PigEvent pigEvent, Long fromEventId) {
        toInputDto.setIsAuto(IsOrNot.YES.getValue());
        toInputDto.setPigId(fromInputDto.getPigId());
        toInputDto.setPigCode(fromInputDto.getPigCode());
        toInputDto.setPigType(fromInputDto.getPigType());
        toInputDto.setBarnId(fromInputDto.getBarnId());
        toInputDto.setBarnName(fromInputDto.getBarnName());
        toInputDto.setBarnType(fromInputDto.getBarnType());
        toInputDto.setRelPigEventId(fromEventId);
        toInputDto.setEventName(pigEvent.getName());
        toInputDto.setEventType(pigEvent.getKey());
        toInputDto.setEventDesc(pigEvent.getDesc());
        toInputDto.setEventSource(fromInputDto.getEventSource());
    }

    /**
     * ?????????????????????track snapshot
     * @param newEvent ????????????
     */
    protected void createTrackSnapshot(DoctorPigEvent newEvent) {
        DoctorPigTrack currentTrack = doctorPigTrackDao.findByPigId(newEvent.getPigId());
        DoctorTrackSnapshot snapshot = DoctorTrackSnapshot.builder()
                .farmId(newEvent.getFarmId())
                .farmName(newEvent.getFarmName())
                .businessId(newEvent.getPigId())
                .businessCode(newEvent.getPigCode())
                .businessType(DoctorEventModifyRequest.TYPE.PIG.getValue())
                .eventId(newEvent.getId())
                .eventSource(DoctorTrackSnapshot.EventSource.EVENT.getValue())
                .trackJson(TO_JSON_MAPPER.toJson(currentTrack))
                .build();
        doctorTrackSnapshotDao.create(snapshot);
    }

    protected Date generateEventAt(Date eventAt){
        if(eventAt != null){
            Date now = new Date();
            if(DateUtil.inSameDate(eventAt, now)){
                // ??????????????????, ?????????????????????
                return now;
            } else {
                // ??????????????????, ??????????????????0, ???????????????
                return Dates.startOfDay(eventAt);
            }
        }
        return null;
    }

    /**
     * ??????????????????
     * @param executeEvent ?????????????????????
     */
    private void checkEventAt(DoctorPigEvent executeEvent, DoctorPigEvent lastEvent) {
        Date eventAt = executeEvent.getEventAt();
        if (Objects.equals(executeEvent.getType(), PigEvent.ENTRY.getKey())) {
            if (Dates.startOfDay(eventAt).after(Dates.startOfDay(new Date()))){
                throw new InvalidException("entry.event.at.after.now", DateUtil.toDateString(eventAt), DateUtil.toDateString(new Date()));
            }
            return;
        }
        if (notNull(lastEvent) && (Dates.startOfDay(eventAt).before(Dates.startOfDay(lastEvent.getEventAt())) || Dates.startOfDay(eventAt).after(Dates.startOfDay(new Date())))) {
            throw new InvalidException("event.at.range.error", DateUtil.toDateString(lastEvent.getEventAt()), DateUtil.toDateString(new Date()), DateUtil.toDateString(eventAt));
        }
    }

    /**
     * ????????????????????????????????????
     * @param barnName ?????????
     * @return ?????????
     */
    public static String grateGroupCode(String barnName, Date eventAt) {
        expectTrue(notEmpty(barnName), "generate.code.barn.name.not.null");
        return barnName + "(" +DateUtil.toDateString(eventAt) + ")";
    }
}
