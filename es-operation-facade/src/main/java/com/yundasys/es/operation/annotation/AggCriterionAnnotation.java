package com.yundasys.es.operation.annotation;


import com.yundasys.es.operation.constant.AggCriterionType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.FIELD, ElementType.LOCAL_VARIABLE, ElementType.PARAMETER })
@Inherited
public @interface AggCriterionAnnotation {
	String field(); // 聚合域
	//String key() default ""; // 聚合后主键
	AggCriterionType aggCriterionType(); // 聚合类型
}
