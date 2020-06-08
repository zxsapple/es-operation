package com.yundasys.es.operation.model.response;

import com.yundasys.es.operation.constant.ESErrorCode;

import java.io.Serializable;

public class ESBaseResponse<T> implements Serializable {

    private static final long serialVersionUID = -321165040443207334L;
    
    private int code;//一级状态码
    private int status;//二级状态码
    private T data;

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public ESBaseResponse() {}
    
    public ESBaseResponse(int code, T data) {
        this.code = code;
        this.data = data;
    }
    
    public ESBaseResponse(T data) {
        this.code = ESErrorCode.SUCCESS;
        this.data = data;
    }
    
    public int getCode() {
        return code;
    }
    public void setCode(int code) {
        this.code = code;
    }
    public T getData() {
        return data;
    }
    public void setData(T data) {
        this.data = data;
    }
}
