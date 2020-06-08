package com.yundasys.es.operation.model;

import lombok.Data;

import java.io.Serializable;

@Data
public class Hit implements Serializable {
	String value; //具体的json
	String id;
	Long version;
	String script; //脚本
	String index;
}
