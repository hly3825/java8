package io.terminus.doctor.basic.manager;

import io.terminus.common.exception.ServiceException;
import io.terminus.doctor.basic.dao.DoctorMaterialCodeDao;
import io.terminus.doctor.basic.dao.DoctorMaterialVendorDao;
import io.terminus.doctor.basic.dao.DoctorWarehouseStockDao;
import io.terminus.doctor.basic.dto.warehouseV2.WarehouseStockInDto;
import io.terminus.doctor.basic.dto.warehouseV2.WarehouseStockOutDto;
import io.terminus.doctor.basic.model.DoctorWareHouse;
import io.terminus.doctor.basic.model.warehouseV2.DoctorMaterialCode;
import io.terminus.doctor.basic.model.warehouseV2.DoctorMaterialVendor;
import io.terminus.doctor.basic.model.warehouseV2.DoctorWarehouseStock;
import io.terminus.doctor.basic.service.warehouseV2.DoctorWarehouseStockWriteServiceImpl;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.rmi.ServerException;
import java.util.List;
import java.util.Optional;

/**
 * Created by sunbo@terminus.io on 2017/9/12.
 */
@Component
public class DoctorWarehouseStockManager {

    @Autowired
    private DoctorWarehouseStockDao doctorWarehouseStockDao;
    @Autowired
    private DoctorMaterialCodeDao doctorMaterialCodeDao;
    @Autowired
    private DoctorMaterialVendorDao doctorMaterialVendorDao;

//    @Transactional
    public DoctorWarehouseStock in(WarehouseStockInDto inDto, WarehouseStockInDto.WarehouseStockInDetailDto detailDto, DoctorWarehouseStockWriteServiceImpl.StockContext context) {
        //find stock
        DoctorWarehouseStock stock = getStock(inDto.getWarehouseId(), detailDto.getMaterialId()).orElseGet(() -> {
            return DoctorWarehouseStock.builder()
                    .farmId(context.getWareHouse().getFarmId())
                    .warehouseId(context.getWareHouse().getId())
                    .warehouseName(context.getWareHouse().getWareHouseName())
                    .warehouseType(context.getWareHouse().getType())
                    .materialId(detailDto.getMaterialId())
                    .materialName(context.getSupportedMaterials().get(detailDto.getMaterialId()))
                    .unit(detailDto.getUnit())
                    .quantity(detailDto.getQuantity())
                    .build();
        });

        if (null != stock.getId()) {
            stock.setQuantity(stock.getQuantity().add(detailDto.getQuantity()));
            doctorWarehouseStockDao.update(stock);
        } else
            doctorWarehouseStockDao.create(stock);

        if ((StringUtils.isNotBlank(detailDto.getSpecification()) || StringUtils.isNotBlank(detailDto.getMaterialCode())) &&
                doctorMaterialCodeDao.list(DoctorMaterialCode.builder()
                        .warehouseId(stock.getWarehouseId())
                        .materialId(stock.getMaterialId())
                        .vendorName(detailDto.getVendorName())
                        .build()).isEmpty()) {
            DoctorMaterialCode materialCode = new DoctorMaterialCode();
            materialCode.setWarehouseId(stock.getWarehouseId());
            materialCode.setMaterialId(stock.getMaterialId());
            materialCode.setVendorName(detailDto.getVendorName());
            materialCode.setSpecification(detailDto.getSpecification());
            materialCode.setCode(detailDto.getMaterialCode());
            doctorMaterialCodeDao.create(materialCode);
        }

        if (StringUtils.isNotBlank(detailDto.getVendorName()) &&
                doctorMaterialVendorDao.list(DoctorMaterialVendor.builder()
                        .warehouseId(stock.getWarehouseId())
                        .materialId(stock.getMaterialId())
                        .vendorName(detailDto.getVendorName())
                        .build()).isEmpty()) {
            DoctorMaterialVendor materialVendor = new DoctorMaterialVendor();
            materialVendor.setWarehouseId(stock.getWarehouseId());
            materialVendor.setMaterialId(stock.getMaterialId());
            materialVendor.setVendorName(detailDto.getVendorName());
            doctorMaterialVendorDao.create(materialVendor);
        }

        return stock;
    }

//    @Transactional(propagation = Propagation.NESTED)
    public DoctorWarehouseStock out(WarehouseStockOutDto outDto, WarehouseStockOutDto.WarehouseStockOutDetail detailDto) {
        DoctorWarehouseStock stock = getStock(outDto.getWarehouseId(), detailDto.getMaterialId()).orElseThrow(() ->
                new ServiceException("stock.not.found"));

        if (stock.getQuantity().compareTo(detailDto.getQuantity()) < 0)
            throw new ServiceException("stock.not.enough");

        stock.setQuantity(stock.getQuantity().subtract(detailDto.getQuantity()));
        doctorWarehouseStockDao.update(stock);

        return stock;
    }


    public Optional<DoctorWarehouseStock> getStock(Long warehouseId, Long materialId) {
        List<DoctorWarehouseStock> stocks = doctorWarehouseStockDao.list(DoctorWarehouseStock.builder()
                .warehouseId(warehouseId)
                .materialId(materialId)
                .build());
        if (null == stocks || stocks.isEmpty())
            return Optional.empty();
        else
            return Optional.of(stocks.get(0));
    }

}
