package com.yundasys.es.operation.model.request;

import lombok.Data;

@Data
public class ConvertedHit<T> {
	private T value; // value
	private String id; // id
	private Long version; // version

}
