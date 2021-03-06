package io.terminus.doctor.basic.service.warehouseV2;

import com.google.common.collect.Maps;
import io.terminus.boot.rpc.common.annotation.RpcProvider;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.exception.ServiceException;
import io.terminus.common.model.Response;
import io.terminus.doctor.basic.dao.*;
import io.terminus.doctor.basic.dto.DoctorWarehouseStockHandleDto;
import io.terminus.doctor.basic.dto.warehouseV2.WarehouseStockDetailDto;
import io.terminus.doctor.basic.dto.warehouseV2.WarehouseStockDto;
import io.terminus.doctor.basic.enums.WarehouseMaterialHandleType;
import io.terminus.doctor.basic.manager.DoctorWarehouseHandlerManager;
import io.terminus.doctor.basic.model.DoctorBasicMaterial;
import io.terminus.doctor.basic.model.DoctorFarmBasic;
import io.terminus.doctor.basic.model.DoctorWareHouse;
import io.terminus.doctor.basic.model.warehouseV2.DoctorWarehouseMonthlyStock;
import io.terminus.doctor.basic.model.warehouseV2.DoctorWarehouseStock;
import io.terminus.doctor.basic.model.warehouseV2.DoctorWarehouseStockHandlerDetail;
import io.terminus.doctor.basic.service.DoctorFarmBasicReadService;
import io.terminus.doctor.basic.service.DoctorWareHouseReadService;
import org.apache.commons.collections.map.HashedMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;

/**
 * Created by sunbo@terminus.io on 2017/8/10.
 */
@Service
@RpcProvider
public class NewDoctorWarehouseWriterServiceImpl implements NewDoctorWarehouseWriterService {


    @Autowired
    private DoctorFarmBasicReadService doctorFarmBasicReadService;

    @Autowired
    private DoctorWareHouseReadService doctorWareHouseReadService;

    @Autowired
    private DoctorBasicMaterialDao doctorBasicMaterialDao;


    @Autowired
    private DoctorWarehouseStockDao doctorWarehouseStockDao;
//    @Autowired
//    private DoctorWarehouseStockHandlerDao doctorWarehouseStockHandlerDao;
    @Autowired
    private DoctorWarehouseStockHandlerDetailDao doctorWarehouseStockHandlerDetailDao;
    @Autowired
    private DoctorWarehouseHandlerManager doctorWarehouseHandlerManager;

    @Autowired
    private DoctorWarehouseMonthlyStockDao doctorWarehouseMonthlyStockDao;

    @Override
    public void handler(WarehouseStockDto warehouseStockDto) {

        //??????????????????????????????????????????
        Response<DoctorFarmBasic> farmBasicResponse = doctorFarmBasicReadService.findFarmBasicByFarmId(warehouseStockDto.getFarmID());
        if (!farmBasicResponse.isSuccess())
            throw new JsonResponseException(farmBasicResponse.getError());
        DoctorFarmBasic farmBasic = farmBasicResponse.getResult();
        if (null == farmBasic)
            throw new JsonResponseException("farm.basic.not.found");


        List<Long> currentFarmSupportedMaterils = farmBasic.getMaterialIdList();

        Response<DoctorWareHouse> wareHouseResponse = doctorWareHouseReadService.findById(warehouseStockDto.getWarehouseID());
        if (!wareHouseResponse.isSuccess())
            throw new JsonResponseException(wareHouseResponse.getError());
        DoctorWareHouse wareHouse = wareHouseResponse.getResult();
        if (null == wareHouse)
            throw new JsonResponseException("warehouse.not.found");


        Map<Long, DoctorBasicMaterial> materials = new HashedMap(warehouseStockDto.getDetails().size());
        warehouseStockDto.getDetails().forEach(detail -> {
            if (!currentFarmSupportedMaterils.contains(detail.getMaterialID()))
                throw new ServiceException("material.not.allow.in.this.warehouse");

            DoctorBasicMaterial basicMaterial = doctorBasicMaterialDao.findById(detail.getMaterialID());
            if (null == basicMaterial)
                throw new ServiceException("material.not.found");

            materials.put(detail.getMaterialID(), basicMaterial);
        });

//        List<DoctorWarehouseStockHandlerDetail> handlerDetails = new ArrayList<>(warehouseStockDto.getDetails().size());
//        List<DoctorWarehouseStock> stocks = new ArrayList<>(warehouseStockDto.getDetails().size());
//

        List<DoctorWarehouseStockHandleDto> inHandles = new ArrayList<>();
        List<DoctorWarehouseStockHandleDto> outHandles = new ArrayList<>();
        warehouseStockDto.getDetails().forEach(detail -> {

            DoctorWarehouseStock stock = getStock(warehouseStockDto.getWarehouseID(), detail.getMaterialID(), detail.getVendorID());

            DoctorWarehouseStockHandlerDetail handlerDetail = new DoctorWarehouseStockHandlerDetail();
            handlerDetail.setNumber(detail.getNumber());
            handlerDetail.setUnit(detail.getUnit());
            handlerDetail.setUnitPrice(detail.getUnitPrice());

            DoctorWarehouseStockHandleDto dto = new DoctorWarehouseStockHandleDto();
            dto.setHandleDetail(handlerDetail);
            dto.setHandleDate(warehouseStockDto.getHandlerDate());
            dto.setNumber(handlerDetail.getNumber());
            if (warehouseStockDto.getType() == WarehouseMaterialHandleType.IN.getValue()) {
                if (null == stock) {
                    stock = new DoctorWarehouseStock();
                    stock.setFarmId(warehouseStockDto.getFarmID());
                    stock.setWarehouseId(warehouseStockDto.getWarehouseID());
                    stock.setWarehouseName(wareHouse.getWareHouseName());
                    stock.setWarehouseType(wareHouse.getType());
//                    stock.setManagerId(warehouseStockDto.getOperatorID());
                    stock.setQuantity(detail.getNumber());
//                    stock.setUnit(detail.getUnit());
//                    stock.setUnitPrice(detail.getUnitPrice());
                    stock.setSkuId(detail.getMaterialID());
                    stock.setSkuName(materials.get(detail.getMaterialID()).getName());
                }
                dto.setStock(stock);
                inHandles.add(dto);
            } else if (warehouseStockDto.getType() == WarehouseMaterialHandleType.OUT.getValue()) {
                if (stock == null)
                    throw new ServiceException("?????????????????????????????????????????????");

                if (stock.getQuantity().compareTo(detail.getNumber()) < 0)
                    throw new ServiceException("?????????????????????????????????????????????");
                handlerDetail.setPigId(detail.getPigID());
                handlerDetail.setRecipientId(detail.getRecipientID());
                dto.setStock(stock);
                outHandles.add(dto);
            } else if (warehouseStockDto.getType() == WarehouseMaterialHandleType.TRANSFER.getValue()) {
                if (stock == null)
                    throw new ServiceException("?????????????????????????????????????????????");

                if (stock.getQuantity().compareTo(detail.getNumber()) < 0)
                    throw new ServiceException("?????????????????????????????????????????????");
                dto.setStock(stock);
                outHandles.add(dto);


                if (stock.getWarehouseId() == detail.getTargetWarehouseID())
                    throw new ServiceException("??????????????????????????????????????????");

                Map<String, Object> targetWarehouseStockCeriteria = new HashedMap();
                targetWarehouseStockCeriteria.put("warehouseId", detail.getTargetWarehouseID());
                targetWarehouseStockCeriteria.put("materialId", detail.getMaterialID());
                if (null != detail.getVendorID())
                    targetWarehouseStockCeriteria.put("vendorId", detail.getVendorID());
                List<DoctorWarehouseStock> targetWarehouseStockes = doctorWarehouseStockDao.list(targetWarehouseStockCeriteria);
                if (null == targetWarehouseStockes || targetWarehouseStockes.isEmpty()) {
                    throw new ServiceException("?????????????????????");
                }

                Response<DoctorWareHouse> targetWareHouseResponse = doctorWareHouseReadService.findById(detail.getTargetWarehouseID());
                if (!targetWareHouseResponse.isSuccess() || null == targetWareHouseResponse.getResult())
                    throw new ServiceException("???????????????");
                if (wareHouse.getType() != targetWareHouseResponse.getResult().getType())
                    throw new ServiceException("???????????????????????????????????????");

                DoctorWarehouseStockHandleDto indto = new DoctorWarehouseStockHandleDto();
                indto.setStock(targetWarehouseStockes.get(0));
                handlerDetail.setTargetWarehouseId(detail.getTargetWarehouseID());
                indto.setHandleDetail(handlerDetail);
                indto.setNumber(handlerDetail.getNumber());
                indto.setHandleDate(warehouseStockDto.getHandlerDate());
                inHandles.add(indto);
            } else if (warehouseStockDto.getType() == WarehouseMaterialHandleType.INVENTORY.getValue()) {
                if (null == stock)
                    throw new ServiceException("???????????????");
                dto.setStock(stock);
                if (stock.getQuantity().compareTo(detail.getNumber()) > 0) {
                    //??????
                    dto.setNumber(stock.getQuantity().subtract(detail.getNumber()));
                    outHandles.add(dto);
                } else if (stock.getQuantity().compareTo(detail.getNumber()) < 0) {
                    //??????
                    dto.setNumber(detail.getNumber().subtract(stock.getQuantity()));
                    inHandles.add(dto);
                }

            }
//            if (warehouseStockDto.getType() == WarehouseMaterialHandleType.IN.getValue())
//                stock = in(stock, warehouseStockDto, wareHouse, detail, materials);
//            else if (warehouseStockDto.getType() == WarehouseMaterialHandleType.OUT.getValue())
//                out(stock, detail, handlerDetail);
//            else if (warehouseStockDto.getType() == WarehouseMaterialHandleType.INVENTORY.getValue())
//                stock = inventory(stock, warehouseStockDto, wareHouse, detail, materials);
//            else if (warehouseStockDto.getType() == WarehouseMaterialHandleType.TRANSFER.getValue())
//                stocks.add(transfer(stock, detail, wareHouse, handlerDetail));
//            else
//                throw new ServiceException("??????????????????????????????");
//
//            stocks.add(stock);


//            handlerDetails.add(handlerDetail);
        });
//        DoctorWarehouseStockHandler handler = new DoctorWarehouseStockHandler();
//        handler.setFarmId(warehouseStockDto.getFarmID());
//        handler.setWarehouseId(warehouseStockDto.getWarehouseID());
//        handler.setHandlerType(warehouseStockDto.getType());
//        handler.setHandlerDate(warehouseStockDto.getHandlerDate());


//        doctorWarehouseHandlerManager.inAndOutStock(inHandles, outHandles, handler);
    }


    private DoctorWarehouseStock in(DoctorWarehouseStock stock, WarehouseStockDto warehouseStockDto, DoctorWareHouse wareHouse, WarehouseStockDetailDto detail, Map<Long, DoctorBasicMaterial> materials) {
        if (null == stock) {
            stock = new DoctorWarehouseStock();
            stock.setFarmId(warehouseStockDto.getFarmID());
            stock.setWarehouseId(warehouseStockDto.getWarehouseID());
            stock.setWarehouseName(wareHouse.getWareHouseName());
            stock.setWarehouseType(wareHouse.getType());
//            stock.setManagerId(warehouseStockDto.getOperatorID());
            stock.setQuantity(detail.getNumber());
//            stock.setUnit(detail.getUnit());
            stock.setSkuId(detail.getMaterialID());
            stock.setSkuName(materials.get(detail.getMaterialID()).getName());
//            doctorWarehouseStockDao.create(stock);
        } else {
            stock.setQuantity(detail.getNumber().add(stock.getQuantity()));
//            doctorWarehouseStockDao.update(stock);
        }
        return stock;
    }


    private void out(DoctorWarehouseStock stock, WarehouseStockDetailDto detail, DoctorWarehouseStockHandlerDetail handlerDetail) {

        if (stock == null)
            throw new ServiceException("?????????????????????????????????????????????");

        if (stock.getQuantity().compareTo(detail.getNumber()) < 0)
            throw new ServiceException("?????????????????????????????????????????????");

        stock.setQuantity(stock.getQuantity().subtract(detail.getNumber()));
//            doctorWarehouseStockDao.update(stock);

        handlerDetail.setPigId(detail.getPigID());

    }

    /**
     * ??????
     *
     * @param stock
     * @param warehouseStockDto
     * @param wareHouse
     * @param detail
     * @param materials
     */
    private DoctorWarehouseStock inventory(DoctorWarehouseStock stock, WarehouseStockDto warehouseStockDto, DoctorWareHouse wareHouse, WarehouseStockDetailDto detail, Map<Long, DoctorBasicMaterial> materials) {
        if (null == stock) {
            stock = new DoctorWarehouseStock();
            stock.setFarmId(warehouseStockDto.getFarmID());
            stock.setWarehouseId(warehouseStockDto.getWarehouseID());
            stock.setWarehouseName(wareHouse.getWareHouseName());
            stock.setWarehouseType(wareHouse.getType());
//            stock.setManagerId(warehouseStockDto.getOperatorID());
            stock.setQuantity(detail.getNumber());
//            stock.setUnit(detail.getUnit());
            stock.setSkuId(detail.getMaterialID());
            stock.setSkuName(materials.get(detail.getMaterialID()).getName());
        } else {
            stock.setQuantity(detail.getNumber());
        }
        return stock;
    }

    /**
     * ??????
     *
     * @param stock
     * @param detail
     */
    private DoctorWarehouseStock transfer(DoctorWarehouseStock stock, WarehouseStockDetailDto detail, DoctorWareHouse wareHouse, DoctorWarehouseStockHandlerDetail handlerDetail) {
        if (null == stock)
            throw new ServiceException("???????????????????????????");
        if (detail.getNumber().compareTo(stock.getQuantity()) > 0)
            throw new ServiceException("??????????????????????????????");

        if (stock.getWarehouseId() == detail.getTargetWarehouseID())
            throw new ServiceException("??????????????????????????????????????????");


        Map<String, Object> targetWarehouseStockCeriteria = new HashedMap();
        targetWarehouseStockCeriteria.put("warehouseId", detail.getTargetWarehouseID());
        targetWarehouseStockCeriteria.put("materialId", detail.getMaterialID());
        List<DoctorWarehouseStock> targetWarehouseStockes = doctorWarehouseStockDao.list(targetWarehouseStockCeriteria);
        if (null == targetWarehouseStockes || targetWarehouseStockes.isEmpty()) {
            throw new ServiceException("?????????????????????");
        }


        Response<DoctorWareHouse> wareHouseResponse = doctorWareHouseReadService.findById(detail.getTargetWarehouseID());
        if (!wareHouseResponse.isSuccess() || null == wareHouseResponse.getResult())
            throw new ServiceException("???????????????");
        if (wareHouse.getType() != wareHouseResponse.getResult().getType())
            throw new ServiceException("???????????????????????????????????????");

        stock.setQuantity(stock.getQuantity().subtract(detail.getNumber()));
        DoctorWarehouseStock targetWarehouseStock = targetWarehouseStockes.get(0);
        targetWarehouseStock.setQuantity(targetWarehouseStock.getQuantity().add(detail.getNumber()));

        handlerDetail.setTargetWarehouseId(detail.getTargetWarehouseID());
        return targetWarehouseStock;

    }

    private DoctorWarehouseStock getStock(Long warehouseID, Long materialID, Long vendorId) {
        Map<String, Object> criteria = new HashedMap();
        criteria.put("warehouseId", warehouseID);
        criteria.put("materialId", materialID);
        if (null != vendorId)
            criteria.put("vendorId", vendorId);
        List<DoctorWarehouseStock> existedStock = doctorWarehouseStockDao.list(criteria);
        if (existedStock.isEmpty())
            return null;

        return existedStock.get(0);
    }


    private void handleStockMonthlyReport(DoctorWarehouseStock stock, Date handleDate, BigDecimal number, Long unitPrice, boolean in) {
//        Map<String, Object> criteria = new HashedMap();
//        criteria.put("warehouseId", stock.getWarehouseId());
//        criteria.put("materialId", stock.getManagerId());
//        if (null != stock.getVendorName())
//            criteria.put("vendorName", stock.getVendorName());
//
//        Calendar calendar = Calendar.getInstance();
//        calendar.setTime(handleDate);
//
//        criteria.put("year", calendar.get(Calendar.YEAR));
//        criteria.put("month", calendar.get(Calendar.MONTH));
//        List<DoctorWarehouseMonthlyStock> monthlyStockResult = doctorWarehouseMonthlyStockDao.list(criteria);
//
//
//        DoctorWarehouseMonthlyStock lastMonthStock = findLastMonth(criteria);
//
//        Long money = number.multiply(new BigDecimal(unitPrice)).longValue();
//
//        if (null == monthlyStockResult || monthlyStockResult.isEmpty()) {
//
//            DoctorWarehouseMonthlyStock monthlyStock = new DoctorWarehouseMonthlyStock();
//            monthlyStock.setWarehouseId(stock.getWarehouseId());
//            monthlyStock.setWarehouseName(stock.getWarehouseName());
//            monthlyStock.setWarehouseType(stock.getWarehouseType());
//            monthlyStock.setMaterialId(stock.getMaterialId());
//            monthlyStock.setMaterialName(stock.getMaterialName());
//            monthlyStock.setVendorId(stock.getVendorName());
//            if (null == lastMonthStock) {
//                monthlyStock.setEarlyMoney(0L);
//                monthlyStock.setEarlyNumber(new BigDecimal(0));
//            } else {
//                monthlyStock.setEarlyNumber(lastMonthStock.getBalanceNumber());
//                monthlyStock.setEarlyMoney(lastMonthStock.getBalanceMoney());
//            }
//            monthlyStock.setYear(calendar.get(Calendar.YEAR));
//            monthlyStock.setMonth(calendar.get(Calendar.MONTH));
//            if (in) {
//                monthlyStock.setInNumber(number);
//                monthlyStock.setInMoney(money);
//            } else {
//                monthlyStock.setOutNumber(number);
//                monthlyStock.setOutMoney(money);
//            }
//            monthlyStock.setBalanceMoney(money);
//            monthlyStock.setBalanceNumber(number);
//        } else {
//
//            DoctorWarehouseMonthlyStock monthlyStock = monthlyStockResult.get(0);
//
//            if (in) {
//                monthlyStock.setInMoney(monthlyStock.getInMoney() + money);
//                monthlyStock.setInNumber(monthlyStock.getInNumber().add(number));
//                monthlyStock.setBalanceNumber(monthlyStock.getBalanceNumber().add(number));
//                monthlyStock.setBalanceMoney(monthlyStock.getBalanceMoney() + money);
//            } else {
//                monthlyStock.setOutNumber(monthlyStock.getOutNumber().add(number));
//                monthlyStock.setOutMoney(monthlyStock.getOutMoney() + money);
//                monthlyStock.setBalanceMoney(monthlyStock.getBalanceMoney() - money);
//                monthlyStock.setBalanceNumber(monthlyStock.getBalanceNumber().subtract(number));
//            }
//        }
    }


    private DoctorWarehouseMonthlyStock findLastMonth(Map<String, Object> criteria) {

        Map<String, Object> lastMonthCriteria = Maps.newHashMap(criteria);
        if (criteria.get("month").equals("1")) {
            lastMonthCriteria.put("month", 12);
            lastMonthCriteria.put("year", Integer.parseInt(criteria.get("year").toString()) - 1);
        } else {
            lastMonthCriteria.put("month", Integer.parseInt(criteria.get("month").toString()) - 1);
        }

        List<DoctorWarehouseMonthlyStock> lastMonthStockResult = doctorWarehouseMonthlyStockDao.list(lastMonthCriteria);
        if (null == lastMonthCriteria || lastMonthCriteria.isEmpty())
            return null;
        else return lastMonthStockResult.get(0);
    }


    protected interface WarehouseStockHandler {

        void handler(DoctorWarehouseStock stock);
    }
}
