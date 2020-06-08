package com.yundasys.es.operation.base;


import lombok.Data;

import java.io.Serializable;

/**
 * 返回的查询 & 统计数据
 * @param <T> 页面数据 & 统计数据
 */
@Data
public class ClientResponseResult<D,S> implements Serializable {
	private static final long serialVersionUID = 6544107910440001815L;
	
	ClientPageInfo<D> pageInfo;

	S summary;

    String scrollId;

}
