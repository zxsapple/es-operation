package com.yundasys.es.operation.exception;


public class ESOperationException extends RuntimeException {

    private static final long serialVersionUID = -1309219042672149450L;

    private int code;
    
    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public ESOperationException(int errorCode, String msg, Throwable e) {
        super(msg, e);
        this.code = errorCode;
    }
    
    public ESOperationException(int errorCode, String msg) {
        super(msg);
        this.code = errorCode;
    }
}
