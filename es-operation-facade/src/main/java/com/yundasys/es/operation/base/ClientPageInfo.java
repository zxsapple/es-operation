package com.yundasys.es.operation.base;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 结果集 & 总记录数
 * @param <T> 结果集
 */
@Data
public class ClientPageInfo<T> implements Serializable {
    private static final long serialVersionUID = -4973069067478682582L;
    /**
     * 总记录数
     */
    long total;
    /**
     * 结果集
     */
    List<T> list;

    public ClientPageInfo() {}

    public ClientPageInfo(long total, List<T> list) {
        this.total = total;
        this.list = list;
    }

}
