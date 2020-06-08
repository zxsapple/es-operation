package com.yundasys.es.operation.constant;

public interface ClientErrorCode {
	/** 正常 */
    int SUCCESS = 200;

    /** 参数错误 */
    int PARAMETER_INCORRECT = 460;

    /** 封装参数错误 */
    int SET_PARAMETER_ERROR = 550;

    /** ES操作失败 */
    int ES_OPERATE_FAIL = 461;

    /** 结果集为空 */
    int RESULT_EMPTY = 462;

    /** 结果集溢出 */
    int RESULT_OVERFLOW = 463;

    /** 下载操作失败 */
    int DOWNLOAD_OPERATE_FAIL = 464;

    /** 请求参数日期超过期限 */
    int PARAMETER_DATE_OVERFLOW = 465;
    /** 请求es超时 */
    int TIME_OUT = 467;

    /** 封装结果集错误 */
    int RESULT__INSTALL_ERROR = 470;

    /** 重算失败 */
    int RECALCULATION_FAIL = 480;

    /** 超过重新计算的限额 */
    int RECALCULATION_SIZE_OVERFLOW = 481;

    /** 服务调用异常 */
    int RPC_ERROR = 490;

    /** 系统异常 */
    int SYSTEM_ERROR = 500;


}
