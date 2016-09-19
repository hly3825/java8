package io.terminus.doctor.warehouse.handler.out;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import io.terminus.doctor.warehouse.constants.DoctorWareHouseTrackConstants;
import io.terminus.doctor.warehouse.dao.DoctorMaterialConsumeAvgDao;
import io.terminus.doctor.warehouse.dao.DoctorWareHouseTrackDao;
import io.terminus.doctor.warehouse.dto.DoctorMaterialConsumeProviderDto;
import io.terminus.doctor.warehouse.handler.IHandler;
import io.terminus.doctor.warehouse.model.DoctorMaterialConsumeAvg;
import io.terminus.doctor.warehouse.model.DoctorMaterialConsumeProvider;
import io.terminus.doctor.warehouse.model.DoctorWareHouseTrack;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.google.common.base.Preconditions.checkState;
import static java.util.Objects.isNull;

/**
 * Created by yaoqijun.
 * Date:2016-05-30
 * Email:yaoqj@terminus.io
 * Descirbe:
 */
@Component
public class DoctorWareHouseTrackConsumeHandler implements IHandler{

    private final DoctorWareHouseTrackDao doctorWareHouseTrackDao;

    private final DoctorMaterialConsumeAvgDao doctorMaterialConsumeAvgDao;

    @Autowired
    public DoctorWareHouseTrackConsumeHandler(DoctorWareHouseTrackDao doctorWareHouseTrackDao,
                                              DoctorMaterialConsumeAvgDao doctorMaterialConsumeAvgDao){
        this.doctorWareHouseTrackDao = doctorWareHouseTrackDao;
        this.doctorMaterialConsumeAvgDao = doctorMaterialConsumeAvgDao;
    }

    @Override
    public Boolean ifHandle(DoctorMaterialConsumeProviderDto dto, Map<String, Object> context) {
        DoctorMaterialConsumeProvider.EVENT_TYPE eventType = DoctorMaterialConsumeProvider.EVENT_TYPE.from(dto.getActionType());
        return eventType != null && eventType.isOut();
    }

    @Override
    public void handle(DoctorMaterialConsumeProviderDto dto, Map<String, Object> context) throws RuntimeException {
        // update warehouse track
        DoctorWareHouseTrack doctorWareHouseTrack = this.doctorWareHouseTrackDao.findById(dto.getWareHouseId());
        checkState(!isNull(doctorWareHouseTrack), "not.find.doctorWareHouse");
        doctorWareHouseTrack.setLotNumber(doctorWareHouseTrack.getLotNumber() - dto.getCount());

        // track中 存放 不同material 数量信息
        Map<String, Object> consumeMap = doctorWareHouseTrack.getExtraMap();
        Double count = Double.valueOf(consumeMap.get(dto.getMaterialTypeId().toString()).toString());
        consumeMap.put(dto.getMaterialTypeId().toString(), count - dto.getCount());
        consumeMap.put(DoctorWareHouseTrackConstants.RECENT_CONSUME_DATE, DateTime.now().toDate());

        // 消耗日期信息
        List<DoctorMaterialConsumeAvg> avgList =
                doctorMaterialConsumeAvgDao.queryByIds(ImmutableMap.of("farmId", dto.getFarmId(), "wareHouseId", dto.getWareHouseId()));
        if(!isNull(avgList) && !Iterables.isEmpty(avgList)){
            Double total = avgList.stream().filter(a->!isNull(a.getConsumeAvgCount())).map(s->s.getConsumeAvgCount()).reduce((c,d)->c+d).orElse(0D);
            if(total != 0){
                consumeMap.put(DoctorWareHouseTrackConstants.REST_CONSUME_DATE, doctorWareHouseTrack.getLotNumber() * avgList.size()/total);
            }
        }
        doctorWareHouseTrack.setExtraMap(consumeMap);
        doctorWareHouseTrackDao.update(doctorWareHouseTrack);
    }
}