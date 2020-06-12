package com.yundasys.es.operation.util;

import com.yundasys.es.operation.annotation.agg.AggCalField;
import com.yundasys.es.operation.annotation.agg.AggDateField;
import com.yundasys.es.operation.annotation.agg.AggGroupByField;
import com.yundasys.es.operation.constant.ESErrorCode;
import com.yundasys.es.operation.exception.ESOperationException;
import com.yundasys.es.operation.model.agg.AggCondition;
import com.yundasys.es.operation.model.agg.AggDateCondition;
import com.yundasys.es.operation.model.agg.CalField;
import com.yundasys.es.operation.model.request.BaseAggInfo;
import com.yundasys.es.operation.model.request.BaseDateInfo;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.springframework.beans.BeanUtils;

import java.lang.reflect.Field;
import java.util.Date;

/**
 * @author zhengxiaosu
 * @desc  客户端aggInfo转为 es组件需要的aggCondition
 * @date 2020/6/8 13:53
 */
public class ConvertAggTool {

    public static AggCondition info2AggCondition(BaseAggInfo aggInfo) {

        if (aggInfo == null) {
            return null;
        }
        AggCondition aggCondition = new AggCondition();
        BeanUtils.copyProperties(aggInfo, aggCondition);

        Class<? extends BaseAggInfo> clz = aggInfo.getClass();

        Field[] groupBys = FieldUtils.getFieldsWithAnnotation(clz, AggGroupByField.class);

        //设置groupby字段
        String[] groupByFields = new String[groupBys.length];
        for (int i = 0; i < groupBys.length; i++) {
            groupByFields[i] = groupBys[i].getName();
        }
        aggCondition.setGroupByFields(groupByFields);

        CalField[] calFields = getCalFields(clz);
        aggCondition.setCalFields(calFields);
        return aggCondition;

    }

    /**
     * 汇总class AggCalField 类转换为 汇总计算字段
     */
    private static CalField[] getCalFields(Class clz) {
        Field[] cals = FieldUtils.getFieldsWithAnnotation(clz, AggCalField.class);

        if (BaseAggInfo.class.isAssignableFrom(clz) && ArrayUtils.isEmpty(cals)) {
            throw new ESOperationException(ESErrorCode.PARAMETER_INCORRECT, "baseAggInfo里 AggCalField 注解的字段至少存在一个");
        }

        CalField[] calFields = new CalField[cals.length];
        for (int i = 0; i < cals.length; i++) {
            CalField calField = new CalField();
            Field cal = cals[i];
            AggCalField annotation = cal.getAnnotation(AggCalField.class);
            calField.setField(cal.getName());
            calField.setCriterionType(annotation.aggCriterionType());
            calFields[i] = calField;
        }
        return calFields;
    }

    public static AggDateCondition info2AggDateCondition(BaseDateInfo dateInfo, BaseAggInfo aggInfo) {

        AggDateCondition aggDateCondition = new AggDateCondition();
        aggDateCondition.setAggCondition(info2AggCondition(aggInfo));

        Class<? extends BaseDateInfo> clz = dateInfo.getClass();

        Field[] fields = FieldUtils.getFieldsWithAnnotation(clz, AggDateField.class);
        if (fields.length != 1) {
            throw new ESOperationException(ESErrorCode.PARAMETER_INCORRECT, "AggDateField 注解的字段必须有且只有一个");
        }
        if (!Date.class.equals(fields[0].getType())) {
            throw new ESOperationException(ESErrorCode.PARAMETER_INCORRECT, "AggDateField 必须修饰java.util.Date类型");
        }
        CalField[] calFields = getCalFields(clz);
        aggDateCondition.setCalFields(calFields);
        aggDateCondition.setInterval(dateInfo.getInterval());
        aggDateCondition.setSortType(dateInfo.getSortType());
        aggDateCondition.setSortField(dateInfo.getSortField());
        aggDateCondition.setDateField(fields[0].getName());
        return aggDateCondition;
    }


}
