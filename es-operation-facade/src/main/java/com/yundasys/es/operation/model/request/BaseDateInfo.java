package com.yundasys.es.operation.model.request;

import com.yundasys.es.operation.constant.SortType;
import lombok.Data;

import java.util.List;

/**
 * @author zhengxiaosu
 * @desc 普通汇总基础类
 * @date 2020/6/8 15:34
 */
@Data
public abstract class BaseDateInfo {
    String interval; // 统计区间   DateInterval.class类里有常用的

    SortType sortType; // 排序方式

    String sortField;// key(时间) 或者 count(数量)

    List<? extends BaseAggInfo> aggInfo;//客户端获取结果后使用 上游不用自己set
}
