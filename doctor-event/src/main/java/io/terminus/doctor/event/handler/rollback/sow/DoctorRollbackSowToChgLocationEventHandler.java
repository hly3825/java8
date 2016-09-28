package io.terminus.doctor.event.handler.rollback.sow;

import com.google.common.collect.Lists;
import io.terminus.doctor.event.dto.DoctorRollbackDto;
import io.terminus.doctor.event.dto.event.usual.DoctorChgLocationDto;
import io.terminus.doctor.event.enums.PigEvent;
import io.terminus.doctor.event.enums.RollbackType;
import io.terminus.doctor.event.handler.rollback.DoctorAbstractRollbackPigEventHandler;
import io.terminus.doctor.event.model.DoctorPigEvent;

import java.util.List;
import java.util.Objects;

/**
 * Created by xiao on 16/9/28.
 */
public class DoctorRollbackSowToChgLocationEventHandler extends DoctorAbstractRollbackPigEventHandler{
    @Override
    protected boolean handleCheck(DoctorPigEvent pigEvent) {
        return (Objects.equals(pigEvent.getType(), PigEvent.TO_MATING.getKey()) || Objects.equals(pigEvent.getType(), PigEvent.TO_FARROWING.getKey())) && isLastEvent(pigEvent);
    }

    @Override
    protected void handleRollback(DoctorPigEvent pigEvent, Long operatorId, String operatorName) {
        handleRollbackWithStatus(pigEvent, operatorId, operatorName);
    }

    @Override
    protected List<DoctorRollbackDto> handleReport(DoctorPigEvent pigEvent) {
        DoctorChgLocationDto dto = JSON_MAPPER.fromJson(pigEvent.getExtra(), DoctorChgLocationDto.class);
        DoctorRollbackDto doctorRollbackDto = DoctorRollbackDto.builder()
                .esBarnId(dto.getChgLocationFromBarnId())
                .esPigId(pigEvent.getPigId())
                .farmId(pigEvent.getFarmId())
                .rollbackTypes(Lists.newArrayList(RollbackType.SEARCH_BARN, RollbackType.SEARCH_PIG, RollbackType.DAILY_LIVESTOCK, RollbackType.MONTHLY_REPORT))
                .eventAt(pigEvent.getEventAt())
                .build();
        DoctorRollbackDto doctorRollbackDto1 = DoctorRollbackDto.builder()
                .esBarnId(dto.getChgLocationToBarnId())
                .farmId(pigEvent.getFarmId())
                .rollbackTypes(Lists.newArrayList(RollbackType.SEARCH_BARN))
                .eventAt(pigEvent.getEventAt())
                .build();
        return Lists.newArrayList(doctorRollbackDto, doctorRollbackDto1);
    }
}