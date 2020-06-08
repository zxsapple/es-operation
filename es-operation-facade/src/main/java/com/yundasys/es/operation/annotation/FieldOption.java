package com.yundasys.es.operation.annotation;


import com.yundasys.es.operation.constant.SearchLogic;
import com.yundasys.es.operation.constant.SearchType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.FIELD, ElementType.LOCAL_VARIABLE, ElementType.PARAMETER })
@Inherited
public @interface FieldOption {
	String name() default "";//单值匹配时使用  默认值是其属性名称

	String[] names() default "";
	
	SearchLogic searchLogic() default SearchLogic.SL_FILTER;

	SearchType searchType();
}
