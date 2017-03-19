package io.terminus.doctor.event.manager;

import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.utils.Arguments;
import io.terminus.common.utils.BeanMapper;
import io.terminus.doctor.common.enums.PigType;
import io.terminus.doctor.common.utils.DateUtil;
import io.terminus.doctor.common.utils.JsonMapperUtil;
import io.terminus.doctor.event.dao.DoctorEventRelationDao;
import io.terminus.doctor.event.dao.DoctorGroupEventDao;
import io.terminus.doctor.event.dao.DoctorGroupTrackDao;
import io.terminus.doctor.event.dao.DoctorPigEventDao;
import io.terminus.doctor.event.dto.DoctorBasicInputInfoDto;
import io.terminus.doctor.event.dto.event.group.DoctorTurnSeedGroupEvent;
import io.terminus.doctor.event.dto.event.usual.DoctorFarmEntryDto;
import io.terminus.doctor.event.enums.BoarEntryType;
import io.terminus.doctor.event.enums.EventStatus;
import io.terminus.doctor.event.enums.IsOrNot;
import io.terminus.doctor.event.enums.PigEvent;
import io.terminus.doctor.event.enums.PigSource;
import io.terminus.doctor.event.handler.group.DoctorGroupEventHandlers;
import io.terminus.doctor.event.handler.usual.DoctorEntryHandler;
import io.terminus.doctor.event.model.DoctorEventRelation;
import io.terminus.doctor.event.model.DoctorGroupEvent;
import io.terminus.doctor.event.model.DoctorGroupTrack;
import io.terminus.doctor.event.model.DoctorPig;
import io.terminus.doctor.event.model.DoctorPigEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

/**
 * Created by IntelliJ IDEA.
 * Author: luoys
 * Date: 11:38 17/3/9
 */

@Slf4j
@Component
public class DoctorEditGroupEventManager {

    private DoctorGroupEventHandlers doctorGroupEventHandlers;
    private DoctorGroupEventDao doctorGroupEventDao;
    private DoctorGroupTrackDao doctorGroupTrackDao;
    private DoctorPigEventDao doctorPigEventDao;
    private DoctorEntryHandler doctorEntryHandler;
    private DoctorEventRelationDao doctorEventRelationDao;
    private static JsonMapperUtil JSON_MAPPER = JsonMapperUtil.JSON_NON_DEFAULT_MAPPER;

    @Autowired
    public DoctorEditGroupEventManager(DoctorGroupEventHandlers doctorGroupEventHandlers,
                                       DoctorGroupEventDao doctorGroupEventDao,
                                       DoctorGroupTrackDao doctorGroupTrackDao,
                                       DoctorPigEventDao doctorPigEventDao,
                                       DoctorEntryHandler doctorEntryHandler,
                                       DoctorEventRelationDao doctorEventRelationDao){
        this.doctorGroupEventHandlers = doctorGroupEventHandlers;
        this.doctorGroupEventDao = doctorGroupEventDao;
        this.doctorGroupTrackDao = doctorGroupTrackDao;
        this.doctorPigEventDao = doctorPigEventDao;
        this.doctorEntryHandler = doctorEntryHandler;
        this.doctorEventRelationDao= doctorEventRelationDao;
    }

    @Transactional
    public Boolean updateDoctorGroupEventStatus(List<Long> doctorGroupEventList, Integer status){
        return doctorGroupEventDao.updateGroupEventStatus(doctorGroupEventList, status);
    }

    @Transactional
    public DoctorGroupTrack elicitDoctorGroupTrack(List<DoctorGroupEvent> triggerDoctorGroupEventList, List<Long> doctorGroupEventList, DoctorGroupTrack doctorGroupTrack, DoctorGroupEvent newEvent){
        DoctorGroupEvent oldEvent = new DoctorGroupEvent();
        BeanMapper.copy(newEvent, oldEvent);
        return doctorGroupEventHandlers.getEventHandlerMap().get(newEvent.getType()).editGroupEvent(triggerDoctorGroupEventList, doctorGroupEventList, doctorGroupTrack, oldEvent, newEvent);
    }


    @Transactional
    public Boolean rollbackElicitEvents(List<DoctorGroupTrack> doctorGroupTrackList, List<Long> newDoctorGroupEvents, List<Long> oldDoctorGroupEvents) {
        Boolean status = doctorGroupEventDao.updateGroupEventStatus(oldDoctorGroupEvents, EventStatus.VALID.getValue());
        status = status && doctorGroupEventDao.updateGroupEventStatus(newDoctorGroupEvents, EventStatus.INVALID.getValue());
        doctorGroupTrackList.forEach(doctorGroupTrack -> doctorGroupTrackDao.update(doctorGroupTrack));
        //回滚关联关系
        doctorEventRelationDao.updateStatusUnderHandling(oldDoctorGroupEvents, DoctorEventRelation.Status.VALID.getValue());
        doctorEventRelationDao.batchUpdateStatus(newDoctorGroupEvents, DoctorEventRelation.Status.INVALID.getValue());
        return status;
    }

    public Boolean triggerPigEvents(DoctorGroupEvent doctorGroupEvent){
        DoctorPigEvent oldPigEvent = doctorPigEventDao.findById(doctorGroupEvent.getRelPigEventId());
        if(Arguments.isNull(oldPigEvent)){
            log.info("find pigEvent failed, doctorGroupEvent = {}", doctorGroupEvent);
            throw new JsonResponseException("find.pigEvent.failed");
        }
        DoctorBasicInputInfoDto basicDto = new DoctorBasicInputInfoDto();
        DoctorFarmEntryDto farmEntryDto = new DoctorFarmEntryDto();
        DoctorTurnSeedGroupEvent trunSeedGroupEvent = JSON_MAPPER.fromJson(doctorGroupEvent.getExtra(), DoctorTurnSeedGroupEvent.class);

        ///恭母猪进场字段
        if (Objects.equals(getSex(trunSeedGroupEvent.getToBarnType()), DoctorPig.PigSex.BOAR)) {
            farmEntryDto.setPigType(DoctorPig.PigSex.BOAR.getKey());
            farmEntryDto.setBoarType(BoarEntryType.HGZ.getKey());
            farmEntryDto.setBoarTypeName(BoarEntryType.HGZ.getCode());
        } else {
            farmEntryDto.setPigType(DoctorPig.PigSex.SOW.getKey());
            farmEntryDto.setParity(1);
            farmEntryDto.setEarCode(trunSeedGroupEvent.getEarCode());
        }

        //基本信息
        farmEntryDto.setRelGroupEventId(doctorGroupEvent.getId());
        farmEntryDto.setPigId(oldPigEvent.getPigId());
        farmEntryDto.setPigCode(trunSeedGroupEvent.getPigCode());
        farmEntryDto.setBarnId(trunSeedGroupEvent.getToBarnId());
        farmEntryDto.setBarnName(trunSeedGroupEvent.getToBarnName());
        basicDto.setFarmId(doctorGroupEvent.getFarmId());
        basicDto.setFarmName(doctorGroupEvent.getFarmName());
        basicDto.setOrgId(doctorGroupEvent.getOrgId());
        basicDto.setOrgName(doctorGroupEvent.getOrgName());
        farmEntryDto.setEventType(PigEvent.ENTRY.getKey());
        farmEntryDto.setEventName(PigEvent.ENTRY.getName());
        farmEntryDto.setEventDesc(PigEvent.ENTRY.getDesc());
        basicDto.setStaffId(doctorGroupEvent.getCreatorId());
        basicDto.setStaffName(doctorGroupEvent.getCreatorName());
        farmEntryDto.setIsAuto(IsOrNot.YES.getValue());

        //进场信息
        //farmEntryDto.setPigType(basicDto.getPigType());
        farmEntryDto.setPigCode(trunSeedGroupEvent.getPigCode());
        farmEntryDto.setBirthday(DateUtil.toDate(trunSeedGroupEvent.getBirthDate()));
        farmEntryDto.setInFarmDate(doctorGroupEvent.getEventAt());
        farmEntryDto.setBarnId(trunSeedGroupEvent.getToBarnId());
        farmEntryDto.setBarnName(trunSeedGroupEvent.getToBarnName());
        farmEntryDto.setSource(PigSource.LOCAL.getKey());
        farmEntryDto.setBreed(trunSeedGroupEvent.getBreedId());
        farmEntryDto.setBreedName(trunSeedGroupEvent.getBreedName());
        farmEntryDto.setBreedType(trunSeedGroupEvent.getGeneticId());
        farmEntryDto.setBreedTypeName(trunSeedGroupEvent.getGeneticName());
        farmEntryDto.setMotherCode(trunSeedGroupEvent.getMotherEarCode());
        farmEntryDto.setEarCode(trunSeedGroupEvent.getEarCode());
        farmEntryDto.setWeight(trunSeedGroupEvent.getWeight());

        DoctorPigEvent pigEvent = doctorEntryHandler.buildPigEvent(basicDto, farmEntryDto);
        return true;
    }

    //获取转种猪性别
    private static DoctorPig.PigSex getSex(Integer toBarnType) {
        if (PigType.MATING_TYPES.contains(toBarnType)) {
            return DoctorPig.PigSex.SOW;
        }
        return DoctorPig.PigSex.BOAR;
    }
}