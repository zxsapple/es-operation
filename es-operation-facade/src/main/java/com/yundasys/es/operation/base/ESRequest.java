package com.yundasys.es.operation.base;

import com.yundasys.es.operation.model.IndexInfo;
import com.yundasys.es.operation.model.request.BaseAggInfo;
import com.yundasys.es.operation.model.request.BaseDateInfo;
import lombok.Data;

/**
 * @author zhengxiaosu
 * @desc 请求es公共方法请求
 * @date 2018/7/6 9:20
 */
@Data
public class ESRequest<D> {

    /**
     * 请求参数参数
     */
    ClientRequest<?> request;
    /**
     * 查询条件 支持多条件查询
     */
    Object[] criteria;

    /**
     * groupby 汇总 可嵌套到时间下面一层
     */
    BaseAggInfo aggInfo;

    /**
     * 时间汇总
     */
    BaseDateInfo dateInfo;
    /**
     * es 的index索引 信息
     */
    IndexInfo indexInfo;
    /**
     * 前台返回的实体
     */
    Class<D> dtoClz;


    public void setCriterias(Object... criteria) {
        this.criteria = criteria;
    }
}
