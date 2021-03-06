package io.terminus.doctor.basic.manager;

import io.terminus.common.exception.ServiceException;
import io.terminus.common.model.Response;
import io.terminus.doctor.basic.dao.DoctorWarehouseMaterialApplyDao;
import io.terminus.doctor.basic.dto.warehouseV2.WarehouseStockRefundDto;
import io.terminus.doctor.basic.enums.WarehouseMaterialHandleDeleteFlag;
import io.terminus.doctor.basic.enums.WarehouseMaterialHandleType;
import io.terminus.doctor.basic.model.DoctorWareHouse;
import io.terminus.doctor.basic.model.warehouseV2.DoctorWarehouseMaterialHandle;
import io.terminus.doctor.basic.model.warehouseV2.DoctorWarehouseStock;
import io.terminus.doctor.basic.model.warehouseV2.DoctorWarehouseStockHandle;
import io.terminus.doctor.common.exception.InvalidException;
import io.terminus.doctor.common.utils.DateUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * ιζε₯εΊ
 * Created by sunbo@terminus.io on 2018/4/8.
 */
@Component
public class WarehouseReturnManager extends AbstractStockManager<WarehouseStockRefundDto.WarehouseStockRefundDetailDto, WarehouseStockRefundDto> {

    @Autowired
    private DoctorWarehouseMaterialApplyDao doctorWarehouseMaterialApplyDao;

    @Override
    public DoctorWarehouseMaterialHandle create(WarehouseStockRefundDto.WarehouseStockRefundDetailDto detail,
                                                WarehouseStockRefundDto stockDto,
                                                DoctorWarehouseStockHandle stockHandle,
                                                DoctorWareHouse wareHouse) {

        throw new UnsupportedOperationException();
    }

    @Override
    public void create(List<WarehouseStockRefundDto.WarehouseStockRefundDetailDto> details, WarehouseStockRefundDto stockDto, DoctorWarehouseStockHandle stockHandle, DoctorWareHouse wareHouse) {
        //εΊεΊεζ?
        DoctorWarehouseStockHandle outStockHandle = doctorWarehouseStockHandleDao.findById(stockDto.getOutStockHandleId());
        if (null == outStockHandle)
            throw new InvalidException("warehouse.stock.handle.not.found", stockDto.getOutStockHandleId());

        //θ·εεΊεΊζη»οΌεΉΆζη§η©ζεη»
        Map<Long/*materialId*/, List<DoctorWarehouseMaterialHandle>> outMaterialHandleMap = doctorWarehouseMaterialHandleDao
                .findByStockHandle(outStockHandle.getId())
                .stream()
                .collect(Collectors.groupingBy(DoctorWarehouseMaterialHandle::getMaterialId));


        details.forEach(d -> {
            if (!outMaterialHandleMap.containsKey(d.getMaterialId()))
                throw new InvalidException("material.not.allow.refund", d.getMaterialId());

            if (d.getApplyBarnId() == null && d.getApplyGroupId() == null)
                throw new ServiceException("apply.barn.or.apply.group.not.null");

            //ζ Ήζ?ι’η¨ηηͺηΎ€ζηͺθζΎε°δΈζ‘εΊεΊζη»γδΈη¬εΊεΊεζ?δΈ­εδΈͺη©ζι’η¨ε°εδΈͺηͺηΎ€ζηͺηΎ€εͺεθ?ΈεΊη°δΈζ‘εΊεΊζη»
            DoctorWarehouseMaterialHandle outMaterialHandle = doctorWarehouseMaterialHandleDao.findByApply(outStockHandle.getId(), d.getApplyGroupId(), d.getApplyBarnId());
            if (outMaterialHandle == null)
                throw new ServiceException("material.handle.not.found");

//            DoctorWarehouseMaterialHandle outMaterialHandle = outMaterialHandleMap.get(d.getMaterialId()).get(0);
            if (outMaterialHandle.getQuantity().compareTo(d.getQuantity().multiply(BigDecimal.valueOf(-1))) < 0)
                throw new InvalidException("quantity.not.enough.to.refund", outMaterialHandle.getQuantity());

            //ε·²ιζ°ι
            BigDecimal alreadyRefundQuantity = doctorWarehouseMaterialHandleDao.countQuantityAlreadyRefund(outMaterialHandle.getId());
//            BigDecimal alreadyRefundQuantity = doctorWarehouseMaterialHandleDao.findRetreatingById(outMaterialHandle.getId(), null, stockHandleId);
            //θ?‘η?ε―ιζ°ι
            if (outMaterialHandle.getQuantity().add(alreadyRefundQuantity).subtract(d.getFormerQuantity()).compareTo(d.getQuantity().multiply(BigDecimal.valueOf(-1))) < 0)
                throw new InvalidException("quantity.not.enough.to.refund", outMaterialHandle.getQuantity().add(alreadyRefundQuantity).subtract(d.getFormerQuantity()));

            DoctorWarehouseMaterialHandle materialHandle = buildMaterialHandle(d, stockDto, stockHandle, wareHouse);
            materialHandle.setType(WarehouseMaterialHandleType.RETURN.getValue());
            materialHandle.setRelMaterialHandleId(outMaterialHandle.getId());

            //ε₯εΊη±»εοΌε½ε€©η¬¬δΈη¬
            if (!DateUtil.inSameDate(stockDto.getHandleDate().getTime(), new Date())) {

                buildNewHandleDateForUpdate(materialHandle, stockDto.getHandleDate());

                if (materialHandle.getHandleDate().before(outMaterialHandle.getHandleDate()) &&
                        !materialHandle.getHandleDate().equals(outMaterialHandle.getHandleDate())) {
                    throw new ServiceException("refund.date.before.out.date");
                }

                //θ·εθ―₯η¬ζη»δΉεηεΊε­ιοΌεζ¬θ―₯δΊδ»Άζ₯ζ
                BigDecimal historyQuantity = getHistoryQuantityInclude(materialHandle.getHandleDate(), wareHouse.getId(), d.getMaterialId());

                materialHandle.setBeforeStockQuantity(historyQuantity);

                historyQuantity = historyQuantity.add(d.getQuantity());

                //θ―₯η¬εζ?ζη»δΉεεζ?ζη»ιθ¦ιη?
                recalculate(materialHandle.getHandleDate(), false, wareHouse.getId(), d.getMaterialId(), historyQuantity);
            } else {
                BigDecimal currentQuantity = doctorWarehouseStockDao.findBySkuIdAndWarehouseId(d.getMaterialId(), wareHouse.getId())
                        .orElse(DoctorWarehouseStock.builder().quantity(new BigDecimal(0)).build())
                        .getQuantity();
                materialHandle.setBeforeStockQuantity(currentQuantity);
            }
            doctorWarehouseMaterialHandleDao.create(materialHandle);
            //ι’η¨θ?°ε½δΈ­ε’ε ιζζ°ι
            doctorWarehouseMaterialApplyDao.findAllByMaterialHandle(outMaterialHandle.getId()).forEach(apply -> {
                //ιζζ°ιθ?°ε½δΈΊθ΄ζ°
                if (apply.getRefundQuantity() == null)
                    apply.setRefundQuantity(new BigDecimal(0));
                apply.setRefundQuantity(apply.getRefundQuantity().add(d.getQuantity()));
                doctorWarehouseMaterialApplyDao.update(apply);
            });

        });

    }

    @Override
    public void delete(DoctorWarehouseMaterialHandle materialHandle) {

        materialHandle.setDeleteFlag(WarehouseMaterialHandleDeleteFlag.DELETE.getValue());
        doctorWarehouseMaterialHandleDao.update(materialHandle);

//        if (!DateUtil.inSameDate(materialHandle.getHandleDate(), new Date())) {
        //ε ι€εε²εζ?ζη»
        recalculate(materialHandle);
//        }

        //ι’η¨θ?°ε½δΈ­εε°ιζζ°ι
        doctorWarehouseMaterialApplyDao.findAllByMaterialHandle(materialHandle.getId()).forEach(apply -> {
            //ιζζ°ιθ?°ε½δΈΊθ΄ζ°
            if (apply.getRefundQuantity() == null)
                apply.setRefundQuantity(new BigDecimal(0));
            apply.setRefundQuantity(apply.getRefundQuantity().subtract(materialHandle.getQuantity()));
            doctorWarehouseMaterialApplyDao.update(apply);
        });

    }


}
