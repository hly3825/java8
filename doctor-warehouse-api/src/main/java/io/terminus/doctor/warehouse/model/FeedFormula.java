package io.terminus.doctor.warehouse.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import com.google.common.collect.Iterables;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.utils.JsonMapper;
import io.terminus.doctor.common.constants.JacksonType;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.experimental.Builder;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static java.util.Objects.isNull;

/**
 * Code generated by terminus code gen
 * Desc: 饲料配方表Model类
 * Date: 2016-09-26
 */
@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class FeedFormula implements Serializable {
    private static final long serialVersionUID = -7638873685503008738L;

    public static final ObjectMapper OBJECT_MAPPER = JsonMapper.JSON_NON_DEFAULT_MAPPER.getMapper();

    public static final Long DEFAULT_COUNT = 1000L;

    public static final Integer SCALE = 10; // 默认精度大小

    private static final Integer PERCENT_SCALE = 4; // percent 数据大小比例

    private Long id;
    
    /**
     * 饲料id
     */
    private Long feedId;
    
    /**
     * 饲料名称
     */
    private String feedName;
    
    /**
     * 猪场id
     */
    private Long farmId;
    
    /**
     * 猪场名称
     */
    private String farmName;
    
    /**
     * 配方json
     */
    @Setter(AccessLevel.NONE)
    @JsonIgnore
    private String formula;

    @Setter(AccessLevel.NONE)
    private Map<String,String> formulaMap;
    
    private Date createdAt;
    
    private Date updatedAt;

    @SneakyThrows
    public void setFormula(String formula){
        this.formula = formula;
        if(Strings.isNullOrEmpty(formula)){
            this.formulaMap = Collections.emptyMap();
        }else {
            this.formulaMap = OBJECT_MAPPER.readValue(formula, JacksonType.MAP_OF_OBJECT);
        }
    }

    @SneakyThrows
    public void setFormulaMap(Map<String,String> formulaMap){
        this.formulaMap = formulaMap;
        if(isNull(formulaMap) || Iterables.isEmpty(formulaMap.entrySet())){
            this.formula = null;
        }else {
            this.formula = OBJECT_MAPPER.writeValueAsString(formulaMap);
        }
    }

    public String getMaterialName(){
        return this.feedName;
    }
    public Map<String,String> getExtraMap(){
        return this.formulaMap;
    }

    public Integer getCanProduce(){
        return 1;
    }

    /**
     * 对应的物料生产信息( 原料, 药品 配比信息 )
     */
    @Builder
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class FeedProduce implements Serializable{

        private static final long serialVersionUID = 633401329050233302L;

        private Double total; //生产物料总量信息

        private List<MaterialProduceEntry> materialProduceEntries ; // 原料占比信息

        private List<MaterialProduceEntry> medicalProduceEntries; // 对应的药品比例信息

        // 计算合计数量
        public Double calculateTotalPercent(){
            this.total = 0D;
            if(!isNull(materialProduceEntries)){
                this.total += materialProduceEntries.stream().map(MaterialProduceEntry::getMaterialCount).reduce((a,b)->a+b).orElse(0D);
            }
            if(total.longValue() < FeedFormula.DEFAULT_COUNT){
                throw new JsonResponseException("input.totalMaterialCount.error");
            }else{
                total = FeedFormula.DEFAULT_COUNT.doubleValue();
            }

            BigDecimal totalDecimal = BigDecimal.valueOf(total);
            if(!isNull(materialProduceEntries)){
                materialProduceEntries.forEach(m->{
                    m.setPercent(BigDecimal.valueOf(m.getMaterialCount() * 100).divide(totalDecimal, PERCENT_SCALE, BigDecimal.ROUND_CEILING).doubleValue());
                });
            }
            return this.total;
        }

        public void calculatePercentByTotal(Double baseCount){
            if(!isNull(materialProduceEntries)){
                materialProduceEntries.forEach(m->{
                    m.setMaterialCount(
                            BigDecimal.valueOf(baseCount)
                                    .divide(BigDecimal.valueOf(total), SCALE, BigDecimal.ROUND_UP)
                                    .multiply(BigDecimal.valueOf(m.getMaterialCount())).doubleValue());
                });
            }

            if(!isNull(medicalProduceEntries)){
                medicalProduceEntries.forEach(m -> {
                    m.setMaterialCount(BigDecimal.valueOf(baseCount)
                            .divide(BigDecimal.valueOf(total), SCALE, BigDecimal.ROUND_UP)
                            .multiply(BigDecimal.valueOf(m.getMaterialCount())).doubleValue());
                });
            }

            this.total = baseCount;
        }
    }


    /**
     * 物料生产Entry
     */
    @Builder
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class MaterialProduceEntry implements Serializable{

        private static final long serialVersionUID = -5681942806422877951L;

        private Long materialId;    //原料Id

        private String materialName;    //  原料名称

        private Double materialCount; // 原料数量信息

        private Double percent; //原料配比信息
    }
}
