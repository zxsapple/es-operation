package com.yundasys.es.operation.facade;

import com.yundasys.es.operation.model.request.SearchByConditionRequest;
import com.yundasys.es.operation.model.request.SearchResult;
import com.yundasys.es.operation.model.response.ESBaseResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * @author zhengxiaosu
 * @desc
 * @date 2020/6/2 16:47
 */
@FeignClient("es-operation-service")
public interface IEsOperationFacade {

//    /**
//     * 批量插入文档
//     */
//    void docBulkInsert(List<Hit> hits)  ;
//    /**
//     * 文档更新
//     */
//    void docBulkUpdate(List<Hit> hits) ;

    /**
     * 文档查询
     */
    @PostMapping("docQuery")
    ESBaseResponse<SearchResult> docQuery(@RequestBody SearchByConditionRequest request) ;

    /**
     * 游标id 查询--后续查询
     */
    @PostMapping("docQueryWithScrollId")
    ESBaseResponse<SearchResult> docQueryWithScrollId(String scrollId);
    /**
     * 清除游标
     */
    @PostMapping("scrollClear")
    void scrollClear(String scrollId) ;

//    /**
//     * 文档删除--条件删除-使用组合查询条件
//     */
//    void docDeleteByQuery(IndexInfo indexInfo, CompoundCriteria compoundCriteria) ;


}
