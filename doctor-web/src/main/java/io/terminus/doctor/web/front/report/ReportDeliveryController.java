package io.terminus.doctor.web.front.report;

import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.doctor.event.service.DoctorDeliveryReadService;
import io.terminus.doctor.web.core.export.Exporter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Date;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/doctor/report/")
public class ReportDeliveryController {
    @RpcConsumer
    private DoctorDeliveryReadService doctorDeliveryReadService;

    @Autowired
    private Exporter exporter;

    @RequestMapping(method = RequestMethod.GET, value = "delivery")
    public Map<String,Object> deliveryReport(@RequestParam(required = true) Long farmId,
                                                   @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date beginDate,
                                                   @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date endDate,
                                                   @RequestParam(required = false) String pigCode,
                                                   @RequestParam(required = false) String operatorName,
                                                   @RequestParam(required = false) int isdelivery) {
        return doctorDeliveryReadService.getMating(farmId,beginDate,endDate,pigCode,operatorName,isdelivery);
    }
    //??????excel
    @RequestMapping(method = RequestMethod.GET, value = "delivery/export")
    public void deliveryReports(@RequestParam(required = true) Long farmId,
                                @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date beginDate,
                                @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date endDate,
                                @RequestParam(required = false) String pigCode,
                                @RequestParam(required = false) String operatorName,
                                @RequestParam(required = false) int isdelivery,
                                HttpServletRequest request, HttpServletResponse response) {
        Map<String,Object> map = doctorDeliveryReadService.getMating(farmId,beginDate,endDate,pigCode,operatorName,isdelivery);
        List<Map<String,Object>> list = (List<Map<String,Object>>) map.get("data");
        //????????????
        try  {
            //????????????
            exporter.setHttpServletResponse(request,  response,"?????????????????????");
            try  (XSSFWorkbook workbook  =  new  XSSFWorkbook())  {
                //???
                Sheet sheet  =  workbook.createSheet();
                sheet.addMergedRegion(new CellRangeAddress(0,0,0,20));
                Row count = sheet.createRow(0);
                count.createCell(0).setCellValue("????????????:"+String.valueOf(map.get("matingcount"))
                        +"  ?????????/?????????:"+String.valueOf(map.get("deliverycount"))+"/"+String.valueOf(map.get("deliveryrate"))
                        +"  ?????????/?????????:"+String.valueOf(map.get("fqcount"))+"/"+String.valueOf(map.get("fqrate"))
                        +"  ?????????/?????????:"+String.valueOf(map.get("lccount"))+"/"+String.valueOf(map.get("lcrate"))
                        +"  ?????????/?????????:"+String.valueOf(map.get("yxcount"))+"/"+String.valueOf(map.get("yxrate"))
                        +"  ?????????/?????????:"+String.valueOf(map.get("swcount"))+"/"+String.valueOf(map.get("swrate"))
                        +"  ?????????/?????????:"+String.valueOf(map.get("ttcount"))+"/"+String.valueOf(map.get("ttrate"))
                );

                Row title  =  sheet.createRow(1);
                int  pos  =  2;

                title.createCell(0).setCellValue("??????");
                title.createCell(1).setCellValue("?????????");
                title.createCell(2).setCellValue("????????????");
                title.createCell(3).setCellValue("????????????");
                title.createCell(4).setCellValue("????????????");
                title.createCell(5).setCellValue("????????????");
                title.createCell(6).setCellValue("????????????");
                title.createCell(7).setCellValue("????????????");
                title.createCell(8).setCellValue("?????????");
                title.createCell(9).setCellValue("????????????");
                title.createCell(10).setCellValue("????????????");
                title.createCell(11).setCellValue("????????????");
                title.createCell(12).setCellValue("????????????");
                title.createCell(13).setCellValue("??????????????????");
                title.createCell(14).setCellValue("????????????");

                for(int i = 0;i<list.size();i++) {
                    Map a = list.get(i);
                    Row row = sheet.createRow(pos++);
                    row.createCell(0).setCellValue(String.valueOf(i+1));
                    row.createCell(1).setCellValue(String.valueOf(a.get("pig_code")));
                    row.createCell(2).setCellValue(String.valueOf(a.get("current_barn_name")));
                    row.createCell(3).setCellValue(String.valueOf(a.get("barn_name")));
                    row.createCell(4).setCellValue(String.valueOf(a.get("pig_status")));
                    row.createCell(5).setCellValue(String.valueOf(a.get("event_at")));
                    row.createCell(6).setCellValue(String.valueOf(a.get("current_mating_count")));
                    row.createCell(7).setCellValue(String.valueOf(a.get("boar_code")));
                    row.createCell(8).setCellValue(String.valueOf(a.get("operator_name")));
                    row.createCell(9).setCellValue(String.valueOf(a.get("deliveryFarm")));
                    row.createCell(10).setCellValue(String.valueOf(a.get("deliveryBarn")));
                    row.createCell(11).setCellValue(String.valueOf(a.get("judge_preg_date")));
                    row.createCell(12).setCellValue(String.valueOf(a.get("deliveryDate")));
                    String check_event_at = "";
                    String leave_event_at = "";
                    if (!String.valueOf(a.get("check_event_at")).equals("") && !String.valueOf(a.get("notdelivery")).equals("??????")) {
                        check_event_at = "("+String.valueOf(a.get("check_event_at"))+")";
                    }
                    if (!String.valueOf(a.get("leave_event_at")).equals("")) {
                        leave_event_at = "("+String.valueOf(a.get("leave_event_at"))+")";
                    }
                    row.createCell(13).setCellValue(String.valueOf(a.get("notdelivery"))+check_event_at);
                    row.createCell(14).setCellValue(String.valueOf(a.get("deadorescape"))+leave_event_at);
                }
                workbook.write(response.getOutputStream());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * ??????????????????
     * @param farmId ??????id
     * @param time  ????????????
     * @param pigCode   ??????
     * @param operatorName  ?????????
     * @param barnId    ??????id
     * @param breedId     ??????
     * @param parity    ??????
     * @param pigStatus ?????????
     * @param beginInFarmTime ????????????
     * @return
     */
    @RequestMapping(method = RequestMethod.GET, value = "sows")
    public List<Map<String,Object>> sowsReport(@RequestParam(required = true) Long farmId,
                                               @RequestParam(required = true) @DateTimeFormat(pattern = "yyyy-MM-dd") Date time,
                                               @RequestParam(required = false) String pigCode,
                                               @RequestParam(required = false) String operatorName,
                                               @RequestParam(required = false) Long barnId,
                                               @RequestParam(required = false) Integer breedId,
                                               @RequestParam(required = false) Integer parity,
                                               @RequestParam(required = false) Integer pigStatus,
                                               @RequestParam(required = false) Integer sowsStatus,
                                               @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date beginInFarmTime,
                                               @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date endInFarmTime) {
        return doctorDeliveryReadService.sowsReport(farmId,time,pigCode,operatorName,barnId,breedId,parity,pigStatus,beginInFarmTime,endInFarmTime,sowsStatus);
    }

    /**
     * ????????????????????????
     */
    @RequestMapping(value = "boarReport", method = RequestMethod.GET)
    public List<Map<String,Object>> listBoarReport(@RequestParam Long farmId,
                                                   @RequestParam (required = true) @DateTimeFormat(pattern = "yyyy-MM-dd") Date queryDate,
                                                   @RequestParam (required = false) String staffName,
                                                   @RequestParam (required = false) String pigCode,
                                                   @RequestParam (required = false) Integer barnId,
                                                   @RequestParam (required = false) Integer breedId,
                                                   @RequestParam (required = false) Integer pigType,
                                                   @RequestParam (required = false) Integer boarsStatus,
                                                   @RequestParam (required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date beginDate,
                                                   @RequestParam (required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date endDate){
//        if(null != beginDate && null != endDate && beginDate.after(endDate))
//            throw new JsonResponseException("start.date.after.end.date");
        return doctorDeliveryReadService.boarReport(farmId,pigType,boarsStatus,queryDate,pigCode,staffName,barnId,breedId,beginDate,endDate);

    }

    //????????????????????????EXCEL
    @RequestMapping(method = RequestMethod.GET, value = "sows/export")
    public void sowsReports(@RequestParam(required = true) Long farmId,
                            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date time,
                            @RequestParam(required = false) String pigCode,
                            @RequestParam(required = false) String operatorName,
                            @RequestParam(required = false) Long barnId,
                            @RequestParam(required = false) Integer breedId,
                            @RequestParam(required = false) Integer parity,
                            @RequestParam(required = false) Integer pigStatus,
                            @RequestParam(required = false) Integer sowsStatus,
                            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date beginInFarmTime,
                            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date endInFarmTime,
                            HttpServletRequest request, HttpServletResponse response) {
        List<Map<String,Object>> ls=doctorDeliveryReadService.sowsReport(farmId,time,pigCode,operatorName,barnId,breedId,parity,pigStatus,beginInFarmTime,endInFarmTime,sowsStatus);

        //????????????
        try  {
            //????????????
            exporter.setHttpServletResponse(request,  response,"??????????????????");
            try  (XSSFWorkbook workbook  =  new  XSSFWorkbook())  {
                //???
                Sheet sheet  =  workbook.createSheet();
                sheet.addMergedRegion(new CellRangeAddress(0,0,0,20));
                Row count = sheet.createRow(0);
                Row title  =  sheet.createRow(1);
                int  pos  =  2;
                title.createCell(0).setCellValue("??????");
//                title.createCell(1).setCellValue("??????");
//                title.createCell(2).setCellValue("??????");
//                title.createCell(3).setCellValue("??????");
//                title.createCell(4).setCellValue("????????????");
//                title.createCell(5).setCellValue("??????");
//                title.createCell(6).setCellValue("?????????");
//                title.createCell(7).setCellValue("?????????");
//                title.createCell(8).setCellValue("??????");
//                title.createCell(9).setCellValue("????????????");
//                title.createCell(10).setCellValue("????????????");
                title.createCell(1).setCellValue("??????");
                title.createCell(2).setCellValue("??????");
                title.createCell(3).setCellValue("??????");
                title.createCell(4).setCellValue("??????");
                title.createCell(5).setCellValue("????????????");
                title.createCell(6).setCellValue("?????????");
                title.createCell(7).setCellValue("?????????");
                title.createCell(8).setCellValue("??????");
                title.createCell(9).setCellValue("????????????");
                title.createCell(10).setCellValue("????????????");
                for(int i = 0;i<ls.size();i++) {
                    Map a = ls.get(i);
                    Row row = sheet.createRow(pos++);
                    row.createCell(0).setCellValue(String.valueOf(i+1));
                    row.createCell(1).setCellValue(String.valueOf(a.get("pig_code")));
                    String rfid=String.valueOf(a.get("current_barn_name"));
                    if(rfid.equals("null")){
                        rfid="";
                    }
                    row.createCell(2).setCellValue(String.valueOf(rfid));
                    row.createCell(3).setCellValue(String.valueOf(a.get("breed_name")));
                    row.createCell(4).setCellValue(String.valueOf(a.get("parity")));
                    row.createCell(5).setCellValue(String.valueOf(a.get("status")));
                    row.createCell(6).setCellValue(String.valueOf(a.get("staff_name")));
                    row.createCell(7).setCellValue(String.valueOf(a.get("daizaishu")));
                    row.createCell(8).setCellValue(String.valueOf(a.get("source")));
                    String str = String.valueOf(a.get("in_farm_date"));
                    if("null".equals(str)){
                        row.createCell(9).setCellValue("");
                    }else {
                        String[] strs = str.split(" ");
                        row.createCell(9).setCellValue(String.valueOf(strs[0]));
                    }
                    String bd=String.valueOf(a.get("birth_date"));
                    if("null".equals(bd)){
                        row.createCell(10).setCellValue(" ");
                    }else {
                        String str1 = String.valueOf(a.get("birth_date"));
                        String[] strs1=str1.split(" ");
                        row.createCell(10).setCellValue(String.valueOf(strs1[0]));
                    }
                }
                workbook.write(response.getOutputStream());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    //????????????????????????EXCEL
    @RequestMapping(method = RequestMethod.GET, value = "boars/export")
    public void boarsReports(@RequestParam Long farmId,
                             @RequestParam (required = true) @DateTimeFormat(pattern = "yyyy-MM-dd") Date queryDate,
                             @RequestParam (required = false) String staffName,
                             @RequestParam (required = false) String pigCode,
                             @RequestParam (required = false) Integer barnId,
                             @RequestParam (required = false) Integer breedId,
                             @RequestParam (required = false) Integer pigType,
                             @RequestParam (required = false) Integer boarsStatus,
                             @RequestParam (required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date beginDate,
                             @RequestParam (required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date endDate,
                            HttpServletRequest request, HttpServletResponse response) {
        List<Map<String,Object>> ls=doctorDeliveryReadService.boarReport(farmId,pigType,boarsStatus,queryDate,pigCode,staffName,barnId,breedId,beginDate,endDate);

        //????????????
        try  {
            //????????????
            exporter.setHttpServletResponse(request,  response,"??????????????????");
            try  (XSSFWorkbook workbook  =  new  XSSFWorkbook())  {
                //???
                Sheet sheet  =  workbook.createSheet();
                sheet.addMergedRegion(new CellRangeAddress(0,0,0,20));
                Row count = sheet.createRow(0);
                Row title  =  sheet.createRow(1);
                int  pos  =  2;
                title.createCell(0).setCellValue("??????");
                title.createCell(1).setCellValue("??????");
                title.createCell(2).setCellValue("??????");
                title.createCell(3).setCellValue("??????");
                title.createCell(4).setCellValue("????????????");
                title.createCell(5).setCellValue("?????????");
                title.createCell(6).setCellValue("??????");
                title.createCell(7).setCellValue("????????????");
                title.createCell(8).setCellValue("????????????");
                title.createCell(9).setCellValue("????????????");
                for(int i = 0;i<ls.size();i++) {
                    Map a = ls.get(i);
                    Row row = sheet.createRow(pos++);
                    row.createCell(0).setCellValue(String.valueOf(i+1));
                    row.createCell(1).setCellValue(String.valueOf(a.get("pig_code")));
                    String rfid=String.valueOf(a.get("current_barn_name"));
                    if(rfid.equals("null")){
                        rfid="";
                    }
                    row.createCell(2).setCellValue(String.valueOf(rfid));
                    row.createCell(3).setCellValue(String.valueOf(a.get("breed_name")));
                    row.createCell(4).setCellValue(String.valueOf(a.get("status")));
                    row.createCell(5).setCellValue(String.valueOf(a.get("staff_name")));
                    row.createCell(6).setCellValue(String.valueOf(a.get("source")));
                    String str = String.valueOf(a.get("in_farm_date"));
                    if("null".equals(str)){
                        row.createCell(7).setCellValue("");
                    }else {
                        String[] strs = str.split(" ");
                        row.createCell(7).setCellValue(String.valueOf(strs[0]));
                    }
                    String bd=String.valueOf(a.get("birth_date"));
                    if("null".equals(bd)){
                        row.createCell(8).setCellValue(" ");
                    }else {
                        String str1 = String.valueOf(a.get("birth_date"));
                        String[] strs1=str1.split(" ");
                        row.createCell(8).setCellValue(String.valueOf(strs1[0]));
                    }
                    row.createCell(9).setCellValue(String.valueOf(a.get("boar_type")));
                }
                workbook.write(response.getOutputStream());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     *??????????????????
     * @param farmId
     * @param time
     * @param groupCode
     * @param operatorName
     * @param barn
     * @param groupType
     * @param groupStatus
     * @param buildBeginGroupTime
     * @param buildEndGroupTime
     * @return
     */
    @RequestMapping(method = RequestMethod.GET, value = "group")
    public Map<String,Object> groupReport(@RequestParam(required = true) Long farmId,
                                               @RequestParam(required = true) @DateTimeFormat(pattern = "yyyy-MM-dd") Date time,
                                               @RequestParam(required = false) String groupCode,
                                               @RequestParam(required = false) String operatorName,
                                               @RequestParam(required = false) Long barn,
                                                @RequestParam(required = false) Integer groupType,
                                               @RequestParam(required = false) Integer groupStatus,
                                               @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date buildBeginGroupTime,
                                                @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date buildEndGroupTime,
                                                @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date closeBeginGroupTime,
                                                @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date closeEndGroupTime) {
        return doctorDeliveryReadService.groupReport(farmId,time,groupCode,operatorName,barn,groupType,groupStatus,buildBeginGroupTime,buildEndGroupTime,closeBeginGroupTime,closeEndGroupTime);
    }

    /**
     * ??????????????????
     * @param farmId
     * @param operatorName
     * @param pigType
     * @param beginTime
     * @param endTime
     * @return
     */
    @RequestMapping(method = RequestMethod.GET, value = "barns")
    public List<Map<String,Object>> barnsReport(@RequestParam(required = true) Long farmId,
                                               @RequestParam(required = false) String operatorName,
                                               @RequestParam(required = false) String barnName,
                                                @RequestParam(required = false) Integer pigType,
                                               @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date beginTime,
                                               @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date endTime) {
        return doctorDeliveryReadService.barnsReport(farmId,operatorName,barnName,beginTime,endTime,pigType);
    }

    @RequestMapping(method = RequestMethod.GET, value = "group/export")
    public void groupReports(@RequestParam(required = true) Long farmId,
                                          @RequestParam(required = true) @DateTimeFormat(pattern = "yyyy-MM-dd") Date time,
                                          @RequestParam(required = false) String groupCode,
                                          @RequestParam(required = false) String operatorName,
                                          @RequestParam(required = false) Long barn,
                                          @RequestParam(required = false) Integer groupType,
                                          @RequestParam(required = false) Integer groupStatus,
                                          @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date buildBeginGroupTime,
                                          @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date buildEndGroupTime,
                                          @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date closeBeginGroupTime,
                                          @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date closeEndGroupTime,
                                          HttpServletRequest request, HttpServletResponse response){
        Map<String,Object> map1 =  doctorDeliveryReadService.groupReport(farmId,time,groupCode,operatorName,barn,groupType,groupStatus,buildBeginGroupTime,buildEndGroupTime,closeBeginGroupTime,closeEndGroupTime);
        List ls = (List)map1.get("data");
        //????????????
        try  {
            //????????????
            exporter.setHttpServletResponse(request,  response,"??????????????????");
            try  (XSSFWorkbook workbook  =  new  XSSFWorkbook())  {
                //???
                Sheet sheet  =  workbook.createSheet();
                sheet.addMergedRegion(new CellRangeAddress(0,0,0,20));
                Row count = sheet.createRow(0);
                Row title  =  sheet.createRow(1);
                int  pos  =  2;
                title.createCell(0).setCellValue("??????");
                title.createCell(1).setCellValue("?????????");
                title.createCell(2).setCellValue("??????");
                title.createCell(3).setCellValue("??????");
                title.createCell(4).setCellValue("????????????");
                title.createCell(5).setCellValue("????????????");
                title.createCell(6).setCellValue("????????????");
                title.createCell(7).setCellValue("????????????");
                title.createCell(8).setCellValue("?????????");
                title.createCell(9).setCellValue("????????????");
                title.createCell(10).setCellValue("????????????");
                for(int i = 0;i<ls.size();i++) {
                    Map map = (Map)ls.get(i);
                    Row row = sheet.createRow(pos++);
                    row.createCell(0).setCellValue(String.valueOf(i+1));
                    row.createCell(1).setCellValue(String.valueOf(map.get("group_code")));
                    String rfid=String.valueOf(map.get("current_barn_name"));
                    if(rfid.equals("null")){
                        rfid="";
                    }
                    row.createCell(2).setCellValue(String.valueOf(rfid));
                    row.createCell(3).setCellValue(String.valueOf(map.get("pig_type")));
                    row.createCell(4).setCellValue(String.valueOf(map.get("cunlanshu")));
                    row.createCell(5).setCellValue(String.valueOf(map.get("getAvgDayAge")));
                    row.createCell(6).setCellValue(String.valueOf(map.get("inAvgweight")));
                    row.createCell(7).setCellValue(String.valueOf(map.get("outAvgweight")));
                    row.createCell(8).setCellValue(String.valueOf(map.get("staff_name")));
                    String str = String.valueOf(map.get("build_event_at"));
                    if("null".equals(str)){
                        row.createCell(9).setCellValue("");
                    }else {
                        String[] strs = str.split(" ");
                        row.createCell(9).setCellValue(String.valueOf(strs[0]));
                    }
                    String bd=String.valueOf(map.get("close_event_at"));
                    if("null".equals(bd)){
                        row.createCell(10).setCellValue(" ");
                    }else {
                        String str1 = String.valueOf(map.get("close_event_at"));
                        String[] strs1=str1.split(" ");
                        row.createCell(10).setCellValue(String.valueOf(strs1[0]));
                    }
                }
                workbook.write(response.getOutputStream());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    @RequestMapping(method = RequestMethod.GET, value = "barns/export")
    public void barnsReports(@RequestParam(required = true) Long farmId,
                                                @RequestParam(required = false) String operatorName,
                                                @RequestParam(required = false) String barnName,
                                                @RequestParam(required = false) Integer pigType,
                                                @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date beginTime,
                                                @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date endTime,
                                                 HttpServletRequest request, HttpServletResponse response ) {
        List<Map<String,Object>> ls = doctorDeliveryReadService.barnsReport(farmId,operatorName,barnName,beginTime,endTime,pigType);
        //????????????
        try  {
            //????????????
            exporter.setHttpServletResponse(request,  response,"??????????????????");
            try  (XSSFWorkbook workbook  =  new  XSSFWorkbook())  {
                //???
                Sheet sheet  =  workbook.createSheet();
                sheet.addMergedRegion(new CellRangeAddress(0,0,0,20));
                Row count = sheet.createRow(0);
                Row title  =  sheet.createRow(1);
                int  pos  =  2;
                title.createCell(0).setCellValue("??????");
                title.createCell(1).setCellValue("??????");
                title.createCell(2).setCellValue("??????");
                title.createCell(3).setCellValue("?????????");
                title.createCell(4).setCellValue("????????????");
                title.createCell(5).setCellValue("????????????");
                title.createCell(6).setCellValue("??????");
                title.createCell(7).setCellValue("??????");
                title.createCell(8).setCellValue("??????");
                title.createCell(9).setCellValue("??????");
                title.createCell(10).setCellValue("????????????");
                title.createCell(11).setCellValue("????????????");
                for(int i = 0;i<ls.size();i++) {
                    Map map = ls.get(i);
                    Row row = sheet.createRow(pos++);
                    row.createCell(0).setCellValue(String.valueOf(i+1));
                    String rfid=String.valueOf(map.get("name"));
                    if(rfid.equals("null")){
                        rfid="";
                    }
                    row.createCell(1).setCellValue(String.valueOf(rfid));
                    row.createCell(2).setCellValue(String.valueOf(map.get("pig_type")));
                    row.createCell(3).setCellValue(String.valueOf(map.get("staff_name")));
                    row.createCell(4).setCellValue(String.valueOf(map.get("qichucunlan")));
                    row.createCell(5).setCellValue(String.valueOf(map.get("zhuanru")));
                    if(map.get("siwang") == null){
                        row.createCell(6).setCellValue("");
                    }else {
                        row.createCell(6).setCellValue(String.valueOf(map.get("siwang")));
                    }
                    if(map.get("taotai") == null){
                        row.createCell(7).setCellValue("");
                    }else {
                        row.createCell(7).setCellValue(String.valueOf(map.get("taotai")));
                    }
                    if(map.get("xiaoshou") == null){
                        row.createCell(8).setCellValue("");
                    }else {
                        row.createCell(8).setCellValue(String.valueOf(map.get("xiaoshou")));
                    }
                    //row.createCell(6).setCellValue(String.valueOf(map.get("taotai")));
                    //row.createCell(7).setCellValue(String.valueOf(map.get("xiaoshou")));
                    row.createCell(9).setCellValue(String.valueOf(map.get("zhuanchu")));
                    row.createCell(10).setCellValue(String.valueOf(map.get("qitajianshao")));
                    row.createCell(11).setCellValue(String.valueOf(map.get("qimucunlan")));
                }
                workbook.write(response.getOutputStream());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}