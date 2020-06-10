package com.yundasys.es.operation.model.agg;

import com.yundasys.es.operation.constant.AggCriterionType;
import lombok.Data;

/**
 * @author zhengxiaosu
 * @desc
 * @date 2020/6/8 14:53
 */
@Data
public class CalField {
    String field; // 聚合字段
    AggCriterionType criterionType; // 聚合类型
}
