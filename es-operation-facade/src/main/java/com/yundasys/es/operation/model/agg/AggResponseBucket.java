package com.yundasys.es.operation.model.agg;

import lombok.Data;

import java.io.Serializable;
import java.util.Map;

@Data
public class AggResponseBucket implements Serializable {

    String key; // key
    Map<String, Object> result; // 聚合结果
    Map<String, AggResult> innerAggResults; // 嵌套聚合信息
}
