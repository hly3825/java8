package io.terminus.doctor.move.controller;

import com.google.common.base.Throwables;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.exception.ServiceException;
import io.terminus.doctor.common.enums.DataEventType;
import io.terminus.doctor.common.event.DataEvent;
import io.terminus.doctor.event.search.barn.BarnSearchDumpService;
import io.terminus.doctor.event.search.group.GroupDumpService;
import io.terminus.doctor.event.search.pig.PigDumpService;
import io.terminus.doctor.move.dto.DoctorImportSheet;
import io.terminus.doctor.move.service.DoctorImportDataService;
import io.terminus.zookeeper.pubsub.Subscriber;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

/**
 * Desc:
 * Mail: yangzl@terminus.io
 * author: DreamYoung
 * Date: 2016/10/19
 */
@Slf4j
@RestController
@RequestMapping("/api/doctor/import/data")
public class DoctorImportDataController {

    @Autowired
    private DoctorImportDataService doctorImportDataService;
    @Autowired
    private BarnSearchDumpService barnSearchDumpService;
    @Autowired
    private GroupDumpService groupDumpService;
    @Autowired
    private PigDumpService pigDumpService;
    @Autowired
    private Subscriber subscriber;

    @PostConstruct
    public void init () throws Exception{
        subscriber.subscribe(data -> {
            DataEvent dataEvent = DataEvent.fromBytes(data);
            if(dataEvent != null && dataEvent.getEventType().equals(DataEventType.ImportExcel.getKey())){
                log.warn("成功监听到导数事件, content={}", dataEvent.getContent());
                String fileURL = DataEvent.analyseContent(dataEvent, String.class);
                String fileType;
                if(fileURL.endsWith(".xlsx")){
                    fileType = "xlsx";
                }else if(fileURL.endsWith(".xls")){
                    fileType = "xls";
                }else{
                    throw new ServiceException("file.type.error");
                }
                try {
                    InputStream inputStream = new URL(fileURL).openConnection().getInputStream();
                    importByInputStream(inputStream, fileType);
                } catch (Exception e) {
                    log.error(Throwables.getStackTraceAsString(e));
                }
            }
        });
    }
    /**
     * 导入所有的猪场数据
     * @param path excel文件路径
     * @return 是否成功
     */
    @RequestMapping(value = "/all", method = RequestMethod.GET)
    public String importAll(@RequestParam("path") String path) {
        try {
            File file = new File(path);
            String fileType;
            if(file.getName().endsWith(".xlsx")){
                fileType = "xlsx";
            }else if(file.getName().endsWith(".xls")){
                fileType = "xls";
            }else{
                throw new ServiceException("file.type.error");
            }
            this.importByInputStream(new FileInputStream(file), fileType);

            return "true";
        } catch (ServiceException | JsonResponseException e) {
            log.error("import all excel failed, path:{}, cause:{}", path, Throwables.getStackTraceAsString(e));
            return e.getMessage();
        } catch (Exception e) {
            log.error("import all excel failed, path:{}, cause:{}", path, Throwables.getStackTraceAsString(e));
            return "false";
        }
    }

    private Sheet getSheet(Workbook wk, String name) {
        Sheet sheet = wk.getSheet(name);
        if (sheet == null) {
            throw new JsonResponseException("sheet.not.found");
        }
        return sheet;
    }

    private void importByInputStream(InputStream inputStream, String fileType) throws IOException {
        Workbook workbook;
        switch (fileType) {
            case "xlsx":
                workbook = new XSSFWorkbook(inputStream);  //2007
                break;
            case "xls":
                workbook = new HSSFWorkbook(inputStream);  //2003
                break;
            default:
                throw new ServiceException("file.type.error");
        }

        DoctorImportSheet sheet = new DoctorImportSheet();
        sheet.setFarm(getSheet(workbook, "猪场"));
        sheet.setStaff(getSheet(workbook, "员工"));
        sheet.setBarn(getSheet(workbook, "1.猪舍"));
        sheet.setBreed(getSheet(workbook, "2.品种"));
        sheet.setSow(getSheet(workbook, "3.母猪信息"));
        sheet.setBoar(getSheet(workbook, "4.公猪信息"));
        sheet.setGroup(getSheet(workbook, "5.商品猪（猪群）信息"));
        sheet.setWarehouse(getSheet(workbook, "6.仓库"));
        sheet.setMedicine(getSheet(workbook, "7.药品"));
        sheet.setVacc(getSheet(workbook, "8.疫苗"));
        sheet.setMaterial(getSheet(workbook, "9.原料"));
        sheet.setFeed(getSheet(workbook, "10.饲料"));
        sheet.setConsume(getSheet(workbook, "11.易耗品"));
        doctorImportDataService.importAll(sheet);

        log.warn("ElasticSearch dump start !");
        barnSearchDumpService.fullDump(null);
        groupDumpService.fullDump(null);
        pigDumpService.fullDump(null);
        log.warn("all data moved successfully, CONGRATULATIONS!!!");
    }
}
