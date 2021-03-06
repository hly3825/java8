package io.terminus.doctor.web.admin.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.MoreObjects;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import io.terminus.common.utils.JsonMapper;
import io.terminus.common.utils.Splitters;
import io.terminus.doctor.basic.model.DoctorBasicMaterial;
import io.terminus.doctor.basic.model.DoctorChangeReason;
import io.terminus.doctor.basic.model.DoctorCustomer;
import io.terminus.doctor.basic.service.DoctorBasicMaterialReadService;
import io.terminus.doctor.basic.service.DoctorBasicReadService;
import io.terminus.doctor.common.enums.PigType;
import io.terminus.doctor.common.enums.SourceType;
import io.terminus.doctor.common.utils.*;
import io.terminus.doctor.event.dto.DoctorGroupDetail;
import io.terminus.doctor.event.dto.search.SearchedPig;
import io.terminus.doctor.event.enums.PigStatus;
import io.terminus.doctor.event.model.*;
import io.terminus.doctor.event.service.*;
import io.terminus.doctor.web.admin.dto.DoctorGroupEventDetail;
import io.terminus.doctor.web.admin.utils.SmartGroupEventHandler;
import io.terminus.doctor.web.admin.utils.SmartPigEventBuilder;
import io.terminus.doctor.web.admin.utils.TransFromUtil;
import io.terminus.doctor.web.admin.vo.PigAndPigGroup;
import lombok.Data;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.time.DateUtils;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.Errors;
import org.springframework.validation.SmartValidator;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;

import javax.annotation.PostConstruct;
import javax.validation.Valid;
import java.util.*;

import static io.terminus.doctor.common.utils.Checks.expectNotNull;
import static java.util.stream.Collectors.toList;

/**
 * Created by sunbo@terminus.io on 2017/9/8.
 */
@Slf4j
@Setter
@RestController
@RequestMapping("api/doctor/admin/god")
public class DoctorGodController {

    private static final ObjectMapper OBJECT_MAPPER = JsonMapper.JSON_NON_DEFAULT_MAPPER.getMapper();
    protected final ToJsonMapper TO_JSON_MAPPER = ToJsonMapper.JSON_NON_DEFAULT_MAPPER;


    @RpcConsumer
    private DoctorGroupReadService doctorGroupReadService;
    @RpcConsumer
    private DoctorGroupWriteService doctorGroupWriteService;

    @RpcConsumer
    private DoctorBasicReadService doctorBasicReadService;
    @RpcConsumer
    private DoctorBarnReadService doctorBarnReadService;
    @RpcConsumer
    private DoctorPigEventReadService doctorPigEventReadService;
    @RpcConsumer
    private DoctorPigEventWriteService doctorPigEventWriteService;

    @RpcConsumer
    private DoctorPigReadService doctorPigReadService;
    @RpcConsumer
    private DoctorPigWriteService doctorPigWriteService;

    @RpcConsumer
    private DoctorBasicMaterialReadService doctorBasicMaterialReadService;
    @RpcConsumer
    private DoctorModifyEventService doctorModifyEventService;

    @RpcConsumer
    private DoctorDailyGroupWriteService doctorDailyGroupWriteService;
    @RpcConsumer
    private DoctorDailyReportWriteService doctorDailyReportWriteService;
    @RpcConsumer
    private DoctorRangeReportWriteService doctorRangeReportWriteService;
    @RpcConsumer
    private DoctorParityMonthlyReportWriteService doctorParityMonthlyReportWriteService;
    @RpcConsumer
    private DoctorBoarMonthlyReportWriteService doctorBoarMonthlyReportWriteService;

    private SmartValidator validator;

    @Autowired
    private TransFromUtil transFromUtil;

    @Autowired
    private SmartPigEventBuilder pigEventBuilder;
    @Autowired
    private SmartGroupEventHandler groupEventHandler;

    @Autowired
    private EventBus eventBus;

    @PostConstruct
    public void init() {
        eventBus.register(this);
    }

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        validator = (SmartValidator) binder.getValidator();
    }

    @RequestMapping(method = RequestMethod.GET, value = "event")
    public Object eventPaging(@RequestParam Map<String, Object> params,
                              @RequestParam(required = false) Integer pageNo,
                              @RequestParam(required = false) Integer pageSize) {


        if (params == null || params.isEmpty()) {
            return Paging.empty();
        }
        if (params.get("kind") == null || "".equals(params.get("kind"))) {
            params.put("kind", 1);
        }

        //execl?????????????????????????????????
        List<Integer> eventSources = new ArrayList<>(2);
        eventSources.add(SourceType.IMPORT.getValue());
        eventSources.add(SourceType.MOVE.getValue());
        params.put("eventSources", eventSources);

        //?????????????????????????????????????????????
        Date threeMonthAgo = DateUtils.addMonths(new Date(), -3);
        if (params.containsKey("endDate")) {
            Date endDate = DateUtil.toDate(params.get("endDate").toString());
            if (endDate.after(threeMonthAgo)) {
                params.put("endDate", DateUtil.toDateString(threeMonthAgo));
            }
        } else
            params.put("endDate", DateUtil.toDateString(threeMonthAgo));

        //??????  kind????????????
        String kind = String.valueOf(params.get("kind"));
        switch (kind) {
            case "4":
                //??????????????????
                return this.queryGroupEventsByCriteria(params, pageNo, pageSize);
            case "1":
            case "2":
            case "3":
                params.put("ordered", 0);
                return this.queryPigEventsByCriteria(params, pageNo, pageSize);
            default:
                return Paging.empty();
        }
    }

    @RequestMapping(method = RequestMethod.DELETE, value = "event/{id}")
    public Boolean eventDelete(@PathVariable Long id) {

        return RespWithExHelper.orInvalid(doctorPigEventWriteService.delete(id));
    }


    @RequestMapping(method = RequestMethod.PUT, value = "event/pig/{id}")
    public boolean eventEdit(@PathVariable("id") Long eventId,
                             @RequestBody String input) {

        if (StringUtils.isBlank(input))
            throw new JsonResponseException("god.event.input.blank");

        Response<DoctorPigEvent> pigEventResponse = doctorPigEventReadService.findById(eventId);
        if (!pigEventResponse.isSuccess())
            throw new JsonResponseException(pigEventResponse.getError());
        if (null == pigEventResponse.getResult())
            throw new JsonResponseException("god.event.not.found");
        DoctorPigEvent pigEvent = pigEventResponse.getResult();
        String oldPigEvent = TO_JSON_MAPPER.toJson(pigEvent);

        if (pigEventBuilder.isSupportedEvent(pigEvent))
            pigEventBuilder.buildEvent(input, pigEvent);

        return RespWithExHelper.orInvalid(doctorModifyEventService.modifyPigEvent(oldPigEvent, pigEvent));
    }

    @RequestMapping(method = RequestMethod.PUT, value = "event/group/{id}")
    public boolean groupEventEdit(@PathVariable("id") Long eventId,
                                  @RequestBody String input) {

        if (StringUtils.isBlank(input))
            throw new JsonResponseException("god.event.input.blank");

        DoctorGroupEvent groupEvent = expectNotNull(RespHelper.or500(doctorGroupReadService.findGroupEventById(eventId)), "group.event.not.found");
        String oldPigEvent = TO_JSON_MAPPER.toJson(groupEvent);

        if (groupEventHandler.isSupported(groupEvent))
            groupEventHandler.updateEvent(input, groupEvent);

        return RespWithExHelper.orInvalid(doctorModifyEventService.modifyGroupEvent(oldPigEvent, groupEvent));
    }

    @RequestMapping(method = RequestMethod.GET, value = "report/refresh")
    public boolean refreshReport(@RequestParam Integer type,//1??????2??????
                                 @RequestParam Long farmId,
                                 @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date startDate,
                                 @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date endDate) {
        //???????????????????????????
        //????????????????????????????????????
        if (null == endDate)
            endDate = new Date();

        if (1 != type.intValue() && 2 != type.intValue()) {
            throw new JsonResponseException("refresh.target.type.not.support");
        }

        RefreshReportEvent event = new RefreshReportEvent();
        event.setType(type);
        event.setFarmId(farmId);
        event.setStartDate(startDate);
        event.setEndDate(endDate);
        eventBus.post(event);

        return true;
    }


    @RequestMapping(method = RequestMethod.GET, value = "status")
    public PigAndPigGroup pigAndGroupQuery(@RequestParam Long farmId,
                                           @RequestParam int type,//1?????????2??????,4??????
                                           @RequestParam String code) {

        PigAndPigGroup vo = new PigAndPigGroup();

        if (type == 4) {
            Response<DoctorGroup> groupResponse = doctorGroupReadService.findGroupByFarmIdAndGroupCode(farmId, code);
            if (!groupResponse.isSuccess())
                throw new JsonResponseException(groupResponse.getError());
            if (null == groupResponse.getResult())
                throw new JsonResponseException("pig.group.not.found");

            DoctorGroup group = groupResponse.getResult();
            Response<DoctorGroupDetail> groupDetailResponse = doctorGroupReadService.findGroupDetailByGroupId(group.getId());
            if (!groupDetailResponse.isSuccess())
                throw new JsonResponseException(groupDetailResponse.getError());
            if (null == groupDetailResponse.getResult() || null == groupDetailResponse.getResult().getGroupTrack())
                throw new JsonResponseException("pig.group.track.not.found");

            DoctorGroupTrack groupTrack = groupDetailResponse.getResult().getGroupTrack();

            vo.setId(group.getId());
            vo.setQuantity(groupTrack.getQuantity());//??????????????????
            vo.setAvgDayAge(groupTrack.getAvgDayAge());//????????????
            vo.setOpenAt(group.getOpenAt());//??????????????????
            vo.setCurrentBarnId(group.getCurrentBarnId());//????????????
            vo.setUnweanQty(groupTrack.getUnweanQty());
            vo.setHealthyQty(groupTrack.getHealthyQty());
            vo.setWeakQty(groupTrack.getWeakQty());
            vo.setLiveQty(groupTrack.getLiveQty());

        } else {
            Map<String, Object> params = new HashMap<>();
            params.put("farmId", farmId);
            params.put("pigType", type);
            params.put("precisePigCode", code);
            params.put("all", "1");//?????????????????????all??????null??????
            Response<Paging<SearchedPig>> pagingResponse = doctorPigReadService.pagingPig(params, 1, 1);
            if (!pagingResponse.isSuccess())
                throw new JsonResponseException(pagingResponse.getError());
            if (pagingResponse.getResult().getData().isEmpty())
                throw new JsonResponseException("pig.not.found");

            SearchedPig pig = pagingResponse.getResult().getData().get(0);

            Response<DoctorPigTrack> pigTrackResponse = doctorPigReadService.findPigTrackByPigId(pig.getId());
            if (!pigTrackResponse.isSuccess())
                throw new JsonResponseException(pigTrackResponse.getError());
            if (null == pigTrackResponse.getResult())
                throw new JsonResponseException("pig.track.not.found");
            DoctorPigTrack pigTrack = pigTrackResponse.getResult();

            vo.setId(pig.getId());
            vo.setStatus(pig.getStatus());
            if (vo.getStatus().intValue() == PigStatus.KongHuai.getKey().intValue()) {
                vo.setStatus(pig.getPregCheckResult());
            }
            vo.setBreedId(pig.getBreedId());//??????
            vo.setCurrentBarnId(pig.getCurrentBarnId());//??????
            vo.setCurrentParity(pig.getCurrentParity());//??????
            vo.setGeneticId(pig.getGeneticId());//??????

            vo.setBoarType(pig.getBoarType());//????????????
            vo.setIsRemoval(pigTrack.getIsRemoval());//????????????
            vo.setFarrowQty(pigTrack.getFarrowQty());//????????????
            vo.setUnweanQty(pigTrack.getUnweanQty());//???????????????
            vo.setFarrowAvgWeight(pigTrack.getFarrowAvgWeight());//????????????(kg)
            vo.setWeanQty(pigTrack.getWeanQty());//????????????
            vo.setWeanAvgWeight(pigTrack.getWeanAvgWeight());//????????????(kg)
        }
        return vo;
    }

    @RequestMapping(method = RequestMethod.PUT, value = "status")
    public boolean pigAndGroupEdit(@RequestParam Long farmId,
                                   @RequestParam int type,//1?????????2??????,4??????
                                   @RequestParam long id,
                                   @RequestBody @Valid PigAndPigGroup pigAndPigGroup,
                                   Errors errors) {
        if (errors.hasErrors())
            throw new JsonResponseException(errors.getFieldError().getDefaultMessage());

        if (type == 1 || type == 2) {//?????????
            Response<DoctorPig> pigResponse = doctorPigReadService.findPigById(id);
            if (!pigResponse.isSuccess())
                throw new JsonResponseException(pigResponse.getError());
            if (pigResponse.getResult() == null)
                throw new JsonResponseException("pig.not.found");

            DoctorPig pig = pigResponse.getResult();

            Response<DoctorPigTrack> pigTrackResponse = doctorPigReadService.findPigTrackByPigId(pig.getId());
            if (!pigTrackResponse.isSuccess())
                throw new JsonResponseException(pigTrackResponse.getError());
            if (null == pigTrackResponse.getResult())
                throw new JsonResponseException("pig.track.not.found");
            DoctorPigTrack pigTrack = pigTrackResponse.getResult();

            pig.setBreedId(pigAndPigGroup.getBreedId());
            pig.setBreedName(RespHelper.orServEx(doctorBasicReadService.findBasicById(pigAndPigGroup.getBreedId())).getName());
            pig.setGeneticId(pigAndPigGroup.getGeneticId());
            if (null != pigAndPigGroup.getGeneticId())
                pig.setGeneticName(RespHelper.orServEx(doctorBasicReadService.findBasicById(pigAndPigGroup.getGeneticId())).getName());
            if (pig.getPigType().intValue() == DoctorPig.PigSex.BOAR.getKey())
                pig.setBoarType(pigAndPigGroup.getBoarType());


            if (PigStatus.BOAR_LEAVE.getKey().intValue() == pigAndPigGroup.getStatus() || PigStatus.Removal.getKey().intValue() == pigAndPigGroup.getStatus()) {
                pigTrack.setIsRemoval(1);
                pig.setIsRemoval(1);
            } else {
                pig.setIsRemoval(pigAndPigGroup.getIsRemoval());
                pigTrack.setIsRemoval(pigAndPigGroup.getIsRemoval());
            }

            pigTrack.setStatus(pigAndPigGroup.getStatus());
            pigTrack.setCurrentBarnId(pigAndPigGroup.getCurrentBarnId());
            pigTrack.setCurrentBarnName(RespHelper.orServEx(doctorBarnReadService.findBarnById(pigAndPigGroup.getCurrentBarnId())).getName());
            pigTrack.setCurrentParity(pigAndPigGroup.getCurrentParity());
            pigTrack.setFarrowQty(pigAndPigGroup.getFarrowQty());
            pigTrack.setUnweanQty(pigAndPigGroup.getUnweanQty());
            pigTrack.setFarrowAvgWeight(pigAndPigGroup.getFarrowAvgWeight());
            pigTrack.setWeanQty(pigAndPigGroup.getWeanQty());
            pigTrack.setWeanAvgWeight(pigAndPigGroup.getWeanAvgWeight());


            doctorPigWriteService.updatePig(pig, pigTrack);
        } else if (type == 4) {//????????????

            DoctorGroup group = RespHelper.or500(doctorGroupReadService.findGroupById(id));
            if (null == group)
                throw new JsonResponseException("group.not.found");
            Response<DoctorGroupDetail> groupDetailResponse = doctorGroupReadService.findGroupDetailByGroupId(group.getId());
            if (!groupDetailResponse.isSuccess())
                throw new JsonResponseException(groupDetailResponse.getError());
            if (null == groupDetailResponse.getResult() || null == groupDetailResponse.getResult().getGroupTrack())
                throw new JsonResponseException("group.track.not.found");

            DoctorGroupTrack groupTrack = groupDetailResponse.getResult().getGroupTrack();

            groupTrack.setQuantity(pigAndPigGroup.getQuantity());//??????????????????
            groupTrack.setAvgDayAge(pigAndPigGroup.getAvgDayAge());//????????????
            group.setOpenAt(group.getOpenAt());//??????????????????
            group.setCurrentBarnId(group.getCurrentBarnId());//????????????
            group.setCurrentBarnName(RespHelper.orServEx(doctorBarnReadService.findBarnById(pigAndPigGroup.getCurrentBarnId())).getName());
            groupTrack.setUnweanQty(pigAndPigGroup.getUnweanQty());
            groupTrack.setHealthyQty(pigAndPigGroup.getHealthyQty());
            groupTrack.setWeakQty(pigAndPigGroup.getWeakQty());
            groupTrack.setLiveQty(pigAndPigGroup.getLiveQty());

            doctorGroupWriteService.updateGroup(group, groupTrack);
        } else {
            throw new JsonResponseException("god.pig.and.group.type.not.support");
        }

        return true;
    }

    //-----------------???????????????-----------------------

    @RequestMapping(method = RequestMethod.GET, value = "boar")
    public Paging<SearchedPig> boarQuery(@RequestParam Long farmId,
                                         @RequestParam(required = false) Integer pageNo,
                                         @RequestParam(required = false) Integer pageSize) {
        Map<String, Object> params = new HashMap<>();
        params.put("farmId", farmId);
        params.put("status", PigStatus.BOAR_ENTRY.getKey());
        return RespHelper.or500(doctorPigReadService.pagingPig(params, pageNo, pageSize));
    }

    @RequestMapping(method = RequestMethod.GET, value = "vaccine")
    public List<DoctorBasicMaterial> material(@RequestParam Long farmId) {
        return RespHelper.or500(doctorBasicMaterialReadService.findBasicMaterialsOwned(farmId, 3L, null));
    }

    @RequestMapping(method = RequestMethod.GET, value = "changeReason")
    public List<DoctorChangeReason> changeReason(@RequestParam Long farmId,
                                                 @RequestParam Long changeTypeId) {
        return RespHelper.or500(doctorBasicReadService.findChangeReasonByChangeTypeIdAndSrmFilterByFarmId(farmId, changeTypeId, null));
    }

    @RequestMapping(method = RequestMethod.GET, value = "customer")
    public List<DoctorCustomer> customer(@RequestParam("farmId") Long farmId) {
        return RespHelper.or500(doctorBasicReadService.findCustomersByFarmId(farmId));
    }


    //-----------------???????????????-----------------------

    public Paging<DoctorPigEvent> queryPigEventsByCriteria(@RequestParam Map<String, Object> params, @RequestParam(required = false) Integer pageNo, @RequestParam(required = false) Integer pageSize) {
        if (params == null || params.isEmpty()) {
            return Paging.empty();
        }
        params = Params.filterNullOrEmpty(params);
        if (params.get("eventTypes") != null) {
            params.put("types", Splitters.COMMA.splitToList((String) params.get("eventTypes")));
            params.remove("eventTypes");
        }

        if (StringUtils.isNotBlank((String) params.get("barnTypes"))) {
            params.put("barnTypes", Splitters.UNDERSCORE.splitToList((String) params.get("barnTypes")));
        }

        if (StringUtils.isNotBlank((String) params.get("pigCode"))) {
            params.put("pigCodeFuzzy", params.get("pigCode"));
            params.remove("pigCode");
        }

        Response<Paging<DoctorPigEvent>> pigEventPagingResponse = doctorPigEventReadService.queryPigEventsByCriteria(params, pageNo, pageSize);
        if (!pigEventPagingResponse.isSuccess()) {
            return Paging.empty();
        }
        transFromUtil.transFromExtraMap(pigEventPagingResponse.getResult().getData());
        return pigEventPagingResponse.getResult();
    }


    private Paging<DoctorGroupEventDetail> queryGroupEventsByCriteria(Map<String, Object> params, Integer pageNo, Integer pageSize) {
        if (params == null || params.isEmpty()) {
            return Paging.empty();
        }
        params = Params.filterNullOrEmpty(params);
        if (params.get("eventTypes") != null) {
            params.put("types", Splitters.COMMA.splitToList((String) params.get("eventTypes")));
            params.remove("eventTypes");
        }

        if (StringUtils.isNotBlank((String) params.get("pigTypes"))) {
            params.put("pigTypes", Splitters.UNDERSCORE.splitToList((String) params.get("pigTypes")));
        }

        if (StringUtils.isNotBlank((String) params.get("changeTypeIds"))) {
            params.put("changeTypeIds", Splitters.UNDERSCORE.splitToList((String) params.get("changeTypeIds")));
        }
        if (StringUtils.isNotBlank((String) params.get("groupCode"))) {
            params.put("groupCodeFuzzy", params.get("groupCode"));
            params.remove("groupCode");
        }
        Response<Paging<DoctorGroupEvent>> pagingResponse = doctorGroupReadService.queryGroupEventsByCriteria(params, pageNo, pageSize);
        if (!pagingResponse.isSuccess()) {
            return Paging.empty();
        }
        List<DoctorGroupEventDetail> groupEventDetailList = pagingResponse.getResult().getData().stream()
                .map(doctorGroupEvent -> {
                    DoctorGroupEventDetail detail = OBJECT_MAPPER.convertValue(doctorGroupEvent, DoctorGroupEventDetail.class);
                    Boolean isRollback = false;
                    Response<Boolean> booleanResponse = doctorGroupReadService.eventCanRollback(doctorGroupEvent.getId());
                    if (booleanResponse.isSuccess()) {
                        isRollback = booleanResponse.getResult();
                    }
                    detail.setIsRollback(isRollback);
                    return detail;
                }).collect(toList());
        return new Paging<>(pagingResponse.getResult().getTotal(), groupEventDetailList);
    }


    @Subscribe
    public void refreshReport(RefreshReportEvent event) {
        log.info("start to refresh report,type:[{}],farm:[{}],startDate:[{}],endDate:[{}]", event.getType(), event.getFarmId(), event.getStartDate(), event.getEndDate());
        if (2 == event.getType().intValue()) {
            doctorDailyGroupWriteService.createDailyGroupsByDateRange(event.getFarmId(), event.getStartDate(), event.getEndDate());
        } else {
            doctorDailyReportWriteService.createDailyReports(event.getFarmId(), event.getStartDate(), event.getEndDate());
        }
        //???????????????
        Integer index = DateUtil.getDeltaMonthsAbs(event.getStartDate(), new Date()) + 1;
        DateUtil.getBeforeMonthEnds(DateTime.now().toDate(), MoreObjects.firstNonNull(index, 12))
                .forEach(date -> doctorRangeReportWriteService.flushDoctorRangeReports(event.getFarmId(), date));
        DateUtil.getBeforeMonthEnds(new Date(), MoreObjects.firstNonNull(index, 12))
                .forEach(date -> doctorParityMonthlyReportWriteService.createMonthlyReport(event.getFarmId(), date));
        DateUtil.getBeforeMonthEnds(new Date(), MoreObjects.firstNonNull(index, 12))
                .forEach(date -> doctorBoarMonthlyReportWriteService.createMonthlyReport(event.getFarmId(), date));
    }


    @Data
    public static class RefreshReportEvent {
        private Integer type;
        private Long farmId;
        private Date startDate;
        private Date endDate;
    }

}
