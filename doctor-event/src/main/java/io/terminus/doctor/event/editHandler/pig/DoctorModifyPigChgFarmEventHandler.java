package io.terminus.doctor.event.editHandler.pig;

import io.terminus.doctor.event.model.DoctorPigEvent;
import org.springframework.stereotype.Component;

/**
 * Created by xjn on 17/4/19.
 * 转场
 */
@Component
public class DoctorModifyPigChgFarmEventHandler extends DoctorAbstractModifyPigEventHandler {
    @Override
    protected boolean rollbackHandleCheck(DoctorPigEvent deletePigEvent) {
        return false;
    }
}
