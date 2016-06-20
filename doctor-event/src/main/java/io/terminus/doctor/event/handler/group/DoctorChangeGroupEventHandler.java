package io.terminus.doctor.event.handler.group;

import com.google.common.base.MoreObjects;
import io.terminus.common.utils.BeanMapper;
import io.terminus.doctor.common.event.CoreEventDispatcher;
import io.terminus.doctor.event.dao.DoctorGroupEventDao;
import io.terminus.doctor.event.dao.DoctorGroupSnapshotDao;
import io.terminus.doctor.event.dao.DoctorGroupTrackDao;
import io.terminus.doctor.event.dto.event.group.DoctorChangeGroupEvent;
import io.terminus.doctor.event.dto.event.group.input.BaseGroupInput;
import io.terminus.doctor.event.dto.event.group.input.DoctorChangeGroupInput;
import io.terminus.doctor.event.enums.GroupEventType;
import io.terminus.doctor.event.model.DoctorGroup;
import io.terminus.doctor.event.model.DoctorGroupEvent;
import io.terminus.doctor.event.model.DoctorGroupTrack;
import io.terminus.doctor.event.util.EventUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * Desc: 猪群变动事件处理器
 * Mail: yangzl@terminus.io
 * author: DreamYoung
 * Date: 16/6/18
 */
@Slf4j
@Component
public class DoctorChangeGroupEventHandler extends DoctorAbstractGroupEventHandler {

    private final DoctorGroupEventDao doctorGroupEventDao;
    private final DoctorCommonGroupEventHandler doctorCommonGroupEventHandler;

    @Autowired
    public DoctorChangeGroupEventHandler(DoctorGroupSnapshotDao doctorGroupSnapshotDao,
                                         DoctorGroupTrackDao doctorGroupTrackDao,
                                         CoreEventDispatcher coreEventDispatcher,
                                         DoctorGroupEventDao doctorGroupEventDao,
                                         DoctorCommonGroupEventHandler doctorCommonGroupEventHandler) {
        super(doctorGroupSnapshotDao, doctorGroupTrackDao, coreEventDispatcher);
        this.doctorGroupEventDao = doctorGroupEventDao;
        this.doctorCommonGroupEventHandler = doctorCommonGroupEventHandler;
    }


    // TODO: 16/5/30 销售要记录销售数量
    @Override
    protected <I extends BaseGroupInput> void handleEvent(DoctorGroup group, DoctorGroupTrack groupTrack, I input) {
        DoctorChangeGroupInput change = (DoctorChangeGroupInput) input;

        checkQuantity(groupTrack.getQuantity(), change.getQuantity());
        checkQuantity(groupTrack.getBoarQty(), change.getBoarQty());
        checkQuantity(groupTrack.getSowQty(), change.getSowQty());
        checkQuantityEqual(change.getQuantity(), change.getBoarQty(), change.getSowQty());

        //1.转换猪群变动事件
        DoctorChangeGroupEvent changeEvent = BeanMapper.map(change, DoctorChangeGroupEvent.class);

        //2.创建猪群变动事件
        DoctorGroupEvent<DoctorChangeGroupEvent> event = dozerGroupEvent(group, GroupEventType.CHANGE, change);
        event.setQuantity(change.getQuantity());
        event.setAvgDayAge(groupTrack.getAvgDayAge());  //变动的日龄不需要录入, 直接取猪群的日龄
        event.setWeight(change.getWeight());
        event.setAvgWeight(EventUtil.getAvgWeight(change.getWeight(), change.getQuantity()));
        event.setExtraMap(changeEvent);
        doctorGroupEventDao.create(event);

        Integer oldQuantity = groupTrack.getQuantity();

        //3.更新猪群跟踪
        groupTrack.setQuantity(EventUtil.minusQuantity(groupTrack.getQuantity(), change.getQuantity()));
        groupTrack.setBoarQty(EventUtil.minusQuantity(groupTrack.getBoarQty(), change.getBoarQty()));
        groupTrack.setSowQty(EventUtil.minusQuantity(groupTrack.getSowQty(), change.getSowQty()));

        //重新计算重量
        groupTrack.setWeight(groupTrack.getWeight() - change.getWeight());
        groupTrack.setAvgWeight(EventUtil.getAvgWeight(groupTrack.getWeight(), groupTrack.getQuantity()));

        //重新计算金额
        groupTrack.setAmount(groupTrack.getAmount() - MoreObjects.firstNonNull(change.getAmount(), 0L));
        groupTrack.setPrice(EventUtil.getPrice(groupTrack.getAmount(), groupTrack.getQuantity()));
        updateGroupTrack(groupTrack, event);

        //4.创建镜像
        createGroupSnapShot(group, event, groupTrack, GroupEventType.CHANGE);

        //5.判断变动数量, 如果 = 猪群数量, 触发关闭猪群事件
        if (Objects.equals(oldQuantity, change.getQuantity())) {
            doctorCommonGroupEventHandler.autoGroupEventClose(group, groupTrack, change);
        }

        //发布统计事件
        publishCountGroupEvent(group.getOrgId(), group.getFarmId());
        publistGroupAndBarn(group.getId(), group.getCurrentBarnId());
    }
}
