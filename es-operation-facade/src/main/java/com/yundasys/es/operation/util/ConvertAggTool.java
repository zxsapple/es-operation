package com.yundasys.es.operation.util;

import com.yundasys.es.operation.annotation.agg.AggCalField;
import com.yundasys.es.operation.annotation.agg.AggDateField;
import com.yundasys.es.operation.annotation.agg.AggGroupByField;
import com.yundasys.es.operation.model.agg.AggCondition;
import com.yundasys.es.operation.model.agg.AggDateCondition;
import com.yundasys.es.operation.model.request.BaseAggInfo;
import com.yundasys.es.operation.model.request.BaseDateInfo;
import com.yundasys.es.operation.model.agg.CalField;
import org.springframework.beans.BeanUtils;
import org.apache.commons.lang3.reflect.FieldUtils;

import java.lang.reflect.Field;

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

        Field[] cals = FieldUtils.getFieldsWithAnnotation(clz, AggCalField.class);

        CalField[] calFields = new CalField[cals.length];
        for (int i = 0; i < cals.length; i++) {
            CalField calField = new CalField();
            Field cal = cals[i];
            AggCalField annotation = cal.getAnnotation(AggCalField.class);
            calField.setField(cal.getName());
            calField.setCriterionType(annotation.aggCriterionType());
            calFields[i] = calField;
        }
        aggCondition.setCalFields(calFields);
        return aggCondition;

    }

    public static AggDateCondition info2AggDateCondition(BaseDateInfo dateInfo, BaseAggInfo aggInfo) {

        AggDateCondition aggDateCondition = new AggDateCondition();
        aggDateCondition.setAggCondition(info2AggCondition(aggInfo));

        Class<? extends BaseDateInfo> clz = dateInfo.getClass();

        Field[] fields = FieldUtils.getFieldsWithAnnotation(clz, AggDateField.class);
        if (fields.length != 1) {
            throw new RuntimeException("AggDateField 注解的字段必须有且只有一个");
        }
        Field[] cals = FieldUtils.getFieldsWithAnnotation(clz, AggCalField.class);

        CalField[] calFields = new CalField[cals.length];
        for (int i = 0; i < cals.length; i++) {
            CalField calField = new CalField();
            Field cal = cals[i];
            AggCalField annotation = cal.getAnnotation(AggCalField.class);
            calField.setField(cal.getName());
            calField.setCriterionType(annotation.aggCriterionType());
            calFields[i] = calField;
        }
        aggDateCondition.setCalFields(calFields);
        aggDateCondition.setInterval(dateInfo.getInterval());
        aggDateCondition.setSortType(dateInfo.getSortType());
        aggDateCondition.setSortField(dateInfo.getSortField());
        aggDateCondition.setDateField(fields[0].getName());
        return aggDateCondition;
    }


}
