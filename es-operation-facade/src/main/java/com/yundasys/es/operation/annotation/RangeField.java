package com.yundasys.es.operation.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.FIELD, ElementType.LOCAL_VARIABLE, ElementType.PARAMETER })
@Inherited
public @interface RangeField {
	String from() default "";
	boolean includeFrom() default true;
	String to() default "";
	boolean includeTo() default true;
}
