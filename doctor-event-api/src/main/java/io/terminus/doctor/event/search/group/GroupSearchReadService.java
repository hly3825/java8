package io.terminus.doctor.event.search.group;

import io.terminus.common.model.Response;

import java.util.Map;

/**
 * Desc: 猪群(索引对象)查询服务
 * Mail: chk@terminus.io
 * Created by icemimosa
 * Date: 16/5/24
 */
public interface GroupSearchReadService {

    /**
     * 公共搜索方法
     * @param pageNo        页码
     * @param pageSize      页大小
     * @param template      模板路径
     * @param params        查询参数
     * @return
     */
    Response<SearchedGroupDto> searchWithAggs(Integer pageNo, Integer pageSize, String template, Map<String, String> params);

}
