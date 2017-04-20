package io.terminus.doctor.web.admin.job;

import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.doctor.basic.model.DoctorMaterialConsumeProvider;
import io.terminus.doctor.basic.model.DoctorMaterialInWareHouse;
import io.terminus.doctor.basic.service.DoctorMaterialConsumeProviderReadService;
import io.terminus.doctor.basic.service.DoctorMaterialInWareHouseReadService;
import io.terminus.doctor.basic.service.DoctorMaterialPriceInWareHouseReadService;
import io.terminus.doctor.common.utils.DateUtil;
import io.terminus.doctor.common.utils.RespHelper;
import io.terminus.doctor.event.model.DoctorBarn;
import io.terminus.doctor.event.model.DoctorGroup;
import io.terminus.doctor.event.model.DoctorMasterialDatailsGroup;
import io.terminus.doctor.event.service.DoctorBarnReadService;
import io.terminus.doctor.event.service.DoctorGroupMaterialWriteServer;
import io.terminus.doctor.event.service.DoctorGroupReadService;
import io.terminus.doctor.user.model.DoctorFarm;
import io.terminus.doctor.user.service.DoctorFarmReadService;
import io.terminus.zookeeper.leader.HostLeader;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.Objects.isNull;

/**
 * Created by terminus on 2017/4/19.
 */
@RestController
@RequestMapping("/api/ware")
@Slf4j
public class DoctorWareDetailsJobs {

    @RpcConsumer
    private DoctorGroupMaterialWriteServer doctorGroupMaterialWriteServer;
    @RpcConsumer
    private DoctorFarmReadService doctorFarmReadService;

    private final HostLeader hostLeader;
    @Autowired
    public DoctorWareDetailsJobs(HostLeader hostLeader) {
        this.hostLeader = hostLeader;
    }
    private final static Integer DETAILS = 2;
//    @Scheduled(cron = "0 0 1 * * ?")
    @Scheduled(cron = "0 */1 * * * ?")
    @RequestMapping(value = "/details", method = RequestMethod.GET)
    public void groupMaterialReport() {
        try {
            if(!hostLeader.isLeader()) {
                log.info("current leader is:{}, skip", hostLeader.currentLeaderId());
                return;
            }
            doctorGroupMaterialWriteServer.deleteDoctorGroupMaterial(DETAILS);
            log.info("daily ware details job start, now is:{}", DateUtil.toDateTimeString(new Date()));
            doctorGroupMaterialWriteServer.insterDoctorGroupMaterialWareDetails(getAllFarmIds(), DETAILS);
            log.info("daily ware details job end, now is:{}", DateUtil.toDateTimeString(new Date()));
        } catch (Exception e) {
            log.error("daily report job failed, cause:{}", Throwables.getStackTraceAsString(e));
        }
    }

    private List<Long> getAllFarmIds() {
        return RespHelper.orServEx(doctorFarmReadService.findAllFarms()).stream().map(DoctorFarm::getId).collect(Collectors.toList());
    }
}
