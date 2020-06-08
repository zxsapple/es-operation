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
public @interface AggTermsBucketAnnotation {
	String[] groupBy();
	//String key() default "";
	String[] sortField() default {};
	SortType[] sortType() default {};
	String size() default "";
	String delimiter() default "";
}
