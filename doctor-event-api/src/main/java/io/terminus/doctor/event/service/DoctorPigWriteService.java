package io.terminus.doctor.event.service;

import io.terminus.common.model.Response;
import io.terminus.doctor.event.model.DoctorPig;
import io.terminus.doctor.event.model.DoctorPigEvent;
import io.terminus.doctor.event.model.DoctorPigSnapshot;
import io.terminus.doctor.event.model.DoctorPigTrack;

/**
 * Created by yaoqijun.
 * Date:2016-05-13
 * Email:yaoqj@terminus.io
 * Descirbe: pig 信息内容
 */
public interface DoctorPigWriteService {

    Response<Long> createPig(DoctorPig pig);
    Response<Long> createPigTrack(DoctorPigTrack pigTrack);
    Response<Long> createPigEvent(DoctorPigEvent pigEvent);
    Response<Long> createPigSnapShot(DoctorPigSnapshot pigSnapshot);
}
