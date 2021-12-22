package com.fsolsh.es.common;

import lombok.Data;
import org.elasticsearch.search.sort.SortOrder;

/**
 * ES排序组件
 */
@Data
public class ESSort {

    //排序字段
    private String fieldName;
    //排序方式
    private SortOrder sortOrder = SortOrder.DESC;

}
