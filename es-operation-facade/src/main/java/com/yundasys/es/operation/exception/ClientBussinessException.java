package com.yundasys.es.operation.exception;


public class ClientBussinessException extends RuntimeException {
	private static final long serialVersionUID = -7767159705208743978L;
	
	private int code;
    
    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public ClientBussinessException(int errorCode, String msg, Throwable e) {
        super(msg, e);
        this.code = errorCode;
    }
    
    public ClientBussinessException(int errorCode, String msg) {
        super(msg);
        this.code = errorCode;
    }
}
