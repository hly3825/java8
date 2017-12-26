package io.terminus.doctor.user.model;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * Desc: 猪场表Model类
 * Mail: yangzl@terminus.io
 * author: DreamYoung
 * Date: 2016-05-17
 */
@Data
public class DoctorFarm implements Serializable {
    private static final long serialVersionUID = -8307446112600790028L;

    private Long id;
    
    /**
     * 猪场名称
     */
    private String name;

    /**
     * 猪场号
     */
    private String farmCode;
    
    /**
     * 公司id
     */
    private Long orgId;
    
    /**
     * 公司名称
     */
    private String orgName;

    private Integer provinceId;

    private String provinceName;

    private Integer cityId;

    private String cityName;

    private Integer districtId;

    private String districtName;

    private String detailAddress;

    /**
     * 外部id
     */
    private String outId;

    /**
     * 来源
     * @see io.terminus.doctor.common.enums.SourceType
     */
    private Integer source;
    
    /**
     * 附加字段
     */
    private String extra;

    /**
     *
     * 是否是智能猪舍（物联网使用默认是0）
     * 1->智能猪场 0不是猪场
     * @see io.terminus.doctor.common.enums.IsOrNot
     */
    private Integer isIntelligent;

    /**
     * 弱仔数是否作为活仔数
     * @see io.terminus.doctor.common.enums.IsOrNot
     */
    private Integer isWeak;
    
    /**
     * 创建时间
     */
    private Date createdAt;
    
    /**
     * 修改时间
     */
    private Date updatedAt;
}
