package io.terminus.doctor.move.builder;

import io.terminus.doctor.event.dto.event.BasePigEventInputDto;
import io.terminus.doctor.move.dto.DoctorMoveBasicData;
import io.terminus.doctor.move.model.View_EventListPig;

/**
 * Created by xjn on 17/8/4.
 * 事件输入构建器
 */
public interface DoctorPigEventInputBuilder {

    BasePigEventInputDto buildPigEventInput(DoctorMoveBasicData moveBasicData,
                                            View_EventListPig pigRawEvent);
}
