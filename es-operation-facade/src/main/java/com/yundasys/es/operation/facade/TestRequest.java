package com.yundasys.es.operation.facade;

import lombok.Data;

import java.io.Serializable;

/**
 * @author zhengxiaosu
 * @desc
 * @date 2020/6/4 13:42
 */@Data
public class TestRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    String name;
}
