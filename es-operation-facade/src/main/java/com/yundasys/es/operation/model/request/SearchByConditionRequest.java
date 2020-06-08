package com.yundasys.es.operation.model.request;

import lombok.Data;

@Data
public class SearchByConditionRequest extends BaseRequest {

    /* 条件检索用 */
    private SearchCondition searchCondition;

    //是否使用scroll
    boolean useScroll;

}
