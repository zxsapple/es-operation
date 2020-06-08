package com.yundasys.es.operation.model;

import java.io.Serializable;

public class Range implements Serializable {
    private static final long serialVersionUID = 89050826020349158L;
    
    private Item from;
    private Item to;
    
    public Item getFrom() {
        return from;
    }

    public void setFrom(Object value, boolean include, Class<?> clz) {
        this.from = new Item(value, include, clz);
    }
    
    public void setFrom(Object value, Class<?> clz) {
    	setFrom(value, true, clz);
    }
    
    public void setFrom(Object value, boolean include) {
    	setFrom(value, include, null);
    }
    
    public void setFrom(Object value) {
    	setFrom(value, true, null);
    }

    public Item getTo() {
        return to;
    }

    public void setTo(Object value, boolean include, Class<?> clz) {
        this.to = new Item(value, include, clz);
    }
    
    public void setTo(Object value, Class<?> clz) {
    	setTo(value, true, clz);
    }
    
    public void setTo(Object value, boolean include) {
    	setTo(value, include, null);
    }
    
    public void setTo(Object value) {
    	setTo(value, true, null);
    }
    
    public void setFrom(Item from) {
        this.from = from;
    }

    public void setTo(Item to) {
        this.to = to;
    }

    public class Item implements Serializable {
        private static final long serialVersionUID = -7592931100446416513L;
        
        private Object value;
        private boolean include;
        private Class<?> clz;
        
        public Item () {}
        
        Item (Object value, boolean include) {
            this.value = value;
            this.include = include;
        }
        
        Item (Object value, boolean include, Class<?> clz) {
            this.value = value;
            this.include = include;
            this.clz = clz;
        }
        
        public void setValue(Object value) {
            this.value = value;
        }

        public void setInclude(boolean include) {
            this.include = include;
        }
        
        public Object getValue() {
            return value;
        }
        
        public boolean isInclude() {
            return include;
        }

		public Class<?> getClz() {
			return clz;
		}

		public void setClz(Class<?> clz) {
			this.clz = clz;
		}
    }
}
