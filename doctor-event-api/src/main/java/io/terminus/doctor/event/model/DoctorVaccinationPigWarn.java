package io.terminus.doctor.event.model;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * Code generated by terminus code gen
 * Desc: 猪只设置免疫程序统计方式Model类
 * Date: 2016-06-13
 */
@Data
public class DoctorVaccinationPigWarn implements Serializable {

    private static final long serialVersionUID = -8338877015067037775L;

    private Long id;
    
    /**
     * 猪场仓库信息
     */
    private Long farmId;
    
    /**
     * 猪场名称
     */
    private String farmName;
    
    /**
     * 猪类
     * @see io.terminus.doctor.common.enums.PigType
     */
    private Integer pigType;

    
    /**
     * 疫苗Id
     */
    private Long materialId;
    
    /**
     * 疫苗名称
     */
    private String materialName;
    
    /**
     * 开始时间
     */
    private Date startDate;
    
    /**
     * 结束时间
     */
    private Date endDate;
    
    /**
     * 免疫日期类型, 枚举类VaccinationDateType
     * @see io.terminus.doctor.event.enums.VaccinationDateType
     */
    private Integer vaccinationDateType;
    
    /**
     * 录入的天数/重量
     */
    private Integer inputValue;
    
    /**
     * 录入的日期
     */
    private Date inputDate;
    
    /**
     * 消耗剂量
     */
    private Long dose;
    
    /**
     * 备注
     */
    private String remark;
}
