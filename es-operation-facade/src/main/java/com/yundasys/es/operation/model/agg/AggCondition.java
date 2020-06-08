package com.yundasys.es.operation.model.agg;

import lombok.Data;

import java.io.Serializable;
import java.util.List;
@Data
public class AggCondition implements Serializable {

     String key; // 聚合后主键
     AggBucket aggBucket; // 桶
     List<AggCriterion> aggCriteria; // 指标
     List<AggCondition> innerAggConditions; // 嵌套桶条件

}