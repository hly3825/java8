package io.terminus.doctor.event.editHandler.pig;

import io.terminus.common.utils.BeanMapper;
import io.terminus.doctor.event.dto.event.BasePigEventInputDto;
import io.terminus.doctor.event.dto.event.usual.DoctorDiseaseDto;
import io.terminus.doctor.event.model.DoctorPigEvent;

import static io.terminus.doctor.event.dto.DoctorBasicInputInfoDto.generateEventDescFromExtra;

/**
 * 疾病新事件的创建
 * Created by terminus on 2017/4/17.
 */
public class DoctorModifyDiseaseEventHandler extends DoctorAbstractModifyPigEventHandler{

    @Override
    public DoctorPigEvent buildNewEvent(DoctorPigEvent oldPigEvent, BasePigEventInputDto inputDto) {
        DoctorDiseaseDto diseaseDto = (DoctorDiseaseDto) inputDto;
        DoctorPigEvent doctorPigEvent = new DoctorPigEvent();
        BeanMapper.copy(oldPigEvent, doctorPigEvent);
        doctorPigEvent.setRemark(diseaseDto.getDiseaseRemark());
        doctorPigEvent.setEventAt(diseaseDto.eventAt());
        doctorPigEvent.setBasicId(diseaseDto.getDiseaseId());
        doctorPigEvent.setBasicName(diseaseDto.getDiseaseName());
        doctorPigEvent.setDesc(generateEventDescFromExtra(diseaseDto));
        doctorPigEvent.setExtra(TO_JSON_MAPPER.toJson(diseaseDto));
        return doctorPigEvent;
    }
}
