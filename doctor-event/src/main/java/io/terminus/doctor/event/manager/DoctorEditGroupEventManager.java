package io.terminus.doctor.event.manager;

import com.google.common.collect.Lists;
import io.terminus.common.utils.Arguments;
import io.terminus.common.utils.Dates;
import io.terminus.doctor.common.exception.InvalidException;
import io.terminus.doctor.common.utils.JsonMapperUtil;
import io.terminus.doctor.common.utils.ToJsonMapper;
import io.terminus.doctor.event.dao.DoctorEventModifyLogDao;
import io.terminus.doctor.event.dao.DoctorGroupDao;
import io.terminus.doctor.event.dao.DoctorGroupEventDao;
import io.terminus.doctor.event.dao.DoctorGroupTrackDao;
import io.terminus.doctor.event.dao.DoctorPigEventDao;
import io.terminus.doctor.event.dto.event.DoctorEventInfo;
import io.terminus.doctor.event.enums.EventStatus;
import io.terminus.doctor.event.enums.GroupEventType;
import io.terminus.doctor.event.enums.IsOrNot;
import io.terminus.doctor.event.handler.group.DoctorGroupEventHandlers;
import io.terminus.doctor.event.handler.usual.DoctorEntryHandler;
import io.terminus.doctor.event.model.DoctorEventModifyLog;
import io.terminus.doctor.event.model.DoctorEventModifyRequest;
import io.terminus.doctor.event.model.DoctorGroup;
import io.terminus.doctor.event.model.DoctorGroupEvent;
import io.terminus.doctor.event.model.DoctorGroupTrack;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Created by IntelliJ IDEA.
 * Author: luoys
 * Date: 11:38 17/3/9
 */

@Slf4j
@Component
public class DoctorEditGroupEventManager {

    private DoctorGroupEventHandlers doctorGroupEventHandlers;
    private DoctorGroupEventDao doctorGroupEventDao;
    private DoctorGroupTrackDao doctorGroupTrackDao;
    private DoctorPigEventDao doctorPigEventDao;
    private DoctorEntryHandler doctorEntryHandler;
    private DoctorGroupDao doctorGroupDao;
    private DoctorEventModifyLogDao doctorEventModifyLogDao;
    private static JsonMapperUtil JSON_MAPPER = JsonMapperUtil.JSON_NON_DEFAULT_MAPPER;

    @Autowired
    public DoctorEditGroupEventManager(DoctorGroupEventHandlers doctorGroupEventHandlers,
                                       DoctorGroupEventDao doctorGroupEventDao,
                                       DoctorGroupTrackDao doctorGroupTrackDao,
                                       DoctorPigEventDao doctorPigEventDao,
                                       DoctorEntryHandler doctorEntryHandler,
                                       DoctorGroupDao doctorGroupDao,
                                       DoctorEventModifyLogDao doctorEventModifyLogDao){
        this.doctorGroupEventHandlers = doctorGroupEventHandlers;
        this.doctorGroupEventDao = doctorGroupEventDao;
        this.doctorGroupTrackDao = doctorGroupTrackDao;
        this.doctorPigEventDao = doctorPigEventDao;
        this.doctorEntryHandler = doctorEntryHandler;
        this.doctorGroupDao = doctorGroupDao;
        this.doctorEventModifyLogDao = doctorEventModifyLogDao;
    }

    @Transactional
    public void reElicitGroupEventByGroupId(Long groupId) {
        List<DoctorGroupEvent> groupEvents = doctorGroupEventDao.findLinkedGroupEventsByGroupId(groupId);
        if(Arguments.isNullOrEmpty(groupEvents)){
            log.error("group events info broken, groupId: {}", groupId);
            throw new InvalidException("group.events.info.broken", groupId);
        }
        DoctorGroupTrack track = doctorGroupTrackDao.findByGroupId(groupId);
        if(Arguments.isNull(track)){
            log.error("group track info broken, groupId: {}", groupId);
            throw new InvalidException("group.track.info.broken", groupId);
        }
        DoctorGroupTrack newTrack = new DoctorGroupTrack();
        newTrack.setId(track.getId());
        newTrack.setQuantity(0);
        newTrack.setSowQty(0);
        newTrack.setBoarQty(0);
        newTrack.setAvgDayAge(0);
        newTrack.setBirthWeight(0d);
        newTrack.setWeanWeight(0d);
        newTrack.setNest(0);
        newTrack.setLiveQty(0);
        newTrack.setHealthyQty(0);
        newTrack.setWeanQty(0);
        newTrack.setUnweanQty(0);
        newTrack.setUnqQty(0);
        newTrack.setQuaQty(0);
        newTrack.setUnqQty(0);


        //按时间,id排序
        groupEvents = groupEvents.stream().sorted(
                (doctorGroupEvent1, doctorGroupEvent2)-> {
                    if(Dates.startOfDay(doctorGroupEvent1.getEventAt()).compareTo(Dates.startOfDay(doctorGroupEvent2.getEventAt())) == 0){
                        return doctorGroupEvent1.getId().compareTo(doctorGroupEvent2.getId());
                    }

                    return doctorGroupEvent1.getEventAt().compareTo(doctorGroupEvent2.getEventAt());
                }
        ).collect(Collectors.toList());

        Long fromEventId = 0L;
        for(DoctorGroupEvent doctorGroupEvent: groupEvents) {
            newTrack = doctorGroupEventHandlers.getEventHandlerMap().get(doctorGroupEvent.getType()).elicitGroupTrack(doctorGroupEvent, newTrack);
            if(!checkTrack(newTrack)){
                throw new InvalidException("group.track.info.broken", groupId);
            }
            createSnapshots(fromEventId, doctorGroupEvent, newTrack);
            fromEventId = doctorGroupEvent.getId();
        }
        doctorGroupTrackDao.update(newTrack);

        DoctorGroupEvent lastEvent = groupEvents.get(groupEvents.size() - 1);
        //track.quantity == 0 最后一个事件不是关闭猪群事件
        if(Objects.equals(newTrack.getQuantity(), 0) && !Objects.equals(lastEvent.getType(), GroupEventType.CLOSE.getValue())){
            closeGroupEvent(lastEvent);
        }
        //track.quantity != 0 最后一个事件是关闭猪群事件应该删除关闭猪群的事件
        if(!Objects.equals(newTrack.getQuantity(), 0) && Objects.equals(lastEvent.getType(), GroupEventType.CLOSE.getValue())){
            deleteCloseGroupEvent(lastEvent);
        }
    }

    private boolean checkTrack(DoctorGroupTrack newTrack) {
        if(newTrack.getQuantity() < 0 ){
            return false;
        }
        return true;
    }

    private void deleteCloseGroupEvent(DoctorGroupEvent lastEvent) {
        lastEvent.setStatus(EventStatus.INVALID.getValue());
        doctorGroupEventDao.update(lastEvent);
    }

    private void createSnapshots(Long fromEventId, DoctorGroupEvent doctorGroupEvent, DoctorGroupTrack newTrack) {
    }

    public void closeGroupEvent(DoctorGroupEvent doctorGroupEvent) {
        DoctorGroup group = doctorGroupDao.findById(doctorGroupEvent.getGroupId());
        group.setStatus(DoctorGroup.Status.CLOSED.getValue());
        doctorGroupDao.update(group);
        doctorGroupEvent.setType(GroupEventType.CLOSE.getValue());
        doctorGroupEvent.setName(GroupEventType.CLOSE.getDesc());
        doctorGroupEvent.setDesc("【系统自动】");
        doctorGroupEvent.setIsAuto(IsOrNot.YES.getValue());
        doctorGroupEvent.setRelGroupEventId(doctorGroupEvent.getId());
        doctorGroupEvent.setExtra(null);
        doctorGroupEventDao.create(doctorGroupEvent);
    }

    @Transactional
    public List<DoctorEventInfo> elicitDoctorGroupTrackRebuildOne(DoctorGroupEvent newEvent, Long modifyRequestId) {
        DoctorGroupEvent oldEvent = doctorGroupEventDao.findById(newEvent.getId());
        if(Objects.equals(EventStatus.INVALID.getValue(), oldEvent.getStatus()) || Objects.equals(EventStatus.HANDLING.getValue(), oldEvent.getStatus())){
            log.error("event has been handled, eventId: {}", oldEvent.getId());
            throw new InvalidException("event.has.been.handled", oldEvent.getId());
        }
        DoctorEventModifyLog modifyLog = new DoctorEventModifyLog();
        modifyLog.setFarmId(newEvent.getFarmId());
        modifyLog.setBusinessId(newEvent.getGroupId());
        modifyLog.setBusinessCode(newEvent.getGroupCode());
        modifyLog.setType(DoctorEventModifyRequest.TYPE.GROUP.getValue());
        modifyLog.setFromEvent(ToJsonMapper.JSON_NON_DEFAULT_MAPPER.toJson(oldEvent));
        modifyLog.setToEvent(ToJsonMapper.JSON_NON_DEFAULT_MAPPER.toJson(newEvent));
        modifyLog.setModifyRequestId(modifyRequestId);
        doctorEventModifyLogDao.create(modifyLog);

//        DoctorGroupEvent updateEvent = new DoctorGroupEvent();
//        updateEvent.setId(oldEvent.getId());
//        updateEvent.setStatus(EventStatus.INVALID.getValue());
//        doctorGroupEventDao.update(updateEvent);
        doctorGroupEventDao.update(newEvent);
//        updateRelation(oldEvent, newEvent); //更新事件关联关系
        reElicitGroupEventByGroupId(newEvent.getGroupId());

        if (Objects.equals(Dates.startOfDay(newEvent.getEventAt()), Dates.startOfDay(oldEvent.getEventAt()))) {
            return Lists.newArrayList(buildGroupEventInfo(newEvent));
        } else {
            return Lists.newArrayList(buildGroupEventInfo(oldEvent), buildGroupEventInfo(newEvent));
        }
    }

    /**
     * 构建事件信息
     * @param groupEvent 事件
     * @return 事件信息
     */
    private DoctorEventInfo buildGroupEventInfo(DoctorGroupEvent groupEvent) {
        return DoctorEventInfo.builder()
                .orgId(groupEvent.getOrgId())
                .farmId(groupEvent.getFarmId())
                .businessId(groupEvent.getGroupId())
                .businessType(DoctorEventInfo.Business_Type.GROUP.getValue())
                .eventId(groupEvent.getId())
                .eventType(groupEvent.getType())
                .eventAt(groupEvent.getEventAt())
                .pigType(groupEvent.getPigType())
                .build();
    }
}
