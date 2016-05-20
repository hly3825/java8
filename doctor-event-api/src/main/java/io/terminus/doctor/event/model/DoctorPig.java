package io.terminus.doctor.event.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.Builder;

import java.io.Serializable;
import java.util.Date;
import java.util.Objects;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class DoctorPig implements Serializable{

    private static final long serialVersionUID = -5981942073814626473L;

    private Long id;

    private Long orgId;

    private String orgName;

    private Long farmId;

    private String farmName;

    private String outId;

    private String pigCode;

    private Integer pigType;

    private Long pigFatherId;

    private Long pigMotherId;

    private Integer source;

    private Date birthDate;

    private Double birthWeight;

    private Date inFarmDate;

    private Integer inFarmDayAge;

    private Long initBarnId;

    private String initBarnName;

    private Long breedId;

    private String breedName;

    private Long geneticId;

    private String geneticName;

    private String extra;

    private String remark;

    private Long creatorId;

    private String creatorName;

    private Long updatorId;

    private String updatorName;

    private Date createdAt;

    private Date updatedAt;

    /**
     * 猪类型信息表数据
     */
    public static enum PIG_TYPE{
        SOW(1, "母猪"),
        BOAR(2, "公猪"),
        LITTER(3, "仔猪");

        @Getter
        private Integer key;

        @Getter
        private String desc;

        private PIG_TYPE(Integer key, String desc){
            this.key = key;
            this.desc = desc;
        }

        public static PIG_TYPE from(Integer key){
            for(PIG_TYPE pig_type : PIG_TYPE.values()){
                if(Objects.equals(pig_type.getKey(), key)){
                    return pig_type;
                }
            }
            return null;
        }
    }
}