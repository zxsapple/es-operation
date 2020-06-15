package com.yundasys.es.operation.model.request;

import com.yundasys.es.operation.constant.SortType;
import lombok.Data;

/**
 * @author zhengxiaosu
 * @desc  日期汇总基础类
 * @date 2020/6/8 14:16
 */
@Data
public abstract class BaseAggInfo {
    String[] sortField;//排序字段

    SortType[] sortType;//排序方式

    Long size;//聚合的最大数据量 越大越耗es性能
}

