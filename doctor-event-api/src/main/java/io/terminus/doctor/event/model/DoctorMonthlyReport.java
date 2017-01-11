package io.terminus.doctor.event.model;

import io.terminus.common.utils.JsonMapper;
import io.terminus.doctor.event.dto.report.common.DoctorCommonReportDto;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Setter;
import lombok.SneakyThrows;
import org.springframework.util.StringUtils;

import java.io.Serializable;
import java.util.Date;

/**
 * Desc: 猪场月报表Model类
 * Mail: yangzl@terminus.io
 * author: DreamYoung
 * Date: 2016-08-11
 */
@Data
public class DoctorMonthlyReport implements Serializable {
    private static final long serialVersionUID = -6794869708968237462L;

    private static final JsonMapper MAPPER = JsonMapper.nonEmptyMapper();

    private Long id;
    
    /**
     * 猪场id
     */
    private Long farmId;
    
    /**
     * 月报数据，json存储
     * @see DoctorCommonReportDto
     */
    @Setter(AccessLevel.NONE)
    private String data;
    
    /**
     * 附加字段
     */
    private String extra;
    
    /**
     * 统计时间
     */
    private Date sumAt;
    
    /**
     * 创建时间(仅做记录创建时间，不参与查询)
     */
    private Date createdAt;

    /**
     * 月报data
     */
    @Setter(AccessLevel.NONE)
    private DoctorCommonReportDto reportDto;

    @SneakyThrows
    public void setReportDto(DoctorCommonReportDto reportDto) {
        this.reportDto = reportDto;
        if (reportDto == null) {
            this.data = "";
        } else {
            this.data = MAPPER.toJson(reportDto);
        }
    }

    @SneakyThrows
    public void setData(String data) {
        this.data = data;
        if (StringUtils.hasText(data)) {
            this.reportDto = MAPPER.fromJson(data, DoctorCommonReportDto.class);
        } else {
            this.reportDto = null;
        }
    }
}
