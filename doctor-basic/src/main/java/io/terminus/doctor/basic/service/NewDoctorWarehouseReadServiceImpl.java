package io.terminus.doctor.basic.service;

import com.google.common.base.Throwables;
import io.terminus.boot.rpc.common.annotation.RpcProvider;
import io.terminus.common.model.PageInfo;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import io.terminus.doctor.basic.dao.DoctorWareHouseDao;
import io.terminus.doctor.basic.dao.DoctorWarehousePurchaseDao;
import io.terminus.doctor.basic.dto.warehouseV2.AmountAndQuantityDto;
import io.terminus.doctor.basic.dto.DoctorWareHouseCriteria;
import io.terminus.doctor.basic.enums.WarehousePurchaseHandleFlag;
import io.terminus.doctor.basic.model.DoctorWareHouse;
import io.terminus.doctor.basic.service.warehouseV2.NewDoctorWarehouseReaderService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.map.HashedMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Created by sunbo@terminus.io on 2017/8/9.
 */
@Slf4j
@Service
@RpcProvider
public class NewDoctorWarehouseReadServiceImpl implements NewDoctorWarehouseReaderService {

    @Autowired
    private DoctorWareHouseDao doctorWareHouseDao;

    @Autowired
    private DoctorWarehousePurchaseDao doctorWarehousePurchaseDao;

    @Override
    public Response<Paging<DoctorWareHouse>> paging(DoctorWareHouseCriteria criteria) {
        PageInfo pageInfo = new PageInfo(criteria.getPageNo(), criteria.getPageSize());


        Map<String, Object> param = new HashedMap();
        param.put("farmId", criteria.getFarmId());
        param.put("type", criteria.getType());
        Paging<DoctorWareHouse> wareHousePaging = doctorWareHouseDao.paging(pageInfo.getOffset(), pageInfo.getLimit(), param);

        return Response.ok(wareHousePaging);
    }


    @Override
    public Response<List<DoctorWareHouse>> list(DoctorWareHouse criteria) {
        try {
            return Response.ok(doctorWareHouseDao.list(criteria));
        } catch (Exception e) {
            log.error("failed to list doctor warehouseV2, cause:{}", Throwables.getStackTraceAsString(e));
            return Response.fail("doctor.warehouseV2.list.fail");
        }
    }

    @Override
    public Response<List<DoctorWareHouse>> findByFarmId(Long farmId) {
        try {
            return Response.ok(doctorWareHouseDao.findByFarmId(farmId));
        } catch (Exception e) {
            log.error("failed to find doctor warehouseV2, cause:{}", Throwables.getStackTraceAsString(e));
            return Response.fail("doctor.warehouseV2.find.fail");
        }
    }


    @Override
    public Response<DoctorWareHouse> findById(Long warehouseId) {
        try {
            return Response.ok(doctorWareHouseDao.findById(warehouseId));
        } catch (Exception e) {
            log.error("failed to find doctor warehouseV2, cause:{}", Throwables.getStackTraceAsString(e));
            return Response.fail("doctor.warehouseV2.find.fail");
        }
    }

    @Override
    public Response<AmountAndQuantityDto> countWarehouseBalance(Long warehouseId) {
        List<DoctorWarehousePurchase> warehousePurchases = doctorWarehousePurchaseDao.list(DoctorWarehousePurchase.builder()
                .handleFinishFlag(WarehousePurchaseHandleFlag.NOT_OUT_FINISH.getValue())
                .warehouseId(warehouseId)
                .build());

        if (null == warehousePurchases || warehousePurchases.isEmpty()) {
            log.info("该仓库[{}]已出库完", warehouseId);
            return Response.ok(new AmountAndQuantityDto(0, new BigDecimal(0)));
        }

        BigDecimal totalQuantity = new BigDecimal(0);
        long totalAmount = 0L;
        for (DoctorWarehousePurchase purchase : warehousePurchases) {
            BigDecimal leftQuantity = purchase.getQuantity().subtract(purchase.getHandleQuantity());
            totalQuantity = totalQuantity.add(leftQuantity);
            totalAmount += leftQuantity.multiply(new BigDecimal(purchase.getUnitPrice())).longValue();
        }
        return Response.ok(new AmountAndQuantityDto(totalAmount, totalQuantity));
    }
}
