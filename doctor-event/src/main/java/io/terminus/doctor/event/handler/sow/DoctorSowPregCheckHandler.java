package io.terminus.doctor.event.handler.sow;

import io.terminus.doctor.common.enums.PigType;
import io.terminus.doctor.common.enums.SourceType;
import io.terminus.doctor.common.exception.InvalidException;
import io.terminus.doctor.event.dto.DoctorBasicInputInfoDto;
import io.terminus.doctor.event.dto.event.BasePigEventInputDto;
import io.terminus.doctor.event.dto.event.sow.DoctorPregChkResultDto;
import io.terminus.doctor.event.editHandler.pig.DoctorModifyPigPregCheckEventHandler;
import io.terminus.doctor.event.enums.IsOrNot;
import io.terminus.doctor.event.enums.KongHuaiPregCheckResult;
import io.terminus.doctor.event.enums.PigEvent;
import io.terminus.doctor.event.enums.PigStatus;
import io.terminus.doctor.event.enums.PregCheckResult;
import io.terminus.doctor.event.handler.DoctorAbstractEventHandler;
import io.terminus.doctor.event.model.DoctorPigEvent;
import io.terminus.doctor.event.model.DoctorPigTrack;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.joda.time.Days;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Objects;

import static io.terminus.common.utils.Arguments.notNull;
import static io.terminus.doctor.common.utils.Checks.expectTrue;

/**
 * Created by yaoqijun.
 * Date:2016-05-27
 * Email:yaoqj@terminus.io
 * Descirbe:
 */
@Slf4j
@Component
public class DoctorSowPregCheckHandler extends DoctorAbstractEventHandler {

    @Autowired
    private DoctorModifyPigPregCheckEventHandler doctorModifyPigPregCheckEventHandler;

    @Override
    public void handleCheck(DoctorPigEvent executeEvent, DoctorPigTrack fromTrack) {
        super.handleCheck(executeEvent, fromTrack);
        expectTrue(Objects.equals(fromTrack.getStatus(), PigStatus.Mate.getKey())
                        || Objects.equals(fromTrack.getStatus(), PigStatus.KongHuai.getKey())
                        || Objects.equals(fromTrack.getStatus(), PigStatus.Pregnancy.getKey())
                        || Objects.equals(fromTrack.getStatus(), PigStatus.Farrow.getKey())
                ,"pig.status.failed", PigEvent.from(executeEvent.getType()).getName(), PigStatus.from(fromTrack.getStatus()).getName());
        DoctorPregChkResultDto pregChkResultDto = JSON_MAPPER.fromJson(executeEvent.getExtra(), DoctorPregChkResultDto.class);

        // TODO: 17/6/1 ???????????????????????????????????? 
//        if (Objects.equals(executeEvent.getPregCheckResult(), PregCheckResult.LIUCHAN.getKey())) {
//            expectTrue(notNull(pregChkResultDto.getAbortionReasonId()), "liuchan.reason.not.null", pregChkResultDto.getPigCode());
//        }
        if (Objects.equals(executeEvent.getIsModify(), IsOrNot.NO.getValue())
                && !Objects.equals(executeEvent.getEventSource(), SourceType.MOVE.getValue())) {
            checkCanPregCheckResult(fromTrack.getStatus(), pregChkResultDto.getCheckResult(), pregChkResultDto.getPigCode());
        }
    }

    @Override
    public DoctorPigEvent buildPigEvent(DoctorBasicInputInfoDto basic, BasePigEventInputDto inputDto) {
        DoctorPigEvent doctorPigEvent = super.buildPigEvent(basic, inputDto);
        DoctorPregChkResultDto pregChkResultDto = (DoctorPregChkResultDto) inputDto;
        DoctorPigTrack doctorPigTrack = doctorPigTrackDao.findByPigId(pregChkResultDto.getPigId());
        expectTrue(notNull(doctorPigTrack), "pig.track.not.null", inputDto.getPigId());
        //????????????????????????extra????????????
        Integer pregCheckResult = pregChkResultDto.getCheckResult();
        doctorPigEvent.setPregCheckResult(pregCheckResult);

        //????????????????????????
        DateTime checkDate = new DateTime(pregChkResultDto.eventAt());
        doctorPigEvent.setCheckDate(checkDate.toDate());

        //??????????????????????????????
        DoctorPigEvent lastMate = doctorPigEventDao.queryLastFirstMate(doctorPigTrack.getPigId(),
                doctorPigEventDao.findLastParity(doctorPigTrack.getPigId()));

        expectTrue(notNull(lastMate), "preg.last.mate.not.null", inputDto.getPigId());
        if (!Objects.equals(pregCheckResult, PregCheckResult.YANG.getKey())) {
            DateTime mattingDate = new DateTime(lastMate.getEventAt());
            int npd = Math.abs(Days.daysBetween(checkDate, mattingDate).getDays());

            if (Objects.equals(pregCheckResult, PregCheckResult.FANQING.getKey())) {
                //???????????????pfNPD
                doctorPigEvent.setPfnpd(doctorPigEvent.getPfnpd() + npd);
                doctorPigEvent.setNpd(doctorPigEvent.getNpd() + npd);
            } else if (Objects.equals(pregCheckResult, PregCheckResult.YING.getKey())) {
                //???????????????pyNPD
                doctorPigEvent.setPynpd(doctorPigEvent.getPynpd() + npd);
                doctorPigEvent.setNpd(doctorPigEvent.getNpd() + npd);
            } else if (Objects.equals(pregCheckResult, PregCheckResult.LIUCHAN.getKey())) {
                //???????????????plNPD
                doctorPigEvent.setBasicId(pregChkResultDto.getAbortionReasonId());
                doctorPigEvent.setBasicName(pregChkResultDto.getAbortionReasonName());
                doctorPigEvent.setPlnpd(doctorPigEvent.getPlnpd() + npd);
                doctorPigEvent.setNpd(doctorPigEvent.getNpd() + npd);
            }

        }


        //??????????????????, ???????????????: ?????? => ??????, ?????????????????????????????????
        if (Objects.equals(doctorPigTrack.getStatus(), PigStatus.KongHuai.getKey())) {
            DoctorPigEvent lastPregEvent = doctorPigEventDao.queryLastPregCheck(doctorPigTrack.getPigId());
            expectTrue(notNull(lastPregEvent), "preg.check.event.not.null", pregChkResultDto.getPigId());
            expectTrue(PregCheckResult.KONGHUAI_RESULTS.contains(lastPregEvent.getPregCheckResult()), "preg.check.result.error",
                    lastPregEvent.getPregCheckResult(), pregChkResultDto.getPigCode());
            doctorPigEventDao.delete(lastPregEvent.getId());
            if (!Objects.equals(doctorPigEvent.getEventSource(), SourceType.MOVE.getValue())){
                doctorModifyPigPregCheckEventHandler.updateDailyOfDelete(lastPregEvent);
            }
        }

        return doctorPigEvent;
    }

    @Override
    protected void specialHandle(DoctorPigEvent doctorPigEvent, DoctorPigTrack doctorPigTrack){
        super.specialHandle(doctorPigEvent, doctorPigTrack);
        if (Objects.equals(doctorPigTrack.getStatus(), PigStatus.Pregnancy.getKey())) {
            //???????????????????????? ????????????????????? ??? isImpregnation ????????????true
            DoctorPigEvent firstMate = doctorPigEventDao.queryLastFirstMate(doctorPigTrack.getPigId()
                    , doctorPigEventDao.findLastParity(doctorPigTrack.getPigId()));

            expectTrue(notNull(firstMate), "first.mate.not.null", doctorPigEvent.getPigId());
            firstMate.setIsImpregnation(1);
            doctorPigEventDao.update(firstMate);
        }
    }

    @Override
    public DoctorPigTrack buildPigTrack(DoctorPigEvent executeEvent, DoctorPigTrack fromTrack) {
        DoctorPigTrack toTrack = super.buildPigTrack(executeEvent, fromTrack);

        Integer pregCheckResult = executeEvent.getPregCheckResult();

        //?????????????????????0
        toTrack.setCurrentMatingCount(0);

        Map<String, Object> extra = toTrack.getExtraMap();
        //???extra???????????????????????????????????????????????????
        if (Objects.equals(pregCheckResult, PregCheckResult.FANQING.getKey())) {
            extra.put("fanqingToMate", true);
            extra.put("pregCheckResult", KongHuaiPregCheckResult.FANQING.getKey());
        } else if (Objects.equals(pregCheckResult, PregCheckResult.YING.getKey())) {
            extra.put("yinToMate", true);
            extra.put("pregCheckResult", KongHuaiPregCheckResult.YING.getKey());
        } else if (Objects.equals(pregCheckResult, PregCheckResult.LIUCHAN.getKey())) {
            extra.put("liuchanToMateCheck", true);
            extra.put("pregCheckResult", KongHuaiPregCheckResult.LIUCHAN.getKey());
        }else if (Objects.equals(pregCheckResult, PregCheckResult.YANG.getKey())){
            extra.put("pregCheckResult", PigStatus.Pregnancy.getKey());
        }

        toTrack.setExtraMap(extra);

        if (Objects.equals(pregCheckResult, PregCheckResult.UNSURE.getKey())) {
            // ???????????????
        } else if (Objects.equals(pregCheckResult, PregCheckResult.YANG.getKey())) {
            // ??????
            toTrack.setStatus(PigStatus.Pregnancy.getKey());

            // ????????????????????????????????????
            if (Objects.equals(toTrack.getCurrentBarnType(), PigType.DELIVER_SOW.getValue())) {
                toTrack.setStatus(PigStatus.Farrow.getKey());
            }

        } else {
            // ???????????? ????????????
            toTrack.setStatus(PigStatus.KongHuai.getKey());
        }
        //doctorPigTrack.addPigEvent(basic.getPigType(), (Long) context.get("doctorPigEventId"));
        return toTrack;
    }

    @Override
    protected void updateDailyForNew(DoctorPigEvent newPigEvent) {
        BasePigEventInputDto inputDto = JSON_MAPPER.fromJson(newPigEvent.getExtra(), DoctorPregChkResultDto.class);
        doctorModifyPigPregCheckEventHandler.updateDailyOfNew(newPigEvent, inputDto);
    }

    //???????????????????????????????????????
    private static void checkCanPregCheckResult(Integer pigStatus, Integer checkResult, String pigCode) {
        //???????????????????????????
        if (Objects.equals(pigStatus, PigStatus.Mate.getKey())) {
            return;
        }

        //??????(?????????)?????????????????????
        if (Objects.equals(pigStatus, PigStatus.Pregnancy.getKey()) || Objects.equals(pigStatus, PigStatus.Farrow.getKey())) {
            if (!PregCheckResult.KONGHUAI_RESULTS.contains(checkResult)) {
                throw new InvalidException("pregnancy.only.to.konghuai", PregCheckResult.from(checkResult).getDesc(), pigCode);
            }
            return;
        }

        //????????????????????????????????????
        if (Objects.equals(pigStatus, PigStatus.KongHuai.getKey())) {
            if (!Objects.equals(checkResult, PregCheckResult.YANG.getKey())) {
                throw new InvalidException("konghuai.only.to.pregnancy", PregCheckResult.from(checkResult).getDesc(), pigCode);
            }
            return;
        }
        //???????????? ?????????, ????????????????????????, ?????????????????????
        throw new InvalidException("sow.not.allow.preg.check", PigStatus.from(pigStatus).getName(), pigCode);
    }
}
