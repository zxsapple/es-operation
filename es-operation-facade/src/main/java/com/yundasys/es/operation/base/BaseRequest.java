package com.yundasys.es.operation.base;

import lombok.Data;

import java.io.Serializable;

@Data
public class BaseRequest implements Serializable {
    /**
     * 页码
     */
    Integer pageNo=1;
    /**
     * 页面大小
     */
    Integer pageSize=50;

}
