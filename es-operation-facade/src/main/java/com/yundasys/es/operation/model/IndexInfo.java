package com.yundasys.es.operation.model;

import lombok.Data;

import java.io.Serializable;

@Data
public class IndexInfo implements Serializable{

    private String[] indexes;

    public void setIndexes(String... indexes) {
        this.indexes = indexes;
    }
}
