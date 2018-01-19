package io.terminus.doctor.web.core.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Desc: 猪的统计信息
 * Mail: yangzl@terminus.io
 * author: DreamYoung
 * Date: 16/5/25
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class DoctorStatisticDto implements Serializable {
    private static final long serialVersionUID = 4470661541591381247L;

    /**
     * 统计类型
     * @see PigType
     */
    private String type;

    /**
     * 统计结果
     */
    private Integer quantity;


    public enum PigType {

        SOW("母猪存栏量（头）", "母猪"),
        BOAR("公猪存栏量（头）", "公猪"),
        FARROW_PIGLET("产房仔猪存栏量（头）", "产房仔猪"),
        NURSERY_PIGLET("保育猪存栏量（头）", "保育猪"),
        FATTEN_PIG("育肥猪存栏量（头）", "育肥猪"),
        BREEDING_PIG("育种猪存栏量（头）", "育种猪"),
        HOUBEI("后备猪存栏量（头）", "后备猪"),
        PEIHUAI("配怀猪存栏量（头）", "配怀猪");

        @Getter
        private final String desc;
        @Getter
        private final String cutDesc;

        PigType(String desc, String cutDesc) {
            this.desc = desc;
            this.cutDesc = cutDesc;
        }
    }
}
