package com.yundasys.es.operation.model.agg;

import com.yundasys.es.operation.constant.AggBucketType;
import com.yundasys.es.operation.model.request.CompoundCriteria;
import lombok.Data;

import java.io.Serializable;

@Data
public class AggFilterBucket implements Serializable {

    private CompoundCriteria compoundCriteria; // 组合过滤条件

}
