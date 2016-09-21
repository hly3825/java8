package io.terminus.doctor.warehouse.handler.out;

import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import io.terminus.common.exception.ServiceException;
import io.terminus.common.utils.BeanMapper;
import io.terminus.doctor.warehouse.constants.DoctorFarmWareHouseTypeConstants;
import io.terminus.doctor.warehouse.dao.DoctorFarmWareHouseTypeDao;
import io.terminus.doctor.warehouse.dao.DoctorMaterialConsumeAvgDao;
import io.terminus.doctor.warehouse.dao.DoctorMaterialConsumeProviderDao;
import io.terminus.doctor.warehouse.dao.DoctorWarehouseSnapshotDao;
import io.terminus.doctor.warehouse.dto.DoctorMaterialConsumeProviderDto;
import io.terminus.doctor.warehouse.dto.EventHandlerContext;
import io.terminus.doctor.warehouse.handler.IHandler;
import io.terminus.doctor.warehouse.model.DoctorFarmWareHouseType;
import io.terminus.doctor.warehouse.model.DoctorMaterialConsumeAvg;
import io.terminus.doctor.warehouse.model.DoctorMaterialConsumeProvider;
import io.terminus.doctor.warehouse.model.DoctorWarehouseSnapshot;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

import static com.google.common.base.Preconditions.checkState;
import static java.util.Objects.isNull;

/**
 * Created by yaoqijun.
 * Date:2016-05-30
 * Email:yaoqj@terminus.io
 * Descirbe: 大类warehouse type 类型的修改方式   TODO 添加平均消耗日期信息
 */
@Component
public class DoctorWareHouseTypeConsumerHandler implements IHandler{

    private final DoctorFarmWareHouseTypeDao doctorFarmWareHouseTypeDao;
    private final DoctorMaterialConsumeProviderDao doctorMaterialConsumeProviderDao;
    private final DoctorMaterialConsumeAvgDao doctorMaterialConsumeAvgDao;
    private final DoctorWarehouseSnapshotDao doctorWarehouseSnapshotDao;

    @Autowired
    public DoctorWareHouseTypeConsumerHandler(DoctorFarmWareHouseTypeDao doctorFarmWareHouseTypeDao,
                                              DoctorMaterialConsumeProviderDao doctorMaterialConsumeProviderDao,
                                              DoctorMaterialConsumeAvgDao doctorMaterialConsumeAvgDao,
                                              DoctorWarehouseSnapshotDao doctorWarehouseSnapshotDao){
        this.doctorFarmWareHouseTypeDao = doctorFarmWareHouseTypeDao;
        this.doctorMaterialConsumeProviderDao = doctorMaterialConsumeProviderDao;
        this.doctorMaterialConsumeAvgDao = doctorMaterialConsumeAvgDao;
        this.doctorWarehouseSnapshotDao = doctorWarehouseSnapshotDao;
    }

    @Override
    public boolean ifHandle(DoctorMaterialConsumeProviderDto dto) {
        DoctorMaterialConsumeProvider.EVENT_TYPE eventType = DoctorMaterialConsumeProvider.EVENT_TYPE.from(dto.getActionType());
        return eventType != null && eventType.isOut();
    }

    @Override
    public void handle(DoctorMaterialConsumeProviderDto dto, EventHandlerContext context) throws RuntimeException {
        // update ware house type count
        DoctorFarmWareHouseType doctorFarmWareHouseType = doctorFarmWareHouseTypeDao.findByFarmIdAndType(
                dto.getFarmId(), dto.getType());
        checkState(!isNull(doctorFarmWareHouseType), "doctorFarm.wareHouseType.empty");
        context.getSnapshot().setFarmWareHouseType(BeanMapper.map(doctorFarmWareHouseType, DoctorFarmWareHouseType.class));
        // 修改当前消耗的数量
        doctorFarmWareHouseType.setLotNumber(doctorFarmWareHouseType.getLotNumber() - dto.getCount());

        // 修改数量当日领用信息
        Map<String,Object> extraMap = isNull(doctorFarmWareHouseType.getExtraMap())? Maps.newHashMap() :doctorFarmWareHouseType.getExtraMap();
        if(extraMap.containsKey(DoctorFarmWareHouseTypeConstants.CONSUME_DATE) &&
                DateTime.now().withTimeAtStartOfDay().isEqual(Long.valueOf(extraMap.get(DoctorFarmWareHouseTypeConstants.CONSUME_DATE).toString()))){
            extraMap.put(DoctorFarmWareHouseTypeConstants.CONSUME_COUNT,
                    Double.valueOf(extraMap.get(DoctorFarmWareHouseTypeConstants.CONSUME_COUNT).toString()) + dto.getCount());
        }else {
            extraMap.put(DoctorFarmWareHouseTypeConstants.CONSUME_DATE, DateTime.now().withTimeAtStartOfDay().getMillis());
            extraMap.put(DoctorFarmWareHouseTypeConstants.CONSUME_COUNT, dto.getCount());
        }

        // 修改预计领用时间
        List<DoctorMaterialConsumeAvg> avgs = doctorMaterialConsumeAvgDao.queryByFarmIdAndType(dto.getFarmId(), dto.getType());
        if(!isNull(avgs) && !Iterables.isEmpty(avgs)){
            Double avg = avgs.stream().filter(a->!isNull(a.getConsumeAvgCount()))
                    .map(DoctorMaterialConsumeAvg::getConsumeAvgCount).reduce((c,d)->c+d).orElse(0D);
            if(avg != 0l)
                extraMap.put(DoctorFarmWareHouseTypeConstants.TO_CONSUME_DATE, doctorFarmWareHouseType.getLotNumber() * avgs.size()/avg);
        }
        doctorFarmWareHouseType.setExtraMap(extraMap);
        doctorFarmWareHouseTypeDao.update(doctorFarmWareHouseType);
        context.setDoctorFarmWareHouseTypeId(doctorFarmWareHouseType.getId());
    }

    @Override
    public boolean canRollback(Long eventId) {
        DoctorMaterialConsumeProvider cp = doctorMaterialConsumeProviderDao.findById(eventId);
        DoctorMaterialConsumeProvider.EVENT_TYPE eventType = DoctorMaterialConsumeProvider.EVENT_TYPE.from(cp.getEventType());
        return eventType != null && eventType.isOut();
    }

    @Override
    public void rollback(Long eventId) {
        DoctorMaterialConsumeProvider cp = doctorMaterialConsumeProviderDao.findById(eventId);
        DoctorFarmWareHouseType doctorFarmWareHouseType = doctorFarmWareHouseTypeDao.findByFarmIdAndType(cp.getFarmId(), cp.getType());
        checkState(!isNull(doctorFarmWareHouseType), "doctorFarm.wareHouseType.empty");
        DoctorWarehouseSnapshot snapshot = doctorWarehouseSnapshotDao.findByEventId(eventId);
        if(snapshot == null){
            throw new ServiceException("snapshot.not.found");
        }
        DoctorFarmWareHouseType old = snapshot.json2Snapshot().getFarmWareHouseType();
        doctorFarmWareHouseTypeDao.update(old);
    }
}
