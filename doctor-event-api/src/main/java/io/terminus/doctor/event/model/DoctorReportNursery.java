package io.terminus.doctor.event.model;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * Desc:
 * Mail: [ your email ]
 * Date: 2018-01-11 16:19:33
 * Created by [ your name ]
 */
@Data
public class DoctorReportNursery implements Serializable {

    private static final long serialVersionUID = -3989338021759144858L;

    /**
     * 自增主键
     */
    private Long id;
    
    /**
     * 
     */
    private Date sumAt;
    
    /**
     * 
     */
    private String sumAtName;
    
    /**
     * 日期类型，日周月季年
     */
    private String dateType;
    
    /**
     * 组织ID
     */
    private Long orzId;
    
    /**
     * 组织名称
     */
    private String orzName;
    
    /**
     * 组织类型，猪场，公司，集团
     */
    private String orzType;
    
    /**
     * 期初存栏
     */
    private Integer start;
    
    /**
     * 转入数量
     */
    private String turnInto;
    
    /**
     * 转入日龄
     */
    private Integer turnIntoAge;
    
    /**
     * 销售数量
     */
    private String sale;
    
    /**
     * 销售均重
     */
    private Double saleAvgWeight;
    
    /**
     * 转育肥数量
     */
    private String toFatten;
    
    /**
     * 转育肥数均重
     */
    private Double toFattenAvgWeight;
    
    /**
     * 转后备数量
     */
    private String toHoubei;
    
    /**
     * 转后备均重
     */
    private Double toHoubeiAvgWeight;
    
    /**
     * 转场数量
     */
    private String chgFarmOut;
    
    /**
     * 转场均重
     */
    private Double chgFarmAvgWeight;
    
    /**
     * 死亡数量
     */
    private String dead;
    
    /**
     * 淘汰数量
     */
    private String weedOut;
    
    /**
     * 其他减少
     */
    private String otherChange;
    
    /**
     * 期末存栏
     */
    private Integer end;
    
    /**
     * 日均存栏
     */
    private Double dailyPigCount;
    
    /**
     * 保育转出均重（70日龄（10周龄重））
     */
    private Double outAvgWeight70;
    
    /**
     * 死淘率
     */
    private Double deadWeedOutRate;
    
    /**
     * 保育成活率
     */
    private Double livingRate;
    
    /**
     * 料肉比
     */
    private Double feedMeatRate;
    
    /**
     * 创建时间
     */
    private Date createdAt;

    /**
     * 更新时间
     */
    private Date updatedAt;

}