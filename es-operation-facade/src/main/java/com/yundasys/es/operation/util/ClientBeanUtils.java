package com.yundasys.es.operation.util;

import com.alibaba.fastjson.JSON;
import com.yundasys.es.operation.annotation.ElectiveField;
import com.yundasys.es.operation.annotation.RangeField;
import com.yundasys.es.operation.constant.ClientErrorCode;
import com.yundasys.es.operation.exception.ClientBussinessException;
import com.yundasys.es.operation.model.Range;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.util.StringUtils;

import java.lang.reflect.Field;
import java.util.Date;
@Slf4j
public class ClientBeanUtils {

	
	/**
     * bean 属性复制, 解决自定义Annotation的复制
     * @param source
     * @param target
     */
    public static void copyProperties(Object source, Object... targets) {
		for (Object target : targets) {
			try {
				BeanUtils.copyProperties(source,target);
			} catch (Exception e1) {
				log.error("bean转换错误 {}", JSON.toJSONString(source));
				throw new ClientBussinessException(ClientErrorCode.PARAMETER_INCORRECT, "参数有误");
			}

			Class<?> clazz = target.getClass();
			Field[] fields = clazz.getDeclaredFields();
			for (Field field : fields) {
				// 处理RangeFeild注解
				dealRangeField(source, target, field);
				// 处理ElectiveField注解
				dealElectiveField(source, target, field);
			}
		}
    }
    
    private static void dealRangeField(Object source, Object target, Field field) {
    	// 获取RangeFeild注解
    	RangeField rangeFeild = field.getAnnotation(RangeField.class);
    	if (null == rangeFeild) {
            return;
        }
        
        Range rangeData = new Range();
        String from = rangeFeild.from();
        String to = rangeFeild.to();

        if (StringUtils.isEmpty(from) && StringUtils.isEmpty(to)) {
        	return;
        }
        
        try {
        	// 获取From源数据属性

            Field fromField = getField(source.getClass(), from);
            if (fromField != null) {
                fromField.setAccessible(Boolean.TRUE);
                //设置为date类型, 供中间件 序列化为年月日
                if (fromField.get(source) != null) {
					if (Date.class.equals(fromField.getType())) {
						rangeData.setFrom(fromField.get(source), rangeFeild.includeFrom(), Date.class);
					} else {
						rangeData.setFrom(fromField.get(source), rangeFeild.includeFrom());
					}

                }
            }

            
            // 获取To源数据属性
            Field toField = getField(source.getClass(), to);
            if (toField != null) {
                toField.setAccessible(Boolean.TRUE);
                if (toField.get(source) != null) {
                    rangeData.setTo(toField.get(source), rangeFeild.includeTo());
                    //设置为date类型, 供中间件 序列化为年月日
                    if (Date.class.equals(toField.getType())) {
                        rangeData.setTo(toField.get(source), rangeFeild.includeTo(), Date.class);
                    } else {
                        rangeData.setTo(toField.get(source), rangeFeild.includeTo());
                    }
                }
            }

            
            // 无效range时不做处理
            if (rangeData.getFrom() == null && rangeData.getTo() == null) {
            	return;
            }
            
            field.setAccessible(Boolean.TRUE);
            field.set(target, rangeData);
		} catch (IllegalArgumentException | IllegalAccessException e) {
			e.printStackTrace();
            log.warn("封装 dealRangeField source:{}field:{}",field,source);
            throw new ClientBussinessException(ClientErrorCode.SET_PARAMETER_ERROR, "封装请求es参数错误 dealRangeField");
        }
    }
    
    private static void dealElectiveField(Object source, Object target, Field field) {
    	// 获取ElectiveField注解
    	ElectiveField electiveField = field.getAnnotation(ElectiveField.class);
    	if (null == electiveField) {
            return;
        }

        try {
        	// 获取源数据属性
            Field sourceField = getField(source.getClass(), electiveField.property());
            if (sourceField == null) {
            	return;
            }
			sourceField.setAccessible(true);
            // 源数据属性值
            Object sourceValue = sourceField.get(source);
            if (sourceValue == null) {
            	return;
            }
            
            if (sourceValue.toString().equals(electiveField.validateValue())) {
            	// 只在有效值时处理
            	field.setAccessible(Boolean.TRUE);
            	switch(field.getType().getSimpleName()) {
            	case "Integer":
            		field.set(target, Integer.valueOf(electiveField.value()));
            		break;
            	case "Integer[]":
            		Integer[] intValues = new Integer[electiveField.values().length];
            		for (int i = 0; i < electiveField.values().length; i++) {
            			intValues[i] = Integer.valueOf(electiveField.values()[i]);
            		}
            		field.set(target, intValues);
            		break;
            	case "Long":
            		field.set(target, Long.valueOf(electiveField.value()));
            		break;
            	case "Long[]":
            		Long[] longValues = new Long[electiveField.values().length];
            		for (int i = 0; i < electiveField.values().length; i++) {
            			longValues[i] = Long.valueOf(electiveField.values()[i]);
            		}
            		field.set(target, longValues);
            		break;
            	case "String":
            		field.set(target, electiveField.value());
            		break;
            	case "String[]":
            		field.set(target, electiveField.values());
            		break;
            	case "Boolean":
            		field.set(target, Boolean.valueOf(electiveField.value()));
            		break;
        		default:
        			log.error("处理 ElectiveField error, unsupported type, field:{}", field.getName());
        			throw new ClientBussinessException(ClientErrorCode.PARAMETER_INCORRECT, "处理 ElectiveField error, unsupported type");
            	}
            }
		} catch (IllegalArgumentException | IllegalAccessException e) {
			e.printStackTrace();
			log.warn("封装 dealElectiveField source:{}field:{}",field,source);
			throw new ClientBussinessException(ClientErrorCode.SET_PARAMETER_ERROR, "封装请求es参数错误 dealElectiveField");
		}
    }
    
    private static Field getField(Class<?> clz, String name) {
    	Field field = null;
    	// 子类获取不到时尝试获取父类
        for(; clz != Object.class ; clz = clz.getSuperclass()) {
        	
			try {
				field = clz.getDeclaredField(name);
			} catch (NoSuchFieldException | SecurityException e) {
				continue;
			}
        	if (field != null) {
        		return field;
        	}
        }
        
        return null;
    }
}
