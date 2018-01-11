package io.terminus.doctor.event.dto;

import lombok.Data;

import java.util.Date;

/**
 * Created by xjn on 18/1/11.
 * email:xiaojiannan@terminus.io
 */
@Data
public class DoctorDimensionCriteria {
    /**
     * 组织id
     */
    private Integer orzId;
    /**
     * 组织维度
     * @see io.terminus.doctor.event.enums.OrzDimension
     */
    private Integer orzType;

    /**
     * 统计时间
     */
    private Date sumAt;

    /**
     * 时间维度
     * @see io.terminus.doctor.event.enums.DateDimension
     */
    private Integer dateType;

}
