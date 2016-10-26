package io.terminus.doctor.msg.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Builder;

import java.io.Serializable;
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

    private Integer ruleValueId;

}
