package io.terminus.doctor.event.manager;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import io.terminus.common.exception.ServiceException;
import io.terminus.common.utils.Arguments;
import io.terminus.common.utils.JsonMapper;
import io.terminus.doctor.common.exception.InvalidException;
import io.terminus.doctor.event.dao.DoctorGroupDao;
import io.terminus.doctor.event.dao.DoctorGroupEventDao;
import io.terminus.doctor.event.dao.DoctorGroupTrackDao;
import io.terminus.doctor.event.dao.DoctorRevertLogDao;
import io.terminus.doctor.event.dto.DoctorGroupDetail;
import io.terminus.doctor.event.dto.event.DoctorEventInfo;
import io.terminus.doctor.event.dto.event.group.input.BaseGroupInput;
import io.terminus.doctor.event.dto.event.group.input.DoctorGroupInputInfo;
import io.terminus.doctor.event.editHandler.DoctorModifyGroupEventHandler;
import io.terminus.doctor.event.editHandler.group.DoctorModifyGroupEventHandlers;
import io.terminus.doctor.event.enums.GroupEventType;
import io.terminus.doctor.event.enums.IsOrNot;
import io.terminus.doctor.event.handler.DoctorGroupEventHandler;
import io.terminus.doctor.event.handler.group.DoctorAntiepidemicGroupEventHandler;
import io.terminus.doctor.event.handler.group.DoctorChangeGroupEventHandler;
import io.terminus.doctor.event.handler.group.DoctorCloseGroupEventHandler;
import io.terminus.doctor.event.handler.group.DoctorCommonGroupEventHandler;
import io.terminus.doctor.event.handler.group.DoctorDiseaseGroupEventHandler;
import io.terminus.doctor.event.handler.group.DoctorLiveStockGroupEventHandler;
import io.terminus.doctor.event.handler.group.DoctorMoveInGroupEventHandler;
import io.terminus.doctor.event.handler.group.DoctorTransFarmGroupEventHandler;
import io.terminus.doctor.event.handler.group.DoctorTransGroupEventHandler;
import io.terminus.doctor.event.handler.group.DoctorTurnSeedGroupEventHandler;
import io.terminus.doctor.event.handler.group.DoctorWeanGroupEventHandler;
import io.terminus.doctor.event.model.DoctorGroupEvent;
import io.terminus.doctor.event.model.DoctorGroupTrack;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Desc:
 * Mail: yangzl@terminus.io
 * author: DreamYoung
 * Date: 16/6/18
 */
@Slf4j
@Component
public class DoctorGroupEventManager {

    @Autowired
    private ApplicationContext applicationContext;
    @Autowired
    private DoctorGroupDao doctorGroupDao;
    @Autowired
    private DoctorGroupTrackDao doctorGroupTrackDao;
    @Autowired
    private DoctorGroupEventDao doctorGroupEventDao;
    @Autowired
    private DoctorRevertLogDao doctorRevertLogDao;
    @Autowired
    private DoctorCommonGroupEventHandler doctorCommonGroupEventHandler;
    @Autowired
    private DoctorModifyGroupEventHandlers modifyGroupEventHandlers;

    private static final JsonMapper JSON_MAPPER = JsonMapper.nonEmptyMapper();

    private Map<Class<? extends DoctorGroupEventHandler>, DoctorGroupEventHandler> handlerMapping;

    private Map<Integer, DoctorGroupEventHandler> handlerMap;

    /**
     * ????????????????????????
     */
    @PostConstruct
    public void initHandlers() {
        handlerMapping = Maps.newHashMap();
        handlerMap = Maps.newHashMap();
        Map<String, DoctorGroupEventHandler> handlers = applicationContext.getBeansOfType(DoctorGroupEventHandler.class);
        log.info("Doctor group event handlers :{}", handlers);
        if (!handlers.isEmpty()) {
            handlers.values().forEach(handler -> handlerMapping.put(handler.getClass(), handler));
            handlerMap.put(GroupEventType.MOVE_IN.getValue(), handlerMapping.get(DoctorMoveInGroupEventHandler.class));
            handlerMap.put(GroupEventType.CHANGE.getValue(), handlerMapping.get(DoctorChangeGroupEventHandler.class));
            handlerMap.put(GroupEventType.TRANS_GROUP.getValue(), handlerMapping.get(DoctorTransGroupEventHandler.class));
            handlerMap.put(GroupEventType.TURN_SEED.getValue(), handlerMapping.get(DoctorTurnSeedGroupEventHandler.class));
            handlerMap.put(GroupEventType.LIVE_STOCK.getValue(), handlerMapping.get(DoctorLiveStockGroupEventHandler.class));
            handlerMap.put(GroupEventType.DISEASE.getValue(), handlerMapping.get(DoctorDiseaseGroupEventHandler.class));
            handlerMap.put(GroupEventType.ANTIEPIDEMIC.getValue(), handlerMapping.get(DoctorAntiepidemicGroupEventHandler.class));
            handlerMap.put(GroupEventType.TRANS_FARM.getValue(), handlerMapping.get(DoctorTransFarmGroupEventHandler.class));
            handlerMap.put(GroupEventType.CLOSE.getValue(), handlerMapping.get(DoctorCloseGroupEventHandler.class));
            handlerMap.put(GroupEventType.WEAN.getValue(), handlerMapping.get(DoctorWeanGroupEventHandler.class));
        }
    }

    /**
     * ??????????????????
     * @param inputInfo ????????????
     * @param eventType ????????????
     * @param <I>
     * @return ????????????
     */
    public <I extends BaseGroupInput> DoctorGroupEvent buildGroupEvent(DoctorGroupInputInfo inputInfo, Integer eventType) {
        //??????????????????????????????????????????(?????????????????????)
        return getHandler(eventType).buildGroupEvent(inputInfo.getGroupDetail().getGroup(), inputInfo.getGroupDetail().getGroupTrack(), inputInfo.getInput());
    }

    /**
     * ????????????????????????????????????
     * @param groupDetail  ????????????
     * @param input        ????????????
     * @param handlerClass ??????handler????????????
     * @see GroupEventType
     */
    @Transactional
    public <I extends BaseGroupInput>
    List<DoctorEventInfo> handleEvent(DoctorGroupDetail groupDetail, I input, Class<? extends DoctorGroupEventHandler> handlerClass) {
        log.info("group event handle starting, handler class:{}", handlerClass);
        final List<DoctorEventInfo> eventInfoList = Lists.newArrayList();
        getHandler(handlerClass).handle(eventInfoList, groupDetail.getGroup(), groupDetail.getGroupTrack(), input);
        log.info("group event handle ending, handler class:{}", handlerClass);
        return eventInfoList;
    }

    /**
     * ??????????????????
     * @param inputInfoList
     * @param eventType
     * @return
     */
    @Transactional
    public List<DoctorEventInfo> batchHandleEvent(List<DoctorGroupInputInfo> inputInfoList, Integer eventType) {
        //eventRepeatCheck(inputInfoList); // TODO: 17/1/20 ??????????????????????????????????????????
        log.info("batch group event handle starting, eventType:{}", eventType);
        final List<DoctorEventInfo> eventInfoList = Lists.newArrayList();
        inputInfoList.forEach(inputInfo -> {
            try {
                getHandler(eventType)
                        .handle(eventInfoList, doctorGroupDao.findById(inputInfo.getGroupDetail().getGroup().getId()), doctorGroupTrackDao.findById(inputInfo.getGroupDetail().getGroupTrack().getId()), inputInfo.getInput());
            } catch (InvalidException e) {
                throw new InvalidException(true, e.getError(), inputInfo.getGroupDetail().getGroup().getGroupCode(), e.getParams());
            }
        });
        log.info("batch group event handle ending, eventType:{}", eventType);
        return eventInfoList;
    }

    /**
     * ??????????????????, ????????????: ?????????????????????????????????, ??????????????????????????????????????????????????????????????????
     * @param groupEvent ???????????????id
     */
    @Transactional
    public void rollbackEvent(DoctorGroupEvent groupEvent, Long reverterId, String reverterName) {
        log.info("rollback group event starting, group event id:{}", groupEvent.getId());
        //??????????????????
        checkCanRollback(groupEvent);
        //??????????????? -> ?????????????????? -> ???????????? -> ????????????
        doctorGroupEventDao.delete(groupEvent.getId());
        log.info("rollback group event ending, group event id:{}", groupEvent.getId());
    }

    /**
     * ????????????????????????
     * @param inputDto ????????????
     * @param eventId ??????id
     * @param eventType ????????????
     */
    @Transactional
    public void modifyGroupEventHandle(BaseGroupInput inputDto, Long eventId, Integer eventType) {
        DoctorGroupEvent oldGroupEvent = doctorGroupEventDao.findById(eventId);
        DoctorModifyGroupEventHandler handler = modifyGroupEventHandlers.getModifyGroupEventHandlerMap().get(eventType);
        if (!handler.canModify(oldGroupEvent)) {
            throw new InvalidException("event.not.modify");
        }
        handler.modifyHandle(oldGroupEvent, inputDto);
    }

    //??????????????????
    private void checkCanRollback(DoctorGroupEvent event) {
        DoctorGroupTrack groupTrack = doctorGroupTrackDao.findByGroupId(event.getGroupId());

        //????????????????????????????????????
        if (!Objects.equals(event.getId(), groupTrack.getRelEventId())) {
            log.error("group event not the latest, can not rollback, event:{}", event);
            throw new InvalidException("group.event.not.the.latest", event.getId());
        }

        //????????????????????????????????????
        if (Objects.equals(IsOrNot.YES.getValue(), event.getIsAuto())) {
            log.error("group event is auto event, can not rollback, event:{}", event);
            throw new InvalidException("group.event.is.auto");
        }

        //??????????????????????????????
        if (Objects.equals(GroupEventType.NEW.getValue(), event.getType())) {
            log.error("new group event can not rollback, event:{}", event);
            throw new InvalidException("group.event.new.not.rollback", event.getId());
        }
    }

    /**
     * ?????????????????????
     * @param interfaceClass ??????????????????
     * @return ???????????????
     */
    private DoctorGroupEventHandler getHandler(Class<? extends DoctorGroupEventHandler> interfaceClass) {
        if (!handlerMapping.containsKey(interfaceClass) || handlerMapping.get(interfaceClass) == null) {
            log.error("Not any event handler found for illegal class:{}", interfaceClass.getName());
            throw new ServiceException("handler.not.found");
        }
        return handlerMapping.get(interfaceClass);
    }

    /**
     * ?????????????????????
     * @param eventType ????????????
     * @return ???????????????
     */
    private DoctorGroupEventHandler getHandler(Integer eventType) {
        if (!handlerMap.containsKey(eventType) || handlerMap.get(eventType) == null) {
            log.error("Not any event handler found for illegal eventType:{}", eventType);
            throw new ServiceException("handler.not.found");
        }
        return handlerMap.get(eventType);
    }

    /**
     * ??????????????????????????????
     * @param inputList ??????????????????
     */
    private void eventRepeatCheck(List<DoctorGroupInputInfo> inputList) {
        if (Arguments.isNullOrEmpty(inputList)) {
            throw new ServiceException("batch.event.input.empty");
        }
        if (Objects.equals(inputList.get(0).getInput().getEventType(), GroupEventType.TURN_SEED.getValue())) {
            return;
        }
        Set<String> inputSet = inputList.stream().map(groupInputInfo -> groupInputInfo.getGroupDetail().getGroup().getGroupCode()).collect(Collectors.toSet());
        if (inputList.size() != inputSet.size()) {
            throw new ServiceException("batch.event.groupCode.not.repeat");
        }
    }

}
