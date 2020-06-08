package com.yundasys.es.operation.base;


import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.yundasys.es.operation.annotation.AggTermsBucketAnnotation;
import com.yundasys.es.operation.constant.DefaultConstant;
import com.yundasys.es.operation.constant.ESErrorCode;
import com.yundasys.es.operation.constant.ClientErrorCode;
import com.yundasys.es.operation.exception.ClientBussinessException;
import com.yundasys.es.operation.facade.IEsOperationFacade;
import com.yundasys.es.operation.model.*;
import com.yundasys.es.operation.constant.AggBucketType;
import com.yundasys.es.operation.model.agg.AggCondition;
import com.yundasys.es.operation.model.agg.AggResult;
import com.yundasys.es.operation.model.agg.ConvertedAggResponseBucket;
import com.yundasys.es.operation.model.request.SearchByConditionRequest;
import com.yundasys.es.operation.model.request.SearchCondition;
import com.yundasys.es.operation.model.request.SearchResult;
import com.yundasys.es.operation.model.response.ESBaseResponse;
import com.yundasys.es.operation.util.ClientStringUtil;
import com.yundasys.es.operation.util.ConvertTool;
import com.yundasys.es.operation.util.ClientBeanUtils;
import com.yundasys.es.operation.util.ClientPageUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;

import java.util.*;

/**
 * @author zhengxiaosu
 * @desc 基础biz
 * @date 2018/6/29 16:22
 */
@Slf4j
public class BaseBiz {


    @Autowired
    protected IEsOperationFacade esOperationFacade;

    /**
     * 获得es的查询数量
     * @param requestCondition 前台传过来的条件
     * @param criteria         查询条件
     * @param indexInfo        索引信息
     * @return
     */
    protected long getEsSizeNum(Object requestCondition, Object [] criteria, IndexInfo indexInfo) {
        ClientBeanUtils.copyProperties(requestCondition, criteria);
        SearchByConditionRequest searchRequest = new SearchByConditionRequest();
        SearchCondition condition = new SearchCondition();
        // 检索条件
        condition.setCompoundCriteria(ConvertTool.convert2CompoundCriteria(Arrays.asList(criteria)));
        searchRequest.setIndexInfo(indexInfo);
        searchRequest.setSearchCondition(condition);
        log.info("请求es参数docSizeQuery:{}", searchRequest);
        ESBaseResponse<SearchResult> sizeResponse = esOperationFacade.docQuery(searchRequest);

        if (sizeResponse.getCode() != ESErrorCode.SUCCESS) {
            log.warn("查询es失败, 原因:{}", sizeResponse.getData());
            throw new ClientBussinessException(ClientErrorCode.ES_OPERATE_FAIL, sizeResponse.getData() + "");
        }
        return sizeResponse.getData().getTotal();
    }

    /**
     * @return
     * @desc 查询es通用方法
     * @author zhengxiaosu
     * @date 2018/6/26 16:21
     */
    protected <A, D, S> ClientResponse<ClientResponseResult<D,S>> getEsResponse(ESRequest<A, D,S> esRequest) {
        SearchResult searchResult = queryES(esRequest);

        return setResult(esRequest, searchResult);
    }

    /**
     * 请求es返回原始数据
     * @param esRequest
     * @return
     */
    protected <A, D, S> SearchResult queryES(ESRequest<A, D,S> esRequest) {
        ClientRequest<?> request = esRequest.getRequest();
        if (request.isUseScroll()) {
            //使用游标查询(下载用到)
            esRequest.setAggClz(null);
            esRequest.setOutAggClz(null);
        }
        Object[] criteria = esRequest.getCriteria();
//        Class<H> hitClz = esRequest.getHitClz();
        Class outAggClz = esRequest.getOutAggClz();
        Class<A> aggClz = esRequest.getAggClz();
        IndexInfo indexInfo = esRequest.getIndexInfo();

        ESBaseResponse<SearchResult> esResponse;

        if (request.isUseScroll() && ClientStringUtil.isNotEmpty(request.getScrollId())) {
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

//            if (hitClz != null) {
//                // 检索字段
//                condition.setIncludes(ConvertTool.convert2Includes(hitClz));
//            }
            if (aggClz != null) {
                // 聚合条件
                if (outAggClz == null) {
                    condition.setAggConditions(buildAggCondition(esRequest));
                } else {
                    condition.setAggConditions(buildAssembleAggCondition(esRequest));
                }
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
     *
     * @param esRequest
     * @param searchResult
     * @return
     */
    private <A, D, S> ClientResponse<ClientResponseResult<D, S>> setResult(ESRequest< A, D, S> esRequest, SearchResult searchResult) {

//        Class<H> hitClz = esRequest.getHitClz();
        Class<A> aggClz = esRequest.getAggClz();
        Class<D> dtoClz = esRequest.getDtoClz();

        Class<S> summaryClz = esRequest.getSummaryClz();
        // 转换为返回值
        ClientResponseResult<D, S> result = new ClientResponseResult<>();
//        if (hitClz != null) {
//            List<H> billList = ConvertTool.convertHits2List(searchResult.getHits(), hitClz);
        ClientPageInfo<D> pageInfo = ClientPageUtil.page(searchResult.getTotal(), searchResult.getHits(), dtoClz);
        result.setPageInfo(pageInfo);
//        }

        if (aggClz != null) {
            parseAggInfo(aggClz,summaryClz, searchResult.getAggregations(), result, esRequest.getAggBucket());
        }
        result.setScrollId(searchResult.getScrollId());
        return new ClientResponse<>(result);
    }

    /**
     * 封装汇总结果集信息(全部数据的汇总)
     * @param aggClz
     * @param aggregations
     * @param result
     */
    private <A, D,S> void parseAggInfo(Class<A> aggClz, Class<S> summaryClz, Map<String, AggResult> aggregations, ClientResponseResult<D,S> result, AggBucketType aggBucketType) {
        List<ConvertedAggResponseBucket<A>> convertedAggResponseBuckets = ConvertTool.convertAggResult2ListByBucketKey(aggregations, aggClz, aggBucketType);
        result.setSummary(convert2Summary(convertedAggResponseBuckets, aggClz,summaryClz));
    }

    /**
     * 将返回的聚合信息key封装结果类
     * @param key agginfo key
     * @param resultClz 待封装的类
     * @param aggInfoClz 聚合请求类
     * @return
     */
    protected  <T> T parseAggKey(String key ,Class<T> resultClz,Class aggInfoClz) {
        JSONObject jsonObject = new JSONObject();

        String[] groupBy =((AggTermsBucketAnnotation) aggInfoClz.getAnnotation(AggTermsBucketAnnotation.class)).groupBy();
        String[] values = key.split(DefaultConstant.SPLIT_CHAR,-1);
        if (groupBy.length != values.length) {
            throw new ClientBussinessException(ClientErrorCode.RESULT__INSTALL_ERROR, "groupBy统计信息split错误");
        }
        for (int i = 0; i < groupBy.length; i++) {
            String fieldName = ClientStringUtil.underlineToCamelhump(groupBy[i]);//下划线转驼峰
            jsonObject.put(fieldName, values[i]);
        }
        return jsonObject.toJavaObject(resultClz);
    }
    /**
     * 封装汇总信息请求(全部数据的汇总)
     * @param esRequest
     * @return
     */
    private List<AggCondition> buildAggCondition(ESRequest esRequest) {
        if (esRequest.getAggBucket() == null) {
            esRequest.setAggBucket(AggBucketType.AGG_BUCKET_NONE);
        }
        AggCondition aggCondition = ConvertTool.convert2AggCondition(esRequest.getAggClz(), esRequest.getAggBucket());
        if (aggCondition == null) {
            log.warn("封装汇总请求失败, clazz:{}, bucketType:{}", esRequest.getAggClz().toString(), esRequest.getAggBucket());
            return null;
        }
        return Arrays.asList(aggCondition);
    }
    /**
     * 封装汇总信息结果(嵌套桶)
     * @param esRequest
     * @return
     */
    protected List<AggCondition> buildAssembleAggCondition(ESRequest esRequest) {

        List<AggCondition> aggConditions = new ArrayList<>();
        // 外层桶条件
        AggCondition aggCondition = ConvertTool.convert2AggCondition(esRequest.getOutAggClz(), AggBucketType.AGG_BUCKET_DATE_HISTOGRAM);
        if (aggCondition != null) {
            // 内层桶条件
            AggCondition innerAggCondition = ConvertTool.convert2AggCondition(esRequest.getAggClz(), AggBucketType.AGG_BUCKET_TERMS);
            if (innerAggCondition != null) {
                aggCondition.setInnerAggConditions(Collections.singletonList(innerAggCondition));
            }
            aggConditions.add(aggCondition);
        }

        return aggConditions;
    }

    /**
     * 统计信息, 转为 dto实体
     * @param
     * @return
     */
    private <A, S> S convert2Summary(List<ConvertedAggResponseBucket<A>> convertedAggResponseBuckets, Class<A> aggClz, Class<S> summaryClz) {
        if (CollectionUtils.isEmpty(convertedAggResponseBuckets)) {
            return null;
        }
        S summary = null;
        try {
            summary = summaryClz.newInstance();
        } catch (Exception e) {
            log.error("实例 {} 失败" ,summaryClz.getName());
            throw new ClientBussinessException(ClientErrorCode.ES_OPERATE_FAIL, "实例化失败");
        }

        A aggInfo = convertedAggResponseBuckets.get(0).getValue();
        BeanUtils.copyProperties(aggInfo, summary);


        return summary;
    }

    /**
     * 对list分页
     * @param pageSize
     * @param pageNo
     * @param resultList
     * @param <T>
     * @return
     */
    protected  <T> List<T> subPageList(int pageSize,int pageNo, List<T> resultList) {

        checkPageInfo(pageSize, pageNo, resultList.size());
        int start = (pageNo - 1) * pageSize;
        int end = pageNo * pageSize;
        if (end > resultList.size()) end = resultList.size();

        return resultList.subList(start, end);
    }

    protected void checkPageInfo(int pageSize, int pageNo,long total) {
        if (total == 0 && pageNo == 1) {
            return;
        }
        if (pageSize * (pageNo - (total % pageSize == 0 ? 0 : 1)) > total) {
            throw new ClientBussinessException(ClientErrorCode.RESULT_OVERFLOW, "分页信息有误");
        }
    }





}
