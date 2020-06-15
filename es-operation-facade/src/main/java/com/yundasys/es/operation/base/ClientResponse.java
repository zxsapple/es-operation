package com.yundasys.es.operation.base;

import lombok.Data;

import java.io.Serializable;

/**
 * 返回公共实体
 *
 * @param <T>
 */
@Data
public class ClientResponse<T> implements Serializable {

    int code;
    String msg;
    T data;

    public ClientResponse() {}

    public ClientResponse(int code, T data) {
        this.code = code;
        this.data = data;
    }

    public ClientResponse(T data) {
        this.code = 200;
        this.data = data;
    }
}
