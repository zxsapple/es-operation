package com.yundasys.es.operation.annotation;


import com.yundasys.es.operation.constant.SortType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE })
@Inherited
public @interface AggDateHistogramBucketAnnotation {
	//String key() default ""; // 聚合后主键
	String field(); // 聚合时间字段
	String interval(); // 统计区间
	String format() default ""; // 返回的时间主键格式化
	String minDocCount() default ""; // 最小文档数
	String boundsMin() default ""; // 时间区间最小值
	String boundsMax() default ""; // 时间区间最大值
	String sortField() default ""; // 排序字段
	SortType sortType() default SortType.ASC; // 排序方式
}
