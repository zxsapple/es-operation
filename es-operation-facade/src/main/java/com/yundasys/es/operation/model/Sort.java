package com.yundasys.es.operation.model;

import com.yundasys.es.operation.constant.SortType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor                 //无参构造
@AllArgsConstructor                //有参构造
public class Sort implements Serializable {
    String field;
    SortType type;
}
