package io.terminus.doctor.basic.model.warehouse;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Builder;

import java.io.Serializable;
import java.util.Date;

/**
 * Desc:
 * Mail: [ your email ]
 * Date: 2017-08-23 14:29:31
 * Created by [ your name ]
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DoctorWarehouseMaterialApply implements Serializable {

    private static final long serialVersionUID = -2920383388284436179L;

    /**
     * 自增主键
     */
    private Long id;
    
    /**
     * 仓库编号
     */
    private Long warehouseId;
    
    /**
     * 领用猪舍编号
     */
    private Long pigBarnId;
    
    /**
     * 领用猪舍名称
     */
    private String pigBarnName;
    
    /**
     * 领用猪群编号
     */
    private Long pigGroupId;
    
    /**
     * 领用猪群名称
     */
    private String pigGroupName;
    
    /**
     * 物料编号
     */
    private Long materialId;
    
    /**
     * 领用日期
     */
    private Date applyDate;
    
    /**
     * 领用人
     */
    private String applyStaffName;
    
    /**
     * 领用年
     */
    private Integer applyYear;
    
    /**
     * 领用月
     */
    private Integer applyMonth;
    
    /**
     * 物料名称
     */
    private String materialName;
    
    /**
     * 物料类型，易耗品，原料，饲料，药品，饲料
     */
    private Integer type;
    
    /**
     * 单位
     */
    private String unit;
    
    /**
     * 数量
     */
    private java.math.BigDecimal quantity;
    
    /**
     * 单价，单位分
     */
    private Long unitPrice;
    
    /**
     * 创建时间
     */
    private Date createdAt;

    /**
     * 更新时间
     */
    private Date updatedAt;

}