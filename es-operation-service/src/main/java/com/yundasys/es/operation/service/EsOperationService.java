package com.yundasys.es.operation.service;

import com.yundasys.es.operation.exception.ESOperationException;
import com.yundasys.es.operation.helper.ESBuilderHelper;
import com.yundasys.es.operation.helper.ESResponseHelper;
import com.yundasys.es.operation.constant.ESErrorCode;
import com.yundasys.es.operation.model.IndexInfo;
import com.yundasys.es.operation.model.request.SearchByConditionRequest;
import com.yundasys.es.operation.model.request.SearchCondition;
import com.yundasys.es.operation.model.request.SearchResult;
import com.yundasys.es.operation.model.Sort;
import com.yundasys.es.operation.constant.SortType;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * @author zhengxiaosu
 * @desc
 * @date 2020/6/2 16:41
 */
@Component
@Slf4j
public class EsOperationService{
    @Value("${elasticsearch.scroll.scrollSort:false}")
    private boolean scrollSort; //游标查询是否可以排序
    @Autowired
    private RestHighLevelClient restHighLevelClient;
    @Autowired
    private ESBuilderHelper esBuilderHelper;
    @Autowired
    private ESResponseHelper esResponseHelper;

    public SearchResult docQuery(SearchByConditionRequest request) {
        IndexInfo indexInfo = request.getIndexInfo();
        SearchCondition condition = request.getSearchCondition();
        boolean useScroll = request.isUseScroll();

        QueryBuilder queryBuilder = esBuilderHelper.createCompoundBoolQueryBuilder(indexInfo, condition.getCompoundCriteria());

        int from = (condition.getPageNo() - 1) * condition.getPageSize();
        int size = condition.getPageSize();
        SearchRequest searchRequest = new SearchRequest(indexInfo.getIndexes());

        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        sourceBuilder.query(queryBuilder);
        sourceBuilder.from(from).size(size).version(true).explain(false);
        searchRequest.source(sourceBuilder);
        if (useScroll) {
            searchRequest.scroll(new TimeValue(2, TimeUnit.MINUTES));
        }
        // agg parameter

        esBuilderHelper.buildAggs(indexInfo, condition.getAggDateCondition(), sourceBuilder);
        // return fields
        if (condition.getIncludes() != null) {
            sourceBuilder.fetchSource(condition.getIncludes(), null);
        }

        // sort
        if (!useScroll || scrollSort) {
            if (condition.getSorts() != null) {
                for (Sort sort : condition.getSorts()) {
                    SortOrder sortOrder = sort.getType().equals(SortType.DESC) ? SortOrder.DESC : SortOrder.ASC;
                    sourceBuilder.sort(sort.getField(), sortOrder);
                }
            }
        }



        // do query
        SearchResponse searchResponse;
        try {
            log.info("查询es语句:\n{}", sourceBuilder);
            searchResponse = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);
        } catch (Exception e) {
            log.error("docSearch 条件查询es错误:{} useScroll:{} e:{}", sourceBuilder.toString(), useScroll, e);
            throw new ESOperationException(ESErrorCode.ES_OPERATE_FAIL, e.getMessage());
        }

        return esResponseHelper.dealSearchResult(searchResponse, condition.getAggDateCondition());
    }
}
