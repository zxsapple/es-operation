package com.yundasys.es.operation.model.agg;

import com.yundasys.es.operation.constant.SortType;
import lombok.Data;

/**
 * @author zhengxiaosu
 * @desc 感觉时间维度汇总 内层可嵌套AggNewCondition
 * @date 2020/6/8 15:21
 */
@Data
public class AggDateCondition {
    AggCondition aggCondition;//普通group by汇总

    String dateField; // 聚合时间字段

    String interval; // 统计区间

    SortType sortType;  // 排序方式

    String sortField;// key(时间) 或者 count(数量)

    CalField[] calFields;//需要汇总的字段


}
