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
import com.yundasys.es.operation.model.request.CompoundCriteria;
import com.yundasys.es.operation.model.request.Criteria;
import com.yundasys.es.operation.model.request.SearchField;
import com.yundasys.es.operation.util.DateUtil;
import com.yundasys.es.operation.model.agg.AggCondition;
import com.yundasys.es.operation.model.agg.AggDateCondition;
import com.yundasys.es.operation.model.agg.CalField;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.ArrayUtils;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.elasticsearch.script.Script;
import org.elasticsearch.script.ScriptType;
import org.elasticsearch.search.aggregations.AbstractAggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.BucketOrder;
import org.elasticsearch.search.aggregations.bucket.histogram.DateHistogramAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.histogram.DateHistogramInterval;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
/**
 * @author zhengxiaosu
 * @desc  请求es前的条件处理
 * @date 2020/6/3 14:29
 */
@Component
@Slf4j
public class ESBuilderHelper {

    private static final String SCRIPT_UPD_PREFIX = "ctx._source.";
    private static final String SCRIPT_FIELD_VALUE_PREFIX = "doc['";
    private static final String SCRIPT_FIELD_VALUE_SUFFIX = "'].value";
    private static final String SCRIPT_FIELD_EQUALS_SYMBOL = " == ";
    private static final String SCRIPT_FIELD_NOT_EQUALS_SYMBOL = " != ";
    private static final String SCRIPT_MULTI_GROUP_BY_CONNECT_SYMBOL = " + '" + ElasticConstant.SCRIPT_MULTI_GROUP_BY_SPLIT + "' + ";//group by 脚本分割符
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
    

    public void buildAggs(IndexInfo indexInfo, AggDateCondition aggDateCondition, SearchSourceBuilder searchSourceBuilder) {
        if (aggDateCondition == null) {
            return;
        }
        //时间汇总
        if (!StringUtils.isEmpty(aggDateCondition.getDateField())) {
            DateHistogramAggregationBuilder dateHistogramAggregationBuilder = buildDateHistogram(aggDateCondition);
            searchSourceBuilder.aggregation(dateHistogramAggregationBuilder);
        } else {
            AggCondition aggCondition = aggDateCondition.getAggCondition();
            if (aggCondition != null) {//没有时间汇总 仅group by
                if (ArrayUtils.isEmpty(aggCondition.getGroupByFields())) {//没有 group by的字段

                    if (ArrayUtils.isEmpty(aggCondition.getCalFields())) {
                        //AggCondition 里2个重要字段全为空 忽略
                        log.error("aggCondition 信息里没有聚合字段,也没有分组字段  {}", indexInfo.getIndexes());
                    } else {
                        for (int i = 0; i < aggCondition.getCalFields().length; i++) {//直接聚合查询条件
                            searchSourceBuilder.aggregation(buildCriterion(aggCondition.getCalFields()[i]));
                        }
                    }
                } else {
                    searchSourceBuilder.aggregation(buildTermsAgg(aggCondition));
                }
            }
        }
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
            log.error("不支持的 SearchLogic");
            throw new ESOperationException(ESErrorCode.ES_OPERATE_FAIL, "不支持的 SearchLogic");
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
            log.error("Unsupported SearchType");
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
            log.error("Unsupported SearchType");
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
        Range range ;
        
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

    /**
     * 建立 DateHistogram
     */
    private DateHistogramAggregationBuilder buildDateHistogram(AggDateCondition aggDateCondition) {
        // date_histogram bucket
        DateHistogramInterval dateHistogramInterval = new DateHistogramInterval(aggDateCondition.getInterval());
        DateHistogramAggregationBuilder dateHistogramAggregationBuilder = AggregationBuilders.dateHistogram(aggDateCondition.getDateField() + ElasticConstant.FIELD_CAL_SUFFIX)
                .field(aggDateCondition.getDateField())
                .calendarInterval(dateHistogramInterval);

        // 排序

        dateHistogramAggregationBuilder.order(buildHistogramAggOrder(aggDateCondition.getSortField(),aggDateCondition.getSortType()));

        //时间分组本层聚合
        if (!ArrayUtils.isEmpty(aggDateCondition.getCalFields())) {
            for (CalField calField : aggDateCondition.getCalFields()) {
                AbstractAggregationBuilder<?> criterion = buildCriterion(calField);
                dateHistogramAggregationBuilder.subAggregation(criterion);
            }
        }
        //时间分组里面一层聚合
        AggCondition AggCondition = aggDateCondition.getAggCondition();
        if (AggCondition != null) {
            // 拼接 group by脚本

            TermsAggregationBuilder termsAggregationBuilder = buildTermsAgg(AggCondition);

            dateHistogramAggregationBuilder.subAggregation(termsAggregationBuilder);
        }


        return dateHistogramAggregationBuilder;
    }

    /**
     * group by 汇总 build建立
     */
    private TermsAggregationBuilder buildTermsAgg(AggCondition aggCondition) {
      
        Script script = buildGroupByScript(aggCondition.getGroupByFields(),SCRIPT_MULTI_GROUP_BY_CONNECT_SYMBOL);

        TermsAggregationBuilder termsAggregationBuilder = AggregationBuilders.terms(ElasticConstant.GROUP_BY_KEY).script(script);

        // size
        Integer size = aggCondition.getSize();
        if (size == null) {
            size = ElasticConstant.AGG_MAX_BUCKET_SIZE;
        } else if (size > ElasticConstant.AGG_MAX_BUCKET_SIZE) {
            throw new ESOperationException(ESErrorCode.AGG_RESPONSE_SIZE_LIMIT, "agg response size can not be greater than " + ElasticConstant.AGG_MAX_BUCKET_SIZE);
        }
        termsAggregationBuilder.size(size);

        // order
        Sort[] sorts = aggCondition.getSorts();
        if (sorts != null && sorts.length > 0) {
            for (Sort sort : sorts) {
                termsAggregationBuilder.order(buildAggOrder(sort));
            }
        }

        // criteria

        if (!ArrayUtils.isEmpty(aggCondition.getCalFields())) {
            for (CalField calField : aggCondition.getCalFields()) {
                AbstractAggregationBuilder<?> criterion = buildCriterion(calField);
                termsAggregationBuilder.subAggregation(criterion);
            }
        }
        return termsAggregationBuilder;
    }


    private BucketOrder buildHistogramAggOrder(String sortField, SortType sortType) {
        if (AggInnerField.KEY.equals(sortField)) {
            //key 排序 asc true  desc:false
            return BucketOrder.key(SortType.ASC.equals(sortType));
        }

        if (AggInnerField.COUNT.equals(sortField)) {
            // count 排序 asc true  desc:false
            return BucketOrder.count(SortType.ASC.equals(sortType));
        }
        return BucketOrder.aggregation(sortField, sortType.equals(SortType.ASC));
    }


    
    private Script buildGroupByScript(String[] groupBy, String connectSymbol) {
        String esScript = "";
        for (int i = 0; i < groupBy.length; i++) {
            String groupByI = groupBy[i];
            if (i == groupBy.length - 1) {
                esScript += SCRIPT_FIELD_VALUE_PREFIX + groupByI + SCRIPT_FIELD_VALUE_SUFFIX;
            } else {
            	esScript += SCRIPT_FIELD_VALUE_PREFIX + groupByI + SCRIPT_FIELD_VALUE_SUFFIX + connectSymbol;
            }
        }

        return new Script(ScriptType.INLINE, Script.DEFAULT_SCRIPT_LANG, esScript, Collections.emptyMap());
    }
    
    private BucketOrder buildAggOrder(Sort sort) {
       return BucketOrder.aggregation(sort.getField(), sort.getType().equals(SortType.ASC));
    }

    private AbstractAggregationBuilder<?> buildCriterion(CalField calField) {
        AbstractAggregationBuilder<?> result = null;

        String field = calField.getField();
        String name = calField.getField()+ElasticConstant.FIELD_CAL_SUFFIX;
        switch (calField.getCriterionType()) {
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
            default:
                log.error("不支持的聚合类型 {}",calField.getCriterionType());
                throw new ESOperationException(ESErrorCode.ES_OPERATE_FAIL, "不支持的聚合类型");
        }
        return result;
    }
    

    private Script buildFieldCompareScript(String[] fields, String symbol) {
        StringBuilder sb = new StringBuilder();
        sb.append(SCRIPT_FIELD_VALUE_PREFIX).append(fields[0]).append(SCRIPT_FIELD_VALUE_SUFFIX)
            .append(symbol).append(SCRIPT_FIELD_VALUE_PREFIX).append(fields[1]).append(SCRIPT_FIELD_VALUE_SUFFIX);
        
        Script script = new Script(sb.toString());
        
        return script;
    }
}
