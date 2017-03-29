package io.terminus.doctor.event.service;

import com.google.common.base.Throwables;
import io.terminus.boot.rpc.common.annotation.RpcProvider;
import io.terminus.common.model.Response;
import io.terminus.common.utils.Dates;
import io.terminus.doctor.common.utils.DateUtil;
import io.terminus.doctor.event.dao.DoctorGroupInfoCheckDao;
import io.terminus.doctor.event.manager.DoctorEditGroupEventManager;
import io.terminus.doctor.event.model.DoctorGroupInfoCheck;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

/**
 * Code generated by terminus code gen
 * Desc: 猪群数据校验表写服务实现类
 * Date: 2017-03-25
 */
@Slf4j
@Service
@RpcProvider
public class DoctorGroupInfoCheckWriteServiceImpl implements DoctorGroupInfoCheckWriteService {

    private final DoctorGroupInfoCheckDao doctorGroupInfoCheckDao;

    private final DoctorEditGroupEventManager doctorEditGroupEventManage;

    @Autowired
    public DoctorGroupInfoCheckWriteServiceImpl(DoctorGroupInfoCheckDao doctorGroupInfoCheckDao, DoctorEditGroupEventManager doctorEditGroupEventManage) {
        this.doctorGroupInfoCheckDao = doctorGroupInfoCheckDao;
        this.doctorEditGroupEventManage = doctorEditGroupEventManage;
    }

    @Override
    public Response<Long> createDoctorGroupInfoCheck(DoctorGroupInfoCheck doctorGroupInfoCheck) {
        try {
            doctorGroupInfoCheckDao.create(doctorGroupInfoCheck);
            return Response.ok(doctorGroupInfoCheck.getId());
        } catch (Exception e) {
            log.error("create doctorGroupInfoCheck failed, doctorGroupInfoCheck:{}, cause:{}", doctorGroupInfoCheck, Throwables.getStackTraceAsString(e));
            return Response.fail("doctorGroupInfoCheck.create.fail");
        }
    }

    @Override
    public Response<Boolean> updateDoctorGroupInfoCheck(DoctorGroupInfoCheck doctorGroupInfoCheck) {
        try {
            return Response.ok(doctorGroupInfoCheckDao.update(doctorGroupInfoCheck));
        } catch (Exception e) {
            log.error("update doctorGroupInfoCheck failed, doctorGroupInfoCheck:{}, cause:{}", doctorGroupInfoCheck, Throwables.getStackTraceAsString(e));
            return Response.fail("doctorGroupInfoCheck.update.fail");
        }
    }

    @Override
    public Response<Boolean> deleteDoctorGroupInfoCheckById(Long doctorGroupInfoCheckId) {
        try {
            return Response.ok(doctorGroupInfoCheckDao.delete(doctorGroupInfoCheckId));
        } catch (Exception e) {
            log.error("delete doctorGroupInfoCheck failed, doctorGroupInfoCheckId:{}, cause:{}", doctorGroupInfoCheckId, Throwables.getStackTraceAsString(e));
            return Response.fail("doctorGroupInfoCheck.delete.fail");
        }
    }

    @Override
    public Response<Boolean> generateGroupCheckDatas(List<Long> farmIds) {
        try{
            farmIds.forEach(farmId -> {
                generateGroupCheckDatasByFarm(farmId);
            });
        }catch(Exception e){
            log.error("find doctorGroupInfoCheck by id failed,  cause:{}", Throwables.getStackTraceAsString(e));
            return Response.fail("get.group.check.data.fail");
        }
        return Response.ok();
    }

    private void generateGroupCheckDatasByFarm(Long farmId){
        log.info("generate group check data start, farmId: {}, now is: {}", farmId, DateUtil.toDateTimeString(new Date()));
        try{
            doctorGroupInfoCheckDao.deletebyFarmIdAndSumAt(farmId, new Date());
            Integer offset = 1;
            Integer limit = 500;
            Boolean hasNext = true;
            while(hasNext){
                List<DoctorGroupInfoCheck> lists = doctorGroupInfoCheckDao.getGroupCheckDatas(offset, limit, farmId);
                if(lists.size() > 0){
                    offset += 1;
                    doctorGroupInfoCheckDao.creates(lists);
                    continue;
                }
                hasNext = false;
            }
            //修复这些错误的数据
//            repairGroupTrack();
            log.info("generate group check data end, now is: {}", DateUtil.toDateTimeString(new Date()));
        }catch(Exception e){
            log.info("generate group check data failed, cause: {}", Throwables.getStackTraceAsString(e));
            throw e;
        }

    }

    private void repairGroupTrack() {
        DoctorGroupInfoCheck search = new DoctorGroupInfoCheck();
        search.setStatus(DoctorGroupInfoCheck.DataCheckStatus.ERROR.getValue());
        search.setSumAt(Dates.startOfDay(new Date()));
        List<DoctorGroupInfoCheck> needRepairList = doctorGroupInfoCheckDao.list(search);
        needRepairList.forEach(doctorGroupInfoCheck -> {
            doctorEditGroupEventManage.reElicitGroupEventByGroupId(doctorGroupInfoCheck.getGroupId());
        });
    }
}
