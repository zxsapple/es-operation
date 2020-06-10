package com.yundasys.es.operation.model.request;

import com.yundasys.es.operation.constant.SortType;
import lombok.Data;

/**
 * @author zhengxiaosu
 * @desc
 * @date 2020/6/8 14:16
 */
@Data
public class BaseAggInfo {
    String[] sortField;//排序字段

    SortType[] sortType;//排序方式

    Long size;
}

