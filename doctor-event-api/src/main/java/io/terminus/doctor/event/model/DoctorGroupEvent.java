package io.terminus.doctor.event.model;

import com.google.common.base.Objects;
import io.terminus.doctor.common.utils.JsonMapperUtil;
import io.terminus.doctor.common.utils.ToJsonMapper;
import io.terminus.doctor.event.dto.event.group.input.BaseGroupInput;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;

import java.io.Serializable;
import java.util.Date;
import java.util.Map;

/**
 * Desc: 猪群事件表Model类
 * Mail: yangzl@terminus.io
 * author: DreamYoung
 * Date: 2016-05-20
 */
@Data
public class DoctorGroupEvent<T extends BaseGroupInput> implements Serializable {
    private static final long serialVersionUID = 2651236908562482893L;

    private static final JsonMapperUtil JSON_MAPPER = JsonMapperUtil.nonEmptyMapper();
    private static final ToJsonMapper TO_JSON_MAPPER = ToJsonMapper.JSON_NON_EMPTY_MAPPER;

    private Long id;
    
    /**
     * 公司id
     */
    private Long orgId;
    
    /**
     * 公司名称
     */
    private String orgName;
    
    /**
     * 猪场id
     */
    private Long farmId;
    
    /**
     * 猪场名称
     */
    private String farmName;
    
    /**
     * 猪群卡片id
     */
    private Long groupId;
    
    /**
     * 猪群号
     */
    private String groupCode;

    /**
     * 原值
     */
    private Double origin;

    /**
     * 事件发生日期
     */
    private Date eventAt;
    
    /**
     * 事件类型 枚举 总共10种
     * @see io.terminus.doctor.event.enums.GroupEventType
     */
    private Integer type;
    
    /**
     * 事件名称 冗余枚举的name
     */
    private String name;
    
    /**
     * 事件描述
     */
    private String desc;
    
    /**
     * 事件发生猪舍id
     */
    private Long barnId;
    
    /**
     * 事件发生猪舍name
     */
    private String barnName;
    
    /**
     * 猪类枚举 9种
     * @see io.terminus.doctor.common.enums.PigType
     */
    private Integer pigType;
    
    /**
     * 事件猪只数
     */
    private Integer quantity;
    
    /**
     * 总活体重(公斤)
     */
    private Double weight;
    
    /**
     * 平均体重(公斤)
     */
    private Double avgWeight;

    /**
     * 销售基础重量
     * @see io.terminus.doctor.event.enums.SaleBaseWeight
     */
    private Integer baseWeight;

    /**
     * 平均日龄
     */
    private Integer avgDayAge;

    /**
     * 是否是自动生成的事件(用于区分是触发事件还是手工录入事件) 0 否, 1 是
     * @see io.terminus.doctor.event.enums.IsOrNot
     */
    private Integer isAuto;

    /**
     * 变动类型id
     */
    private Long changeTypeId;

    /**
     * 销售单价(分)
     */
    private Long price;

    /**
     * 销售总额(分)
     */
    private Long amount;

    /**
     * 超出价格(分/kg)
     */
    private Long overPrice;

    /**
     * 转群类型 0 内转(同阶段) 1 外转(不同阶段)
     */
    private Integer transGroupType;

    /**
     * 仔猪转入事件: 转入类型
     * @see io.terminus.doctor.event.enums.InType
     */
    private Integer inType;

    /**
     * 猪群转入转出事件的来源/目标id (转群事件: 目标猪舍id, 仔猪转入事件: 来源猪舍id, 转种猪事件: 目标猪舍id)
     */
    private Long otherBarnId;

    /**
     * 猪群转入转出事件的来源/目标猪舍类型
     * @see io.terminus.doctor.common.enums.PigType
     */
    private Integer otherBarnType;

    /**
     * 关联猪群事件id
     */
    private Long relGroupEventId;

    /**
     * 关联猪事件id
     */
    private Long relPigEventId;

    /**
     * 外部id
     */
    private String outId;

    /**
     * 状态
     * @see
     */
    private Integer status;

    private String statusName;

    /**
     * 备注
     */
    private String remark;

    /**
     * 具体事件的内容通过json存储
     */
    private String extra;

    /**
     * 有母猪触发的事件关联的猪id
     */
    private Long sowId;

    /**
     * 有母猪触发的事件关联的猪code
     */
    private String sowCode;

    /**
     * 销售时客户id
     */
    private Long customerId;

    /**
     * 销售时客户名
     */
    private String customerName;

    /**
     * 基础数据id(疾病id,防疫项目id)
     */
    private Long basicId;

    /**
     * 基础数据名(疾病,防疫)
     */
    private String basicName;

    /**
     * 防疫结果
     * @see io.terminus.doctor.event.enums.VaccinResult
     */
    private Integer vaccinResult;

    /**
     * 疫苗
     */
    private Long vaccinationId;

    /**
     * 疫苗名称
     */
    private String vaccinationName;

    /**
     * 操作人
     */
    private Long operatorId;

    /**
     * 操作人姓名
     */
    private String operatorName;

    /**
     * 具体事件转换成的实体类
     * @see BaseGroupInput
     */
    @Setter(AccessLevel.NONE)
    private T extraMap;

    /**
     * 创建时间
     */
    private Date createdAt;
    
    /**
     * 创建人id
     */
    private Long creatorId;

    /**
     * 创建人name
     */
    private String creatorName;

    /**
     * 更新信息
     */
    private Date updatedAt;
    private Long updatorId;
    private String updatorName;

    private Map<String, Object> extraData;

    /**
     * 开始时间
     */
    private String startDate;

    /**
     * 结束时间
     */
    private String endDate;

    /**
     * 事件来源
     * @see io.terminus.doctor.common.enums.SourceType
     */
    private Integer eventSource;

    /**
     * 是否是编辑事件(不存于数据库)
     */
    private Boolean isRollback;

    @SneakyThrows
    public void setExtraMap(T extraMap){
        this.extraMap = extraMap;
        if(extraMap == null){
            this.extra = null;
        }else {
            this.extra = TO_JSON_MAPPER.toJson(extraMap);
        }
    }

    public enum TransGroupType {
        IN(0, "内转"),
        OUT(1, "外转");

        @Getter
        private Integer value;
        @Getter
        private String desc;

        TransGroupType(Integer value, String desc){
            this.value = value;
            this.desc = desc;
        }

        public static TransGroupType from(String desc) {
            for (TransGroupType type : TransGroupType.values()) {
                if (Objects.equal(type.desc, desc)) {
                    return type;
                }
            }
            return null;
        }
    }
}