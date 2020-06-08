package com.yundasys.es.operation.constant;

public interface ESErrorCode {
    // 正常
    int SUCCESS = 200;
    // 无权限
    int UNAUTHORIZED = 401;
    // 无效index
    int INVALID_INDEX = 451;
    // 无效type
    int INVALID_TYPE = 452;
    // 批处理数量超限
    int BATCH_MAX_SIZE_OVERFLOW = 453;
    // 页码超限
    int PAGE_NO_OVERFLOW = 454;
    // 页面大小超限
    int PAGE_SIZE_OVERFLOW = 455;
    // ES操作失败-版本冲突
    int ES_OPERATE_VERSION_CONFLICT = 457;
    // ES批量操作存在失败
    int ES_BATCH_OPERATE_HAS_FAIL = 458;
    // ES操作失败
    int ES_OPERATE_FAIL = 459;
    // 参数错误
    int PARAMETER_INCORRECT = 460;
    // cache操作失败
    int CACHE_OPERATE_FAIL = 461;
    // 文件上传失败
    int FILE_UPLOAD_FAIL = 462;
    // 无可用资源
    int NO_RESOURCE_ACCESSIBLE = 463;
    // 嵌套桶最大层数限制
    int INNER_BUCKET_MAX_LOOP_LIMIT = 464;
    // 聚合结果最大数量限制
    int AGG_RESPONSE_SIZE_LIMIT = 465;
    // AppInfo请求失败
    int APP_INFO_REQUEST_FAIL = 466;
    // 操作超时
    int OP_TIMEOUT = 467;
    // 服务熔断
    int STATUS_RED = 468;
    //服务降级 不返回聚合信息
    int STATUS_YELLOW = 469;
    // 系统异常
    int SYSTEM_ERROR = 500;
}
