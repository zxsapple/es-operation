package com.yundasys.es.operation.service;

import com.yundasys.es.operation.exception.ESOperationException;
import com.yundasys.es.operation.facade.IEsOperationFacade;
import com.yundasys.es.operation.facade.TestRequest;
import com.yundasys.es.operation.model.response.ESBaseResponse;
import com.yundasys.es.operation.constant.ESErrorCode;
import com.yundasys.es.operation.model.request.SearchByConditionRequest;
import com.yundasys.es.operation.model.request.SearchResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author zhengxiaosu
 * @desc
 * @date 2020/6/3 13:43
 */
@RestController
@Slf4j
public class EsOperationProvider implements IEsOperationFacade {
    @Autowired
    private EsOperationService esOperationService;


    @Override
    public String test(TestRequest request) {
        return "hello test"+request.getName();
    }

    @Override
    public ESBaseResponse<SearchResult> docQuery(SearchByConditionRequest request) {
        ESBaseResponse<SearchResult> response=new ESBaseResponse<>();
        try {
            response.setData(esOperationService.docQuery(request));
        } catch (ESOperationException e) {
            response.setCode(e.getCode());
        }catch (Exception e) {
            response.setCode(ESErrorCode.SYSTEM_ERROR);
            log.error("查询发生系统异常", e);
        }
        response.setCode(ESErrorCode.SUCCESS);
        return response;
    }

    @Override
    public ESBaseResponse<SearchResult> docQueryWithScrollId(String scrollId) {
        return null;
    }

    @Override
    public void scrollClear(String scrollId) {

    }
}
