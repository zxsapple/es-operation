package com.yundasys.es.operation.model.request;

import com.yundasys.es.operation.constant.ESErrorCode;
import com.yundasys.es.operation.exception.ESOperationException;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
public class Criteria implements Serializable {

    private List<SearchField> criteria;


    public Criteria() {
        criteria = new ArrayList<>();
    }

    public Criteria addCriterion(SearchField condition) {
        if (condition == null) {
            throw new ESOperationException(ESErrorCode.PARAMETER_INCORRECT, "condition cannot be null");
        }
        criteria.add(condition);

        return this;
    }
}