package com.yundasys.es.operation.model.request;

import com.yundasys.es.operation.model.Sort;
import com.yundasys.es.operation.model.agg.AggDateCondition;
import lombok.Data;

import java.io.Serializable;

@Data
public class SearchCondition  implements Serializable {
    AggDateCondition aggDateCondition;//新聚合条件

    CompoundCriteria compoundCriteria; // 组合检索条件
    Integer pageNo; // 分页页码
    Integer pageSize; // 分页大小
    String[] includes; // 返回结果字段
    Sort[] sorts; // 排序

}
