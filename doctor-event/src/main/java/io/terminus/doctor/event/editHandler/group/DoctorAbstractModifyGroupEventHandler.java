package io.terminus.doctor.event.editHandler.group;

import com.google.common.collect.Lists;
import io.terminus.common.utils.BeanMapper;
import io.terminus.common.utils.Dates;
import io.terminus.doctor.common.enums.PigType;
import io.terminus.doctor.common.enums.SourceType;
import io.terminus.doctor.common.exception.InvalidException;
import io.terminus.doctor.common.utils.DateUtil;
import io.terminus.doctor.common.utils.JsonMapperUtil;
import io.terminus.doctor.common.utils.ToJsonMapper;
import io.terminus.doctor.event.dao.DoctorBarnDao;
import io.terminus.doctor.event.dao.DoctorDailyGroupDao;
import io.terminus.doctor.event.dao.DoctorEventModifyLogDao;
import io.terminus.doctor.event.dao.DoctorGroupBatchSummaryDao;
import io.terminus.doctor.event.dao.DoctorGroupDailyDao;
import io.terminus.doctor.event.dao.DoctorGroupDao;
import io.terminus.doctor.event.dao.DoctorGroupEventDao;
import io.terminus.doctor.event.dao.DoctorGroupTrackDao;
import io.terminus.doctor.event.dao.DoctorTrackSnapshotDao;
import io.terminus.doctor.event.dto.event.edit.DoctorEventChangeDto;
import io.terminus.doctor.event.dto.event.group.input.BaseGroupInput;
import io.terminus.doctor.event.dto.event.group.input.DoctorCloseGroupInput;
import io.terminus.doctor.event.editHandler.DoctorModifyGroupEventHandler;
import io.terminus.doctor.event.enums.EventStatus;
import io.terminus.doctor.event.enums.GroupEventType;
import io.terminus.doctor.event.enums.IsOrNot;
import io.terminus.doctor.event.helper.DoctorConcurrentControl;
import io.terminus.doctor.event.helper.DoctorEventBaseHelper;
import io.terminus.doctor.event.manager.DoctorDailyReportManager;
import io.terminus.doctor.event.manager.DoctorDailyReportV2Manager;
import io.terminus.doctor.event.model.DoctorBarn;
import io.terminus.doctor.event.model.DoctorDailyGroup;
import io.terminus.doctor.event.model.DoctorEventModifyLog;
import io.terminus.doctor.event.model.DoctorEventModifyRequest;
import io.terminus.doctor.event.model.DoctorGroup;
import io.terminus.doctor.event.model.DoctorGroupDaily;
import io.terminus.doctor.event.model.DoctorGroupEvent;
import io.terminus.doctor.event.model.DoctorGroupTrack;
import io.terminus.doctor.event.model.DoctorTrackSnapshot;
import io.terminus.doctor.event.util.EventUtil;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Date;
import java.util.List;
import java.util.Objects;

import static io.terminus.common.utils.Arguments.isNull;
import static io.terminus.common.utils.Arguments.notNull;
import static io.terminus.doctor.common.enums.SourceType.UN_MODIFY;
import static io.terminus.doctor.common.utils.Checks.expectNotNull;
import static io.terminus.doctor.common.utils.Checks.expectTrue;

/**
 * Created by xjn on 17/4/13.
 * ????????????????????????
 */
@Slf4j
public abstract class DoctorAbstractModifyGroupEventHandler implements DoctorModifyGroupEventHandler {
    @Autowired
    protected DoctorGroupDao doctorGroupDao;
    @Autowired
    protected DoctorGroupEventDao doctorGroupEventDao;
    @Autowired
    protected DoctorGroupTrackDao doctorGroupTrackDao;
    @Autowired
    protected DoctorGroupDailyDao doctorGroupDailyDao;
    @Autowired
    private DoctorEventModifyLogDao doctorEventModifyLogDao;
    @Autowired
    protected DoctorDailyReportV2Manager doctorDailyReportManager;
    @Autowired
    protected DoctorBarnDao doctorBarnDao;
    @Autowired
    protected DoctorDailyGroupDao oldDailyGroupDao;
    @Autowired
    protected DoctorDailyReportManager oldDailyReportManager;

    @Autowired
    protected DoctorConcurrentControl doctorConcurrentControl;

    @Autowired
    protected DoctorGroupBatchSummaryDao doctorGroupBatchSummaryDao;

    @Autowired
    protected DoctorTrackSnapshotDao doctorTrackSnapshotDao;

    @Autowired
    protected DoctorEventBaseHelper doctorEventBaseHelper;

    protected final JsonMapperUtil JSON_MAPPER = JsonMapperUtil.JSON_NON_DEFAULT_MAPPER;

    protected final ToJsonMapper TO_JSON_MAPPER = ToJsonMapper.JSON_NON_DEFAULT_MAPPER;

    @Override
    public final Boolean canModify(DoctorGroupEvent oldGroupEvent) {
        return Objects.equals(oldGroupEvent.getIsAuto(), IsOrNot.NO.getValue())
                && !UN_MODIFY.contains(oldGroupEvent.getEventSource());

    }

    @Override
    public void modifyHandle(DoctorGroupEvent oldGroupEvent, BaseGroupInput input) {
        log.info("modify group event handler starting, oldGroupEvent:{}", oldGroupEvent);
        log.info("input:{}", input);
        String key = "group" + oldGroupEvent.getGroupId().toString();
        expectTrue(doctorConcurrentControl.setKey(key), "event.concurrent.error", oldGroupEvent.getGroupCode());

        //1.??????
        modifyHandleCheck(oldGroupEvent, input);

        //2.??????????????????
        DoctorEventChangeDto changeDto = buildEventChange(oldGroupEvent, input);

        //3.????????????
        DoctorGroupEvent newEvent = buildNewEvent(oldGroupEvent, input);
        doctorGroupEventDao.updateIncludeNull(newEvent);

        //4.???????????????????????????????????????
        Long modifyLogId = createModifyLog(oldGroupEvent, newEvent);

        //5.????????????
        if (isUpdateGroup(changeDto)) {
            DoctorGroup oldGroup = doctorGroupDao.findById(oldGroupEvent.getGroupId());
            DoctorGroup newGroup = buildNewGroup(oldGroup, input);
            doctorGroupDao.update(newGroup);
        }

        //6.??????track
        if (isUpdateTrack(changeDto)) {
            DoctorGroupTrack oldTrack = doctorGroupTrackDao.findByGroupId(oldGroupEvent.getGroupId());
            DoctorGroupTrack newTrack = buildNewTrack(oldTrack, changeDto);

            //??????track
            doctorEventBaseHelper.validTrackAfterUpdate(newTrack);

            //??????track
            doctorGroupTrackDao.update(newTrack);

            //???????????????????????????
            autoCloseOrOpen(newTrack);

            //??????????????????
            createTrackSnapshotFroModify(newEvent, modifyLogId);
        }

        //7.????????????????????????
        updateDailyForModify(oldGroupEvent, input, changeDto);

        //8.???????????????????????????
        triggerEventModifyHandle(newEvent);

        log.info("modify group event handler ending");
    }

    @Override
    public Boolean canRollback(DoctorGroupEvent deleteGroupEvent) {
        return Objects.equals(deleteGroupEvent.getIsAuto(), IsOrNot.NO.getValue())
                && rollbackHandleCheck(deleteGroupEvent)
                && !UN_MODIFY.contains(deleteGroupEvent.getEventSource());
    }

    @Override
    public void rollbackHandle(DoctorGroupEvent deleteGroupEvent, Long operatorId, String operatorName) {

        log.info("rollback handle starting, deleteGroupEvent:{}", deleteGroupEvent);
        String key = "group" + deleteGroupEvent.getGroupId().toString();
        expectTrue(doctorConcurrentControl.setKey(key), "event.concurrent.error", deleteGroupEvent.getGroupCode());

        //2.??????????????????
        triggerEventRollbackHandle(deleteGroupEvent, operatorId, operatorName);

        //3.????????????
        doctorGroupEventDao.delete(deleteGroupEvent.getId());

        //4.????????????
        Long modifyLogId = createModifyLog(deleteGroupEvent);

        //5.?????????
        if (isUpdateGroup(deleteGroupEvent.getType())) {
            DoctorGroup oldGroup = doctorGroupDao.findById(deleteGroupEvent.getGroupId());
            if (Objects.equals(deleteGroupEvent.getType(), GroupEventType.NEW.getValue())) {
                doctorGroupDao.delete(oldGroup.getId());
            } else {
                DoctorGroup newGroup = buildNewGroupForRollback(deleteGroupEvent, oldGroup);
                doctorGroupDao.update(newGroup);
            }
        }

        //6.??????track
        if (isUpdateTrack(deleteGroupEvent.getType())) {
            DoctorGroupTrack oldTrack = doctorGroupTrackDao.findByGroupId(deleteGroupEvent.getGroupId());
            if (Objects.equals(deleteGroupEvent.getType(), GroupEventType.NEW.getValue())) {
                doctorGroupTrackDao.delete(oldTrack.getId());
            } else {
                DoctorGroupTrack newTrack = buildNewTrackForRollback(deleteGroupEvent, oldTrack);

                //??????track
                doctorEventBaseHelper.validTrackAfterUpdate(newTrack);

                //??????track
                doctorGroupTrackDao.update(newTrack);

                //???????????????????????????
                autoCloseOrOpen(newTrack);

                //??????????????????
                createTrackSnapshotFroDelete(deleteGroupEvent, modifyLogId);
            }
        }

        //7.????????????
        updateDailyForDelete(deleteGroupEvent);

        log.info("rollback handle ending");
    }

    /**
     * ???????????????????????????
     *
     * @param oldGroupEvent ?????????
     * @param input         ??????
     */
    protected void modifyHandleCheck(DoctorGroupEvent oldGroupEvent, BaseGroupInput input) {
        if (!Objects.equals(oldGroupEvent.getType(), GroupEventType.NEW.getValue())) {
            DoctorGroupEvent newCreateEvent = doctorGroupEventDao.findNewGroupByGroupId(oldGroupEvent.getGroupId());
            DoctorGroupEvent closeEvent = doctorGroupEventDao.findCloseGroupByGroupId(oldGroupEvent.getGroupId());
            validEventAt(DateUtil.toDate(input.getEventAt()), notNull(newCreateEvent) ? newCreateEvent.getEventAt() : null
                    , notNull(closeEvent) ? closeEvent.getEventAt() : null);
        }
    }

    @Override
    public DoctorEventChangeDto buildEventChange(DoctorGroupEvent oldGroupEvent, BaseGroupInput input) {
        return null;
    }

    @Override
    public DoctorGroupEvent buildNewEvent(DoctorGroupEvent oldGroupEvent, BaseGroupInput input) {
        DoctorGroupEvent newEvent = new DoctorGroupEvent();
        BeanMapper.copy(oldGroupEvent, newEvent);
        newEvent.setDesc(input.generateEventDesc());
        newEvent.setExtra(TO_JSON_MAPPER.toJson(input));
        newEvent.setEventAt(DateUtil.toDate(input.getEventAt()));
        newEvent.setRemark(input.getRemark());
        return newEvent;
    }

    @Override
    public DoctorGroup buildNewGroup(DoctorGroup oldGroup, BaseGroupInput input) {
        return oldGroup;
    }

    @Override
    public DoctorGroupTrack buildNewTrack(DoctorGroupTrack oldGroupTrack, DoctorEventChangeDto changeDto) {
        return oldGroupTrack;
    }

    /**
     * ??????????????????
     *
     * @param oldGroupEvent ?????????
     * @param input         ?????????
     * @param changeDto     ??????
     */
    protected void updateDailyForModify(DoctorGroupEvent oldGroupEvent, BaseGroupInput input, DoctorEventChangeDto changeDto) {
    }

    ;

    /**
     * ???????????????????????????(??????)
     *
     * @param newEvent ???????????????
     */
    protected void triggerEventModifyHandle(DoctorGroupEvent newEvent) {
    }

    /**
     * ?????????????????????????????????
     *
     * @param groupTrack ??????track
     */
    protected void autoCloseOrOpen(DoctorGroupTrack groupTrack) {
        DoctorGroupEvent closeEvent = doctorGroupEventDao.findCloseGroupByGroupId(groupTrack.getGroupId());
        //1.????????????0,????????????
        if (!Objects.equals(groupTrack.getQuantity(), 0) && notNull(closeEvent)) {
            DoctorBarn doctorBarn = doctorBarnDao.findById(closeEvent.getBarnId());
            expectTrue(Objects.equals(doctorBarn.getStatus(), DoctorBarn.Status.USING.getValue())
                    , "barn.is.not.used", doctorBarn.getName());

            DoctorGroup group = doctorGroupDao.findById(groupTrack.getGroupId());
            if (Objects.equals(group.getPigType(), PigType.DELIVER_SOW.getValue())) {
                List<DoctorGroup> groupList = doctorGroupDao.findByCurrentBarnId(group.getCurrentBarnId());
                expectTrue(groupList.isEmpty(), "lead.to.deliver.two.group", group.getId());
            }
            //(1).??????????????????
            doctorGroupEventDao.delete(closeEvent.getId());
            createModifyLog(closeEvent);

            //(2).??????????????????
            group.setStatus(DoctorGroup.Status.CREATED.getValue());
            doctorGroupDao.update(group);

            groupTrack.setCloseAt(DateUtil.toDate("1970-01-01"));
            doctorGroupTrackDao.update(groupTrack);
            return;
        }

        // TODO: 17/7/5 ?????????????????????,????????????
//        //2.????????????,?????????
//        if (Objects.equals(groupTrack.getQuantity(), 0) && isNull(closeEvent)){
//            DoctorGroup group = doctorGroupDao.findById(groupTrack.getGroupId());
//
//            //(1).??????????????????
//            DoctorCloseGroupInput closeGroupInput = new DoctorCloseGroupInput();
//            closeGroupInput.setEventAt(DateUtil.toDateString(new Date()));
//            closeGroupInput.setIsAuto(IsOrNot.NO.getValue());
//            DoctorGroupEvent closeEvent1 = dozerGroupEvent(group, GroupEventType.CLOSE, closeGroupInput);
//            doctorGroupEventDao.create(closeEvent1);
//
//            //(2).??????????????????
//            group.setStatus(DoctorGroup.Status.CLOSED.getValue());
//            group.setCloseAt(new Date());
//            doctorGroupDao.update(group);
//        }
    }

    /**
     * ?????????????????????????????????(??????)
     *
     * @param deleteGroupEvent ????????????
     */
    public Boolean rollbackHandleCheck(DoctorGroupEvent deleteGroupEvent) {
        return true;
    }

    /**
     * ???????????????????????????(??????)
     *
     * @param deleteGroupEvent ????????????
     */
    protected void triggerEventRollbackHandle(DoctorGroupEvent deleteGroupEvent, Long operatorId, String operatorName) {
    }

    /**
     * ??????????????????(??????)
     *
     * @param deleteGroupEvent ????????????
     * @param oldGroup         ???????????????
     * @return ???????????????
     */
    protected DoctorGroup buildNewGroupForRollback(DoctorGroupEvent deleteGroupEvent, DoctorGroup oldGroup) {
        return oldGroup;
    }

    /**
     * ??????track(??????)
     *
     * @param deleteGroupEvent ????????????
     * @param oldGroupTrack    ???track
     * @return ??? track
     */
    protected DoctorGroupTrack buildNewTrackForRollback(DoctorGroupEvent deleteGroupEvent, DoctorGroupTrack oldGroupTrack) {
        return oldGroupTrack;
    }

    /**
     * ???????????????(??????)
     *
     * @param deleteGroupEvent ????????????
     */
    protected void updateDailyForDelete(DoctorGroupEvent deleteGroupEvent) {
    }

    /**
     * ???????????????????????????
     *
     * @param oldGroupEvent ???????????????
     */
    public void updateDailyOfDelete(DoctorGroupEvent oldGroupEvent) {
    }

    /**
     * ???????????????????????????
     *
     * @param newGroupEvent ????????????
     * @param input         ?????????
     */
    public void updateDailyOfNew(DoctorGroupEvent newGroupEvent, BaseGroupInput input) {
    }

    /**
     * ?????????????????????
     *
     * @param oldDailyGroup ?????????
     * @param changeDto     ?????????
     * @return ???????????????
     */
    protected DoctorGroupDaily buildDailyGroup(DoctorGroupDaily oldDailyGroup, DoctorEventChangeDto changeDto) {
        return expectNotNull(oldDailyGroup, "daily.group.not.null");
    }

    /**
     * ????????????????????????(??????)
     *
     * @param changeDto ????????????
     * @return
     */
    private boolean isUpdateGroup(DoctorEventChangeDto changeDto) {
        //// TODO: 17/4/13 ??????????????????
        return true;
    }

    /**
     * ????????????????????????(??????)
     *
     * @param eventType ????????????
     * @return
     */
    private boolean isUpdateGroup(Integer eventType) {
        //// TODO: 17/4/13 ??????????????????
        return true;
    }

    /**
     * ??????????????????track
     *
     * @param changeDto ????????????
     * @return
     */
    private boolean isUpdateTrack(DoctorEventChangeDto changeDto) {
        // TODO: 17/4/13 ??????????????????
        return true;
    }

    /**
     * ??????????????????track(??????)
     *
     * @param eventType ????????????
     * @return
     */
    private boolean isUpdateTrack(Integer eventType) {
        // TODO: 17/4/13 ??????????????????
        return true;
    }

    private void createTrackSnapshotFroDelete(DoctorGroupEvent deleteEvent, Long modifyLogId) {
        //???????????????track
        DoctorGroupTrack currentTrack = doctorGroupTrackDao.findByGroupId(deleteEvent.getGroupId());
        DoctorTrackSnapshot snapshot = DoctorTrackSnapshot.builder()
                .farmId(deleteEvent.getFarmId())
                .farmName(deleteEvent.getFarmName())
                .businessId(deleteEvent.getGroupId())
                .businessCode(deleteEvent.getGroupCode())
                .businessType(DoctorEventModifyRequest.TYPE.GROUP.getValue())
                .eventId(modifyLogId)
                .eventSource(DoctorTrackSnapshot.EventSource.MODIFY.getValue())
                .trackJson(TO_JSON_MAPPER.toJson(currentTrack))
                .build();
        doctorTrackSnapshotDao.create(snapshot);
    }

    private void createTrackSnapshotFroModify(DoctorGroupEvent newEvent, Long modifyLogId) {
        //???????????????track
        DoctorGroupTrack currentTrack = doctorGroupTrackDao.findByGroupId(newEvent.getGroupId());
        DoctorTrackSnapshot snapshot = DoctorTrackSnapshot.builder()
                .farmId(newEvent.getFarmId())
                .farmName(newEvent.getFarmName())
                .businessId(newEvent.getGroupId())
                .businessCode(newEvent.getGroupCode())
                .businessType(DoctorEventModifyRequest.TYPE.GROUP.getValue())
                .eventId(modifyLogId)
                .eventSource(DoctorTrackSnapshot.EventSource.MODIFY.getValue())
                .trackJson(TO_JSON_MAPPER.toJson(currentTrack))
                .build();
        doctorTrackSnapshotDao.create(snapshot);
    }
    /**
     * ??????????????????
     *
     * @param oldEvent ?????????
     * @param newEvent ?????????
     */
    private Long createModifyLog(DoctorGroupEvent oldEvent, DoctorGroupEvent newEvent) {
        DoctorEventModifyLog modifyLog = DoctorEventModifyLog.builder()
                .businessId(newEvent.getGroupId())
                .businessCode(newEvent.getGroupCode())
                .farmId(newEvent.getFarmId())
                .fromEvent(ToJsonMapper.JSON_NON_DEFAULT_MAPPER.toJson(oldEvent))
                .toEvent(ToJsonMapper.JSON_NON_DEFAULT_MAPPER.toJson(newEvent))
                .type(DoctorEventModifyRequest.TYPE.GROUP.getValue())
                .build();
        doctorEventModifyLogDao.create(modifyLog);
        return modifyLog.getId();
    }

    /**
     * ??????????????????
     *
     * @param deleteEvent ????????????
     */
    private Long createModifyLog(DoctorGroupEvent deleteEvent) {
        DoctorEventModifyLog modifyLog = DoctorEventModifyLog.builder()
                .businessId(deleteEvent.getGroupId())
                .businessCode(deleteEvent.getGroupCode())
                .farmId(deleteEvent.getFarmId())
                .deleteEvent(ToJsonMapper.JSON_NON_DEFAULT_MAPPER.toJson(deleteEvent))
                .type(DoctorEventModifyRequest.TYPE.GROUP.getValue())
                .build();
        doctorEventModifyLogDao.create(modifyLog);

        return modifyLog.getId();
    }

    /**
     * ????????????????????????????????????????????????
     *
     * @param farmId      ??????id
     * @param sumAt       ????????????
     * @param changeCount ???????????????
     */
    protected void updateDailyGroupLiveStock(Long farmId, Integer pigType, Date sumAt, Integer changeCount) {

        List<DoctorGroupDaily> groupDailyList = doctorGroupDailyDao.queryAfterSumAt(farmId, pigType, sumAt);
        groupDailyList.forEach(groupDaily -> {
            groupDaily.setStart(EventUtil.plusInt(groupDaily.getStart(), changeCount));
            groupDaily.setEnd(EventUtil.plusInt(groupDaily.getEnd(), changeCount));
            doctorGroupDailyDao.update(groupDaily);
        });
    }


    /**
     * ????????????????????????????????????????????????
     *
     * @param groupId     ??????id
     * @param sumAt       ????????????
     * @param changeCount ???????????????
     */
    protected void oldUpdateDailyGroupLiveStock(Long groupId, Date sumAt, Integer changeCount) {
        oldDailyGroupDao.updateDailyGroupLiveStock(groupId, sumAt, changeCount);
    }

    /**
     * ??????????????????????????????(????????????)
     *
     * @param groupId       ??????id
     * @param deleteEventId ???????????????ID
     * @param sumAt         ????????????(??????)
     * @param changeCount   ????????????
     */
    public boolean validGroupLiveStockForDelete(Long groupId, Long deleteEventId, Date sumAt, Integer changeCount) {
        List<Integer> includeTypes = Lists.newArrayList(GroupEventType.CHANGE.getValue(), GroupEventType.MOVE_IN.getValue(),
                GroupEventType.TRANS_FARM.getValue(), GroupEventType.TRANS_GROUP.getValue());
        List<DoctorGroupEvent> groupEventList = doctorGroupEventDao.findEventIncludeTypesForDesc(groupId, includeTypes, DateUtil.toDateString(sumAt));
        DoctorGroupTrack groupTrack = doctorGroupTrackDao.findByGroupId(groupId);
        int quantity = EventUtil.plusInt(groupTrack.getQuantity(), changeCount);
        for (DoctorGroupEvent groupEvent : groupEventList) {

            if (quantity < 0) {
                return false;
            }

            //???????????????????????????????????????????????????????????????????????????
            if (groupEvent.getId().equals(deleteEventId))
                break;

            if (Objects.equals(groupEvent.getType(), GroupEventType.MOVE_IN.getValue())) {
                quantity = EventUtil.minusInt(quantity, groupEvent.getQuantity());
            } else {
                quantity = EventUtil.plusInt(quantity, groupEvent.getQuantity());
            }
        }

        // TODO: 18/2/6 ????????????????????????
//        oldValidGroupLiveStockForDelete(groupId, sumAt, changeCount);

        return true;
    }


    /**
     * ?????????????????????????????????????????????????????????????????????
     * @param groupId ??????id
     * @param groupCode ??????code
     * @param eventAt ????????????
     * @param changeCount ????????????
     */
    public void validGroupLiveStock(Long groupId, String groupCode, Date eventAt, Integer changeCount) {
        List<Integer> includeTypes = Lists.newArrayList(GroupEventType.CHANGE.getValue(), GroupEventType.MOVE_IN.getValue(),
                GroupEventType.TRANS_FARM.getValue(), GroupEventType.TRANS_GROUP.getValue());
        Date dayAfterEventAt = new DateTime(eventAt).plusDays(1).toDate();
        List<DoctorGroupEvent> groupEventList = doctorGroupEventDao.findEventIncludeTypesForDesc(groupId, includeTypes, DateUtil.toDateString(dayAfterEventAt));
        DoctorGroupTrack groupTrack = doctorGroupTrackDao.findByGroupId(groupId);
        int quantity = EventUtil.plusInt(groupTrack.getQuantity(), changeCount);

        if (quantity < 0) {
            throw new InvalidException("new.report.group.live.stock.lower.zero", groupCode, DateUtil.toDateString(new Date()));
        }

        for (DoctorGroupEvent groupEvent : groupEventList) {

            if (quantity < 0) {
                throw new InvalidException("new.report.group.live.stock.lower.zero", groupCode, DateUtil.toDateString(groupEvent.getEventAt()));
            }

            if (Objects.equals(groupEvent.getType(), GroupEventType.MOVE_IN.getValue())) {
                quantity = EventUtil.minusInt(quantity, groupEvent.getQuantity());
            } else {
                quantity = EventUtil.plusInt(quantity, groupEvent.getQuantity());
            }
        }
    }

    /**
     * ???????????????????????????????????????
     *
     * @param groupId     ??????id
     * @param oldEventAt  ?????????
     * @param newEventAt  ?????????
     * @param oldQuantity ?????????
     * @param newQuantity ?????????
     */
    public void validGroupLiveStock(Long groupId, String groupCode, Long eventId, Date oldEventAt, Date newEventAt, Integer oldQuantity,
                                       Integer newQuantity, Integer changeCount) {
        oldEventAt = new DateTime(oldEventAt).withTimeAtStartOfDay().toDate();
        newEventAt = new DateTime(newEventAt).withTimeAtStartOfDay().toDate();
        Date sumAt = oldEventAt.before(newEventAt) ? oldEventAt : newEventAt;
        List<Integer> includeTypes = Lists.newArrayList(GroupEventType.CHANGE.getValue(), GroupEventType.MOVE_IN.getValue(),
                GroupEventType.TRANS_FARM.getValue(), GroupEventType.TRANS_GROUP.getValue());
        List<DoctorGroupEvent> groupEventList = doctorGroupEventDao.findEventIncludeTypesForDesc(groupId, includeTypes, DateUtil.toDateString(sumAt));
        DoctorGroupTrack groupTrack = doctorGroupTrackDao.findByGroupId(groupId);

        int quantity;

        //???????????????????????????
        if (oldEventAt.equals(newEventAt)) {
            quantity = EventUtil.plusInt(groupTrack.getQuantity(), changeCount);

            for (DoctorGroupEvent groupEvent : groupEventList) {
                if (quantity < 0) {
                    throw new InvalidException("new.report.group.live.stock.lower.zero", groupCode, DateUtil.toDateString(groupEvent.getEventAt()));
                }

                //????????????????????????????????????????????????????????????????????????
                if (groupEvent.getId().equals(eventId)) {
                    break;
                }

                if (Objects.equals(groupEvent.getType(), GroupEventType.MOVE_IN.getValue())) {
                    quantity = EventUtil.minusInt(quantity, groupEvent.getQuantity());
                } else {
                    quantity = EventUtil.plusInt(quantity, groupEvent.getQuantity());
                }
            }
        } else {

            //?????????????????????
            quantity = groupTrack.getQuantity();

            for (DoctorGroupEvent groupEvent : groupEventList) {

                int sameDayLiveStock = quantity;

                if (!groupEvent.getEventAt().before(oldEventAt)) {
                    sameDayLiveStock += oldQuantity;
                }

                if (!groupEvent.getEventAt().before(newEventAt)) {
                    sameDayLiveStock += newQuantity;
                }

                if (sameDayLiveStock < 0) {
                    throw new InvalidException("new.report.group.live.stock.lower.zero", groupCode, DateUtil.toDateString(groupEvent.getEventAt()));
                }

                if (Objects.equals(groupEvent.getType(), GroupEventType.MOVE_IN.getValue())) {
                    quantity = EventUtil.minusInt(quantity, groupEvent.getQuantity());
                } else {
                    quantity = EventUtil.plusInt(quantity, groupEvent.getQuantity());
                }
            }
        }

        // TODO: 18/2/6 ????????????????????????
//        oldValidGroupLiveStock(groupId, groupCode, oldEventAt, newEventAt, oldQuantity, newQuantity, changeCount);

    }


    /**
     * ??????????????????????????????(????????????)
     *
     * @param groupId     ??????id
     * @param sumAt       ????????????(??????)
     * @param changeCount ????????????
     */
    public boolean oldValidGroupLiveStockForDelete(Long groupId, Date sumAt, Integer changeCount) {
        List<DoctorDailyGroup> dailyGroupList = oldDailyGroupDao.findAfterSumAt(groupId, DateUtil.toDateString(sumAt));
        for (DoctorDailyGroup dailyGroup : dailyGroupList) {
            if (EventUtil.plusInt(dailyGroup.getEnd(), changeCount) < 0) {
                return false;
            }
        }
        return true;
    }

    /**
     * ??????????????????
     *
     * @param groupId     ??????id
     * @param oldEventAt  ?????????
     * @param newEventAt  ?????????
     * @param oldQuantity ?????????
     * @param newQuantity ?????????
     */
    protected void oldValidGroupLiveStock(Long groupId, String groupCode, Date oldEventAt, Date newEventAt, Integer oldQuantity, Integer newQuantity, Integer changeCount) {
        Date sumAt = oldEventAt.before(newEventAt) ? oldEventAt : newEventAt;
        List<DoctorDailyGroup> dailyGroupList = oldDailyGroupDao.findAfterSumAt(groupId, DateUtil.toDateString(sumAt));

        //?????????????????????????????????????????????????????????????????????
        DoctorDailyGroup doctorDailyGroup = oldDailyGroupDao.findByGroupIdAndSumAt(groupId, newEventAt);
        if (isNull(doctorDailyGroup)) {
            dailyGroupList.add(oldDailyReportManager.findByGroupIdAndSumAt(groupId, newEventAt));
        }

        if (Objects.equals(oldEventAt, newEventAt)) {
            dailyGroupList.stream()
                    .filter(dailyGroup -> !oldEventAt.after(dailyGroup.getSumAt()))
                    .forEach(dailyGroup -> dailyGroup.setEnd(EventUtil.plusInt(dailyGroup.getEnd(), changeCount)));
        } else {
            dailyGroupList.stream()
                    .filter(dailyGroup -> !oldEventAt.after(dailyGroup.getSumAt()))
                    .forEach(dailyGroup -> dailyGroup.setEnd(EventUtil.plusInt(dailyGroup.getEnd(), oldQuantity)));
            dailyGroupList.stream()
                    .filter(dailyGroup -> !newEventAt.after(dailyGroup.getSumAt()))
                    .forEach(dailyGroup -> dailyGroup.setEnd(EventUtil.plusInt(dailyGroup.getEnd(), newQuantity)));
        }
        for (DoctorDailyGroup dailyGroup : dailyGroupList) {
            expectTrue(notNull(dailyGroup.getEnd()) && dailyGroup.getEnd() >= 0,
                    "group.live.stock.lower.zero", groupCode, DateUtil.toDateString(dailyGroup.getSumAt()));
        }
    }

    /**
     * ?????????????????????
     *
     * @param date ??????
     * @return ?????????
     */
    public static Date getAfterDay(Date date) {
        return new DateTime(date).plusDays(1).toDate();
    }

    /**
     * ??????????????????, ?????????????????????, ?????????????????????
     *
     * @param eventAt     ????????????
     * @param downEventAt ????????????
     * @param upEventAt   ????????????
     */
    public static void validEventAt(Date eventAt, Date downEventAt, Date upEventAt) {
        if ((notNull(downEventAt)
                && Dates.startOfDay(eventAt).before(Dates.startOfDay(downEventAt)))
                || (notNull(upEventAt) && Dates.startOfDay(eventAt).after(Dates.startOfDay(upEventAt)))) {
            throw new InvalidException("event.at.error", DateUtil.toDateString(downEventAt), DateUtil.toDateString(upEventAt));
        }
    }

    /**
     * ????????????????????????
     *
     * @param groupEvent ?????????
     * @return ????????????
     */
    protected BaseGroupInput buildGroupCloseInput(DoctorGroupEvent groupEvent) {
        DoctorCloseGroupInput closeInput = new DoctorCloseGroupInput();
        closeInput.setIsAuto(IsOrNot.YES.getValue());
        closeInput.setEventAt(DateUtil.toDateString(groupEvent.getEventAt()));
        closeInput.setRelGroupEventId(groupEvent.getId());
        return closeInput;
    }

    protected DoctorGroupEvent dozerGroupEvent(DoctorGroup group, GroupEventType eventType, BaseGroupInput baseInput) {
        DoctorGroupEvent event = new DoctorGroupEvent();
        event.setEventAt(DateUtil.toDate(baseInput.getEventAt()));
        event.setOrgId(group.getOrgId());       //????????????
        event.setOrgName(group.getOrgName());
        event.setFarmId(group.getFarmId());     //????????????
        event.setFarmName(group.getFarmName());
        event.setGroupId(group.getId());        //????????????
        event.setGroupCode(group.getGroupCode());
        event.setType(eventType.getValue());    //????????????
        event.setName(eventType.getDesc());
        event.setBarnId(group.getCurrentBarnId());      //??????????????????
        event.setBarnName(group.getCurrentBarnName());
        event.setPigType(group.getPigType());           //??????
        event.setIsAuto(baseInput.getIsAuto());
        event.setCreatorId(baseInput.getCreatorId());   //?????????
        event.setCreatorName(baseInput.getCreatorName());
        event.setOperatorId(baseInput.getCreatorId());
        event.setOperatorName(baseInput.getCreatorName());
        event.setDesc(baseInput.generateEventDesc());
        event.setRemark(baseInput.getRemark());
        event.setRelGroupEventId(baseInput.getRelGroupEventId());
        event.setRelPigEventId(baseInput.getRelPigEventId());
        event.setStatus(EventStatus.VALID.getValue());
        event.setEventSource(SourceType.INPUT.getValue());
        return event;
    }

}
