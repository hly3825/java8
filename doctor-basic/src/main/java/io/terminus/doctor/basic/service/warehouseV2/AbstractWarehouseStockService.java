package io.terminus.doctor.basic.service.warehouseV2;

import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.exception.ServiceException;
import io.terminus.common.model.Response;
import io.terminus.doctor.basic.dao.DoctorWareHouseDao;
import io.terminus.doctor.basic.dao.DoctorWarehouseMaterialHandleDao;
import io.terminus.doctor.basic.dao.DoctorWarehouseStockHandleDao;
import io.terminus.doctor.basic.dto.warehouseV2.*;
import io.terminus.doctor.basic.enums.WarehouseMaterialHandleType;
import io.terminus.doctor.basic.manager.DoctorWarehouseStockHandleManager;
import io.terminus.doctor.basic.manager.DoctorWarehouseStockManager;
import io.terminus.doctor.basic.model.DoctorWareHouse;
import io.terminus.doctor.basic.model.warehouseV2.DoctorWarehouseMaterialHandle;
import io.terminus.doctor.basic.model.warehouseV2.DoctorWarehouseStockHandle;
import io.terminus.doctor.common.utils.DateUtil;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.support.locks.LockRegistry;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.stream.Collectors;

/**
 * Created by sunbo@terminus.io on 2018/4/19.
 */
@Slf4j
public abstract class AbstractWarehouseStockService<T extends AbstractWarehouseStockDto, F extends AbstractWarehouseStockDetail> {

    @Autowired
    private LockRegistry lockRegistry;

    @Autowired
    protected DoctorWareHouseDao doctorWareHouseDao;
    @Autowired
    protected DoctorWarehouseStockHandleDao doctorWarehouseStockHandleDao;
    @Autowired
    protected DoctorWarehouseMaterialHandleDao doctorWarehouseMaterialHandleDao;

    @Autowired
    protected DoctorWarehouseStockHandleManager doctorWarehouseStockHandleManager;
    @Autowired
    protected DoctorWarehouseStockManager doctorWarehouseStockManager;

    private static final ThreadLocal<List<Lock>> stockLocks = new ThreadLocal<>();

    @Transactional
    @ExceptionHandle("stock.handle.fail")
    public Response<InventoryDto> handle(T stockDto) {

        lockedIfNecessary(stockDto);
        try {

            DoctorWareHouse wareHouse = doctorWareHouseDao.findById(stockDto.getWarehouseId());
            if (null == wareHouse)
                throw new ServiceException("warehouse.not.found");

            // ????????????????????????????????????????????? ????????? 2018-09-18???
            String str = new String();
            if(!stockDto.getIsFormula()) { // ??????????????????????????? ????????? 2018-09-27???
                List<F> details = this.getDetails(stockDto);
                Iterator<F> it = details.iterator();
                while (it.hasNext()) {
                    F next = it.next();
                    // ?????????????????????
                    Long materialId;
                    if (details.size() > 1) {
                        materialId = next.getMaterialId();
                    } else {
                        materialId = details.get(0).getMaterialId();
                    }
                    Long materialHandleId;
                    if (details.size() > 1) {
                        materialHandleId = next.getMaterialHandleId();
                    } else {
                        materialHandleId = details.get(0).getMaterialHandleId();
                    }

                    if (null == materialHandleId) {// ???????????????????????????????????????????????????????????????
                        Date handleDate = stockDto.getHandleDate().getTime();
                        DoctorWarehouseMaterialHandle material = doctorWarehouseMaterialHandleDao.getMaxInventoryDate(stockDto.getWarehouseId(), materialId, handleDate);
                        if (material != null) {// ?????????
                            str = str + material.getMaterialName() + ",";
                            it.remove();
                        }
                    } else { // ??????????????????????????????????????????????????????????????? ????????? 2018-09-19)
                        //??????????????????
                        Map<Long, List<DoctorWarehouseMaterialHandle>> oldMaterialHandles = doctorWarehouseMaterialHandleDao.findByStockHandle(stockDto.getStockHandleId()).stream().collect(Collectors.groupingBy(DoctorWarehouseMaterialHandle::getId));
                        if (oldMaterialHandles.containsKey(materialHandleId)) {
                            DoctorWarehouseMaterialHandle materialHandle = oldMaterialHandles.get(materialHandleId).get(0);
                            if (!materialHandle.getMaterialId().equals(next.getMaterialId())) {// ?????????????????????????????????
                                Date handleDate = stockDto.getHandleDate().getTime();
                                DoctorWarehouseMaterialHandle material = doctorWarehouseMaterialHandleDao.getMaxInventoryDate(stockDto.getWarehouseId(), materialId, handleDate);
                                if (material != null) {// ????????? ??????????????????????????????
                                    // ???????????????????????????????????????????????????Yes???????????? ????????? 2018-09-20???
                                    DoctorWarehouseStockHandle stockHandle = doctorWarehouseStockHandleDao.findById(stockDto.getStockHandleId());
                                    if (!material.getStockHandleId().equals(stockHandle.getId())) {
                                        if (stockHandle.getUpdatedAt().compareTo(material.getHandleDate()) < 0) {
                                            str = str + material.getMaterialName() + ",";
                                            if (details.size() > 1) {
                                                next.setBeforeStockQuantity(materialHandle.getBeforeStockQuantity().toString());
                                                next.setMaterialId(materialHandle.getMaterialId());
                                                next.setQuantity(materialHandle.getQuantity());
                                                next.setRemark(materialHandle.getRemark());
                                            } else {
                                                details.get(0).setBeforeStockQuantity(materialHandle.getBeforeStockQuantity().toString());
                                                details.get(0).setMaterialId(materialHandle.getMaterialId());
                                                details.get(0).setQuantity(materialHandle.getQuantity());
                                                details.get(0).setRemark(materialHandle.getRemark());
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            DoctorWarehouseStockHandle stockHandle = new DoctorWarehouseStockHandle();
            if (null == stockDto.getStockHandleId()) {
                //??????
                if(!stockDto.getIsFormula()){
                    if(!str.equals("")){
                        str = str + "????????????,???????????????";
                    }else{
                        str = "??????????????????";
                    }
                    List<F> dd = this.getDetails(stockDto);
                    if(dd.size()>=1){
                        stockHandle = create(stockDto, wareHouse);
                    }
                }else{
                    stockHandle = create(stockDto, wareHouse);
                }
            } else {
                if(!stockDto.getIsFormula()){
                    if(!str.equals("")){
                        str = str + "????????????,???????????????";
                    }else{
                        str = "??????????????????";
                    }
                }else{
                    str = "??????????????????";
                }
                stockHandle = doctorWarehouseStockHandleDao.findById(stockDto.getStockHandleId());
                //???????????????????????????????????????
                beforeUpdate(stockDto, stockHandle);
                //??????
                stockHandle = update(stockDto, wareHouse, stockHandle);
            }

            InventoryDto inventoryDto = new InventoryDto();
            inventoryDto.setId(stockHandle.getId());
            inventoryDto.setDesc(str);

            return Response.ok(inventoryDto);

        } catch (Throwable e) {
            throw e;
        } finally {
            releaseLocks();
        }
    }

    protected DoctorWarehouseStockHandle create(T stockDto, DoctorWareHouse wareHouse) {
        DoctorWarehouseStockHandle stockHandle = doctorWarehouseStockHandleManager.create(stockDto, wareHouse, getMaterialHandleType(), null);

        this.getDetails(stockDto).forEach(detail -> {
            create(stockDto, detail, stockHandle, wareHouse);
        });
        return stockHandle;
    }

    protected DoctorWarehouseStockHandle update(T stockDto, DoctorWareHouse wareHouse, DoctorWarehouseStockHandle stockHandle) {

        //??????????????????
        Map<Long, List<DoctorWarehouseMaterialHandle>> oldMaterialHandles = doctorWarehouseMaterialHandleDao.findByStockHandle(stockDto.getStockHandleId()).stream().collect(Collectors.groupingBy(DoctorWarehouseMaterialHandle::getId));

        List<F> details = this.getDetails(stockDto);

        oldMaterialHandles.forEach((materialHandleId, oldMaterialHandle) -> {
            boolean include = false;
            for (F detail : details) {
                if (materialHandleId.equals(detail.getMaterialHandleId())) {
                    include = true;
                    break;
                }
            }
            if (!include) { //??????????????????????????????????????????????????????????????????????????????????????????
                delete(oldMaterialHandle.get(0));
            }
        });

        Map<F, DoctorWarehouseMaterialHandle> changed = new HashMap<>();
        details.forEach(detail -> {
            if (detail.getMaterialHandleId() == null)
                create(stockDto, detail, stockHandle, wareHouse);
            else if (oldMaterialHandles.containsKey(detail.getMaterialHandleId())) {
                DoctorWarehouseMaterialHandle materialHandle = oldMaterialHandles.get(detail.getMaterialHandleId()).get(0);

                if (!materialHandle.getMaterialId().equals(detail.getMaterialId())) {
                    create(stockDto, detail, stockHandle, wareHouse);
                    doctorWarehouseStockManager.in(detail.getMaterialId(), detail.getQuantity(), wareHouse);
                    delete(materialHandle);
                    doctorWarehouseStockManager.out(materialHandle.getMaterialId(), materialHandle.getQuantity(), wareHouse);
                } else {
//                    changed(materialHandle, detail, stockHandle, stockDto, wareHouse);
                    changed.put(detail, materialHandle);
                }
            }
        });

        changed(changed, stockHandle, stockDto, wareHouse);

        //????????????
        buildNewHandleDateForUpdate(stockHandle, stockDto.getHandleDate());
        doctorWarehouseStockHandleManager.update(stockDto, stockHandle);
        return stockHandle;
    }

    /**
     * ????????????????????????
     * ???????????????????????????????????????????????????+23:59:59
     * ????????????????????????????????????????????????
     *
     * @param stockHandle
     * @param newHandleDate
     */
    public void buildNewHandleDateForUpdate(DoctorWarehouseStockHandle stockHandle, Calendar newHandleDate) {

        if (DateUtil.inSameDate(newHandleDate.getTime(), new Date())) {
            DateTime old = new DateTime(newHandleDate.getTime());
            DateTime newDate = new DateTime().withDate(old.getYear(), old.getMonthOfYear(), old.getDayOfMonth());
            stockHandle.setHandleDate(newDate.toDate());
            return;
        }

        newHandleDate.set(Calendar.HOUR_OF_DAY, 23);
        newHandleDate.set(Calendar.MINUTE, 59);
        newHandleDate.set(Calendar.SECOND, 59);
        newHandleDate.set(Calendar.MILLISECOND, 0);
        stockHandle.setHandleDate(newHandleDate.getTime());
    }

    /**
     * ????????????
     *
     * @param warehouseId
     */
    public void lockWarehouse(Long warehouseId) {
        List<Lock> locks = stockLocks.get();
        boolean isNewLocks = false;
        if (null == locks) {
            locks = new ArrayList<>();
            isNewLocks = true;
        }
        Lock lock = lockRegistry.obtain(warehouseId.toString());
        if (!lock.tryLock())
            throw new JsonResponseException("stock.handle.in.operation");
        locks.add(lock);

        if (isNewLocks)
            stockLocks.set(locks);
    }

    /**
     * ???????????????????????????????????????
     *
     * @param stockDto
     */
    public void beforeUpdate(T stockDto, DoctorWarehouseStockHandle stockHandle) {

    }

    protected abstract WarehouseMaterialHandleType getMaterialHandleType();

    protected abstract List<F> getDetails(T stockDto);

    protected abstract void create(T stockDto, F detail, DoctorWarehouseStockHandle stockHandle, DoctorWareHouse wareHouse);

    protected abstract void delete(DoctorWarehouseMaterialHandle materialHandle);


    public void changed(Map<F, DoctorWarehouseMaterialHandle> changed, DoctorWarehouseStockHandle stockHandle,
                        T stockDto,
                        DoctorWareHouse wareHouse) {
        changed.forEach((detail, materialHandle) -> {
            changed(materialHandle, detail, stockHandle, stockDto, wareHouse);
        });

    }

    protected abstract void changed(DoctorWarehouseMaterialHandle materialHandle,
                                    F detail,
                                    DoctorWarehouseStockHandle stockHandle,
                                    T stockDto,
                                    DoctorWareHouse wareHouse);


    private void lockedIfNecessary(AbstractWarehouseStockDto stockDto) {

        //??????/??????????????????????????????????????????????????????
//        if (DateUtil.inSameDate(stockDto.getHandleDate().getTime(), new Date()))
//            return;
//
        List<Lock> locks = new ArrayList<>();

        log.info("lock for warehouse :{}", stockDto.getWarehouseId());
        Lock lock = lockRegistry.obtain(stockDto.getWarehouseId().toString());
        if (!lock.tryLock())
            throw new JsonResponseException("stock.handle.in.operation");

        locks.add(lock);
        if (stockDto instanceof WarehouseStockTransferDto) {
            Set<Long> transferInWarehouseIds = new HashSet<>();
            ((WarehouseStockTransferDto) stockDto).getDetails().forEach(d -> {
                transferInWarehouseIds.add(d.getTransferInWarehouseId());
            });

            transferInWarehouseIds.forEach(id -> {
                log.info("lock for warehouse :{}", id);
                Lock l = lockRegistry.obtain(id.toString());
                if (!l.tryLock())
                    throw new JsonResponseException("stock.handle.in.operation");
                locks.add(l);
            });
        } else if (stockDto.getStockHandleId() != null && stockDto instanceof WarehouseFormulaDto) {
            //??????????????????????????????????????????????????????????????????????????????
            Set<Long> formulaOutWarehouseIds = new HashSet<>();
            ((WarehouseFormulaDto) stockDto).getDetails().forEach(d -> {
                formulaOutWarehouseIds.add(d.getWarehouseId());
            });
            formulaOutWarehouseIds.forEach(id -> {
                log.info("lock for warehouse :{}", id);
                Lock l = lockRegistry.obtain(id.toString());
                if (!l.tryLock())
                    throw new JsonResponseException("stock.handle.in.operation");
                locks.add(l);
            });
        }
        stockLocks.set(locks);
    }

    private void releaseLocks() {
        List<Lock> locks = stockLocks.get();
        if (null != locks) {
            locks.forEach(l -> {
                l.unlock();
            });
            stockLocks.set(null);
        }
    }


}
