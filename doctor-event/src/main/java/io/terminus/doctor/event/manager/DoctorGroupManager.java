package io.terminus.doctor.event.manager;

import com.google.common.collect.Lists;
import io.terminus.common.exception.ServiceException;
import io.terminus.common.utils.BeanMapper;
import io.terminus.common.utils.Dates;
import io.terminus.common.utils.JsonMapper;
import io.terminus.doctor.common.exception.InvalidException;
import io.terminus.doctor.common.enums.PigType;
import io.terminus.doctor.common.event.CoreEventDispatcher;
import io.terminus.doctor.common.utils.DateUtil;
import io.terminus.doctor.common.utils.JsonMapperUtil;
import io.terminus.doctor.common.utils.RespHelper;
import io.terminus.doctor.event.dao.*;
import io.terminus.doctor.event.dto.DoctorGroupSnapShotInfo;
import io.terminus.doctor.event.dto.event.DoctorEventInfo;
import io.terminus.doctor.event.dto.event.group.DoctorNewGroupEvent;
import io.terminus.doctor.event.dto.event.group.input.DoctorNewGroupInput;
import io.terminus.doctor.event.dto.event.group.input.DoctorNewGroupInputInfo;
import io.terminus.doctor.event.enums.EventStatus;
import io.terminus.doctor.event.enums.GroupEventType;
import io.terminus.doctor.event.event.DoctorGroupPublishDto;
import io.terminus.doctor.event.event.ListenedGroupEvent;
import io.terminus.doctor.event.model.*;
import io.terminus.doctor.event.service.DoctorGroupReadService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static io.terminus.common.utils.Arguments.notEmpty;

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
    private final DoctorGroupSnapshotDao doctorGroupSnapshotDao;
    private final DoctorGroupTrackDao doctorGroupTrackDao;
    private final DoctorGroupReadService doctorGroupReadService;
    private final CoreEventDispatcher coreEventDispatcher;
    private final DoctorBarnDao doctorBarnDao;

    @Autowired
    public DoctorGroupManager(DoctorGroupDao doctorGroupDao,
                              DoctorGroupEventDao doctorGroupEventDao,
                              DoctorGroupSnapshotDao doctorGroupSnapshotDao,
                              DoctorGroupTrackDao doctorGroupTrackDao,
                              DoctorGroupReadService doctorGroupReadService,
                              CoreEventDispatcher coreEventDispatcher,
                              DoctorBarnDao doctorBarnDao) {
        this.doctorGroupDao = doctorGroupDao;
        this.doctorGroupEventDao = doctorGroupEventDao;
        this.doctorGroupSnapshotDao = doctorGroupSnapshotDao;
        this.doctorGroupTrackDao = doctorGroupTrackDao;
        this.doctorGroupReadService = doctorGroupReadService;
        this.coreEventDispatcher = coreEventDispatcher;
        this.doctorBarnDao = doctorBarnDao;
    }

    /**
     * 新建猪群
     * @param group 猪群
     * @param newGroupInput 新建猪群录入信息
     * @return 猪群id
     */
    @Transactional
    public Long createNewGroup(List<DoctorEventInfo> eventInfoList, DoctorGroup group, DoctorNewGroupInput newGroupInput) {
        newGroupInput.setEventType(GroupEventType.NEW.getValue());
        checkFarrowGroupUnique(newGroupInput.getPigType(), newGroupInput.getBarnId(), group.getGroupCode());

        //0.校验猪群号是否重复
        checkGroupCodeExist(newGroupInput.getFarmId(), newGroupInput.getGroupCode());

        //1. 创建猪群
        doctorGroupDao.create(getNewGroup(group, newGroupInput));
        Long groupId = group.getId();

        //2. 创建新建猪群事件
        DoctorGroupEvent<DoctorNewGroupEvent> groupEvent = getNewGroupEvent(group, newGroupInput);
        doctorGroupEventDao.create(groupEvent);

        //3. 创建猪群跟踪
        DoctorGroupTrack groupTrack = BeanMapper.map(groupEvent, DoctorGroupTrack.class);
        groupTrack.setGroupId(groupId);
        groupTrack.setRelEventId(groupEvent.getId());
        groupTrack.setBoarQty(0);
        groupTrack.setSowQty(0);
        groupTrack.setQuantity(0);
        groupTrack.setBirthDate(DateUtil.toDate(newGroupInput.getEventAt()));    //出生日期(用于计算日龄)

        int age = DateUtil.getDeltaDaysAbs(groupTrack.getBirthDate(), new Date());
        groupTrack.setAvgDayAge(age + 1);             //日龄

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

        //4. 创建猪群镜像
        DoctorGroupSnapshot groupSnapshot = new DoctorGroupSnapshot();
        groupSnapshot.setGroupId(groupEvent.getGroupId());
        groupSnapshot.setFromEventId(0L);
        groupSnapshot.setToEventId(groupEvent.getId());
        groupSnapshot.setToInfo(JSON_MAPPER.toJson(DoctorGroupSnapShotInfo.builder()
                .group(group)
                .groupEvent(groupEvent)
                .groupTrack(groupTrack)
                .build()));
        doctorGroupSnapshotDao.create(groupSnapshot);

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
        //发布统计事件
        //publistGroupAndBarn(groupEvent);
        return groupId;
    }

    /**
     * 批量新建猪群
     * @param inputInfoList 批量事件信息
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

    //产房只能有1个猪群
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
        //设置猪舍
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

        //建群时间与状态
        group.setOpenAt(generateEventAt(DateUtil.toDate(newGroupInput.getEventAt())));
        group.setStatus(DoctorGroup.Status.CREATED.getValue());
        return group;
    }

    //构造新建猪群事件
    private DoctorGroupEvent<DoctorNewGroupEvent> getNewGroupEvent(DoctorGroup group, DoctorNewGroupInput newGroupInput) {
        DoctorGroupEvent<DoctorNewGroupEvent> groupEvent = new DoctorGroupEvent<>();

        groupEvent.setGroupId(group.getId());   //关联猪群id
        groupEvent.setRelGroupEventId(newGroupInput.getRelGroupEventId()); //关联猪群事件id
        groupEvent.setRelPigEventId(newGroupInput.getRelPigEventId());     //关联猪事件id(比如分娩时的新建猪群)

        groupEvent.setOrgId(group.getOrgId());
        groupEvent.setOrgName(group.getOrgName());
        groupEvent.setFarmId(group.getFarmId());
        groupEvent.setFarmName(group.getFarmName());
        groupEvent.setGroupCode(group.getGroupCode());

        //事件信息
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

        DoctorNewGroupEvent newGroupEvent = new DoctorNewGroupEvent();
        newGroupEvent.setSource(newGroupInput.getSource());
        groupEvent.setExtraMap(newGroupEvent);
        groupEvent.setStatus(EventStatus.VALID.getValue());
        return groupEvent;
    }

    //校验猪群号是否重复
    private void checkGroupCodeExist(Long farmId, String groupCode) {
        List<DoctorGroup> groups = RespHelper.or500(doctorGroupReadService.findGroupsByFarmId(farmId));
        if (groups.stream().map(DoctorGroup::getGroupCode).collect(Collectors.toList()).contains(groupCode)) {
            throw new InvalidException("group.code.exist");
        }
    }

    //发布猪群事件
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

    protected Date generateEventAt(Date eventAt){
        if(eventAt != null){
            Date now = new Date();
            if(DateUtil.inSameDate(eventAt, now)){
                // 如果处在今天, 则使用此刻瞬间
                return now;
            } else {
                // 如果不在今天, 则将时间置为0, 只保留日期
                return Dates.startOfDay(eventAt);
            }
        }
        return null;
    }
}
