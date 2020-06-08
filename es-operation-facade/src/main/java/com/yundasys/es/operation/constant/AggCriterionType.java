package com.yundasys.es.operation.constant;

import java.io.Serializable;

public enum AggCriterionType implements Serializable {
    AGG_CRITERION_MIN("min", "min", "min"),
    AGG_CRITERION_MAX("max", "max", "max"),
    AGG_CRITERION_SUM("sum", "sum", "sum"),
    AGG_CRITERION_AVG("avg", "avg", "avg"),
    AGG_CRITERION_COUNT("count", "count", "count"),
    AGG_CRITERION_TOP_HITS("top hits", "top hits", "top hits"),
    /// ....
    ERROR("", "", "");

    public final String name;
    public final String value;
    public final String intro;

    AggCriterionType(String name, String value, String intro) {
        this.name = name;
        this.value = value;
        this.intro = intro;
    }
}
