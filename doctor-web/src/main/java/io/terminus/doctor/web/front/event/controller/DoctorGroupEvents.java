package io.terminus.doctor.web.front.event.controller;

import com.google.common.base.MoreObjects;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import io.terminus.common.utils.Arguments;
import io.terminus.common.utils.JsonMapper;
import io.terminus.common.utils.Splitters;
import io.terminus.doctor.basic.model.DoctorBasic;
import io.terminus.doctor.basic.service.DoctorBasicReadService;
import io.terminus.doctor.common.enums.PigType;
import io.terminus.doctor.common.utils.Params;
import io.terminus.doctor.common.utils.RespHelper;
import io.terminus.doctor.common.utils.RespWithExHelper;
import io.terminus.doctor.event.dto.DoctorGroupDetail;
import io.terminus.doctor.event.dto.event.group.input.DoctorNewGroupInput;
import io.terminus.doctor.event.dto.event.group.input.DoctorTransGroupInput;
import io.terminus.doctor.event.enums.GroupEventType;
import io.terminus.doctor.event.enums.IsOrNot;
import io.terminus.doctor.event.model.DoctorGroup;
import io.terminus.doctor.event.model.DoctorGroupEvent;
import io.terminus.doctor.event.model.DoctorPigTrack;
import io.terminus.doctor.event.service.DoctorEventModifyRequestReadService;
import io.terminus.doctor.event.service.DoctorEventModifyRequestWriteService;
import io.terminus.doctor.event.service.DoctorGroupReadService;
import io.terminus.doctor.event.service.DoctorGroupWriteService;
import io.terminus.doctor.event.service.DoctorPigReadService;
import io.terminus.doctor.web.core.export.Exporter;
import io.terminus.doctor.web.front.auth.DoctorFarmAuthCenter;
import io.terminus.doctor.web.front.event.dto.DoctorBatchGroupEventDto;
import io.terminus.doctor.web.front.event.dto.DoctorBatchNewGroupEventDto;
import io.terminus.doctor.web.front.event.dto.DoctorGroupDetailEventsDto;
import io.terminus.doctor.web.front.event.service.DoctorGroupWebService;
import io.terminus.doctor.web.util.TransFromUtil;
import io.terminus.pampas.common.UserUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Desc:
 * Mail: yangzl@terminus.io
 * author: DreamYoung
 * Date: 16/5/31
 */
@Slf4j
@RestController
@RequestMapping("/api/doctor/events/group")
public class DoctorGroupEvents {

    private final DoctorGroupWebService doctorGroupWebService;
    private final DoctorGroupReadService doctorGroupReadService;
    private final DoctorFarmAuthCenter doctorFarmAuthCenter;
    private final DoctorGroupWriteService doctorGroupWriteService;
    private final DoctorBasicReadService doctorBasicReadService;
    private final TransFromUtil transFromUtil;

    @RpcConsumer
    private DoctorPigReadService doctorPigReadService;
    @RpcConsumer
    private DoctorEventModifyRequestWriteService doctorEventModifyRequestWriteService;
    @RpcConsumer
    private DoctorEventModifyRequestReadService doctorEventModifyRequestReadService;

    @Autowired
    private Exporter exporter;

    @Autowired
    public DoctorGroupEvents(DoctorGroupWebService doctorGroupWebService,
                             DoctorGroupReadService doctorGroupReadService,
                             DoctorFarmAuthCenter doctorFarmAuthCenter,
                             DoctorGroupWriteService doctorGroupWriteService,
                             DoctorBasicReadService doctorBasicReadService, TransFromUtil transFromUtil
                             ) {
        this.doctorGroupWebService = doctorGroupWebService;
        this.doctorGroupReadService = doctorGroupReadService;
        this.doctorFarmAuthCenter = doctorFarmAuthCenter;
        this.doctorGroupWriteService = doctorGroupWriteService;
        this.doctorBasicReadService = doctorBasicReadService;
        this.transFromUtil = transFromUtil;
    }

    /**
     * У����Ⱥ���Ƿ��ظ�
     *
     * @param farmId    ��id
     * @param groupCode ��Ⱥ��
     * @return true �ظ�, false ���ظ�
     */
    @RequestMapping(value = "/check/groupCode", method = RequestMethod.GET)
    public Boolean checkGroupRepeat(@RequestParam("farmId") Long farmId,
                                    @RequestParam("groupCode") String groupCode) {
        return RespHelper.or500(doctorGroupReadService.checkGroupRepeat(farmId, groupCode));
    }

    /**
     * �½���Ⱥ
     *
     * @return ��Ⱥid
     */
    @RequestMapping(value = "/new", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public Long createNewGroup(@RequestBody DoctorNewGroupInput newGroupDto) {
        return RespWithExHelper.orInvalid(doctorGroupWebService.createNewGroup(newGroupDto));
    }

    /**
     * �����½���Ⱥ
     *
     * @param batchNewGroupEventDto �����½���Ϣ
     * @return
     */
    @RequestMapping(value = "/batchNew", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public Boolean batchCreateNewGroup(@RequestBody DoctorBatchNewGroupEventDto batchNewGroupEventDto) {
        return RespWithExHelper.orInvalid(doctorGroupWebService.batchNewGroupEvent(batchNewGroupEventDto));
    }

    /**
     * ¼����Ⱥ�¼�
     *
     * @param groupId   ��Ⱥid
     * @param eventType �¼�����
     * @param data      ���
     * @return �Ƿ�ɹ�
     * @see io.terminus.doctor.event.enums.GroupEventType
     * @see io.terminus.doctor.event.dto.event.group.input.BaseGroupInput
     */
    @RequestMapping(value = "/other", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public Boolean createGroupEvent(@RequestParam("groupId") Long groupId,
                                    @RequestParam("eventType") Integer eventType,
                                    @RequestParam("data") String data) {
        return RespWithExHelper.orInvalid(doctorGroupWebService.createGroupEvent(groupId, eventType, data));
    }

    /**
     * ������Ⱥ�༭����
     * @param groupId ��Ⱥid
     * @param eventType �¼�����
     * @param eventId �¼�id
     * @param data ��������
     * @return
     */
    @RequestMapping(value = "/createGroupModifyRequest", method = RequestMethod.POST)
    public void createGroupModifyEventRequest(@RequestParam("groupId") Long groupId,
                                                 @RequestParam("eventType") Integer eventType,
                                              @RequestParam("eventId") Long eventId,
                                              @RequestParam("data") String data) {
        RespWithExHelper.orInvalid(doctorGroupWebService.createGroupModifyEventRequest(groupId, eventType, eventId, data));
    }

    /**
     * ������Ⱥ�¼�
     *
     * @param batchGroupEventDto �����¼������װ
     * @return �Ƿ�ɹ�
     */
    @RequestMapping(value = "/batchOther", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public Boolean batchCreateGroupEvent(@RequestBody DoctorBatchGroupEventDto batchGroupEventDto) {
        return RespWithExHelper.orInvalid(doctorGroupWebService.batchGroupEvent(batchGroupEventDto));
    }

    /**
     * ������Ⱥid��ѯ���Բ������¼�����
     *
     * @param groupIds ��Ⱥids
     * @return �¼�����s
     * @see io.terminus.doctor.event.enums.GroupEventType
     */
    @RequestMapping(value = "/types", method = RequestMethod.POST)
    public List<Integer> findEventTypesByGroupIds(@RequestParam("groupIds[]") Long[] groupIds) {
        return RespHelper.or500(doctorGroupReadService.findEventTypesByGroupIds(Lists.newArrayList(groupIds)));
    }

    /**
     * ������Ⱥ�� ������(yyyy-MM-dd)
     *
     * @param barnName ��������
     * @return ��Ⱥ��
     */
    @RequestMapping(value = "/code", method = RequestMethod.GET)
    public String generateGroupCode(@RequestParam(value = "barnName", required = false) String barnName) {
        return doctorGroupWebService.generateGroupCode(barnName).getResult();
    }

    /**
     * ����id������Ⱥ��(��Ҫ���ڷ�����: �����ǰ���������Ⱥֱ�ӷ��ش���Ⱥ��, ���������, ��������Ⱥ��
     *
     * @param pigId ��id
     * @return ��Ⱥ��
     */
    @RequestMapping(value = "/pigCode", method = RequestMethod.GET)
    public String generateGroupCodeByPigId(@RequestParam(value = "pigId", required = false) Long pigId, @RequestParam String eventAt) {
        if (pigId == null) {
            return null;
        }
        DoctorPigTrack pigTrack = RespHelper.or500(doctorPigReadService.findPigTrackByPigId(pigId));
        List<DoctorGroup> groupList = RespHelper.or500(doctorGroupReadService.findGroupByCurrentBarnId(pigTrack.getCurrentBarnId()));
        if (Arguments.isNullOrEmpty(groupList)) {
            return pigTrack.getCurrentBarnName() + "(" + eventAt + ")";
        }
        return groupList.get(0).getGroupCode();
    }

    /**
     * �����������Ƿ�����Ⱥ
     * @param pigId ��id
     * @return
     */
    @RequestMapping(value = "/has/group/{pigId}", method = RequestMethod.GET)
    public Boolean hasGroup(@PathVariable Long pigId) {
        DoctorPigTrack pigTrack = RespHelper.or500(doctorPigReadService.findPigTrackByPigId(pigId));
        List<DoctorGroup> groupList = RespHelper.or500(doctorGroupReadService.findGroupByCurrentBarnId(pigTrack.getCurrentBarnId()));
        if (Arguments.isNullOrEmpty(groupList)) {
            return Boolean.FALSE;
        }
        return Boolean.TRUE;
    }

    /**
     * ��ѯ��Ⱥ����
     *
     * @param groupId   ��Ⱥid
     * @param eventSize �¼���С
     * @return ��Ⱥ����
     */
    @RequestMapping(value = "/detail", method = RequestMethod.GET)
    public DoctorGroupDetailEventsDto findGroupDetailByGroupId(@RequestParam("groupId") Long groupId,
                                                               @RequestParam(value = "eventSize", required = false) Integer eventSize) {
        DoctorGroupDetail groupDetail = RespHelper.or500(doctorGroupReadService.findGroupDetailByGroupId(groupId));

        //��ѯ��Ⱥ���¼�, Ĭ��3��
        List<DoctorGroupEvent> groupEvents = RespHelper.or500(doctorGroupReadService.pagingGroupEventDelWean(
                groupDetail.getGroup().getFarmId(), groupId, null, null, MoreObjects.firstNonNull(eventSize, 3), null, null)).getData();

        Response<DoctorGroupEvent> response = doctorGroupReadService.findLastGroupEventByType(groupId, GroupEventType.LIVE_STOCK.getValue());
        Double avgWeight = 0.0;
        if (response.isSuccess() && response.getResult() != null) {
            avgWeight = response.getResult().getAvgWeight();
        }
        return new DoctorGroupDetailEventsDto(groupDetail.getGroup(), groupDetail.getGroupTrack()
                , transFromUtil.transFromGroupEvents(groupEvents), avgWeight);
    }

    /**
     * ��Ⱥ���鵼��
     * @param groupId
     * @param eventSize
     */
    @RequestMapping(value = "/detail/export", method = RequestMethod.GET)
    public void findGroupDetailByGroupIdExport(@RequestParam("groupId") Long groupId,
                                               @RequestParam(value = "eventSize", required = false) Integer eventSize,
                                               HttpServletRequest request, HttpServletResponse res){
        DoctorGroupDetail groupDetail = RespHelper.or500(doctorGroupReadService.findGroupDetailByGroupId(groupId));
        //����ƽ������
        Response<DoctorGroupEvent> response = doctorGroupReadService.findLastGroupEventByType(groupId, GroupEventType.LIVE_STOCK.getValue());
        Double avgWeight = 0.0;
        if (response.isSuccess() && response.getResult() != null) {
            avgWeight = response.getResult().getAvgWeight();
        }


        //��ʼ����
        try {
            //��������
            exporter.setHttpServletResponse(request,res,"��Ⱥ����");
            XSSFWorkbook workbook = new XSSFWorkbook();
            //��
            Sheet sheet = workbook.createSheet();
            sheet.createRow(0).createCell(5).setCellValue("��Ⱥ����");

            Row title = sheet.createRow(1);
//            int pos = 2;

            title.createCell(0).setCellValue("��Ⱥ��");
            title.createCell(1).setCellValue("��Ⱥ����");
            title.createCell(2).setCellValue("��");
            title.createCell(3).setCellValue("��ֻ��");
            title.createCell(4).setCellValue("ƽ������");
            title.createCell(5).setCellValue("ƽ������");
            title.createCell(6).setCellValue("��Ⱥ����");
            title.createCell(7).setCellValue("״̬");
            title.createCell(8).setCellValue("����Ա");

            Row row = sheet.createRow(3);
            row.createCell(0).setCellValue(String.valueOf(groupDetail.getGroup().getGroupCode()));

            //ö������
            String a=String.valueOf(groupDetail.getGroup().getPigType());
            if(a.equals(String.valueOf(PigType.NURSERY_PIGLET.getValue()))){
                row.createCell(1).setCellValue(String.valueOf(PigType.NURSERY_PIGLET.getDesc()));
            }
            if(a.equals(String.valueOf(PigType.FATTEN_PIG.getValue()))){
                row.createCell(1).setCellValue(String.valueOf(PigType.FATTEN_PIG.getDesc()));
            }
            if(a.equals(String.valueOf(PigType.RESERVE.getValue()))){
                row.createCell(1).setCellValue(String.valueOf(PigType.RESERVE.getDesc()));
            }
            if(a.equals(String.valueOf(PigType.MATE_SOW.getValue()))){
                row.createCell(1).setCellValue(String.valueOf(PigType.MATE_SOW.getDesc()));
            }
            if(a.equals(String.valueOf(PigType.PREG_SOW.getValue()))){
                row.createCell(1).setCellValue(String.valueOf(PigType.PREG_SOW.getDesc()));
            }
            if(a.equals(String.valueOf(PigType.DELIVER_SOW.getValue()))){
                row.createCell(1).setCellValue(String.valueOf(PigType.DELIVER_SOW.getDesc()));
            }
            if(a.equals(String.valueOf(PigType.BOAR.getValue()))){
                row.createCell(1).setCellValue(String.valueOf(PigType.BOAR.getDesc()));
            }

            row.createCell(2).setCellValue(String.valueOf(groupDetail.getGroup().getFarmName()));
            row.createCell(3).setCellValue(String.valueOf(groupDetail.getGroupTrack().getQuantity()));
            row.createCell(4).setCellValue(String.valueOf(groupDetail.getGroupTrack().getAvgDayAge()));
            row.createCell(5).setCellValue(String.valueOf(avgWeight+" ����"));

            //date���͵�תyyyy��MM��dd�ո�ʽ
            SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd-");
            String format = sdf.format(groupDetail.getGroup().getOpenAt());
            row.createCell(6).setCellValue(String.valueOf(format));

            //ö������
            String b=String.valueOf(groupDetail.getGroup().getStatus());
            if(b.equals(DoctorGroup.Status.CREATED.getValue())){
                row.createCell(7).setCellValue(DoctorGroup.Status.CREATED.getDesc());
            }
            if(b.equals(DoctorGroup.Status.CLOSED.getValue())){
                row.createCell(7).setCellValue(DoctorGroup.Status.CLOSED.getDesc());
            }

            row.createCell(8).setCellValue(String.valueOf(groupDetail.getGroup().getStaffName()));

            workbook.write(res.getOutputStream());

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    /**
     * ��ҳ��ѯ��Ⱥ��ʷ�¼�
     *
     * @param farmId  ��id
     * @param groupId ��Ⱥid
     * @param type    �¼�����
     * @param pageNo  ��ҳ��С
     * @param size    ��ǰҳ��
     * @return ��ҳ���
     */
    @RequestMapping(value = "/paging", method = RequestMethod.GET)
    public Paging<DoctorGroupEvent> pagingGroupEvent(@RequestParam("farmId") Long farmId,
                                                     @RequestParam(value = "groupId", required = false) Long groupId,
                                                     @RequestParam(value = "type", required = false) Integer type,
                                                     @RequestParam(value = "pageNo", required = false) Integer pageNo,
                                                     @RequestParam(value = "size", required = false) Integer size,
                                                     @RequestParam(value = "startDate", required = false) String startDate,
                                                     @RequestParam(value = "endDate", required = false) String endDate) {

        Paging<DoctorGroupEvent> doctorGroupEventPaging = RespHelper.or500(doctorGroupReadService.pagingGroupEvent(farmId, groupId, type, pageNo, size, startDate, endDate));
        transFromUtil.transFromGroupEvents(doctorGroupEventPaging.getData());
        return doctorGroupEventPaging;
    }

    @RequestMapping(value = "/pagingRollbackGroupEvent", method = RequestMethod.GET)
    public Paging<DoctorGroupEvent> pagingGroupEventWithCanRollback(@RequestParam("farmId") Long farmId,
                                                                     @RequestParam(value = "groupId", required = false) Long groupId,
                                                                     @RequestParam(value = "type", required = false) Integer type,
                                                                     @RequestParam(value = "pageNo", required = false) Integer pageNo,
                                                                     @RequestParam(value = "size", required = false) Integer size,
                                                                     @RequestParam(value = "startDate", required = false) String startDate,
                                                                     @RequestParam(value = "endDate", required = false) String endDate) {
        return pagingGroupEvent(farmId, groupId, type, pageNo, size, startDate, endDate);
    }

    /**
     * ��ѯ��Ⱥ�¼�����
     *
     * @param eventId �¼�id
     * @return ��Ⱥ�¼�
     */
    @RequestMapping(value = "/event", method = RequestMethod.GET)
    public DoctorGroupEvent findGroupEventById(@RequestParam("eventId") Long eventId) {
        return RespHelper.or500(doctorGroupReadService.findGroupEventById(eventId));
    }

    /**
     * ��ѯ�ѽ�Ⱥ����Ⱥ
     *
     * @param farmId ��id
     * @return ��Ⱥ
     */
    @RequestMapping(value = "/open", method = RequestMethod.GET)
    public List<DoctorGroup> findOpenGroupsByFarmId(@RequestParam("farmId") Long farmId) {
        return RespHelper.or500(doctorGroupReadService.findGroupsByFarmId(farmId)).stream()
                .filter(group -> Objects.equals(DoctorGroup.Status.CREATED.getValue(), group.getStatus()))
                .collect(Collectors.toList());
    }

    /**
     * ��������id��ѯ�ѽ�Ⱥ����Ⱥ
     *
     * @param barnId ����id
     * @return ��Ⱥ
     */
    @RequestMapping(value = "/open/barn", method = RequestMethod.GET)
    public List<DoctorGroup> findOpenGroupsByBarnId(@RequestParam(value = "barnId", required = false) Long barnId) {
        if (barnId == null) {
            return Lists.newArrayList();
        }
        return RespHelper.or500(doctorGroupReadService.findGroupByCurrentBarnId(barnId)).stream()
                .filter(group -> Objects.equals(DoctorGroup.Status.CREATED.getValue(), group.getStatus()))
                .collect(Collectors.toList());
    }

    /**
     * �ع���Ⱥ�¼�
     *
     * @param eventId �ع��¼���id
     * @return ��Ⱥ����
     */
    @RequestMapping(value = "/rollback", method = RequestMethod.GET)
    public Boolean rollbackGroupEvent(@RequestParam("eventId") Long eventId) {
        DoctorGroupEvent event = RespHelper.or500(doctorGroupReadService.findGroupEventById(eventId));

        //Ȩ������У��Ȩ��
        doctorFarmAuthCenter.checkFarmAuth(event.getFarmId());
        return RespHelper.or500(doctorGroupWriteService.rollbackGroupEvent(event, UserUtil.getUserId(), UserUtil.getCurrentUser().getName()));
    }

    /**
     * ��ѯ����ת���Ʒ��
     *
     * @param groupId ��Ⱥid
     * @return ��ת��Ʒ��
     */
    @RequestMapping(value = "/breeds", method = RequestMethod.GET)
    public List<DoctorBasic> findCanBreed(@RequestParam("groupId") Long groupId) {
        DoctorGroup group = RespHelper.or500(doctorGroupReadService.findGroupById(groupId));
        if (group.getBreedId() != null) {
            DoctorBasic breed = RespHelper.or500(doctorBasicReadService.findBasicById(group.getBreedId()));
            if (breed != null) {
                return Lists.newArrayList(breed);
            }
        }
        return RespHelper.or500(doctorBasicReadService.findValidBasicByTypeAndSrm(DoctorBasic.Type.BREED.getValue(), null));
    }

    /**
     * ������id����Ⱥ�Ų�ѯ��Ⱥ
     *
     * @param farmId    ��id
     * @param groupCode ��Ⱥ��
     * @return ��Ⱥ
     */
    @RequestMapping(value = "/farmGroupCode", method = RequestMethod.GET)
    public DoctorGroup findGroupByFarmIdAndGroupCode(@RequestParam("farmId") Long farmId,
                                                     @RequestParam("groupCode") String groupCode) {
        return RespHelper.or500(doctorGroupReadService.findGroupByFarmIdAndGroupCode(farmId, groupCode));
    }

    /**
     * ��ȡ��Ⱥ�¼������б�
     *
     * @return
     * @see GroupEventType
     */
    @RequestMapping(value = "/groupEvents")
    @ResponseBody
    public List<String> queryGroupEvents() {
        return Arrays.stream(GroupEventType.values()).map(GroupEventType::getDesc).collect(Collectors.toList());
    }

    /**
     * ��ҳ��ѯĳһ���͵���Ⱥ�¼�
     *
     * @param params
     * @param pageNo
     * @param pageSize
     * @return
     */
    @RequestMapping(value = "/groupPaging", method = RequestMethod.GET)
    @ResponseBody
    public Paging<DoctorGroupEvent> queryGroupEventsByCriteria(@RequestParam Map<String, Object> params, @RequestParam(required = false) Integer pageNo, @RequestParam(required = false) Integer pageSize) {
        if (params == null || params.isEmpty()) {
            return Paging.empty();
        }
        params = Params.filterNullOrEmpty(params);
        if (params.get("eventTypes") != null) {
            params.put("types", Splitters.COMMA.splitToList((String) params.get("eventTypes")));
            params.remove("eventTypes");
        }
        if (Objects.isNull(params.get("endDate"))) {
            params.put("endDate", new DateTime(params.get("endDate")).withTimeAtStartOfDay().plusSeconds(86399).toDate());
        }
        return RespHelper.or500(doctorGroupReadService.queryGroupEventsByCriteria(params, pageNo, pageSize));
    }

    /**
     * ��ȡ��Ⱥ��ʼ�¼�
     *
     * @param groupId ��Ⱥid
     * @return �½��¼�
     */
    @RequestMapping(value = "/find/newGroupEvent", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public DoctorGroupEvent findNewGroupEvent(@RequestParam Long groupId) {
        return RespHelper.or500(doctorGroupReadService.findInitGroupEvent(groupId));
    }

    /**
     * ��ȺתȺ�¼���extra�����groupid(��ʱ)
     *
     * @return
     */
    @RequestMapping(value = "/fix/groupExtra", method = RequestMethod.GET)
    public Boolean fixGroupEventExtra() {
        try {
            Map<String, Object> map = Maps.newHashMap();
            map.put("type", GroupEventType.NEW.getValue());
            map.put("isAuto", IsOrNot.YES.getValue());
            Paging<DoctorGroupEvent> paging = RespHelper.or500(doctorGroupReadService.queryGroupEventsByCriteria(map, 1, Integer.MAX_VALUE));
            if (paging.isEmpty()) {
                return Boolean.TRUE;
            }
            paging.getData().forEach(doctorGroupEvent -> {
                try {
                    DoctorGroupEvent relEvent = RespHelper.or500(doctorGroupReadService.findGroupEventById(doctorGroupEvent.getRelGroupEventId()));
                    if (Objects.equals(relEvent.getType(), GroupEventType.TRANS_GROUP.getValue())) {
                        DoctorTransGroupInput doctorTransGroupEvent = JsonMapper.JSON_NON_EMPTY_MAPPER.fromJson(relEvent.getExtra(), DoctorTransGroupInput.class);
                        doctorTransGroupEvent.setToGroupId(doctorGroupEvent.getGroupId());
                        relEvent.setExtraMap(doctorTransGroupEvent);
                        doctorGroupWriteService.updateGroupEvent(relEvent);
                    }
                } catch (Exception e) {
                    log.error("eventId {}", doctorGroupEvent.getId());
                }
            });
            return Boolean.TRUE;
        } catch (Exception e) {
            log.error("fix group event extra error, cause by {}", Throwables.getStackTraceAsString(e));
            return Boolean.FALSE;
        }
    }

    /**
     * ��������id��ѯ��
     * @param farmId
     * @param barnId
     * @return
     */
    @RequestMapping(value = "/barnId/group", method = RequestMethod.GET)
    public List<DoctorGroup> doctorGroupDetails(@RequestParam Long farmId,@RequestParam Long barnId) {
        return RespHelper.or500(doctorGroupReadService.findGroupId(farmId, barnId));
    }
}
