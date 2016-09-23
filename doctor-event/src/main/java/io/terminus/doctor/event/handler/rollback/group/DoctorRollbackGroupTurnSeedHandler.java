package io.terminus.doctor.event.handler.rollback.group;

import com.google.common.collect.Lists;
import io.terminus.doctor.common.utils.RespHelper;
import io.terminus.doctor.event.dto.DoctorRollbackDto;
import io.terminus.doctor.event.enums.GroupEventType;
import io.terminus.doctor.event.enums.RollbackType;
import io.terminus.doctor.event.handler.rollback.DoctorAbstractRollbackGroupEventHandler;
import io.terminus.doctor.event.model.DoctorGroupEvent;
import io.terminus.doctor.event.model.DoctorPigEvent;
import io.terminus.doctor.event.model.DoctorRevertLog;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

/**
 * Desc: 商品猪转种猪事件回滚
 * Mail: yangzl@terminus.io
 * author: DreamYoung
 * Date: 16/9/20
 */
@Slf4j
@Component
public class DoctorRollbackGroupTurnSeedHandler extends DoctorAbstractRollbackGroupEventHandler {

    @Override
    protected boolean handleCheck(DoctorGroupEvent groupEvent) {
        //商品猪转种猪会触发猪的进场事件，所以需要校验猪的进场事件是否是最新事件
        if (!Objects.equals(groupEvent.getType(), GroupEventType.TRANS_GROUP.getValue())) {
            return false;
        }
        DoctorPigEvent toPigEvent = doctorPigEventDao.findByRelGroupEventId(groupEvent.getId());
        return RespHelper.orFalse(doctorPigEventReadService.isLastEvent(toPigEvent.getPigId(), toPigEvent.getId()));

    }

    @Override
    protected DoctorRevertLog handleRollback(DoctorGroupEvent groupEvent, Long operatorId, String operatorName) {
        // TODO: 2016/9/23 调用猪进场事件的回滚
        return sampleRollback(groupEvent);
    }

    @Override
    protected List<DoctorRollbackDto> handleReport(DoctorGroupEvent groupEvent) {
        DoctorRollbackDto fromDto = new DoctorRollbackDto();
        fromDto.setFarmId(groupEvent.getFarmId());
        fromDto.setEsBarnId(groupEvent.getBarnId());
        fromDto.setEsGroupId(groupEvent.getGroupId());

        //更新统计：存栏日报，存栏月报，猪舍统计，猪群统计
        fromDto.setRollbackTypes(Lists.newArrayList(RollbackType.DAILY_LIVESTOCK, RollbackType.MONTHLY_REPORT,
                RollbackType.SEARCH_BARN, RollbackType.SEARCH_GROUP));

        DoctorPigEvent toPigEvent = doctorPigEventDao.findByRelGroupEventId(groupEvent.getId());
        DoctorRollbackDto toDto = new DoctorRollbackDto();
        toDto.setFarmId(toPigEvent.getFarmId());
        toDto.setEsBarnId(toPigEvent.getBarnId());
        toDto.setEsPigId(toPigEvent.getPigId());

        //更新统计：猪舍，猪
        fromDto.setRollbackTypes(Lists.newArrayList(RollbackType.SEARCH_BARN, RollbackType.SEARCH_PIG));
        return Lists.newArrayList(fromDto, toDto);
    }
}
