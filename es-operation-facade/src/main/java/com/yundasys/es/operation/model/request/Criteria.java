package com.yundasys.es.operation.model.request;

import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
public class Criteria implements Serializable {
    private static final long serialVersionUID = 7307266810297781432L;

    private List<SearchField> criteria;


    public Criteria() {
        criteria = new ArrayList<>();
    }

    public Criteria addCriterion(SearchField condition) {
        if (condition == null) {
            throw new RuntimeException("condition cannot be null");
        }
        criteria.add(condition);

        return this;
    }
}