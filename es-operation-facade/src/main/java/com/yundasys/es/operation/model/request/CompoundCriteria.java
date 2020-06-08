package com.yundasys.es.operation.model.request;

import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
public class CompoundCriteria implements Serializable {
    private static final long serialVersionUID = 6712375055890599609L;
    
    private List<Criteria> oredCriteria= new ArrayList<>();; // or连接所有and
    private List<Criteria> extendOredCriteria=new ArrayList<>(); // and连接所有or

    public List<Criteria> getOredCriteria() {
        return oredCriteria;
    }
    
    public List<Criteria> getExtendOredCriteria() {
        return extendOredCriteria;
    }

    public void or(Criteria criteria) {
        oredCriteria.add(criteria);
    }
    
    public void and(Criteria criteria) {
    	extendOredCriteria.add(criteria);
    }

    public Criteria or() {
        Criteria criteria = createCriteriaInternal();
        oredCriteria.add(criteria);
        return criteria;
    }
    
    public Criteria and() {
        Criteria criteria = createCriteriaInternal();
        extendOredCriteria.add(criteria);
        return criteria;
    }

    public Criteria createCriteria() {
        Criteria criteria = createCriteriaInternal();
        if (oredCriteria.size() == 0) {
            oredCriteria.add(criteria);
        }
        return criteria;
    }
    
    public Criteria createExtendCriteria() {
        Criteria criteria = createCriteriaInternal();
        if (extendOredCriteria.size() == 0) {
        	extendOredCriteria.add(criteria);
        }
        return criteria;
    }

    protected Criteria createCriteriaInternal() {
        Criteria criteria = new Criteria();
        return criteria;
    }

    public void clear() {
        oredCriteria.clear();
    }
    
    public void clearExtend() {
    	extendOredCriteria.clear();
    }


}