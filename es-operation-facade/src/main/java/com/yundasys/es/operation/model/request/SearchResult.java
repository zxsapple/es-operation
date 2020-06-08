package com.yundasys.es.operation.model.request;

import com.yundasys.es.operation.model.Hit;
import com.yundasys.es.operation.model.agg.AggResult;
import lombok.Data;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

@Data
public class SearchResult implements Serializable {

    long total; // 总量
    List<Hit> hits; // 结果集
    Map<String, AggResult> aggregations; // 聚合结果--map结构json
    String scrollId; // 游标--只使用于游标查询

}
