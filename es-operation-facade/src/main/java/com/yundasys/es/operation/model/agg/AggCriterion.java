package com.yundasys.es.operation.model.agg;

import com.yundasys.es.operation.constant.AggCriterionType;
import lombok.Data;

import java.io.Serializable;

@Data
public class AggCriterion implements Serializable {
    String key; // 聚合结果key
    String field; // 域
    AggCriterionType criterionType; // 聚合类型
}
