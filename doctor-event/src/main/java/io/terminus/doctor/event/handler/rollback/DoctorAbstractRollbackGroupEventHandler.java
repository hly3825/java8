package io.terminus.doctor.event.handler.rollback;

import io.terminus.common.utils.JsonMapper;
import io.terminus.doctor.common.utils.RespHelper;
import io.terminus.doctor.event.dao.DoctorGroupDao;
import io.terminus.doctor.event.dao.DoctorGroupEventDao;
import io.terminus.doctor.event.dao.DoctorGroupSnapshotDao;
import io.terminus.doctor.event.dao.DoctorGroupTrackDao;
import io.terminus.doctor.event.dao.DoctorPigEventDao;
import io.terminus.doctor.event.dto.DoctorGroupSnapShotInfo;
import io.terminus.doctor.event.dto.DoctorRollbackDto;
import io.terminus.doctor.event.enums.IsOrNot;
import io.terminus.doctor.event.enums.RollbackType;
import io.terminus.doctor.event.handler.DoctorRollbackGroupEventHandler;
import io.terminus.doctor.event.model.DoctorGroupEvent;
import io.terminus.doctor.event.model.DoctorGroupSnapshot;
import io.terminus.doctor.event.model.DoctorRevertLog;
import io.terminus.doctor.event.service.DoctorGroupReadService;
import io.terminus.doctor.event.service.DoctorPigEventReadService;
import io.terminus.doctor.event.service.DoctorRevertLogWriteService;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

/**
 * Desc: 猪群事件回滚处理器
 * Mail: yangzl@terminus.io
 * author: DreamYoung
 * Date: 16/9/20
 */
@Slf4j
public abstract class DoctorAbstractRollbackGroupEventHandler extends DoctorRollbackReportHandler implements DoctorRollbackGroupEventHandler {

    protected static final JsonMapper JSON_MAPPER = JsonMapper.nonEmptyMapper();

    @Autowired private DoctorRevertLogWriteService doctorRevertLogWriteService;
    @Autowired protected DoctorGroupReadService doctorGroupReadService;
    @Autowired protected DoctorGroupDao doctorGroupDao;
    @Autowired protected DoctorGroupEventDao doctorGroupEventDao;
    @Autowired protected DoctorGroupTrackDao doctorGroupTrackDao;
    @Autowired protected DoctorGroupSnapshotDao doctorGroupSnapshotDao;
    @Autowired protected DoctorPigEventDao doctorPigEventDao;
    @Autowired protected DoctorPigEventReadService doctorPigEventReadService;

    /**
     * 判断能否回滚(1.手动事件 2.三个月内的事件 3.最新事件 4.子类根据事件类型特殊处理)
     */
    @Override
    public final boolean canRollback(DoctorGroupEvent groupEvent) {
        return Objects.equals(groupEvent.getIsAuto(), IsOrNot.NO.getValue()) &&
                groupEvent.getEventAt().after(DateTime.now().plusMonths(-3).toDate()) &&
                handleCheck(groupEvent);
    }

    /**
     * 回滚实现
     */
    @Override
    public final void rollback(DoctorGroupEvent groupEvent, Long operatorId, String operatorName) {
        handleRollback(groupEvent, operatorId, operatorName);
    }

    /**
     * 更新统计报表, es搜索(发zk事件)
     */
    @Override
    public final void updateReport(DoctorGroupEvent groupEvent) {
        checkAndPublishRollback(handleReport(groupEvent));
    }

    /**
     * 每个子类根据事件类型 判断是否应该由此handler执行回滚
     */
    protected abstract boolean handleCheck(DoctorGroupEvent groupEvent);

    /**
     * 处理回滚操作(事务处理)
     */
    @Transactional
    protected abstract void handleRollback(DoctorGroupEvent groupEvent, Long operatorId, String operatorName);

    /**
     * 需要更新的统计
     * @see RollbackType
     */
    protected abstract List<DoctorRollbackDto> handleReport(DoctorGroupEvent groupEvent);

    /**
     * 是否是最新事件
     */
    protected boolean isLastEvent(DoctorGroupEvent groupEvent) {
        return RespHelper.orFalse(doctorGroupReadService.isLastEvent(groupEvent.getGroupId(), groupEvent.getId()));
    }

    /**
     * 单个猪群事件的回滚，可以抽象出来，自取自用
     * @param groupEvent 事件
     * @return 回滚日志
     */
    protected void sampleRollback(DoctorGroupEvent groupEvent, Long operatorId, String operatorName) {
        DoctorGroupSnapshot snapshot = doctorGroupSnapshotDao.findGroupSnapshotByToEventId(groupEvent.getId());

        DoctorGroupSnapShotInfo info = JSON_MAPPER.fromJson(snapshot.getFromInfo(), DoctorGroupSnapShotInfo.class);

        //删除此事件 -> 回滚猪群跟踪 -> 回滚猪群 -> 删除镜像
        doctorGroupEventDao.delete(groupEvent.getId());
        doctorGroupTrackDao.update(info.getGroupTrack());
        doctorGroupDao.update(info.getGroup());
        doctorGroupSnapshotDao.delete(snapshot.getId());

        //创建回滚日志
        createRevertLog(groupEvent, snapshot, operatorId, operatorName);
    }

    protected void createRevertLog(DoctorGroupEvent groupEvent, DoctorGroupSnapshot snapshot, Long operatorId, String operatorName) {
        DoctorRevertLog revertLog = new  DoctorRevertLog();
        revertLog.setFarmId(groupEvent.getFarmId());
        revertLog.setGroupId(groupEvent.getGroupId());
        revertLog.setFromInfo(snapshot.getToInfo());
        revertLog.setToInfo(snapshot.getFromInfo());
        revertLog.setType(DoctorRevertLog.Type.GROUP.getValue());
        revertLog.setReverterId(operatorId);
        revertLog.setReverterName(operatorName);
        RespHelper.orServEx(doctorRevertLogWriteService.createRevertLog(revertLog));
    }
}