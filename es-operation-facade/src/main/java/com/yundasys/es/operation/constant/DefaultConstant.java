package com.yundasys.es.operation.constant;

public interface DefaultConstant {
    // ES冲突时重试次数
    int RETRY_ON_CONFLICT = 1;
    
    // 查询失败时重试次数
    int RETRY_ON_QUERY_FAIL = 1;
    
    // TERMS查询最大支持数量
    int TERMS_MAX_SIZE = 1024;
    
    // TERMS查询最大支持数量--Filter查询
    int FILTER_TERMS_MAX_SIZE = 4000;

    //app默认流量
    int DEFAULT_PERMITS = 10;

}
