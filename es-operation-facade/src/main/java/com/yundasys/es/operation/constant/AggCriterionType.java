package com.yundasys.es.operation.constant;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public enum AggCriterionType implements Serializable {
    AGG_CRITERION_MIN("min", "min", "min"),
    AGG_CRITERION_MAX("max", "max", "max"),
    AGG_CRITERION_SUM("sum", "sum", "sum"),
    AGG_CRITERION_AVG("avg", "avg", "avg"),
    AGG_CRITERION_COUNT("count", "count", "count"),
    /// ....
    ERROR("", "", "");

    private final String name;
    private final String value;
    private final String intro;

    AggCriterionType(String name, String value, String intro) {
        this.name = name;
        this.value = value;
        this.intro = intro;
    }

    public static Map<String, AggCriterionType> map = new HashMap<>();
    static {
        AggCriterionType[] types = AggCriterionType.values();
        for (AggCriterionType type : types) {
            map.put(type.name, type);
        }
    }

    public static AggCriterionType getTypeByName(String name) {
        return map.get(name);
    }

}
