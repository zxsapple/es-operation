package com.yundasys.es.operation.util;

import com.alibaba.fastjson.JSON;
import com.yundasys.es.operation.constant.ClientErrorCode;
import com.yundasys.es.operation.base.ClientPageInfo;
import com.yundasys.es.operation.base.ClientRequest;
import com.yundasys.es.operation.exception.ClientBussinessException;
import com.yundasys.es.operation.model.Hit;
import com.yundasys.es.operation.model.Sort;
import com.yundasys.es.operation.constant.SortType;
import com.yundasys.es.operation.model.request.SearchResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
public class ClientPageUtil {

    private static final Set<String> sortNames = new HashSet<>();
    static {
        SortType[] sortTypes = SortType.values();
        for (SortType sortType : sortTypes) {
            sortNames.add(sortType.name());
        }
    }


    public static <T> ClientPageInfo<T> page(SearchResult searchResult, Class<T> targetClz) {

        List<Hit> hits = searchResult.getHits();
        List<T> targetList = new ArrayList<>(hits.size());

        for (Hit hit : hits) {
            T target = JSON.parseObject(hit.getValue(), targetClz);
            targetList.add(target);
        }
        ClientPageInfo<T> pageInfo = new ClientPageInfo<>();
        pageInfo.setTotal(searchResult.getTotal());
        pageInfo.setList(targetList);
        return pageInfo;
    }

    /**
     * 设置查询的排序信息
     *
     * @param request
     * @param condition
     */
    public static Sort[] setSortInfo(ClientRequest request) {
        if (StringUtils.isNotEmpty(request.getSortField())) {

                Sort[] sorts = new Sort[]{new Sort()};
                sorts[0].setField(request.getSortField());
                if (StringUtils.isNotEmpty(request.getSortType())) {
                    if (sortNames.contains(request.getSortType().toUpperCase())) {
                        sorts[0].setType(SortType.valueOf(request.getSortType().toUpperCase()));
                    }else{
                        throw new ClientBussinessException(ClientErrorCode.PARAMETER_INCORRECT, "排序方式字段有误:" + request.getSortType());
                    }
                }else{
                    sorts[0].setType(SortType.ASC);
                }
                return sorts;

        }
        return null;
    }
}
