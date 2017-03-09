package io.terminus.doctor.event.handler;

import com.google.common.base.MoreObjects;
import io.terminus.common.utils.Dates;
import io.terminus.doctor.common.exception.InvalidException;
import io.terminus.doctor.common.utils.DateUtil;
import io.terminus.doctor.common.utils.JsonMapperUtil;
import io.terminus.doctor.event.dao.DoctorBarnDao;
import io.terminus.doctor.event.dao.DoctorPigDao;
import io.terminus.doctor.event.dao.DoctorPigEventDao;
import io.terminus.doctor.event.dao.DoctorPigSnapshotDao;
import io.terminus.doctor.event.dao.DoctorPigTrackDao;
import io.terminus.doctor.event.dao.DoctorRevertLogDao;
import io.terminus.doctor.event.dto.DoctorBasicInputInfoDto;
import io.terminus.doctor.event.dto.DoctorPigSnapShotInfo;
import io.terminus.doctor.event.dto.event.BasePigEventInputDto;
import io.terminus.doctor.event.dto.event.DoctorEventInfo;
import io.terminus.doctor.event.enums.EventStatus;
import io.terminus.doctor.event.enums.IsOrNot;
import io.terminus.doctor.event.enums.PigEvent;
import io.terminus.doctor.event.model.DoctorBarn;
import io.terminus.doctor.event.model.DoctorPig;
import io.terminus.doctor.event.model.DoctorPigEvent;
import io.terminus.doctor.event.model.DoctorPigSnapshot;
import io.terminus.doctor.event.model.DoctorPigTrack;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.List;
import java.util.Objects;

import static io.terminus.common.utils.Arguments.notEmpty;
import static io.terminus.common.utils.Arguments.notNull;
import static io.terminus.doctor.common.utils.Checks.expectTrue;

/**
 * Created by xjn.
 * Date:2017/1/3
 */
@Slf4j
public abstract class DoctorAbstractEventHandler implements DoctorPigEventHandler {

    @Autowired
    protected  DoctorPigDao doctorPigDao;
    @Autowired
    protected  DoctorPigEventDao doctorPigEventDao;
    @Autowired
    protected  DoctorPigTrackDao doctorPigTrackDao;
    @Autowired
    protected  DoctorPigSnapshotDao doctorPigSnapshotDao;
    @Autowired
    protected  DoctorRevertLogDao doctorRevertLogDao;
    @Autowired
    protected DoctorBarnDao doctorBarnDao;

    protected static final JsonMapperUtil JSON_MAPPER = JsonMapperUtil.JSON_NON_EMPTY_MAPPER;

    @Override
    public void handleCheck(BasePigEventInputDto eventDto, DoctorBasicInputInfoDto basic) {
        checkEventAt(eventDto);
    }

    @Override
    public void handle(List<DoctorEventInfo> doctorEventInfoList, DoctorPigEvent executeEvent, DoctorPigTrack fromTrack) {
//        //获取镜像有关event和track
//        DoctorPigTrack pigSnapshotTrack = doctorPigTrackDao.findByPigId(inputDto.getPigId());
//        expectTrue(notNull(pigSnapshotTrack), "pig.track.not.null", inputDto.getPigId());
//        DoctorPigEvent pigSnapshotEvent = doctorPigEventDao.queryLastPigEventById(inputDto.getPigId());
//        expectTrue(notNull(pigSnapshotEvent), "pig.last.event.not.null", inputDto.getPigId());


        //1.创建事件
        //DoctorPigEvent doctorPigEvent = buildPigEvent(basic, inputDto);
        doctorPigEventDao.create(executeEvent);

        //2.创建或更新track
        DoctorPigTrack toTrack = buildPigTrack(executeEvent, fromTrack);
        doctorPigTrackDao.update(toTrack);

        //3.创建镜像
        DoctorPigSnapshot doctorPigSnapshot = createPigSnapshot(toTrack, executeEvent, fromTrack.getCurrentEventId());
        doctorPigSnapshotDao.create(doctorPigSnapshot);

        //4.特殊处理
        specialHandle(executeEvent, toTrack);

        //5.记录发生的事件信息
        DoctorBarn doctorBarn = doctorBarnDao.findById(toTrack.getCurrentBarnId());
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
                .pigType(doctorBarn.getPigType())
                .build();
        doctorEventInfoList.add(doctorEventInfo);

        //6.触发事件
        triggerEvent(doctorEventInfoList, executeEvent, toTrack);
    }

    /**
     * 构建基础事件信息
     * @param basic
     * @param inputDto
     * @return
     */
    @Override
    public DoctorPigEvent buildPigEvent(DoctorBasicInputInfoDto basic, BasePigEventInputDto inputDto) {
        DoctorPigEvent doctorPigEvent = DoctorPigEvent.builder()
                .orgId(basic.getOrgId()).orgName(basic.getOrgName())
                .farmId(basic.getFarmId()).farmName(basic.getFarmName())
                .pigId(inputDto.getPigId()).pigCode(inputDto.getPigCode())
                .eventAt(generateEventAt(inputDto.eventAt())).type(inputDto.getEventType())
                .barnId(inputDto.getBarnId()).barnName(inputDto.getBarnName())
                .kind(inputDto.getPigType()).relPigEventId(inputDto.getRelPigEventId()).relGroupEventId(inputDto.getRelGroupEventId())
                .name(inputDto.getEventName()).desc(basic.generateEventDescFromExtra(inputDto))
                .operatorId(MoreObjects.firstNonNull(inputDto.getOperatorId(), basic.getStaffId()))
                .operatorName(StringUtils.hasText(inputDto.getOperatorName()) ? inputDto.getOperatorName() : basic.getStaffName())
                .creatorId(basic.getStaffId()).creatorName(basic.getStaffName())
                .isAuto(MoreObjects.firstNonNull(inputDto.getIsAuto(), IsOrNot.NO.getValue()))
                .status(EventStatus.VALID.getValue())
                .npd(0)
                .dpnpd(0)
                .pfnpd(0)
                .plnpd(0)
                .psnpd(0)
                .pynpd(0)
                .ptnpd(0)
                .jpnpd(0)
                .build();
        DoctorPigTrack doctorPigTrack = doctorPigTrackDao.findByPigId(inputDto.getPigId());
        if (doctorPigTrack != null) {
            doctorPigEvent.setPigStatusBefore(doctorPigTrack.getStatus());
            doctorPigEvent.setParity(doctorPigTrack.getCurrentParity());
        }
        doctorPigEvent.setExtraMap(inputDto.toMap());
        return doctorPigEvent;
    }

    /**
     * 构建事件发生后的track
     * @param executeEvent 发生事件信息
     * @param fromTrack 事件发生前的track
     * @return 事件发生后track
     */
    protected DoctorPigTrack buildPigTrack(DoctorPigEvent executeEvent, DoctorPigTrack fromTrack) {
        return fromTrack;
    }

    /**
     *  创建猪跟踪和镜像表
     *  @param toTrack 事件发生导致track
     *  @param executeEvent 发生事件
     *  @param lastEventId 上一次事件
     *
     */
    protected DoctorPigSnapshot createPigSnapshot(DoctorPigTrack toTrack, DoctorPigEvent executeEvent, Long lastEventId) {
        DoctorPig snapshotPig = doctorPigDao.findById(toTrack.getPigId());
        expectTrue(notNull(snapshotPig), "pig.not.null", toTrack.getPigId());


        //创建猪镜像
        return DoctorPigSnapshot.builder()
                .pigId(snapshotPig.getId())
                .fromEventId(lastEventId)
                .toEventId(executeEvent.getId())
                .toPigInfo(JSON_MAPPER.toJson(
                        DoctorPigSnapShotInfo.builder().pig(snapshotPig).pigTrack(toTrack).build()))
                .build();
    }

    /**
     * 事件的创建后的补充和特殊处理
     * @param executeEvent 执行的猪事件
     * @param toTrack 执行事件后导致的猪track
     */
    protected void specialHandle(DoctorPigEvent executeEvent, DoctorPigTrack toTrack){
        executeEvent.setPigStatusAfter(toTrack.getStatus());
        doctorPigEventDao.update(executeEvent);
    }

    /**
     * 触发事件, 触发其他事件时需要实现此方法
     * @param doctorEventInfoList 事件信息列表
     * @param executeEvent 执行的猪事件
     * @param toTrack 执行事件后导致的猪track
     */
    protected void triggerEvent(List<DoctorEventInfo> doctorEventInfoList, DoctorPigEvent executeEvent, DoctorPigTrack toTrack) {

    }

    /**
     * 构建自动事件的共有信息(原事件与触发事件为同一头猪时)
     * @param fromInputDto 原事件信息
     * @param toInputDto 被触发事件信息
     * @param pigEvent 被触发的事件
     */
    protected void buildAutoEventCommonInfo(BasePigEventInputDto fromInputDto, BasePigEventInputDto toInputDto, PigEvent pigEvent, Long fromEventId) {
        toInputDto.setIsAuto(IsOrNot.YES.getValue());
        toInputDto.setPigId(fromInputDto.getPigId());
        toInputDto.setPigCode(fromInputDto.getPigCode());
        toInputDto.setPigType(fromInputDto.getPigType());
        toInputDto.setBarnId(fromInputDto.getBarnId());
        toInputDto.setBarnName(fromInputDto.getBarnName());
        toInputDto.setRelPigEventId(fromEventId);
        toInputDto.setEventName(pigEvent.getName());
        toInputDto.setEventType(pigEvent.getKey());
        toInputDto.setEventDesc(pigEvent.getDesc());
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

    /**
     * 事件日期校验
     * @param inputDto
     */
    private void checkEventAt(BasePigEventInputDto inputDto) {
        if (Objects.equals(inputDto.getPigType(), PigEvent.ENTRY.getKey())) {
            return;
        }
        Date eventAt = inputDto.eventAt();
        DoctorPigEvent lastEvent = doctorPigEventDao.queryLastPigEventById(inputDto.getPigId());
        if (notNull(lastEvent) && (Dates.startOfDay(eventAt).before(Dates.startOfDay(lastEvent.getEventAt())) || Dates.startOfDay(eventAt).after(Dates.startOfDay(new Date())))) {
            throw new InvalidException("event.at.range.error", DateUtil.toDateString(lastEvent.getEventAt()), DateUtil.toDateString(new Date()), DateUtil.toDateString(eventAt));
        }
    }

    /**
     * 新建猪群时自动生成猪群号
     * @param barnName 猪舍名
     * @return 猪群号
     */
    protected String grateGroupCode(String barnName) {
        expectTrue(notEmpty(barnName), "generate.code.barn.name.not.null");
        return barnName + "(" +DateUtil.toDateString(new Date()) + ")";
    }
}
