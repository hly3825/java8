package io.terminus.doctor.warehouse.manager;

import com.google.common.base.Preconditions;
import io.terminus.doctor.warehouse.dao.DoctorWareHouseDao;
import io.terminus.doctor.warehouse.dao.DoctorWareHouseTrackDao;
import io.terminus.doctor.warehouse.model.DoctorWareHouse;
import io.terminus.doctor.warehouse.model.DoctorWareHouseTrack;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Created by yaoqijun.
 * Date:2016-07-02
 * Email:yaoqj@terminus.io
 * Descirbe: 仓库信息管理方式
 */
@Component
@Slf4j
public class DoctorWareHouseManager {

    private final DoctorWareHouseDao doctorWareHouseDao;

    private final DoctorWareHouseTrackDao doctorWareHouseTrackDao;

    @Autowired
    public DoctorWareHouseManager(DoctorWareHouseDao doctorWareHouseDao,
                                  DoctorWareHouseTrackDao doctorWareHouseTrackDao){
        this.doctorWareHouseDao = doctorWareHouseDao;
        this.doctorWareHouseTrackDao = doctorWareHouseTrackDao;
    }

    @Transactional
    public boolean updateWareHouseInfo(DoctorWareHouse doctorWareHouse){

        Preconditions.checkState(doctorWareHouseDao.update(doctorWareHouse),
                "update.doctorWareHouse.error");

        DoctorWareHouseTrack doctorWareHouseTrack = DoctorWareHouseTrack.builder().wareHouseId(doctorWareHouse.getId())
                .managerId(doctorWareHouse.getManagerId()).managerName(doctorWareHouse.getManagerName())
                .build();

        Preconditions.checkState(doctorWareHouseTrackDao.update(doctorWareHouseTrack), "update.doctorWarehouseTrack.fail");

        return Boolean.TRUE;
    }



}
