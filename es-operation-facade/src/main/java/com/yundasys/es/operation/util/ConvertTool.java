package com.yundasys.es.operation.util;

import com.yundasys.es.operation.annotation.ExtendOredCriteria;
import com.yundasys.es.operation.annotation.FieldOption;
import com.yundasys.es.operation.constant.ClientErrorCode;
import com.yundasys.es.operation.constant.SearchType;
import com.yundasys.es.operation.exception.ClientBussinessException;
import com.yundasys.es.operation.model.request.CompoundCriteria;
import com.yundasys.es.operation.model.request.Criteria;
import com.yundasys.es.operation.model.request.SearchField;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author zhengxiaosu
 * @desc  客户端条件转为 es组件需要的condition
 * @date 2020/6/8 13:53
 */
@Slf4j
public class ConvertTool {

    /**
     * 将条件对象转换成组合条件
     * 对象List，默认：对象之间使用or连接
     * 使用ExtendOredCriteria注解的对象，对象之间使用and连接
     * 扫描条件对象中@FieldOption注解，将该注解的属性转化为实际检索条件
     */
    public static CompoundCriteria convert2CompoundCriteria(List<Object> targets) {
    	if (targets == null || targets.size() == 0) {
    		return null;
    	}
    	
        CompoundCriteria compoundCriteria = new CompoundCriteria();
        for (Object target : targets) {
        	ExtendOredCriteria extendOredCriteria = target.getClass().getAnnotation(ExtendOredCriteria.class);
        	if (extendOredCriteria != null) {
        		Criteria criteria = compoundCriteria.and();
            	buildCriteria(criteria, target);
        	} else {
        		Criteria criteria = compoundCriteria.or();
            	buildCriteria(criteria, target);
        	}
        }
    	return compoundCriteria;
    }
    
    private static void buildCriteria(Criteria criteria, Object target) {
    	Class<?> clazz = target.getClass();
        Field[] fields = getFields(clazz);
    	for (Field field : fields) {
        	field.setAccessible(Boolean.TRUE);
        	FieldOption fieldOption = field.getAnnotation(FieldOption.class);
            if (null == fieldOption) {
            	// 跳过无@FieldOption注解属性
                continue;
            }

            Object fieldObj = null;
			try {
				fieldObj = field.get(target);
			} catch (IllegalArgumentException | IllegalAccessException e) {
                log.error("转换field 有误 ", e);
				throw new ClientBussinessException(ClientErrorCode.PARAMETER_INCORRECT,"Criteria转换错误");
			}
            
            if (fieldObj == null) {
            	// 跳过值为null的属性
            	continue;
            }

            SearchField searchField = new SearchField();
            searchField.setSearchLogic(fieldOption.searchLogic());
            searchField.setSearchType(fieldOption.searchType());
            searchField.setValue(fieldObj);
            criteria.addCriterion(searchField);
            if (fieldOption.searchType().equals(SearchType.ST_MULTI_MATCH)
            		|| fieldOption.searchType().equals(SearchType.ST_FIELD_EQUALS_OR_NOT)
            		|| fieldOption.searchType().equals(SearchType.ST_FIELD_GREATER_THAN_OR_NOT)
            		|| fieldOption.searchType().equals(SearchType.ST_FIELD_GREATER_THAN_EQUALS_OR_NOT)) {
            	// 使用扩展检索域--多域操作
                searchField.setNames(fieldOption.names());
            } else {
            	// 单域操作
                searchField.setName(StringUtils.isNotEmpty(fieldOption.name()) ? fieldOption.name() : field.getName());//空时使用字段名称

            }

        }
    }

    /**
     * 获取类的所有属性-包括从父类继承的属性
     * @param clz
     * @return
     */
    private static Field[] getFields(Class<?> clz) {
    	List<Field> fieldList = new ArrayList<>();
        // 子类获取不到时尝试获取父类
        for(; clz != Object.class ; clz = clz.getSuperclass()) {
        	Field[] fields = clz.getDeclaredFields();
        	if (fields != null && fields.length > 0) {
        		fieldList.addAll(Arrays.asList(fields));
        	}
        }

        Field[] result = new Field[fieldList.size()];
        fieldList.toArray(result);
        return result;
    }

}
