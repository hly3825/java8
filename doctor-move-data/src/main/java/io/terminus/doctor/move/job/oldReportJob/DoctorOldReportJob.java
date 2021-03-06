package io.terminus.doctor.move.job.oldReportJob;

import com.google.common.base.Throwables;
import com.google.common.collect.Maps;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.model.Response;
import io.terminus.common.utils.Dates;
import io.terminus.doctor.common.utils.DateUtil;
import io.terminus.doctor.common.utils.RespHelper;
import io.terminus.doctor.event.service.DoctorDailyGroupWriteService;
import io.terminus.doctor.event.service.DoctorDailyReportReadService;
import io.terminus.doctor.event.service.DoctorDailyReportWriteService;
import io.terminus.doctor.event.service.DoctorRangeReportWriteService;
import io.terminus.doctor.user.model.DoctorFarm;
import io.terminus.doctor.user.model.DoctorOrg;
import io.terminus.doctor.user.service.DoctorDepartmentReadService;
import io.terminus.doctor.user.service.DoctorFarmReadService;
import io.terminus.doctor.user.service.DoctorOrgReadService;
import io.terminus.zookeeper.leader.HostLeader;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by xjn on 18/2/3.
 * email:xiaojiannan@terminus.io
 */
@Slf4j
@EnableScheduling
@RestController
@RequestMapping("/api/doctor/old/report")
public class DoctorOldReportJob {

    @RpcConsumer
    private DoctorDailyReportReadService doctorDailyReportReadService;
    @RpcConsumer
    private DoctorDailyReportWriteService doctorDailyReportWriteService;
    @RpcConsumer(timeout = "60000")
    private DoctorRangeReportWriteService doctorRangeReportWriteService;
    @RpcConsumer(timeout = "60000")
    private DoctorDailyGroupWriteService doctorDailyGroupWriteService;
    @RpcConsumer
    private DoctorFarmReadService doctorFarmReadService;
    @RpcConsumer
    private DoctorOrgReadService doctorOrgReadService;
    @RpcConsumer
    private DoctorDepartmentReadService doctorDepartmentReadService;

    private final HostLeader hostLeader;

    @Autowired
    public DoctorOldReportJob(HostLeader hostLeader) {
        this.hostLeader = hostLeader;
    }

    /**
     * ??????????????????job
     * ????????????1????????????????????????
     */
   // @Scheduled(cron = "0 0 2 * * ?")
    @RequestMapping(value = "/daily", method = RequestMethod.GET)
    public void dailyReport() {
        try {
            if(!hostLeader.isLeader())
            {
                log.info("current leader is:{}, skip", hostLeader.currentLeaderId());
                return;
            }
            log.info("daily report job start, now is:{}", DateUtil.toDateTimeString(new Date()));

            doctorDailyReportWriteService.generateYesterdayAndTodayReports(getAllFarmIds());

            log.info("daily report job end, now is:{}", DateUtil.toDateTimeString(new Date()));
        } catch (Exception e) {
            log.error("daily report job failed, cause:{}", Throwables.getStackTraceAsString(e));
        }
    }

   // @Scheduled(cron = "0 0 3 * * ?")
    @RequestMapping(value = "/group/daily", method = RequestMethod.GET)
    public void groupDaily() {
        try {
            if(!hostLeader.isLeader()){
                log.info("current leader is:{}, skip", hostLeader.currentLeaderId());
                return;
            }
            log.info("daily group job start, now is:{}", DateUtil.toDateTimeString(new Date()));
            Date today = Dates.startOfDay(new Date());
            RespHelper.or500(doctorDailyGroupWriteService.generateYesterdayAndToday(getAllFarmIds(), today));
            log.info("daily group job end, now is:{}", DateUtil.toDateTimeString(new Date()));
        }catch (Exception e){
            log.error("daily group job failed, cause:{}", Throwables.getStackTraceAsString(e));
        }
    }

    /**
     * ??????????????????job
     * ?????????????????????
     */
    //@Scheduled(cron = "0 0 4 * * ?")
    @RequestMapping(value = "/farm/range", method = RequestMethod.GET)
    public void monthlyReport() {
        try {
            if (!hostLeader.isLeader()) {
                log.info("current leader is:{}, skip", hostLeader.currentLeaderId());
                return;
            }
            log.info("range report job start, now is:{}", DateUtil.toDateTimeString(new Date()));
            List<Long> farmIds = getAllFarmIds();
            Date yesterday = DateTime.now().minusDays(1).withTimeAtStartOfDay().toDate();
            doctorRangeReportWriteService.generateDoctorRangeReports(farmIds, yesterday);
            log.info("range report job end, now is:{}", DateUtil.toDateTimeString(new Date()));
        } catch (Exception e) {
            log.error("range report job failed, cause:{}", Throwables.getStackTraceAsString(e));
        }
    }

    /**
     * ??????????????????job
     * ?????????????????????
     */
    //@Scheduled(cron = "0 0 5 * * ?")
    @RequestMapping(value = "/org/range", method = RequestMethod.GET)
    public void monthlyOrgReport() {
        try {
            if (!hostLeader.isLeader()) {
                log.info("current leader is:{}, skip", hostLeader.currentLeaderId());
                return;
            }
            log.info("range report job start, now is:{}", DateUtil.toDateTimeString(new Date()));
            Date since = DateTime.now().minusYears(1).toDate();
            doctorRangeReportWriteService.generateOrgDoctorRangeReports(getOrgToFarm(), since);
            log.info("range report job end");
        } catch (Exception e) {
            log.error("range report job failed, cause:{}", Throwables.getStackTraceAsString(e));
        }
    }

    private List<Long> getAllFarmIds() {
        return RespHelper.orServEx(doctorFarmReadService.findAllFarms()).stream().map(DoctorFarm::getId).collect(Collectors.toList());
    }

    private Map<Long, List<Long>> getOrgToFarm() {
        Map<Long, List<Long>> orgMapToFarm = Maps.newHashMap();
        List<DoctorOrg> orgList = RespHelper.orServEx(doctorOrgReadService.findAllOrgs());
        orgList.forEach(doctorOrg -> {
            Response<List<DoctorFarm>> farmResponse = doctorDepartmentReadService.findAllFarmsByOrgId(doctorOrg.getId());
            if (farmResponse.isSuccess()) {
                orgMapToFarm.put(doctorOrg.getId(), farmResponse.getResult().stream().map(DoctorFarm::getId).collect(Collectors.toList()));
            }
        });
        return orgMapToFarm;
    }
}
