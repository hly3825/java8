package io.terminus.doctor.move.service;

import com.google.common.base.Joiner;
import com.google.common.base.MoreObjects;
import com.google.common.base.Strings;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.PageInfo;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import io.terminus.common.utils.Arguments;
import io.terminus.common.utils.BeanMapper;
import io.terminus.common.utils.Joiners;
import io.terminus.doctor.basic.model.DoctorBasic;
import io.terminus.doctor.basic.model.DoctorBasicMaterial;
import io.terminus.doctor.basic.model.DoctorChangeReason;
import io.terminus.doctor.basic.model.DoctorCustomer;
import io.terminus.doctor.basic.service.DoctorMaterialConsumeProviderReadService;
import io.terminus.doctor.common.enums.PigType;
import io.terminus.doctor.common.enums.SourceType;
import io.terminus.doctor.common.utils.DateUtil;
import io.terminus.doctor.common.utils.JsonMapperUtil;
import io.terminus.doctor.common.utils.RespHelper;
import io.terminus.doctor.common.utils.ToJsonMapper;
import io.terminus.doctor.event.constants.DoctorFarmEntryConstants;
import io.terminus.doctor.event.dao.DoctorBarnDao;
import io.terminus.doctor.event.dao.DoctorGroupDao;
import io.terminus.doctor.event.dao.DoctorGroupEventDao;
import io.terminus.doctor.event.dao.DoctorGroupTrackDao;
import io.terminus.doctor.event.dao.DoctorPigDao;
import io.terminus.doctor.event.dao.DoctorPigEventDao;
import io.terminus.doctor.event.dao.DoctorPigTrackDao;
import io.terminus.doctor.event.dao.DoctorPigTypeStatisticDao;
import io.terminus.doctor.event.dto.DoctorGroupDetail;
import io.terminus.doctor.event.dto.DoctorGroupSearchDto;
import io.terminus.doctor.event.dto.event.boar.DoctorBoarConditionDto;
import io.terminus.doctor.event.dto.event.boar.DoctorSemenDto;
import io.terminus.doctor.event.dto.event.group.input.DoctorAntiepidemicGroupInput;
import io.terminus.doctor.event.dto.event.group.input.DoctorChangeGroupInput;
import io.terminus.doctor.event.dto.event.group.input.DoctorCloseGroupInput;
import io.terminus.doctor.event.dto.event.group.input.DoctorDiseaseGroupInput;
import io.terminus.doctor.event.dto.event.group.input.DoctorGroupInputInfo;
import io.terminus.doctor.event.dto.event.group.input.DoctorLiveStockGroupInput;
import io.terminus.doctor.event.dto.event.group.input.DoctorMoveInGroupInput;
import io.terminus.doctor.event.dto.event.group.input.DoctorNewGroupInput;
import io.terminus.doctor.event.dto.event.group.input.DoctorTransFarmGroupInput;
import io.terminus.doctor.event.dto.event.group.input.DoctorTransGroupInput;
import io.terminus.doctor.event.dto.event.group.input.DoctorTurnSeedGroupInput;
import io.terminus.doctor.event.dto.event.group.input.DoctorWeanGroupInput;
import io.terminus.doctor.event.dto.event.sow.DoctorFarrowingDto;
import io.terminus.doctor.event.dto.event.sow.DoctorFostersDto;
import io.terminus.doctor.event.dto.event.sow.DoctorMatingDto;
import io.terminus.doctor.event.dto.event.sow.DoctorPigletsChgDto;
import io.terminus.doctor.event.dto.event.sow.DoctorPregChkResultDto;
import io.terminus.doctor.event.dto.event.sow.DoctorWeanDto;
import io.terminus.doctor.event.dto.event.usual.DoctorChgFarmDto;
import io.terminus.doctor.event.dto.event.usual.DoctorChgLocationDto;
import io.terminus.doctor.event.dto.event.usual.DoctorConditionDto;
import io.terminus.doctor.event.dto.event.usual.DoctorDiseaseDto;
import io.terminus.doctor.event.dto.event.usual.DoctorFarmEntryDto;
import io.terminus.doctor.event.dto.event.usual.DoctorRemovalDto;
import io.terminus.doctor.event.dto.event.usual.DoctorVaccinationDto;
import io.terminus.doctor.event.enums.BoarEntryType;
import io.terminus.doctor.event.enums.DoctorBasicEnums;
import io.terminus.doctor.event.enums.DoctorMatingType;
import io.terminus.doctor.event.enums.EventStatus;
import io.terminus.doctor.event.enums.FarrowingType;
import io.terminus.doctor.event.enums.GroupEventType;
import io.terminus.doctor.event.enums.InType;
import io.terminus.doctor.event.enums.IsOrNot;
import io.terminus.doctor.event.enums.MatingType;
import io.terminus.doctor.event.enums.PigEvent;
import io.terminus.doctor.event.enums.PigSource;
import io.terminus.doctor.event.enums.PigStatus;
import io.terminus.doctor.event.enums.PregCheckResult;
import io.terminus.doctor.event.enums.VaccinResult;
import io.terminus.doctor.event.handler.sow.DoctorSowMatingHandler;
import io.terminus.doctor.event.manager.DoctorGroupEventManager;
import io.terminus.doctor.event.manager.DoctorGroupReportManager;
import io.terminus.doctor.event.manager.DoctorPigEventManager;
import io.terminus.doctor.event.model.DoctorBarn;
import io.terminus.doctor.event.model.DoctorGroup;
import io.terminus.doctor.event.model.DoctorGroupEvent;
import io.terminus.doctor.event.model.DoctorGroupTrack;
import io.terminus.doctor.event.model.DoctorPig;
import io.terminus.doctor.event.model.DoctorPigEvent;
import io.terminus.doctor.event.model.DoctorPigTrack;
import io.terminus.doctor.event.service.DoctorGroupBatchSummaryReadService;
import io.terminus.doctor.event.service.DoctorGroupBatchSummaryWriteService;
import io.terminus.doctor.event.service.DoctorPigReadService;
import io.terminus.doctor.event.util.EventUtil;
import io.terminus.doctor.move.handler.DoctorMoveDatasourceHandler;
import io.terminus.doctor.move.job.DoctorGroupBatchSummaryManager;
import io.terminus.doctor.move.model.DoctorSowFarrowWeight;
import io.terminus.doctor.move.model.Proc_InventoryGain;
import io.terminus.doctor.move.model.SowOutFarmSoon;
import io.terminus.doctor.move.model.View_BoarCardList;
import io.terminus.doctor.move.model.View_EventListBoar;
import io.terminus.doctor.move.model.View_EventListGain;
import io.terminus.doctor.move.model.View_EventListSow;
import io.terminus.doctor.move.model.View_GainCardList;
import io.terminus.doctor.move.model.View_SowCardList;
import io.terminus.doctor.user.dao.DoctorFarmDao;
import io.terminus.doctor.user.dao.DoctorOrgDao;
import io.terminus.doctor.user.dao.DoctorUserDataPermissionDao;
import io.terminus.doctor.user.dao.PrimaryUserDao;
import io.terminus.doctor.user.dao.SubDao;
import io.terminus.doctor.user.model.DoctorFarm;
import io.terminus.doctor.user.model.DoctorOrg;
import io.terminus.doctor.user.model.DoctorUserDataPermission;
import io.terminus.doctor.user.model.PrimaryUser;
import io.terminus.doctor.user.model.Sub;
import io.terminus.parana.user.impl.dao.UserDao;
import io.terminus.parana.user.model.User;
import io.terminus.parana.user.service.UserWriteService;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.joda.time.Days;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static io.terminus.common.utils.Arguments.*;
import static io.terminus.doctor.common.enums.PigType.FARROW_TYPES;
import static io.terminus.doctor.event.enums.PregCheckResult.YANG;
import static io.terminus.doctor.event.enums.PregCheckResult.from;

/**
 * Desc: ????????????
 * Mail: yangzl@terminus.io
 * author: DreamYoung
 * Date: 16/7/27
 */
@Slf4j
@Service
public class DoctorMoveDataService {

    private static final JsonMapperUtil JSON_MAPPER = JsonMapperUtil.nonEmptyMapper();
    private static final JsonMapperUtil MAPPER = JsonMapperUtil.nonDefaultMapperWithFormat(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"));

    private final DoctorMoveDatasourceHandler doctorMoveDatasourceHandler;
    private final DoctorGroupDao doctorGroupDao;
    private final DoctorGroupEventDao doctorGroupEventDao;
    private final DoctorGroupTrackDao doctorGroupTrackDao;
    private final DoctorPigDao doctorPigDao;
    private final DoctorPigTrackDao doctorPigTrackDao;
    private final DoctorPigEventDao doctorPigEventDao;
    private final DoctorMoveBasicService doctorMoveBasicService;
    private final DoctorPigReadService doctorPigReadService;
    private final DoctorGroupReportManager doctorGroupReportManager;
    private final DoctorGroupBatchSummaryReadService doctorGroupBatchSummaryReadService;
    private final DoctorGroupBatchSummaryWriteService doctorGroupBatchSummaryWriteService;
    private final DoctorMaterialConsumeProviderReadService doctorMaterialConsumeProviderReadService;
    private final UserWriteService<User> userWriteService;
    private final UserDao userDao;
    @Autowired
    private DoctorPigEventManager doctorPigEventManager;
    @Autowired
    private DoctorGroupEventManager doctorGroupEventManager;
    @Autowired
    private DoctorFarmDao doctorFarmDao;
    @Autowired
    private DoctorOrgDao doctorOrgDao;
    @Autowired
    private SubDao subDao;
    @Autowired
    private PrimaryUserDao primaryUserDao;
    @Autowired
    private DoctorUserDataPermissionDao doctorUserDataPermissionDao;
    @Autowired
    private DoctorPigTypeStatisticDao doctorPigTypeStatisticDao;
    @Autowired
    private DoctorGroupBatchSummaryManager groupBatchSummaryManager;
    @Autowired
    private DoctorBarnDao doctorBarnDao;

    @Autowired
    public DoctorMoveDataService(DoctorMoveDatasourceHandler doctorMoveDatasourceHandler,
                                 DoctorGroupDao doctorGroupDao,
                                 DoctorGroupEventDao doctorGroupEventDao,
                                 DoctorGroupTrackDao doctorGroupTrackDao,
                                 DoctorPigDao doctorPigDao,
                                 DoctorPigTrackDao doctorPigTrackDao,
                                 DoctorPigEventDao doctorPigEventDao,
                                 DoctorMoveBasicService doctorMoveBasicService,
                                 DoctorPigReadService doctorPigReadService,
                                 DoctorGroupReportManager doctorGroupReportManager,
                                 DoctorBarnDao doctorBarnDao,
                                 DoctorGroupBatchSummaryReadService doctorGroupBatchSummaryReadService,
                                 DoctorGroupBatchSummaryWriteService doctorGroupBatchSummaryWriteService,
                                 DoctorMaterialConsumeProviderReadService doctorMaterialConsumeProviderReadService,
                                 UserWriteService userWriteService,
                                 UserDao userDao) {
        this.doctorMoveDatasourceHandler = doctorMoveDatasourceHandler;
        this.doctorGroupDao = doctorGroupDao;
        this.doctorGroupEventDao = doctorGroupEventDao;
        this.doctorGroupTrackDao = doctorGroupTrackDao;
        this.doctorPigDao = doctorPigDao;
        this.doctorPigTrackDao = doctorPigTrackDao;
        this.doctorPigEventDao = doctorPigEventDao;
        this.doctorMoveBasicService = doctorMoveBasicService;
        this.doctorPigReadService = doctorPigReadService;
        this.doctorGroupReportManager = doctorGroupReportManager;
        this.doctorGroupBatchSummaryReadService = doctorGroupBatchSummaryReadService;
        this.doctorGroupBatchSummaryWriteService = doctorGroupBatchSummaryWriteService;
        this.doctorMaterialConsumeProviderReadService = doctorMaterialConsumeProviderReadService;
        this.userWriteService = userWriteService;
        this.userDao = userDao;
    }

    //????????????????????????????????????
    public void deleteAllPigs(Long farmId) {
        doctorPigDao.deleteByFarmId(farmId);
        doctorPigEventDao.deleteByFarmId(farmId);
        doctorPigTrackDao.deleteByFarmId(farmId);
    }

    /**
     * ???????????????????????????
     */
    @Transactional
    public void updateClosedGroupDayAge(Long moveId, DoctorFarm farm) {
        Map<String, Integer> ageMap = RespHelper.orServEx(doctorMoveDatasourceHandler
                .findByHbsSql(moveId, Proc_InventoryGain.class, "DoctorGroupTrack-Proc_InventoryGain", ImmutableMap.of("date", DateUtil.toDateTimeString(new Date())))).stream()
                .collect(Collectors.toMap(Proc_InventoryGain::getGroupOutId, Proc_InventoryGain::getAvgDayAge));

        DoctorGroupSearchDto search = new DoctorGroupSearchDto();
        search.setFarmId(farm.getId());
        search.setStatus(DoctorGroup.Status.CLOSED.getValue());
        doctorGroupDao.findBySearchDto(search).forEach(group -> {
            DoctorGroupTrack groupTrack = doctorGroupTrackDao.findByGroupId(group.getId());

            //??????<=0 ?????? ???????????????, ?????????????????????
            if (groupTrack.getAvgDayAge() <= 0) {
                Integer age = ageMap.get(group.getOutId());
                if (age != null) {
                    DoctorGroupTrack updateTrack = new DoctorGroupTrack();
                    updateTrack.setId(groupTrack.getId());
                    updateTrack.setAvgDayAge(age);
                    doctorGroupTrackDao.update(updateTrack);
                }
            }
        });
    }

    /**
     * ????????????????????????
     */
    public void createClosedGroupSummary(Long farmId) {
        List<DoctorGroup> groups = doctorGroupDao.findByFarmId(farmId);
        groups.forEach(group -> groupBatchSummaryManager.createGroupSummary(group));
    }

    /**
     * ????????????????????????
     */
    @Transactional
    public void updateFarrowGroupTrack(DoctorFarm farm) {
        DoctorGroupSearchDto search = new DoctorGroupSearchDto();
        search.setFarmId(farm.getId());
        search.setPigTypes(PigType.FARROW_TYPES);

        doctorGroupDao.findBySearchDto(search).forEach(group -> {
            DoctorGroupTrack groupTrack = doctorGroupTrackDao.findByGroupId(group.getId());

            DoctorGroupTrack updateTrack = new DoctorGroupTrack();
            updateTrack.setId(groupTrack.getId());
            doctorGroupTrackDao.update(doctorGroupReportManager.updateGroupTrackReport(groupTrack, group.getPigType()));
        });
    }

    /**
     * ????????????????????????
     */
    @Transactional
    public void updateBuruTrack(DoctorFarm farm) {
        updateBuruSowTrack(farm);
    }

    /**
     * ???????????????????????????/????????????
     */
    @Transactional
    public void updateGroupEventOtherBarn(DoctorFarm farm) {
        Map<Long, DoctorBarn> barnMap = doctorBarnDao.findByFarmId(farm.getId()).stream().collect(Collectors.toMap(DoctorBarn::getId, v -> v));

        //????????????
        doctorGroupEventDao.findGroupEventsByEventTypeAndDate(farm.getId(), GroupEventType.MOVE_IN.getValue(), null, null).forEach(event -> {
            DoctorMoveInGroupInput moveIn = JSON_MAPPER.fromJson(event.getExtra(), DoctorMoveInGroupInput.class);
            DoctorGroupEvent updateEvent = new DoctorGroupEvent();
            updateEvent.setId(event.getId());
            updateEvent.setInType(moveIn.getInType());
            DoctorBarn barn = barnMap.get(moveIn.getFromBarnId());
            if (barn != null) {
                updateEvent.setOtherBarnId(barn.getId());
                updateEvent.setOtherBarnType(barn.getPigType());
            }
            doctorGroupEventDao.update(updateEvent);
        });

        //??????
        doctorGroupEventDao.findGroupEventsByEventTypeAndDate(farm.getId(), GroupEventType.TRANS_GROUP.getValue(), null, null).forEach(event -> {
            DoctorTransGroupInput transGroup = JSON_MAPPER.fromJson(event.getExtra(), DoctorTransGroupInput.class);
            updateGroupOtherBarn(event.getId(), barnMap.get(transGroup.getToBarnId()));
        });

        //??????
        doctorGroupEventDao.findGroupEventsByEventTypeAndDate(farm.getId(), GroupEventType.TRANS_FARM.getValue(), null, null).forEach(event -> {
            DoctorTransFarmGroupInput transFarm = JSON_MAPPER.fromJson(event.getExtra(), DoctorTransFarmGroupInput.class);
            updateGroupOtherBarn(event.getId(), barnMap.get(transFarm.getToBarnId()));

        });

        //?????????
        doctorGroupEventDao.findGroupEventsByEventTypeAndDate(farm.getId(), GroupEventType.TURN_SEED.getValue(), null, null).forEach(event -> {
            DoctorTurnSeedGroupInput turnSeed = JSON_MAPPER.fromJson(event.getExtra(), DoctorTurnSeedGroupInput.class);
            updateGroupOtherBarn(event.getId(), barnMap.get(turnSeed.getToBarnId()));
        });
    }

    private void updateGroupOtherBarn(Long eventId, DoctorBarn barn) {
        if (barn != null) {
            DoctorGroupEvent updateEvent = new DoctorGroupEvent();
            updateEvent.setId(eventId);
            updateEvent.setOtherBarnId(barn.getId());
            updateEvent.setOtherBarnType(barn.getPigType());
            doctorGroupEventDao.update(updateEvent);
        }
    }

    /**
     * ??????????????????
     */
    @Transactional
    public void updateMateType(DoctorFarm farm) {
        //???????????????????????? mate_type ???????????????
        doctorPigEventDao.findByFarmIdAndKindAndEventTypes(farm.getId(), DoctorPig.PigSex.SOW.getKey(), Lists.newArrayList(PigEvent.MATING.getKey())).stream()
                .filter(e -> isNull(e.getDoctorMateType()) && e.getCurrentMatingCount() == 1)  //????????????
                .forEach(event -> {
                    List<DoctorPigEvent> pigEvents = doctorPigEventDao.queryAllEventsByPigIdForASC(event.getPigId());
                    DoctorMatingType mateType = DoctorSowMatingHandler.getPigMateType(pigEvents, event.getEventAt());

                    DoctorPigEvent updateEvent = new DoctorPigEvent();
                    updateEvent.setId(event.getId());
                    updateEvent.setDoctorMateType(mateType.getKey());
                    doctorPigEventDao.update(updateEvent);
                });
    }

    /**
     * ????????????????????????
     */
    @Transactional
    public void updateTranGroupType(DoctorFarm farm) {
        doctorGroupEventDao.findGroupEventsByEventTypeAndDate(farm.getId(), GroupEventType.MOVE_IN.getValue(), null, null)
                .forEach(event -> {
                    DoctorMoveInGroupInput moveIn = JSON_MAPPER.fromJson(event.getExtra(), DoctorMoveInGroupInput.class);
                    DoctorGroupEvent updateEvent = new DoctorGroupEvent();
                    updateEvent.setId(event.getId());
                    updateEvent.setTransGroupType(getTransType(moveIn.getInType(), event.getPigType(), moveIn.getFromBarnType()).getValue());
                    doctorGroupEventDao.update(updateEvent);
                });

        doctorGroupEventDao.findGroupEventsByEventTypeAndDate(farm.getId(), GroupEventType.TRANS_GROUP.getValue(), null, null)
                .forEach(event -> {
                    DoctorTransGroupInput trans = JSON_MAPPER.fromJson(event.getExtra(), DoctorTransGroupInput.class);
                    DoctorGroupEvent updateEvent = new DoctorGroupEvent();
                    updateEvent.setId(event.getId());
                    Integer toBarnType = trans.getToBarnType();
                    if (toBarnType == null) {
                        DoctorBarn toBarn = doctorBarnDao.findById(trans.getToBarnId());
                        if (toBarn != null) {
                            toBarnType = toBarn.getPigType();
                        }
                    }
                    updateEvent.setTransGroupType(getTransType(null, event.getPigType(), toBarnType).getValue());
                    doctorGroupEventDao.update(updateEvent);
                });
    }

    /**
     * ????????????????????????
     */
    @Transactional
    public void updateSowAbortion(DoctorFarm farm) {
        List<DoctorPigEvent> events = doctorPigEventDao.findByFarmIdAndKindAndEventTypes(farm.getId(),
                DoctorPig.PigSex.SOW.getKey(), Lists.newArrayList(13));
        if (notEmpty(events)) {
            events.forEach(event -> {
                DoctorPigEvent updateEvent = new DoctorPigEvent();
                updateEvent.setId(event.getId());
                updateEvent.setType(PigEvent.PREG_CHECK.getKey());
                updateEvent.setName(PigEvent.PREG_CHECK.getName());
                updateEvent.setCheckDate(event.getEventAt());
                updateEvent.setPregCheckResult(PregCheckResult.LIUCHAN.getKey());

                //??????
                DoctorPregChkResultDto dto = new DoctorPregChkResultDto();
                dto.setCheckDate(event.getEventAt());
                dto.setCheckResult(PregCheckResult.LIUCHAN.getKey());
                updateEvent.setExtra(ToJsonMapper.JSON_NON_EMPTY_MAPPER.toJson(dto));
                doctorPigEventDao.update(updateEvent);
            });
        }

        //???????????????????????????
        List<DoctorPigTrack> pigTracks = doctorPigTrackDao.findByFarmIdAndStatus(farm.getId(), 6);
        if (notEmpty(pigTracks)) {
            pigTracks.forEach(pigTrack -> {
                DoctorPigTrack updateTrack = new DoctorPigTrack();
                updateTrack.setId(pigTrack.getId());
                updateTrack.setStatus(PigStatus.KongHuai.getKey());
                doctorPigTrackDao.update(updateTrack);
            });
        }
    }

    /**
     * ????????????????????????
     */
    public void updateSowFarrowWeight(Long moveId, DoctorFarm farm) {

        //??????id????????????map
        Map<String, Double> fwmap = RespHelper.orServEx(doctorMoveDatasourceHandler
                .findByHbsSql(moveId, DoctorSowFarrowWeight.class, "DoctorSowFarrowWeight", ImmutableMap.of("farmOutId", farm.getOutId()))).stream()
                .collect(Collectors.toMap(DoctorSowFarrowWeight::getGroupOutId, DoctorSowFarrowWeight::getFarrowWeight));

        List<DoctorPigEvent> events = doctorPigEventDao.findByFarmIdAndKindAndEventTypes(farm.getId(),
                DoctorPig.PigSex.SOW.getKey(), Lists.newArrayList(PigEvent.FARROWING.getKey()));
        if (notEmpty(events)) {
            events.forEach(event -> {
                DoctorPigEvent updateEvent = new DoctorPigEvent();
                updateEvent.setId(event.getId());
                updateEvent.setFarrowWeight(fwmap.get(event.getOutId()));   //???map???????????????
                doctorPigEventDao.update(updateEvent);
            });
        }
    }

    /**
     * ??????????????????, ???????????????
     */
    @Transactional
    public void updateSowTransBarn(DoctorFarm farm) {
        Map<Long, Integer> barnTypeMap = doctorMoveBasicService.getBarnIdMap(farm.getId());

        //????????????
        List<Integer> tarnsBarnTypes = Lists.newArrayList(
                PigEvent.CHG_LOCATION.getKey(),
//                PigEvent.TO_PREG.getKey(),
                PigEvent.TO_MATING.getKey(),
                PigEvent.TO_FARROWING.getKey()
        );

        doctorPigEventDao.findByFarmIdAndKindAndEventTypes(farm.getId(), DoctorPig.PigSex.SOW.getKey(), tarnsBarnTypes)
                .forEach(event -> {
                    DoctorChgLocationDto dto = JSON_MAPPER.fromJson(event.getExtra(), DoctorChgLocationDto.class);
                    if (dto != null) {
                        Integer fromBarnType = barnTypeMap.get(dto.getChgLocationFromBarnId());
                        Integer toBarnType = barnTypeMap.get(dto.getChgLocationToBarnId());
                        PigEvent type = getTransBarnType(fromBarnType, toBarnType);

                        DoctorPigEvent updateEvent = new DoctorPigEvent();
                        updateEvent.setId(event.getId());
                        updateEvent.setType(type.getKey());
                        updateEvent.setName(type.getName());
                        doctorPigEventDao.update(updateEvent);
                    }
                });
    }

    //??????from to?????????, ????????????????????????
    private static PigEvent getTransBarnType(Integer fromBarnType, Integer toBarnType) {
        //???????????????
        if (Objects.equals(toBarnType, PigType.MATE_SOW.getValue()) && !Objects.equals(fromBarnType, toBarnType)) {
            return PigEvent.TO_MATING;
        }
        //???????????????
//        if (Objects.equals(toBarnType, PigType.PREG_SOW.getValue()) && !Objects.equals(fromBarnType, toBarnType)) {
//            return PigEvent.TO_PREG;
//        }
        //?????????
        if (Objects.equals(toBarnType, PigType.DELIVER_SOW.getValue()) && !Objects.equals(fromBarnType, toBarnType)) {
            return PigEvent.TO_FARROWING;
        }
        return PigEvent.CHG_LOCATION;
    }

    /**
     * ??????????????????????????????, ??????????????????????????????(status = 4 and barnType = 7) ?????????????????????, ??????????????? 7 ?????????
     */
    public void updateFarrowSow(DoctorFarm farm) {
        //?????????, ??????id
        Map<String, Long> groupMap = Maps.newHashMap();
        doctorGroupDao.findByFarmId(farm.getId()).forEach(group -> groupMap.put(group.getGroupCode(), group.getId()));

        Map<String, DoctorBarn> barnMap = doctorMoveBasicService.getBarnMap2(farm.getId());

        //????????????????????????
        doctorPigTrackDao.list(DoctorPigTrack.builder()
                .farmId(farm.getId())
                .pigType(DoctorPig.PigSex.SOW.getKey())
                .isRemoval(IsOrNot.NO.getValue()).build())
                .forEach(track -> {
                    Map<String, Object> extraMap = JSON_MAPPER.fromJson(track.getExtra(), JSON_MAPPER.createCollectionType(Map.class, String.class, Object.class));
                    if (Objects.equals(track.getStatus(), PigStatus.Pregnancy.getKey()) &&
                            barnMap.get(track.getCurrentBarnName()).getPigType() == PigType.DELIVER_SOW.getValue()) {
                        track.setStatus(PigStatus.Farrow.getKey());
                        doctorPigTrackDao.update(track);
                    }
                    if (extraMap.containsKey("groupCode")) {
                        extraMap.put("farrowingPigletGroupId", groupMap.get(String.valueOf(extraMap.get("groupCode"))));
                        track.setExtra(ToJsonMapper.JSON_NON_EMPTY_MAPPER.toJson(extraMap));
                        doctorPigTrackDao.update(track);

                    }
                });
    }

    /**
     * ???????????????trackExtraMap
     */
    public void updateSowTrackExtraMap(DoctorFarm farm) {
        //????????????????????????
        doctorPigTrackDao.list(DoctorPigTrack.builder()
                .farmId(farm.getId())
                .pigType(DoctorPig.PigSex.SOW.getKey())
                .isRemoval(IsOrNot.NO.getValue()).build())
                .forEach(track -> {
                    List<DoctorPigEvent> events = doctorPigEventDao.queryAllEventsByPigIdForASC(track.getPigId()).stream()
                            .sorted((a, b) -> a.getEventAt().compareTo(b.getEventAt()))
                            .collect(Collectors.toList());

                    DoctorPigTrack updateTrack = new DoctorPigTrack();
                    updateTrack.setId(track.getId());
                    updateTrack.setExtra(ToJsonMapper.JSON_NON_EMPTY_MAPPER.toJson(getSowExtraMap(events)));
                    doctorPigTrackDao.update(updateTrack);
                });
    }

    /**
     * ????????????
     */
    @Transactional
    public void moveGroup(Long moveId, DoctorFarm farm, List<String> groupEventOutId) {
        //0. ??????????????????: barn, basic, subUser, changeReason, customer
        Map<String, DoctorBarn> barnMap = doctorMoveBasicService.getBarnMap(farm.getId());
        Map<Integer, Map<String, DoctorBasic>> basicMap = doctorMoveBasicService.getBasicMap();
        Map<String, Long> subMap = doctorMoveBasicService.getSubMap(farm.getOrgId());
        Map<String, DoctorChangeReason> changeReasonMap = doctorMoveBasicService.getReasonMap();
        Map<String, DoctorCustomer> customerMap = doctorMoveBasicService.getCustomerMap(farm.getId());
        Map<String, DoctorBasicMaterial> vaccMap = doctorMoveBasicService.getVaccMap();

        //1. ??????DoctorGroup
        List<DoctorGroup> groups = RespHelper.orServEx(doctorMoveDatasourceHandler
                .findByHbsSql(moveId, View_GainCardList.class, "DoctorGroup-GainCardList")).stream()
                .filter(loc -> isFarm(loc.getFarmOutId(), farm.getOutId()))
                .map(gain -> getGroup(farm, gain, barnMap, basicMap, subMap)).collect(Collectors.toList());
        if (!groups.isEmpty()) {
            doctorGroupDao.creates(groups);
        }

        //??????????????????group, key = outId, ?????????, ???????????????????????????
        Map<String, DoctorGroup> groupMap = doctorGroupDao.findByFarmId(farm.getId()).stream().collect(Collectors.toMap(DoctorGroup::getOutId, v -> v));
        Map<String, DoctorPig> pigMap = Maps.newHashMap();
        doctorPigDao.findPigsByFarmId(farm.getId()).forEach(pig -> pigMap.put(pig.getPigCode(), pig));

        //2. ??????DoctorGroupEvent
        List<DoctorGroupEvent> events = RespHelper.orServEx(doctorMoveDatasourceHandler
                .findByHbsSql(moveId, View_EventListGain.class, "DoctorGroupEvent-EventListGain")).stream()
                .map(gainEvent -> getGroupEvent(groupMap, gainEvent, subMap, barnMap, basicMap, changeReasonMap, customerMap, vaccMap, pigMap, groupEventOutId))
                .filter(event -> event != null)
                .collect(Collectors.toList());
        if (!events.isEmpty()) {
            doctorGroupEventDao.creates(events);
        }

        //?????????????????????groupEvent, ????????????id groupBy
        Map<Long, List<DoctorGroupEvent>> eventMap = doctorGroupEventDao.findByFarmId(farm.getId()).stream().collect(Collectors.groupingBy(DoctorGroupEvent::getGroupId));

        //3. ??????DoctorTrack, ???????????????????????????map, ?????????track
        String now = DateUtil.toDateTimeString(new Date());
        Map<String, Proc_InventoryGain> gainMap = RespHelper.orServEx(doctorMoveDatasourceHandler
                .findByHbsSql(moveId, Proc_InventoryGain.class, "DoctorGroupTrack-Proc_InventoryGain", ImmutableMap.of("date", now))).stream()
                .filter(loc -> isFarm(loc.getFarmOutId(), farm.getOutId()))
                .collect(Collectors.toMap(Proc_InventoryGain::getGroupOutId, v -> v));

        List<DoctorGroupTrack> groupTracks = groupMap.values().stream()
                .map(group -> getGroupTrack(group, gainMap.get(group.getOutId()), eventMap.get(group.getId())))
                .collect(Collectors.toList());
        if (!groupTracks.isEmpty()) {
            doctorGroupTrackDao.creates(groupTracks);
        }

        groupTracks.forEach(groupTrack -> {
            DoctorGroupEvent groupEvent = doctorGroupEventDao.findLastEventByGroupId(groupTrack.getGroupId());
            DoctorGroup group = doctorGroupDao.findById(groupTrack.getGroupId());
            //groupTrack??????????????????
            groupTrack.setRelEventId(groupEvent.getId());
            doctorGroupTrackDao.update(groupTrack);

        });

        //????????????
        createClosedGroupSummary(farm.getId());
    }

    /**
     * ??????????????????
     */
    public void movePig(Long moveId, DoctorFarm farm, List<String> groupEventOutId) {
        //0. ??????????????????: barn, basic, subUser
        Map<String, DoctorBarn> barnMap = doctorMoveBasicService.getBarnMap(farm.getId());
        Map<Integer, Map<String, DoctorBasic>> basicMap = doctorMoveBasicService.getBasicMap();
        Map<String, Long> subMap = doctorMoveBasicService.getSubMap(farm.getOrgId());
        Map<String, DoctorChangeReason> changeReasonMap = doctorMoveBasicService.getReasonMap();
        Map<String, DoctorCustomer> customerMap = doctorMoveBasicService.getCustomerMap(farm.getId());
        Map<String, DoctorBasicMaterial> vaccMap = doctorMoveBasicService.getVaccMap();

        //1. ??????boar
        moveBoar(moveId, farm, barnMap, basicMap, changeReasonMap, customerMap, subMap, vaccMap);

        //??????boar, ?????????map
        Map<String, DoctorPig> boarMap = Maps.newHashMap();
        doctorPigDao.findPigsByFarmIdAndPigType(farm.getId(), DoctorPig.PigSex.BOAR.getKey()).forEach(boar -> boarMap.put(boar.getPigCode(), boar));

        //2. ??????sow
        moveSow(moveId, farm, basicMap, barnMap, subMap, customerMap, changeReasonMap, boarMap, vaccMap, groupEventOutId);
    }

    //????????????
    private void moveSow(Long moveId, DoctorFarm farm, Map<Integer, Map<String, DoctorBasic>> basicMap, Map<String, DoctorBarn> barnMap,
                         Map<String, Long> subMap, Map<String, DoctorCustomer> customerMap, Map<String, DoctorChangeReason> changeReasonMap,
                         Map<String, DoctorPig> boarMap, Map<String, DoctorBasicMaterial> vaccMap, List<String> groupEventOutId) {
        //1. ??????DoctorPig
        List<View_SowCardList> sowCards = RespHelper.orServEx(doctorMoveDatasourceHandler
                .findByHbsSql(moveId, View_SowCardList.class, "DoctorPig-SowCardList")).stream()
                .filter(loc -> isFarm(loc.getFarmOutId(), farm.getOutId()))
                .collect(Collectors.toList());
        List<DoctorPig> pigs = sowCards.stream().map(card -> getSow(card, farm, basicMap)).collect(Collectors.toList());
        if (!pigs.isEmpty()) {
            doctorPigDao.creates(pigs);
        }

        //????????????, ?????????map
        Map<String, DoctorPig> sowMap = doctorPigDao.findPigsByFarmIdAndPigType(farm.getId(), DoctorPig.PigSex.SOW.getKey()).stream()
                .collect(Collectors.toMap(DoctorPig::getOutId, v -> v));

        //???????????????, ??????5?????????????????????
        List<View_EventListSow> sowEventViews = Lists.newArrayList();
        List<List<View_SowCardList>> sowLists = Lists.partition(sowCards, sowCards.size() / 5 + 1);

        sowLists.forEach(ss -> {
            String sowOutIds = Joiners.COMMA.join(ss.stream().map(s -> brace(s.getPigOutId())).collect(Collectors.toList()));
            sowEventViews.addAll(RespHelper.orServEx(doctorMoveDatasourceHandler
                    .findByHbsSql(moveId, View_EventListSow.class, "DoctorPigEvent-EventListSow", ImmutableMap.of("sowOutIds", sowOutIds))).stream()
                    .filter(loc -> isFarm(loc.getFarmOutId(), farm.getOutId()))
                    .collect(Collectors.toList())
            );
        });

        //2. ??????DoctorPigEvent
        List<DoctorPigEvent> sowEvents = sowEventViews.stream()
                .map(event -> getSowEvent(event, sowMap, barnMap, basicMap, subMap, customerMap, changeReasonMap, boarMap, vaccMap, groupEventOutId))
                .collect(Collectors.toList());

        //???????????????, ??????5????????????
        if (!sowEvents.isEmpty()) {
            Lists.partition(sowEvents, 5).forEach(doctorPigEventDao::creates);
        }

        //??????????????????, ??????????????????
        Map<Long, List<DoctorPigEvent>> sowEventMap = doctorPigEventDao.findByFarmIdAndKind(farm.getId(), DoctorPig.PigSex.SOW.getKey())
                .stream().collect(Collectors.groupingBy(DoctorPigEvent::getPigId));

        //??????relEventId
        updatePigRelEventId(sowEventMap);

        //3. ??????DoctorPigTrack
        List<DoctorPigTrack> sowTracks = sowCards.stream()
                .map(card -> {
                    DoctorPig sow = sowMap.get(card.getPigOutId());
                    return getSowTrack(card, sow, barnMap, sow == null ? null : sowEventMap.get(sow.getId()), moveId);
                })
                .filter(Arguments::notNull)
                .collect(Collectors.toList());
        if (!sowTracks.isEmpty()) {
            doctorPigTrackDao.creates(sowTracks);
        }

        //4. ?????????????????????????????????

        updateBoarCurrentParity(sowEvents);

        //??????????????????,??????????????????
        updateParityAndBoarCode(farm);

        //??????????????????extra
        updateFosterSowCode(farm);
    }

    /**
     * ???????????????????????????
     * @param farmIds ??????ids
     */
    public void correctChgFarm(List<Long> farmIds) {
        if (Arguments.isNullOrEmpty(farmIds)) {
            return;
        }
        List<DoctorPigEvent> sowEvents = doctorPigEventDao.findByFarmIds(farmIds);
        List<DoctorPigEvent> correctChgFarmEvents = Lists.newArrayList();

        //?????????????????????????????????id??????
        List<Long> pigIds = sowEvents.stream().filter(pigEvent ->
                Objects.equals(pigEvent.getType(), PigEvent.CHG_FARM_IN.getKey()))
                .map(DoctorPigEvent::getPigId).collect(Collectors.toList());

        //???id?????????????????????
        Map<Long, List<DoctorPigEvent>> pigEvents = sowEvents.stream()
                .filter(pigEvent -> pigIds.contains(pigEvent.getPigId()))
                .collect(Collectors.groupingBy(DoctorPigEvent::getPigId));

        //??????????????????(??????pig???pigTrack???pigEvent)
        pigEvents.keySet().forEach(pigId -> {
            try {
                List<DoctorPigEvent> pigEventList = pigEvents.get(pigId);
                for (int i = 0; i < pigEventList.size(); i++) {
                    DoctorPigEvent pigEvent = pigEventList.get(i);
                    if (!Objects.equals(pigEvent.getType(), PigEvent.CHG_FARM_IN.getKey())) {
                        continue;
                    }
                    correctChgFarmEvents.addAll(generateChgFarm(pigEventList.subList(0, i+1), pigId));
                }
            } catch (Exception e) {
                log.error("correct chg farm failed, pigId:{}, cause:{}", pigId, Throwables.getStackTraceAsString(e));
                throw e;
            }
        });

        //??????????????????
        if (!sowEvents.isEmpty()) {
            Lists.partition(correctChgFarmEvents, 5).forEach(doctorPigEventDao::creates);
        }

        //????????????????????????????????????????????????eventSource=5
        flushChgFarmEventSource(farmIds);
    }

    private List<DoctorPigEvent> generateChgFarm(List<DoctorPigEvent> rawList, Long pigId) {
        DoctorPigEvent chgFarmIn = rawList.get(rawList.size() - 1);
        DoctorChgFarmDto chgFarmDto = JSON_MAPPER.fromJson(chgFarmIn.getExtra(), DoctorChgFarmDto.class);
        DoctorBarn fromBarn = doctorBarnDao.findById(chgFarmDto.getFromBarnId());
        if (isNull(fromBarn)) {
            log.warn("from barn is null, pigId:{}, barnId:{}", pigId, chgFarmDto.getFromBarnId());
            return Lists.newArrayList();
        }
        Long rowPigId = generatePigAndTrack(pigId, fromBarn);

        return rawList.stream().map(pigEvent -> {
            DoctorPigEvent rowEvent = new DoctorPigEvent();
            BeanMapper.copy(pigEvent, rowEvent);
            rowEvent.setFarmId(chgFarmDto.getFromFarmId());
            rowEvent.setFarmName(chgFarmDto.getFromFarmName());
            rowEvent.setPigId(rowPigId);
            rowEvent.setBarnId(fromBarn.getId());
            rowEvent.setBarnName(fromBarn.getName());
            rowEvent.setBarnType(fromBarn.getPigType());

            if (Objects.equals(pigEvent.getType(), PigEvent.CHG_FARM_IN.getKey())) {
                rowEvent.setType(PigEvent.CHG_FARM.getKey());
                rowEvent.setName(PigEvent.CHG_FARM.getName());
            }
            return rowEvent;
        }).collect(Collectors.toList());
    }

    private Long generatePigAndTrack(Long pigId, DoctorBarn barn) {
        DoctorPig pig = doctorPigDao.findById(pigId);
        pig.setFarmId(barn.getFarmId());
        pig.setFarmName(barn.getFarmName());
        pig.setIsRemoval(IsOrNot.YES.getValue());
        doctorPigDao.create(pig);

        DoctorPigTrack pigTrack = new DoctorPigTrack();
        pigTrack.setFarmId(pig.getFarmId());
        pigTrack.setPigId(pig.getId());
        pigTrack.setPigType(pig.getPigType());
        pigTrack.setStatus(PigStatus.Removal.getKey());
        pigTrack.setIsRemoval(IsOrNot.YES.getValue());
        pigTrack.setCurrentBarnId(barn.getId());
        pigTrack.setCurrentBarnName(barn.getName());
        pigTrack.setCurrentBarnType(barn.getPigType());
        pigTrack.setCurrentEventId(0L);
        doctorPigTrackDao.create(pigTrack);

        return pig.getId();
    }

    //?????????????????????, ?????????????????????????????????
    private void updateBuruSowTrack(DoctorFarm farm) {
        Map<Long, Long> deleverBarnIdToMap = doctorGroupDao.findByFarmIdAndPigTypeAndStatus(farm.getId(), PigType.DELIVER_SOW.getValue()
                , DoctorGroup.Status.CREATED.getValue()).stream().collect(Collectors.toMap(DoctorGroup::getCurrentBarnId, DoctorGroup::getId));
        doctorPigTrackDao.findByFarmIdAndStatus(farm.getId(), PigStatus.FEED.getKey()).stream()
                .filter(t -> Objects.equals(t.getIsRemoval(), IsOrNot.NO.getValue())
                        && Objects.equals(t.getPigType(), DoctorPig.PigSex.SOW.getKey()))
                .forEach(track -> {
                    track.setExtra(track.getExtra());
                    Map<String, Object> extraMap = track.getExtraMap();

                    DoctorPigTrack updateTrack = new DoctorPigTrack();
                    updateTrack.setId(track.getId());
                    updateTrack.setGroupId(deleverBarnIdToMap.get(track.getCurrentBarnId()));
                    //????????????????????????
                    updateTrack.setUnweanQty(doctorPigEventDao.getSowUnweanCount(track.getPigId()));
                    updateTrack.setWeanQty(getIntegerDefault0(extraMap, "partWeanPigletsCount"));
                    updateTrack.setFarrowQty(getIntegerDefault0(extraMap, "farrowingLiveCount"));
                    updateTrack.setFarrowAvgWeight(getDoubleDefault0(extraMap, "birthNestAvg"));
                    updateTrack.setWeanAvgWeight(getDoubleDefault0(extraMap, "partWeanAvgWeight"));
                    doctorPigTrackDao.update(updateTrack);
                });
    }

    private static Integer getIntegerDefault0(Map<String, Object> extraMap, String key) {
        return extraMap.containsKey(key) ? Integer.valueOf(String.valueOf(extraMap.get(key))) : 0;
    }

    private static Double getDoubleDefault0(Map<String, Object> extraMap, String key) {
        return extraMap.containsKey(key) ? Double.valueOf(String.valueOf(extraMap.get(key))) : 0D;
    }

    //????????????????????????(??????????????????)
    private void updateBoarCurrentParity(List<DoctorPigEvent> sowEvents) {
        sowEvents.stream()
                .filter(e -> Objects.equals(e.getType(), PigEvent.MATING.getKey()))
                .map(m -> JSON_MAPPER.fromJson(m.getExtra(), DoctorMatingDto.class))
                .filter(p -> p != null && p.getMatingBoarPigId() != null)
                .collect(Collectors.groupingBy(DoctorMatingDto::getMatingBoarPigId))
                .forEach((k, v) -> doctorPigTrackDao.updateBoarCurrentParity(k, v.size()));
    }

    //????????????
    private DoctorPig getSow(View_SowCardList card, DoctorFarm farm, Map<Integer, Map<String, DoctorBasic>> basicMap) {
        DoctorPig sow = new DoctorPig();
        sow.setOrgId(farm.getOrgId());
        sow.setOrgName(farm.getOrgName());
        sow.setFarmId(farm.getId());
        sow.setFarmName(farm.getName());
        sow.setOutId(card.getPigOutId());           //??????OID
        sow.setPigCode(card.getPigCode());
        sow.setPigType(DoctorPig.PigSex.SOW.getKey());  //???????????????
        sow.setIsRemoval("?????????".equals(card.getStatus()) ? IsOrNot.YES.getValue() : IsOrNot.NO.getValue());
        sow.setPigFatherCode(card.getPigFatherCode());
        sow.setPigMotherCode(card.getPigMotherCode());
        sow.setSource(card.getSource());
        sow.setBirthDate(card.getBirthDate());
        sow.setBirthWeight(card.getBirthWeight());
        sow.setInFarmDate(card.getInFarmDate());
        sow.setInFarmDayAge(card.getInFarmDayAge());
        sow.setInitBarnName(card.getInitBarnName());
        sow.setRemark(card.getRemark());

        //??????
        DoctorBasic breed = basicMap.get(DoctorBasic.Type.BREED.getValue()).get(card.getBreed());
        sow.setBreedId(breed == null ? null : breed.getId());
        sow.setBreedName(card.getBreed());

        //??????
        DoctorBasic gene = basicMap.get(DoctorBasic.Type.GENETICS.getValue()).get(card.getGenetic());
        sow.setGeneticId(gene == null ? null : gene.getId());
        sow.setGeneticName(card.getGenetic());

        //????????????
        sow.setExtraMap(ImmutableMap.of(
                DoctorFarmEntryConstants.EAR_CODE, card.getPigCode(),   //??????????????????
                DoctorFarmEntryConstants.FIRST_PARITY, card.getFirstParity(),
                DoctorFarmEntryConstants.LEFT_COUNT, card.getLeftCount(),
                DoctorFarmEntryConstants.RIGHT_COUNT, card.getRightCount()
        ));
        return sow;
    }

    //??????????????????(boarMap: key = boarCode, value = DoctorPig, ??????????????????)
    private DoctorPigEvent getSowEvent(View_EventListSow event, Map<String, DoctorPig> sowMap, Map<String, DoctorBarn> barnMap,
                                       Map<Integer, Map<String, DoctorBasic>> basicMap, Map<String, Long> subMap,
                                       Map<String, DoctorCustomer> customerMap, Map<String, DoctorChangeReason> changeReasonMap,
                                       Map<String, DoctorPig> boarMap, Map<String, DoctorBasicMaterial> vaccMap, List<String> groupEventOutId) {
        DoctorPig sow = sowMap.get(event.getPigOutId());
        if (sow == null) {
            return null;
        }

        DoctorPigEvent sowEvent = new DoctorPigEvent();
        sowEvent.setOrgId(sow.getOrgId());
        sowEvent.setOrgName(sow.getOrgName());
        sowEvent.setFarmId(sow.getFarmId());
        sowEvent.setFarmName(sow.getFarmName());
        sowEvent.setPigId(sow.getId());
        sowEvent.setPigCode(sow.getPigCode());
        sowEvent.setStatus(EventStatus.VALID.getValue());

        sowEvent.setEventAt(event.getEventAt());
        sowEvent.setKind(sow.getPigType());       // ??????(??????2,??????1)
        sowEvent.setName(event.getEventName());
        sowEvent.setDesc(event.getEventDesc());
        sowEvent.setOutId(event.getEventOutId());
        sowEvent.setRemark(event.getRemark());
        sowEvent.setEventSource(SourceType.MOVE.getValue());

        //????????????, (?????????????????????, ??????????????????????????????)
        PigEvent eventType = PigEvent.from(event.getEventName());
        if (Objects.equals(event.getEventName(), "?????????")) {
            eventType = PigEvent.TO_FARROWING;
        }
        if (Objects.equals(event.getEventName(), "???????????????")) {
            eventType = PigEvent.TO_MATING;
        }
        sowEvent.setType(eventType == null ? null : eventType.getKey());

        DoctorBarn barn = barnMap.get(event.getBarnOutId());
        if (barn != null) {
            sowEvent.setBarnId(barn.getId());
            sowEvent.setBarnName(barn.getName());
            sowEvent.setBarnType(barn.getPigType());
        }
        return getSowEventExtra(eventType, sowEvent, event, subMap, basicMap, barnMap, customerMap, changeReasonMap, boarMap, vaccMap, groupEventOutId);
    }

    //??????????????????extra??????
    private DoctorPigEvent getSowEventExtra(PigEvent eventType, DoctorPigEvent sowEvent, View_EventListSow event, Map<String, Long> subMap,
                                            Map<Integer, Map<String, DoctorBasic>> basicMap, Map<String, DoctorBarn> barnMap,
                                            Map<String, DoctorCustomer> customerMap, Map<String, DoctorChangeReason> changeReasonMap,
                                            Map<String, DoctorPig> boarMap, Map<String, DoctorBasicMaterial> vaccMap, List<String> groupEventOutId) {

        if (eventType == null) {
            return sowEvent;
        }

        //switch ????????????
        switch (eventType) {
            case TO_PREG:        //???????????????
                sowEvent.setExtra(ToJsonMapper.JSON_NON_EMPTY_MAPPER.toJson(getSowTranBarnExtra(event, barnMap)));
                break;
            case TO_MATING:      //???????????????
                sowEvent.setExtra(ToJsonMapper.JSON_NON_EMPTY_MAPPER.toJson(getSowTranBarnExtra(event, barnMap)));
                break;
            case TO_FARROWING:   //?????????
                sowEvent.setExtra(ToJsonMapper.JSON_NON_EMPTY_MAPPER.toJson(getSowTranBarnExtra(event, barnMap)));
                break;
            case CHG_LOCATION:  //??????
                sowEvent.setExtra(ToJsonMapper.JSON_NON_EMPTY_MAPPER.toJson(getSowTranBarnExtra(event, barnMap)));
                if (!Strings.isNullOrEmpty(event.getDisease())){
                    groupEventOutId.add("chgToMoveIn" + event.getDisease());
                }
                if (!Strings.isNullOrEmpty(event.getPregCheckResult())) {
                    groupEventOutId.add(event.getPregCheckResult());
                }
                break;
            case CHG_FARM_IN:      //????????????
                DoctorChgFarmDto tranFarm = new DoctorChgFarmDto();
                tranFarm.setChgFarmDate(event.getEventAt());
                DoctorFarm fromFarm = doctorFarmDao.findByOutId(event.getChgType());
                tranFarm.setFromFarmId(fromFarm.getId());
                tranFarm.setFromFarmName(fromFarm.getName());
                DoctorBarn fromBarn = doctorBarnDao.findByOutId(fromFarm.getId(), event.getBarnOutId());
                tranFarm.setFromBarnId(fromBarn.getId());
                tranFarm.setFromBarnName(fromBarn.getName());
                tranFarm.setRemark(event.getChgReason());
                sowEvent.setExtra(ToJsonMapper.JSON_NON_EMPTY_MAPPER.toJson(tranFarm));
                break;
            case CONDITION:     //??????
                DoctorConditionDto condition = new DoctorConditionDto();
                condition.setConditionDate(event.getEventAt()); //????????????
                condition.setConditionJudgeScore(Double.valueOf(event.getScore()));    //????????????
                condition.setConditionWeight(event.getEventWeight()); // ????????????
                condition.setConditionBackWeight(event.getBackFat()); // ??????
                condition.setConditionRemark(event.getRemark()); //????????????
                sowEvent.setExtra(ToJsonMapper.JSON_NON_EMPTY_MAPPER.toJson(condition));
                break;
            case DISEASE:       //??????
                DoctorDiseaseDto disease = new DoctorDiseaseDto();
                disease.setDiseaseDate(event.getEventAt());

                DoctorBasic ddd = basicMap.get(DoctorBasic.Type.DISEASE.getValue()).get(event.getDiseaseName());
                disease.setDiseaseId(ddd == null ? null : ddd.getId());
                disease.setDiseaseName(event.getDiseaseName());
                disease.setDiseaseStaff(event.getStaffName());
                disease.setDiseaseRemark(event.getRemark());
                DoctorBasicMaterial vaccBasic1 = vaccMap.get(event.getDisease());
                sowEvent.setBasicId(notNull(vaccBasic1) ? vaccBasic1.getId() : null);
                sowEvent.setBasicName(event.getDisease());
                sowEvent.setExtra(ToJsonMapper.JSON_NON_EMPTY_MAPPER.toJson(disease));
                break;
            case VACCINATION:   //??????
                DoctorVaccinationDto vacc = new DoctorVaccinationDto();
                vacc.setVaccinationDate(event.getEventAt());

                //??????
                DoctorBasicMaterial vaccBasic = vaccMap.get(event.getDisease());
                vacc.setVaccinationId(vaccBasic == null ? null : vaccBasic.getId());
                vacc.setVaccinationName(event.getDisease());  //?????????????????????
                vacc.setVaccinationStaffId(subMap.get(event.getChgReason()));
                vacc.setVaccinationStaffName(event.getStaffName());
                vacc.setVaccinationRemark(event.getRemark());
                sowEvent.setVaccinationId(vacc.getVaccinationId());
                sowEvent.setVaccinationName(vacc.getVaccinationName());
                sowEvent.setOperatorId(vacc.getVaccinationStaffId());
                sowEvent.setOperatorName(vacc.getVaccinationStaffName());
                sowEvent.setExtra(ToJsonMapper.JSON_NON_EMPTY_MAPPER.toJson(vacc));
                break;
            case REMOVAL:       //??????
                DoctorRemovalDto removal = getSowRemovalExtra(event, customerMap, basicMap, barnMap, changeReasonMap);
                sowEvent.setExtra(ToJsonMapper.JSON_NON_EMPTY_MAPPER.toJson(removal));
                sowEvent.setChangeTypeId(removal.getChgTypeId());
                sowEvent.setPrice(removal.getPrice());
                sowEvent.setAmount(removal.getSum());
                break;
            case ENTRY:         //??????
                DoctorFarmEntryDto farmEntryDto = getSowEntryExtra(event, basicMap, barnMap);
                sowEvent.setSource(farmEntryDto.getSource());
                sowEvent.setBreedId(farmEntryDto.getBreed());
                sowEvent.setBreedName(farmEntryDto.getBreedName());
                sowEvent.setBreedTypeId(farmEntryDto.getBreedType());
                sowEvent.setBreedTypeName(farmEntryDto.getBreedTypeName());
                sowEvent.setExtra(ToJsonMapper.JSON_NON_EMPTY_MAPPER.toJson(getSowEntryExtra(event, basicMap, barnMap)));
                break;
            case MATING:        //??????
                DoctorMatingDto mating = getSowMatingExtra(event, boarMap, subMap);
                sowEvent.setMattingDate(event.getEventAt());                //????????????
                sowEvent.setJudgePregDate(mating.getJudgePregDate());
                sowEvent.setMateType(mating.getMatingType());
                sowEvent.setExtra(ToJsonMapper.JSON_NON_EMPTY_MAPPER.toJson(mating));
                break;
            case PREG_CHECK:    //????????????
                DoctorPregChkResultDto checkResult = getSowPregCheckExtra(event);
                sowEvent.setPregCheckResult(checkResult.getCheckResult());  //??????????????????
                sowEvent.setCheckDate(event.getEventAt());                  //????????????
                sowEvent.setBasicId(checkResult.getAbortionReasonId());
                sowEvent.setBasicName(checkResult.getAbortionReasonName());
                sowEvent.setExtra(ToJsonMapper.JSON_NON_EMPTY_MAPPER.toJson(checkResult));
                break;
            case FARROWING:     //??????
                DoctorFarrowingDto farrowing = getSowFarrowExtra(event, barnMap);
                sowEvent.setLiveCount(farrowing.getFarrowingLiveCount()); //?????????
                sowEvent.setHealthCount(farrowing.getHealthCount());      //?????????
                sowEvent.setWeakCount(farrowing.getWeakCount());          //?????????
                sowEvent.setMnyCount(farrowing.getMnyCount());            //????????????
                sowEvent.setJxCount(farrowing.getJxCount());              //?????????
                sowEvent.setDeadCount(farrowing.getDeadCount());          //?????????
                sowEvent.setBlackCount(farrowing.getBlackCount());        //?????????
                sowEvent.setFarrowWeight(event.getEventWeight());         //????????????(kg)
                sowEvent.setFarrowingDate(event.getEventAt());            //????????????
                sowEvent.setExtra(ToJsonMapper.JSON_NON_EMPTY_MAPPER.toJson(farrowing));
                if (!Strings.isNullOrEmpty(event.getDisease())){
                    groupEventOutId.add(event.getDisease());
                }
                break;
            case WEAN:          //??????
                DoctorWeanDto wean = getSowWeanExtra(event);
                sowEvent.setWeanCount(wean.getPartWeanPigletsCount());  //?????????
                sowEvent.setWeanAvgWeight(wean.getPartWeanAvgWeight()); //????????????
                sowEvent.setPartweanDate(event.getEventAt());           //????????????
                sowEvent.setExtra(ToJsonMapper.JSON_NON_EMPTY_MAPPER.toJson(wean));
                break;
            case FOSTERS:       //??????
                DoctorFostersDto fostersDto = getSowFosterExtra(event, basicMap);
                sowEvent.setQuantity(fostersDto.getFostersCount());
                sowEvent.setExtra(ToJsonMapper.JSON_NON_EMPTY_MAPPER.toJson(fostersDto));
                if (!Strings.isNullOrEmpty(event.getCustomer())){
                    groupEventOutId.add(event.getCustomer());
                }
                if (!Strings.isNullOrEmpty(event.getServiceType())) {
                    groupEventOutId.add("chgToMoveIn" + event.getServiceType());
                }
                break;
            case FOSTERS_BY:    //?????????
                DoctorFostersDto fostersDto1 = getSowFosterExtra(event, basicMap);
                sowEvent.setQuantity(fostersDto1.getFostersCount());
                sowEvent.setExtra(ToJsonMapper.JSON_NON_EMPTY_MAPPER.toJson(getSowFosterExtra(event, basicMap)));
                break;
            case PIGLETS_CHG:   //????????????
                DoctorPigletsChgDto pigletsChgDto = getSowPigletChangeExtra(event, basicMap, changeReasonMap, customerMap);
                sowEvent.setQuantity(pigletsChgDto.getPigletsCount());
                sowEvent.setWeight(pigletsChgDto.getPigletsWeight());
                sowEvent.setPrice(pigletsChgDto.getPigletsPrice());
                sowEvent.setCustomerId(pigletsChgDto.getPigletsCustomerId());
                sowEvent.setCustomerName(pigletsChgDto.getPigletsCustomerName());
                sowEvent.setExtra(ToJsonMapper.JSON_NON_EMPTY_MAPPER.toJson(getSowPigletChangeExtra(event, basicMap, changeReasonMap, customerMap)));
                if (!Strings.isNullOrEmpty(event.getDisease())){
                    groupEventOutId.add(event.getDisease());
                }
                break;
            default:
                break;
        }
        return sowEvent;
    }

    //??????????????????extra
    private DoctorChgLocationDto getSowTranBarnExtra(View_EventListSow event, Map<String, DoctorBarn> barnMap) {
        DoctorChgLocationDto transBarn = new DoctorChgLocationDto();
        transBarn.setChangeLocationDate(event.getEventAt());
        DoctorBarn fromBarn = barnMap.get(event.getBarnOutId());    //????????????
        if (fromBarn != null) {
            transBarn.setChgLocationFromBarnId(fromBarn.getId());
            transBarn.setChgLocationFromBarnName(fromBarn.getName());
        }
        DoctorBarn toBarn = barnMap.get(event.getToBarnOutId());    //????????????
        if (toBarn != null) {
            transBarn.setChgLocationToBarnId(toBarn.getId());
            transBarn.setChgLocationToBarnName(toBarn.getName());
        }
        return transBarn;
    }

    //?????????????????????extra
    private DoctorRemovalDto getSowRemovalExtra(View_EventListSow event, Map<String, DoctorCustomer> customerMap,
                                                Map<Integer, Map<String, DoctorBasic>> basicMap, Map<String, DoctorBarn> barnMap,
                                                Map<String, DoctorChangeReason> changeReasonMap) {
        DoctorRemovalDto remove = new DoctorRemovalDto();

        //????????????, ????????????
        DoctorBasic changeType = basicMap.get(DoctorBasic.Type.CHANGE_TYPE.getValue()).get(event.getChangeTypeName());
        remove.setChgTypeId(changeType == null ? null : changeType.getId());
        remove.setChgTypeName(event.getChgType());
        DoctorChangeReason reason = changeReasonMap.get(event.getChgReason());
        remove.setChgReasonId(reason == null ? null : reason.getId());
        remove.setChgReasonName(event.getChgReason());

        //?????? ?????????
        remove.setWeight(event.getEventWeight());
        remove.setPrice(event.getPrice());
        remove.setSum(event.getAmount());
        remove.setRemark(event.getRemark());

        //?????? ??????
        DoctorBarn barn = barnMap.get(event.getBarnOutId());
        remove.setToBarnId(barn == null ? null : barn.getId());
        DoctorCustomer customer = customerMap.get(event.getCustomer());
        remove.setCustomerId(customer == null ? null : customer.getId());
        return remove;
    }

    //??????????????????extra
    private DoctorFarmEntryDto getSowEntryExtra(View_EventListSow event, Map<Integer, Map<String, DoctorBasic>> basicMap, Map<String, DoctorBarn> barnMap) {
        DoctorFarmEntryDto entry = new DoctorFarmEntryDto();

        entry.setEarCode(event.getPigCode()); //???????????????
        entry.setParity(event.getParity()); //??????????????????
        entry.setLeft(event.getLeftCount());
        entry.setRight(event.getRightCount());
        entry.setPigType(DoctorPig.PigSex.SOW.getKey());  //??????: ??????
        entry.setPigCode(event.getPigCode());       // pig code ??? ??????
        entry.setBirthday(event.getBirthDate());      // ?????????
        entry.setInFarmDate(event.getInFarmDate());    // ????????????
        entry.setFatherCode(event.getPigFatherCode());    // ??????Code ???????????????
        entry.setMotherCode(event.getPigMotherCode());    // ???Code ???????????????
        entry.setEntryMark(event.getRemark());     // ?????????
        entry.setSource(event.getSource());

        DoctorBarn barn = barnMap.get(event.getBarnOutId());
        if (barn != null) {
            entry.setBarnId(barn.getId());
            entry.setBarnName(barn.getName());
        }
        //?????? ??????
        DoctorBasic breed = basicMap.get(DoctorBasic.Type.BREED.getValue()).get(event.getBreed());
        entry.setBreed(breed == null ? null : breed.getId());         //??????Id ???basic Info???
        entry.setBreedName(event.getBreed());     //????????????

        DoctorBasic gene = basicMap.get(DoctorBasic.Type.GENETICS.getValue()).get(event.getGenetic());
        entry.setBreedType(gene == null ? null : gene.getId());     //??????Id  (basic info)
        entry.setBreedTypeName(event.getGenetic()); //????????????
        return entry;
    }

    //????????????????????????extra
    private DoctorMatingDto getSowMatingExtra(View_EventListSow event, Map<String, DoctorPig> boarMap, Map<String, Long> subMap) {
        DoctorMatingDto mating = new DoctorMatingDto();
        mating.setMatingDate(event.getEventAt()); // ????????????
        mating.setOperatorName(event.getStaffName()); // ????????????
        mating.setOperatorId(subMap.get(event.getStaffName()));
        mating.setMattingMark(event.getRemark()); // ??????mark
        mating.setJudgePregDate(event.getFarrowDate()); //????????????

        // ????????????
        MatingType type = MatingType.from(event.getServiceType());
        mating.setMatingType(type == null ? null : type.getKey());

        //????????????
        DoctorPig matingPig = boarMap.get(event.getBoarCode());
        mating.setMatingBoarPigId(matingPig == null ? null : matingPig.getId());
        mating.setMatingBoarPigCode(event.getBoarCode());
        return mating;
    }

    //????????????????????????extra
    private DoctorPregChkResultDto getSowPregCheckExtra(View_EventListSow event) {
        DoctorPregChkResultDto preg = new DoctorPregChkResultDto();
        preg.setCheckDate(event.getEventAt());
        preg.setCheckMark(event.getRemark());

        //??????????????????
        PregCheckResult result = from(event.getPregCheckResult());
        preg.setCheckResult(result == null ? null : result.getKey());
        return preg;
    }

    //??????????????????extra
    private DoctorFarrowingDto getSowFarrowExtra(View_EventListSow event, Map<String, DoctorBarn> barnMap) {
        DoctorFarrowingDto farrow = new DoctorFarrowingDto();
        farrow.setFarrowingDate(event.getEventAt());       // ????????????
        farrow.setWeakCount(event.getWeakCount());         // ????????????
        farrow.setMnyCount(event.getMummyCount());         // ???????????????
        farrow.setJxCount(event.getJxCount());             // ????????????
        farrow.setDeadCount(event.getDeadCount());         // ????????????
        farrow.setBlackCount(event.getBlackCount());       // ????????????
        farrow.setHealthCount(event.getHealthyCount());    // ????????????
        farrow.setFarrowingLiveCount(event.getHealthyCount() + event.getWeakCount()); //????????? = ??? + ???
        farrow.setFarrowRemark(event.getRemark());
        farrow.setBirthNestAvg(event.getEventWeight());    //????????????
        farrow.setFarrowStaff1(event.getStaffName());  //?????????1
        farrow.setFarrowStaff2(event.getStaffName());  //?????????2
        farrow.setFarrowIsSingleManager(event.getIsSingleManage());    //??????????????????
        farrow.setGroupCode(event.getToGroupCode());   // ????????????Code
        farrow.setNestCode(event.getNestCode()); // ??????

        //????????????
        FarrowingType farrowingType = FarrowingType.from(event.getFarrowType());
        farrow.setFarrowingType(farrowingType == null ? null : farrowingType.getKey());

        //????????????
        DoctorBarn farrowBarn = barnMap.get(event.getBarnOutId());
        if (farrowBarn != null) {
            farrow.setBarnId(farrowBarn.getId());
            farrow.setBarnName(farrowBarn.getName());
        }
        return farrow;
    }

    //??????????????????extra
    private DoctorWeanDto getSowWeanExtra(View_EventListSow event) {
        DoctorWeanDto wean = new DoctorWeanDto();
        wean.setPartWeanDate(event.getEventAt()); //????????????
        wean.setPartWeanRemark(event.getRemark());
        wean.setPartWeanPigletsCount(event.getWeanCount()); //????????????
        wean.setPartWeanAvgWeight(event.getWeanWeight());   //??????????????????
        return wean;
    }

    //??????????????????extra
    private DoctorFostersDto getSowFosterExtra(View_EventListSow event, Map<Integer, Map<String, DoctorBasic>> basicMap) {
        DoctorFostersDto foster = new DoctorFostersDto();
        foster.setFostersDate(DateUtil.toDateString(event.getEventAt()));   // ????????????
        foster.setFostersCount(event.getNetOutCount());   //  ????????????
        foster.setFosterTotalWeight(event.getWeanWeight());   //???????????????
        foster.setFosterSowCode(event.getDisease());      //???????????????
        foster.setFosterSowOutId(event.getNurseSow());      //???????????????

        //????????????
        DoctorBasic reason = basicMap.get(DoctorBasic.Type.FOSTER_REASON.getValue()).get(event.getFosterReasonName());
        foster.setFosterReason(reason == null ? null : reason.getId());
        foster.setFosterReasonName(event.getFosterReasonName());
        foster.setFosterRemark(event.getRemark());
        return foster;
    }

    //??????????????????extra
    private DoctorPigletsChgDto getSowPigletChangeExtra(View_EventListSow event, Map<Integer, Map<String, DoctorBasic>> basicMap,
                                                        Map<String, DoctorChangeReason> changeReasonMap, Map<String, DoctorCustomer> customerMap) {
        DoctorPigletsChgDto change = new DoctorPigletsChgDto();
        change.setPigletsChangeDate(event.getEventAt()); // ??????????????????
        change.setPigletsCount(event.getChgCount());   // ????????????
        change.setPigletsWeight(event.getEventWeight());  // ???????????? (?????????)
        change.setPigletsPrice(event.getPrice());   // ???????????? ???????????????
        change.setPigletsSum(event.getAmount()); //  ?????????????????????
        change.setPigletsMark(event.getRemark());  //??????(?????????)
        change.setPigletsChangeTypeName(event.getChangeTypeName());
        change.setPigletsChangeReasonName(event.getChgReason());   // ??????????????????

        //????????????, ??????, ??????
        DoctorBasic changeType = basicMap.get(DoctorBasic.Type.CHANGE_TYPE.getValue()).get(event.getChangeTypeName());
        change.setPigletsChangeType(changeType == null ? null : changeType.getId());   // ??????????????????
        DoctorChangeReason reason = changeReasonMap.get(event.getChgReason());
        change.setPigletsChangeReason(reason == null ? null : reason.getId());   // ??????????????????
        DoctorCustomer customer = customerMap.get(event.getCustomer());
        change.setPigletsCustomerId(customer == null ? null : customer.getId());    //??????Id ???????????????
        return change;
    }

    //??????????????????
    private DoctorPigTrack getSowTrack(View_SowCardList card, DoctorPig sow, Map<String, DoctorBarn> barnMap, List<DoctorPigEvent> events, Long moveId) {
        if (sow == null) {
            return null;
        }

        //??????????????????????????????????????????????????????, ?????????, ????????????????????????, ??????????????????????????????
        if ("????????????".equals(card.getStatus())) {
            card.setStatus(getLeaveType(moveId, card.getPigOutId()));
        }

        //??????????????????
        PigStatus status = PigStatus.from(card.getStatus());

        DoctorPigTrack track = new DoctorPigTrack();
        track.setFarmId(sow.getFarmId());
        track.setPigId(sow.getId());
        track.setPigType(sow.getPigType());
        track.setStatus(status == null ? PigStatus.Entry.getKey() : status.getKey());
        track.setIsRemoval(sow.getIsRemoval());
        track.setWeight(card.getWeight());
        track.setOutFarmDate(DateUtil.toDate(card.getOutFarmDate()));
        track.setRemark(card.getRemark());
        track.setCurrentParity(card.getCurrentParity());
        DoctorPigEvent lastEvent = doctorPigEventDao.queryLastPigEventById(sow.getId());
        track.setCurrentEventId(notNull(lastEvent) ? lastEvent.getId() : 0L);

        if (notEmpty(events)) {
            //???????????? asc ??????
            events = events.stream().sorted(Comparator.comparing(DoctorPigEvent::getEventAt)).collect(Collectors.toList());
            track.setExtra(ToJsonMapper.JSON_NON_EMPTY_MAPPER.toJson(getSowExtraMap(events)));   //extra?????????????????????????????????????????????extra

            //????????????ids, Map<Parity, EventIds>, ??????????????????
            track.setRelEventIds(getSowRelEventIds(card.getFirstParity(), events));

            //????????????????????????
            track.setCurrentMatingCount(getSowCurrentMatingCount(events, sow));

            //???????????? ??????
            updateParity(events, track);

            //???????????????????????????????????????
            updateDoctorMateType(events);

            //??????????????????????????????
            updateNPD(events);

            //??????????????????????????????????????? ????????????????????? ????????????
            updateFlag(events);

            //???????????? ??? ?????????
            updateDuring(events);

            //??????event
            events.forEach(doctorPigEventDao::update);
        }

        //??????
        DoctorBarn barn = barnMap.get(card.getCurrentBarnOutId());
        if (barn != null) {
            track.setCurrentBarnId(barn.getId());
            track.setCurrentBarnName(barn.getName());
            track.setCurrentBarnType(barn.getPigType());
        }
        return track;
    }

    //???????????????????????????sowTrackExtraMap
    private static Map<String, Object> getSowExtraMap(List<DoctorPigEvent> events) {
        Map<String, Object> extraMap = Maps.newHashMap();
        for (DoctorPigEvent event : events) {
            if (Objects.equals(event.getType(), PigEvent.WEAN.getKey())) {
                extraMap.put("hasWeanToMating", true);
            }

            //?????????????????????????????????, extra???????????????
            if (Objects.equals(event.getType(), PigEvent.MATING.getKey())) {
                extraMap = Maps.newHashMap();
                extraMap.put("hasWeanToMating", false);
            }
            if (notEmpty(event.getExtra())) {
                extraMap.putAll(JSON_MAPPER.fromJson(event.getExtra(), JSON_MAPPER.createCollectionType(Map.class, String.class, Object.class)));

            }
        }
        return extraMap;
    }

    //?????????????????????????????????
    private static String getSowRelEventIds(Integer firstParity, List<DoctorPigEvent> events) {
        Map<Integer, String> relMap = Maps.newHashMap();
        List<Long> ids = Lists.newArrayList();
        int i = 0;
        for (DoctorPigEvent event : events) {
            if (Objects.equals(event.getType(), PigEvent.MATING.getKey())) {
                relMap.put(firstParity + i, Joiners.COMMA.join(ids));  //??????????????????, ??????????????????
                ids.clear();                //??????list, ?????????????????????
                ids.add(event.getId());     //??????????????????
                i++;                        //?????????1

            } else {
                ids.add(event.getId());
            }
        }
        //????????????????????????
        relMap.put(firstParity + i, Joiners.COMMA.join(ids));
        return ToJsonMapper.JSON_NON_EMPTY_MAPPER.toJson(relMap);
    }

    //???????????????????????????(??????, ????????????)
    private static int getSowCurrentMatingCount(List<DoctorPigEvent> events, DoctorPig sow) {
        //??????????????????0
        Boolean leave = false;
        if (Objects.equals(sow.getIsRemoval(), IsOrNot.YES.getValue())) {
            leave = true;
        }

        //????????????????????????????????????????????????????????????
        int count = 0;
        for (DoctorPigEvent event : events) {
            if (Objects.equals(event.getType(), PigEvent.MATING.getKey())) {
                count++;
                //???event????????????????????????
                event.setCurrentMatingCount(count);
            } else if (Objects.equals(event.getType(), PigEvent.TO_MATING.getKey()) //???????????????
                    || Objects.equals(event.getType(), PigEvent.WEAN.getKey()) //????????????
                    || isNotPreg(event)) {
                count = 0;
            }
        }
        if (leave) {
            return 0;
        } else {
            return count;
        }

    }

    //????????????
    private static void updateParity(List<DoctorPigEvent> events, DoctorPigTrack track) {
        List<DoctorPigEvent> revertList = Lists.reverse(events);
        int currentParity = track.getCurrentParity();
        for (DoctorPigEvent event : revertList) {
            //?????????????????????
            if (Objects.equals(event.getType(), PigEvent.FARROWING.getKey())) {
                int num = currentParity - 1;
                event.setParity(num);
                continue;
            }
            event.setParity(currentParity);
        }
    }

    //?????????????????????????????????
    private static void updateDoctorMateType(List<DoctorPigEvent> events) {
        DoctorPigEvent lastFlag = null;
        for (DoctorPigEvent event : events) {
            if (Objects.equals(event.getType(), PigEvent.ENTRY.getKey()) ||
                    Objects.equals(event.getType(), PigEvent.PREG_CHECK.getKey()) ||
                    Objects.equals(event.getType(), PigEvent.WEAN.getKey())
                    ) {
                lastFlag = event;
                continue;
            }

            //???????????????????????????
            if (Objects.equals(event.getType(), PigEvent.MATING.getKey()) && event.getCurrentMatingCount() == 1) {
                if (lastFlag == null) {
                    log.warn("sow data wrong...");
                    log.warn("sow data event:{}", event);
                    continue;
                }
                //???????????????
                if (Objects.equals(lastFlag.getType(), PigEvent.ENTRY.getKey()) && event.getParity() == 1) {
                    //???????????????
                    event.setDoctorMateType(DoctorMatingType.HP.getKey());
                }

                //?????????????????????
                if (Objects.equals(lastFlag.getType(), PigEvent.PREG_CHECK.getKey())) {
                    if (lastFlag.getPregCheckResult() != null) {
                        switch (lastFlag.getPregCheckResult()) {
                            case 2:
                                event.setDoctorMateType(DoctorMatingType.YP.getKey());
                                continue;
                            case 3:
                                event.setDoctorMateType(DoctorMatingType.LPC.getKey());
                                continue;
                            case 4:
                                event.setDoctorMateType(DoctorMatingType.FP.getKey());
                                continue;
                        }
                    } else {
                        log.warn("event sow preg check result is null, event {}", lastFlag);
                    }
                    continue;
                }

                //???????????????
                if (Objects.equals(lastFlag.getType(), PigEvent.WEAN.getKey())) {
                    event.setDoctorMateType(DoctorMatingType.DP.getKey());
                }
            }
        }
    }

    //??????????????????????????????
    private static void updateNPD(List<DoctorPigEvent> events) {
        //?????????????????????
        DoctorPigEvent lastMateFlag = null;
        //?????????????????????
        DoctorPigEvent lastWeanFlag = null;
        //?????????????????????
        DoctorPigEvent lastEnterFlag = null;
        for (DoctorPigEvent event : events) {
            //?????????????????????????????????, ????????????
            if (Objects.equals(event.getType(), PigEvent.ENTRY.getKey()) && lastMateFlag != null) {
                lastEnterFlag = event;
                continue;
            }

            //???????????????????????????
            if (Objects.equals(event.getType(), PigEvent.MATING.getKey()) && event.getCurrentMatingCount() == 1) {
                lastMateFlag = event;
                if (lastWeanFlag != null && lastMateFlag != null) {
                    int days = Days.daysBetween(new DateTime(lastMateFlag.getMattingDate()), new DateTime(lastWeanFlag.getPartweanDate())).getDays();
                    event.setDpnpd(Math.abs(days));
                    event.setNpd(Math.abs(days));
                    lastWeanFlag = null;
                    continue;
                }

                if (lastEnterFlag != null && lastMateFlag != null) {
                    int days = Days.daysBetween(new DateTime(lastMateFlag.getMattingDate()), new DateTime(lastEnterFlag.getEventAt())).getDays();
                    event.setJpnpd(Math.abs(days));
                    event.setNpd(Math.abs(days));
                    lastEnterFlag = null;
                    continue;
                }
                continue;

            }

            //?????????????????????????????????
            if (Objects.equals(event.getType(), PigEvent.PREG_CHECK.getKey()) && lastMateFlag != null) {
                int days = Days.daysBetween(new DateTime(lastMateFlag.getMattingDate()), new DateTime(event.getCheckDate())).getDays();
                if (event.getPregCheckResult() != null) {
                    switch (event.getPregCheckResult()) {
                        case 2:
                            //???????????????
                            event.setPynpd(Math.abs(days));
                            event.setNpd(Math.abs(days));
                            continue;
                        case 3:
                            //???????????????
                            event.setPlnpd(Math.abs(days));
                            event.setNpd(Math.abs(days));
                            continue;
                        case 4:
                            //???????????????
                            event.setPfnpd(Math.abs(days));
                            event.setNpd(Math.abs(days));
                            continue;
                    }
                } else {
                    log.warn("event sow preg check result is null, event {}", event);
                }
                continue;

            }

            // ????????????
            if (Objects.equals(event.getType(), PigEvent.REMOVAL.getKey()) && lastMateFlag != null) {
                //?????????????????????
                if (Objects.equals(event.getChangeTypeId(), DoctorBasicEnums.DEAD.getId())) {
                    int days = Days.daysBetween(new DateTime(lastMateFlag.getMattingDate()), new DateTime(event.getEventAt())).getDays();
                    event.setPsnpd(Math.abs(days));
                    event.setNpd(Math.abs(days));
                    continue;
                }

                //?????????????????????
                if (Objects.equals(event.getChangeTypeId(), DoctorBasicEnums.ELIMINATE.getId())) {
                    int days = Days.daysBetween(new DateTime(lastMateFlag.getMattingDate()), new DateTime(event.getEventAt())).getDays();
                    event.setPtnpd(Math.abs(days));
                    event.setNpd(Math.abs(days));
                    continue;
                }
            }

            //?????????????????????????????????, ????????????
            if (Objects.equals(event.getType(), PigEvent.WEAN.getKey()) && lastMateFlag != null) {
                lastWeanFlag = event;
                continue;
            }
        }
    }

    //??????????????????????????????????????? ????????????????????? ????????????
    private static void updateFlag(List<DoctorPigEvent> events) {
        //?????????????????????
        DoctorPigEvent lastMateFlag = null;
        for (DoctorPigEvent event : events) {
            //???????????????????????????
            if (Objects.equals(event.getType(), PigEvent.MATING.getKey()) && event.getCurrentMatingCount() == 1) {
                lastMateFlag = event;
                continue;
            }
            //????????????????????????????????? ???????????????????????????
            if (Objects.equals(event.getType(), PigEvent.WEAN.getKey())
                    && lastMateFlag != null
                    && Objects.equals(event.getPregCheckResult(), PregCheckResult.YANG.getKey())) {
                lastMateFlag.setIsImpregnation(1);
                continue;
            }

            //???????????????????????????
            if (Objects.equals(event.getType(), PigEvent.FARROWING.getKey())
                    && lastMateFlag != null) {
                //????????????????????????
                lastMateFlag.setIsDelivery(1);
                continue;
            }
        }
    }

    //???????????? ??? ?????????
    private static void updateDuring(List<DoctorPigEvent> events) {
        //?????????????????????
        DoctorPigEvent lastMateFlag = null;
        //?????????????????????
        DoctorPigEvent lastFarrowingFlag = null;
        for (DoctorPigEvent event : events) {
            //???????????????????????????
            if (Objects.equals(event.getType(), PigEvent.MATING.getKey()) && event.getCurrentMatingCount() == 1) {
                lastMateFlag = event;
                continue;
            }
            //???????????????????????????
            if (Objects.equals(event.getType(), PigEvent.FARROWING.getKey())
                    && lastMateFlag != null) {
                lastFarrowingFlag = event;
                //????????????
                int days = Days.daysBetween(new DateTime(event.getFarrowingDate()), new DateTime(lastMateFlag.getMattingDate())).getDays();
                event.setPregDays(Math.abs(days));
                continue;
            }

            //???????????????????????????
            if (Objects.equals(event.getType(), PigEvent.WEAN.getKey())
                    && lastFarrowingFlag != null) {
                //???????????????
                int days = Days.daysBetween(new DateTime(event.getPartweanDate()), new DateTime(lastFarrowingFlag.getFarrowingDate())).getDays();
                event.setFeedDays(Math.abs(days));
                continue;
            }
        }
    }

    //????????????????????????
    private static boolean isNotPreg(DoctorPigEvent event) {
        if (!Objects.equals(event.getType(), PigEvent.PREG_CHECK.getKey())) {
            return false;
        }
        DoctorPregChkResultDto result = JSON_MAPPER.fromJson(event.getExtra(), DoctorPregChkResultDto.class);
        return result != null && !Objects.equals(result.getCheckResult(), YANG.getKey());
    }

    //?????????????????????????????????
    private String getLeaveType(Long moveId, String sowOutId) {
        try {
            List<SowOutFarmSoon> soons = RespHelper.orServEx(doctorMoveDatasourceHandler
                    .findByHbsSql(moveId, SowOutFarmSoon.class, "SowOutFarmSoon", ImmutableMap.of("sowOutId", sowOutId)));
            return notEmpty(soons) ? soons.get(0).getLeaveType() : "";
        } catch (Exception e) {
            log.error("get sow leave type failed, sowOutId:{}, cause:{}", sowOutId, Throwables.getStackTraceAsString(e));
            return "";
        }
    }

    //????????????
    private void moveBoar(Long moveId, DoctorFarm farm, Map<String, DoctorBarn> barnMap, Map<Integer, Map<String, DoctorBasic>> basicMap,
                          Map<String, DoctorChangeReason> changeReasonMap, Map<String, DoctorCustomer> customerMap, Map<String, Long> subMap, Map<String, DoctorBasicMaterial> vaccMap) {
        //1. ??????DoctorPig
        List<View_BoarCardList> boarCards = RespHelper.orServEx(doctorMoveDatasourceHandler
                .findByHbsSql(moveId, View_BoarCardList.class, "DoctorPig-BoarCardList")).stream()
                .filter(loc -> isFarm(loc.getFarmOutId(), farm.getOutId()))
                .collect(Collectors.toList());
        List<DoctorPig> pigs = boarCards.stream().map(card -> getBoar(card, farm, basicMap)).collect(Collectors.toList());
        if (!pigs.isEmpty()) {
            doctorPigDao.creates(pigs);
        }

        //????????????, ?????????map
        List<DoctorPig> boarList = doctorPigDao.findPigsByFarmIdAndPigType(farm.getId(), DoctorPig.PigSex.BOAR.getKey());
        Map<String, DoctorPig> boarMap = boarList.stream()
                .collect(Collectors.toMap(DoctorPig::getOutId, v -> v));

        //2. ??????DoctorPigEvent
        List<DoctorPigEvent> boarEvents = RespHelper.orServEx(doctorMoveDatasourceHandler
                .findByHbsSql(moveId, View_EventListBoar.class, "DoctorPigEvent-EventListBoar")).stream()
                .filter(loc -> isFarm(loc.getFarmOutId(), farm.getOutId()))
                .map(event -> getBoarEvent(event, boarMap, barnMap, basicMap, subMap, customerMap, changeReasonMap, vaccMap)).collect(Collectors.toList());
        if (!boarEvents.isEmpty()) {
            doctorPigEventDao.creates(boarEvents);
        }

        //??????????????????, ??????????????????
        Map<Long, List<DoctorPigEvent>> boarEventMap = doctorPigEventDao.findByFarmIdAndKind(farm.getId(), DoctorPig.PigSex.BOAR.getKey())
                .stream().collect(Collectors.groupingBy(DoctorPigEvent::getPigId));

        //??????relEventId
        updatePigRelEventId(boarEventMap);

        //3. ??????DoctorPigTrack
        List<DoctorPigTrack> boarTracks = boarCards.stream()
                .map(card -> {
                    DoctorPig boar = boarMap.get(card.getPigOutId());
                    return getBoarTrack(card, boar, barnMap, boar == null ? null : boarEventMap.get(boar.getId()));
                })
                .filter(Arguments::notNull)
                .collect(Collectors.toList());
        if (!boarTracks.isEmpty()) {
            doctorPigTrackDao.creates(boarTracks);
        }
    }

    //????????????relEventId, ???????????????????????????????????????id
    private void updatePigRelEventId(Map<Long, List<DoctorPigEvent>> pigEventMap) {
        pigEventMap.values().forEach(events -> {
            //?????? ASC ??????
            events = events.stream().sorted((a, b) -> a.getEventAt().compareTo(b.getEventAt())).collect(Collectors.toList());
            List<Long> eventIds = events.stream().map(DoctorPigEvent::getId).collect(Collectors.toList());
            eventIds.add(0, null);  // ???????????????null, ????????????????????????relEventId

            for (int i = 0; i < events.size(); i++) {
                DoctorPigEvent e = events.get(i);
                e.setRelEventId(eventIds.get(i));
                doctorPigEventDao.updateRelEventId(e);
            }
        });
    }

    //????????????
    private DoctorPig getBoar(View_BoarCardList card, DoctorFarm farm, Map<Integer, Map<String, DoctorBasic>> basicMap) {
        DoctorPig boar = new DoctorPig();
        boar.setOrgId(farm.getOrgId());
        boar.setOrgName(farm.getOrgName());
        boar.setFarmId(farm.getId());
        boar.setFarmName(farm.getName());
        boar.setOutId(card.getPigOutId());           //??????OID
        boar.setPigCode(card.getPigCode());
        boar.setPigType(DoctorPig.PigSex.BOAR.getKey());  //??????????????????
        boar.setIsRemoval("?????????".equals(card.getStatus()) ? IsOrNot.YES.getValue() : IsOrNot.NO.getValue());
        boar.setPigFatherCode(card.getPigFatherCode());
        boar.setPigMotherCode(card.getPigMotherCode());
        boar.setSource(card.getSource());
        boar.setBirthDate(card.getBirthDate());
        boar.setBirthWeight(card.getBirthWeight());
        boar.setInFarmDate(card.getInFarmDate());
        boar.setInFarmDayAge(card.getInFarmDayAge());
        boar.setInitBarnName(card.getInitBarnName());
        boar.setRemark(card.getRemark());

        //??????
        DoctorBasic breed = basicMap.get(DoctorBasic.Type.BREED.getValue()).get(card.getBreed());
        boar.setBreedId(breed == null ? null : breed.getId());
        boar.setBreedName(card.getBreed());

        //??????
        DoctorBasic gene = basicMap.get(DoctorBasic.Type.GENETICS.getValue()).get(card.getGenetic());
        boar.setGeneticId(gene == null ? null : gene.getId());
        boar.setGeneticName(card.getGenetic());

        //????????????, ????????????
        BoarEntryType boarType = BoarEntryType.HGZ;
        boar.setExtraMap(ImmutableMap.of(
                DoctorFarmEntryConstants.BOAR_TYPE_ID, boarType.getKey(),
                DoctorFarmEntryConstants.BOAR_TYPE_NAME, boarType.getDesc()
        ));
        boar.setBoarType(boarType.getKey());
        return boar;
    }

    //??????????????????
    private DoctorPigEvent getBoarEvent(View_EventListBoar event, Map<String, DoctorPig> boarMap, Map<String, DoctorBarn> barnMap,
                                        Map<Integer, Map<String, DoctorBasic>> basicMap, Map<String, Long> subMap,
                                        Map<String, DoctorCustomer> customerMap, Map<String, DoctorChangeReason> changeReasonMap,
                                        Map<String, DoctorBasicMaterial> vaccMap) {
        DoctorPig boar = boarMap.get(event.getGroupOutId());
        if (boar == null) {
            return null;
        }

        DoctorPigEvent boarEvent = new DoctorPigEvent();
        boarEvent.setOrgId(boar.getOrgId());
        boarEvent.setOrgName(boar.getOrgName());
        boarEvent.setFarmId(boar.getFarmId());
        boarEvent.setFarmName(boar.getFarmName());
        boarEvent.setPigId(boar.getId());
        boarEvent.setPigCode(boar.getPigCode());
        boarEvent.setEventAt(event.getEventAt());
        boarEvent.setKind(boar.getPigType());       // ??????(??????2,??????1)
        boarEvent.setName(event.getEventName());
        boarEvent.setDesc(event.getEventDesc());
        boarEvent.setOutId(event.getEventOutId());
        boarEvent.setRemark(event.getRemark());
        boarEvent.setStatus(EventStatus.VALID.getValue());
        boarEvent.setEventSource(SourceType.MOVE.getValue());

        //????????????
        PigEvent eventType = PigEvent.from(event.getEventName());
        boarEvent.setType(eventType == null ? null : eventType.getKey());

        DoctorBarn barn = barnMap.get(event.getBarnOutId());
        if (barn != null) {
            boarEvent.setBarnId(barn.getId());
            boarEvent.setBarnName(barn.getName());
            boarEvent.setBarnType(barn.getPigType());
        }
        return getBoarEventExtra(eventType, boarEvent, event, subMap, basicMap, barnMap, customerMap, changeReasonMap, vaccMap);
    }

    //??????????????????????????????
    private DoctorPigEvent getBoarEventExtra(PigEvent eventType, DoctorPigEvent boarEvent, View_EventListBoar event, Map<String, Long> subMap,
                                             Map<Integer, Map<String, DoctorBasic>> basicMap, Map<String, DoctorBarn> barnMap,
                                             Map<String, DoctorCustomer> customerMap, Map<String, DoctorChangeReason> changeReasonMap,
                                             Map<String, DoctorBasicMaterial> vaccMap) {
        if (eventType == null) {
            return boarEvent;
        }
        //switch ????????????
        switch (eventType) {
            case CHG_LOCATION:  //??????
                DoctorChgLocationDto transBarn = new DoctorChgLocationDto();
                transBarn.setChangeLocationDate(event.getEventAt());
                DoctorBarn fromBarn = barnMap.get(event.getBarnOutId());    //????????????
                if (fromBarn != null) {
                    transBarn.setChgLocationFromBarnId(fromBarn.getId());
                    transBarn.setChgLocationFromBarnName(fromBarn.getName());
                }
                DoctorBarn toBarn = barnMap.get(event.getToBarnOutId());    //????????????
                if (toBarn != null) {
                    transBarn.setChgLocationToBarnId(toBarn.getId());
                    transBarn.setChgLocationToBarnName(toBarn.getName());
                }
                boarEvent.setExtra(ToJsonMapper.JSON_NON_EMPTY_MAPPER.toJson(transBarn));
                break;
            case CHG_FARM:    //??????(????????????, ????????????)
                DoctorChgFarmDto tranFarm = new DoctorChgFarmDto();
                tranFarm.setChgFarmDate(event.getEventAt());
                tranFarm.setFromFarmId(boarEvent.getFarmId());
                tranFarm.setFromFarmName(boarEvent.getFarmName());
                tranFarm.setFromBarnId(boarEvent.getBarnId());
                tranFarm.setFromBarnName(boarEvent.getBarnName());
                tranFarm.setRemark(event.getChgReason());
                boarEvent.setExtra(ToJsonMapper.JSON_NON_EMPTY_MAPPER.toJson(tranFarm));
                break;
            case CONDITION:  //??????
                DoctorBoarConditionDto condition = new DoctorBoarConditionDto();
                condition.setCheckAt(event.getEventAt());
                condition.setScoreHuoli(event.getScoreHuoli());
                condition.setScoreMidu(event.getScoreHuoli());
                condition.setScoreXingtai(event.getScoreXingtai());
                condition.setScoreShuliang(event.getScoreShuliang());
                condition.setWeight(event.getEventWeight());
                boarEvent.setExtra(ToJsonMapper.JSON_NON_EMPTY_MAPPER.toJson(condition));
                break;
            case DISEASE:   //??????
                DoctorDiseaseDto disease = new DoctorDiseaseDto();
                disease.setDiseaseDate(event.getEventAt());

                //??????
                DoctorBasic ddd = basicMap.get(DoctorBasic.Type.DISEASE.getValue()).get(event.getDiseaseName());
                disease.setDiseaseId(ddd == null ? null : ddd.getId());
                disease.setDiseaseName(event.getDiseaseName());
                disease.setDiseaseStaff(event.getChgReason());  //???????????????????????????
                disease.setDiseaseRemark(event.getRemark());
                boarEvent.setExtra(ToJsonMapper.JSON_NON_EMPTY_MAPPER.toJson(disease));
                break;
            case VACCINATION:  //??????
                DoctorVaccinationDto vacc = new DoctorVaccinationDto();
                vacc.setVaccinationDate(event.getEventAt());

                DoctorBasicMaterial vaccBasic = vaccMap.get(event.getVaccName());
                vacc.setVaccinationId(vaccBasic == null ? null : vaccBasic.getId());
                vacc.setVaccinationName(event.getVaccName());
                vacc.setVaccinationStaffId(subMap.get(event.getChgReason()));
                vacc.setVaccinationStaffName(event.getChgReason()); //????????????????????????
                vacc.setVaccinationRemark(event.getRemark());
                boarEvent.setExtra(ToJsonMapper.JSON_NON_EMPTY_MAPPER.toJson(vacc));
                break;
            case REMOVAL:   //??????
                DoctorRemovalDto removal = getBoarRemovalExtra(event, customerMap, basicMap, barnMap, changeReasonMap);
                boarEvent.setExtra(ToJsonMapper.JSON_NON_EMPTY_MAPPER.toJson(removal));
                boarEvent.setChangeTypeId(removal.getChgTypeId());
                boarEvent.setPrice(removal.getPrice());
                boarEvent.setAmount(removal.getSum());
                break;
            case ENTRY:     //??????
                boarEvent.setExtra(ToJsonMapper.JSON_NON_EMPTY_MAPPER.toJson(getBoarEntryExtra(event, basicMap, barnMap)));
                break;
            case SEMEN:     //??????
                boarEvent.setExtra(ToJsonMapper.JSON_NON_EMPTY_MAPPER.toJson(getBoarSemenExtra(event)));
                break;
            default:
                break;
        }
        return boarEvent;
    }

    //??????????????????extra
    private DoctorRemovalDto getBoarRemovalExtra(View_EventListBoar event, Map<String, DoctorCustomer> customerMap,
                                                 Map<Integer, Map<String, DoctorBasic>> basicMap, Map<String, DoctorBarn> barnMap,
                                                 Map<String, DoctorChangeReason> changeReasonMap) {
        DoctorRemovalDto remove = new DoctorRemovalDto();

        //????????????, ????????????
        DoctorBasic changeType = basicMap.get(DoctorBasic.Type.CHANGE_TYPE.getValue()).get(event.getChgType());
        remove.setChgTypeId(changeType == null ? null : changeType.getId());
        remove.setChgTypeName(event.getChgType());
        DoctorChangeReason reason = changeReasonMap.get(event.getChgReason());
        remove.setChgReasonId(reason == null ? null : reason.getId());
        remove.setChgReasonName(event.getChgReason());

        //?????? ?????????
        remove.setWeight(event.getEventWeight());
        remove.setPrice(event.getPrice());
        remove.setSum(event.getAmount());
        remove.setRemark(event.getRemark());

        //?????? ??????
        DoctorBarn barn = barnMap.get(event.getBarnOutId());
        remove.setToBarnId(barn == null ? null : barn.getId());
        DoctorCustomer customer = customerMap.get(event.getCustomer());
        remove.setCustomerId(customer == null ? null : customer.getId());
        return remove;
    }

    //??????????????????extra
    private DoctorFarmEntryDto getBoarEntryExtra(View_EventListBoar event, Map<Integer, Map<String, DoctorBasic>> basicMap, Map<String, DoctorBarn> barnMap) {
        DoctorFarmEntryDto entry = new DoctorFarmEntryDto();

        //??????????????????
        BoarEntryType type = BoarEntryType.from(event.getBoarType());
        entry.setBoarType(type == null ? null : type.getKey());
        entry.setBoarTypeName(event.getBoarType());
        entry.setPigType(DoctorPig.PigSex.BOAR.getKey());  //??????: ??????
        entry.setPigCode(event.getPigCode());       // pig code ??? ??????
        entry.setBirthday(event.getBirthDate());      // ?????????
        entry.setInFarmDate(event.getInFarmDate());    // ????????????
        entry.setFatherCode(event.getPigFatherCode());    // ??????Code ???????????????
        entry.setMotherCode(event.getPigMotherCode());    // ???Code ???????????????
        entry.setEntryMark(event.getRemark());     // ?????????
        entry.setSource(event.getSource());

        DoctorBarn barn = barnMap.get(event.getBarnOutId());
        if (barn != null) {
            entry.setBarnId(barn.getId());
            entry.setBarnName(barn.getName());
        }
        //?????? ??????
        DoctorBasic breed = basicMap.get(DoctorBasic.Type.BREED.getValue()).get(event.getBreed());
        entry.setBreed(breed == null ? null : breed.getId());         //??????Id ???basic Info???
        entry.setBreedName(event.getBreed());     //????????????

        DoctorBasic gene = basicMap.get(DoctorBasic.Type.GENETICS.getValue()).get(event.getGenetic());
        entry.setBreedType(gene == null ? null : gene.getId());     //??????Id  (basic info)
        entry.setBreedTypeName(event.getGenetic()); //????????????
        return entry;
    }

    //??????????????????extra
    private DoctorSemenDto getBoarSemenExtra(View_EventListBoar event) {
        DoctorSemenDto semen = new DoctorSemenDto();
        semen.setSemenDate(event.getEventAt());       //????????????
        semen.setSemenWeight(event.getEventWeight());     //????????????
        semen.setDilutionRatio(event.getDilutionRatio());   //????????????
        semen.setDilutionWeight(event.getDilutionWeight());  //???????????????
        semen.setSemenDensity(event.getSemenDensity());    //????????????
        semen.setSemenActive(event.getSemenActive());     //????????????
        semen.setSemenJxRatio(event.getSemenJxRatio());    //???????????????
        semen.setSemenPh(event.getSemenPh());         //??????PH
        semen.setSemenTotal(Double.valueOf(event.getScore()));      //????????????!!!
        semen.setSemenRemark(event.getRemark());     //???????????????????????????
        return semen;
    }

    //??????????????????
    private DoctorPigTrack getBoarTrack(View_BoarCardList card, DoctorPig boar, Map<String, DoctorBarn> barnMap, List<DoctorPigEvent> events) {
        if (boar == null) {
            return null;
        }
        DoctorPigTrack track = new DoctorPigTrack();
        track.setFarmId(boar.getFarmId());
        track.setPigId(boar.getId());
        track.setPigType(boar.getPigType());
        track.setStatus(Objects.equals(boar.getIsRemoval(), IsOrNot.NO.getValue()) ? PigStatus.BOAR_ENTRY.getKey() : PigStatus.BOAR_LEAVE.getKey());
        track.setIsRemoval(boar.getIsRemoval());
        track.setWeight(card.getWeight());
        track.setOutFarmDate(DateUtil.toDate(card.getOutFarmDate()));
        track.setRemark(card.getRemark());
        DoctorPigEvent lastEvent = doctorPigEventDao.queryLastPigEventById(boar.getId());
        track.setCurrentEventId(notNull(lastEvent) ? lastEvent.getId() : 0L);

        if (notEmpty(events)) {
            //???????????? asc ??????
            events = events.stream().sorted((a, b) -> a.getEventAt().compareTo(b.getEventAt())).collect(Collectors.toList());
            Map<String, Object> extraMap = Maps.newHashMap();
            events.forEach(event -> extraMap.putAll(JSON_MAPPER.fromJson(event.getExtra(), JSON_MAPPER.createCollectionType(Map.class, String.class, Object.class))));
            track.setExtra(ToJsonMapper.JSON_NON_EMPTY_MAPPER.toJson(extraMap));   //extra??????????????????extra
            track.setRelEventIds(Joiners.COMMA.join(events.stream().map(DoctorPigEvent::getId).collect(Collectors.toList()))); //????????????ids, ????????????
        }

        //??????
        DoctorBarn barn = barnMap.get(card.getCurrentBarnOutId());
        if (barn != null) {
            track.setCurrentBarnId(barn.getId());
            track.setCurrentBarnName(barn.getName());
        }
        return track;
    }

    //??????????????????
    private DoctorGroupEvent getGroupEvent(Map<String, DoctorGroup> groupMap, View_EventListGain gainEvent, Map<String, Long> subMap,
                                           Map<String, DoctorBarn> barnMap, Map<Integer, Map<String, DoctorBasic>> basicMap,
                                           Map<String, DoctorChangeReason> changeReasonMap, Map<String, DoctorCustomer> customerMap,
                                           Map<String, DoctorBasicMaterial> vaccMap, Map<String, DoctorPig> pigMap, List<String> groupEventOutId) {
        DoctorGroup group = groupMap.get(gainEvent.getGroupOutId());
        if (group == null) {
            return null;
        }

        DoctorGroupEvent event = new DoctorGroupEvent();
        event.setOrgId(group.getOrgId());
        event.setOrgName(group.getOrgName());
        event.setFarmId(group.getFarmId());
        event.setFarmName(group.getFarmName());
        event.setGroupId(group.getId());
        event.setGroupCode(group.getGroupCode());
        event.setEventAt(gainEvent.getEventAt());
        event.setStatus(EventStatus.VALID.getValue());
        event.setEventSource(SourceType.MOVE.getValue());

        //??????????????????
        GroupEventType type = GroupEventType.from(gainEvent.getEventTypeName());

        event.setType(type == null ? null : type.getValue());
        event.setName(gainEvent.getEventTypeName());
        event.setDesc(gainEvent.getEventDesc());

        //??????????????????
        DoctorBarn barn = barnMap.get(gainEvent.getBarnOutId());
        if (barn != null) {
            event.setBarnId(barn.getId());
            event.setBarnName(barn.getName());
        }
        event.setPigType(group.getPigType());
        event.setQuantity(gainEvent.getQuantity());
        event.setWeight(gainEvent.getWeight());
        if (gainEvent.getWeight() == null || gainEvent.getQuantity() == null || gainEvent.getQuantity() == 0) {
            event.setAvgWeight(0D);
        } else {
            event.setAvgWeight(gainEvent.getWeight() / gainEvent.getQuantity());
        }
        event.setAvgDayAge(gainEvent.getAvgDayAge());
        event.setIsAuto(gainEvent.getIsAuto());
        event.setOutId(gainEvent.getGroupEventOutId());
        event.setRemark(gainEvent.getRemark());
        if (groupEventOutId.contains(event.getOutId())) {
            event.setRelPigEventId(-1L);
            event.setSowId(-1L);
            event.setIsAuto(IsOrNot.YES.getValue());
        }

        if (groupEventOutId.contains("chgToMoveIn" + event.getOutId())) {
            event.setSowId(-1L);
            event.setIsAuto(IsOrNot.YES.getValue());
        }
        return getGroupEventExtra(type, event, gainEvent, basicMap, barnMap, groupMap, group, subMap, changeReasonMap, customerMap, vaccMap, pigMap);
    }

    //????????????????????????????????????
    @SuppressWarnings("unchecked")
    private DoctorGroupEvent getGroupEventExtra(GroupEventType type, DoctorGroupEvent event, View_EventListGain gainEvent,
                                                Map<Integer, Map<String, DoctorBasic>> basicMap, Map<String, DoctorBarn> barnMap,
                                                Map<String, DoctorGroup> groupMap, DoctorGroup group, Map<String, Long> subMap,
                                                Map<String, DoctorChangeReason> changeReasonMap, Map<String, DoctorCustomer> customerMap,
                                                Map<String, DoctorBasicMaterial> vaccMap, Map<String, DoctorPig> pigMap) {
        if (type == null) {
            return event;
        }

        switch (type) {
            case NEW:
                DoctorNewGroupInput newEvent = new DoctorNewGroupInput();
                PigSource source = PigSource.from(gainEvent.getSource());
                newEvent.setSource(source == null ? null : source.getKey());
                event.setExtraMap(newEvent);
                break;
            case MOVE_IN:
                DoctorMoveInGroupInput moveIn = getMoveInEvent(gainEvent, basicMap, groupMap, group);
                event.setInType(moveIn.getInType());
                event.setOtherBarnId(moveIn.getFromBarnId());
                event.setOtherBarnType(moveIn.getFromBarnType());
                event.setExtraMap(moveIn);
                event.setTransGroupType(getTransType(moveIn.getInType(), event.getPigType(), moveIn.getFromBarnType()).getValue());  //????????????????????????
                break;
            case CHANGE:
                DoctorChangeGroupInput changeEvent = getChangeEvent(gainEvent, basicMap, changeReasonMap, customerMap);
                event.setExtraMap(changeEvent);
                event.setChangeTypeId(changeEvent.getChangeTypeId());
                event.setPrice(changeEvent.getPrice());
                event.setAmount(changeEvent.getAmount());

                //?????? ??????????????????
                if (event.getPrice() == 0L) {
                    event.setWeight(0D);
                } else {
                    event.setWeight(event.getAmount() / event.getPrice() * 1D);
                }
                if (event.getWeight() == null || event.getQuantity() == null || event.getQuantity() == 0) {
                    event.setAvgWeight(0D);
                } else {
                    event.setAvgWeight(event.getWeight() / event.getQuantity());
                }
                break;
            case TRANS_GROUP:
                DoctorTransGroupInput transGroupEvent = getTranGroupEvent(gainEvent, basicMap, barnMap, groupMap, group);
                event.setOtherBarnId(transGroupEvent.getToBarnId());
                event.setOtherBarnType(transGroupEvent.getToBarnType());
                event.setExtraMap(transGroupEvent);
                event.setTransGroupType(getTransType(null, event.getPigType(), transGroupEvent.getToBarnType()).getValue());  //????????????????????????
                break;
            case TURN_SEED:
                DoctorTurnSeedGroupInput turnSeed = getTurnSeedEvent(gainEvent, basicMap, barnMap, pigMap);
                event.setOtherBarnId(turnSeed.getToBarnId());
                event.setOtherBarnType(turnSeed.getToBarnType());
                event.setExtraMap(turnSeed);
                break;
            case LIVE_STOCK:
                DoctorLiveStockGroupInput liveStock = new DoctorLiveStockGroupInput();
                liveStock.setEventAt(DateUtil.toDateTimeString(gainEvent.getEventAt()));
                event.setExtraMap(liveStock);
                break;
            case DISEASE:
                DoctorDiseaseGroupInput disease = new DoctorDiseaseGroupInput();
                DoctorBasic basic = basicMap.get(DoctorBasic.Type.DISEASE.getValue()).get(gainEvent.getDiseaseName());
                disease.setDiseaseId(basic == null ? null : basic.getId());
                disease.setDiseaseName(gainEvent.getDiseaseName());
                disease.setDoctorId(subMap.get(gainEvent.getStaffName()));
                disease.setDoctorName(gainEvent.getStaffName());
                disease.setQuantity(gainEvent.getQuantity());
                event.setExtraMap(disease);
                break;
            case ANTIEPIDEMIC:
                DoctorAntiepidemicGroupInput anti = new DoctorAntiepidemicGroupInput();

                //??????
                DoctorBasicMaterial vaccBasic = vaccMap.get(gainEvent.getNotDisease());
                anti.setVaccinId(vaccBasic == null ? null : vaccBasic.getId());
                anti.setVaccinName(gainEvent.getNotDisease());

                VaccinResult result = VaccinResult.from(gainEvent.getContext());
                anti.setVaccinResult(result == null ? null : result.getValue());
                anti.setVaccinStaffId(subMap.get(gainEvent.getStaffName()));
                anti.setVaccinStaffName(gainEvent.getStaffName());
                anti.setQuantity(gainEvent.getQuantity());
                event.setExtraMap(anti);
                break;
            case TRANS_FARM: //?????????????????????, ???????????????
                DoctorTransFarmGroupInput transFarmEvent = getTranFarmEvent(gainEvent, basicMap, barnMap, groupMap, group);
                event.setOtherBarnId(transFarmEvent.getToBarnId());
                event.setOtherBarnType(transFarmEvent.getToBarnType());
                event.setExtraMap(transFarmEvent);
                event.setTransGroupType(DoctorGroupEvent.TransGroupType.OUT.getValue());
                break;
            case CLOSE:
                DoctorCloseGroupInput close = new DoctorCloseGroupInput();
                close.setEventAt(DateUtil.toDateTimeString(gainEvent.getEventAt()));
                event.setExtraMap(close);
                break;
            default:
                break;
        }
        return event;
    }

    //????????????????????????
    private static DoctorGroupEvent.TransGroupType getTransType(Integer inType, Integer pigType, Integer toBarnType) {
        if (inType != null && !Objects.equals(inType, InType.GROUP.getValue())) {
            return DoctorGroupEvent.TransGroupType.OUT;
        }
        return Objects.equals(pigType, toBarnType) || (FARROW_TYPES.contains(pigType) && FARROW_TYPES.contains(toBarnType)) ?
                DoctorGroupEvent.TransGroupType.IN : DoctorGroupEvent.TransGroupType.OUT;
    }

    //????????????????????????
    private DoctorMoveInGroupInput getMoveInEvent(View_EventListGain gainEvent, Map<Integer, Map<String, DoctorBasic>> basicMap, Map<String, DoctorGroup> groupMap, DoctorGroup group) {
        DoctorMoveInGroupInput moveIn = new DoctorMoveInGroupInput();

        InType inType = InType.from(gainEvent.getInTypeName());
        moveIn.setInType(inType == null ? null : inType.getValue());
        moveIn.setInTypeName(gainEvent.getInTypeName());

        //??????
        PigSource source = PigSource.from(gainEvent.getSource());
        moveIn.setSource(source == null ? null : source.getKey());
        moveIn.setSex(DoctorGroupTrack.Sex.MIX.getValue());

        //??????
        DoctorBasic basic = basicMap.get(DoctorBasic.Type.BREED.getValue()).get(gainEvent.getBreed());
        moveIn.setBreedId(basic == null ? null : basic.getId());
        moveIn.setBreedName(gainEvent.getBreed());

        //????????????
        moveIn.setFromBarnId(group.getCurrentBarnId());
        moveIn.setFromBarnName(group.getCurrentBarnName());
        moveIn.setFromBarnType(group.getPigType());

        //??????????????????, ????????????????????????
        DoctorGroup fromGroup = groupMap.get(gainEvent.getToGroupOutId());
        if (fromGroup != null) {
            moveIn.setFromGroupId(fromGroup.getId());
            moveIn.setFromGroupCode(fromGroup.getGroupCode());
        }
        moveIn.setBoarQty(gainEvent.getBoarQty());
        moveIn.setSowQty(gainEvent.getSowQty());
        moveIn.setAmount(gainEvent.getAmount());
        return moveIn;
    }

    //????????????
    private DoctorChangeGroupInput getChangeEvent(View_EventListGain gainEvent, Map<Integer, Map<String, DoctorBasic>> basicMap, Map<String, DoctorChangeReason> changeReasonMap, Map<String, DoctorCustomer> custormerMap) {
        DoctorChangeGroupInput change = new DoctorChangeGroupInput();

        //????????????, ??????, ??????, ??????
        DoctorBasic changeType = basicMap.get(DoctorBasic.Type.CHANGE_TYPE.getValue()).get(gainEvent.getChangTypeName());
        change.setChangeTypeId(changeType == null ? null : changeType.getId());
        change.setChangeTypeName(gainEvent.getChangTypeName());

        DoctorChangeReason reason = changeReasonMap.get(gainEvent.getChangeReasonName());
        change.setChangeReasonId(reason == null ? null : reason.getId());
        change.setChangeReasonName(gainEvent.getChangeReasonName());

        DoctorBasic basic = basicMap.get(DoctorBasic.Type.BREED.getValue()).get(gainEvent.getBreed());
        change.setBreedId(basic == null ? null : basic.getId());
        change.setBreedName(gainEvent.getBreed());

        DoctorCustomer customer = custormerMap.get(gainEvent.getCustomer());
        change.setCustomerId(customer == null ? null : customer.getId());
        change.setCustomerName(gainEvent.getCustomer());

        //?????? ?????? ??????
        change.setPrice(gainEvent.getPrice());
        change.setAmount(gainEvent.getAmount());
        change.setBoarQty(gainEvent.getBoarQty());
        change.setSowQty(gainEvent.getSowQty());
        return change;
    }

    //????????????
    private DoctorTransGroupInput getTranGroupEvent(View_EventListGain gainEvent, Map<Integer, Map<String, DoctorBasic>> basicMap,
                                                    Map<String, DoctorBarn> barnMap, Map<String, DoctorGroup> groupMap, DoctorGroup group) {
        DoctorTransGroupInput transGroup = new DoctorTransGroupInput();
        transGroup.setEventAt(DateUtil.toDateTimeString(gainEvent.getEventAt()));

        //???????????? ??????
        transGroup.setFromBarnId(group.getCurrentBarnId());
        transGroup.setFromBarnName(group.getCurrentBarnName());
        transGroup.setFromGroupId(group.getId());
        transGroup.setFromGroupCode(group.getGroupCode());

        //ChgReason=??????????????? ??????????????????????????? Treament: ??????????????? OutDest: ????????????
        if ("????????????".equals(gainEvent.getChangeReasonName())) {
            DoctorBarn barn = barnMap.get(gainEvent.getContext()); //??????TreatMent
            if (barn != null) {
                transGroup.setToBarnId(barn.getId());
                transGroup.setToBarnName(barn.getName());
                transGroup.setToBarnType(barn.getPigType());
            }
            DoctorGroup toGroup = groupMap.get(gainEvent.getToBarnOutId()); //?????? OutDestination
            if (toGroup != null) {
                transGroup.setToGroupId(toGroup.getId());
                transGroup.setToGroupCode(toGroup.getGroupCode());
            }
        } else {
            DoctorBarn barn = barnMap.get(gainEvent.getToBarnOutId()); // ????????? "????????????" ??????
            if (barn != null) {
                transGroup.setToBarnId(barn.getId());
                transGroup.setToBarnName(barn.getName());
                transGroup.setToBarnType(barn.getPigType());
            }
            DoctorGroup toGroup = groupMap.get(gainEvent.getToGroupOutId());
            if (toGroup != null) {
                transGroup.setToGroupId(toGroup.getId());
                transGroup.setToGroupCode(toGroup.getGroupCode());
            }
            //?????????????????? Treatment
            IsOrNot is = IsOrNot.from(gainEvent.getContext());
            transGroup.setIsCreateGroup(is == null ? null : is.getValue());
        }

        //??????
        DoctorBasic basic = basicMap.get(DoctorBasic.Type.BREED.getValue()).get(gainEvent.getBreed());
        transGroup.setBreedId(basic == null ? null : basic.getId());
        transGroup.setBreedName(gainEvent.getBreed());

        transGroup.setBoarQty(gainEvent.getBoarQty());
        transGroup.setSowQty(gainEvent.getSowQty());
        return transGroup;
    }

    //????????????
    private DoctorTransFarmGroupInput getTranFarmEvent(View_EventListGain gainEvent, Map<Integer, Map<String, DoctorBasic>> basicMap,
                                                       Map<String, DoctorBarn> barnMap, Map<String, DoctorGroup> groupMap, DoctorGroup group) {
        DoctorTransFarmGroupInput transFarm = new DoctorTransFarmGroupInput();

        //???????????? ??????
        transFarm.setFromFarmId(group.getFarmId());
        transFarm.setFromFarmName(group.getFarmName());
        transFarm.setFromBarnId(group.getCurrentBarnId());
        transFarm.setFromBarnName(group.getCurrentBarnName());
        transFarm.setFromGroupId(group.getId());
        transFarm.setFromGroupCode(group.getGroupCode());

        DoctorBarn barn = barnMap.get(gainEvent.getToBarnOutId());
        if (barn != null) {
            transFarm.setToBarnId(barn.getId());
            transFarm.setToBarnName(barn.getName());
            transFarm.setToBarnType(barn.getPigType());
        }
        DoctorGroup toGroup = groupMap.get(gainEvent.getToGroupOutId());
        if (toGroup != null) {
            transFarm.setToGroupId(toGroup.getId());
            transFarm.setToGroupCode(toGroup.getGroupCode());
        }
        //?????????????????? Treatment
        IsOrNot is = IsOrNot.from(gainEvent.getContext());
        transFarm.setIsCreateGroup(is == null ? null : is.getValue());

        //??????
        DoctorBasic basic = basicMap.get(DoctorBasic.Type.BREED.getValue()).get(gainEvent.getBreed());
        transFarm.setBreedId(basic == null ? null : basic.getId());
        transFarm.setBreedName(gainEvent.getBreed());

        transFarm.setBoarQty(gainEvent.getBoarQty());
        transFarm.setSowQty(gainEvent.getSowQty());
        return transFarm;
    }

    //???????????????????????????
    private DoctorTurnSeedGroupInput getTurnSeedEvent(View_EventListGain gainEvent, Map<Integer, Map<String, DoctorBasic>> basicMap,
                                                      Map<String, DoctorBarn> barnMap, Map<String, DoctorPig> pigMap) {
        DoctorTurnSeedGroupInput turnSeed = new DoctorTurnSeedGroupInput();
        DoctorPig pig = pigMap.get(gainEvent.getPigCode());
        //turnSeed.setPigId(pig == null ? null : pig.getId());

        //???????????????
        turnSeed.setPigCode(gainEvent.getPigCode());

        //????????????
        turnSeed.setEventAt(DateUtil.toDateTimeString(gainEvent.getEventAt()));
        turnSeed.setBirthDate(DateUtil.toDateTimeString(gainEvent.getBirthDate()));

        //?????? ?????? ?????? ??????
        DoctorBasic breed = basicMap.get(DoctorBasic.Type.BREED.getValue()).get(gainEvent.getBreed());
        turnSeed.setBreedId(breed == null ? null : breed.getId());
        turnSeed.setBreedName(gainEvent.getBreed());

        DoctorBasic genetic = basicMap.get(DoctorBasic.Type.BREED.getValue()).get(gainEvent.getSource()); //????????? source ?????????
        turnSeed.setGeneticId(genetic == null ? null : genetic.getId());
        turnSeed.setGeneticName(gainEvent.getSource());

        DoctorBarn barn = barnMap.get(gainEvent.getContext());
        if (barn != null) {
            turnSeed.setToBarnId(barn.getId());
            turnSeed.setToBarnName(barn.getName());
            turnSeed.setToBarnType(barn.getPigType());
        }

        return turnSeed;
    }

    //??????????????????
    private DoctorGroupTrack getGroupTrack(DoctorGroup group, Proc_InventoryGain gain, List<DoctorGroupEvent> events) {
        DoctorGroupTrack groupTrack = new DoctorGroupTrack();
        groupTrack.setGroupId(group.getId());
        groupTrack.setSex(DoctorGroupTrack.Sex.MIX.getValue());

        //????????????????????????, ?????????????????????????????????0
        if (Objects.equals(group.getStatus(), DoctorGroup.Status.CLOSED.getValue())) {
            groupTrack.setAvgDayAge(gain == null ? 0 : gain.getAvgDayAge());    //?????????????????????
            return getCloseGroupTrack(groupTrack, group, events);
        }

        //??????????????????, ??????
        if (gain != null) {
            groupTrack.setAvgDayAge(gain.getAvgDayAge());
            groupTrack.setQuantity(MoreObjects.firstNonNull(gain.getQuantity(), 0));
            groupTrack.setBoarQty(gain.getQuantity() / 2);
            groupTrack.setSowQty(groupTrack.getQuantity() - groupTrack.getBoarQty());
            groupTrack.setBirthDate(DateTime.now().minusDays(groupTrack.getAvgDayAge() == null ? 1 : groupTrack.getAvgDayAge()).toDate());
        }
        DoctorGroupEvent lastEvent = events.stream().sorted((a, b) -> b.getEventAt().compareTo(a.getEventAt())).findFirst().orElse(null);
        groupTrack.setRelEventId(lastEvent == null ? null : lastEvent.getId());

        //??????????????????
        return doctorGroupReportManager.updateGroupTrackReport(groupTrack, group.getPigType());
    }

    //???????????????????????????
    private DoctorGroupTrack getCloseGroupTrack(DoctorGroupTrack groupTrack, DoctorGroup group, List<DoctorGroupEvent> events) {
        DoctorGroupEvent closeEvent = events.stream().filter(e -> Objects.equals(GroupEventType.CLOSE.getValue(), e.getType())).findFirst().orElse(null);
        if (closeEvent != null) {
            groupTrack.setRelEventId(closeEvent.getId());
        }
        groupTrack.setBirthDate(group.getOpenAt());
        groupTrack.setQuantity(0);
        groupTrack.setBoarQty(0);
        groupTrack.setSowQty(0);
        return groupTrack;
    }

    //????????????
    private DoctorGroup getGroup(DoctorFarm farm, View_GainCardList gain, Map<String, DoctorBarn> barnMap,
                                 Map<Integer, Map<String, DoctorBasic>> basicMap, Map<String, Long> subMap) {
        DoctorGroup group = BeanMapper.map(gain, DoctorGroup.class);

        group.setOrgId(farm.getOrgId());
        group.setOrgName(farm.getOrgName());
        group.setFarmId(farm.getId());
        group.setFarmName(farm.getName());

        //??????
        DoctorBarn barn = barnMap.get(gain.getBarnOutId());
        if (barn != null) {
            group.setInitBarnId(barn.getId());
            group.setInitBarnName(barn.getName());
            group.setCurrentBarnId(barn.getId());
            group.setCurrentBarnName(barn.getName());
            group.setPigType(barn.getPigType());
        }
        //??????
        if (notEmpty(gain.getBreed())) {
            DoctorBasic breed = basicMap.get(DoctorBasic.Type.BREED.getValue()).get(gain.getBreed());
            group.setBreedId(breed == null ? null : breed.getId());
            group.setBreedName(gain.getBreed());
        }
        //??????
        if (notEmpty(gain.getGenetic())) {
            DoctorBasic gene = basicMap.get(DoctorBasic.Type.GENETICS.getValue()).get(gain.getGenetic());
            group.setGeneticId(gene == null ? null : gene.getId());
            group.setGeneticName(gain.getGenetic());
        }
        if (notEmpty(gain.getStaffName())) {
            group.setStaffId(subMap.get(gain.getStaffName()));
            group.setStaffName(gain.getStaffName());
        }
        if (DoctorGroup.Status.CREATED.getValue() == group.getStatus()) {
            group.setCloseAt(null);
        }
        return group;
    }

    private static String brace(String name) {
        return "'" + name + "'";
    }

    //????????????id????????????
    private static boolean isFarm(String farmOID, String outId) {
        return Objects.equals(farmOID, outId);
    }

    private Integer parity;
    private String boarCode;
    private Integer statusBefore;
    private Integer statusAfter;
    private Integer quantity;
    private Integer quantityChange;
    private Boolean isWeanToMate;

    public void updateParityAndBoarCode(DoctorFarm farm) {
        List<DoctorPigEvent> doctorPigEvensList = doctorPigEventDao.list(ImmutableMap.of("farmId", farm.getId(), "type", PigEvent.ENTRY.getKey(), "kind", 1));
        updateParityAndBoarCodeByEntryEvents(doctorPigEvensList);
    }

    private void updateParityAndBoarCodeByEntryEvents(List<DoctorPigEvent> doctorPigEvensList) {
        doctorPigEvensList.forEach(doctorPigEvent -> {
            //log.info("update doctor_pig_events start: {}", doctorPigEvent);
            List<DoctorPigEvent> lists = doctorPigEventDao.queryAllEventsByPigIdForASC(doctorPigEvent.getPigId());
            parity = 1;
            boarCode = null;
            statusBefore = PigStatus.Entry.getKey();
            statusAfter = null;
            quantity = 1000; //?????????????????????????????????, ??????1000,?????????quantityChange ?????????
            quantityChange = 0; //???????????????????????????????????????
            isWeanToMate = false; //??????????????????????????????,??????????????????+1
            lists.stream()
                    .filter(doctorPigEvent1 -> doctorPigEvent1 != null && doctorPigEvent1.getType() != null)
                    .forEach(doctorPigEvent1 -> {
                        statusAfter = statusBefore;
                        switch (doctorPigEvent1.getType()) {
                            case 6:
                                statusAfter = PigStatus.Removal.getKey();
                                break;
                            case 7:
                                if (!isNull(doctorPigEvent1.getExtraMap()) && !isNull(doctorPigEvent1.getExtraMap().get("parity")) && !Objects.equals("0", doctorPigEvent1.getExtraMap().get("parity").toString())) {
                                    parity = Integer.valueOf(Objects.toString(doctorPigEvent1.getExtraMap().get("parity")));
                                }
                                statusAfter = PigStatus.Entry.getKey();
                                break;
                            case 9:
                                if (isWeanToMate && doctorPigEvent1.getCurrentMatingCount() == 1) {
                                    parity += 1;
                                }
                                boarCode = (isNull(doctorPigEvent1.getExtraMap()) || isNull(doctorPigEvent1.getExtraMap().get("matingBoarPigCode"))) ? null : Objects.toString(doctorPigEvent1.getExtraMap().get("matingBoarPigCode"));
                                statusAfter = PigStatus.Mate.getKey();
                                isWeanToMate = false;
                                break;
                            case 10:
                                if (Arguments.isNull(statusBefore)) {
                                    break;
                                }
                                if (statusBefore.equals(PigStatus.Wean.getKey())) {
                                    statusAfter = PigStatus.Mate.getKey();
                                } else if (statusBefore.equals(PigStatus.KongHuai.getKey())) {
                                    statusAfter = PigStatus.Entry.getKey();
                                } else if (statusBefore.equals(PigStatus.Pregnancy.getKey())) {
                                    statusAfter = PigStatus.Pregnancy.getKey();
                                } else if (statusBefore.equals(PigStatus.Mate.getKey())) {
                                    statusAfter = PigStatus.Mate.getKey();
                                }
                                break;
                            case 11:
                                if (!isNull(doctorPigEvent1.getExtraMap()) && !isNull(doctorPigEvent1.getExtraMap().get("checkResult"))) {
                                    String checkResult = Objects.toString(doctorPigEvent1.getExtraMap().get("checkResult"));
                                    switch (checkResult) {
                                        case "1":
                                            statusAfter = PigStatus.Pregnancy.getKey();
                                            break;
                                        case "2":
                                            statusAfter = PigStatus.KongHuai.getKey();
                                            break;
                                        case "3":
                                            statusAfter = PigStatus.KongHuai.getKey();
                                            break;
                                        case "4":
                                            statusAfter = PigStatus.KongHuai.getKey();
                                            break;
                                    }
                                }
                                break;
                            case 14:
                                statusAfter = PigStatus.Farrow.getKey();
                                break;
                            case 15:
                                statusAfter = PigStatus.FEED.getKey();
                                quantity = doctorPigEvent1.getLiveCount();
                                quantityChange = 0;
                                break;
                            case 16:
                                boarCode = null;
                                quantityChange += MoreObjects.firstNonNull(doctorPigEvent1.getWeanCount(), 0);
                                if (quantity == quantityChange) {
                                    statusAfter = PigStatus.Wean.getKey();
                                }
                                isWeanToMate = true;
                                break;
                            case 17:
                                quantityChange += Integer.parseInt(Objects.toString(doctorPigEvent1.getExtraMap().get("fostersCount")));
                                if (quantity == quantityChange) {
                                    statusAfter = PigStatus.Wean.getKey();
                                }
                                break;
                            case 18:
                                quantityChange += Integer.parseInt(Objects.toString(doctorPigEvent1.getExtraMap().get("pigletsCount")));
                                if (quantity == quantityChange) {
                                    statusAfter = PigStatus.Wean.getKey();
                                }
                                break;
                            case 19:
                                quantity += Objects.isNull(doctorPigEvent1.getExtraMap().get("fostersCount")) ? 0 : Integer.parseInt(Objects.toString(doctorPigEvent1.getExtraMap().get("fostersCount")));
                                break;
                            default:
                                break;
                        }
                        doctorPigEvent1.setPigStatusBefore(statusBefore);
                        doctorPigEvent1.setPigStatusAfter(statusAfter);
                        doctorPigEvent1.setParity(parity);
                        doctorPigEvent1.setBoarCode(boarCode);
                        statusBefore = statusAfter;
                    });
            DoctorPigTrack doctorPigTrack = doctorPigTrackDao.findByPigId(doctorPigEvent.getPigId());
            doctorPigTrack.setCurrentParity(parity);
            doctorPigTrackDao.update(doctorPigTrack);
            Boolean result = doctorPigEventDao.updates(lists);
            if (!result) {
                log.info("update parity boarCode fail: {}", lists);
            }
        });
    }


    public void updateExcelMOVEErrorPigEvents(DoctorFarm farm) {
        List<DoctorPigEvent> doctorPigEvensList = doctorPigEventDao.list(ImmutableMap.of("farmId", farm.getId(), "type", PigEvent.ENTRY.getKey(), "kind", 1, "createdAtEnd", "2017-03-20 23:59:59"));
        if (doctorPigEvensList.isEmpty()) {
            return;
        }
        List<List<DoctorPigEvent>> lists = Lists.partition(doctorPigEvensList, 1000);
        lists.forEach(list -> {
            for (DoctorPigEvent doctorPigEvent : list) {
                Map<String, Object> map = new HashMap<String, Object>();
                if (!isNull(doctorPigEvent.getExtraMap()) && !isNull(doctorPigEvent.getExtraMap().get("importParity"))) {
                    continue;
                }
                if (!isNull(doctorPigEvent.getExtraMap()) && !isNull(doctorPigEvent.getExtraMap().get("parity"))) {
                    map = doctorPigEvent.getExtraMap();
                    map.put("importParity", map.get("parity"));
                    map.replace("parity", Integer.valueOf(map.get("parity").toString()) + 1);
                } else {
                    map.put("parity", 1);
                }

                doctorPigEvent.setExtraMap(map);
            }
            if (!list.isEmpty()) {
                doctorPigEventDao.updates(list);
            }
        });
    }


    public void updateFosterSowCode(DoctorFarm farm) {
        List<DoctorPigEvent> doctorPigEvensList = doctorPigEventDao.list(ImmutableMap.of("farmId", farm.getId(), "type", PigEvent.FOSTERS.getKey(), "kind", 1));
        List<List<DoctorPigEvent>> lists = Lists.partition(doctorPigEvensList, 1000);
        lists.forEach(list -> {
            list.forEach(doctorPigEvent -> {
                Map<String, Object> extra = doctorPigEvent.getExtraMap();
                if (extra.containsKey("fosterSowId") && !extra.containsKey("fosterSowCode")) {
                    DoctorPig doctorPig = doctorPigDao.findById(Long.valueOf(Objects.toString(extra.get("fosterSowId"))));
                    if (!isNull(doctorPig)) {
                        extra.put("fosterSowCode", doctorPig.getPigCode());
                    }
                }
                doctorPigEvent.setExtraMap(extra);
            });
            doctorPigEventDao.updates(list);
        });
    }

    public Response<Boolean> refreshPigStatus() {
        try {
            for (int i = 0; ; i++) {
                Paging<DoctorPigTrack> trackPage = doctorPigTrackDao.paging(i, 1000, ImmutableMap.of("status", PigStatus.Entry.getKey()));
                trackPage.getData().forEach(doctorPigTrack -> {
                    if (!doctorPigEventDao.list(ImmutableMap.of("type", PigEvent.TO_MATING.getKey())).isEmpty()) {
                        doctorPigTrack.setStatus(PigStatus.Wean.getKey());
                        doctorPigTrack.setUpdatedAt(new Date());
                        doctorPigTrackDao.update(doctorPigTrack);
                    }
                });
                if (trackPage.getData().size() < 1000) {
                    break;
                }
            }
            return Response.ok(Boolean.TRUE);
        } catch (Exception e) {
            log.error("refresh.pig.status.failed, cause{}", Throwables.getStackTraceAsString(e));
            return Response.fail("refresh.pig.status.failed");
        }
    }

    /**
     * ??????npd
     */
    @Transactional
    public void flushNpd(Long farmId) {
        log.info("flush npd farmId:{}", farmId);
        Map<Long, List<DoctorPigEvent>> eventMap = doctorPigEventDao.findByFarmIdAndKind(farmId, 1).stream()
                .collect(Collectors.groupingBy(DoctorPigEvent::getPigId));
        eventMap.values().forEach(events -> {
            events = events.stream()
                    .sorted(Comparator.comparing(DoctorPigEvent::getEventAt).thenComparing(DoctorPigEvent::getId))
                    .collect(Collectors.toList());

            //??????????????????????????????
            updateNPD(events);

            //??????????????????????????????????????? ????????????????????? ????????????
            updateFlag(events);

            //???????????? ??? ?????????
            updateDuring(events);

            //??????event
            events.forEach(doctorPigEventDao::update);
        });
        log.info("flush npd ok! farmId:{}", farmId);
    }

    /**
     * ???????????????track???group_id
     */
    @Transactional
    public void flushFarrowSowTrackGroupId(Long farmId) {
        log.info("flush farrrow sow track groupId, farmId:{}", farmId);

        //???????????????groupId = null ???????????????, ?????????????????????
        doctorPigTrackDao.findByFarmIdAndStatus(farmId, PigStatus.FEED.getKey()).stream()
                .filter(track -> track.getGroupId() == null)
                .forEach(track -> {
                    DoctorGroup group = doctorGroupDao.findCurrentFarrowByBarnId(track.getCurrentBarnId());
                    if (group == null) {
                        log.error("farrow group not found, piginfo:{}", track);
                    } else {
                        DoctorPigTrack updateTrack = new DoctorPigTrack();
                        updateTrack.setId(track.getId());
                        updateTrack.setGroupId(group.getId());
                        doctorPigTrackDao.update(updateTrack);
                    }
                });
    }

    /**
     * ??????????????????group_id
     */
    @Transactional
    public void flushFarrowEventGroupId(Long farmId) {
        log.info("flush farrow event groupId, farmId:{}", farmId);

        //??????????????? groupId = null ???????????????, ????????????
        doctorPigEventDao.findByFarmIdAndKindAndEventTypes(farmId,
                DoctorPig.PigSex.SOW.getKey(), Lists.newArrayList(PigEvent.FARROWING.getKey())).stream()
                .filter(event -> event.getGroupId() == null)
                .forEach(event -> {
                    DoctorGroup group = null;
                    DoctorFarrowingDto farrow = MAPPER.fromJson(event.getExtra(), DoctorFarrowingDto.class);
                    if (farrow != null && notEmpty(farrow.getGroupCode())) {
                        group = doctorGroupDao.findByFarmIdAndGroupCode(farmId, farrow.getGroupCode());
                    }
                    if (group == null) {
                        group = doctorGroupDao.findByFarmIdAndBarnIdAndDate(farmId, event.getBarnId(), event.getEventAt());
                    }

                    //??????group_id
                    updateEventGroupId(event, group);
                });
    }

    /**
     * ??????????????????group_id
     */
    @Transactional
    public void flushWeanEventGroupId(Long farmId) {
        log.info("flush wean event groupId, farmId:{}", farmId);

        //??????????????? groupId = null ???????????????, ????????????
        doctorPigEventDao.findByFarmIdAndKindAndEventTypes(farmId,
                DoctorPig.PigSex.SOW.getKey(), Lists.newArrayList(PigEvent.WEAN.getKey())).stream()
                .filter(event -> event.getGroupId() == null)
                .forEach(event -> {
                    DoctorGroup group = doctorGroupDao.findByFarmIdAndBarnIdAndDate(farmId, event.getBarnId(), event.getEventAt());
                    if (group == null) {
                        DoctorPigEvent farrowEvent = doctorPigEventDao.findLastByTypeAndDate(event.getPigId(), event.getEventAt(), PigEvent.FARROWING.getKey());
                        if (farrowEvent != null && farrowEvent.getGroupId() != null) {
                            group = new DoctorGroup();
                            group.setId(farrowEvent.getGroupId());
                        }
                    }

                    //??????group_id
                    updateEventGroupId(event, group);
                });
    }

    private void updateEventGroupId(DoctorPigEvent event, DoctorGroup group) {
        if (group == null || group.getId() == null) {
            log.error("farrow event group not found, eventInfo:{}", event);
        } else {
            DoctorPigEvent updateEvent = new DoctorPigEvent();
            updateEvent.setId(event.getId());
            updateEvent.setGroupId(group.getId());
            doctorPigEventDao.update(updateEvent);
        }
    }

    public void updateParityAndBoarCodeByPigId(Long pigId) {
        List<DoctorPigEvent> doctorPigEvensList = doctorPigEventDao.list(ImmutableMap.of("pigId", pigId, "type", PigEvent.ENTRY.getKey()));
        updateParityAndBoarCodeByEntryEvents(doctorPigEvensList);
    }

    /**
     * ????????????????????????
     */
    @Transactional
    public void generateGroupWeanEvent(Long farmId) {
//        //1.?????????????????????????????????
//        doctorGroupEventDao.deleteAddWeanEvents(farmId);

        //2.???????????????????????????????????????????????????ids
        List<Long> excludeIds = doctorGroupEventDao.queryRelPigEventIdsByGroupWeanEvent(farmId);
        int pageNo = 1;
        int pageSize = 1000;
        while (true) {
            PageInfo pageInfo = PageInfo.of(pageNo, pageSize);
            List<DoctorPigEvent> doctorPigEventList = doctorPigEventDao.queryWeansWithoutGroupWean(excludeIds, farmId, pageInfo.getOffset(), pageInfo.getLimit());
            doctorPigEventList.forEach(pigWeanEvent -> {
                try {
                    //3.????????????????????????
                    DoctorWeanGroupInput groupInput = (DoctorWeanGroupInput) doctorPigEventManager.getHandler(PigEvent.WEAN.getKey()).buildTriggerGroupEventInput(pigWeanEvent);
                    DoctorGroupTrack doctorGroupTrack = doctorGroupTrackDao.findByGroupId(groupInput.getGroupId());
                    DoctorGroup doctorGroup = doctorGroupDao.findById(groupInput.getGroupId());
                    DoctorGroupEvent groupWeanEvent = doctorGroupEventManager.buildGroupEvent(new DoctorGroupInputInfo(new DoctorGroupDetail(doctorGroup, doctorGroupTrack), groupInput), GroupEventType.WEAN.getValue());
                    groupWeanEvent.setEventSource(SourceType.ADD.getValue());
                    doctorGroupEventDao.create(groupWeanEvent);
                } catch (Exception e) {
                    log.error("pigEventId:{}, cause:{}", pigWeanEvent.getId(), Throwables.getStackTraceAsString(e));
                    //throw e;
                }
            });
            pageNo++;
            if (doctorPigEventList.size() < 1000) {
                break;
            }
        }
    }

    /**
     * ???????????????
     *
     * @param userId  ??????id
     * @param newName ?????????
     */
    public void updateUserName(Long userId, String newName) {
        User user = userDao.findById(userId);
        user.setName(newName);
        userWriteService.update(user);
    }

    @Transactional
    public void deleteOrg(Long orgId) {
        List<DoctorOrg> subOrgs = doctorOrgDao.findOrgByParentId(orgId);
        if (!Arguments.isNullOrEmpty(subOrgs)) {
            throw new JsonResponseException("clique.not.allow.delete");
        }

        List<DoctorFarm> farmList = doctorFarmDao.findByOrgId(orgId);
        farmList.forEach(doctorFarm -> deleteFarm(doctorFarm.getId()));
        doctorOrgDao.delete(orgId);
    }

    /**
     * ????????????
     *
     * @param farmId ??????id
     */
    @Transactional
    public void deleteFarm(Long farmId) {
        //1.????????????
        doctorFarmDao.delete(farmId);

        //2.??????????????????????????????????????????
        List<Long> userIdList = Lists.newArrayList();
        userIdList.addAll(subDao.findSubsByFarmId(farmId).stream().map(Sub::getUserId).collect(Collectors.toList()));

        //???????????????????????????
        subDao.deleteByFarmId(farmId);

        PrimaryUser primaryUser = primaryUserDao.findPrimaryByFarmId(farmId);
        if (notNull(primaryUser)) {
            userIdList.add(primaryUser.getUserId());
            primaryUserDao.delete(primaryUser.getId());
        }

        if (!userIdList.isEmpty()) {
            doctorUserDataPermissionDao.deletesByUserIds(userIdList);
        }

        //3.??????statistics
        doctorPigTypeStatisticDao.deleteByFarmId(farmId);
    }

    @Transactional
    public void fixAddPigWean() {
        List<DoctorPigEvent> pigEventList = doctorPigEventDao.queryOldAddWeanEvent();
        pigEventList.forEach(pigEvent -> {
            DoctorWeanDto weanDto = DoctorWeanDto.builder()
                    .partWeanDate(pigEvent.getEventAt())
                    .partWeanPigletsCount(0)
                    .partWeanAvgWeight(0D)
                    .farrowingLiveCount(0)
                    .build();
            weanDto.setBarnId(pigEvent.getBarnId());
            weanDto.setBarnName(pigEvent.getBarnName());
            pigEvent.setExtra(ToJsonMapper.JSON_NON_DEFAULT_MAPPER.toJson(weanDto));
            pigEvent.setDesc(Joiner.on("#").withKeyValueSeparator("???").join(weanDto.descMap()));
            pigEvent.setRelPigEventId(null);
            pigEvent.setWeanCount(0);
            pigEvent.setIsAuto(IsOrNot.NO.getValue());
            pigEvent.setOutId(null);
            pigEvent.setEventSource(SourceType.ADD.getValue());
        });
        doctorPigEventDao.updates(pigEventList);
    }

    /**
     * ???????????????????????????????????????????????????
     */
    @Transactional
    public void fixTriggerPigWean() {
        List<DoctorPigEvent> pigEventList = doctorPigEventDao.queryTriggerWeanEvent();
        pigEventList.forEach(pigEvent -> {
            DoctorWeanDto weanDto = DoctorWeanDto.builder()
                    .partWeanDate(pigEvent.getEventAt())
                    .partWeanPigletsCount(0)
                    .partWeanAvgWeight(0D)
                    .farrowingLiveCount(0)
                    .build();
            weanDto.setBarnId(pigEvent.getBarnId());
            weanDto.setBarnName(pigEvent.getBarnName());
            pigEvent.setExtra(ToJsonMapper.JSON_NON_DEFAULT_MAPPER.toJson(weanDto));
            pigEvent.setDesc(Joiner.on("#").withKeyValueSeparator("???").join(weanDto.descMap()));
            pigEvent.setRelPigEventId(null);
            pigEvent.setIsAuto(IsOrNot.NO.getValue());
            pigEvent.setOutId(null);
            pigEvent.setWeanCount(0);
            pigEvent.setEventSource(SourceType.ADD.getValue());
        });
        doctorPigEventDao.updates(pigEventList);
    }

    /**
     * ?????????????????????????????????
     */
    public void fixMatingCount() {
        List<Long> pigIds = doctorPigDao.findAllPigIds();
        pigIds.parallelStream().forEach(this::updateMatingCount);
    }

    private void updateMatingCount(Long pigId) {
        List<DoctorPigEvent> list = doctorPigEventDao.findEffectMatingCountByPigIdForAsc(pigId);
        DoctorPigTrack pigTrack = doctorPigTrackDao.findByPigId(pigId);
        List<DoctorPigEvent> matingList = Lists.newArrayList();
        int currentMatingCount = 0;
        for (DoctorPigEvent pigEvent : list){
            if (Objects.equals(pigEvent.getType(), PigEvent.MATING.getKey())) {
                pigEvent.setCurrentMatingCount(++currentMatingCount);
                matingList.add(pigEvent);
            } else {
                currentMatingCount = 0;
            }
        }
        DoctorPigTrack updateTrack = new DoctorPigTrack();
        updateTrack.setId(pigTrack.getId());
        updateTrack.setCurrentMatingCount(currentMatingCount);
        doctorPigTrackDao.update(updateTrack);
        if (!matingList.isEmpty()) {
            doctorPigEventDao.updates(matingList);
        }
    }

    /**
     * ????????????????????????????????????????????????eventSource=5
     * @param list ??????id??????
     */
    public void flushChgFarmEventSource(List<Long> list) {
        doctorPigEventDao.flushChgFarmEventSource(list);
    }

    /**
     * ????????????
     */
    public void flushSowParity() {
        List<Long> pigIds = doctorPigDao.findAllPigIds();
        pigIds.forEach(this::flushSowParityImpl);
    }

    private void flushSowParityImpl(Long pigId) {
        int parity = 1;
        boolean weanToMating = false;
        List<DoctorPigEvent> list = doctorPigEventDao.queryAllEventsByPigIdForASC(pigId);
        DoctorPigTrack pigTrack = doctorPigTrackDao.findByPigId(pigId);
        for (DoctorPigEvent pigEvent : list) {
            if (Objects.equals(pigEvent.getType(), PigEvent.ENTRY.getKey())
                    &&!isNull(pigEvent.getExtraMap())
                    && !isNull(pigEvent.getExtraMap().get("parity"))
                    && !Objects.equals("0", pigEvent.getExtraMap().get("parity").toString())) {
                parity = Integer.valueOf(Objects.toString(pigEvent.getExtraMap().get("parity")));
            }

            if (Objects.equals(pigEvent.getType(), PigEvent.WEAN.getKey())
                    || Objects.equals(pigEvent.getType(), PigEvent.FARROWING.getKey())) {
                weanToMating = true;
            }

            if (Objects.equals(pigEvent.getType(), PigEvent.MATING.getKey())
                    && weanToMating) {
                parity++;
                weanToMating = false;
                pigEvent.setPigStatusBefore(PigStatus.Wean.getKey());
                pigEvent.setPigStatusAfter(PigStatus.Mate.getKey());
            }

            pigEvent.setParity(parity);
        }

        doctorPigTrackDao.flushCurrentParity(pigTrack.getPigId(), parity);

        doctorPigEventDao.flushParityAndBeforeStatusAndAfterStatus(list);
    }

    public void flushNestCode() {
        List<DoctorPigEvent> list
                = doctorPigEventDao.findAllFarrowNoNestCode();

        list.forEach(doctorPigEvent -> {
            DateTime eventAt = new DateTime(doctorPigEvent.getEventAt());
            doctorPigEventDao.insertNestCode(doctorPigEvent.getFarmId()
                    , DateUtil.toDateString(eventAt.withDayOfMonth(1).toDate())
                    , DateUtil.toDateString(eventAt.plusMonths(1).minusDays(1).toDate()));
        });
    }

    public void flushPrimaryBarnsPermission() {
        List<PrimaryUser> allPrimary = primaryUserDao.listAll();
        List<Long> userIds = allPrimary.stream().map(PrimaryUser::getUserId).collect(Collectors.toList());
        List<DoctorUserDataPermission> permissions = doctorUserDataPermissionDao.findByUserIds(userIds);
        permissions.add(doctorUserDataPermissionDao.findByUserId(10L));
        permissions.forEach(doctorUserDataPermission -> {
            List<Long> barnIds = doctorBarnDao.findByFarmIds(doctorUserDataPermission.getFarmIdsList())
                    .stream().map(DoctorBarn::getId).collect(Collectors.toList());
            DoctorUserDataPermission updatePermission = new DoctorUserDataPermission();
            updatePermission.setId(doctorUserDataPermission.getId());
            updatePermission.setBarnIds(Joiners.COMMA.join(barnIds));
            doctorUserDataPermissionDao.update(updatePermission);
        });
    }

    public void flushGroupChangeEventAvgDayAge() {
        List<Long> groupIds = doctorGroupEventDao.findAllGroupIdWithChangeNoAvgDayAge();
        if (groupIds.isEmpty()) {
            return;
        }

        List<Integer> includeTypes = Lists.newArrayList(GroupEventType.CHANGE.getValue(), GroupEventType.MOVE_IN.getValue(),
                GroupEventType.TRANS_FARM.getValue(), GroupEventType.TRANS_GROUP.getValue());

        groupIds.parallelStream().forEach(groupId -> {
            try {
                List<DoctorGroupEvent> groupEventList = doctorGroupEventDao.findEventIncludeTypes(groupId, includeTypes);
                int currentQuantity = 0;
                int avgDay = 0;
                Date lastEvent = new Date();
                for (DoctorGroupEvent groupEvent : groupEventList) {
                    if (Objects.equals(MoreObjects.firstNonNull(groupEvent.getQuantity(), 0), 0)) {
                        continue;
                    }
                    if (Objects.equals(groupEvent.getType(), GroupEventType.MOVE_IN.getValue())) {
                        avgDay = avgDay + DateUtil.getDeltaDays(lastEvent, groupEvent.getEventAt());
                        avgDay = EventUtil.getAvgDayAge(avgDay, currentQuantity, groupEvent.getAvgDayAge(), groupEvent.getQuantity());
                        currentQuantity += groupEvent.getQuantity();
                        lastEvent = groupEvent.getEventAt();
                    } else {
                        if (Objects.equals(groupEvent.getType(), GroupEventType.CHANGE.getValue()) && isNull(groupEvent.getAvgDayAge())) {
                            doctorGroupEventDao.updateAvgDayAge(groupEvent.getId(),
                                    avgDay + DateUtil.getDeltaDays(lastEvent, groupEvent.getEventAt()));
                        }
                        currentQuantity -= groupEvent.getQuantity();
                    }
                }
            }catch (Exception e){
                log.error("=======groupId:{}", groupId);
            }
        });

    }

    public void flushChgLocation() {
        List<Long> farmIds = doctorFarmDao.findAll().stream().map(DoctorFarm::getId).collect(Collectors.toList());
        farmIds.parallelStream().forEach(farmId -> {
            DoctorPigEvent updateEvent = new DoctorPigEvent();
            List<DoctorPigEvent> pigEvents = doctorPigEventDao.queryToMatingForTime(farmId);
            pigEvents.forEach(doctorPigEvent -> flushChgLocation(doctorPigEvent, updateEvent));
        });

    }

    private void flushChgLocation(DoctorPigEvent doctorPigEvent, DoctorPigEvent updateEvent) {
        try {
            DoctorChgLocationDto dto = JSON_MAPPER.fromJson(doctorPigEvent.getExtra(), DoctorChgLocationDto.class);
            DoctorBarn doctorBarn = doctorBarnDao.findById(dto.getChgLocationToBarnId());
            if (Objects.equals(doctorBarn.getPigType(), PigType.DELIVER_SOW.getValue())) {
                updateEvent.setId(doctorPigEvent.getId());
                updateEvent.setType(PigEvent.CHG_LOCATION.getKey());
                doctorPigEventDao.update(updateEvent);
                log.info("========flush event type chg location, event id:{}", doctorPigEvent.getId());
            }
        } catch (Exception e) {
            log.error("flush to mating error, event id:{}", doctorPigEvent.getId());
        }
    }

    public void flushFarrowRelMate() {
        int pageNo = 1;
        int pageSize = 1000;
        Map<String, Object> map = new HashMap<>();
        map.put("type", PigEvent.FARROWING.getKey());
        DoctorPigEvent updateEvent = new DoctorPigEvent();
        while (true) {
            PageInfo pageInfo = PageInfo.of(pageNo, pageSize);
            Paging<DoctorPigEvent> paging = doctorPigEventDao.paging(pageInfo.getOffset(), pageInfo.getLimit(), map);
            paging.getData().parallelStream().forEach(doctorPigEvent -> flushFarrowRelMate(doctorPigEvent, updateEvent));

            pageNo++;
            if (paging.getData().size() < 1000) {
                break;
            }
        }
    }

    private void flushFarrowRelMate(DoctorPigEvent farrowEvent, DoctorPigEvent updateEvent) {
        try {
            if (notNull(farrowEvent.getRelEventId())) {
                return;
            }
            DoctorPigEvent firstMate = doctorPigEventDao.queryLastFirstMate(farrowEvent.getPigId(), farrowEvent.getParity());
            updateEvent.setId(farrowEvent.getId());
            updateEvent.setRelEventId(firstMate.getId());
            doctorPigEventDao.update(updateEvent);
        } catch (Exception e) {
            log.error("flush farrow rel mate, event id:{}", farrowEvent.getId());
        }
    }
}