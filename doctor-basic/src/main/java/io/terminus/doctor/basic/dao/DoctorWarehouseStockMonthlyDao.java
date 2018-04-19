package io.terminus.doctor.basic.dao;

import io.terminus.doctor.basic.dto.warehouseV2.AmountAndQuantityDto;
import io.terminus.common.mysql.dao.MyBatisDao;

import io.terminus.doctor.basic.model.warehouseV2.DoctorWarehouseStockMonthly;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.*;

/**
 * Desc:
 * Mail: [ your email ]
 * Date: 2017-09-29 13:22:37
 * Created by [ your name ]
 */
@Repository
public class DoctorWarehouseStockMonthlyDao extends MyBatisDao<DoctorWarehouseStockMonthly> {


    public AmountAndQuantityDto statistics(Map<String, Object> params) {

        List<DoctorWarehouseStockMonthly> monthlies = this.getSqlSession().selectList(this.sqlId("statistics"), params);
        BigDecimal amount = new BigDecimal(0);
        BigDecimal quantity = new BigDecimal(0);
        for (DoctorWarehouseStockMonthly monthly : monthlies) {
            amount = amount.add(monthly.getBalanceAmount());
            quantity = quantity.add(monthly.getBalanceQuantity());
        }
        return new AmountAndQuantityDto(amount.longValue(), quantity);
    }


    /**
     * 分组统计某一个仓库下sku的余额和余量
     *
     * @param warehouseId
     * @return
     */
    public Map<Long/*skuId*/, AmountAndQuantityDto> statisticsGroupBySku(Long warehouseId, Date handleDate) {

        Map<String, Object> params = new HashMap<>(2);
        params.put("handleDate", handleDate);
        params.put("warehouseId", warehouseId);

        List<DoctorWarehouseStockMonthly> monthlies = this.getSqlSession().selectList(this.sqlId("statisticsGroupBySku"), params);

        Map<Long, AmountAndQuantityDto> statistics = new HashMap<>();
        for (DoctorWarehouseStockMonthly monthly : monthlies) {
            statistics.put(monthly.getMaterialId(), new AmountAndQuantityDto(monthly.getBalanceAmount().longValue(), monthly.getBalanceQuantity()));
        }

        return statistics;
    }


    public AmountAndQuantityDto statistics(Long warehouseId, Date handleDate) {

        Map<String, Object> params = new HashMap<>();
        params.put("warehouseId", warehouseId);
        params.put("handleDate", handleDate);
        DoctorWarehouseStockMonthly monthly = this.getSqlSession().selectOne(this.sqlId("statisticsWarehouse"), params);
        if (null == monthly || null == monthly.getBalanceQuantity() || null == monthly.getBalanceAmount())
            return new AmountAndQuantityDto(0, new BigDecimal(0));

        return new AmountAndQuantityDto(monthly.getBalanceAmount().longValue(), monthly.getBalanceQuantity());
    }

}
