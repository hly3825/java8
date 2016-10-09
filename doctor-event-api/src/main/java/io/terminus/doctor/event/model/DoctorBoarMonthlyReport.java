package io.terminus.doctor.event.model;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * Code generated by terminus code gen
 * Desc: 公猪生产成绩月报Model类
 * Date: 2016-09-12
 */
@Data
public class DoctorBoarMonthlyReport implements Serializable {

    private static final long serialVersionUID = 3545395570899026323L;

    private Long id;
    
    /**
     * 猪场id
     */
    private Long farmId;
    
    /**
     * 公猪号
     */
    private String boarCode;
    
    /**
     * 配种次数
     */
    private Integer pzCount;
    
    /**
     * 首配母猪头数
     */
    private Integer spmzCount;
    
    /**
     * 受胎头数
     */
    private Integer stCount;
    
    /**
     * 产仔母猪头数
     */
    private Integer cmzCount;
    
    /**
     * 平均产仔数
     */
    private double pjczCount;
    
    /**
     * 平均产活仔数
     */
    private double pjchzCount;
    
    /**
     * 受胎率
     */
    private double stRate;
    
    /**
     * 分娩率
     */
    private double fmRate;
    
    /**
     * 汇总时间
     */
    private String sumAt;
    
    private Date createdAt;
    
    private Date updatedAt;
}