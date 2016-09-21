package io.terminus.doctor.event.handler.rollback;

import io.terminus.doctor.common.utils.RespHelper;
import io.terminus.doctor.event.dto.DoctorRollbackDto;
import io.terminus.doctor.event.enums.IsOrNot;
import io.terminus.doctor.event.enums.RollbackType;
import io.terminus.doctor.event.handler.DoctorRollbackGroupEventHandler;
import io.terminus.doctor.event.model.DoctorGroupEvent;
import io.terminus.doctor.event.model.DoctorRevertLog;
import io.terminus.doctor.event.service.DoctorGroupReadService;
import io.terminus.doctor.event.service.DoctorRevertLogWriteService;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

/**
 * Desc: 猪群事件回滚处理器
 * Mail: yangzl@terminus.io
 * author: DreamYoung
 * Date: 16/9/20
 */
@Slf4j
public abstract class DoctorAbstractRollbackGroupEventHandler extends DoctorAbstrackRollbackReportHandler implements DoctorRollbackGroupEventHandler {

    @Autowired protected DoctorGroupReadService doctorGroupReadService;
    @Autowired private DoctorRevertLogWriteService doctorRevertLogWriteService;

    /**
     * 判断能否回滚(1.手动事件 2.三个月内的事件 3.最新事件 4.子类根据事件类型特殊处理)
     */
    @Override
    public final boolean canRollback(DoctorGroupEvent groupEvent) {
        return Objects.equals(groupEvent.getIsAuto(), IsOrNot.YES.getValue()) &&
                groupEvent.getEventAt().after(DateTime.now().plusMonths(-3).toDate()) &&
                RespHelper.orFalse(doctorGroupReadService.isLastEvent(groupEvent.getGroupId(), groupEvent.getId())) &&
                handleCheck(groupEvent);
    }

    /**
     * 带事务的回滚操作
     */
    @Override @Transactional
    public final void rollback(DoctorGroupEvent groupEvent) {
        DoctorRevertLog revertLog = handleRollback(groupEvent);
        RespHelper.orServEx(doctorRevertLogWriteService.createRevertLog(revertLog));
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
     * 处理回滚操作
     */
    protected abstract DoctorRevertLog handleRollback(DoctorGroupEvent groupEvent);

    /**
     * 需要更新的统计
     * @see RollbackType
     */
    protected abstract DoctorRollbackDto handleReport(DoctorGroupEvent groupEvent);
}
