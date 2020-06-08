package com.yundasys.es.operation.base;

import com.yundasys.es.operation.constant.AggBucketType;
import com.yundasys.es.operation.model.IndexInfo;
import lombok.Data;

/**
 * @author zhengxiaosu
 * @desc 请求es公共方法请求
 * @date 2018/7/6 9:20
 */
@Data
public class ESRequest< A, D, S> {

    /**
     * 请求参数参数
     */
    ClientRequest<?> request;
    /**
     * 查询条件 支持多条件查询
     */
    Object[] criteria;
//    /**
//     * es查询对应实体
//     */
//    Class<H> hitClz;
    /**
     * es(外部)统计对应实体
     */
    Class outAggClz;
    /**
     * es(内部)统计对应实体
     */
    Class<A> aggClz;
    /**
     * es(外部)统计方式
     */
    AggBucketType outAggBucket;
    /**
     * es(内部)统计方式
     */
    AggBucketType aggBucket;
    /**
     * es 的index索引 信息
     */
    IndexInfo indexInfo;
    /**
     * 前台返回的实体
     */
    Class<D> dtoClz;
    /**
     * 前台汇总实体
     */
    Class<S> summaryClz;

    public void setCriterias(Object... criteria) {
        this.criteria = criteria;
    }
}
