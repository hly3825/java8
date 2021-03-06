package io.terminus.doctor.event.handler.usual;

import com.google.common.collect.Maps;
import io.terminus.common.utils.MapBuilder;
import io.terminus.doctor.common.enums.PigType;
import io.terminus.doctor.common.enums.SourceType;
import io.terminus.doctor.common.exception.InvalidException;
import io.terminus.doctor.event.cache.DoctorPigInfoCache;
import io.terminus.doctor.event.constants.DoctorFarmEntryConstants;
import io.terminus.doctor.event.dto.DoctorBasicInputInfoDto;
import io.terminus.doctor.event.dto.event.BasePigEventInputDto;
import io.terminus.doctor.event.dto.event.DoctorEventInfo;
import io.terminus.doctor.event.dto.event.usual.DoctorFarmEntryDto;
import io.terminus.doctor.event.editHandler.pig.DoctorModifyPigEntryEventHandler;
import io.terminus.doctor.event.enums.IsOrNot;
import io.terminus.doctor.event.enums.PigStatus;
import io.terminus.doctor.event.handler.DoctorAbstractEventHandler;
import io.terminus.doctor.event.model.DoctorBarn;
import io.terminus.doctor.event.model.DoctorPig;
import io.terminus.doctor.event.model.DoctorPigEvent;
import io.terminus.doctor.event.model.DoctorPigTrack;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.joda.time.Years;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import static io.terminus.common.utils.Arguments.isNull;
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
public class DoctorEntryHandler extends DoctorAbstractEventHandler{

    @Autowired
    private  DoctorPigInfoCache doctorPigInfoCache;
    @Autowired
    private DoctorModifyPigEntryEventHandler doctorModifyPigEntryEventHandler;

    @Override
    public void handleCheck(DoctorPigEvent executeEvent, DoctorPigTrack fromTrack) {
        super.handleCheck(executeEvent, fromTrack);
        expectTrue((Objects.equals(executeEvent.getKind(), DoctorPig.PigSex.SOW.getKey())
                        && PigType.MATING_TYPES.contains(executeEvent.getBarnType()))
                || (Objects.equals(executeEvent.getKind(), DoctorPig.PigSex.BOAR.getKey())
                        && Objects.equals(executeEvent.getBarnType(), PigType.BOAR.getValue()))
                , "entry.barn.type.error", PigType.from(executeEvent.getBarnType()).getDesc());
    }

    @Override
    public void handle(List<DoctorEventInfo> doctorEventInfoList, DoctorPigEvent executeEvent, DoctorPigTrack fromTrack) {

//        //????????????????????????????????????????????????
//        if (Objects.equals(executeEvent.getIsModify(), IsOrNot.YES.getValue())) {
//            DoctorPig updatePig = doctorPigDao.findById(executeEvent.getPigId());
//            doctorPigDao.update(buildUpdatePig(executeEvent, updatePig));
//        }

        if (isNull(executeEvent.getEventSource())
                || Objects.equals(executeEvent.getEventSource(), SourceType.INPUT.getValue())) {
            String key = executeEvent.getFarmId().toString() + executeEvent.getKind().toString() + executeEvent.getPigCode();
            expectTrue(doctorConcurrentControl.setKey(key), "event.concurrent.error", executeEvent.getPigCode());
        }

        //??????
        handleCheck(executeEvent, fromTrack);

        Long oldEventId = executeEvent.getId();
        //1.????????????
        doctorPigEventDao.create(executeEvent);

        //2.???????????????track
        DoctorPigTrack toTrack = buildPigTrack(executeEvent, fromTrack);

        //??????track
        doctorEventBaseHelper.validTrackAfterUpdate(toTrack);

        if (Objects.equals(executeEvent.getIsModify(), IsOrNot.YES.getValue())) {
            toTrack.setId(fromTrack.getId());
            doctorPigTrackDao.update(toTrack);
        } else {
            doctorPigTrackDao.create(toTrack);
        }
        //3.????????????
        specialHandle(executeEvent, toTrack);

        //4.????????????
        if (isNull(executeEvent.getEventSource())
                || Objects.equals(executeEvent.getEventSource(), SourceType.INPUT.getValue())) {
            updateDailyForNew(executeEvent);
        }

        //5.???????????????????????????
        DoctorEventInfo doctorEventInfo = DoctorEventInfo.builder()
                .orgId(executeEvent.getOrgId())
                .farmId(executeEvent.getFarmId())
                .eventId(executeEvent.getId())
                .eventAt(executeEvent.getEventAt())
                .kind(executeEvent.getKind())
                .mateType(executeEvent.getDoctorMateType())
                .pregCheckResult(executeEvent.getPregCheckResult())
                .businessId(executeEvent.getPigId())
                .code(executeEvent.getPigCode())
                .status(toTrack.getStatus())
                .businessType(DoctorEventInfo.Business_Type.PIG.getValue())
                .eventType(executeEvent.getType())
                .build();
        doctorEventInfoList.add(doctorEventInfo);

        //?????????????????????track snapshot
        createTrackSnapshot(executeEvent);
    }

    /**
     * ??????DoctorPig
     *
     * @param dto ????????????
     * @param basic ????????????
     * @return ???
     */
    private DoctorPig buildDoctorPig(DoctorFarmEntryDto dto, DoctorBasicInputInfoDto basic) {
        DoctorPig doctorPig = DoctorPig.builder()
                .farmId(basic.getFarmId())
                .farmName(basic.getFarmName())
                .orgId(basic.getOrgId())
                .orgName(basic.getOrgName())
                .pigCode(dto.getPigCode())
                .rfid(dto.getRfid())
                .origin(dto.getOrigin())
                .pigType(dto.getPigType())
                .isRemoval(IsOrNot.NO.getValue())
                .pigFatherCode(dto.getFatherCode())
                .pigMotherCode(dto.getMotherCode())
                .source(dto.getSource())
                .birthDate(generateEventAt(dto.getBirthday()))
                .inFarmDate(generateEventAt(dto.getInFarmDate()))
                .inFarmDayAge(Years.yearsBetween(new DateTime(dto.getBirthday()), DateTime.now()).getYears())
                .initBarnId(dto.getBarnId())
                .initBarnName(dto.getBarnName())
                .breedId(dto.getBreed())
                .breedName(dto.getBreedName())
                .geneticId(dto.getBreedType())
                .geneticName(dto.getBreedTypeName())
                .boarType(dto.getBoarType())
                .remark(dto.getEntryMark())
                .creatorId(basic.getStaffId())
                .creatorName(basic.getStaffName())
                .outId(dto.getPigOutId())
                .build();
        if (Objects.equals(dto.getPigType(), DoctorPig.PigSex.SOW.getKey())) {
            // add sow pig info
            Map<String, Object> extraMapInfo = Maps.newHashMap();
            extraMapInfo.put(DoctorFarmEntryConstants.EAR_CODE, dto.getEarCode());
            extraMapInfo.put(DoctorFarmEntryConstants.FIRST_PARITY, dto.getParity());
            extraMapInfo.put(DoctorFarmEntryConstants.LEFT_COUNT, dto.getLeft());
            extraMapInfo.put(DoctorFarmEntryConstants.RIGHT_COUNT, dto.getRight());
            doctorPig.setExtraMap(extraMapInfo);
        }
        return doctorPig;
    }

    /**
     * ???????????????(??????????????????)
     * @param executeEvent ?????????????????????
     * @param pig ????????????
     * @return ??????????????????
     */
    private DoctorPig buildUpdatePig(DoctorPigEvent executeEvent, DoctorPig pig) {
        DoctorFarmEntryDto farmEntryDto = JSON_MAPPER.fromJson(executeEvent.getExtra(), DoctorFarmEntryDto.class);
        pig.setBirthDate(farmEntryDto.getBirthday());
        pig.setInFarmDate(executeEvent.getEventAt());
        pig.setPigCode(executeEvent.getPigCode());
        pig.setBreedId(farmEntryDto.getBreed());
        pig.setBreedName(farmEntryDto.getBreedName());
        pig.setPigFatherCode(farmEntryDto.getFatherCode());
        pig.setPigMotherCode(farmEntryDto.getMotherCode());
        pig.setSource(farmEntryDto.getSource());
        pig.setBoarType(farmEntryDto.getBoarType());
        return pig;
    }

    @Override
    public DoctorPigEvent buildPigEvent(DoctorBasicInputInfoDto basic, BasePigEventInputDto inputDto) {
        DoctorFarmEntryDto farmEntryDto = (DoctorFarmEntryDto) inputDto;
        DoctorPig doctorPig = buildDoctorPig(farmEntryDto, basic);
        if (isNull(farmEntryDto.getPigId())) {
            expectTrue(isNull(doctorPigDao.findPigByFarmIdAndPigCodeAndSex(basic.getFarmId(), farmEntryDto.getPigCode(), farmEntryDto.getPigType())), "pigCode.have.existed");
            doctorPigDao.create(doctorPig);
        } else {
            doctorPigDao.update(doctorPig);
        }
        farmEntryDto.setPigId(doctorPig.getId());
        DoctorPigEvent doctorPigEvent =  super.buildPigEvent(basic, farmEntryDto);
        doctorPigEvent.setOrigin(farmEntryDto.getOrigin());
        doctorPigEvent.setParity(farmEntryDto.getParity());
        doctorPigEvent.setSource(farmEntryDto.getSource());
        doctorPigEvent.setBoarType(farmEntryDto.getBoarType());
        doctorPigEvent.setBreedId(farmEntryDto.getBreed());
        doctorPigEvent.setBreedName(farmEntryDto.getBreedName());
        doctorPigEvent.setBreedTypeId(farmEntryDto.getBreedType());
        doctorPigEvent.setBreedTypeName(farmEntryDto.getBreedTypeName());
        return doctorPigEvent;
    }

    /**
     * ??????????????? Track ?????????
     * @param executeEvent ????????????
     * @param fromTrack ????????? ?????????null(????????????),????????????????????????track
     * @return ???track
     */
    @Override
    public DoctorPigTrack buildPigTrack(DoctorPigEvent executeEvent, DoctorPigTrack fromTrack) {
        DoctorFarmEntryDto farmEntryDto = new DoctorFarmEntryDto();
        if (notNull(executeEvent.getExtra())) {
            farmEntryDto = JSON_MAPPER.fromJson(executeEvent.getExtra(), DoctorFarmEntryDto.class);
        }
        DoctorBarn doctorBarn = doctorBarnDao.findById(executeEvent.getBarnId());
        expectTrue(notNull(doctorBarn), "barn.not.null", executeEvent.getBarnId());
        DoctorPigTrack doctorPigTrack = DoctorPigTrack.builder().farmId(executeEvent.getFarmId())
                .isRemoval(IsOrNot.NO.getValue()).currentMatingCount(0)
                .pigId(executeEvent.getPigId()).pigType(executeEvent.getKind())
                .currentBarnId(doctorBarn.getId()).currentBarnName(doctorBarn.getName())
                .currentBarnType(doctorBarn.getPigType()).currentParity(executeEvent.getParity())
                .weight(farmEntryDto.getWeight())
                .creatorId(executeEvent.getOperatorId()).creatorName(executeEvent.getOperatorName())
                .currentEventId(executeEvent.getId())
                .build();
        if (Objects.equals(executeEvent.getKind(), DoctorPig.PigSex.SOW.getKey())) {
            doctorPigTrack.setStatus(PigStatus.Entry.getKey());
        } else if (Objects.equals(executeEvent.getKind(), DoctorPig.PigSex.BOAR.getKey())) {
            doctorPigTrack.setStatus(PigStatus.BOAR_ENTRY.getKey());
        } else {
            throw new InvalidException("pig.sex.error", executeEvent.getKind());
        }
        //??????????????????????????????
        doctorPigTrack.addAllExtraMap(MapBuilder.<String, Object>of().put("enterToMate", true).map());
        return doctorPigTrack;
    }


    @Override
    protected void specialHandle(DoctorPigEvent executeEvent, DoctorPigTrack toTrack) {
        super.specialHandle(executeEvent, toTrack);
        doctorPigInfoCache.addPigCodeToFarm(executeEvent.getFarmId(), executeEvent.getPigCode());
    }

    @Override
    protected void updateDailyForNew(DoctorPigEvent newPigEvent) {
        BasePigEventInputDto inputDto = JSON_MAPPER.fromJson(newPigEvent.getExtra(), DoctorFarmEntryDto.class);
        doctorModifyPigEntryEventHandler.updateDailyOfNew(newPigEvent, inputDto);
    }
}
