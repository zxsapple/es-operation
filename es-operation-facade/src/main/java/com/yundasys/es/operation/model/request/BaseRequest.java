package com.yundasys.es.operation.model.request;

import com.yundasys.es.operation.model.IndexInfo;
import lombok.Data;

import java.io.Serializable;

@Data
public class BaseRequest implements Serializable {
	private IndexInfo indexInfo;

}
