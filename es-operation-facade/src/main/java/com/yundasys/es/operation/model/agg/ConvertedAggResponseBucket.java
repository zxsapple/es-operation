package com.yundasys.es.operation.model.agg;

import lombok.Data;

import java.io.Serializable;
import java.util.Map;

@Data
public class ConvertedAggResponseBucket<T> implements Serializable {

	private String key;
	private T value;
	// 嵌套聚合信息--因为嵌套聚合的桶类型和结果类型不确定，这里保持原生返回结果，可进一步解析
	private Map<String, AggResult> innerAggResults;

}
