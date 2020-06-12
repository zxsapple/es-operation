package com.yundasys.es.operation.annotation.agg;


import com.yundasys.es.operation.constant.AggCriterionType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 汇总计算的字段
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
@Inherited
public @interface AggCalField {
    // 聚合字段为属性名称
    AggCriterionType aggCriterionType(); // 聚合类型
}