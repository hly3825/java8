package io.terminus.doctor.user.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import io.terminus.common.utils.JsonMapper;
import io.terminus.parana.common.constants.JacksonType;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Setter;
import lombok.SneakyThrows;

import java.io.Serializable;
import java.util.Collections;
import java.util.Date;
import java.util.Map;

/**
 * 猪场子账号
 *
 * @author houly
 */
@Data
public class Sub implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final ObjectMapper OBJECT_MAPPER = JsonMapper.nonEmptyMapper().getMapper();

    /**
     * 主键 ID
     */
    private Long id;

    /**
     * 子账号用户 ID
     */
    private Long userId;

    /**
     * 子账号用户名
     */
    private String userName;

    /**
     * 子账号(员工)真实名, 这是个冗余字段, 跟随 user_profile 表的 real_name 字段
     */
    private String realName;

    /**
     * 主账号 ID
     */
    private Long parentUserId;

    /**
     * 主账号名称
     */
    private String parentUserName;

    /**
     * 角色 ID
     */
    private Long roleId;

    /**
     * 角色名 (冗余)
     */
    private String roleName;

    /**
     * 联系方式
     */
    private String contact;

    /**
     *  0: 未生效(冻结), 1: 生效, -1: 删除
     */
    private Integer status;

    public boolean isActive() {
        return status != null && status == 1;
    }

    /**
     * 扩展信息, 不存数据库
     */
    @Setter(AccessLevel.NONE)
    private Map<String, String> extra;

    /**
     * 扩展信息 JSON, 存数据库
     */
    @Setter(AccessLevel.NONE)
    @JsonIgnore
    private String extraJson;

    /**
     * 创建时间
     */
    private Date createdAt;

    /**
     * 更新时间
     */
    private Date updatedAt;

    @SneakyThrows
    public void setExtra(Map<String, String> extra) {
        this.extra = extra;
        if (extra == null || extra.isEmpty()) {
            this.extraJson = null;
        } else {
            this.extraJson = OBJECT_MAPPER.writeValueAsString(extra);
        }
    }

    @SneakyThrows
    public void setExtraJson(String extraJson) {
        this.extraJson = extraJson;
        if (Strings.isNullOrEmpty(extraJson)) {
            this.extra = Collections.emptyMap();
        } else {
            this.extra = OBJECT_MAPPER.readValue(extraJson, JacksonType.MAP_OF_STRING);
        }
    }
}
