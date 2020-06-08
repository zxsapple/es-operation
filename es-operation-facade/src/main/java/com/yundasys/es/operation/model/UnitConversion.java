package com.yundasys.es.operation.model;

import lombok.Data;

import java.io.Serializable;

@Data
public class UnitConversion implements Serializable {
    private static final long serialVersionUID = -7344803945708275179L;
    
    private Double rate; // 倍率
    private String format; // format: #0.00

}
