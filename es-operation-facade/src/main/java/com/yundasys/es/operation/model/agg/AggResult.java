package com.yundasys.es.operation.model.agg;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class AggResult implements Serializable {

    List<AggResponseBucket> buckets;

}
