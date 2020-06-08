package com.yundasys.es.operation.model.agg;

import com.yundasys.es.operation.model.Sort;
import lombok.Data;

import java.io.Serializable;

@Data
public class AggTermsBucket implements Serializable {
    
    private String[] groupBy; // group by 条件
    private Integer size; // 聚合后数量--ES不支持聚合后分页
    private Sort[] sorts; // 排序
    private String delimiter; // 分隔符

}
