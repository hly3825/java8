package io.terminus.doctor.event.dto.event.usual;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Builder;

import java.io.Serializable;
import java.util.Date;

/**
 * Created by yaoqijun.
 * Date:2016-05-16
 * Email:yaoqj@terminus.io
 * Descirbe:
 */
@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class DoctorChgLocationDto implements Serializable{

    private static final long serialVersionUID = 8270765125209815779L;

    private Date changeLocationDate;

    private Long chgLocationFromBarnId;

    private String chgLocationFromBarnName;

    private Long chgLocationToBarnId;

    private String chgLocationToBarnName;
}
