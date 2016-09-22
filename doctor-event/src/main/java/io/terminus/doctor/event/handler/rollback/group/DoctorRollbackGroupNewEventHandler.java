package io.terminus.doctor.event.handler.rollback.group;

import io.terminus.doctor.event.dto.DoctorRollbackDto;
import io.terminus.doctor.event.enums.GroupEventType;
import io.terminus.doctor.event.handler.rollback.DoctorAbstractRollbackGroupEventHandler;
import io.terminus.doctor.event.model.DoctorGroupEvent;
import io.terminus.doctor.event.model.DoctorGroupSnapshot;
import io.terminus.doctor.event.model.DoctorRevertLog;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

/**
 * Desc: 新建猪群回滚
 * Mail: yangzl@terminus.io
 * author: DreamYoung
 * Date: 2016/9/22
 */
@Slf4j
@Component
public class DoctorRollbackGroupNewEventHandler extends DoctorAbstractRollbackGroupEventHandler {
    @Override
    protected boolean handleCheck(DoctorGroupEvent groupEvent) {
        //允许新建猪群事件回滚
        return Objects.equals(groupEvent.getType(), GroupEventType.NEW.getValue());
    }

    @Override
    protected DoctorRevertLog handleRollback(DoctorGroupEvent groupEvent) {
        DoctorGroupSnapshot snapshot = doctorGroupSnapshotDao.findGroupSnapshotByToEventId(groupEvent.getId());

        //删除此事件 -> 删除猪群跟踪 -> 删除猪群 -> 删除镜像
        doctorGroupEventDao.delete(groupEvent.getId());
        doctorGroupTrackDao.deleteByGroupId(groupEvent.getGroupId());
        doctorGroupDao.delete(groupEvent.getGroupId());
        doctorGroupSnapshotDao.delete(snapshot.getId());
        return DoctorRevertLog.builder()
                .fromInfo(snapshot.getToInfo())
                .toInfo(snapshot.getFromInfo())
                .build();
    }

    @Override
    protected List<DoctorRollbackDto> handleReport(DoctorGroupEvent groupEvent) {
        return null;    //不需要更新统计
    }
}
