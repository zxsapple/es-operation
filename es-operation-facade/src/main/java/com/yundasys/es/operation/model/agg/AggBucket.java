package com.yundasys.es.operation.model.agg;

import com.yundasys.es.operation.constant.AggBucketType;
import lombok.Data;

import java.io.Serializable;

@Data
public class AggBucket implements Serializable {
    AggBucketType aggBucketType;

    AggTermsBucket aggTermsBucket;

    AggFilterBucket aggFilterBucket;

    AggDateHistogramBucket aggDateHistogramBucket;
}
