package io.terminus.doctor.event.handler.usual;

import io.terminus.doctor.common.enums.SourceType;
import io.terminus.doctor.common.exception.InvalidException;
import io.terminus.doctor.event.editHandler.pig.DoctorModifyPigRemoveEventHandler;
import io.terminus.doctor.event.enums.DoctorBasicEnums;
import io.terminus.doctor.event.dto.DoctorBasicInputInfoDto;
import io.terminus.doctor.event.dto.event.BasePigEventInputDto;
import io.terminus.doctor.event.dto.event.usual.DoctorRemovalDto;
import io.terminus.doctor.event.enums.IsOrNot;
import io.terminus.doctor.event.enums.PigStatus;
import io.terminus.doctor.event.handler.DoctorAbstractEventHandler;
import io.terminus.doctor.event.model.DoctorPig;
import io.terminus.doctor.event.model.DoctorPigEvent;
import io.terminus.doctor.event.model.DoctorPigTrack;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.joda.time.Days;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Objects;

import static io.terminus.common.utils.Arguments.notNull;
import static io.terminus.doctor.common.utils.Checks.expectTrue;

/**
 * Created by yaoqijun.
 * Date:2016-05-27
 * Email:yaoqj@terminus.io
 * Descirbe:
 */
@Component
@Slf4j
public class DoctorRemovalHandler extends DoctorAbstractEventHandler {
    @Autowired
    private DoctorModifyPigRemoveEventHandler modifyPigRemoveEventHandler;
    @Override
    public void handleCheck(DoctorPigEvent executeEvent, DoctorPigTrack fromTrack) {
        super.handleCheck(executeEvent, fromTrack);
        if (!Objects.equals(executeEvent.getEventSource(),SourceType.MOVE.getValue())){
            expectTrue(!Objects.equals(fromTrack.getStatus(), PigStatus.FEED.getKey()), "removal.status.not.feed");
        }
        expectTrue(!Objects.equals(fromTrack.getStatus(), PigStatus.Removal.getKey()), "pig.has.removed");
        expectTrue(!Objects.equals(fromTrack.getStatus(), PigStatus.BOAR_LEAVE.getKey()), "pig.has.removed");
    }

    @Override
    public DoctorPigTrack buildPigTrack(DoctorPigEvent executeEvent, DoctorPigTrack fromTrack) {
        DoctorPigTrack toTrack = super.buildPigTrack(executeEvent, fromTrack);
        //DoctorRemovalDto removalDto = (DoctorRemovalDto) inputDto;
        toTrack.setGroupId(-1L);
        //doctorPigTrack.addAllExtraMap(removalDto.toMap());
        //doctorPigTrack.addPigEvent(basic.getPigType(), (Long) context.get("doctorPigEventId"));
        if (Objects.equals(DoctorPig.PigSex.BOAR.getKey(), toTrack.getPigType())) {
            toTrack.setStatus(PigStatus.BOAR_LEAVE.getKey());
        } else if (Objects.equals(DoctorPig.PigSex.SOW.getKey(), toTrack.getPigType())) {
            toTrack.setStatus(PigStatus.Removal.getKey());
        } else {
            throw new InvalidException("pig.sex.error", toTrack.getPigType(),executeEvent.getPigCode());
        }
        toTrack.setIsRemoval(IsOrNot.YES.getValue());
        return toTrack;
    }

    @Override
    protected void specialHandle(DoctorPigEvent inputEvent, DoctorPigTrack currentTrack) {
        super.specialHandle(inputEvent, currentTrack);
       // ?????? ?????? ??????Pig ????????????
        DoctorPig doctorPig = doctorPigDao.findById(inputEvent.getPigId());
        expectTrue(notNull(doctorPig), "pig.not.null", inputEvent.getPigId());
        doctorPigDao.removalPig(doctorPig.getId());
    }

    @Override
    public DoctorPigEvent buildPigEvent(DoctorBasicInputInfoDto basic, BasePigEventInputDto inputDto) {
        DoctorPigEvent doctorPigEvent = super.buildPigEvent(basic, inputDto);
        DoctorRemovalDto removalDto = (DoctorRemovalDto) inputDto;
        doctorPigEvent.setWeight(removalDto.getWeight());
        doctorPigEvent.setCustomerId(removalDto.getCustomerId());
        doctorPigEvent.setCustomerName(removalDto.getCustomerName());

        DoctorPigTrack doctorPigTrack = doctorPigTrackDao.findByPigId(inputDto.getPigId());
        expectTrue(notNull(doctorPigTrack), "pig.track.not.null", inputDto.getPigId());

        doctorPigEvent.setChangeTypeId(removalDto.getChgTypeId());   //????????????id
        doctorPigEvent.setPrice(removalDto.getPrice());      //????????????(???)

        if (removalDto.getWeight() != null && removalDto.getPrice() != null) {
            doctorPigEvent.setAmount((long) (removalDto.getPrice() * removalDto.getWeight()));       //????????????(???)
        }

        if (Objects.equals(removalDto.getChgTypeId(), DoctorBasicEnums.DEAD.getId()) || Objects.equals(removalDto.getChgTypeId(), DoctorBasicEnums.ELIMINATE.getId())) {
            //??????????????? ????????????,??????????????????????????????
            DoctorPigEvent lastMate = doctorPigEventDao.queryLastFirstMate(doctorPigTrack.getPigId(),
                    doctorPigEventDao.findLastParity(doctorPigTrack.getPigId()));
            if (lastMate == null) {
                return doctorPigEvent;
            }
            DateTime mattingDate = new DateTime(lastMate.getEventAt());
            DateTime eventTime = new DateTime(doctorPigEvent.getEventAt());

            int npd = Math.abs(Days.daysBetween(eventTime, mattingDate).getDays());
            if (Objects.equals(removalDto.getChgTypeId(), DoctorBasicEnums.DEAD.getId())) {
                //???????????????
                doctorPigEvent.setPsnpd(doctorPigEvent.getPsnpd() + npd);
                doctorPigEvent.setNpd(doctorPigEvent.getNpd() + npd);
            }
            if (Objects.equals(removalDto.getChgTypeId(), DoctorBasicEnums.ELIMINATE.getId())) {
                //???????????????
                doctorPigEvent.setPtnpd(doctorPigEvent.getPtnpd() + npd);
                doctorPigEvent.setNpd(doctorPigEvent.getNpd() + npd);
            }
        }
        return doctorPigEvent;
    }

    @Override
    protected void updateDailyForNew(DoctorPigEvent newPigEvent) {
        BasePigEventInputDto newDto = JSON_MAPPER.fromJson(newPigEvent.getExtra(), DoctorRemovalDto.class);
        modifyPigRemoveEventHandler.updateDailyOfNew(newPigEvent, newDto);
    }
}
