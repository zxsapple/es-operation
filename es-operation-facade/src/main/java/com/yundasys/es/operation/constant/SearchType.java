package com.yundasys.es.operation.constant;

import java.io.Serializable;

public enum SearchType implements Serializable {

    ///--   Match All Query
    ST_MATCHALL("match_all", "match_all", "match_all"),


    ///--   Full text queries
    ST_MATCH("match", "match", "match"),
    ST_MULTI_MATCH("multi_match", "multi_match", "multi_match"),
    ST_COMMON_TERMS("common_terms", "common_terms", "common_terms"),
    ST_QUERY_STRING("query_string", "query_string", "query_string"),
    ST_SIMPLE_QUERY_STRING("simple_query_string", "simple_query_string", "simple_query_string"),


    ///--   Term level queries
    ST_TERM("term", "term", "词匹配"),
    ST_TERMS("terms", "terms", "多词匹配"),
    ST_RANGE("range", "range", "范围"),
    ST_EXISTS("exists", "exists", "exists"),
    ST_MISSING("missing", "missing", "missing"),
    ST_PREFIX("prefix", "prefix", "前缀"),
    ST_WILDCARD("wildcard", "wildcard", "通配符"),
    ST_REGEXP("regexp", "regexp", "正则"),
    ST_FUZZY("fuzzy", "fuzzy", "模糊"),
    //ST_TYPE("type", "type", "type"),
    ST_IDS("ids", "ids", "ids"),


    ///--    Compound queries
    ST_CONSTANT_SCORE("constant_score", "constant_score", "constant_score"),
    ST_BOOL("bool", "bool", "bool"),  /* Synonyms: and, or, not */
    ST_DIS_MAX("dis_max", "dis_max", "dis_max"),
    ST_FUNCTION_SCORE("function_score", "function_score", "function_score"),
    ST_BOOSTING("boosting", "boosting", "boosting"),
    //ST_INDICES("indices", "indices", "indices"),
    ST_LIMIT("limit", "limit", "limit"),


    ///--   Joining queries
    ST_NESTED("nested", "nested", "nested"),
    ST_HAS_CHILD("has_child", "has_child", "has_child"),
    ST_HAS_PARENT("has_parent", "has_parent", "has_parent"),


    ///--   Geo queries


    ///--   Specialized queries
    ST_MORE_LIKE_THIS("more_like_this", "more_like_this", "more_like_this"),
    ST_TEMPLATE("template", "template", "template"),
    ST_SCRIPT("script", "script", "script"),


    ///--   Span queries
    ST_SPAN_TERM("span_term", "span_term", "span_term"),
    ST_SPAN_MULTI("span_multi", "span_multi", "span_multi"),
    ST_SPAN_FIRST("span_first", "span_first", "span_first"),
    ST_SPAN_NEAR("span_near", "span_near", "span_near"),
    ST_SPAN_OR("span_or", "span_or", "span_or"),
    ST_SPAN_NOT("span_not", "span_not", "span_not"),
    ST_SPAN_CONTAINING("span_containing", "span_containing", "span_containing"),
    ST_SPAN_WITHIN("span_within", "span_within", "span_within"),

    ///--   user-defined
    ST_FIELD_EQUALS_OR_NOT("field_equals_or_not", "field_equals_or_not", "域相等/不相等"),
    ST_FIELD_GREATER_THAN_OR_NOT("field_greater_than_or_not", "field_greater_than_or_not", "域比较大于/小于等于"),
    ST_FIELD_GREATER_THAN_EQUALS_OR_NOT("field_greater_than_equals_or_not", "field_greater_than_equals_or_not", "域比较大于等于/小于"),
    
    END("", "", "");

    public final String name;
    public final String value;
    public final String intro;

    SearchType(String name, String value, String intro) {
        this.name = name;
        this.value = value;
        this.intro = intro;
    }
}
