package io.terminus.doctor.event.service;

import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import io.terminus.common.exception.ServiceException;
import io.terminus.common.utils.Arguments;
import io.terminus.doctor.common.enums.SourceType;
import io.terminus.doctor.common.exception.InvalidException;
import io.terminus.doctor.common.utils.JsonMapperUtil;
import io.terminus.doctor.common.utils.ToJsonMapper;
import io.terminus.doctor.event.dao.DoctorBarnDao;
import io.terminus.doctor.event.dao.DoctorEventModifyLogDao;
import io.terminus.doctor.event.dao.DoctorEventModifyRequestDao;
import io.terminus.doctor.event.dao.DoctorGroupDao;
import io.terminus.doctor.event.dao.DoctorGroupEventDao;
import io.terminus.doctor.event.dao.DoctorPigDao;
import io.terminus.doctor.event.dao.DoctorPigElicitRecordDao;
import io.terminus.doctor.event.dao.DoctorPigEventDao;
import io.terminus.doctor.event.dao.DoctorPigTrackDao;
import io.terminus.doctor.event.dto.event.DoctorEventInfo;
import io.terminus.doctor.event.dto.event.usual.DoctorChgLocationDto;
import io.terminus.doctor.event.enums.PigEvent;
import io.terminus.doctor.event.enums.PigStatus;
import io.terminus.doctor.event.helper.DoctorMessageSourceHelper;
import io.terminus.doctor.event.manager.DoctorGroupEventManager;
import io.terminus.doctor.event.manager.DoctorPigEventManager;
import io.terminus.doctor.event.model.DoctorBarn;
import io.terminus.doctor.event.model.DoctorGroup;
import io.terminus.doctor.event.model.DoctorPig;
import io.terminus.doctor.event.model.DoctorPigElicitRecord;
import io.terminus.doctor.event.model.DoctorPigEvent;
import io.terminus.doctor.event.model.DoctorPigTrack;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static io.terminus.common.utils.Arguments.notNull;
import static io.terminus.doctor.common.utils.Checks.expectNotNull;
import static io.terminus.doctor.event.handler.DoctorAbstractEventHandler.IGNORE_EVENT;

/**
 * Created by xjn on 17/3/12.
 */
@Slf4j
@Service
public class DoctorEditPigEventServiceImpl implements DoctorEditPigEventService {
    @Autowired
    private DoctorPigEventManager doctorPigEventManager;
    @Autowired
    private DoctorGroupEventDao doctorGroupEventDao;
    @Autowired
    private DoctorEventModifyRequestDao eventModifyRequestDao;
    @Autowired
    private DoctorGroupEventManager doctorGroupEventManager;
    @Autowired
    private DoctorPigTrackDao doctorPigTrackDao;
    @Autowired
    private DoctorPigEventDao doctorPigEventDao;
    @Autowired
    private DoctorPigDao doctorPigDao;
    @Autowired
    private DoctorGroupDao doctorGroupDao;
    @Autowired
    private DoctorBarnDao doctorBarnDao;
    @Autowired
    private DoctorEditGroupEventService doctorEditGroupEventService;
    @Autowired
    private DoctorMessageSourceHelper messageSourceHelper;
    @Autowired
    private DoctorPigElicitRecordDao doctorPigElicitRecordDao;
    @Autowired
    private DoctorEventModifyLogDao doctorEventModifyLogDao;

    private static final JsonMapperUtil JSON_MAPPER = JsonMapperUtil.JSON_NON_DEFAULT_MAPPER;

    private static final List<Integer> NOT_MODIFY_EVENT = Lists.newArrayList(PigEvent.CHG_LOCATION.getKey(), PigEvent.CHG_FARM.getKey(), PigEvent.FOSTERS.getKey(), PigEvent.FOSTERS_BY.getKey());

    private static final List<Integer> TRIGGER_GROUP_EVENT = Lists.newArrayList(
            PigEvent.CHG_LOCATION.getKey(), PigEvent.CHG_FARM.getKey(),
            PigEvent.FOSTERS.getKey(), PigEvent.FOSTERS_BY.getKey(),
            PigEvent.FARROWING.getKey(), PigEvent.PIGLETS_CHG.getKey(), PigEvent.WEAN.getKey());

    @Override
    @Transactional
    public List<DoctorEventInfo> modifyPigEventHandle(DoctorPigEvent modifyEvent, Long modifyRequestId) {
        //modifyPigEventHandleImpl(modifyEvent);
        return null;//modifyPigEventHandleOneImpl(modifyEvent, modifyRequestId);
    }

    @Override
    @Transactional
    public void elicitPigTrack(Long pigId) {
        log.info("elicitPigTrack starting, pigId:{}", pigId);
        DoctorPigTrack pigTrack = doctorPigTrackDao.findByPigId(pigId);
        try {
            elicitPigTrackImpl(pigId);
        } catch (InvalidException e) {
            createElicitPigTrackRecord(pigId, pigTrack, pigTrack, DoctorPigElicitRecord.Status.FAIL.getKey(), messageSourceHelper.getMessage(e.getError(), e.getParams()));
            throw e;
        } catch (ServiceException e) {
            createElicitPigTrackRecord(pigId, pigTrack, pigTrack, DoctorPigElicitRecord.Status.FAIL.getKey(), e.getMessage());
            throw e;
        } catch (Exception e) {
            createElicitPigTrackRecord(pigId, pigTrack, pigTrack, DoctorPigElicitRecord.Status.FAIL.getKey(), Throwables.getStackTraceAsString(e));
            throw e;
        }
        log.info("elicitPigTrack ending");
    }

    /**
     * ????????????(????????????)
     * @param pigId ???id
     */
    private void elicitPigTrackImpl(Long pigId) {
        //???????????????
        List<DoctorPigEvent> pigEventList = doctorPigEventDao.queryAllEventsByPigIdForASC(pigId);
        if (pigEventList.isEmpty() || !Objects.equals(pigEventList.get(0).getType(), PigEvent.ENTRY.getKey())) {
            throw new InvalidException("elicit.pig.track.data.source.error", pigId);
        }
        pigEventList = pigEventList.stream().filter(doctorPigEvent -> !IGNORE_EVENT.contains(doctorPigEvent.getType())).collect(Collectors.toList());

        //1.?????????????????????
        DoctorPigTrack oldTrack = doctorPigTrackDao.findByPigId(pigId);
        expectNotNull(oldTrack, "pig.track.not.null", pigId);
        DoctorPigTrack fromTrack = elicitPigTrackFromStep(pigEventList, null, oldTrack.getId(), pigId);

        //2.??????
        createElicitPigTrackRecord(pigId, oldTrack, fromTrack, DoctorPigElicitRecord.Status.SUCCESS.getKey(), null);
    }

    /**
     * ?????????????????? ????????????track???????????????
     * @param pigEventList ?????????????????????
     * @param fromTrack ??????track
     * @return ???track
     */
    private DoctorPigTrack elicitPigTrackFromStep(List<DoctorPigEvent> pigEventList, DoctorPigTrack fromTrack, Long oldTrackId, Long pigId){
        if (Arguments.isNullOrEmpty(pigEventList)) {
            throw new InvalidException("elicit.pig.track.data.source.error", pigId);
        }
        //2.??????track???????????????
        Long lastEventId;
        for (DoctorPigEvent pigEvent : pigEventList) {
            try {
                lastEventId = notNull(fromTrack) ? fromTrack.getCurrentEventId() : 0L;
                //?????????????????????????????????
                if (Objects.equals(pigEvent.getType(), PigEvent.TO_FARROWING.getKey())
                        && Objects.equals(pigEvent.getEventSource(), SourceType.IMPORT.getValue())) {
                    fromTrack.setStatus(PigStatus.Farrow.getKey());
                    fromTrack.setCurrentEventId(pigEvent.getId());
                    fromTrack.setCurrentBarnId(pigEvent.getBarnId());
                    fromTrack.setCurrentBarnName(pigEvent.getBarnName());
                    DoctorBarn doctorBarn = doctorBarnDao.findById(fromTrack.getCurrentBarnId());
                    fromTrack.setCurrentBarnType(doctorBarn.getPigType());
                    fromTrack.setCurrentEventId(pigEvent.getId());
                } else {
                    fromTrack = doctorPigEventManager.buildPigTrack(pigEvent, fromTrack);
                    fromTrack.setId(oldTrackId);
                }

                //???????????????,???????????????????????????????????????id
                if(Objects.equals(fromTrack.getStatus(), PigStatus.FEED.getKey())
                        && Objects.equals(pigEvent.getType(), PigEvent.CHG_LOCATION.getKey())) {
                    DoctorChgLocationDto chgLocationDto = JSON_MAPPER.fromJson(pigEvent.getExtra(), DoctorChgLocationDto.class);
                    DoctorGroup group = doctorGroupDao.findByFarmIdAndBarnIdAndDate(pigEvent.getFarmId(), chgLocationDto.getChgLocationToBarnId(), pigEvent.getEventAt());
                    fromTrack.setGroupId(group.getId());
                }
                //TODO: 17/3/29 ????????????????????????????????????,???????????????

//                doctorPigEventManager.createPigSnapshot(fromTrack, pigEvent, lastEventId);
            } catch (InvalidException e) {
                throw new InvalidException(messageSourceHelper.getMessage(e.getError(), e.getParams()) + ", ??????id:" + pigEvent.getId());
            } catch (Exception e) {
                throw new ServiceException( "??????id:" + pigEvent.getId() + Throwables.getStackTraceAsString(e));
            }
        }

        //3.??????track
        expectNotNull(fromTrack, "elicit.pig.track.failed", pigId);
        fromTrack.setId(oldTrackId);
        doctorPigTrackDao.update(fromTrack);
        return fromTrack;
    }
    /**
     * ??????????????????
     * @param pigId ???id
     * @param fromTrack ???track
     * @param toTrack ?????????track
     * @param status ??????
     * @param errorReason ????????????
     */
    private void createElicitPigTrackRecord(Long pigId, DoctorPigTrack fromTrack, DoctorPigTrack toTrack, Integer status, String errorReason) {
        Integer version = doctorPigElicitRecordDao.findLastVersion(pigId);
        DoctorPig pig = doctorPigDao.findById(pigId);
        DoctorPigElicitRecord pigElicitRecord = DoctorPigElicitRecord
                .builder()
                .farmId(pig.getFarmId())
                .farmName(pig.getFarmName())
                .pigId(pig.getId())
                .pigCode(pig.getPigCode())
                .fromTrack(ToJsonMapper.JSON_NON_DEFAULT_MAPPER.toJson(fromTrack))
                .toTrack(ToJsonMapper.JSON_NON_DEFAULT_MAPPER.toJson(toTrack))
                .status(status)
                .errorReason(errorReason)
                .version(++version)
                .build();
        doctorPigElicitRecordDao.create(pigElicitRecord);
    }

//    /**
//     * ????????????????????????????????????
//     * @param modifyEvent ????????????
//     * @return ??????????????????
//     */
//    private List<DoctorEventInfo> modifyPigEventHandleOneImpl(DoctorPigEvent modifyEvent, Long modifyRequestId) {
//        log.info("modify pig event handle one impl starting, modifyEvent:{}", modifyEvent);
//        List<DoctorEventInfo> doctorEventInfoList = Lists.newArrayList();
//        //1.???????????????
//        expectTrue(canModify(modifyEvent), "event.not.allow.modify");
//        Long oldEventId = modifyEvent.getId();
//        DoctorPigEvent oldEvent = doctorPigEventDao.findEventById(oldEventId);
//
//        //2.???????????????track
//        DoctorPigTrack fromTrack = null;
//
//        //3.??????????????????????????????????????????????????????
//        List<DoctorPigEvent> followEventList = doctorPigEventDao.findFollowEvents(modifyEvent.getPigId(), oldEventId)
//                .stream().filter(doctorPigEvent -> !IGNORE_EVENT.contains(doctorPigEvent.getType())).collect(Collectors.toList());
//        followEventList.add(modifyEvent);
//
//        //4.????????????????????????????????????
////        //4.1????????????????????????
////        if (isEffectWeanEvent(modifyEvent, oldEventId)) {
////            List<DoctorPigEvent> weanEventList = followEventList.stream()
////                    .filter(pigEvent -> Objects.equals(pigEvent.getType(), PigEvent.WEAN.getKey()))
////                    .collect(Collectors.toList());
////            if (!weanEventList.isEmpty()) {
////                DoctorPigEvent weanEvent = weanEventList.get(0);
////
////                DoctorWeanDto weanDto = JSON_MAPPER.fromJson(weanEvent.getExtra(), DoctorWeanDto.class);
////                weanDto.setPartWeanPigletsCount(fromTrack.getUnweanQty());
////                weanEvent.setExtra(ToJsonMapper.JSON_NON_DEFAULT_MAPPER.toJson(weanDto));
////                weanEvent.setWeanCount(fromTrack.getUnweanQty());
////                weanEvent.setDesc(Joiner.on("#").withKeyValueSeparator("???").join(weanDto.descMap()));
////            }
////        }
//
//        doctorPigEventDao.update(modifyEvent);
//        if (Objects.equals(Dates.startOfDay(modifyEvent.getEventAt()), Dates.startOfDay(oldEvent.getEventAt()))) {
//            doctorEventInfoList.add(buildPigEventInfo(modifyEvent));
//        } else {
//            doctorEventInfoList.add(buildPigEventInfo(oldEvent));
//            doctorEventInfoList.add(buildPigEventInfo(modifyEvent));
//        }
//
//        //5.??????track
//        DoctorPigTrack oldTrack = doctorPigTrackDao.findByPigId(modifyEvent.getPigId());
//        elicitPigTrackFromStep(followEventList, fromTrack, oldTrack.getId(), modifyEvent.getPigId());
//
//        //6.????????????
//        DoctorEventModifyLog modifyLog = DoctorEventModifyLog.builder()
//                .modifyRequestId(modifyRequestId)
//                .businessId(modifyEvent.getPigId())
//                .businessCode(modifyEvent.getPigCode())
//                .farmId(modifyEvent.getFarmId())
//                .fromEvent(ToJsonMapper.JSON_NON_DEFAULT_MAPPER.toJson(oldEvent))
//                .toEvent(ToJsonMapper.JSON_NON_DEFAULT_MAPPER.toJson(modifyEvent))
//                .type(DoctorEventModifyRequest.TYPE.PIG.getValue())
//                .build();
//        doctorEventModifyLogDao.create(modifyLog);
//
//        //7.???????????????????????????????????????
//        if (TRIGGER_GROUP_EVENT.contains(modifyEvent.getType())) {
//            //????????????????????????
//            BaseGroupInput newGroupEventInput = doctorPigEventManager.getHandler(modifyEvent.getType()).buildTriggerGroupEventInput(modifyEvent);
//            //?????????????????????????????????
//            DoctorEventRelation eventRelation = doctorEventRelationDao.findGroupEventByPigOrigin(modifyEvent.getId());
//            expectNotNull(eventRelation, "find.event.relation.failed", modifyEvent.getId());
//            DoctorGroupEvent oldGroupModifyEvent = doctorGroupEventDao.findById(eventRelation.getTriggerGroupEventId());
//            expectNotNull(oldGroupModifyEvent, "find.rel.group.event.failed", modifyEvent.getId());
//            DoctorGroupEvent modifyGroupEvent = doctorGroupEventManager.buildGroupEvent(new DoctorGroupInputInfo(new DoctorGroupDetail(beforeGroupSnapShotInfo.getGroup(), beforeGroupSnapShotInfo.getGroupTrack()), newGroupEventInput), oldGroupModifyEvent.getType());
//            expectNotNull(modifyGroupEvent, "build.group.event.failed");
//            modifyGroupEvent.setId(oldGroupModifyEvent.getId());
//
//            //??????????????????
//            doctorEventInfoList.addAll(doctorEditGroupEventService.elicitDoctorGroupTrackRebuildOne(modifyGroupEvent, modifyRequestId));
//        }
//        log.info("modify pig event handle one impl ending");
//        return doctorEventInfoList;
//    }
//
//    /**
//     * ?????????????????????
//     * @param pigEvent ?????????
//     * @return ????????????
//     */
//    private DoctorEventInfo buildPigEventInfo(DoctorPigEvent pigEvent) {
//        return DoctorEventInfo.builder()
//                .orgId(pigEvent.getOrgId())
//                .farmId(pigEvent.getFarmId())
//                .businessId(pigEvent.getPigId())
//                .businessType(DoctorEventInfo.Business_Type.PIG.getValue())
//                .code(pigEvent.getPigCode())
//                .eventId(pigEvent.getId())
//                .eventAt(pigEvent.getEventAt())
//                .eventType(pigEvent.getType())
//                .kind(pigEvent.getKind())
//                .mateType(pigEvent.getDoctorMateType())
//                .pregCheckResult(pigEvent.getPregCheckResult())
//                .build();
//    }
//
//    /**
//     * ??????????????????????????????????????????
//     *
//     * @param modifyEvent ????????????
//     */
//    private void modifyPigEventHandleImpl(DoctorPigEvent modifyEvent, Long modifyRequestId) {
//        log.info("modifyPigEventHandleImpl starting, modifyEvent:{}", modifyEvent);
//
//        //??????????????????????????????
//        expectTrue(canModify(modifyEvent), "event.not.allow.modify");
//
//        List<DoctorEventInfo> doctorEventInfoList = Lists.newArrayList();
//        List<Long> pigOldEventIdList = Lists.newLinkedList();
//        DoctorPigTrack currentTrack = doctorPigTrackDao.findByPigId(modifyEvent.getPigId());
//        DoctorPig oldPig = doctorPigDao.findById(modifyEvent.getPigId());
//        Long oldEventId = modifyEvent.getId();
//        try {
//            //1.?????????????????????
//            modifyPigEventHandle(modifyEvent, doctorEventInfoList, pigOldEventIdList);
//
//            //2.?????????????????????????????????
//            if (TRIGGER_GROUP_EVENT.contains(modifyEvent.getType())) {
//                if (!Objects.equals(modifyEvent.getType(), PigEvent.WEAN.getKey()) && isEffectWeanEvent(modifyEvent, oldEventId)) {
//                    //?????????????????????
//                    List<DoctorEventInfo> weanEventInfoList = doctorEventInfoList.stream()
//                            .filter(doctorEventInfo -> Objects.equals(doctorEventInfo.getBusinessType(), DoctorEventInfo.Business_Type.PIG.getValue())
//                                    && Objects.equals(doctorEventInfo.getEventType(), PigEvent.WEAN.getKey()))
//                            .collect(Collectors.toList());
//                    if (!weanEventInfoList.isEmpty()) {
//
//                        //?????????????????????
//                        DoctorEventInfo weanEventInfo = weanEventInfoList.get(0);
//                        DoctorPigEvent weanEvent = doctorPigEventDao.findById(weanEventInfo.getEventId());
//                        expectNotNull(weanEvent, "pig.event.not.found", weanEventInfo.getEventId());
//
//
//                        //??????????????????????????????
//                        BaseGroupInput weanGroupInput = doctorPigEventManager.getHandler(PigEvent.WEAN.getKey()).buildTriggerGroupEventInput(weanEvent);
//                        expectNotNull(weanGroupInput, "get.group.wean.event.input.failed");
//
//
//                        //?????????????????????????????????
//                        DoctorEventRelation eventRelation = doctorEventRelationDao.findGroupEventByPigOrigin(weanEventInfo.getEventId());
//                        expectNotNull(eventRelation, "find.event.relation.failed", weanEventInfo.getEventId());
//                        DoctorGroupEvent oldGroupWeanEvent = doctorGroupEventDao.findById(eventRelation.getTriggerGroupEventId());
//                        expectNotNull(oldGroupWeanEvent, "find.rel.group.event.failed", weanEventInfo.getEventId());
//                        DoctorGroupSnapshot oldGroupWeanSnapshot = doctorGroupSnapshotDao.queryByEventId(oldGroupWeanEvent.getId());
//                        expectNotNull(oldGroupWeanSnapshot, "find.per.group.snapshot.failed", oldGroupWeanEvent.getId());
//                        DoctorGroupSnapShotInfo oldGroupWeanSnapshotInfo = JSON_MAPPER.fromJson(oldGroupWeanSnapshot.getToInfo(), DoctorGroupSnapShotInfo.class);
//
//                        //????????????????????????
//                        DoctorGroupEvent newGroupWeanEvent = doctorGroupEventManager.buildGroupEvent(new DoctorGroupInputInfo(new DoctorGroupDetail(oldGroupWeanSnapshotInfo.getGroup(), oldGroupWeanSnapshotInfo.getGroupTrack()), weanGroupInput), GroupEventType.WEAN.getValue());
//                        expectNotNull(newGroupWeanEvent, "build.group.event.failed");
//                        newGroupWeanEvent.setId(oldGroupWeanEvent.getId());
//
//                        //??????????????????
//                        doctorEditGroupEventService.elicitDoctorGroupTrackRebuildOne(newGroupWeanEvent, modifyRequestId);
//                    }
//                }
//
//                //????????????????????????
//                BaseGroupInput newGroupEventInput = doctorPigEventManager.getHandler(modifyEvent.getType()).buildTriggerGroupEventInput(modifyEvent);
//                //?????????????????????????????????
//                DoctorEventRelation eventRelation = doctorEventRelationDao.findGroupEventByPigOrigin(modifyEvent.getId());
//                expectNotNull(eventRelation, "find.event.relation.failed", modifyEvent.getId());
//                DoctorGroupEvent oldGroupModifyEvent = doctorGroupEventDao.findById(eventRelation.getTriggerGroupEventId());
//                expectNotNull(oldGroupModifyEvent, "find.rel.group.event.failed", modifyEvent.getId());
//                DoctorGroupSnapshot beforeGroupSnapshot = doctorGroupSnapshotDao.queryByEventId(oldGroupModifyEvent.getId());
//                expectNotNull(beforeGroupSnapshot, "find.per.group.snapshot.failed", oldGroupModifyEvent.getId());
//                DoctorGroupSnapShotInfo beforeGroupSnapShotInfo = JSON_MAPPER.fromJson(beforeGroupSnapshot.getToInfo(), DoctorGroupSnapShotInfo.class);
//                DoctorGroupEvent modifyGroupEvent = doctorGroupEventManager.buildGroupEvent(new DoctorGroupInputInfo(new DoctorGroupDetail(beforeGroupSnapShotInfo.getGroup(), beforeGroupSnapShotInfo.getGroupTrack()), newGroupEventInput), oldGroupModifyEvent.getType());
//                expectNotNull(modifyGroupEvent, "build.group.event.failed");
//                modifyGroupEvent.setId(oldGroupModifyEvent.getId());
//
//                //??????????????????
//                doctorEditGroupEventService.elicitDoctorGroupTrackRebuildOne(modifyGroupEvent, modifyRequestId);
//
//            }
//        } catch (Exception e) {
//            //Map<Integer, List<DoctorEventInfo>> businessTypeMap = doctorEventInfoList.stream().collect(Collectors.groupingBy(DoctorEventInfo::getBusinessType));
//
//            //?????????????????????
//            doctorPigEventManager.modifyPidEventRollback(doctorEventInfoList, pigOldEventIdList, currentTrack, oldPig);
//
//            log.info("modify pig event handle failed, cause by:{}", Throwables.getStackTraceAsString(e));
//            throw e;
//        }
//        doctorPigEventDao.updateEventsStatus(pigOldEventIdList, EventStatus.INVALID.getValue());
//        List<Long> pigCreateOldEventIdList =  doctorEventInfoList.stream().map(DoctorEventInfo::getOldEventId).collect(Collectors.toList());
//        doctorEventRelationDao.updatePigEventStatusUnderHandling(pigCreateOldEventIdList, DoctorEventRelation.Status.INVALID.getValue());
//        log.info("modifyPigEventHandleImpl ending");
//    }
//
//    /**
//     * ??????????????????????????????
//     *
//     * @param modifyEvent ????????????
//     * @return ????????????
//     */
//    private Boolean canModify(DoctorPigEvent modifyEvent) {
//        DoctorPigEvent oldEvent = doctorPigEventDao.findEventById(modifyEvent.getId());
//        return Objects.equals(modifyEvent.getKind(), DoctorEventModifyRequest.TYPE.PIG.getValue())
//                && !NOT_MODIFY_EVENT.contains(modifyEvent.getType())
//                && Objects.equals(oldEvent.getStatus(), EventStatus.VALID.getValue());
//    }
//
//    /**
//     * ???????????????????????????
//     *
//     * @param modifyEvent         ?????????????????????
//     * @param doctorEventInfoList ????????????
//     * @param oldEventIdList      ?????????id??????
//     */
//    private void modifyPigEventHandle(DoctorPigEvent modifyEvent, List<DoctorEventInfo> doctorEventInfoList, List<Long> oldEventIdList) {
//        modifyEvent.setIsModify(IsOrNot.YES.getValue());
//        modifyEvent.setStatus(EventStatus.VALID.getValue());
//        //?????????id
//        Long oldEventId = modifyEvent.getId();
//        oldEventIdList.add(oldEventId);
//
//        //??????????????????
//        List<DoctorPigEvent> followEventList = doctorPigEventDao.findFollowEvents(modifyEvent.getPigId(), oldEventId)
//                .stream().filter(doctorPigEvent -> !IGNORE_EVENT.contains(doctorPigEvent.getType())).collect(Collectors.toList());
//        //??????????????????????????????
//        oldEventIdList.addAll(followEventList.stream().map(DoctorPigEvent::getId).collect(Collectors.toList()));
//        doctorPigEventDao.updateEventsStatus(oldEventIdList, EventStatus.HANDLING.getValue());
//
//        //??????????????????track
//        DoctorPigSnapshot lastPigSnapshot;
//        if (!Objects.equals(modifyEvent.getType(), PigEvent.ENTRY.getKey())) {
//            lastPigSnapshot = doctorPigSnapshotDao.queryByEventId(oldEventId);
//        } else {
//            lastPigSnapshot = doctorPigSnapshotDao.findByToEventId(oldEventId);
//        }
//        expectNotNull(lastPigSnapshot, "find.per.pig.snapshot.failed", oldEventId);
//        DoctorPigTrack fromTrack = JSON_MAPPER.fromJson(lastPigSnapshot.getToPigInfo(), DoctorPigSnapShotInfo.class).getPigTrack();
//        if(Arguments.isNull(fromTrack.getCurrentEventId()) && Arguments.isNull(lastPigSnapshot.getFromEventId())){
//            log.error("find pig snapshot info failed, pigId: {}", modifyEvent.getPigId());
//            throw new InvalidException("pig.snapshot.info.broken", modifyEvent.getPigId());
//        }
//        fromTrack.setCurrentEventId(lastPigSnapshot.getToEventId());
//        expectNotNull(fromTrack, "find.pig.track.from.snapshot.failed", lastPigSnapshot.getId());
//        //???????????????????????????,?????????????????????????????????????????????
//        modifyEvent.setBarnId(fromTrack.getCurrentBarnId());
//        modifyEvent.setBarnName(fromTrack.getCurrentBarnName());
//
//        //?????????????????????
//        DoctorPigEventHandler handler = doctorPigEventManager.getHandler(modifyEvent.getType());
//        //????????????
//        handler.handleCheck(modifyEvent, fromTrack);
//        //??????????????????
//        //handler.handle(doctorEventInfoList, modifyEvent, fromTrack);
//        doctorPigEventManager.transactionalHandle(handler, doctorEventInfoList, modifyEvent, fromTrack);
//
//        if (followEventList.isEmpty()) {
//            return;
//        }
//        //??????????????????
//        followEventList.forEach(followEvent -> followPigEventHandle(doctorEventInfoList, followEvent));
//    }
//
//    /**
//     * ?????????????????????
//     *
//     * @param doctorEventInfoList ??????????????????
//     * @param executeEvent        ????????????
//     */
//    private void followPigEventHandle(List<DoctorEventInfo> doctorEventInfoList, DoctorPigEvent executeEvent) {
//        log.info("followPigEventHandle stating, executeEvent:{}", executeEvent);
//        //?????????????????????track
//        DoctorPigTrack fromTrack = doctorPigTrackDao.findByPigId(executeEvent.getPigId());
//        expectNotNull(fromTrack, "pig.track.not.null", executeEvent.getPigId());
//        expectTrue(!isFeedChgLocation(executeEvent, fromTrack), "follow.event.is.feed.chgLocation");
//
//        //??????????????????
//        executeEvent.setIsModify(IsOrNot.YES.getValue());
//        executeEvent.setStatus(EventStatus.VALID.getValue());
//        //???????????????????????????????????????????????????
//        if (Objects.equals(executeEvent.getType(), PigEvent.WEAN.getKey())) {
//            DoctorWeanDto weanDto = JSON_MAPPER.fromJson(executeEvent.getExtra(), DoctorWeanDto.class);
//            weanDto.setPartWeanPigletsCount(fromTrack.getUnweanQty());
//            executeEvent.setExtra(ToJsonMapper.JSON_NON_DEFAULT_MAPPER.toJson(weanDto));
//            executeEvent.setWeanCount(fromTrack.getUnweanQty());
//            executeEvent.setDesc(Joiner.on("#").withKeyValueSeparator("???").join(weanDto.descMap()));
//        }
//
//        //?????????????????????
//        DoctorPigEventHandler handler = doctorPigEventManager.getHandler(executeEvent.getType());
//        //????????????
//        handler.handleCheck(executeEvent, fromTrack);
//        //????????????
//        //handler.handle(doctorEventInfoList, executeEvent, fromTrack);
//        doctorPigEventManager.transactionalHandle(handler, doctorEventInfoList, executeEvent, fromTrack);
//    }
//
//    /**
//     * ???????????????????????????
//     * @param executeEvent ??????
//     * @param fromTrack ??????track
//     * @return ??????????????????
//     */
//    private boolean isFeedChgLocation(DoctorPigEvent executeEvent, DoctorPigTrack fromTrack) {
//        return Objects.equals(fromTrack.getStatus(), PigStatus.FEED.getKey())
//                && Objects.equals(executeEvent.getType(), PigEvent.CHG_LOCATION.getKey());
//    }
//
//    /**
//     * ???????????????????????????
//     *
//     * @param modifyEvent ??????????????????
//     * @param oldEventId  ?????????id
//     * @return ????????????
//     */
//    private boolean isEffectWeanEvent(DoctorPigEvent modifyEvent, Long oldEventId) {
//        DoctorPigEvent oldPigEvent = doctorPigEventDao.findById(oldEventId);
//        //1.????????????
//        if (Objects.equals(oldPigEvent.getType(), PigEvent.FARROWING.getKey())
//                && !Objects.equals(oldPigEvent.getLiveCount(), modifyEvent.getLiveCount())) {
//            return true;
//        }
//
//        //2.??????????????????
//        if (Objects.equals(oldPigEvent.getType(), PigEvent.PIGLETS_CHG.getKey())) {
//            DoctorPigletsChgDto oldPigletsChgDto = JSON_MAPPER.fromJson(oldPigEvent.getExtra(), DoctorPigletsChgDto.class);
//            DoctorPigletsChgDto newPigletsChgDto = JSON_MAPPER.fromJson(modifyEvent.getExtra(), DoctorPigletsChgDto.class);
//            return !Objects.equals(oldPigletsChgDto.getPigletsCount(), newPigletsChgDto.getPigletsCount());
//        }
//
//        //3.????????????
//        if (Objects.equals(oldPigEvent.getType(), PigEvent.FOSTERS.getKey())) {
//            DoctorFostersDto oldFostersDto = JSON_MAPPER.fromJson(oldPigEvent.getExtra(), DoctorFostersDto.class);
//            DoctorFostersDto newFostersDto = JSON_MAPPER.fromJson(modifyEvent.getExtra(), DoctorFostersDto.class);
//            return !Objects.equals(oldFostersDto.getFostersCount(), newFostersDto.getFostersCount());
//        }
//
//        //4.???????????????
//        if (Objects.equals(oldPigEvent.getType(), PigEvent.FOSTERS.getKey())) {
//            DoctorFosterByDto oldFostersByDto = JSON_MAPPER.fromJson(oldPigEvent.getExtra(), DoctorFosterByDto.class);
//            DoctorFosterByDto newFostersByDto = JSON_MAPPER.fromJson(modifyEvent.getExtra(), DoctorFosterByDto.class);
//            return !Objects.equals(oldFostersByDto.getFosterByCount(), newFostersByDto.getFosterByCount());
//        }
//        return false;
//    }
}
