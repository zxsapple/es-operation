package com.yundasys.es.operation.model.agg;

import com.yundasys.es.operation.constant.SortType;
import com.yundasys.es.operation.model.Sort;
import lombok.Data;

/**
 * @author zhengxiaosu
 * @desc
 * @date 2020/6/8 14:22
 */
@Data
public class AggCondition {
    /**
     * group by 的字段
     */
    String[] groupByFields;
    /**
     * 需要汇总的字段
     */
    CalField[] calFields;


    //以下 base

    String[] sortField;

    SortType[] sortType;

    Sort[] sorts;

    Integer size;
}

