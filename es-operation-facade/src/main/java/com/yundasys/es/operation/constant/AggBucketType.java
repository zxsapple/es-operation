package com.yundasys.es.operation.constant;

import lombok.Data;

import java.io.Serializable;

public enum AggBucketType implements Serializable {
    AGG_BUCKET_TERMS("Terms", "terms", "group by"),
    AGG_BUCKET_FILTER("Filter", "filter", "过滤桶"),
    AGG_BUCKET_GLOBAL("Global", "global", "全局桶"),
    AGG_BUCKET_DATE_HISTOGRAM("DateHistogram", "date_histogram", "按时间聚合"),
    AGG_BUCKET_NONE("None", "none", "只有指标");

    public final String name;
    public final String value;
    public final String intro;

    AggBucketType(String name, String value, String intro) {
        this.name = name;
        this.value = value;
        this.intro = intro;
    }
}
