package com.yundasys.es.operation.helper;

import com.alibaba.fastjson.JSON;
import com.yundasys.es.operation.model.agg.AggCondition;
import com.yundasys.es.operation.model.agg.AggCriterion;
import com.yundasys.es.operation.constant.AggCriterionType;
import com.yundasys.es.operation.model.agg.AggResponseBucket;
import com.yundasys.es.operation.model.agg.AggResult;
import com.yundasys.es.operation.model.Hit;
import com.yundasys.es.operation.model.request.SearchResult;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.get.MultiGetItemResponse;
import org.elasticsearch.action.get.MultiGetResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.InternalSingleBucketAggregation;
import org.elasticsearch.search.aggregations.bucket.MultiBucketsAggregation;
import org.elasticsearch.search.aggregations.bucket.MultiBucketsAggregation.Bucket;
import org.elasticsearch.search.aggregations.metrics.ParsedAvg;
import org.elasticsearch.search.aggregations.metrics.ParsedMax;
import org.elasticsearch.search.aggregations.metrics.ParsedMin;
import org.elasticsearch.search.aggregations.metrics.ParsedSum;
import org.elasticsearch.search.aggregations.metrics.ParsedTopHits;
import org.elasticsearch.search.aggregations.metrics.ParsedValueCount;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class ESResponseHelper {
    /**
     * 处理docGet检索结果
     * @param response
     * @return
     */
    public SearchResult dealSearchResult(GetResponse response) {
        SearchResult searchResult = new SearchResult();
        List<Hit> hits = new ArrayList<>();
        
        if (response.isExists()) {
            searchResult.setTotal(1L);
            Hit hit = new Hit();
            hit.setValue(response.getSourceAsString());
            hit.setId(response.getId());
            hit.setVersion(response.getVersion());
            hits.add(hit);
        } else {
            searchResult.setTotal(0L);
        }

        searchResult.setHits(hits);

        return searchResult;
    }
    
    /**
     * 处理docMultiGet检索结果
     * @param response
     * @return
     */
    public SearchResult dealSearchResult(MultiGetResponse response) {
        SearchResult searchResult = new SearchResult();
        List<Hit> hits = new ArrayList<>();
        
        for (MultiGetItemResponse itemResponse : response.getResponses()) {
        	if (itemResponse.getResponse().isExists()) {
        		Hit hit = new Hit();
            	hit.setValue(itemResponse.getResponse().getSourceAsString());
                hit.setId(itemResponse.getResponse().getId());
                hit.setVersion(itemResponse.getResponse().getVersion());
                hits.add(hit);
        	}
        }
        searchResult.setHits(hits);
        searchResult.setTotal(hits.size());

        return searchResult;
    }
    
	/**
	 * 处理docQuery检索结果
	 * @param searchResponse
	 * @param aggConditions
	 * @return
	 */
    public SearchResult dealSearchResult(SearchResponse searchResponse, List<AggCondition> aggConditions) {
        SearchResult searchResult = new SearchResult();
        List<Hit> hits = new ArrayList<>();
        Map<String, AggResult> aggregations = new HashMap<>();

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
        
        Aggregations searchAggs = searchResponse.getAggregations();
        if (searchAggs != null && !CollectionUtils.isEmpty(aggConditions)) {
            aggregations = dealAggResponse(aggConditions, searchAggs);
        }
        
        searchResult.setTotal(searchResponse.getHits().getTotalHits().value);
        searchResult.setHits(hits);
        searchResult.setAggregations(aggregations);
        searchResult.setScrollId(searchResponse.getScrollId());

        return searchResult;
    }
    
    /**
     * 处理聚合结果
     * @param aggConditions
     * @param searchAggs
     * @return
     */
    private Map<String, AggResult> dealAggResponse(List<AggCondition> aggConditions, Aggregations searchAggs) {
        Map<String, AggResult> aggResults = new HashMap<>();
        for (AggCondition aggCondition : aggConditions) {
            String aggBucketName = aggCondition.getKey();
            switch (aggCondition.getAggBucket().getAggBucketType()) {
            case AGG_BUCKET_TERMS:
                // terms bucket
            case AGG_BUCKET_DATE_HISTOGRAM:
                // date histogram bucket
                AggResult multiBucketResult = convertMultiBucket2AggResult(aggCondition, searchAggs.get(aggBucketName));
                aggResults.put(aggBucketName, multiBucketResult);
                break;
            case AGG_BUCKET_FILTER:
                // filter bucket
            case AGG_BUCKET_GLOBAL:
                // global bucket
            	AggResult singleBucketResult = convertSingleBucket2AggResult(aggCondition, searchAggs.get(aggBucketName));
                aggResults.put(aggBucketName, singleBucketResult);
                break;
            case AGG_BUCKET_NONE:
                // none bucket--criteria only
            	AggResult noneBucketResult = convertNoneBucket2AggResult(aggCondition, searchAggs);
            	aggResults.put(aggBucketName, noneBucketResult);
                break;
            default:
                //
            	break;
            }
        }
        
        return aggResults;
    }
    
    /**
     * 抽取指标信息
     * @param aggCriteria
     * @param aggregations
     * @return
     */
    private Map<String, Aggregation> extractCriteria(List<AggCriterion> aggCriteria, Aggregations aggregations) {
    	Map<String, Aggregation> result = new HashMap<>();
    	for (AggCriterion aggCriterion : aggCriteria) {
    		result.put(aggCriterion.getKey(), aggregations.get(aggCriterion.getKey()));
    	}
    	
    	return result;
    }
    
    /**
     * 只有指标的桶
     * @param aggCriteria
     * @param aggregations
     * @return
     */
    private AggResult convertNoneBucket2AggResult(List<AggCriterion> aggCriteria, Map<String, Aggregation> aggregations) {
        AggResponseBucket aggBucket = new AggResponseBucket();
        Map<String, Object> result = parseAggResultFromMap(aggCriteria, aggregations);
        aggBucket.setResult(result);
        
        List<AggResponseBucket> aggBuckets = new ArrayList<>();
        aggBuckets.add(aggBucket);
        
        AggResult aggResult = new AggResult();
        aggResult.setBuckets(aggBuckets); // buckets
        return aggResult;
    }
    
    /**
     * 单值聚合结果转换
     * @param aggCondition
     * @param singleBucketAggregation
     * @return
     */
    private AggResult convertSingleBucket2AggResult(AggCondition aggCondition, InternalSingleBucketAggregation singleBucketAggregation) {
    	AggResult aggResult = new AggResult();
        
    	// AggResponseBucket由3部分组成：key, result, innerAggResults
        AggResponseBucket aggBucket = new AggResponseBucket();
        
        // key
        aggBucket.setKey(singleBucketAggregation.getName());
        
        // result
    	Aggregations aggregations = singleBucketAggregation.getAggregations();
    	if (!CollectionUtils.isEmpty(aggCondition.getAggCriteria())) {
    		Map<String, Aggregation> criteriaResult = extractCriteria(aggCondition.getAggCriteria(), aggregations); // 抽取指标信息
            Map<String, Object> result = parseAggResultFromMap(aggCondition.getAggCriteria(), criteriaResult); // 指标信息结果
            aggBucket.setResult(result);
    	}
        
        // innnerAggResults
        if (!CollectionUtils.isEmpty(aggCondition.getInnerAggConditions())) {
        	// 内部桶
        	Map<String, AggResult> innerAggResults = parseInnerAggResults(aggCondition, aggregations);
            aggBucket.setInnerAggResults(innerAggResults);
        }
        
        List<AggResponseBucket> aggBuckets = new ArrayList<>();
        aggBuckets.add(aggBucket);
        aggResult.setBuckets(aggBuckets); // buckets
        
        return aggResult;
    }
    
    /**
     * 只有指标的桶处理
     * @param aggCondition
     * @param aggregations
     * @return
     */
    private AggResult convertNoneBucket2AggResult(AggCondition aggCondition, Aggregations aggregations) {
    	AggResult aggResult = new AggResult();
        
    	// AggResponseBucket由3部分组成：key, result, innerAggResults
        AggResponseBucket aggBucket = new AggResponseBucket();
        
        // key
        aggBucket.setKey(aggCondition.getKey());
        
        // result
        if (!CollectionUtils.isEmpty(aggCondition.getAggCriteria())) {
    		Map<String, Aggregation> criteriaResult = extractCriteria(aggCondition.getAggCriteria(), aggregations); // 抽取指标信息
            Map<String, Object> result = parseAggResultFromMap(aggCondition.getAggCriteria(), criteriaResult); // 指标信息结果
            aggBucket.setResult(result);
    	}
        
        // innnerAggResults
        if (!CollectionUtils.isEmpty(aggCondition.getInnerAggConditions())) {
        	// 内部桶
            Map<String, AggResult> innerAggResults = parseInnerAggResults(aggCondition, aggregations);
            aggBucket.setInnerAggResults(innerAggResults);
        }
        
        List<AggResponseBucket> aggBuckets = new ArrayList<>();
        aggBuckets.add(aggBucket);
        aggResult.setBuckets(aggBuckets); // buckets
        
        return aggResult;
    }
    
    /**
     * 处理内部桶
     * @param aggCondition
     * @param aggregations
     * @return
     */
    private Map<String, AggResult> parseInnerAggResults(AggCondition aggCondition, Aggregations aggregations) {
    	Map<String, AggResult> innerAggResults = new HashMap<>();
        for (AggCondition innerAggCondition : aggCondition.getInnerAggConditions()) {
            switch (innerAggCondition.getAggBucket().getAggBucketType()) {
            case AGG_BUCKET_TERMS:
                // terms bucket
            case AGG_BUCKET_DATE_HISTOGRAM:
                // global bucket
            	AggResult innerMultiBucketResult = convertMultiBucket2AggResult(innerAggCondition, aggregations.get(innerAggCondition.getKey()));
                innerAggResults.put(innerAggCondition.getKey(), innerMultiBucketResult);
                break;
            case AGG_BUCKET_FILTER:
                // filter bucket
            case AGG_BUCKET_GLOBAL:
                // global bucket
                AggResult innerSingleBucketResult = convertSingleBucket2AggResult(innerAggCondition, aggregations.get(innerAggCondition.getKey()));
                innerAggResults.put(innerAggCondition.getKey(), innerSingleBucketResult);
                break;
            case AGG_BUCKET_NONE:
                // none bucket--criteria only
                AggResult innerNoneBucketResult = convertNoneBucket2AggResult(innerAggCondition.getAggCriteria(), aggregations.get(innerAggCondition.getKey()));
                innerAggResults.put(innerAggCondition.getKey(), innerNoneBucketResult);
                break;
            default:
                //
            	break;
            }
        }
        
        return innerAggResults;
    }
    
    /**
     * 多值聚合结果处理
     * @param aggCondition
     * @param aggregation
     * @return
     */
    private AggResult convertMultiBucket2AggResult(AggCondition aggCondition, MultiBucketsAggregation aggregation) {
        AggResult aggResult = new AggResult();
        List<AggResponseBucket> aggBuckets = new ArrayList<>();
        aggResult.setBuckets(aggBuckets); // buckets
        
        List<? extends Bucket> buckets = aggregation.getBuckets();
        if (buckets != null) {
            for (Bucket bucket : buckets) {
                // convert terms result--bucket to aggBucket
                aggBuckets.add(createAggBucket(bucket, aggCondition));
            }
        }

        return aggResult;
    }
    
    /**
     * 多值聚合结果单个桶处理
     * @param bucket
     * @param aggCondition
     * @return
     */
    private AggResponseBucket createAggBucket(Bucket bucket, AggCondition aggCondition) {
        // convert ES agg result--bucket to aggBucket
        //List<AggCriterion> aggCriteria = aggCondition.getAggCriteria();
        
        // AggResponseBucket由3部分组成：key, result, innerAggResults
        AggResponseBucket aggBucket = new AggResponseBucket();
        
        // key
        aggBucket.setKey(bucket.getKeyAsString());
        
        // result
        Aggregations aggregations = bucket.getAggregations();
        if (!CollectionUtils.isEmpty(aggCondition.getAggCriteria())) {
    		Map<String, Aggregation> criteriaResult = extractCriteria(aggCondition.getAggCriteria(), aggregations); // 抽取指标信息
            Map<String, Object> result = parseAggResultFromMap(aggCondition.getAggCriteria(), criteriaResult); // 指标信息结果
            aggBucket.setResult(result);
    	}
        
        // innnerAggResults
        if (!CollectionUtils.isEmpty(aggCondition.getInnerAggConditions())) {
        	// 内部桶
        	Map<String, AggResult> innerAggResults = parseInnerAggResults(aggCondition, aggregations);
            aggBucket.setInnerAggResults(innerAggResults);
        }
        
        return aggBucket;
    }
    
    /**
     * 处理指标
     * @param aggCriteria
     * @param map
     * @return
     */
    private Map<String, Object> parseAggResultFromMap(List<AggCriterion> aggCriteria, Map<String, Aggregation> map) {
        if (CollectionUtils.isEmpty(aggCriteria)) {
            return null;
        }
        
        Map<String, Object> result = new HashMap<>();
        for (AggCriterion aggCriterion : aggCriteria) {
            // get each agg field
            String aggName = aggCriterion.getKey();
            AggCriterionType aggType = aggCriterion.getCriterionType();
            switch (aggType) {
                case AGG_CRITERION_SUM:
                    ParsedSum sum = (ParsedSum) map.get(aggName);
                    result.put(sum.getName(), sum.getValue());
                    break;
                case AGG_CRITERION_COUNT:
                    ParsedValueCount count = (ParsedValueCount) map.get(aggName);
                    result.put(count.getName(), count.getValue());
                    break;
                case AGG_CRITERION_MAX:
                    ParsedMax max = (ParsedMax) map.get(aggName);
                    result.put(max.getName(), max.getValue());
                    break;
                case AGG_CRITERION_MIN:
                    ParsedMin min = (ParsedMin) map.get(aggName);
                    result.put(min.getName(), min.getValue());
                    break;
                case AGG_CRITERION_AVG:
                    ParsedAvg avg = (ParsedAvg) map.get(aggName);
                    result.put(avg.getName(), avg.getValue());
                    break;
                case AGG_CRITERION_TOP_HITS:
                    ParsedTopHits topHits = (ParsedTopHits) map.get(aggName);
                    SearchHit[] searchHits = topHits.getHits().getHits();
                    if (searchHits != null) {
                        List<Hit> hits = new ArrayList<>();
                        for (SearchHit searchHit : searchHits) {
                        	Hit hit = new Hit();
                        	hit.setId(searchHit.getId());
                            hit.setValue(JSON.toJSONString(searchHit.getSourceAsString()));
                        	hit.setVersion(searchHit.getVersion());
                        	hits.add(hit);
                        }
                        result.put(topHits.getName(), hits);
                    }
                    break;
                default:
                    break;
            }
        }
        
        return result;
    }

}
