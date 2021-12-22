package com.fsolsh.es.common;

import lombok.Data;

import static com.fsolsh.es.common.ESQuery.QueryType.SHOULD;

/**
 * ES查询组件
 */
@Data
public class ESQuery {

    //查询类型
    private QueryType queryType = SHOULD;
    //查询字段
    private String fieldName;
    //查询参数
    private Object queryParam;

    /**
     * 查询条件类型
     */
    public enum QueryType {
        MUST, SHOULD, MUST_NOT
    }
}
