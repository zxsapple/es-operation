package com.yundasys.es.operation.model.agg;

import com.yundasys.es.operation.constant.AggBucketType;
import com.yundasys.es.operation.model.Sort;
import lombok.Data;

import java.io.Serializable;

@Data
public class AggDateHistogramBucket implements Serializable{

    private String field; // 时间字段
    private String interval; // 统计区间-年，季度，月，周，日，时，分，秒等等--可使用提供的DateInterval或自定义
    private String format; // 返回的时间主键格式化
    private Long minDocCount; // 最小文档数--少于最小文档数的桶不返回（默认文档数为0的桶不返回）
    private String boundsMin; // 时间区间最小值
    private String boundsMax; // 时间区间最大值
    // 如果数据只落在了 4 月和 7 月之间，那么你只能得到这些月份的 buckets（可能为空也可能不为空）
    // 因此为了得到全年数据需要设置boundsMin，boundsMax
    private Sort[] sorts;


}
