package com.yundasys.es.operation.constant;

public enum SearchLogic {

    SL_MUST("must", "must", "must"),
    SL_SHOULD("should", "should", "should"),
    SL_MUST_NOT("must_not", "must_not", "must_not"),
    SL_FILTER("filter", "filter", "filter");

    //SL_NULL("", "", "");

    public final String name;
    public final String value;
    public final String intro;

    SearchLogic(String name, String value, String intro) {
        this.name = name;
        this.value = value;
        this.intro = intro;
    }
}
