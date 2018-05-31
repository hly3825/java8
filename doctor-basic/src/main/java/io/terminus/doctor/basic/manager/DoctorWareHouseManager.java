package io.terminus.doctor.basic.manager;

import com.google.common.base.Preconditions;
import io.terminus.doctor.basic.dao.DoctorWareHouseDao;
import io.terminus.doctor.basic.dao.DoctorWareHouseTrackDao;
import io.terminus.doctor.basic.enums.IsOrNot;
import io.terminus.doctor.basic.model.DoctorWareHouse;
import io.terminus.doctor.basic.model.DoctorWareHouseTrack;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;

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

//        DoctorWareHouseTrack doctorWareHouseTrack = DoctorWareHouseTrack.builder().wareHouseId(doctorWareHouse.getId())
//                .managerId(doctorWareHouse.getManagerId()).managerName(doctorWareHouse.getManagerName())
//                .build();
//
//        Preconditions.checkState(doctorWareHouseTrackDao.update(doctorWareHouseTrack), "update.doctorWarehouseTrack.fail");

        return Boolean.TRUE;
    }

    @Transactional
    public boolean deleteWareHouseInfo(DoctorWareHouse doctorWareHouse){

        Preconditions.checkState(doctorWareHouseDao.delete(doctorWareHouse.getId()),
                "delete.doctorWareHouse.error");

//        DoctorWareHouseTrack doctorWareHouseTrack = DoctorWareHouseTrack.builder().wareHouseId(doctorWareHouse.getId())
//                .managerId(doctorWareHouse.getManagerId()).managerName(doctorWareHouse.getManagerName())
//                .build();
//        Preconditions.checkState(doctorWareHouseTrackDao.delete(doctorWareHouseTrack.getWareHouseId()), "delete.doctorWarehouseTrack.fail");

        return Boolean.TRUE;
    }

    @Transactional
    public Boolean createWareHouseInfo(DoctorWareHouse doctorWareHouse){

        // 创建WareHouse 信息
        doctorWareHouseDao.create(doctorWareHouse);

//        DoctorWareHouseTrack doctorWareHouseTrack = DoctorWareHouseTrack.builder()
//                .wareHouseId(doctorWareHouse.getId())
//                .farmId(doctorWareHouse.getFarmId()).farmName(doctorWareHouse.getFarmName())
//                .managerId(doctorWareHouse.getManagerId()).managerName(doctorWareHouse.getManagerName())
//                .materialLotNumber(null).lotNumber(0D).isDefault(IsOrNot.NO.getKey())
//                .extraMap(new HashMap<>())
//                .build();
//
//        doctorWareHouseTrackDao.create(doctorWareHouseTrack);
        return Boolean.TRUE;
    }

}
