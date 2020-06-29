package com.yundasys.es.operation.base;


import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.yundasys.es.operation.constant.ClientErrorCode;
import com.yundasys.es.operation.constant.ESErrorCode;
import com.yundasys.es.operation.constant.ElasticConstant;
import com.yundasys.es.operation.exception.ClientBussinessException;
import com.yundasys.es.operation.facade.IEsOperationFacade;
import com.yundasys.es.operation.model.IndexInfo;
import com.yundasys.es.operation.model.request.SearchByConditionRequest;
import com.yundasys.es.operation.model.request.SearchCondition;
import com.yundasys.es.operation.model.request.SearchResult;
import com.yundasys.es.operation.model.response.ESBaseResponse;
import com.yundasys.es.operation.util.ClientBeanUtils;
import com.yundasys.es.operation.util.ClientPageUtil;
import com.yundasys.es.operation.util.ConvertTool;
import com.yundasys.es.operation.model.agg.AggCondition;
import com.yundasys.es.operation.util.ConvertAggTool;
import com.yundasys.es.operation.model.agg.AggDateCondition;
import com.yundasys.es.operation.model.request.BaseAggInfo;
import com.yundasys.es.operation.model.request.BaseDateInfo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author zhengxiaosu
 * @desc 基础biz
 * @date 2018/6/29 16:22
 */
@Slf4j
@Component
public class BaseEsClient {


    @Autowired
    protected IEsOperationFacade esOperationFacade;

    /**
     * @return
     * @desc 查询es通用方法
     * @author zhengxiaosu
     * @date 2018/6/26 16:21
     */
    protected <D> ClientResponseResult<D> getEsResponse(ESRequest<D> esRequest) {
        SearchResult searchResult = queryES(esRequest);

        return setResult(esRequest, searchResult);
    }

    /**
     * 请求es返回原始数据
     */
    protected <D> SearchResult queryES(ESRequest<D> esRequest) {

        ClientRequest<?> request = esRequest.getRequest();
        if (request.isUseScroll()) {
            //使用游标查询(下载用到)
            esRequest.setAggInfo(null);
            esRequest.setDateInfo(null);
        }
        Object[] criteria = esRequest.getCriteria();
        BaseDateInfo dateInfo = esRequest.getDateInfo();
        BaseAggInfo aggInfo = esRequest.getAggInfo();
        IndexInfo indexInfo = esRequest.getIndexInfo();

        ESBaseResponse<SearchResult> esResponse;

        if (request.isUseScroll() && StringUtils.isNotEmpty(request.getScrollId())) {
            //使用游标查询 不需检索条件
            esResponse = esOperationFacade.docQueryWithScrollId(request.getScrollId());

        } else {
            // 检索条件转换
            ClientBeanUtils.copyProperties(request.getCondition(), criteria);
            SearchByConditionRequest searchRequest = new SearchByConditionRequest();
            SearchCondition condition = new SearchCondition();
            // 检索条件
            condition.setCompoundCriteria(ConvertTool.convert2CompoundCriteria(Arrays.asList(criteria)));
            // 分页信息
            condition.setPageNo(request.getPageNo());
            condition.setPageSize(request.getPageSize());

            // 聚合条件
            if (dateInfo == null) {//仅仅group by
                AggCondition AggCondition = ConvertAggTool.info2AggCondition(esRequest.getAggInfo());
                AggDateCondition aggDateCondition = new AggDateCondition();
                aggDateCondition.setAggCondition(AggCondition);
                condition.setAggDateCondition(aggDateCondition);
            } else {//有时间汇总
                condition.setAggDateCondition(ConvertAggTool.info2AggDateCondition(esRequest.getDateInfo(), esRequest.getAggInfo()));
            }

            // 设置排序
            condition.setSorts(ClientPageUtil.setSortInfo(request));

            searchRequest.setIndexInfo(indexInfo);
            searchRequest.setSearchCondition(condition);

            log.info("请求es组件参数:{}", JSON.toJSONString(searchRequest));

            if (request.isUseScroll()) {//第一次使用游标查询
                searchRequest.setUseScroll(true);

            }

            esResponse = esOperationFacade.docQuery(searchRequest);

        }
        if (esResponse == null || esResponse.getCode() != ESErrorCode.SUCCESS) {
            log.warn("请求es失败:{}", esResponse.getData());
            throw new ClientBussinessException(ClientErrorCode.ES_OPERATE_FAIL, esResponse.getData() + "");
        }
        SearchResult searchResult = esResponse.getData();
        if (searchResult.getTotal() == 0) {
            // 结果为空
            log.info("请求es ,空结果集");

        }

        log.info("ES中间件返回 searchResult:{}", searchResult);

        if (request.isUseScroll() && request.getPageNo() * request.getPageSize() >= searchResult.getTotal()) {
            //游标查询最后一次查询 调服务清除游标
            esOperationFacade.scrollClear(request.getScrollId());
        }

        return searchResult;
    }

    /**
     * 将es返回的数据进行结果集封装
     */
    private <D> ClientResponseResult<D> setResult(ESRequest<D> esRequest, SearchResult searchResult) {

        Class<D> dtoClz = esRequest.getDtoClz();

        // 转换hits数据 + total
        ClientResponseResult<D> result = new ClientResponseResult<>();
        ClientPageInfo<D> pageInfo = ClientPageUtil.page(searchResult, dtoClz);
        result.setPageInfo(pageInfo);
        //设置汇总
        result.setSummaryInfo(setSummaryInfo(esRequest, searchResult));

        result.setScrollId(searchResult.getScrollId());
        return result;
    }

    /**
     * 客户端的agg信息 封装
     */
    private <D> List<? extends Object> setSummaryInfo(ESRequest<D> esRequest, SearchResult searchResult) {

        if (CollectionUtils.isEmpty(searchResult.getAggregations())) {
            return null;
        }

        BaseDateInfo dateInfo = esRequest.getDateInfo();
        BaseAggInfo aggInfo = esRequest.getAggInfo();
        List<JSONObject> aggregations = searchResult.getAggregations();

        if (dateInfo != null) {//有时间汇总
            List<BaseDateInfo> aggResultList = new ArrayList<>();
            for (JSONObject aggregation : aggregations) {
                BaseDateInfo dateInfoResult = aggregation.toJavaObject(dateInfo.getClass());
                if (aggInfo != null) {//内部还有再封装
                    List<? extends BaseAggInfo> innerGroupBy = aggregation.getJSONArray(ElasticConstant.DATE_HISTOGRAM_JSON_INNER_KEY).toJavaList(aggInfo.getClass());
                    dateInfoResult.setAggInfo(innerGroupBy);
                }
                aggResultList.add(dateInfoResult);
            }
            return aggResultList;
        } else if (aggInfo != null) {
            List<BaseAggInfo> aggResultList = new ArrayList<>(aggregations.size());
            for (JSONObject aggregation : aggregations) {
                aggResultList.add(aggregation.toJavaObject(aggInfo.getClass()));
            }
            return aggResultList;
        }
        return null;
    }

    /**
     * 根据条件只查total数
     */
    protected long getEsSizeNum(ESRequest esRequest) {
        esRequest.setAggInfo(null);
        esRequest.setDateInfo(null);
        esRequest.setDtoClz(null);
        esRequest.getRequest().setPageSize(0);
        SearchResult searchResult = queryES(esRequest);
        return searchResult.getTotal();
    }
}
