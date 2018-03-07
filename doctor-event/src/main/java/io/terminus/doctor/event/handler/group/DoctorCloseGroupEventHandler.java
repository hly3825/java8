package io.terminus.doctor.event.handler.group;

import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import io.terminus.doctor.common.enums.DataEventType;
import io.terminus.doctor.common.event.DataEvent;
import io.terminus.doctor.common.exception.InvalidException;
import io.terminus.doctor.event.dao.DoctorBarnDao;
import io.terminus.doctor.event.dao.DoctorGroupDao;
import io.terminus.doctor.event.dao.DoctorGroupEventDao;
import io.terminus.doctor.event.dao.DoctorGroupTrackDao;
import io.terminus.doctor.event.dto.event.DoctorEventInfo;
import io.terminus.doctor.event.dto.event.group.input.BaseGroupInput;
import io.terminus.doctor.event.dto.event.group.input.DoctorCloseGroupInput;
import io.terminus.doctor.event.enums.GroupEventType;
import io.terminus.doctor.event.event.MsgGroupPublishDto;
import io.terminus.doctor.event.event.MsgListenedGroupEvent;
import io.terminus.doctor.event.model.DoctorGroup;
import io.terminus.doctor.event.model.DoctorGroupEvent;
import io.terminus.doctor.event.model.DoctorGroupTrack;
import io.terminus.zookeeper.pubsub.Publisher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Desc: 关闭猪群事件处理器
 * Mail: yangzl@terminus.io
 * author: DreamYoung
 * Date: 16/6/18
 */
@Slf4j
@Component
@SuppressWarnings("unchecked")
public class DoctorCloseGroupEventHandler extends DoctorAbstractGroupEventHandler {

    private final DoctorGroupDao doctorGroupDao;
    private final DoctorGroupEventDao doctorGroupEventDao;
    @Autowired
    private Publisher publisher;

    @Autowired
    public DoctorCloseGroupEventHandler(DoctorGroupTrackDao doctorGroupTrackDao,
                                        DoctorGroupDao doctorGroupDao,
                                        DoctorGroupEventDao doctorGroupEventDao,
                                        DoctorBarnDao doctorBarnDao) {
        super(doctorGroupTrackDao, doctorGroupEventDao, doctorBarnDao);
        this.doctorGroupDao = doctorGroupDao;
        this.doctorGroupEventDao = doctorGroupEventDao;
    }


    @Override
    protected <I extends BaseGroupInput> void handleEvent(List<DoctorEventInfo> eventInfoList, DoctorGroup group, DoctorGroupTrack groupTrack, I input) {
        input.setEventType(GroupEventType.CLOSE.getValue());

        //校验能否关闭
        checkCanClose(groupTrack);

        DoctorCloseGroupInput close = (DoctorCloseGroupInput) input;

        //1.转换下信息

        //2.创建关闭猪群事件
        DoctorGroupEvent event = dozerGroupEvent(group, GroupEventType.CLOSE, close);

        event.setExtraMap(close);
        doctorGroupEventDao.create(event);

        //创建关联关系
        //createEventRelation(event);

        groupTrack.setCloseAt(event.getEventAt());
        updateGroupTrack(groupTrack, event);

        //4.猪群状态改为关闭
        group.setStatus(DoctorGroup.Status.CLOSED.getValue());
        group.setCloseAt(event.getEventAt());
        doctorGroupDao.update(group);

        //发布zk事件
        try{
            // 向zk发送刷新消息的事件
            MsgGroupPublishDto msgGroupPublishDto = new MsgGroupPublishDto(group.getId(), event.getId(), event.getEventAt(), event.getType());
            publisher.publish(DataEvent.toBytes(DataEventType.GroupEventClose.getKey(), new MsgListenedGroupEvent(group.getOrgId(), group.getFarmId(), Lists.newArrayList(msgGroupPublishDto))));
        }catch(Exception e){
            log.error(Throwables.getStackTraceAsString(e));
        }
    }

    @Override
    public <I extends BaseGroupInput> DoctorGroupEvent buildGroupEvent(DoctorGroup group, DoctorGroupTrack groupTrack, I input) {
        input.setEventType(GroupEventType.CLOSE.getValue());

        DoctorCloseGroupInput close = (DoctorCloseGroupInput) input;

        //2.创建关闭猪群事件
        DoctorGroupEvent event = dozerGroupEvent(group, GroupEventType.CLOSE, close);

        event.setExtraMap(close);

        return event;
    }

    @Override
    public DoctorGroupTrack updateTrackOtherInfo(DoctorGroupEvent event, DoctorGroupTrack track) {
        return track;
    }


    //猪群里还有猪不可关闭!
    private void checkCanClose(DoctorGroupTrack groupTrack) {
        if (groupTrack.getQuantity() > 0) {
            throw new InvalidException("group.not.empty.cannot.close");
        }
    }
}
