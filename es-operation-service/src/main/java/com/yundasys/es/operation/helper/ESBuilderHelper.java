package com.yundasys.es.operation.helper;

import com.alibaba.fastjson.JSON;
import com.yundasys.es.operation.constant.AggInnerField;
import com.yundasys.es.operation.constant.DefaultConstant;
import com.yundasys.es.operation.constant.ESErrorCode;
import com.yundasys.es.operation.constant.ElasticConstant;
import com.yundasys.es.operation.constant.SearchLogic;
import com.yundasys.es.operation.constant.SortType;
import com.yundasys.es.operation.exception.ESOperationException;
import com.yundasys.es.operation.model.IndexInfo;
import com.yundasys.es.operation.model.Range;
import com.yundasys.es.operation.model.Sort;
import com.yundasys.es.operation.model.TopHits;
import com.yundasys.es.operation.model.TopHitsAggCriterion;
import com.yundasys.es.operation.model.agg.AggBucket;
import com.yundasys.es.operation.model.agg.AggCondition;
import com.yundasys.es.operation.model.agg.AggCriterion;
import com.yundasys.es.operation.model.agg.AggDateHistogramBucket;
import com.yundasys.es.operation.model.agg.AggFilterBucket;
import com.yundasys.es.operation.model.agg.AggTermsBucket;
import com.yundasys.es.operation.model.request.BaseCondition;
import com.yundasys.es.operation.model.request.CompoundCriteria;
import com.yundasys.es.operation.model.request.Criteria;
import com.yundasys.es.operation.model.request.SearchByConditionRequest;
import com.yundasys.es.operation.model.request.SearchCondition;
import com.yundasys.es.operation.model.request.SearchField;
import com.yundasys.es.operation.util.DateUtil;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.elasticsearch.script.Script;
import org.elasticsearch.script.ScriptType;
import org.elasticsearch.search.aggregations.AbstractAggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.BucketOrder;
import org.elasticsearch.search.aggregations.bucket.filter.FilterAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.global.GlobalAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.histogram.DateHistogramAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.histogram.DateHistogramInterval;
import org.elasticsearch.search.aggregations.bucket.histogram.ExtendedBounds;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.elasticsearch.search.aggregations.metrics.TopHitsAggregationBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.SortBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class ESBuilderHelper {

    private Logger logger = LoggerFactory.getLogger(getClass());

    private static final String SCRIPT_UPD_PREFIX = "ctx._source.";
    private static final String SCRIPT_FIELD_VALUE_PREFIX = "doc['";
    private static final String SCRIPT_FIELD_VALUE_SUFFIX = "'].value";
    private static final String SCRIPT_FIELD_EQUALS_SYMBOL = " == ";
    private static final String SCRIPT_FIELD_NOT_EQUALS_SYMBOL = " != ";
    private static final String SCRIPT_MULTI_GROUP_BY_CONNECT_SYMBOL = " + '-' + ";
    private static final String SCRIPT_MULTI_GROUP_BY_CONNECT_PLUS_PREFIX = " + '";
    private static final String SCRIPT_MULTI_GROUP_BY_CONNECT_PLUS_SUFFIX = "' + ";
    private static final String SCRIPT_FIELD_GREATER_THAN_SYMBOL = " > ";
    private static final String SCRIPT_FIELD_GREATER_THAN_EQUALS_SYMBOL = " >= ";
    private static final String SCRIPT_FIELD_LESS_THAN_SYMBOL = " < ";
    private static final String SCRIPT_FIELD_LESS_THAN_EQUALS_SYMBOL = " <= ";
    
    /**
     * build bool query builder
     * @param indexInfo
     * @param searchFields
     * @return
     * @throws ESOperationException
     */
    private BoolQueryBuilder createBoolQueryBuilder(IndexInfo indexInfo, List<SearchField> searchFields)  {
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();

        if (!CollectionUtils.isEmpty(searchFields)) {
            for (SearchField searchField : searchFields) {
                QueryBuilder queryBuilder = singleFieldQueryBuilder(indexInfo, searchField);
                boolQueryBuilder(searchField.getSearchLogic(), queryBuilder, boolQueryBuilder);
            }
        }
        
        return boolQueryBuilder;
    }

    /**
     * build compound bool query builder
     * @param indexInfo
     * @param compoundCriteria
     * @return
     * @throws ESOperationException
     */
    public BoolQueryBuilder createCompoundBoolQueryBuilder(IndexInfo indexInfo, CompoundCriteria compoundCriteria) throws ESOperationException {
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();

        if (compoundCriteria != null && !CollectionUtils.isEmpty(compoundCriteria.getOredCriteria())) {
        	BoolQueryBuilder oredBoolQueryBuilder = QueryBuilders.boolQuery();
        	for (Criteria criteria : compoundCriteria.getOredCriteria()) {
        		if (!CollectionUtils.isEmpty(criteria.getCriteria())) {
        			BoolQueryBuilder subBoolQueryBuilder = createBoolQueryBuilder(indexInfo, criteria.getCriteria());
                    oredBoolQueryBuilder.should(subBoolQueryBuilder);
        		}
                /*BoolQueryBuilder subBoolQueryBuilder = createBoolQueryBuilder(indexInfo, criteria.getCriteria());
                oredBoolQueryBuilder.should(subBoolQueryBuilder);*/
            }

        	boolQueryBuilder.filter(oredBoolQueryBuilder);
        }
        
        if (compoundCriteria != null && !CollectionUtils.isEmpty(compoundCriteria.getExtendOredCriteria())) {
        	BoolQueryBuilder extendOredBoolQueryBuilder = QueryBuilders.boolQuery();
        	for (Criteria criteria : compoundCriteria.getExtendOredCriteria()) {
                if (!CollectionUtils.isEmpty(criteria.getCriteria())) {
                    BoolQueryBuilder subBoolQueryBuilder = createBoolQueryBuilder(indexInfo, criteria.getCriteria());
                    extendOredBoolQueryBuilder.must(subBoolQueryBuilder);
                }
            }
        	boolQueryBuilder.filter(extendOredBoolQueryBuilder);
        }
        
        return boolQueryBuilder;
    }
    
    /**
     * build aggs
     * @param aggFields
     * @param searchRequestBuilder
     * @throws ESOperationException
     */
    public void buildAggs(IndexInfo indexInfo, List<AggCondition> aggConditions, SearchSourceBuilder searchSourceBuilder)  {
        if (!CollectionUtils.isEmpty(aggConditions)) {
            for (AggCondition aggCondition : aggConditions) {
                List<AbstractAggregationBuilder<?>> aggregationBuilders = buildSingleAgg(indexInfo, aggCondition, 1);
                for (AbstractAggregationBuilder<?> aggregationBuilder : aggregationBuilders) {
                    searchSourceBuilder.aggregation(aggregationBuilder);
                }
            }
        }
    }
    
    /**
     * build update script
     * @param mapObj
     * @return
     */
    public Script buildUpdScript(Map<String, Object> mapObj) {
        Map<String, Object> newMapObj = new HashMap<String, Object>();
        StringBuilder sb = new StringBuilder();
        
        for (String key : mapObj.keySet()) {
            newMapObj.put(SCRIPT_UPD_PREFIX + key, mapObj.get(key));
            sb.append(SCRIPT_UPD_PREFIX).append(key).append("=").append(mapObj.get(key)).append(";");
        }
        
        Script script = new Script(sb.toString());
        
        return script;
    }
    
    /**
     * build search request--for search total size
     * @param downloadRequest
     * @return
     */
    public SearchByConditionRequest buildSizeSearchRequest(IndexInfo indexInfo, BaseCondition condition) {
    	SearchByConditionRequest searchRequest = new SearchByConditionRequest();
        searchRequest.setIndexInfo(indexInfo);
        
        SearchCondition newCondition = new SearchCondition();
        BeanUtils.copyProperties(condition, newCondition);
        newCondition.setPageNo(1);
        newCondition.setPageSize(0);
        newCondition.setIncludes(new String[] {});
        newCondition.setSorts(null);
        newCondition.setAggConditions(null);
        searchRequest.setSearchCondition(newCondition);
        
        return searchRequest;
    }
    
    private void boolQueryBuilder(SearchLogic searchLogic, QueryBuilder queryBuilder,
                                  BoolQueryBuilder boolQueryBuilder) throws ESOperationException {
        switch (searchLogic) {
        case SL_MUST:
            boolQueryBuilder.must(queryBuilder);
            break;
        case SL_SHOULD:
            boolQueryBuilder.should(queryBuilder);
            break;
        case SL_MUST_NOT:
            boolQueryBuilder.mustNot(queryBuilder);
            break;
        case SL_FILTER:
            boolQueryBuilder.filter(queryBuilder);
            break;
        default:
            logger.error("Unsupported SearchLogic");
            throw new ESOperationException(ESErrorCode.ES_OPERATE_FAIL, "Unsupported SearchLogic");
        }
    }
    
    private QueryBuilder singleFieldQueryBuilder(IndexInfo indexInfo, SearchField searchField) throws ESOperationException {
    	validate(searchField);
        QueryBuilder queryBuilder = null;
        switch (searchField.getSearchType()) {
        case ST_MATCHALL:
            queryBuilder = QueryBuilders.matchAllQuery();
            break;
        case ST_RANGE:
            queryBuilder = buildRangeQuery(searchField);
            break;
        case ST_TERM:
            queryBuilder = QueryBuilders.termQuery(searchField.getName(), searchField.getValue()).boost(searchField.getBoost());
            break;
        case ST_TERMS:
            queryBuilder = buildTermsQuery(searchField);
            break;
        case ST_QUERY_STRING:
            queryBuilder = QueryBuilders
                    .queryStringQuery(String.valueOf(searchField.getValue())).field(searchField.getName())
                    .boost(searchField.getBoost());
            break;
        case ST_MATCH:
            queryBuilder = QueryBuilders.matchQuery(searchField.getName(), searchField.getValue())
                    .boost(searchField.getBoost());
            break;
        case ST_MULTI_MATCH:
            queryBuilder = QueryBuilders.multiMatchQuery(searchField.getValue(), searchField.getNames())
                    .boost(searchField.getBoost());
            break;
        case ST_WILDCARD:
            queryBuilder = QueryBuilders.wildcardQuery(searchField.getName(),
                    String.valueOf(searchField.getValue())).boost(searchField.getBoost());
            break;
        case ST_IDS:
            String[] ids;
            if (!searchField.getValue().getClass().isArray()) {
                ids = JSON.parseObject(JSON.toJSONString(searchField.getValue()), String[].class);
            } else {
                ids = (String[]) searchField.getValue();
            }
            queryBuilder = QueryBuilders.idsQuery().addIds(ids).boost(searchField.getBoost());
            break;
        case ST_EXISTS:
        	queryBuilder = buildExistQuery(searchField);
            break;
        case ST_MISSING:
        	queryBuilder = buildMissingQuery(searchField);
            break;
        case ST_PREFIX:
            queryBuilder = QueryBuilders.prefixQuery(searchField.getName(), searchField.getValue().toString());
            break;
        case ST_FIELD_EQUALS_OR_NOT:
        	if (!(searchField.getValue() instanceof Boolean)) {
        		throw new ESOperationException(ESErrorCode.PARAMETER_INCORRECT, "ST_FIELD_EQUALS_OR_NOT parameter should be Boolean");
        	}
        	Boolean equalsOrNot = (Boolean) searchField.getValue();
        	String equalsOrNotSymbol = equalsOrNot ? SCRIPT_FIELD_EQUALS_SYMBOL : SCRIPT_FIELD_NOT_EQUALS_SYMBOL;
            queryBuilder = buildFieldCompareQuery(searchField, equalsOrNotSymbol);
            break;

        case ST_FIELD_GREATER_THAN_OR_NOT:
        	if (!(searchField.getValue() instanceof Boolean)) {
        		throw new ESOperationException(ESErrorCode.PARAMETER_INCORRECT, "ST_FIELD_GREATER_THAN_OR_NOT parameter should be Boolean");
        	}
        	Boolean greaterThanOrNot = (Boolean) searchField.getValue();
        	String greaterThanOrNotSymbol = greaterThanOrNot ? SCRIPT_FIELD_GREATER_THAN_SYMBOL : SCRIPT_FIELD_LESS_THAN_EQUALS_SYMBOL;
            queryBuilder = buildFieldCompareQuery(searchField, greaterThanOrNotSymbol);
            break;
        case ST_FIELD_GREATER_THAN_EQUALS_OR_NOT:
        	if (!(searchField.getValue() instanceof Boolean)) {
        		throw new ESOperationException(ESErrorCode.PARAMETER_INCORRECT, "ST_FIELD_GREATER_THAN_EQUALS_OR_NOT parameter should be Boolean");
        	}
        	Boolean greaterThanEqualsOrNot = (Boolean) searchField.getValue();
        	String greaterThanEqualsOrNotSymbol = greaterThanEqualsOrNot ? SCRIPT_FIELD_GREATER_THAN_EQUALS_SYMBOL : SCRIPT_FIELD_LESS_THAN_SYMBOL;
            queryBuilder = buildFieldCompareQuery(searchField, greaterThanEqualsOrNotSymbol);
            break;
        case ST_SCRIPT:
            queryBuilder = buildScriptQuery(searchField);
            break;
        default:
            logger.error("Unsupported SearchType");
            throw new ESOperationException(ESErrorCode.ES_OPERATE_FAIL, "Unsupported SearchType");
        }

        return queryBuilder;
    }
    
    private void validateValue(SearchField searchField) {
    	if (searchField.getValue() == null) {
    		throw new ESOperationException(ESErrorCode.PARAMETER_INCORRECT, "parameter error: " + JSON.toJSONString(searchField));
    	}
    }
    
    private void validateNameAndValue(SearchField searchField) {
    	if (StringUtils.isEmpty(searchField.getName()) || searchField.getValue() == null) {
    		throw new ESOperationException(ESErrorCode.PARAMETER_INCORRECT, "parameter error: " + JSON.toJSONString(searchField));
    	}
    }
    
    private void validateNamesAndValue(SearchField searchField) {
    	if (searchField.getNames() == null 
    			|| searchField.getNames().length == 0
    			|| searchField.getValue() == null) {
    		throw new ESOperationException(ESErrorCode.PARAMETER_INCORRECT, "parameter error: " + JSON.toJSONString(searchField));
    	}
    }
    
    private void validate(SearchField searchField) throws ESOperationException {
        switch (searchField.getSearchType()) {
        case ST_MATCHALL:
            return;
        case ST_RANGE:
        case ST_TERM:
        case ST_TERMS:
        case ST_QUERY_STRING:
        case ST_MATCH:
        case ST_WILDCARD:
        case ST_MISSING:
        case ST_EXISTS:
        case ST_PREFIX:
        	validateNameAndValue(searchField);
        	break;
        case ST_MULTI_MATCH:
            validateNamesAndValue(searchField);
            break;
        case ST_IDS:
        case ST_FIELD_EQUALS_OR_NOT:
        case ST_FIELD_GREATER_THAN_OR_NOT:
        case ST_FIELD_GREATER_THAN_EQUALS_OR_NOT:
        case ST_SCRIPT:
        	validateValue(searchField);
            break;
        default:
            logger.error("Unsupported SearchType");
            throw new ESOperationException(ESErrorCode.ES_OPERATE_FAIL, "Unsupported SearchType");
        }
    }
    
    
    
    private QueryBuilder buildExistQuery(SearchField searchField) {
    	if (searchField.getValue() != null) {
    		if (searchField.getValue() instanceof Boolean) {
    			Boolean exist = (Boolean) searchField.getValue();
    			if (exist) {
    				return QueryBuilders.existsQuery(searchField.getName());
    			} else {
    				QueryBuilder existQueryBuilder = QueryBuilders.existsQuery(searchField.getName());
    	            return QueryBuilders.boolQuery().mustNot(existQueryBuilder);
    			}
    		}
    	}
    	
    	throw new ESOperationException(ESErrorCode.ES_OPERATE_FAIL, "exist query value is null");
    }
    
    private QueryBuilder buildMissingQuery(SearchField searchField) {
    	if (searchField.getValue() != null) {
    		if (searchField.getValue() instanceof Boolean) {
    			Boolean missing = (Boolean) searchField.getValue();
    			if (!missing) {
    				return QueryBuilders.existsQuery(searchField.getName());
    			} else {
    				QueryBuilder existQueryBuilder = QueryBuilders.existsQuery(searchField.getName());
    	            return QueryBuilders.boolQuery().mustNot(existQueryBuilder);
    			}
    		}
    	}
    	
    	throw new ESOperationException(ESErrorCode.ES_OPERATE_FAIL, "missing query value is null");
    }
    
    private QueryBuilder buildFieldCompareQuery(SearchField searchField, String symbol) {
        Script script = buildFieldCompareScript(searchField.getNames(), symbol);
        return QueryBuilders.scriptQuery(script);
    }
    
    private QueryBuilder buildScriptQuery(SearchField searchField) {
        return QueryBuilders.scriptQuery(new Script(searchField.getValue().toString()));
    }
    
    private QueryBuilder buildTermsQuery(SearchField searchField) {
        QueryBuilder termsQuery;
        Object[] terms = null;
        if (searchField.getValue().getClass().isArray()) {
            terms = (Object[]) searchField.getValue();
        } else if(Collection.class.isAssignableFrom(searchField.getValue().getClass())) {
            terms = ((Collection) searchField.getValue()).toArray();
        }else{
            terms = JSON.parseObject(JSON.toJSONString(searchField.getValue()), Object[].class);
        }

        // terms查询超过1024
        if (!searchField.getSearchLogic().equals(SearchLogic.SL_FILTER)) {
        	if (terms.length > DefaultConstant.TERMS_MAX_SIZE) {
            	throw new ESOperationException(ESErrorCode.PARAMETER_INCORRECT, "terms oversize, max size:" + DefaultConstant.TERMS_MAX_SIZE);
            }
        } else {
            // filter方式terms查询最大量限制
            if (terms.length > DefaultConstant.FILTER_TERMS_MAX_SIZE) {
                throw new ESOperationException(ESErrorCode.PARAMETER_INCORRECT, "terms oversize, max size:" + DefaultConstant.FILTER_TERMS_MAX_SIZE);
            }
        }
        
        termsQuery = QueryBuilders.termsQuery(searchField.getName(), terms)
        .boost(searchField.getBoost());
        return termsQuery;
    }
    
    private RangeQueryBuilder buildRangeQuery(SearchField searchField) {
        RangeQueryBuilder rangeQueryBuilder = QueryBuilders.rangeQuery(searchField.getName());
        Range range = null;
        
        if (searchField.getValue() instanceof Range) {
            range = (Range) searchField.getValue();
        } else {
            range = JSON.parseObject(JSON.toJSONString(searchField.getValue()), Range.class);
        }
        
        // from
        if (range.getFrom() != null) {
            rangeQueryBuilder.from(parseValue(range.getFrom()), range.getFrom().isInclude());
        }
        
        // to
        if (range.getTo() != null) {
            rangeQueryBuilder.to(parseValue(range.getTo()), range.getTo().isInclude());
        }

        return rangeQueryBuilder;
    }
    
    private Object parseValue (Range.Item item) {
        Object value = item.getValue();

        if (value instanceof Date) {
            // 时间类型
            value = DateUtil.getStringByPattern((Date) value);
        } else if (Date.class.equals(item.getClz())) {
        	// 增强时间类型
        	if (value instanceof Long) {
        		value = DateUtil.getStringByPattern(new Date((long) value));
        	}
        }
        
        return value;
    }
    
    private List<AbstractAggregationBuilder<?>> buildSingleAgg(IndexInfo indexInfo, AggCondition aggCondition, int loopTimes) throws ESOperationException {
        // check loop, avoid endless loop
        checkLoop(loopTimes);
        AggBucket aggBucket = aggCondition.getAggBucket();
        switch (aggBucket.getAggBucketType()) {
        case AGG_BUCKET_TERMS:
            // terms bucket
            AggTermsBucket aggTermsBucket =  aggBucket.getAggTermsBucket();
            TermsAggregationBuilder termsAggregationBuilder = buildTermsBucket(indexInfo, aggTermsBucket, aggCondition, loopTimes);
            return Arrays.asList(termsAggregationBuilder);
        case AGG_BUCKET_FILTER:
            // filter bucket
            AggFilterBucket aggFilterBucket =  aggBucket.getAggFilterBucket();
            FilterAggregationBuilder filterAggregationBuilder = buildFilterBucket(indexInfo, aggFilterBucket, aggCondition, loopTimes);
            return Arrays.asList(filterAggregationBuilder);
        case AGG_BUCKET_GLOBAL:
            // global bucket
            GlobalAggregationBuilder globalAggregationBuilder = buildGlobalBucket(indexInfo, aggCondition, loopTimes);
            return Arrays.asList(globalAggregationBuilder);
        case AGG_BUCKET_DATE_HISTOGRAM:
            // date_histogram
            AggDateHistogramBucket aggDateHistogramBucket =  aggBucket.getAggDateHistogramBucket();
            DateHistogramAggregationBuilder dateHistogramCriteria = buildDateHistogramBucket(indexInfo, aggDateHistogramBucket, aggCondition, loopTimes);
            return Arrays.asList(dateHistogramCriteria);
        case AGG_BUCKET_NONE:
            // none bucket--criteria only
            List<AbstractAggregationBuilder<?>> noneCriteria = buildNoneBucket(indexInfo, aggCondition, loopTimes);
            return noneCriteria;
        default:
            logger.error("unsupported bucket type!");
            throw new ESOperationException(ESErrorCode.PARAMETER_INCORRECT, "unsupported bucket type!");
        }
    }
    
    /**
     * avoid endless loop
     * @param loop
     * @throws ESOperationException
     */
    private void checkLoop(int loopTimes) throws ESOperationException {
        if (loopTimes > ElasticConstant.INNER_BUCKET_MAX_LOOP) {
            throw new ESOperationException(ESErrorCode.INNER_BUCKET_MAX_LOOP_LIMIT, "inner bucket max loop limit:" + ElasticConstant.INNER_BUCKET_MAX_LOOP);
        }
    }
    
    /**
     * build TermsAggregationBuilder
     * @param aggBucket
     * @param aggCondition
     * @return
     * @throws ESOperationException
     */
    private TermsAggregationBuilder buildTermsBucket(IndexInfo indexInfo, AggTermsBucket aggTermsBucket, AggCondition aggCondition, int loopTimes) throws ESOperationException {
        //  自定义连接符
    	// group by
    	String connectSymbol = SCRIPT_MULTI_GROUP_BY_CONNECT_SYMBOL;
    	if (!StringUtils.isEmpty(aggTermsBucket.getDelimiter())) {
    		connectSymbol = SCRIPT_MULTI_GROUP_BY_CONNECT_PLUS_PREFIX + aggTermsBucket.getDelimiter() + SCRIPT_MULTI_GROUP_BY_CONNECT_PLUS_SUFFIX;
    	}
        Script script = buildGroupByScript(aggTermsBucket.getGroupBy(), connectSymbol);
        TermsAggregationBuilder termsAggregationBuilder = AggregationBuilders.terms(aggCondition.getKey()).script(script);
        
        // size
        Integer size = aggTermsBucket.getSize();
        if (size == null) {
        	size = ElasticConstant.AGG_MAX_BUCKET_SIZE;
        } else if (size > ElasticConstant.AGG_MAX_BUCKET_SIZE) {
        	throw new ESOperationException(ESErrorCode.AGG_RESPONSE_SIZE_LIMIT, "agg response size can not be greater than " + ElasticConstant.AGG_MAX_BUCKET_SIZE);
        }
        termsAggregationBuilder.size(size);

        // order
        Sort[] sorts = aggTermsBucket.getSorts();
        if (sorts != null && sorts.length > 0) {
            for (Sort sort : sorts) {
                termsAggregationBuilder.order(buildAggOrder(sort));
            }
        }
        
        // criteria
        List<AbstractAggregationBuilder<?>> termsCriteria = buildCriteria(aggCondition.getAggCriteria());

        for (AbstractAggregationBuilder<?> criterion : termsCriteria) {
            termsAggregationBuilder.subAggregation(criterion);
        }
        
        if (!CollectionUtils.isEmpty(aggCondition.getInnerAggConditions())) {
            // inner bucket
            for (AggCondition innerAggCondition : aggCondition.getInnerAggConditions()) {
                List<AbstractAggregationBuilder<?>> innerAggregationBuilders = buildSingleAgg(indexInfo, innerAggCondition, loopTimes + 1);
                for (AbstractAggregationBuilder<?> innerAggregationBuilder : innerAggregationBuilders) {
                    termsAggregationBuilder.subAggregation(innerAggregationBuilder);
                }
            }
        }
        
        return termsAggregationBuilder;
    }
    
    private FilterAggregationBuilder buildFilterBucket(IndexInfo indexInfo, AggFilterBucket aggFilterBucket, AggCondition aggCondition, int loopTimes) throws ESOperationException {
        // filter bucket
        QueryBuilder queryBuilder = createCompoundBoolQueryBuilder(indexInfo, aggFilterBucket.getCompoundCriteria());
        FilterAggregationBuilder filterAggregationBuilder = AggregationBuilders.filter(aggCondition.getKey(), queryBuilder);
        
        // criteria
        List<AbstractAggregationBuilder<?>> filterCriteria = buildCriteria(aggCondition.getAggCriteria());

        for (AbstractAggregationBuilder<?> criterion : filterCriteria) {
            filterAggregationBuilder.subAggregation(criterion);
        }
        
        if (!CollectionUtils.isEmpty(aggCondition.getInnerAggConditions())) {
            // inner bucket
            for (AggCondition innerAggCondition : aggCondition.getInnerAggConditions()) {
                List<AbstractAggregationBuilder<?>> innerAggregationBuilders = buildSingleAgg(indexInfo, innerAggCondition, loopTimes + 1);
                for (AbstractAggregationBuilder<?> innerAggregationBuilder : innerAggregationBuilders) {
                	filterAggregationBuilder.subAggregation(innerAggregationBuilder);
                }
            }
        }
        
        return filterAggregationBuilder;
    }
    
    private GlobalAggregationBuilder buildGlobalBucket(IndexInfo indexInfo, AggCondition aggCondition, int loopTimes) throws ESOperationException {
        // global bucket
        GlobalAggregationBuilder globalAggregationBuilder = AggregationBuilders.global(aggCondition.getKey());
        
        // criteria
        List<AbstractAggregationBuilder<?>> globalCriteria = buildCriteria(aggCondition.getAggCriteria());

        for (AbstractAggregationBuilder<?> criterion : globalCriteria) {
            globalAggregationBuilder.subAggregation(criterion);
        }
        
        if (!CollectionUtils.isEmpty(aggCondition.getInnerAggConditions())) {
            // inner bucket
            for (AggCondition innerAggCondition : aggCondition.getInnerAggConditions()) {
                List<AbstractAggregationBuilder<?>> innerAggregationBuilders = buildSingleAgg(indexInfo, innerAggCondition, loopTimes + 1);
                for (AbstractAggregationBuilder<?> innerAggregationBuilder : innerAggregationBuilders) {
                	globalAggregationBuilder.subAggregation(innerAggregationBuilder);
                }
            }
        }
        
        return globalAggregationBuilder;
    }
    
    private DateHistogramAggregationBuilder buildDateHistogramBucket(IndexInfo indexInfo, AggDateHistogramBucket aggDateHistogramBucket, AggCondition aggCondition, int loopTimes) throws ESOperationException {
        // date_histogram bucket
        DateHistogramInterval dateHistogramInterval = new DateHistogramInterval(aggDateHistogramBucket.getInterval());
        DateHistogramAggregationBuilder dateHistogramAggregationBuilder = AggregationBuilders.dateHistogram(aggCondition.getKey())
                .field(aggDateHistogramBucket.getField())
                .dateHistogramInterval(dateHistogramInterval);
        
        // order
        Sort[] sorts = aggDateHistogramBucket.getSorts();
        if (sorts != null && sorts.length > 0) {
            for (Sort sort : sorts) {
            	dateHistogramAggregationBuilder.order(buildHistogramAggOrder(sort));
            }
        }
        
        // format
        if (!StringUtils.isEmpty(aggDateHistogramBucket.getFormat())) {
            dateHistogramAggregationBuilder.format(aggDateHistogramBucket.getFormat());
        }
        
        // minDocCount
        if (aggDateHistogramBucket.getMinDocCount() != null) {
            dateHistogramAggregationBuilder.minDocCount(aggDateHistogramBucket.getMinDocCount());
        }
        
        // extendedBounds
        if (!StringUtils.isEmpty(aggDateHistogramBucket.getBoundsMin())
                || !StringUtils.isEmpty(aggDateHistogramBucket.getBoundsMax())) {
            ExtendedBounds extendedBounds = new ExtendedBounds(aggDateHistogramBucket.getBoundsMin(), aggDateHistogramBucket.getBoundsMax());
            dateHistogramAggregationBuilder.extendedBounds(extendedBounds);
        }
        
        // criteria
        List<AbstractAggregationBuilder<?>> filterCriteria = buildCriteria(aggCondition.getAggCriteria());

        for (AbstractAggregationBuilder<?> criterion : filterCriteria) {
            dateHistogramAggregationBuilder.subAggregation(criterion);
        }
        
        if (!CollectionUtils.isEmpty(aggCondition.getInnerAggConditions())) {
            // inner bucket
            for (AggCondition innerAggCondition : aggCondition.getInnerAggConditions()) {
                List<AbstractAggregationBuilder<?>> innerAggregationBuilders = buildSingleAgg(indexInfo, innerAggCondition, loopTimes + 1);
                for (AbstractAggregationBuilder<?> innerAggregationBuilder : innerAggregationBuilders) {
                    dateHistogramAggregationBuilder.subAggregation(innerAggregationBuilder);
                }
            }
        }
        
        return dateHistogramAggregationBuilder;
    }
    
    private List<AbstractAggregationBuilder<?>> buildNoneBucket(IndexInfo indexInfo, AggCondition aggCondition, int loopTimes) throws ESOperationException {
        // criteria
        List<AbstractAggregationBuilder<?>> noneCriteria = buildCriteria(aggCondition.getAggCriteria());

        if (!CollectionUtils.isEmpty(aggCondition.getInnerAggConditions())) {
            // inner bucket
            for (AggCondition innerAggCondition : aggCondition.getInnerAggConditions()) {
                List<AbstractAggregationBuilder<?>> innerAggregationBuilders = buildSingleAgg(indexInfo, innerAggCondition, loopTimes + 1);
                noneCriteria.addAll(innerAggregationBuilders);
            }
        }
        
        return noneCriteria;
    }
    
    private Script buildGroupByScript(String[] groupBy, String connectSymbol) {
        String esScript = "";
        for (int i = 0; i < groupBy.length; i++) {
            String groupByI = groupBy[i];
            if (i == groupBy.length - 1) {
                esScript += SCRIPT_FIELD_VALUE_PREFIX + groupByI + SCRIPT_FIELD_VALUE_SUFFIX;
            } else {
            	//group by连接符
                //esScript += SCRIPT_FIELD_VALUE_PREFIX + groupByI + SCRIPT_FIELD_VALUE_SUFFIX + SCRIPT_MULTI_GROUP_BY_CONNECT_SYMBOL;
            	esScript += SCRIPT_FIELD_VALUE_PREFIX + groupByI + SCRIPT_FIELD_VALUE_SUFFIX + connectSymbol;
            }
        }

        return new Script(ScriptType.INLINE, Script.DEFAULT_SCRIPT_LANG, esScript, Collections.emptyMap());
    }
    
    private BucketOrder buildAggOrder(Sort sort) {
       return BucketOrder.aggregation(sort.getField(), sort.getType().equals(SortType.ASC));
    }
    
    private BucketOrder buildHistogramAggOrder(Sort sort) {
    	if (AggInnerField.KEY.equals(sort.getField())) {
    		// sort by key
    		if (SortType.ASC.equals(sort.getType())) {
    			// asc
                return BucketOrder.key(true);
    		} else {
    			// desc
                return BucketOrder.key(false);
    		}
    	}
    	
    	if (AggInnerField.COUNT.equals(sort.getField())) {
    		// sort by count
    		if (SortType.ASC.equals(sort.getType())) {
    			// asc
    			return BucketOrder.count(true);
    		} else {
    			// desc
    			return BucketOrder.count(false);
    		}
    	}
        return BucketOrder.aggregation(sort.getField(), sort.getType().equals(SortType.ASC));
    }
    
    /**
     * 构建指标
     * @param AggCriterions
     * @return
     * @throws ESOperationException
     */
    private List<AbstractAggregationBuilder<?>> buildCriteria(List<AggCriterion> aggCriteria) throws ESOperationException {
        List<AbstractAggregationBuilder<?>> result = new ArrayList<>();
        
        if (!CollectionUtils.isEmpty(aggCriteria)) {
            for (AggCriterion aggCriterion : aggCriteria) {
                AbstractAggregationBuilder<?> criterion = buildCriterion(aggCriterion);
                result.add(criterion);
            }
        }
        
        return result;
    }
    
    /**
     * 构建指标
     * @param aggCriterion
     * @return
     * @throws ESOperationException
     */
    private AbstractAggregationBuilder<?> buildCriterion(AggCriterion aggCriterion) throws ESOperationException {
        AbstractAggregationBuilder<?> result = null;
        
        String field = aggCriterion.getField();
        String name = aggCriterion.getKey();
        switch (aggCriterion.getCriterionType()) {
            case AGG_CRITERION_MIN:
                result = AggregationBuilders
                        .min(name)
                        .field(field);
                break;
            case AGG_CRITERION_MAX:
                result = AggregationBuilders
                        .max(name)
                        .field(field);
                break;
            case AGG_CRITERION_SUM:
                result = AggregationBuilders
                        .sum(name)
                        .field(field);
                break;
            case AGG_CRITERION_AVG:
                result = AggregationBuilders
                        .avg(name)
                        .field(field);
                break;
            case AGG_CRITERION_COUNT:
                result = AggregationBuilders
                        .count(name)
                        .field(field);
                break;
            case AGG_CRITERION_TOP_HITS:
                result = buildTopHits(name, (TopHitsAggCriterion)aggCriterion);
                break;
            default:
                logger.error("Unsupported AggType");
                throw new ESOperationException(ESErrorCode.ES_OPERATE_FAIL, "Unsupported AggType");
        }
        return result;
    }
    
    private TopHitsAggregationBuilder buildTopHits(String name, TopHitsAggCriterion topHitsAggCriterion) {
        TopHits topHits = topHitsAggCriterion.getTopHits();
        TopHitsAggregationBuilder builder = AggregationBuilders.topHits(name);
        // from
        if (topHits.getFrom() != null) {
            builder.from(topHits.getFrom());
        }
        // size
        if (topHits.getSize() != null) {
            int size = topHits.getSize() > ElasticConstant.TOP_HITS_MAX_SIZE ? ElasticConstant.TOP_HITS_MAX_SIZE : topHits.getSize();
            builder.size(size);
        }
        // includes
        if (topHits.getIncludes() != null && topHits.getIncludes().length > 0) {
            builder.fetchSource(topHits.getIncludes(), null);
        }
        // sort
        if (topHits.getSorts() != null && topHits.getSorts().length > 0) {
            Sort[] sorts = topHits.getSorts();
            List<SortBuilder<?>> sortBuilders = new ArrayList<>();
            for (Sort sort : sorts) {
                FieldSortBuilder sortBuilder = new FieldSortBuilder(sort.getField());
                SortOrder sortOrder;
                switch (sort.getType()) {
                case DESC:
                    sortOrder = SortOrder.DESC;
                    break;
                case ASC:
                    sortOrder = SortOrder.ASC;
                    break;
                default: 
                    sortOrder = SortOrder.DESC; 
                }
                sortBuilder.order(sortOrder);
                sortBuilders.add(sortBuilder);
            }
            builder.sorts(sortBuilders);
        }
        // a 返回id与version
        builder.explain(false).version(true);
        
        return builder;
    }
    
    private Script buildFieldCompareScript(String[] fields, String symbol) {
        StringBuilder sb = new StringBuilder();
        sb.append(SCRIPT_FIELD_VALUE_PREFIX).append(fields[0]).append(SCRIPT_FIELD_VALUE_SUFFIX)
            .append(symbol).append(SCRIPT_FIELD_VALUE_PREFIX).append(fields[1]).append(SCRIPT_FIELD_VALUE_SUFFIX);
        
        Script script = new Script(sb.toString());
        
        return script;
    }
}
