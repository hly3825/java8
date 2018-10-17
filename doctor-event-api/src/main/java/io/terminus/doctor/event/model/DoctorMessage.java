package io.terminus.doctor.event.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.Builder;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * Code generated by terminus code gen
 * Desc: 猪场软件消息表Model类
 * Date: 2016-05-31
 * Author: chk@terminus.io
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DoctorMessage implements Serializable {

    private static final long serialVersionUID = 1684778508002616587L;

    /**
     * 自增主键
     */
    private Long id;

    /**
     * 猪场id
     */
    private Long farmId;

    /**
     * 规则id
     */
    private Long ruleId;

    /**
     * 子账号的角色id
     */
    private Long roleId;

    /**
     * 消息规则模板id
     */
    private Long templateId;

    /**
     * 消息模板的名称
     */
    private String templateName;

    /**
     * 消息数据填充模板的名称
     */
    private String messageTemplate;

    /**
     * 消息类型: 0->系统消息, 1->预警消息, 2->警报消息
     * @see DoctorMessageRuleTemplate.Type
     */
    private Integer type;

    /**
     * 需要操作的事件类型
     * @SeePigEvent
     */
    private Integer eventType;

    /**
     * 消息种类
     */
    private Integer category;

    /**
     * 发送的内容填充数据, json(map). 或系统消息
     */
    private String data;

    /**
     * 产生消息时猪的事件时间(例如:待配种时,断奶事件时间)
     */
    private Date eventAt;

    /**
     * 用于记录其他所需一些事件时间(例如:待配种提示,预产期)
     */
    private Date otherAt;

    /**
     * 事件发生到消息产生时天数与规则中时间时间差
     */
    private Integer ruleTimeDiff;

    /**
     * 记录事件发生到消息产生时的时间差
     */
    private Integer timeDiff;

    /**
     * 仓库
     */
    private Long wareHouseId;
    private String wareHouseName;

    /**
     * 剩余量
     */
    private Double lotNumber;

    /**
     * 猪只数
     */
    private Integer quantity;

    /**
     * 平均日龄
     */
    private Integer avgDayAge;

    private Long barnId;
    private String barnName;
    //ysq
    private String staffName;

    /**
     * 消息对象的状态
     */
    private Integer status;
    private String statusName;

    /**
     * 事件的操作人id
     */
    private Long operatorId;

    /**
     * 事件操作人姓名
     */
    private String operatorName;

    /**
     * 消息产生原因
     */
    private String reason;

    /**
     * 猪号、猪群号、物料名
     */
    private String code;

    /**
     * 母猪胎次
     */
    private Integer parity;

    /**
     * app回调url
     */
    private String url;


    /**
     * 未读消息的数量
     * (数据库没有对应字段)
     */
    private Long noReadCount;

    /**
     * 操作人id
     */
    private Long createdBy;

    /**
     * 创建时间
     */
    private Date createdAt;

    /**
     * 更新时间
     */
    private Date updatedAt;

    /**
     * 消息对应的操作id: 猪id、猪群id、物料id
     */
    private Long businessId;

    private Integer businessType;

    private Integer ruleValueId;

    /**
     * 消耗剂量
     */
    private BigDecimal dose;

    /**
     * 疫苗Id
     */
    private Long materialId;

    /**
     * 疫苗名称
     */
    private String materialName;
    /**
     * 备注
     */
    private String remark;

    /**
     * 免疫日期类型, 枚举类VaccinationDateType
     * @see io.terminus.doctor.event.enums.VaccinationDateType
     */
    private String vaccinationDateType;

    /**
     * 免疫日期
     */
    private Date vaccinationDate;

    /**
     * 事件日期
     */
    private String eventDate;

    /**
     * 猪类
     */
    private String pigType;

    public enum BUSINESS_TYPE{
        PIG(1, "猪"),
        GROUP(2, "猪群"),
        WAREHOUSE(3, "仓库");

        BUSINESS_TYPE(Integer value, String name) {
            this.value = value;
            this.name = name;
        }
        @Getter
        Integer value;

        @Getter
        String name;

        public BUSINESS_TYPE from(Integer value) {
            for (BUSINESS_TYPE type : BUSINESS_TYPE.values()) {
                if (type.getValue().equals(value)) {
                    return type;
                }
            }
            return null;
        }

    }
}
