package com.yundasys.es.operation.model;

import lombok.Data;

import java.io.Serializable;

@Data
public class TopHits implements Serializable {

    private Integer from; // from
    private Integer size; // size
    private String[] includes; // 返回结果字段
    private Sort[] sorts; // 排序
    

}
