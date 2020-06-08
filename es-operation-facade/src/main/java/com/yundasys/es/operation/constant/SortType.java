package com.yundasys.es.operation.constant;

public enum SortType {
    DESC(0, "desc"),
    ASC(1, "asc");

    private int code;
    private String type;

    private SortType(int code, String type) {
        this.code = code;
        this.type = type;
    }
    
    public int getCode() {
        return code;
    }

    public String getType() {
        return type;
    }
}
