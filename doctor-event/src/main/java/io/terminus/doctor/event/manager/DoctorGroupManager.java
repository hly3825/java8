package io.terminus.doctor.event.manager;

import com.google.common.collect.Lists;
import io.terminus.common.exception.ServiceException;
import io.terminus.common.utils.BeanMapper;
import io.terminus.common.utils.Dates;
import io.terminus.doctor.common.enums.PigType;
import io.terminus.doctor.common.enums.SourceType;
import io.terminus.doctor.common.event.CoreEventDispatcher;
import io.terminus.doctor.common.exception.InvalidException;
import io.terminus.doctor.common.utils.DateUtil;
import io.terminus.doctor.common.utils.JsonMapperUtil;
import io.terminus.doctor.common.utils.RespHelper;
import io.terminus.doctor.common.utils.ToJsonMapper;
import io.terminus.doctor.event.dao.DoctorBarnDao;
import io.terminus.doctor.event.dao.DoctorDailyGroupDao;
import io.terminus.doctor.event.dao.DoctorGroupDao;
import io.terminus.doctor.event.dao.DoctorGroupEventDao;
import io.terminus.doctor.event.dao.DoctorGroupTrackDao;
import io.terminus.doctor.event.dao.DoctorKpiDao;
import io.terminus.doctor.event.dao.DoctorTrackSnapshotDao;
import io.terminus.doctor.event.dto.event.DoctorEventInfo;
import io.terminus.doctor.event.dto.event.group.input.DoctorNewGroupInput;
import io.terminus.doctor.event.dto.event.group.input.DoctorNewGroupInputInfo;
import io.terminus.doctor.event.enums.EventStatus;
import io.terminus.doctor.event.enums.GroupEventType;
import io.terminus.doctor.event.event.DoctorGroupPublishDto;
import io.terminus.doctor.event.event.ListenedGroupEvent;
import io.terminus.doctor.event.helper.DoctorConcurrentControl;
import io.terminus.doctor.event.model.DoctorBarn;
import io.terminus.doctor.event.model.DoctorDailyGroup;
import io.terminus.doctor.event.model.DoctorEventModifyRequest;
import io.terminus.doctor.event.model.DoctorGroup;
import io.terminus.doctor.event.model.DoctorGroupEvent;
import io.terminus.doctor.event.model.DoctorGroupTrack;
import io.terminus.doctor.event.model.DoctorTrackSnapshot;
import io.terminus.doctor.event.service.DoctorGroupReadService;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static io.terminus.common.utils.Arguments.*;
import static io.terminus.doctor.common.utils.Checks.expectTrue;

/**
 * Desc:
 * Mail: yangzl@terminus.io
 * author: DreamYoung
 * Date: 16/5/25
 */
@Slf4j
@Component
public class DoctorGroupManager {

    private static final JsonMapperUtil JSON_MAPPER = JsonMapperUtil.nonEmptyMapper();

    private final DoctorGroupDao doctorGroupDao;
    private final DoctorGroupEventDao doctorGroupEventDao;
    private final DoctorGroupTrackDao doctorGroupTrackDao;
    private final DoctorGroupReadService doctorGroupReadService;
    private final CoreEventDispatcher coreEventDispatcher;
    private final DoctorBarnDao doctorBarnDao;
    private final DoctorKpiDao doctorKpiDao;
    private final DoctorDailyGroupDao doctorDailyGroupDao;
    private final DoctorConcurrentControl doctorConcurrentControl;

    private final DoctorTrackSnapshotDao doctorTrackSnapshotDao;

    private static final ToJsonMapper TO_JSON_MAPPER = ToJsonMapper.JSON_NON_EMPTY_MAPPER;

    @Autowired
    public DoctorGroupManager(DoctorGroupDao doctorGroupDao,
                              DoctorGroupEventDao doctorGroupEventDao,
                              DoctorGroupTrackDao doctorGroupTrackDao,
                              DoctorGroupReadService doctorGroupReadService,
                              CoreEventDispatcher coreEventDispatcher,
                              DoctorBarnDao doctorBarnDao, DoctorKpiDao doctorKpiDao, DoctorDailyGroupDao doctorDailyGroupDao, DoctorConcurrentControl doctorConcurrentControl, DoctorTrackSnapshotDao doctorTrackSnapshotDao) {
        this.doctorGroupDao = doctorGroupDao;
        this.doctorGroupEventDao = doctorGroupEventDao;
        this.doctorGroupTrackDao = doctorGroupTrackDao;
        this.doctorGroupReadService = doctorGroupReadService;
        this.coreEventDispatcher = coreEventDispatcher;
        this.doctorBarnDao = doctorBarnDao;
        this.doctorKpiDao = doctorKpiDao;
        this.doctorDailyGroupDao = doctorDailyGroupDao;
        this.doctorConcurrentControl = doctorConcurrentControl;
        this.doctorTrackSnapshotDao = doctorTrackSnapshotDao;
    }

    /**
     * ????????????
     *
     * @param group         ??????
     * @param newGroupInput ????????????????????????
     * @return ??????id
     */
    @Transactional
    public Long createNewGroup(List<DoctorEventInfo> eventInfoList, DoctorGroup group, DoctorNewGroupInput newGroupInput) {

        if (isNull(newGroupInput.getEventSource()) || Objects.equals(newGroupInput.getEventSource(), SourceType.INPUT.getValue())) {
            String key = group.getFarmId().toString() + group.getGroupCode();
            expectTrue(doctorConcurrentControl.setKey(key),
                    "event.concurrent.error", group.getGroupCode());
        }

        newGroupInput.setEventType(GroupEventType.NEW.getValue());
        checkFarrowGroupUnique(newGroupInput.getPigType(), newGroupInput.getBarnId(), group.getGroupCode());

        //0.???????????????????????????
        checkGroupCodeExist(newGroupInput.getFarmId(), newGroupInput.getGroupCode());

        //1. ????????????
        doctorGroupDao.create(getNewGroup(group, newGroupInput));
        Long groupId = group.getId();

        //2. ????????????????????????
        DoctorGroupEvent groupEvent = getNewGroupEvent(group, newGroupInput);
        groupEvent.setSowId(newGroupInput.getSowId());
        groupEvent.setSowCode(newGroupInput.getSowCode());
        doctorGroupEventDao.create(groupEvent);

        //3. ??????????????????
        DoctorGroupTrack groupTrack = BeanMapper.map(groupEvent, DoctorGroupTrack.class);
        groupTrack.setGroupId(groupId);
        groupTrack.setRelEventId(groupEvent.getId());
        groupTrack.setBoarQty(0);
        groupTrack.setSowQty(0);
        groupTrack.setQuantity(0);
        groupTrack.setBirthDate(DateUtil.toDate(newGroupInput.getEventAt()));    //????????????(??????????????????)

        int age = DateUtil.getDeltaDaysAbs(groupTrack.getBirthDate(), new Date());
        groupTrack.setAvgDayAge(age + 1);             //??????

        groupTrack.setSex(newGroupInput.getSex());
        groupTrack.setWeanWeight(0D);
        groupTrack.setBirthWeight(0D);
        groupTrack.setNest(0);
        groupTrack.setLiveQty(0);
        groupTrack.setHealthyQty(0);
        groupTrack.setWeakQty(0);
        groupTrack.setWeanQty(0);
        groupTrack.setWeanWeight(0D);
        groupTrack.setUnweanQty(0);
        groupTrack.setQuaQty(0);
        groupTrack.setUnqQty(0);
        doctorGroupTrackDao.create(groupTrack);

        DoctorEventInfo eventInfo = DoctorEventInfo.builder()
                .orgId(group.getOrgId())
                .farmId(group.getFarmId())
                .eventId(groupEvent.getId())
                .eventType(groupEvent.getType())
                .businessId(group.getId())
                .businessType(DoctorEventInfo.Business_Type.GROUP.getValue())
                .code(group.getGroupCode())
                .build();
        eventInfoList.add(eventInfo);

        createTrackSnapshot(groupEvent, groupTrack);

        autoDailyGroup(groupEvent.getFarmId(), groupEvent.getGroupId(), group.getPigType(), groupEvent.getEventAt());
        return groupId;
    }

    /**
     * ?????????????????????track snapshot
     * @param newEvent ????????????
     */
    private void createTrackSnapshot(DoctorGroupEvent newEvent, DoctorGroupTrack currentTrack) {
        DoctorTrackSnapshot snapshot = DoctorTrackSnapshot.builder()
                .farmId(newEvent.getFarmId())
                .farmName(newEvent.getFarmName())
                .businessId(newEvent.getGroupId())
                .businessCode(newEvent.getGroupCode())
                .businessType(DoctorEventModifyRequest.TYPE.GROUP.getValue())
                .eventId(newEvent.getId())
                .eventSource(DoctorTrackSnapshot.EventSource.EVENT.getValue())
                .trackJson(TO_JSON_MAPPER.toJson(currentTrack))
                .build();
        doctorTrackSnapshotDao.create(snapshot);
    }
    /**
     * ??????????????????
     *
     * @param inputInfoList ??????????????????
     * @return
     */
    @Transactional
    public List<DoctorEventInfo> batchNewGroupEventHandle(List<DoctorNewGroupInputInfo> inputInfoList) {
        log.info("batch new group event handle starting");
        List<DoctorEventInfo> eventInfoList = Lists.newArrayList();
        inputInfoList.forEach(newGroupInputInfo -> {
            try {
                createNewGroup(eventInfoList, newGroupInputInfo.getGroup(), newGroupInputInfo.getNewGroupInput());
            } catch (InvalidException e) {
                throw new InvalidException(true, e.getError(), newGroupInputInfo.getGroup().getGroupCode(), e.getParams());
            }
        });
        log.info("batch new group event handle ending");
        return eventInfoList;
    }

    @Transactional
    public void updateGroup(DoctorGroup group, DoctorGroupTrack groupTrack) {
        doctorGroupDao.update(group);
        doctorGroupTrackDao.update(groupTrack);
    }

    //???????????????1?????????
    private void checkFarrowGroupUnique(Integer pigType, Long barnId, String groupCode) {
        if (!Objects.equals(pigType, PigType.DELIVER_SOW.getValue())) {
            return;
        }

        List<DoctorGroup> groups = RespHelper.orServEx(doctorGroupReadService.findGroupByCurrentBarnId(barnId));
        DoctorBarn doctorBarn = doctorBarnDao.findById(barnId);
        if (notEmpty(groups)) {
            throw new InvalidException("farrow.group.exist", doctorBarn.getName(), groupCode);
        }
    }

    private DoctorGroup getNewGroup(DoctorGroup group, DoctorNewGroupInput newGroupInput) {
        //????????????
        group.setInitBarnId(newGroupInput.getBarnId());
        group.setInitBarnName(newGroupInput.getBarnName());
        group.setCurrentBarnId(newGroupInput.getBarnId());
        group.setCurrentBarnName(newGroupInput.getBarnName());

        DoctorBarn barn = doctorBarnDao.findById(group.getInitBarnId());
        if (barn == null) {
            throw new ServiceException("barn.not.null");
        }
        group.setPigType(barn.getPigType());
        group.setStaffId(barn.getStaffId());
        group.setStaffName(barn.getStaffName());

        //?????????????????????
        group.setOpenAt(generateEventAt(DateUtil.toDate(newGroupInput.getEventAt())));
        group.setStatus(DoctorGroup.Status.CREATED.getValue());

        group.setOutId(newGroupInput.getGroupOutId());
        return group;
    }

    //????????????????????????
    private DoctorGroupEvent<DoctorNewGroupInput> getNewGroupEvent(DoctorGroup group, DoctorNewGroupInput newGroupInput) {
        DoctorGroupEvent<DoctorNewGroupInput> groupEvent = new DoctorGroupEvent<>();

        groupEvent.setGroupId(group.getId());   //????????????id
        groupEvent.setRelGroupEventId(newGroupInput.getRelGroupEventId()); //??????????????????id
        groupEvent.setRelPigEventId(newGroupInput.getRelPigEventId());     //???????????????id(??????????????????????????????)

        groupEvent.setOrgId(group.getOrgId());
        groupEvent.setOrgName(group.getOrgName());
        groupEvent.setFarmId(group.getFarmId());
        groupEvent.setFarmName(group.getFarmName());
        groupEvent.setGroupCode(group.getGroupCode());

        //????????????
        groupEvent.setEventAt(group.getOpenAt());
        groupEvent.setType(GroupEventType.NEW.getValue());
        groupEvent.setName(GroupEventType.NEW.getDesc());
        groupEvent.setDesc(newGroupInput.generateEventDesc());

        groupEvent.setBarnId(group.getInitBarnId());
        groupEvent.setBarnName(group.getInitBarnName());
        groupEvent.setPigType(group.getPigType());

        groupEvent.setIsAuto(newGroupInput.getIsAuto());
        groupEvent.setCreatorId(group.getCreatorId());
        groupEvent.setCreatorName(group.getCreatorName());
        groupEvent.setRemark(group.getRemark());

        newGroupInput.setSource(newGroupInput.getSource());
        groupEvent.setExtraMap(newGroupInput);
        groupEvent.setStatus(EventStatus.VALID.getValue());
        groupEvent.setEventSource(notNull(newGroupInput.getEventSource()) ? newGroupInput.getEventSource()
                : SourceType.INPUT.getValue());
        return groupEvent;
    }

    //???????????????????????????
    private void checkGroupCodeExist(Long farmId, String groupCode) {
        List<DoctorGroup> groups = RespHelper.or500(doctorGroupReadService.findGroupsByFarmId(farmId));
        if (groups.stream().map(DoctorGroup::getGroupCode).collect(Collectors.toList()).contains(groupCode)) {
            throw new InvalidException("group.code.exist");
        }
    }

    //??????????????????
    private void publistGroupAndBarn(DoctorGroupEvent event) {
        DoctorGroupPublishDto publish = new DoctorGroupPublishDto();
        publish.setEventAt(event.getEventAt());
        publish.setEventId(event.getId());
        publish.setGroupId(event.getGroupId());
        publish.setPigType(event.getPigType());
        coreEventDispatcher.publish(ListenedGroupEvent.builder()
                .farmId(event.getFarmId())
                .orgId(event.getOrgId())
                .eventType(event.getType())
                .groups(Lists.newArrayList(publish))
                .build());
    }

    protected Date generateEventAt(Date eventAt) {
        if (eventAt != null) {
            Date now = new Date();
            if (DateUtil.inSameDate(eventAt, now)) {
                // ??????????????????, ?????????????????????
                return now;
            } else {
                // ??????????????????, ??????????????????0, ???????????????
                return Dates.startOfDay(eventAt);
            }
        }
        return null;
    }

    /**
     * ????????????dailyGroup
     *
     * @param farmId  ??????id
     * @param eventAt ????????????
     */
    protected void autoDailyGroup(Long farmId, Long groupId, Integer pigType, Date eventAt) {
        while (!eventAt.after(new Date())) {
            DoctorDailyGroup doctorDailyGroup = new DoctorDailyGroup();
            doctorDailyGroup.setStart(0);
            doctorDailyGroup.setEnd(0);
            doctorDailyGroup.setSumAt(eventAt);
            doctorDailyGroup.setGroupId(groupId);
            doctorDailyGroup.setFarmId(farmId);
            doctorDailyGroup.setType(pigType);
            doctorDailyGroupDao.create(doctorDailyGroup);
            eventAt = new DateTime(eventAt).plusDays(1).toDate();
        }
    }
}
