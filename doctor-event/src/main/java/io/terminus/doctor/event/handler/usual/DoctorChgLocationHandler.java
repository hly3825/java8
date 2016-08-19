package io.terminus.doctor.event.handler.usual;

import io.terminus.common.exception.ServiceException;
import io.terminus.doctor.common.utils.Params;
import io.terminus.doctor.event.dao.DoctorBarnDao;
import io.terminus.doctor.event.dao.DoctorPigDao;
import io.terminus.doctor.event.dao.DoctorPigEventDao;
import io.terminus.doctor.event.dao.DoctorPigSnapshotDao;
import io.terminus.doctor.event.dao.DoctorPigTrackDao;
import io.terminus.doctor.event.dao.DoctorRevertLogDao;
import io.terminus.doctor.event.dto.DoctorBasicInputInfoDto;
import io.terminus.doctor.event.enums.PigEvent;
import io.terminus.doctor.event.handler.DoctorAbstractEventHandler;
import io.terminus.doctor.event.model.DoctorBarn;
import io.terminus.doctor.event.model.DoctorPigTrack;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Objects;

/**
 * Created by yaoqijun.
 * Date:2016-05-27
 * Email:yaoqj@terminus.io
 * Descirbe:
 */
@Component
public class DoctorChgLocationHandler extends DoctorAbstractEventHandler{

    private final DoctorBarnDao doctorBarnDao;

    @Autowired
    public DoctorChgLocationHandler(DoctorPigDao doctorPigDao, DoctorPigEventDao doctorPigEventDao, DoctorPigTrackDao doctorPigTrackDao, DoctorPigSnapshotDao doctorPigSnapshotDao, DoctorRevertLogDao doctorRevertLogDao, DoctorBarnDao doctorBarnDao) {
        super(doctorPigDao, doctorPigEventDao, doctorPigTrackDao, doctorPigSnapshotDao, doctorRevertLogDao);
        this.doctorBarnDao = doctorBarnDao;
    }

    @Override
    public Boolean preHandler(DoctorBasicInputInfoDto basic, Map<String, Object> extra, Map<String, Object> context) throws RuntimeException {
        return Objects.equals(basic.getEventType(), PigEvent.CHG_LOCATION.getKey());
    }

    @Override
    public DoctorPigTrack updateDoctorPigTrackInfo(DoctorPigTrack doctorPigTrack, DoctorBasicInputInfoDto basic, Map<String, Object> extra, Map<String, Object> context) {
        Long toBarnId = Params.getWithConvert(extra,"chgLocationFromBarnId",a->Long.valueOf(a.toString()));

        //校验猪舍类型是否相同, 只有同类型才可以普通转舍
        checkBarnTypeEqual(doctorPigTrack.getCurrentBarnId(), toBarnId);
        doctorPigTrack.setCurrentBarnId(toBarnId);
        doctorPigTrack.setCurrentBarnName(Params.getWithConvert(extra, "chgLocationFromBarnName", String::valueOf));
        doctorPigTrack.addAllExtraMap(extra);
        doctorPigTrack.addPigEvent(basic.getPigType(), (Long) context.get("doctorPigEventId"));
        return doctorPigTrack;
    }

    private void checkBarnTypeEqual(Long fromBarnId, Long toBarnId) {
        DoctorBarn fromBarn = doctorBarnDao.findById(fromBarnId);
        DoctorBarn toBarn = doctorBarnDao.findById(fromBarnId);
        if (fromBarn == null || toBarn == null || !Objects.equals(fromBarn.getPigType(), toBarn.getPigType())) {
            throw new ServiceException("barn.type.not.equal");
        }
    }
}
