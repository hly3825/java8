package io.terminus.doctor.basic.manager;

import io.terminus.common.exception.ServiceException;
import io.terminus.common.model.PageInfo;
import io.terminus.common.model.Paging;
import io.terminus.doctor.basic.dao.DoctorWarehouseHandleDetailDao;
import io.terminus.doctor.basic.dao.DoctorWarehousePurchaseDao;
import io.terminus.doctor.basic.dto.warehouseV2.AbstractWarehouseStockDto;
import io.terminus.doctor.basic.dto.warehouseV2.WarehouseStockInDto;
import io.terminus.doctor.basic.enums.WarehouseMaterialHandleType;
import io.terminus.doctor.basic.enums.WarehousePurchaseHandleFlag;
import io.terminus.doctor.basic.model.warehouseV2.*;
import io.terminus.doctor.common.exception.InvalidException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by sunbo@terminus.io on 2017/9/12.
 */
@Slf4j
@Component
@Deprecated
public class DoctorWarehousePurchaseManager {


    @Autowired
    private DoctorWarehousePurchaseDao doctorWarehousePurchaseDao;
    @Autowired
    private DoctorWarehouseStockManager doctorWarehouseStockManager;

    @Autowired
    private DoctorWarehouseHandleDetailDao doctorWarehouseHandleDetailDao;
    @Autowired
    private DoctorVendorManager doctorVendorManager;

    //    @Transactional
    public DoctorWarehousePurchase in(AbstractWarehouseStockDto stockDto, WarehouseStockInDto.WarehouseStockInDetailDto detail, DoctorWarehouseStock stock, DoctorWarehouseSku sku) {

        DoctorWarehousePurchase purchase = new DoctorWarehousePurchase();
        purchase.setFarmId(stock.getFarmId());
        purchase.setWarehouseId(stock.getWarehouseId());
        purchase.setWarehouseName(stock.getWarehouseName());
        purchase.setWarehouseType(stock.getWarehouseType());
        purchase.setMaterialId(detail.getMaterialId());
        purchase.setVendorName(doctorVendorManager.findById(sku.getVendorId()).getName());
        purchase.setQuantity(detail.getQuantity());
        purchase.setHandleQuantity(new BigDecimal(0));
        purchase.setUnitPrice(detail.getUnitPrice().longValue());
        purchase.setHandleDate(stockDto.getHandleDate().getTime());
        purchase.setHandleYear(stockDto.getHandleDate().get(Calendar.YEAR));
        purchase.setHandleMonth(stockDto.getHandleDate().get(Calendar.MONTH) + 1);
        purchase.setHandleFinishFlag(WarehousePurchaseHandleFlag.NOT_OUT_FINISH.getValue());
        doctorWarehousePurchaseDao.create(purchase);
        return purchase;
    }

    //    @Transactional(propagation = Propagation.NESTED)
    public DoctorWarehouseHandlerManager.PurchaseHandleContext out(DoctorWarehouseStock stock, BigDecimal quantity) {

        DoctorWarehousePurchase purchaseCriteria = new DoctorWarehousePurchase();
        purchaseCriteria.setWarehouseId(stock.getWarehouseId());
        purchaseCriteria.setMaterialId(stock.getSkuId());
        purchaseCriteria.setHandleFinishFlag(WarehousePurchaseHandleFlag.NOT_OUT_FINISH.getValue());//???????????????
        List<DoctorWarehousePurchase> materialPurchases = doctorWarehousePurchaseDao.list(purchaseCriteria);
        if (null == materialPurchases || materialPurchases.isEmpty())
            throw new InvalidException("purchase.not.found", stock.getWarehouseName(), stock.getSkuName());
        materialPurchases.sort(Comparator.comparing(DoctorWarehousePurchase::getHandleDate));

        DoctorWarehouseHandlerManager.PurchaseHandleContext purchaseHandleContext = new DoctorWarehouseHandlerManager.PurchaseHandleContext();

        BigDecimal needPurchaseQuantity = quantity;
        BigDecimal totalHandleQuantity = new BigDecimal(0);
        long totalHandleMoney = 0L;
        for (DoctorWarehousePurchase purchase : materialPurchases) {
            if (needPurchaseQuantity.compareTo(new BigDecimal(0)) <= 0)
                break;

            BigDecimal availablePurchaseQuantity = purchase.getQuantity().subtract(purchase.getHandleQuantity());
            BigDecimal actualCutDownQuantity = availablePurchaseQuantity;
            if (needPurchaseQuantity.compareTo(availablePurchaseQuantity) <= 0) {
                actualCutDownQuantity = needPurchaseQuantity;
            }

            purchase.setHandleQuantity(purchase.getHandleQuantity().add(actualCutDownQuantity));
            if (purchase.getHandleQuantity().compareTo(purchase.getQuantity()) >= 0)
                purchase.setHandleFinishFlag(0);

            purchaseHandleContext.getPurchaseQuantity().put(purchase, actualCutDownQuantity);

            totalHandleMoney += actualCutDownQuantity.multiply(new BigDecimal(purchase.getUnitPrice())).longValue();
            totalHandleQuantity = totalHandleQuantity.add(actualCutDownQuantity);

            needPurchaseQuantity = needPurchaseQuantity.subtract(actualCutDownQuantity);
        }
        //?????????????????????????????????
        purchaseHandleContext.setAveragePrice(new BigDecimal(totalHandleMoney).divide(totalHandleQuantity, 0, BigDecimal.ROUND_HALF_UP).longValue());

        for (DoctorWarehousePurchase purchase : materialPurchases) {
            doctorWarehousePurchaseDao.update(purchase);
        }

        return purchaseHandleContext;
    }


    public BigDecimal calculateUnitPrice(Long warehouseId, Long materialId) {
        return calculateUnitPrice(doctorWarehouseStockManager.getStock(warehouseId, materialId).orElseThrow(() ->
                new InvalidException("stock.not.found", warehouseId, materialId)
        ));
    }

    /**
     * ????????????
     * ?????????????????????????????????????????????????????????????????????
     * ?????????????????????????????????????????????????????????????????????
     *
     * @param stock
     * @return
     * @throws ServiceException ????????????????????????????????????
     */
    public BigDecimal calculateUnitPrice(DoctorWarehouseStock stock) {
        Calendar thisMonth = Calendar.getInstance();
//        lastMonth.add(Calendar.MONTH, -1);//????????????
        List<DoctorWarehousePurchase> thisMonthPurchases = doctorWarehousePurchaseDao.list(DoctorWarehousePurchase.builder()
                .warehouseId(stock.getWarehouseId())
                .materialId(stock.getSkuId())
                .handleYear(thisMonth.get(Calendar.YEAR))
                .handleMonth(thisMonth.get(Calendar.MONTH) + 1)//Calendar???????????????0??????
                .build());

        if (thisMonthPurchases.isEmpty()) {
            PageInfo pageInfo = new PageInfo(1, 1);
            Paging<DoctorWarehousePurchase> lastOnePurchases = doctorWarehousePurchaseDao.paging(pageInfo.getOffset(), pageInfo.getLimit(), DoctorWarehousePurchase.builder()
                    .warehouseId(stock.getWarehouseId())
                    .materialId(stock.getSkuId())
                    .build());
            if (lastOnePurchases.isEmpty())
                throw new InvalidException("purchase.not.found", stock.getWarehouseName(), stock.getSkuName());
            return new BigDecimal(lastOnePurchases.getData().get(0).getUnitPrice());
        } else {

            long totalPrice = 0;
            BigDecimal totalQuantity = new BigDecimal(0);
            for (DoctorWarehousePurchase purchase : thisMonthPurchases) {
                totalPrice = totalPrice + purchase.getQuantity().multiply(new BigDecimal(purchase.getUnitPrice())).longValue();
                totalQuantity = totalQuantity.add(purchase.getQuantity());
            }
            return new BigDecimal(totalPrice).divide(totalQuantity, 0, BigDecimal.ROUND_HALF_UP);
        }
    }


    /**
     * ???????????????????????????????????????
     *
     * @param materialHandle
     */
    public void delete(DoctorWarehouseMaterialHandle materialHandle) {

        if (!WarehouseMaterialHandleType.isBigIn(materialHandle.getType()))//?????????????????????????????????????????????
            throw new ServiceException("purchase.not.allow.delete");

        Map<String, Object> params = new HashMap<>();
        params.put("materialHandleId", materialHandle.getId());
        List<Long> purchaseIds = doctorWarehouseHandleDetailDao.list(params).stream().map(DoctorWarehouseHandleDetail::getMaterialPurchaseId).collect(Collectors.toList());
        if (purchaseIds.isEmpty()) {
            log.warn("material handle [{}] not associate purchase", materialHandle.getId());
            return;
        }

        List<DoctorWarehousePurchase> purchases = doctorWarehousePurchaseDao.findByIds(purchaseIds);
        if (purchases.isEmpty()) {
            log.warn("purchase not found for material handle[{}]", materialHandle.getId());
            return;
        }
        for (DoctorWarehousePurchase purchase : purchases) {
            if (purchase.getHandleQuantity().compareTo(new BigDecimal(0)) != 0) { //????????????????????????????????????
                throw new InvalidException("purchase.has.been.eat", purchase.getUnitPrice(), materialHandle.getMaterialName());
            }

            doctorWarehousePurchaseDao.delete(purchase.getId());
            log.info("delete purchase with warehouse[{}],sku[{}],quantity[{}],unitPrice[{}]", purchase.getWarehouseId(), purchase.getMaterialId(), purchase.getQuantity(), purchase.getUnitPrice());
        }
    }
}
