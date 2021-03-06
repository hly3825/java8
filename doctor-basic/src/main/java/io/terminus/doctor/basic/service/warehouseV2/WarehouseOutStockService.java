package io.terminus.doctor.basic.service.warehouseV2;

import io.terminus.common.exception.ServiceException;
import io.terminus.doctor.basic.dao.DoctorWarehouseMaterialApplyDao;
import io.terminus.doctor.basic.dto.warehouseV2.WarehouseStockOutDto;
import io.terminus.doctor.basic.enums.WarehouseMaterialApplyType;
import io.terminus.doctor.basic.enums.WarehouseMaterialHandleType;
import io.terminus.doctor.basic.manager.WarehouseOutManager;
import io.terminus.doctor.basic.model.DoctorWareHouse;
import io.terminus.doctor.basic.model.warehouseV2.DoctorWarehouseMaterialApply;
import io.terminus.doctor.basic.model.warehouseV2.DoctorWarehouseMaterialHandle;
import io.terminus.doctor.basic.model.warehouseV2.DoctorWarehouseStockHandle;
import io.terminus.doctor.common.exception.InvalidException;
import io.terminus.doctor.common.utils.DateUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by sunbo@terminus.io on 2018/4/19.
 */
@Slf4j
@Service
public class WarehouseOutStockService extends AbstractWarehouseStockService<WarehouseStockOutDto, WarehouseStockOutDto.WarehouseStockOutDetail> {

    @Autowired
    private WarehouseOutManager warehouseOutManager;

    @Autowired
    private DoctorWarehouseMaterialApplyDao doctorWarehouseMaterialApplyDao;

    @Override
    protected WarehouseMaterialHandleType getMaterialHandleType() {
        return WarehouseMaterialHandleType.OUT;
    }

    @Override
    protected List<WarehouseStockOutDto.WarehouseStockOutDetail> getDetails(WarehouseStockOutDto stockDto) {
        return stockDto.getDetails();
    }

    @Override
    protected void create(WarehouseStockOutDto stockDto,
                          WarehouseStockOutDto.WarehouseStockOutDetail detail,
                          DoctorWarehouseStockHandle stockHandle,
                          DoctorWareHouse wareHouse) {

        warehouseOutManager.create(detail, stockDto, stockHandle, wareHouse);
        doctorWarehouseStockManager.out(detail.getMaterialId(), detail.getQuantity(), wareHouse);
    }

    @Override
    protected void delete(DoctorWarehouseMaterialHandle materialHandle) {

        BigDecimal refundQuantity = doctorWarehouseMaterialHandleDao.countQuantityAlreadyRefund(materialHandle.getId());
        if (refundQuantity.compareTo(new BigDecimal(0)) > 0)
            throw new InvalidException("already.refund.not.allow.delete", materialHandle.getMaterialName());

        warehouseOutManager.delete(materialHandle);

        DoctorWareHouse wareHouse = new DoctorWareHouse();
        wareHouse.setId(materialHandle.getWarehouseId());
        wareHouse.setWareHouseName(materialHandle.getWarehouseName());
        wareHouse.setFarmId(materialHandle.getFarmId());
        wareHouse.setType(materialHandle.getWarehouseType());
        doctorWarehouseStockManager.in(materialHandle.getMaterialId(), materialHandle.getQuantity(), wareHouse);
    }

    @Override
    public void changed(Map<WarehouseStockOutDto.WarehouseStockOutDetail, DoctorWarehouseMaterialHandle> changed, DoctorWarehouseStockHandle stockHandle, WarehouseStockOutDto stockDto, DoctorWareHouse wareHouse) {

        if (!DateUtil.inSameDate(stockHandle.getHandleDate(), stockDto.getHandleDate().getTime())) {
            Date newDate = warehouseOutManager.buildNewHandleDate(stockDto.getHandleDate()).getTime();

            //???????????????????????????????????????????????????
            Date firstRefundDate = doctorWarehouseMaterialHandleDao.findFirstRefundHandleDate(changed.values().stream().map(DoctorWarehouseMaterialHandle::getId).collect(Collectors.toList()));

            if (firstRefundDate != null && newDate.after(firstRefundDate)) {
                throw new ServiceException("out.handle.date.after.refund");
            }
        }

        changed.forEach((detail, materialHandle) -> {
            this.changed(materialHandle, detail, stockHandle, stockDto, wareHouse);
        });
    }

    @Override
    protected void changed(DoctorWarehouseMaterialHandle materialHandle,
                           WarehouseStockOutDto.WarehouseStockOutDetail detail,
                           DoctorWarehouseStockHandle stockHandle,
                           WarehouseStockOutDto stockDto,
                           DoctorWareHouse wareHouse) {

        materialHandle.setRemark(detail.getRemark());
        materialHandle.setSettlementDate(stockDto.getSettlementDate());

        boolean changeHandleDate = !DateUtil.inSameDate(stockHandle.getHandleDate(), stockDto.getHandleDate().getTime());
        boolean changeQuantity = detail.getQuantity().compareTo(materialHandle.getQuantity()) != 0;

        if (changeQuantity)
            log.info("recalculate stock history {},{},{} by change quantity,from {},to {},{},{}",
                    materialHandle.getId(),
                    materialHandle.getWarehouseId(),
                    materialHandle.getMaterialId(),
                    materialHandle.getQuantity(),
                    detail.getQuantity(),
                    detail.getMaterialHandleId(),
                    detail.getMaterialId());

        if (changeQuantity || changeHandleDate) {

            //??????????????????????????????????????????

            if (changeQuantity) {

                BigDecimal changedQuantity = detail.getQuantity().subtract(materialHandle.getQuantity());
                if (changedQuantity.compareTo(new BigDecimal(0)) > 0) {
                    doctorWarehouseStockManager.out(detail.getMaterialId(), changedQuantity, wareHouse);
                } else {
                    //????????????
                    BigDecimal alreadyRefundQuantity = doctorWarehouseMaterialHandleDao.countQuantityAlreadyRefund(materialHandle.getId());
                    if (detail.getQuantity().compareTo(alreadyRefundQuantity) < 0)
//                        throw new ServiceException("warehouse.stock.not.enough");
                        throw new InvalidException("small.then.refund.quantity", materialHandle.getMaterialName(), alreadyRefundQuantity);

                    doctorWarehouseStockManager.in(detail.getMaterialId(), changedQuantity.negate(), wareHouse);
                }
                materialHandle.setQuantity(detail.getQuantity());
            }
            Date recalculateDate = materialHandle.getHandleDate();
            if (changeHandleDate) {
                log.info("change handle date from {} to {}", stockHandle.getHandleDate(), stockDto.getHandleDate().getTime());
                warehouseOutManager.buildNewHandleDateForUpdate(materialHandle, stockDto.getHandleDate());
                if (stockDto.getHandleDate().getTime().before(stockHandle.getHandleDate())) {//??????????????????????????????????????????????????????
                    log.info("handle date get small");
                    recalculateDate = materialHandle.getHandleDate();
                }
            }
            doctorWarehouseMaterialHandleDao.update(materialHandle);
            if (changeHandleDate)
                log.info("recalculate stock history {},{},{} by change handle date",
                        materialHandle.getId(),
                        materialHandle.getWarehouseId(), materialHandle.getMaterialId());

            warehouseOutManager.recalculate(materialHandle, recalculateDate);

        } else {
            //??????????????????
            doctorWarehouseMaterialHandleDao.update(materialHandle);
        }

        DoctorWarehouseMaterialApply apply = doctorWarehouseMaterialApplyDao.findMaterialHandle(materialHandle.getId());
        if (null == apply)
            warehouseOutManager.createApply(materialHandle, detail);
        else if (!detail.getApplyPigBarnId().equals(apply.getPigBarnId())
                || (detail.getApplyPigGroupId() != null && !detail.getApplyPigGroupId().equals(apply.getPigGroupId()))
                || (detail.getApplyPigGroupId() == null && apply.getPigGroupId() != null)
                || changeHandleDate
                || changeQuantity) {

            if (changeHandleDate) {
                apply.setSettlementDate(stockDto.getSettlementDate());
                apply.setSettlementDate(stockDto.getSettlementDate());
                apply.setApplyDate(stockDto.getHandleDate().getTime());
                apply.setApplyYear(stockDto.getHandleDate().get(Calendar.YEAR));
                apply.setApplyMonth(stockDto.getHandleDate().get(Calendar.MONTH) + 1);
            }

            if (changeQuantity) {
                apply.setQuantity(detail.getQuantity());
            }

            apply.setPigBarnId(detail.getApplyPigBarnId());
            apply.setPigBarnName(detail.getApplyPigBarnName());
            apply.setApplyStaffId(detail.getApplyStaffId());
            apply.setApplyStaffName(detail.getApplyStaffName());
            apply.setPigType(detail.getPigType());
    //        apply.setRefundQuantity(detail.getQuantity().subtract(materialHandle.getQuantity()));

            if (apply.getApplyType().equals(WarehouseMaterialApplyType.SOW.getValue())
                    || apply.getApplyType().equals(WarehouseMaterialApplyType.GROUP.getValue())) {

                if (detail.getApplyPigGroupId() == null) {
                    //???????????????????????????????????????????????????????????????????????????
                    doctorWarehouseMaterialApplyDao.deleteGroupApply(materialHandle.getId());
                } else if (apply.getApplyType().equals(WarehouseMaterialApplyType.SOW.getValue()) && !detail.getApplyPigGroupId().equals(-1L)) {
                    //????????????????????????????????????
                    apply.setApplyType(WarehouseMaterialApplyType.GROUP.getValue());
                    apply.setPigGroupName(detail.getApplyPigGroupName());
                    apply.setPigGroupId(detail.getApplyPigGroupId());
                } else if (apply.getApplyType().equals(WarehouseMaterialApplyType.GROUP.getValue()) && detail.getApplyPigGroupId().equals(-1L)) {
                    //????????????????????????????????????
                    apply.setApplyType(WarehouseMaterialApplyType.SOW.getValue());
                    apply.setPigGroupName("??????");
                    apply.setPigGroupId(-1L);
                } else {
                    //????????????????????????
                    apply.setPigGroupId(detail.getApplyPigGroupId());
                    apply.setPigGroupName(detail.getApplyPigGroupName());
                }
                //???????????????????????????
                doctorWarehouseMaterialApplyDao.updateBarnApply(materialHandle.getId(), apply);
            } else {
                if (detail.getApplyPigGroupId() != null) {
                    //???????????????????????????????????????????????????
                    DoctorWarehouseMaterialApply groupApply = new DoctorWarehouseMaterialApply();
                    BeanUtils.copyProperties(apply, groupApply);
                    if (detail.getApplyPigGroupId().equals(-1L)) {
                        groupApply.setApplyType(WarehouseMaterialApplyType.SOW.getValue());
                        groupApply.setPigGroupId(-1L);
                        groupApply.setPigGroupName("??????");
                    } else {
                        groupApply.setApplyType(WarehouseMaterialApplyType.GROUP.getValue());
                        groupApply.setPigGroupId(detail.getApplyPigGroupId());
                        groupApply.setPigGroupName(detail.getApplyPigGroupName());
                    }
                    //??????????????????
                    doctorWarehouseMaterialApplyDao.create(groupApply);
                }
            }

            doctorWarehouseMaterialApplyDao.update(apply);
        }
    }
}
