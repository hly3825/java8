package io.terminus.doctor.event.editHandler.pig;

import com.google.common.collect.Maps;
import io.terminus.doctor.common.exception.InvalidException;
import io.terminus.doctor.common.utils.Checks;
import io.terminus.doctor.event.dto.event.BasePigEventInputDto;
import io.terminus.doctor.event.dto.event.edit.DoctorEventChangeDto;
import io.terminus.doctor.event.dto.event.sow.DoctorMatingDto;
import io.terminus.doctor.event.enums.DoctorMatingType;
import io.terminus.doctor.event.enums.PigEvent;
import io.terminus.doctor.event.enums.PigStatus;
import io.terminus.doctor.event.model.DoctorDailyReport;
import io.terminus.doctor.event.model.DoctorPigEvent;
import io.terminus.doctor.event.model.DoctorPigTrack;
import io.terminus.doctor.event.util.EventUtil;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Objects;

import static io.terminus.doctor.event.editHandler.pig.DoctorModifyPigPregCheckEventHandler.PREG_CHECK_RESULT;

/**
 * Created by terminus on 2017/4/17.
 * 配种
 */
@Component
public class DoctorModifyPigMatingEventHandler extends DoctorAbstractModifyPigEventHandler{

    @Override
    public DoctorEventChangeDto buildEventChange(DoctorPigEvent oldPigEvent, BasePigEventInputDto inputDto) {
        return DoctorEventChangeDto.builder()
                .farmId(oldPigEvent.getFarmId())
                .businessId(oldPigEvent.getPigId())
                .oldEventAt(oldPigEvent.getEventAt())
                .newEventAt(inputDto.eventAt())
                .build();
    }

    @Override
    public DoctorPigEvent buildNewEvent(DoctorPigEvent oldPigEvent, BasePigEventInputDto inputDto) {
        DoctorPigEvent doctorPigEvent = super.buildNewEvent(oldPigEvent, inputDto);
        DoctorMatingDto newDto = (DoctorMatingDto) inputDto;
        doctorPigEvent.setMateType(newDto.getMatingType());
        doctorPigEvent.setBoarCode(newDto.getMatingBoarPigCode());
        doctorPigEvent.setOperatorName(newDto.getOperatorName());
        doctorPigEvent.setOperatorId(newDto.getOperatorId());
        return doctorPigEvent;
    }

    @Override
    protected void updateDailyForModify(DoctorPigEvent oldPigEvent, BasePigEventInputDto inputDto, DoctorEventChangeDto changeDto) {
        if (!Objects.equals(changeDto.getNewEventAt(), changeDto.getOldEventAt())) {
            //原日期记录更新
            updateDailyOfDelete(oldPigEvent);

            //新日记记录更新
            updateDailyOfNew(oldPigEvent, inputDto);
        }
    }

    @Override
    protected DoctorPigTrack buildNewTrackForRollback(DoctorPigEvent deletePigEvent, DoctorPigTrack oldPigTrack) {
        DoctorPigEvent beforeStatusEvent = doctorPigEventDao.getLastStatusEventBeforeEventAt(deletePigEvent.getPigId(), deletePigEvent.getEventAt());
        oldPigTrack.setCurrentMatingCount(EventUtil.minusInt(oldPigTrack.getCurrentMatingCount(), 1));
        Map<String, Object> extra = Maps.newHashMap();

        //之前,事件进场
        if (Objects.equals(beforeStatusEvent.getType(), PigEvent.ENTRY.getKey())) {
            oldPigTrack.setStatus(PigStatus.Entry.getKey());
            extra.put("enterToMate", true);
            oldPigTrack.setExtraMap(extra);
            return oldPigTrack;
        }

        //之前,事件配种
        if (Objects.equals(beforeStatusEvent.getType(), PigEvent.MATING.getKey())) {
            oldPigTrack.setStatus(PigStatus.Mate.getKey());
            return oldPigTrack;
        }

        //之前,事件妊娠检查
        if (Objects.equals(beforeStatusEvent.getType(), PigEvent.PREG_CHECK.getKey())) {
            DoctorPigEvent firstMateEvent = doctorPigEventDao.getFirstMateEvent(deletePigEvent.getPigId(), deletePigEvent.getEventAt());
            oldPigTrack.setStatus(PigStatus.KongHuai.getKey());
            extra.put("pregCheckResult", PREG_CHECK_RESULT.get(beforeStatusEvent.getPregCheckResult()));
            extra.put("judgePregDate", firstMateEvent.getJudgePregDate());
            oldPigTrack.setExtraMap(extra);
            return oldPigTrack;
        }

        //之前,事件断奶
        oldPigTrack.setStatus(PigStatus.Wean.getKey());
        oldPigTrack.setCurrentParity(EventUtil.minusInt(oldPigTrack.getCurrentParity(), 1));
        extra.put("hasWeanToMating", true);
        oldPigTrack.setExtraMap(extra);
        return oldPigTrack;
    }

    @Override
    protected void updateDailyForDelete(DoctorPigEvent deletePigEvent) {
        updateDailyOfDelete(deletePigEvent);
    }

    @Override
    public void updateDailyOfDelete(DoctorPigEvent oldPigEvent) {
        if (oldPigEvent.getCurrentMatingCount() > 1) {
            return;
        }
        DoctorDailyReport oldDailyPig1 = doctorDailyPigDao.findByFarmIdAndSumAt(oldPigEvent.getFarmId(), oldPigEvent.getEventAt());
        DoctorEventChangeDto changeDto1 = DoctorEventChangeDto.builder()
                .doctorMateType(oldPigEvent.getDoctorMateType())
                .doctorMateTypeCountChange(-1)
                .build();
        doctorDailyPigDao.update(buildDailyPig(oldDailyPig1, changeDto1));
    }

    @Override
    public void updateDailyOfNew(DoctorPigEvent newPigEvent, BasePigEventInputDto inputDto) {
        if (newPigEvent.getCurrentMatingCount() > 1) {
            return;
        }
        DoctorDailyReport oldDailyPig2 = doctorDailyPigDao.findByFarmIdAndSumAt(newPigEvent.getFarmId(), inputDto.eventAt());
        DoctorEventChangeDto changeDto2 = DoctorEventChangeDto.builder()
                .doctorMateType(newPigEvent.getDoctorMateType())
                .doctorMateTypeCountChange(1)
                .build();
        doctorDailyPigDao.update(buildDailyPig(oldDailyPig2, changeDto2));

    }

    @Override
    protected DoctorDailyReport buildDailyPig(DoctorDailyReport oldDailyPig, DoctorEventChangeDto changeDto) {
        oldDailyPig = super.buildDailyPig(oldDailyPig, changeDto);
        DoctorMatingType matingType = DoctorMatingType.from(changeDto.getDoctorMateType());
        Checks.expectNotNull(matingType, "mating.type.is.null");
        switch (matingType) {
            case HP:
                oldDailyPig.setMateHb(EventUtil.plusInt(oldDailyPig.getMateHb(), changeDto.getDoctorMateTypeCountChange()));
                break;
            case LPC:
                oldDailyPig.setMateLc(EventUtil.plusInt(oldDailyPig.getMateLc(), changeDto.getDoctorMateTypeCountChange()));
                break;
            case DP:
                oldDailyPig.setMateDn(EventUtil.plusInt(oldDailyPig.getMateDn(), changeDto.getDoctorMateTypeCountChange()));
                break;
            case YP:
                oldDailyPig.setMateYx(EventUtil.plusInt(oldDailyPig.getMateYx(), changeDto.getDoctorMateTypeCountChange()));
                break;
            case FP:
                oldDailyPig.setMateFq(EventUtil.plusInt(oldDailyPig.getMateFq(), changeDto.getDoctorMateTypeCountChange()));
                break;
            default:
                throw new InvalidException("mating.type.error");
        }
        return oldDailyPig;
    }
}
