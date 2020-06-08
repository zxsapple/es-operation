package com.yundasys.es.operation.annotation;


import com.yundasys.es.operation.constant.SortType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.FIELD, ElementType.LOCAL_VARIABLE, ElementType.PARAMETER })
@Inherited
public @interface AggCriterionTopHitsAnnotation {
	String from() default "";
	String size() default "";
	String[] sortField() default {};
	SortType[] sortType() default {};
}
