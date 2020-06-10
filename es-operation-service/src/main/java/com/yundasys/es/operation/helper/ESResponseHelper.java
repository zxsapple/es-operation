package com.yundasys.es.operation.helper;

import com.alibaba.fastjson.JSONObject;
import com.yundasys.es.operation.constant.AggCriterionType;
import com.yundasys.es.operation.constant.ESErrorCode;
import com.yundasys.es.operation.constant.ElasticConstant;
import com.yundasys.es.operation.exception.ESOperationException;
import com.yundasys.es.operation.model.Hit;
import com.yundasys.es.operation.model.request.SearchResult;
import com.yundasys.es.operation.model.agg.AggCondition;
import com.yundasys.es.operation.model.agg.AggDateCondition;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.ArrayUtils;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.histogram.Histogram;
import org.elasticsearch.search.aggregations.bucket.histogram.ParsedDateHistogram;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedStringTerms;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.metrics.ParsedAvg;
import org.elasticsearch.search.aggregations.metrics.ParsedMax;
import org.elasticsearch.search.aggregations.metrics.ParsedMin;
import org.elasticsearch.search.aggregations.metrics.ParsedSum;
import org.elasticsearch.search.aggregations.metrics.ParsedValueCount;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
/**
 * @author zhengxiaosu
 * @desc es 返回数据处理helper
 * @date 2020/6/3 14:29
 */
@Component
@Slf4j
public class ESResponseHelper {

	/**
	 * 处理docQuery检索结果
	 */
    public SearchResult dealSearchResult(SearchResponse searchResponse, AggDateCondition aggDateCondition) {
        SearchResult searchResult = new SearchResult();
        List<Hit> hits = new ArrayList<>();

        if (searchResponse.getHits().getTotalHits().value > 0) {
            SearchHits searchHints = searchResponse.getHits();
            if (searchHints != null) {
                for (SearchHit searchHit : searchHints) {
                	Hit hit = new Hit();
                	hit.setValue(searchHit.getSourceAsString());
                    hit.setId(searchHit.getId());
                    hit.setVersion(searchHit.getVersion());
                    hits.add(hit);
                }
            }
        }


        searchResult.setTotal(searchResponse.getHits().getTotalHits().value);
        searchResult.setHits(hits);
        searchResult.setAggregations( dealAggResponse(aggDateCondition, searchResponse.getAggregations()));
        searchResult.setScrollId(searchResponse.getScrollId());

        return searchResult;
    }


    /****************************  下面是处理agg信息  ****************************/

    
    /**
     * 处理聚合结果
     */
    private List<JSONObject> dealAggResponse(AggDateCondition aggDateCondition, Aggregations searchAggs) {
        if (aggDateCondition == null) {
            return null;
        }

        List<JSONObject> dateHistogramResp = new ArrayList<>();
        AggCondition aggCondition = aggDateCondition.getAggCondition();
        if (aggDateCondition.getDateField() != null) {//有时间汇总


            Map<String, Aggregation> asMap = searchAggs.getAsMap();
            ParsedDateHistogram parsedDateHistogram = (ParsedDateHistogram) (asMap.get(aggDateCondition.getDateField() + ElasticConstant.FIELD_CAL_SUFFIX));
            List<? extends Histogram.Bucket> buckets = parsedDateHistogram.getBuckets();
            for (Histogram.Bucket bucket : buckets) {
                JSONObject dateSingleResult = new JSONObject();
                dateSingleResult.put(aggDateCondition.getDateField(), bucket.getKey());
                dateSingleResult.put("docCount", bucket.getDocCount());


                for (Aggregation aggregation : bucket.getAggregations().asList()) {
                    if (aggregation instanceof ParsedStringTerms) {//填充内部聚合结果
                        List<JSONObject> innerDatas = dealGroupBy(aggCondition, (ParsedStringTerms) aggregation);
                        dateSingleResult.put(ElasticConstant.DATE_HISTOGRAM_JSON_INNER_KEY, innerDatas);
                    } else {
                        dealParsedNum(dateSingleResult, aggregation);
                    }

                }

                dateHistogramResp.add(dateSingleResult);
            }
            return dateHistogramResp;
        } else if (aggCondition != null && !ArrayUtils.isEmpty(aggCondition.getCalFields())) {//没有时间汇总

            JSONObject noneBucketData = new JSONObject();
            for (Aggregation aggregation : searchAggs.asList()) {
                if (aggregation instanceof ParsedStringTerms) {//填充内部聚合结果 有group by字段
                    return dealGroupBy(aggCondition, (ParsedStringTerms) aggregation);
                } else if (aggregation instanceof ParsedSum) {//没有group by字段
                    dealParsedNum(noneBucketData, aggregation);
                } else {
                    log.error("agg处理结果是发现未知类型");
                    throw new RuntimeException("不知道什么类型");
                }
            }
            return Collections.singletonList(noneBucketData);
        }

        return null;//到这里说明没有汇总
    }


    /**
     * 处理普通group by 聚合信息 可在date里嵌套
     */
    private List<JSONObject> dealGroupBy(AggCondition aggCondition, ParsedStringTerms aggregation) {
        List<? extends Terms.Bucket> innerBuckets = aggregation.getBuckets();
        List<JSONObject> innerDatas = new ArrayList<>(innerBuckets.size());
        for (Terms.Bucket innerBucket : innerBuckets) {
            String groupByFields = String.valueOf(innerBucket.getKey());
            String[] fieldsValue = groupByFields.split(ElasticConstant.SCRIPT_MULTI_GROUP_BY_SPLIT);

            JSONObject innerData = new JSONObject();
            innerDatas.add(innerData);
            for (int i = 0; i < fieldsValue.length; i++) {//封装group by的字段对应值
                innerData.put(aggCondition.getGroupByFields()[i], fieldsValue[i]);
            }
            for (Aggregation innerAggregation : innerBucket.getAggregations().asList()) {
                dealParsedNum(innerData, innerAggregation);
            }

        }
        return innerDatas;
    }

    /**
     * 处理数字类型的汇总信息
     */
    private void dealParsedNum(JSONObject noneBucketData, Aggregation aggregation) {
        double value;
        switch (AggCriterionType.getTypeByName(aggregation.getType())) {
            case AGG_CRITERION_SUM:
                value = ((ParsedSum) aggregation).getValue();
                break;
            case AGG_CRITERION_COUNT:
                value = ((ParsedValueCount) aggregation).getValue();
                break;
            case AGG_CRITERION_MAX:
                value = ((ParsedMax) aggregation).getValue();
                break;
            case AGG_CRITERION_MIN:
                value = ((ParsedMin) aggregation).getValue();
                break;
            case AGG_CRITERION_AVG:
                value = ((ParsedAvg) aggregation).getValue();
                break;
            default:
                throw new ESOperationException(ESErrorCode.PARAMETER_INCORRECT, "agg 聚合未知聚合类型");
        }

        String name = aggregation.getName();
        noneBucketData.put(name.substring(0, name.length() - ElasticConstant.FIELD_CAL_SUFFIX.length()), value);

    }

}
