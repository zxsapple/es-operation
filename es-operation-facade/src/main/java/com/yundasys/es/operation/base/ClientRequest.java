package com.yundasys.es.operation.base;

import lombok.Data;

import java.io.Serializable;

/**
 * 请求公共实体
 *
 * @param <T> 检索条件
 */
@Data
public class ClientRequest<T> extends BaseRequest implements Serializable {
    /**
     * 检索条件
     */
    T condition;

    /**
     * 排序 字段   驼峰
     */
    String sortField;
    /**
     * 排序方式 DESC:降序	ASC:升序
     */
    String sortType;



    /** 下载用游标信息 */
    /**
     * 是否使用游标信息
     */
    boolean useScroll;
    /**
     * 游标
     */
    String scrollId;
}
