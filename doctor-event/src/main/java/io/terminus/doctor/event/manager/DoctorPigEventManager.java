package io.terminus.doctor.event.manager;

import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import io.terminus.common.exception.ServiceException;
import io.terminus.common.utils.Arguments;
import io.terminus.doctor.common.enums.PigType;
import io.terminus.doctor.common.event.CoreEventDispatcher;
import io.terminus.doctor.common.exception.InvalidException;
import io.terminus.doctor.common.utils.Checks;
import io.terminus.doctor.common.utils.ToJsonMapper;
import io.terminus.doctor.event.dao.DoctorEventModifyLogDao;
import io.terminus.doctor.event.dao.DoctorGroupEventDao;
import io.terminus.doctor.event.dao.DoctorPigDao;
import io.terminus.doctor.event.dao.DoctorPigEventDao;
import io.terminus.doctor.event.dao.DoctorPigTrackDao;
import io.terminus.doctor.event.dto.DoctorBasicInputInfoDto;
import io.terminus.doctor.event.dto.DoctorSuggestPigSearch;
import io.terminus.doctor.event.dto.event.BasePigEventInputDto;
import io.terminus.doctor.event.dto.event.DoctorEventInfo;
import io.terminus.doctor.event.editHandler.DoctorModifyPigEventHandler;
import io.terminus.doctor.event.editHandler.pig.DoctorModifyPigEventHandlers;
import io.terminus.doctor.event.enums.EventStatus;
import io.terminus.doctor.event.enums.GroupEventType;
import io.terminus.doctor.event.enums.PigEvent;
import io.terminus.doctor.event.enums.PigStatus;
import io.terminus.doctor.event.event.MsgGroupPublishDto;
import io.terminus.doctor.event.event.MsgListenedGroupEvent;
import io.terminus.doctor.event.event.MsgListenedPigEvent;
import io.terminus.doctor.event.event.MsgPigPublishDto;
import io.terminus.doctor.event.handler.DoctorEventSelector;
import io.terminus.doctor.event.handler.DoctorPigEventHandler;
import io.terminus.doctor.event.handler.DoctorPigEventHandlers;
import io.terminus.doctor.event.handler.DoctorPigsByEventSelector;
import io.terminus.doctor.event.handler.admin.SmartPigEventHandler;
import io.terminus.doctor.event.helper.DoctorConcurrentControl;
import io.terminus.doctor.event.helper.DoctorEventBaseHelper;
import io.terminus.doctor.event.model.DoctorEventModifyLog;
import io.terminus.doctor.event.model.DoctorEventModifyRequest;
import io.terminus.doctor.event.model.DoctorGroupEvent;
import io.terminus.doctor.event.model.DoctorPig;
import io.terminus.doctor.event.model.DoctorPigEvent;
import io.terminus.doctor.event.model.DoctorPigTrack;
import io.terminus.zookeeper.pubsub.Publisher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static io.terminus.common.utils.Arguments.notEmpty;

/**
 * Created by xjn.
 * Date:2017-01-19
 * ???????????????
 */
@Component
@Slf4j
public class DoctorPigEventManager {

    @Autowired
    private DoctorPigEventHandlers pigEventHandlers;
    @Autowired
    private DoctorPigTrackDao doctorPigTrackDao;
    @Autowired
    private DoctorPigEventDao doctorPigEventDao;
    @Autowired
    private DoctorPigDao doctorPigDao;
    @Autowired
    private DoctorModifyPigEventHandlers modifyPigEventHandlers;
    @Autowired
    private DoctorConcurrentControl doctorConcurrentControl;

    @Autowired
    private DoctorEventModifyLogDao doctorEventModifyLogDao;
    @Autowired
    private DoctorGroupEventDao doctorGroupEventDao;
    @Autowired
    private SmartPigEventHandler pigEventHandler;
    @Autowired
    private DoctorEventBaseHelper doctorEventBaseHelper;

    /**
     * ????????????
     *
     * @param inputDto ??????????????????
     * @param basic    ????????????
     */
    @Transactional
    public List<DoctorEventInfo> eventHandle(BasePigEventInputDto inputDto, DoctorBasicInputInfoDto basic) {
        log.info("pig event handle starting, pigCode:{}, farmId:{}", inputDto.getPigCode(), basic.getFarmId());
        final List<DoctorEventInfo> doctorEventInfoList = Lists.newArrayList();
        DoctorPigEventHandler handler = pigEventHandlers.getEventHandlerMap().get(inputDto.getEventType());
        //???????????????????????????
        DoctorPigEvent executeEvent = handler.buildPigEvent(basic, inputDto);
        //????????????????????????
        DoctorPigTrack fromTrack = doctorPigTrackDao.findByPigId(inputDto.getPigId());

        // TODO: 18/3/12 ?????????handle???????????????
//        //????????????
//        handler.handleCheck(executeEvent, fromTrack);
        //????????????
        handler.handle(doctorEventInfoList, executeEvent, fromTrack);
        log.info("pig event handle ending,  pigCode:{}, farmId:{}", inputDto.getPigCode(), basic.getFarmId());
        return doctorEventInfoList;
    }

    /**
     * ??????????????????
     *
     * @param basic    ????????????
     * @param inputDto ????????????
     * @return ????????????
     */
    public DoctorPigEvent buildPigEvent(DoctorBasicInputInfoDto basic, BasePigEventInputDto inputDto) {
        DoctorPigEventHandler handler = getHandler(inputDto.getEventType());
        return handler.buildPigEvent(basic, inputDto);
    }

    /**
     * ?????????????????????track
     *
     * @param executeEvent ??????????????????
     * @param fromTrack    ?????????track
     * @return ???????????????track
     */
    public DoctorPigTrack buildPigTrack(DoctorPigEvent executeEvent, DoctorPigTrack fromTrack) {
        DoctorPigEventHandler handler = getHandler(executeEvent.getType());
        return handler.buildPigTrack(executeEvent, fromTrack);
    }

    @Transactional
    public void transactionalHandle(DoctorPigEventHandler handler, List<DoctorEventInfo> doctorEventInfoList, DoctorPigEvent executeEvent, DoctorPigTrack fromTrack) {
        handler.handle(doctorEventInfoList, executeEvent, fromTrack);
    }

    /**
     * ??????????????????????????????
     *
     * @param doctorEventInfoList ???????????????
     * @param pigOldEventIdList   ?????????id???
     * @param fromTrack           ??????????????????track
     */
    @Transactional
    public void modifyPidEventRollback(List<DoctorEventInfo> doctorEventInfoList, List<Long> pigOldEventIdList, DoctorPigTrack fromTrack, DoctorPig oldPig) {
        log.info("rollback.modify.failed, starting");
        log.info("doctorEventInfoList:{}, pigOldEventIdList:{}, fromTrack:{}, oldPig:{}", doctorEventInfoList, pigOldEventIdList, fromTrack, oldPig);
        //1.??????????????????????????????
        List<Long> pigNewEventIdList = doctorEventInfoList.stream()
                .filter(doctorEventInfo -> Objects.equals(doctorEventInfo.getBusinessType(), DoctorEventInfo.Business_Type.PIG.getValue()))
                .map(DoctorEventInfo::getEventId).collect(Collectors.toList());
        if (!Arguments.isNullOrEmpty(pigNewEventIdList)) {
            doctorPigEventDao.updateEventsStatus(pigNewEventIdList, EventStatus.INVALID.getValue());
        }
        //2.????????????????????????
        if (!Arguments.isNullOrEmpty(pigOldEventIdList)) {
            doctorPigEventDao.updateEventsStatus(pigOldEventIdList, EventStatus.VALID.getValue());
        }
        //3.??????track
        doctorPigTrackDao.update(fromTrack);
        //4.?????????
        doctorPigDao.update(oldPig);
        log.info("rollback.modify.failed, ending");
    }

    /**
     * ??????????????????
     *
     * @param eventInputs
     * @param basic
     * @return
     */
    @Transactional
    public List<DoctorEventInfo> batchEventsHandle(List<BasePigEventInputDto> eventInputs, DoctorBasicInputInfoDto basic) {
        log.info("batch pig event handle starting, event type:{}", eventInputs.get(0).getEventType());
        //??????????????????????????????
        eventRepeatCheck(eventInputs);

        DoctorPigEventHandler handler = getHandler(eventInputs.get(0).getEventType());
        final List<DoctorEventInfo> eventInfos = Lists.newArrayList();
        eventInputs.forEach(inputDto -> {
            try {
                //???????????????????????????
                DoctorPigEvent executeEvent = handler.buildPigEvent(basic, inputDto);
                //????????????????????????
                DoctorPigTrack fromTrack = doctorPigTrackDao.findByPigId(inputDto.getPigId());

                // TODO: 18/3/12 ?????????handle???????????????
//                //????????????
//                handler.handleCheck(executeEvent, fromTrack);
                //????????????
                handler.handle(eventInfos, executeEvent, fromTrack);
            } catch (InvalidException e) {
                throw new InvalidException(true, e.getError(), inputDto.getPigCode(), e.getParams());
            }
        });
        log.info("batch pig event handle ending, event type:{}", eventInputs.get(0).getEventType());
        return eventInfos;
    }

    /**
     * ?????????????????????
     *
     * @param inputDto  ????????????
     * @param eventId   ??????id
     * @param eventType ????????????
     */
    @Transactional
    public void modifyPigEventHandle(BasePigEventInputDto inputDto, Long eventId, Integer eventType) {
        DoctorPigEvent oldPigEvent = doctorPigEventDao.findById(eventId);
        DoctorModifyPigEventHandler handler = modifyPigEventHandlers.getModifyPigEventHandlerMap().get(eventType);
        if (!handler.canModify(oldPigEvent)) {
            throw new InvalidException("event.not.modify");
        }
        handler.modifyHandle(oldPigEvent, inputDto);

    }

    /**
     * ???????????????????????????????????????
     *
     * @param eventType ????????????
     * @return ???????????????
     */
    public DoctorPigEventHandler getHandler(Integer eventType) {
        return Checks.expectNotNull(pigEventHandlers.getEventHandlerMap().get(eventType), "get.pig.handler.failed", eventType);

    }

    /**
     * ??????????????????????????????????????????
     */
    public static void checkAndPublishEvent(List<DoctorEventInfo> dtos, CoreEventDispatcher coreEventDispatcher, Publisher publisher) {
        try {
            if (notEmpty(dtos)) {
                //checkFarmIdAndEventAt(dtos);
                publishPigEvent(dtos, coreEventDispatcher, publisher);
            }
        } catch (Exception e) {
            log.error("publish event failed, dtos:{}, cause: {}", dtos, Throwables.getStackTraceAsString(e));
        }
    }

    //????????????, ????????????????????????
    private static void publishPigEvent(List<DoctorEventInfo> eventInfoList, CoreEventDispatcher coreEventDispatcher, Publisher publisher) {

        if (Arguments.isNullOrEmpty(eventInfoList)) {
            return;
        }
        Long orgId = eventInfoList.get(0).getOrgId();
        Long farmId = eventInfoList.get(0).getFarmId();

        Map<Integer, List<DoctorEventInfo>> eventInfoMap = eventInfoList.stream()
                .collect(Collectors.groupingBy(DoctorEventInfo::getBusinessType));

        //1.???????????????
        List<DoctorEventInfo> pigEventList = eventInfoMap.get(DoctorEventInfo.Business_Type.PIG.getValue());
        if (!Arguments.isNullOrEmpty(pigEventList)) {
            publishPigEvent(pigEventList, orgId, farmId, coreEventDispatcher, publisher);
        }

        //2.??????????????????
        List<DoctorEventInfo> groupEventList = eventInfoMap.get(DoctorEventInfo.Business_Type.GROUP.getValue());
        if (!Arguments.isNullOrEmpty(groupEventList)) {
            publishGroupEvent(groupEventList, orgId, farmId, coreEventDispatcher, publisher);
        }

    }

    /**
     * ???????????????
     *
     * @param eventInfoList ???????????????
     * @param orgId         ??????id
     * @param farmId        ??????id
     */
    private static void publishPigEvent(List<DoctorEventInfo> eventInfoList, Long orgId, Long farmId, CoreEventDispatcher coreEventDispatcher, Publisher publisher) {
        log.info("publish pig event starting");

        //???????????????????????????(zk)
        try {
            List<MsgPigPublishDto> msgPigPublishDtoList = eventInfoList.stream()
                    .filter(doctorEventInfo -> PigEvent.NOTICE_MESSAGE_PIG_EVENT.contains(doctorEventInfo.getEventType()))
                    .map(doctorEventInfo -> {

                        MsgPigPublishDto msgPigPublishDto = MsgPigPublishDto.builder()
                                .pigId(doctorEventInfo.getBusinessId())
                                .eventAt(doctorEventInfo.getEventAt())
                                .eventId(doctorEventInfo.getEventId())
                                .eventType(doctorEventInfo.getEventType())
                                .build();
                        if (Objects.equals(doctorEventInfo.getPreStatus(), PigStatus.Pregnancy.getKey())
                                && !Objects.equals(doctorEventInfo.getStatus(), PigStatus.Pregnancy.getKey())) {
                            msgPigPublishDto.setEventType(PigEvent.TO_FARROWING.getKey());
                        }
                        return msgPigPublishDto;
                    }).collect(Collectors.toList());
            if (!Arguments.isNullOrEmpty(msgPigPublishDtoList)) {
                coreEventDispatcher.publish(new MsgListenedPigEvent(orgId, farmId, msgPigPublishDtoList));
            }
        } catch (Exception e) {
            log.error("publish.pig.event.fail");
        }
    }

    /**
     * ??????????????????
     *
     * @param eventInfoList ??????????????????
     * @param orgId         ??????id
     * @param farmId        ??????id
     */
    private static void publishGroupEvent(List<DoctorEventInfo> eventInfoList, Long orgId, Long farmId, CoreEventDispatcher coreEventDispatcher, Publisher publisher) {
        log.info("publish group event starting");

        //?????????????????????????????????(zk)
        try {
            List<MsgGroupPublishDto> msgGroupPublishDtoList = eventInfoList.stream()
                    .filter(doctorEventInfo -> GroupEventType.NOTICE_MESSAGE_GROUP_EVENT.contains(doctorEventInfo.getEventType()))
                    .map(doctorEventInfo -> {
                        return MsgGroupPublishDto.builder()
                                .groupId(doctorEventInfo.getBusinessId())
                                .eventAt(doctorEventInfo.getEventAt())
                                .eventId(doctorEventInfo.getEventId())
                                .eventType(doctorEventInfo.getEventType())
                                .build();
                    }).collect(Collectors.toList());
            if (!Arguments.isNullOrEmpty(msgGroupPublishDtoList)) {
                coreEventDispatcher.publish(new MsgListenedGroupEvent(orgId, farmId, msgGroupPublishDtoList));
            }
        } catch (Exception e) {
            log.error("publish.pig.event.fail");
        }
    }

    private static void checkFarmIdAndEventAt(List<DoctorEventInfo> dtos) {
        dtos.forEach(dto -> {
            if (dto.getFarmId() == null || dto.getEventAt() == null) {
                throw new ServiceException("publish.create.event.not.null");
            }
        });
    }

    /**
     * ????????????????????????
     *
     * @param pigStatus ?????????
     * @param pigType   ????????????
     * @return ???????????????
     */
    public List<PigEvent> selectEvents(PigStatus pigStatus, PigType pigType) {
        return DoctorEventSelector.selectPigEvent(pigStatus, pigType);
    }

    /**
     * ????????????????????????????????????
     *
     * @param eventType ????????????
     * @return ??????track?????????
     */
    public DoctorSuggestPigSearch selectPigs(Integer eventType) {
        return DoctorPigsByEventSelector.select(eventType);
    }

    /**
     * ??????????????????????????????
     *
     * @param inputList ??????????????????
     */
    private void eventRepeatCheck(List<BasePigEventInputDto> inputList) {
        List<String> inputPigCodeList = inputList.stream().map(BasePigEventInputDto::getPigCode).sorted().collect(Collectors.toList());

        for (int i = 0; i < inputPigCodeList.size() - 1; i++) {
            if (inputPigCodeList.get(i).equals(inputPigCodeList.get(i + 1))) {
                throw new InvalidException("batch.event.pigCode.not.repeat", inputPigCodeList.get(i));
            }
        }
    }

    @Transactional
    public void delete(Long id) {
        DoctorPigEvent pigEvent = doctorPigEventDao.findById(id);
        if (null == pigEvent) {
            throw new InvalidException("pig.event.not.found", id);
        }
        doctorPigEventDao.delete(id);

        if (PigEvent.WEAN.getKey().intValue() == pigEvent.getType().intValue()) {
            DoctorGroupEvent groupEvent = doctorGroupEventDao.findByRelPigEventId(pigEvent.getId());
            doctorGroupEventDao.delete(groupEvent.getId());
            //4.????????????
            DoctorEventModifyLog modifyLog = DoctorEventModifyLog.builder()
                    .businessId(groupEvent.getGroupId())
                    .businessCode(groupEvent.getGroupCode())
                    .farmId(groupEvent.getFarmId())
                    .deleteEvent(ToJsonMapper.JSON_NON_DEFAULT_MAPPER.toJson(groupEvent))
                    .type(DoctorEventModifyRequest.TYPE.GROUP.getValue())
                    .build();
            doctorEventModifyLogDao.create(modifyLog);
        }

        DoctorEventModifyLog modifyLog = DoctorEventModifyLog.builder()
                .businessId(pigEvent.getPigId())
                .businessCode(pigEvent.getPigCode())
                .farmId(pigEvent.getFarmId())
                .deleteEvent(ToJsonMapper.JSON_NON_DEFAULT_MAPPER.toJson(pigEvent))
                .type(DoctorEventModifyRequest.TYPE.PIG.getValue())
                .build();
        doctorEventModifyLogDao.create(modifyLog);
    }

    @Transactional
    public void modifyPigEvent(DoctorPigEvent newEvent, String oldEvent) {

        doctorPigEventDao.update(newEvent);

        doctorEventModifyLogDao.create(DoctorEventModifyLog.builder()
                .businessId(newEvent.getPigId())
                .businessCode(newEvent.getPigCode())
                .farmId(newEvent.getFarmId())
                .fromEvent(oldEvent)
                .toEvent(ToJsonMapper.JSON_NON_DEFAULT_MAPPER.toJson(newEvent))
                .type(DoctorEventModifyRequest.TYPE.PIG.getValue())
                .build());
        if (pigEventHandler.isSupportedEvent(newEvent))
            pigEventHandler.handle(newEvent);
    }


    @Transactional
    public void modifyGroupEvent(DoctorGroupEvent newEvent, String oldEvent) {

        doctorGroupEventDao.update(newEvent);

        doctorEventModifyLogDao.create(DoctorEventModifyLog.builder()
                .businessId(newEvent.getGroupId())
                .businessCode(newEvent.getGroupCode())
                .farmId(newEvent.getFarmId())
                .fromEvent(oldEvent)
                .toEvent(ToJsonMapper.JSON_NON_DEFAULT_MAPPER.toJson(newEvent))
                .type(DoctorEventModifyRequest.TYPE.GROUP.getValue())
                .build());
    }


}
