package io.terminus.doctor.event.search.pig;

import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import io.terminus.boot.rpc.common.annotation.RpcProvider;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import io.terminus.doctor.event.enums.PigStatus;
import io.terminus.search.api.Searcher;
import io.terminus.search.api.model.WithAggregations;
import io.terminus.search.api.query.Criterias;
import io.terminus.search.model.Bucket;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Desc: 猪(索引对象)查询服务
 * Mail: chk@terminus.io
 * Created by icemimosa
 * Date: 16/5/24
 */
@Service
@Slf4j
@RpcProvider
public class PigSearchReadServiceImpl implements PigSearchReadService {

    @Autowired
    private Searcher searcher;

    @Autowired
    private BasePigQueryBuilder basePigQueryBuilder;

    @Autowired
    private PigSearchProperties pigSearchProperties;

    @Override
    public Response<SearchedPigDto> searchWithAggs(Integer pageNo, Integer pageSize, String template, Map<String, String> params) {
        try{
            // 获取关键词, 设置高亮
            String q = params.get("q");
            if (StringUtils.isNotBlank(q)) {
                // 暂不做高亮处理
                // params.put("highlight", "pigCode");
            }

            // 获取存在的猪状态
            String aggs = params.get("aggs");
            if (StringUtils.isBlank(aggs)) {
                params.put("aggs", "aggs_sowStatus:status:0");
            }

            // 构建查询条件, 并查询
            Criterias criterias = basePigQueryBuilder.buildCriterias(pageNo, pageSize, params);
            WithAggregations<SearchedPig> searchedPigs = searcher.searchWithAggs(
                    pigSearchProperties.getIndexName(),
                    pigSearchProperties.getIndexType(),
                    template,
                    criterias,
                    SearchedPig.class
            );
            // 处理日龄
            searchedPigs.getData().forEach(searchedPig -> {
                if (searchedPig.getBirthDate() != null) {
                    searchedPig.setDayAge((int)(DateTime.now()
                            .minus(searchedPig.getBirthDate().getTime()).getMillis() / (1000 * 60 * 60 * 24) + 1));
                }
            });
            Paging<SearchedPig> paging = new Paging<>(searchedPigs.getTotal(), searchedPigs.getData());

            // 处理聚合属性
            List<SearchedPigDto.SowStatus> sowStatuses = Lists.newArrayList();
            Map<String, List<Bucket>> aggregations = searchedPigs.getAggregations();
            if (aggregations != null) {
                // 母猪状态
                aggregations.get("aggs_sowStatus").forEach(bucket ->
                        sowStatuses.add(SearchedPigDto.createStatus(
                                PigStatus.from(Integer.parseInt(bucket.getKey())))));
            }

            // 构建返回对象
            SearchedPigDto searchedPigDto = SearchedPigDto.builder()
                    .pigs(paging)
                    .sowStatuses(sowStatuses)
                    .build();

            return Response.ok(searchedPigDto);
        } catch (Exception e) {
            log.error("pig search failed, cause by {}", Throwables.getStackTraceAsString(e));
            return Response.fail("search.pig.fail");
        }
    }
}
