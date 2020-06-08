package com.yundasys.es.operation.model.request;

import com.yundasys.es.operation.constant.SearchLogic;
import com.yundasys.es.operation.constant.SearchType;
import lombok.Data;

import java.io.Serializable;

@Data
public class SearchField implements Serializable {

    private String name; // 域
    private Object value; // 值
    private SearchLogic searchLogic; // 检索逻辑
    private SearchType searchType; // 检索类型
    private float boost = 1.0f; // 相关性


    private String[] names; // 多域

}
