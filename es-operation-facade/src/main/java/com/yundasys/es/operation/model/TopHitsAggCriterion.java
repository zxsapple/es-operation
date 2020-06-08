package com.yundasys.es.operation.model;

import com.yundasys.es.operation.model.agg.AggCriterion;
import lombok.Data;

@Data
public class TopHitsAggCriterion extends AggCriterion {
    
    private TopHits topHits;

}
