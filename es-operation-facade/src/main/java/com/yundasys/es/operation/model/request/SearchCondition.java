package com.yundasys.es.operation.model.request;

import com.yundasys.es.operation.model.agg.AggCondition;
import com.yundasys.es.operation.model.request.BaseCondition;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class SearchCondition extends BaseCondition implements Serializable {
    private static final long serialVersionUID = 3694419708282427724L;
    
    private List<AggCondition> aggConditions; // 聚合条件

}
