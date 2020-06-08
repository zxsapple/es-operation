package com.yundasys.es.operation.util;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.yundasys.es.operation.annotation.*;
import com.yundasys.es.operation.constant.AggBucketType;
import com.yundasys.es.operation.constant.AggCriterionType;
import com.yundasys.es.operation.constant.SearchType;
import com.yundasys.es.operation.constant.SortType;
import com.yundasys.es.operation.model.Hit;
import com.yundasys.es.operation.model.Sort;
import com.yundasys.es.operation.model.TopHits;
import com.yundasys.es.operation.model.TopHitsAggCriterion;
import com.yundasys.es.operation.model.agg.*;
import com.yundasys.es.operation.model.request.CompoundCriteria;
import com.yundasys.es.operation.model.request.ConvertedHit;
import com.yundasys.es.operation.model.request.Criteria;
import com.yundasys.es.operation.model.request.SearchField;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class ConvertTool {
	/**
	 * TopHits转换为对象Hit数组
	 * @param value
	 * @return
	 */
	public static <T> List<ConvertedHit<T>> convertTopHits2ListWithIdAndVersion(Object value, Class<T> clazz) {
    	List<Hit> hits = (List<Hit>) value;
        return convertHits2ListWithIdAndVersion(hits, clazz);
    }
    
    /**
	 * TopHits转换为对象Hit数组
	 * @param value
	 * @return
	 */
    @SuppressWarnings("unchecked")
	public static <T> List<T> convertTopHits2List(Object value, Class<T> clazz) {
    	List<Hit> hits = (List<Hit>) value;
        return convertHits2List(hits, clazz);
    }
    
    public static List<String> convertHits2StringList(List<Hit> hits) {
    	if (hits == null || hits.size() == 0) {
			return new ArrayList<>(0);
		}
		List<String> convertedHits = new ArrayList<>(hits.size());
		for (Hit hit : hits) {
			convertedHits.add(hit.getValue());
		}
		
		return convertedHits;
    }
    
    /**
     * 处理返回值为科学计数法
     * @param map
     */
    public static void convertMapValue2Integer(Map<String, Object> map) {
        for (Entry<String, Object> entry : map.entrySet()) {
            Object value = entry.getValue();
            BigDecimal adjustValue = new BigDecimal(value.toString());
            map.put(entry.getKey(), adjustValue.intValue());
        }
    }
    
    /**
     * 处理返回值为科学计数法
     * @param map
     */
    public static void convertMapValue2Long(Map<String, Object> map) {
        for (Entry<String, Object> entry : map.entrySet()) {
            Object value = entry.getValue();
            BigDecimal adjustValue = new BigDecimal(value.toString());
            map.put(entry.getKey(), adjustValue.longValue());
        }
    }
    
    /**
     * 将条件对象转换成组合条件
     * 对象List，默认：对象之间使用or连接
     * 使用ExtendOredCriteria注解的对象，对象之间使用and连接
     * 扫描条件对象中@FieldOption注解，将该注解的属性转化为实际检索条件
     * @param targets
     * @return
     * @throws IllegalArgumentException
     * @throws IllegalAccessException
     */
    public static CompoundCriteria convert2CompoundCriteria(List<Object> targets) {
    	if (targets == null || targets.size() == 0) {
    		return null;
    	}
    	
        CompoundCriteria compoundCriteria = new CompoundCriteria();
        for (Object target : targets) {
        	ExtendOredCriteria extendOredCriteria = target.getClass().getAnnotation(ExtendOredCriteria.class);
        	if (extendOredCriteria != null) {
        		Criteria criteria = compoundCriteria.and();
            	buildCriteria(criteria, target);
        	} else {
        		Criteria criteria = compoundCriteria.or();
            	buildCriteria(criteria, target);
        	}
        }
    	return compoundCriteria;
    }
    
    private static void buildCriteria(Criteria criteria, Object target) {
    	Class<?> clazz = target.getClass();
        Field[] fields = getFields(clazz);
    	for (Field field : fields) {
        	field.setAccessible(Boolean.TRUE);
        	FieldOption fieldOption = field.getAnnotation(FieldOption.class);
            if (null == fieldOption) {
            	// 跳过无@FieldOption注解属性
                continue;
            }

            Object fieldObj = null;
			try {
				fieldObj = field.get(target);
			} catch (IllegalArgumentException | IllegalAccessException e) {
				throw new RuntimeException(e);
			}
            
            if (fieldObj == null) {
            	// 跳过值为null的属性
            	continue;
            }

            SearchField searchField = new SearchField();
            searchField.setSearchLogic(fieldOption.searchLogic());
            searchField.setSearchType(fieldOption.searchType());
            searchField.setValue(fieldObj);
            criteria.addCriterion(searchField);
            if (fieldOption.searchType().equals(SearchType.ST_MULTI_MATCH)
            		|| fieldOption.searchType().equals(SearchType.ST_FIELD_EQUALS_OR_NOT)
            		|| fieldOption.searchType().equals(SearchType.ST_FIELD_GREATER_THAN_OR_NOT)
            		|| fieldOption.searchType().equals(SearchType.ST_FIELD_GREATER_THAN_EQUALS_OR_NOT)) {
            	// 使用扩展检索域--多域操作
                searchField.setNames(fieldOption.names());
            } else {
            	// 单域操作
                searchField.setName(ClientStringUtil.isNotEmpty(fieldOption.name()) ? fieldOption.name() : field.getName());//空时使用字段名称

            }

        }
    }
    
    /**
     * 将条件对象转换成组合条件
     * 扫描条件对象中@FieldOption注解，将该注解的属性转化为实际检索条件
     * @param target
     * @return
     * @throws IllegalArgumentException
     * @throws IllegalAccessException
     */
    public static CompoundCriteria convert2CompoundCriteria(Object target) {
        CompoundCriteria compoundCriteria = new CompoundCriteria();
    	ExtendOredCriteria extendOredCriteria = target.getClass().getAnnotation(ExtendOredCriteria.class);
    	if (extendOredCriteria != null) {
    		Criteria criteria = compoundCriteria.createExtendCriteria();
        	buildCriteria(criteria, target);
    	} else {
    		Criteria criteria = compoundCriteria.createCriteria();
        	buildCriteria(criteria, target);
    	}
    	
    	return compoundCriteria;
    }
    
    /**
     * 从检索结果对象类型中解析搜索包含的域
     * 扫描结果对象类型@FieldHits注解，所有该注解的field为检索包含的域
     * @param clazz
     * @return
     */
    public static String[] convert2Includes(Class<?> clazz) {
        Field[] fields = getFields(clazz);
        List<String> includes = new ArrayList<>();
        
        for (Field field : fields) {
        	FieldHit fieldHit = field.getAnnotation(FieldHit.class);
            if (null == fieldHit) {
                continue;
            }
            
            includes.add(fieldHit.name());
        }
        
        if (includes.size() == 0) {
        	return null;
        }
        
        String[] array = new String[includes.size()];
        includes.toArray(array);
    	return array;
    }
    
    /**
     * 检索结果转换为对象list
     * @param clazz
     * @return
     */
    public static <T> List<T> convertHits2List(List<Hit> hits, Class<T> clazz) {
        List<T> result = new ArrayList<>(hits.size());
        
        Field[] fields = getFields(clazz);
        Map<String, String> hitNameFieldNameMap = new HashMap<>();
        
        for (Field field : fields) {
        	FieldHit fieldHit = field.getAnnotation(FieldHit.class);
            if (null == fieldHit) {
                continue;
            }

            String formatFieldHitName = ClientStringUtil.underlineToCamelhump(fieldHit.name());
            if (!field.getName().equals(formatFieldHitName)) {
            	// 结果集中属性不能转换为对应的驼峰式，记录后期处理
            	hitNameFieldNameMap.put(fieldHit.name(), field.getName());
            }
        }
        
        for (Hit hit : hits) {
        	T obj = convertHit2Object(hit.getValue(), clazz, hitNameFieldNameMap);
        	result.add(obj);
        }
        
    	return result;
    }
    
    /**
     * 检索结果转换为对象list
     * @param clazz
     * @return
     */
    public static <T> List<ConvertedHit<T>> convertHits2ListWithIdAndVersion(List<Hit> hits, Class<T> clazz) {
        List<ConvertedHit<T>> result = new ArrayList<>(hits.size());
        
        Field[] fields = getFields(clazz);
        Map<String, String> hitNameFieldNameMap = new HashMap<>();
        
        for (Field field : fields) {
        	FieldHit fieldHit = field.getAnnotation(FieldHit.class);
            if (null == fieldHit) {
                continue;
            }

            String formatFieldHitName = ClientStringUtil.underlineToCamelhump(fieldHit.name());
            if (!field.getName().equals(formatFieldHitName)) {
            	// 结果集中属性不能转换为对应的驼峰式，记录后期处理
            	hitNameFieldNameMap.put(fieldHit.name(), field.getName());
            }
        }
        
        for (Hit hit : hits) {
        	T obj = convertHit2Object(hit.getValue(), clazz, hitNameFieldNameMap);
        	ConvertedHit<T> convertedHit = new ConvertedHit<>();
        	convertedHit.setId(hit.getId());
        	convertedHit.setValue(obj);
        	convertedHit.setVersion(hit.getVersion());
        	result.add(convertedHit);
        }
        
    	return result;
    }
    
    /**
     * 检索结果的单个值转换为对象
     * @param clazz
     * @return
     */
    public static <T> List<T> convertHitValue2Object(List<Hit> hits, Class<T> clazz) {
        List<T> result = new ArrayList<>(hits.size());
        
        Field[] fields = getFields(clazz);
        Map<String, String> hitNameFieldNameMap = new HashMap<>();
        
        for (Field field : fields) {
        	FieldHit fieldHit = field.getAnnotation(FieldHit.class);
            if (null == fieldHit) {
                continue;
            }

            String formatFieldHitName = ClientStringUtil.underlineToCamelhump(fieldHit.name());
            if (!field.getName().equals(formatFieldHitName)) {
            	// 结果集中属性不能转换为对应的驼峰式，记录后期处理
            	hitNameFieldNameMap.put(fieldHit.name(), field.getName());
            }
        }
        
        for (Hit hit : hits) {
        	T obj = convertHit2Object(hit.getValue(), clazz, hitNameFieldNameMap);
        	result.add(obj);
        }
        
    	return result;
    }
    
    private static <T> T convertHit2Object(String hit, Class<T> clazz, Map<String, String> hitNameFieldNameMap) {
    	if (!hitNameFieldNameMap.isEmpty()) {
    		// 处理不能转换的属性
    		Map<String, Object> hitMap = JSON.parseObject(hit, new TypeReference<HashMap<String, Object>>() {});
    		for (Entry<String, String> entry : hitNameFieldNameMap.entrySet()) {
    			hitMap.put(entry.getValue(), hitMap.get(entry.getKey()));
    			hitMap.remove(entry.getKey());
    		}
    		
    		hit = JSON.toJSONString(hitMap);
    	}
    	
    	return JSON.parseObject(hit, clazz);
    }
    
    /**
     * 转换聚合条件
     * @param clazz
     * @param bucketType
     * @return
     */
    public static AggCondition convert2AggCondition(Class<?> clazz, AggBucketType bucketType) {
        return convert2AggCondition(clazz,  bucketType, null, true);
    }
    
    /**
     * 转换聚合条件--自定义bucketKey
     * 如果同种类型bucket并列使用时需要自定义bucketKey
     * @param clazz
     * @param bucketType
     * @param bucketKey 自定义bucketKey，如果为null则自动构建
     * @param buildAggCriteria 是否构建指标信息
     * @return
     */
    public static AggCondition convert2AggCondition(Class<?> clazz, AggBucketType bucketType, String bucketKey, boolean buildAggCriteria) {
        switch (bucketType) {
        case AGG_BUCKET_TERMS:
        	if (isEmpty(bucketKey)) {
            	bucketKey = buildBucketKey(clazz, AggBucketType.AGG_BUCKET_TERMS);
            }
        	return convert2TermsBucketAggCondition(clazz, bucketKey, buildAggCriteria);
        case AGG_BUCKET_GLOBAL:
        	if (isEmpty(bucketKey)) {
            	bucketKey = buildBucketKey(clazz, AggBucketType.AGG_BUCKET_GLOBAL);
            }
        	return convert2GlobalBucketAggCondition(clazz, bucketKey, buildAggCriteria);
        case AGG_BUCKET_DATE_HISTOGRAM:
        	if (isEmpty(bucketKey)) {
            	bucketKey = buildBucketKey(clazz, AggBucketType.AGG_BUCKET_DATE_HISTOGRAM);
            }
        	return convert2DateHistogramBucketAggCondition(clazz, bucketKey, buildAggCriteria);
        case AGG_BUCKET_NONE:
        	if (isEmpty(bucketKey)) {
            	bucketKey = buildBucketKey(clazz, AggBucketType.AGG_BUCKET_NONE);
            }
        	return convert2NoneBucketAggCondition(clazz, bucketKey, buildAggCriteria);
        default:
        	throw new RuntimeException("unsupported AggBucketType");
        }
    }
    
    /**
     * 转换过滤桶聚合条件
     * @param clazz
     * @param filterCriteria
     * @return
     */
    public static AggCondition convert2FilterAggCondition(Class<?> clazz, CompoundCriteria filterCriteria) {
        String bucketKey = buildBucketKey(clazz, AggBucketType.AGG_BUCKET_FILTER);
    	return convert2FilterAggCondition(clazz, filterCriteria, bucketKey, true);
    }
    
    /**
     * 转换过滤桶聚合条件--自定义bucketKey
     * 如果同种类型bucket并列使用时需要自定义bucketKey
     * @param clazz
     * @param filterCriteria
     * @param bucketKey 自定义bucketKey，如果为null则自动构建
     * @param buildAggCriteria 是否构建指标
     * @return
     */
    public static AggCondition convert2FilterAggCondition(Class<?> clazz, CompoundCriteria filterCriteria, String bucketKey, boolean buildAggCriteria) {
        AggFilterBucketAnnotation aggFilterBucketAnnotation = clazz.getAnnotation(AggFilterBucketAnnotation.class);
        if (aggFilterBucketAnnotation == null) {
        	return null;
        }

        // 聚合条件--包含桶和指标
        AggCondition aggCondition = new AggCondition();
        if (isEmpty(bucketKey)) {
        	bucketKey = buildBucketKey(clazz, AggBucketType.AGG_BUCKET_FILTER);
        }
        aggCondition.setKey(bucketKey);
        
        // 过滤桶
        AggFilterBucket aggFilterBucket = new AggFilterBucket();
        aggFilterBucket.setCompoundCriteria(filterCriteria);

        AggBucket aggBucket = new AggBucket();
        aggBucket.setAggFilterBucket(aggFilterBucket);
        aggBucket.setAggBucketType(AggBucketType.AGG_BUCKET_FILTER);

        aggCondition.setAggBucket(aggBucket);
        
        if (buildAggCriteria) {
        	// 指标
            Field[] fields = getFields(clazz);
            aggCondition.setAggCriteria(buildAggCriteria(fields));
        }
        
    	return aggCondition;
    }
    
    /**
     * 按时间聚合的聚合条件--自定义bucketKey
     * 如果同种类型bucket并列使用时需要自定义bucketKey
     * @param clazz
     * @param bucketKey
     * @param buildAggCriteria
     * @return
     */
    private static AggCondition convert2DateHistogramBucketAggCondition(Class<?> clazz, String bucketKey, boolean buildAggCriteria) {
        AggDateHistogramBucketAnnotation aggDateHistogramBucketAnnotation = clazz.getAnnotation(AggDateHistogramBucketAnnotation.class);
        if (aggDateHistogramBucketAnnotation == null) {
        	return null;
        }

        // 聚合条件--包含桶和指标
        AggCondition aggCondition = new AggCondition();
        aggCondition.setKey(bucketKey);
        
        // 桶
        AggDateHistogramBucket aggDateHistogramBucket = new AggDateHistogramBucket();
        aggDateHistogramBucket.setBoundsMax(aggDateHistogramBucketAnnotation.boundsMax());
        aggDateHistogramBucket.setBoundsMin(aggDateHistogramBucketAnnotation.boundsMin());
        aggDateHistogramBucket.setField(aggDateHistogramBucketAnnotation.field());
        aggDateHistogramBucket.setFormat(aggDateHistogramBucketAnnotation.format());
        aggDateHistogramBucket.setInterval(aggDateHistogramBucketAnnotation.interval());
        String minDocCount = aggDateHistogramBucketAnnotation.minDocCount();
        if (minDocCount != null && !minDocCount.equals("")) {
        	aggDateHistogramBucket.setMinDocCount(Long.parseLong(minDocCount));
        }
        
        String sortField = aggDateHistogramBucketAnnotation.sortField();
        if (sortField != null && !sortField.equals("")) {
        	aggDateHistogramBucket.setSorts(new Sort[]{new Sort(sortField, aggDateHistogramBucketAnnotation.sortType())});
        }


        AggBucket aggBucket = new AggBucket();
        aggBucket.setAggDateHistogramBucket(aggDateHistogramBucket);
        aggBucket.setAggBucketType(AggBucketType.AGG_BUCKET_DATE_HISTOGRAM);

        aggCondition.setAggBucket(aggBucket);
        
        if (buildAggCriteria) {
        	// 指标
            Field[] fields = getFields(clazz);
            aggCondition.setAggCriteria(buildAggCriteria(fields));
        }
        
    	return aggCondition;
    }
    
    /**
     * 只有指标的桶聚合条件--自定义bucketKey
     * 如果同种类型bucket并列使用时需要自定义bucketKey
     * @param clazz
     * @param bucketKey
     * @param buildAggCriteria
     * @return
     */
    private static AggCondition convert2NoneBucketAggCondition(Class<?> clazz, String bucketKey, boolean buildAggCriteria) {
        AggNoneBucketAnnotation aggNoneBucketAnnotation = clazz.getAnnotation(AggNoneBucketAnnotation.class);
        if (aggNoneBucketAnnotation == null) {
        	return null;
        }
        
        // 聚合条件--包含桶和指标
        AggCondition aggCondition = new AggCondition();
        aggCondition.setKey(bucketKey);
        
        // 桶
        AggBucket aggBucket = new AggBucket();
        aggBucket.setAggBucketType(AggBucketType.AGG_BUCKET_NONE);
        aggCondition.setAggBucket(aggBucket);
        
        if (buildAggCriteria) {
        	// 指标
            Field[] fields = getFields(clazz);
            aggCondition.setAggCriteria(buildAggCriteria(fields));
        }
        
    	return aggCondition;
    }

    /**
     * 全局桶聚合条件--自定义bucketKey
     * 如果同种类型bucket并列使用时需要自定义bucketKey
     * @param clazz
     * @param bucketKey
     * @param buildAggCriteria
     * @return
     */
    private static AggCondition convert2GlobalBucketAggCondition(Class<?> clazz, String bucketKey, boolean buildAggCriteria) {
    	AggGlobalBucketAnnotation aggGlobalBucketAnnotation = clazz.getAnnotation(AggGlobalBucketAnnotation.class);
    	if (aggGlobalBucketAnnotation == null) {
        	return null;
        }
        
        // 聚合条件--包含桶和指标
        AggCondition aggCondition = new AggCondition();
        aggCondition.setKey(bucketKey);
        
        // 桶
        AggBucket aggBucket = new AggBucket();
        aggBucket.setAggBucketType(AggBucketType.AGG_BUCKET_GLOBAL);
        aggCondition.setAggBucket(aggBucket);
        
        if (buildAggCriteria) {
        	// 指标
            Field[] fields = getFields(clazz);
            aggCondition.setAggCriteria(buildAggCriteria(fields));
        }
        
    	return aggCondition;
    }

    /**
     * 获取类的所有属性-包括从父类继承的属性
     * @param clz
     * @return
     */
    private static Field[] getFields(Class<?> clz) {
    	List<Field> fieldList = new ArrayList<>();
        // 子类获取不到时尝试获取父类
        for(; clz != Object.class ; clz = clz.getSuperclass()) {
        	Field[] fields = clz.getDeclaredFields();
        	if (fields != null && fields.length > 0) {
        		fieldList.addAll(Arrays.asList(fields));
        	}
        }

        Field[] result = new Field[fieldList.size()];
        fieldList.toArray(result);
        return result;
    }

    /**
     * 通过对象类型转换成聚合条件--自定义bucketKey
     * 如果时自定义bucketKey进行聚合操作，解析结果集时需要使用自定义bucketKey解析
     * 扫描对象类型@AggTermsBucketAnnotation注解，该注解包含了桶相关信息
     * 扫描对象类型@AggCriterionAnnotation注解，该注解包含了指标相关信息
     * @param clazz
     * @param bucketKey
     * @param buildAggCriteria
     * @return
     */
    private static AggCondition convert2TermsBucketAggCondition(Class<?> clazz, String bucketKey, boolean buildAggCriteria) {
        AggTermsBucketAnnotation aggTermsBucketAnnotation = clazz.getAnnotation(AggTermsBucketAnnotation.class);
        if (aggTermsBucketAnnotation == null) {
        	return null;
        }
        
        String[] groupBy = aggTermsBucketAnnotation.groupBy();
        String[] sortFields = aggTermsBucketAnnotation.sortField();
        SortType[] sortTypes = aggTermsBucketAnnotation.sortType();
        String size = aggTermsBucketAnnotation.size();
        // delimiter
        String delimiter = aggTermsBucketAnnotation.delimiter();
        
        // 聚合条件--包含桶和指标
        AggCondition aggCondition = new AggCondition();
        aggCondition.setKey(bucketKey);
        
        // 桶--group by
        AggTermsBucket aggTermsBucket = new AggTermsBucket();
        aggTermsBucket.setGroupBy(groupBy);
        
        // size
        if (size != null && !size.equals("")) {
        	aggTermsBucket.setSize(Integer.parseInt(size));
        }
        
        // sort
        if (sortFields != null && sortFields.length > 0) {
        	Sort[] sorts = buildSort(sortFields, sortTypes);
        	aggTermsBucket.setSorts(sorts);
        }

        // delimiter
        aggTermsBucket.setDelimiter(delimiter);
        AggBucket aggBucket = new AggBucket();
        aggBucket.setAggBucketType(AggBucketType.AGG_BUCKET_TERMS);
        aggBucket.setAggTermsBucket(aggTermsBucket);

        aggCondition.setAggBucket(aggBucket);
        
        if (buildAggCriteria) {
        	// 指标
            Field[] fields = getFields(clazz);
            aggCondition.setAggCriteria(buildAggCriteria(fields));
        }
        
    	return aggCondition;
    }
    
    /**
     * 构建桶Key
     * @param clazz
     * @param bucketType
     * @return
     */
    private static String buildBucketKey(Class<?> clazz, AggBucketType bucketType) {
    	// 类名+$+Bucket类型名称
    	return clazz.getSimpleName() + "$" + bucketType.name;
    }
    
    /**
     * 构建排序
     * @param sortFields
     * @param sortTypes
     * @return
     */
    private static Sort[] buildSort(String[] sortFields, SortType[] sortTypes) {
    	Sort[] sorts = new Sort[sortFields.length];
    	
    	for (int i = 0; i < sortFields.length; i++) {
    		Sort sort = new Sort();
            sort.setField(sortFields[i]);
            if (i >= sortTypes.length) {
            	sort.setType(SortType.ASC); // 默认升序
            } else {
            	sort.setType(sortTypes[i]);
            }
            sorts[i] = sort;
    	}
    	
    	return sorts;
    }
    
    /**
     * 构建TopHit指标
     * @param aggCriterionTopHitsAnnotation
     * @param field
     * @return
     */
    private static TopHitsAggCriterion buildTopHitAggCriteria(AggCriterionTopHitsAnnotation aggCriterionTopHitsAnnotation, Field field) {
    	// 处理TopHits指标
		TopHits topHits = new TopHits();
		String from = aggCriterionTopHitsAnnotation.from();
		String size = aggCriterionTopHitsAnnotation.size();
		String[] sortFields = aggCriterionTopHitsAnnotation.sortField();
        SortType[] sortTypes = aggCriterionTopHitsAnnotation.sortType();

        // from, size
        if (from != null && !from.equals("")) {
        	topHits.setFrom(Integer.parseInt(from));
        }
        if (size != null && !from.equals("")) {
        	topHits.setSize(Integer.parseInt(size));
        }
        
        // sort
        if (sortFields != null && sortFields.length > 0) {
        	Sort[] sorts = buildSort(sortFields, sortTypes);
        	topHits.setSorts(sorts);
        }
        
        // includes
        try {
        	ParameterizedType parameterizedType = (ParameterizedType)field.getGenericType();
            Type rawType = parameterizedType.getRawType();
            
            if (!rawType.getTypeName().equals(List.class.getTypeName())) {
            	throw new RuntimeException("the type of " + field.getName() + " should be java.util.List");
            }
            Type argumentType = parameterizedType.getActualTypeArguments()[0];
            Class<?> topHitsClz = Class.forName(argumentType.getTypeName());
        	String[] includes = convert2Includes(topHitsClz);
            topHits.setIncludes(includes);
		} catch (ClassNotFoundException e) {
			throw new RuntimeException(e);
		}
        
        TopHitsAggCriterion topHitsAggCriterion = new TopHitsAggCriterion();
        topHitsAggCriterion.setTopHits(topHits);
        topHitsAggCriterion.setCriterionType(AggCriterionType.AGG_CRITERION_TOP_HITS);
        topHitsAggCriterion.setKey(field.getName());
        
        return topHitsAggCriterion;
    }
    
    /**
     * 构建聚合指标
     * @param fields
     * @return
     */
    private static List<AggCriterion> buildAggCriteria(Field[] fields) {
    	List<AggCriterion> aggCriteria = new ArrayList<>();
        for (Field field : fields) {
        	AggCriterionAnnotation aggCriterionAnnotation = field.getAnnotation(AggCriterionAnnotation.class);
            if (aggCriterionAnnotation == null) {
                // 无一般指标注解--再次获取TopHits指标
            	AggCriterionTopHitsAnnotation aggCriterionTopHitsAnnotation = field.getAnnotation(AggCriterionTopHitsAnnotation.class);
            	if (aggCriterionTopHitsAnnotation != null) {
            		// 处理TopHits指标
                    TopHitsAggCriterion topHitsAggCriterion = buildTopHitAggCriteria(aggCriterionTopHitsAnnotation, field);
                    aggCriteria.add(topHitsAggCriterion);
            	}
            	
            	continue;
            }
            
            AggCriterionType aggCriterionType = aggCriterionAnnotation.aggCriterionType();
            String aggField = aggCriterionAnnotation.field();
            
            AggCriterion aggCriterion = new AggCriterion();
            aggCriterion.setField(aggField);
            aggCriterion.setCriterionType(aggCriterionType);
            aggCriterion.setKey(field.getName());
            
            aggCriteria.add(aggCriterion);
        }
        
        return aggCriteria;
    }
    
    /**
     * 将某一个聚合结果转换成Map类型
     * 对于嵌套桶只转换最外层信息
     * @param aggregations
     * @param clazz
     * @return
     */
    public static <T> Map<String, T> convertAggResult2MapByBucketKey(Map<String, AggResult> aggregations, Class<T> clazz, AggBucketType bucketType) {
        String bucketKey = buildBucketKey(clazz, bucketType);
    	return convertAggResult2MapByBucketKey(aggregations, clazz, bucketType, bucketKey);
    }

    /**
     * 将某一个聚合结果转换成Map类型--自定义bucketKey
     * 如果时自定义bucketKey进行聚合操作，解析结果集时需要使用自定义bucketKey解析
     * 对于嵌套桶只转换最外层信息
     * @param aggregations
     * @param clazz
     * @return
     */
    public static <T> Map<String, T> convertAggResult2MapByBucketKey(Map<String, AggResult> aggregations, Class<T> clazz, AggBucketType bucketType, String bucketKey) {
        Map<String, T> result = new HashMap<>();
    	
    	if (aggregations == null || aggregations.size() == 0) {
        	return result;
        }
    	
        // 获取目标结果集
        AggResult aggResult = aggregations.get(bucketKey);
        if (aggResult == null) {
        	return result;
        }
        
        List<AggResponseBucket> buckets = aggResult.getBuckets();
        if (buckets == null || buckets.size() == 0) {
        	return result;
        }
        
        // 每个bucket对应一个聚合对象
        for (AggResponseBucket bucket : buckets) {
        	T obj = convertBucketResult2Object(bucket.getResult(), clazz);
        	String key = bucket.getKey();
        	if (key == null || key.equals("")) {
        		// key值不存在
        		key = buildEmptyKey(buckets.indexOf(bucket));
        	}
        	result.put(key, obj);
        }
        
    	return result;
    }
    
    /**
     * 将某一个聚合结果转换成List类型
     * @param aggregations
     * @param clazz
     * @return
     */
    public static <T> List<ConvertedAggResponseBucket<T>> convertAggResult2ListByBucketKey(Map<String, AggResult> aggregations, Class<T> clazz, AggBucketType bucketType) {
    	String bucketKey = buildBucketKey(clazz, bucketType);
    	return convertAggResult2ListByBucketKey(aggregations, clazz, bucketType, bucketKey);
    }
    
    /**
     * 将某一个聚合结果转换成单个实体类
     * @param aggregations
     * @param clazz
     * @return
     */
    public static <T> ConvertedAggResponseBucket<T> convertAggResult2SingleByBucketKey(Map<String, AggResult> aggregations, Class<T> clazz, AggBucketType bucketType) {
    	List<ConvertedAggResponseBucket<T>> result = convertAggResult2ListByBucketKey(aggregations, clazz, bucketType);
    	if (result == null || result.size() == 0) {
    		return null;
    	}
    	
    	return result.get(0);
    }

    /**
     * 将某一个聚合结果转换成List类型
     * @param aggregations
     * @param clazz
     * @return
     */
    public static <T> List<ConvertedAggResponseBucket<T>> convertAggResult2ListByBucketKey(Map<String, AggResult> aggregations, Class<T> clazz, AggBucketType bucketType, String bucketKey) {
        List<ConvertedAggResponseBucket<T>> result = new ArrayList<>();
    	
    	if (aggregations == null || aggregations.size() == 0) {
        	return result;
        }
    	
        // 获取目标结果集
        AggResult aggResult = aggregations.get(bucketKey);
        if (aggResult == null) {
        	return result;
        }
        
        List<AggResponseBucket> buckets = aggResult.getBuckets();
        if (buckets == null || buckets.size() == 0) {
        	return result;
        }
        
        // 每个bucket对应一个聚合对象
        for (AggResponseBucket bucket : buckets) {
        	T obj = convertBucketResult2Object(bucket.getResult(), clazz);
        	ConvertedAggResponseBucket<T> single = new ConvertedAggResponseBucket<>();
        	String key = bucket.getKey();
        	if (key == null || key.equals("")) {
        		// key值不存在
        		key = buildEmptyKey(buckets.indexOf(bucket));
        	}
        	single.setKey(key);
        	single.setValue(obj);
        	single.setInnerAggResults(bucket.getInnerAggResults());
        	result.add(single);
        }
        
    	return result;
    }
    
    /**
     * 将某一个聚合结果转换成单个对象
     * @param aggregations
     * @param clazz
     * @return
     */
    public static <T> ConvertedAggResponseBucket<T> convertSingleAggResultByBucketKey(Map<String, AggResult> aggregations, Class<T> clazz, AggBucketType bucketType) {
    	String bucketKey = buildBucketKey(clazz, bucketType);
    	return convertSingleAggResultByBucketKey(aggregations, clazz, bucketType, bucketKey);
    }
    
    /**
     * 将某一个聚合结果转换成单个对象
     * @param aggregations
     * @param clazz
     * @return
     */
    public static <T> ConvertedAggResponseBucket<T> convertSingleAggResultByBucketKey(Map<String, AggResult> aggregations, Class<T> clazz, AggBucketType bucketType, String bucketKey) {
    	if (aggregations == null || aggregations.size() == 0) {
        	return null;
        }
    	
        // 获取目标结果集
        AggResult aggResult = aggregations.get(bucketKey);
        if (aggResult == null) {
        	return null;
        }
        
        List<AggResponseBucket> buckets = aggResult.getBuckets();
        if (buckets == null || buckets.size() != 1) {
        	return null;
        }
        
        AggResponseBucket bucket = buckets.get(0);
        // 每个bucket对应一个聚合对象
        T obj = convertBucketResult2Object(bucket.getResult(), clazz);
    	ConvertedAggResponseBucket<T> single = new ConvertedAggResponseBucket<>();
    	String key = bucket.getKey();
    	if (key == null || key.equals("")) {
    		// key值不存在
    		key = buildEmptyKey(buckets.indexOf(bucket));
    	}
    	single.setKey(key);
    	single.setValue(obj);
    	single.setInnerAggResults(bucket.getInnerAggResults());
        
    	return single;
    }
    
    /**
     * 构建key为空时默认key
     * @param i
     * @return
     */
    private static String buildEmptyKey(int i) {
    	// 这里使用的方案：使用关键字+数字
		// EMPYT_KEY_0, EMPYT_KEY_1, ..
    	return "EMPYT_KEY_" + i;
    }
    
    /**
     * 将key-value转换成对象
     * @param resultMap
     * @param clazz
     * @return
     */
    private static<T> T convertBucketResult2Object(Map<String, Object> resultMap, Class<T> clazz) {
    	return JSON.parseObject(JSON.toJSONString(resultMap), clazz);
    }
    
    private static boolean isEmpty(String value) {
    	if (value == null || "".equals(value.trim())) {
        	return true;
        }
    	
    	return false;
    }
}
