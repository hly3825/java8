package io.terminus.doctor.event.handler.group;

import io.terminus.common.utils.BeanMapper;
import io.terminus.doctor.common.event.CoreEventDispatcher;
import io.terminus.doctor.common.utils.DateUtil;
import io.terminus.doctor.event.dao.DoctorGroupEventDao;
import io.terminus.doctor.event.dao.DoctorGroupSnapshotDao;
import io.terminus.doctor.event.dao.DoctorGroupTrackDao;
import io.terminus.doctor.event.dto.DoctorGroupSnapShotInfo;
import io.terminus.doctor.event.dto.event.group.DoctorMoveInGroupEvent;
import io.terminus.doctor.event.dto.event.group.input.BaseGroupInput;
import io.terminus.doctor.event.dto.event.group.input.DoctorMoveInGroupInput;
import io.terminus.doctor.event.enums.GroupEventType;
import io.terminus.doctor.event.model.DoctorBarn;
import io.terminus.doctor.event.model.DoctorGroup;
import io.terminus.doctor.event.model.DoctorGroupEvent;
import io.terminus.doctor.event.model.DoctorGroupTrack;
import io.terminus.doctor.event.service.DoctorBarnReadService;
import io.terminus.doctor.event.util.EventUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * Desc: 转入猪群事件处理器
 * Mail: yangzl@terminus.io
 * author: DreamYoung
 * Date: 16/6/18
 */
@Slf4j
@Component
@SuppressWarnings("unchecked")
public class DoctorMoveInGroupEventHandler extends DoctorAbstractGroupEventHandler {

    private final DoctorGroupEventDao doctorGroupEventDao;
    private final DoctorGroupTrackDao doctorGroupTrackDao;

    @Autowired
    public DoctorMoveInGroupEventHandler(DoctorGroupSnapshotDao doctorGroupSnapshotDao,
                                         DoctorGroupTrackDao doctorGroupTrackDao,
                                         CoreEventDispatcher coreEventDispatcher,
                                         DoctorGroupEventDao doctorGroupEventDao,
                                         DoctorBarnReadService doctorBarnReadService) {
        super(doctorGroupSnapshotDao, doctorGroupTrackDao, coreEventDispatcher, doctorGroupEventDao, doctorBarnReadService);
        this.doctorGroupEventDao = doctorGroupEventDao;
        this.doctorGroupTrackDao = doctorGroupTrackDao;
    }


    @Override
    protected <I extends BaseGroupInput> void handleEvent(DoctorGroup group, DoctorGroupTrack groupTrack, I input) {
        DoctorGroupSnapShotInfo oldShot = getOldSnapShotInfo(group, groupTrack);
        DoctorMoveInGroupInput moveIn = (DoctorMoveInGroupInput) input;

        checkQuantityEqual(moveIn.getQuantity(), moveIn.getBoarQty(), moveIn.getSowQty());

        //1.转换转入猪群事件
        DoctorMoveInGroupEvent moveInEvent = BeanMapper.map(moveIn, DoctorMoveInGroupEvent.class);
        checkBreed(group.getBreedId(), moveInEvent.getBreedId());

        //2.创建转入猪群事件
        DoctorGroupEvent<DoctorMoveInGroupEvent> event = dozerGroupEvent(group, GroupEventType.MOVE_IN, moveIn);
        event.setQuantity(moveIn.getQuantity());
        event.setAvgDayAge(moveIn.getAvgDayAge());
        event.setAvgWeight(moveIn.getAvgWeight());
        event.setWeight(EventUtil.getWeight(event.getAvgWeight(), event.getQuantity()));
        event.setInType(moveIn.getInType());

        if (moveIn.getFromBarnId() != null) {
            DoctorBarn fromBarn = getBarnById(moveIn.getFromBarnId());
            moveInEvent.setFromBarnType(fromBarn.getPigType());
            event.setTransGroupType(getTransType(moveIn.getInType(), group.getPigType(), fromBarn).getValue());   //区别内转还是外转
            event.setOtherBarnId(moveIn.getFromBarnId());  //来源猪舍id
            event.setOtherBarnType(fromBarn.getPigType());   //来源猪舍类型
        }
        event.setExtraMap(moveInEvent);
        doctorGroupEventDao.create(event);

        //3.更新猪群跟踪
        Integer oldQty = groupTrack.getQuantity();
        groupTrack.setQuantity(EventUtil.plusInt(groupTrack.getQuantity(), moveIn.getQuantity()));
        groupTrack.setBoarQty(EventUtil.plusInt(groupTrack.getBoarQty(), moveIn.getBoarQty()));
        groupTrack.setSowQty(EventUtil.plusInt(groupTrack.getSowQty(), moveIn.getSowQty()));

        //重新计算日龄, 按照事件录入日期计算
        int deltaDays = DateUtil.getDeltaDaysAbs(event.getEventAt(), new Date());
        groupTrack.setAvgDayAge(EventUtil.getAvgDayAge(getGroupEventAge(groupTrack.getAvgDayAge(), deltaDays), oldQty, moveIn.getAvgDayAge(), moveIn.getQuantity()) + deltaDays);

        //如果是母猪分娩转入，窝数，分娩统计字段需要累加
        if (moveIn.isFarrow()) {
            groupTrack.setNest(EventUtil.plusInt(groupTrack.getNest(), 1));  //窝数加 1
            groupTrack.setLiveQty(EventUtil.plusInt(groupTrack.getLiveQty(), moveIn.getQuantity()));
            groupTrack.setHealthyQty(EventUtil.plusInt(groupTrack.getHealthyQty(), moveIn.getHealthyQty()));
            groupTrack.setWeakQty(EventUtil.plusInt(groupTrack.getWeakQty(), moveIn.getWeakQty()));
            groupTrack.setUnweanQty(EventUtil.plusInt(groupTrack.getUnweanQty(), moveIn.getQuantity()));
            groupTrack.setBirthWeight(EventUtil.plusDouble(groupTrack.getBirthWeight(), moveIn.getAvgWeight() * moveIn.getQuantity()));
        }
        updateGroupTrack(groupTrack, event);

        //4.创建镜像
        createGroupSnapShot(oldShot, new DoctorGroupSnapShotInfo(group, event, groupTrack), GroupEventType.MOVE_IN);

        //发布统计事件
        publistGroupAndBarn(group.getOrgId(), group.getFarmId(), group.getId(), group.getCurrentBarnId(), event.getId());
    }
}
