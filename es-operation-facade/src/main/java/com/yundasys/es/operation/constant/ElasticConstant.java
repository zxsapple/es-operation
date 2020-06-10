package com.yundasys.es.operation.constant;

public interface ElasticConstant {
    // 批量插入/更新最大量
    int UPSERT_MAX_SIZE = 5000;
    
    // 最大页码
    int SEARCH_MAX_PAGE_NO = 500000;
    
    // 最大每页数量
    int SEARCH_MAX_PAGE_SIZE = 10000;
    
    // 默认每页数量
    int SEARCH_DEFALUT_PAGE_SIZE = 50;
    
    // 聚合桶最大大小
    int AGG_MAX_BUCKET_SIZE = 100000;
    
    // tophits-size最大值
    int TOP_HITS_MAX_SIZE = 1000;
    
    // 嵌套桶最大层数
    int INNER_BUCKET_MAX_LOOP = 5;

    String GROUP_BY_KEY = "groupByKey";//groupby key名称

    String FIELD_CAL_SUFFIX = "Key";//统计字段的key后缀

    String SCRIPT_MULTI_GROUP_BY_SPLIT = "##";//group by 脚本分割符

    String DATE_HISTOGRAM_JSON_INNER_KEY = "innerGroupBy";//处理date agg信息时候里面一层的list jsonkey
}
