package io.terminus.doctor.event.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * Desc: 信用模型计算因子
 * Mail: hehaiyang@terminus.io
 * Date: 2017/04/12
 */
@Data
public class DataFactorDto implements Serializable {

    private static final long serialVersionUID = -1162716465425450496L;

    /**
     * 自增主键
     */
    private Long id;
    
    /**
     * 类型
     */
    private Integer type;
    
    /**
     * 类型名称
     */
    private String typeName;
    
    /**
     * 小类：猪类等
     */
    private Integer subType;
    
    /**
     * 小类名称
     */
    private String subTypeName;
    
    /**
     * 系数
     */
    private Double factor;
    
    /**
     * 范围, 最小值 MIN
     */
    private String rangeFrom;
    
    /**
     * 范围，最大值 MAX
     */
    private String rangeTo;
    
    /**
     * 删除标识
     */
    private Integer isDelete;
    
    /**
     * 创建时间
     */
    private Date createdAt;

    /**
     * 更新时间
     */
    private Date updatedAt;

}