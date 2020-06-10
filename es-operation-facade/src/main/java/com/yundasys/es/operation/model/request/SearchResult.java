package com.yundasys.es.operation.model.request;

import com.alibaba.fastjson.JSONObject;
import com.yundasys.es.operation.model.Hit;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class SearchResult implements Serializable {

    long total; // 总量
    List<Hit> hits; // 结果集
    List<JSONObject> aggregations; // 聚合结果--map结构json
    String scrollId; // 游标--只使用于游标查询

}
