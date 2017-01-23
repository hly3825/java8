package io.terminus.doctor.event.handler.rollback;

import io.terminus.doctor.common.util.JsonMapperUtil;
import io.terminus.doctor.common.utils.RespHelper;
import io.terminus.doctor.event.dao.DoctorBarnDao;
import io.terminus.doctor.event.dao.DoctorGroupDao;
import io.terminus.doctor.event.dao.DoctorGroupEventDao;
import io.terminus.doctor.event.dao.DoctorGroupTrackDao;
import io.terminus.doctor.event.dao.DoctorPigDao;
import io.terminus.doctor.event.dao.DoctorPigEventDao;
import io.terminus.doctor.event.dao.DoctorPigSnapshotDao;
import io.terminus.doctor.event.dao.DoctorPigTrackDao;
import io.terminus.doctor.event.dto.DoctorPigSnapShotInfo;
import io.terminus.doctor.event.handler.DoctorRollbackPigEventHandler;
import io.terminus.doctor.event.model.DoctorGroupEvent;
import io.terminus.doctor.event.model.DoctorPig;
import io.terminus.doctor.event.model.DoctorPigEvent;
import io.terminus.doctor.event.model.DoctorPigSnapshot;
import io.terminus.doctor.event.model.DoctorPigTrack;
import io.terminus.doctor.event.model.DoctorRevertLog;
import io.terminus.doctor.event.service.DoctorGroupReadService;
import io.terminus.doctor.event.service.DoctorRevertLogWriteService;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.text.SimpleDateFormat;
import java.util.Objects;

/**
 * Desc: 猪事件回滚handler
 * Mail: yangzl@terminus.io
 * author: DreamYoung
 * Date: 16/9/20
 */
@Slf4j
public abstract class DoctorAbstractRollbackPigEventHandler implements DoctorRollbackPigEventHandler {

    protected static final JsonMapperUtil JSON_MAPPER = JsonMapperUtil.nonEmptyMapper();

    @Autowired
    private DoctorRevertLogWriteService doctorRevertLogWriteService;
    @Autowired
    protected DoctorGroupReadService doctorGroupReadService;
    @Autowired
    protected DoctorGroupDao doctorGroupDao;
    @Autowired
    protected DoctorGroupEventDao doctorGroupEventDao;
    @Autowired
    protected DoctorGroupTrackDao doctorGroupTrackDao;
    @Autowired
    protected DoctorPigSnapshotDao doctorPigSnapshotDao;
    @Autowired
    protected DoctorPigEventDao doctorPigEventDao;
    @Autowired
    protected DoctorPigTrackDao doctorPigTrackDao;
    @Autowired
    protected DoctorPigDao doctorPigDao;
    @Autowired
    protected DoctorBarnDao doctorBarnDao;

    @Value("${flow.definition.key.sow:sow}")
    protected String sowFlowKey;

    /**
     * 判断能否回滚(1.手动事件 2.三个月内的事件 3.最新事件 4.子类根据事件类型特殊处理)
     */
    @Override
    public final boolean canRollback(DoctorPigEvent pigEvent) {
        return isLastManualEvent(pigEvent) &&
                pigEvent.getEventAt().after(DateTime.now().plusMonths(-12).toDate()) &&
                handleCheck(pigEvent);
    }

    /**
     * 带事务的回滚操作
     */
    @Override
    public final void rollback(DoctorPigEvent pigEvent, Long operatorId, String operatorName) {
        handleRollback(pigEvent, operatorId, operatorName);
    }

    /**
     * 是否是最新事件
     */
    protected boolean isLastManualEvent(DoctorPigEvent pigEvent) {
        DoctorPigEvent lastManualEvent = doctorPigEventDao.queryLastManualPigEventById(pigEvent.getPigId());
        return Objects.equals(lastManualEvent.getId(), pigEvent.getId());
    }

    /**
     * 每个子类根据事件类型 判断是否应该由此handler执行回滚
     */
    protected abstract boolean handleCheck(DoctorPigEvent pigEvent);

    /**
     * 处理回滚操作
     */
    protected abstract void handleRollback(DoctorPigEvent pigEvent, Long operatorId, String operatorName);

    /**
     * 不涉及状态的事件回滚处理
     *
     * @param pigEvent 猪事件
     */
    protected void handleRollbackWithoutStatus(DoctorPigEvent pigEvent, Long operatorId, String operatorName) {
        DoctorPigTrack doctorPigTrack = doctorPigTrackDao.findByPigId(pigEvent.getPigId());
        DoctorPig doctorPig = doctorPigDao.findById(pigEvent.getPigId());
        DoctorPigSnapshot snapshot = doctorPigSnapshotDao.queryByEventId(pigEvent.getId());
        JSON_MAPPER.setDateFormat(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"));
        DoctorPigSnapShotInfo info = JSON_MAPPER.fromJson(snapshot.getPigInfo(), DoctorPigSnapShotInfo.class);
        doctorPigEventDao.delete(pigEvent.getId());
        doctorPigTrackDao.update(info.getPigTrack());
        doctorPigDao.update(info.getPig());
        doctorPigSnapshotDao.delete(snapshot.getId());
        createDoctorRevertLog(pigEvent, doctorPigTrack, doctorPig, operatorId, operatorName);
    }

    /**
     * 涉及状态的事件回滚处理
     *
     * @param pigEvent 猪事件
     */
    protected void handleRollbackWithStatus(DoctorPigEvent pigEvent, Long operatorId, String operatorName) {
        handleRollbackWithoutStatus(pigEvent, operatorId, operatorName);
    }

    /**
     * 创建回滚日志
     *
     * @param fromPigEvent 事件
     * @return 回滚日志
     */
    protected void createDoctorRevertLog(DoctorPigEvent fromPigEvent, DoctorPigTrack fromPigTrack, DoctorPig fromPig, Long operatorId, String operatorName) {
        DoctorPigSnapShotInfo fromInfo = DoctorPigSnapShotInfo.builder()
                .pigEvent(fromPigEvent)
                .pigTrack(fromPigTrack)
                .pig(fromPig)
                .build();

        DoctorPigEvent toPigEvent = doctorPigEventDao.queryLastPigEventById(fromPigEvent.getPigId());
        DoctorPigTrack toPigTrack = doctorPigTrackDao.findByPigId(fromPigEvent.getPigId());
        DoctorPig toPig = doctorPigDao.findById(fromPigEvent.getPigId());
        DoctorPigSnapShotInfo toInfo = DoctorPigSnapShotInfo.builder()
                .pigEvent(toPigEvent)
                .pigTrack(toPigTrack)
                .pig(toPig)
                .build();

        DoctorRevertLog revertLog = new DoctorRevertLog();
        revertLog.setFarmId(fromPigEvent.getFarmId());
        revertLog.setPigId(fromPigEvent.getPigId());
        revertLog.setType(fromPigEvent.getKind());
        revertLog.setFromInfo(JSON_MAPPER.toJson(fromInfo));
        revertLog.setToInfo(JSON_MAPPER.toJson(toInfo));
        revertLog.setReverterId(operatorId);
        revertLog.setReverterName(operatorName);
        RespHelper.orServEx(doctorRevertLogWriteService.createRevertLog(revertLog));
    }

    //判断事件链的最后一个事件，是否是最新事件
    protected boolean isRelLastGroupEvent(DoctorGroupEvent event) {
        DoctorGroupEvent tmpEvent = event;
        while (event != null) {
            tmpEvent = event;
            event = doctorGroupEventDao.findByRelGroupEventId(event.getId());
        }
        return RespHelper.orFalse(doctorGroupReadService.isLastEvent(tmpEvent.getGroupId(), tmpEvent.getId()));
    }
}
