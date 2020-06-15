package com.yundasys.es.operation.base;


import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 返回的查询 & 统计数据
 * @param <T> 页面数据 & 统计数据
 */
@Data
public class ClientResponseResult<D> implements Serializable {
	
	ClientPageInfo<D> pageInfo;

	List<? extends Object> summaryInfo;

    String scrollId;

}
