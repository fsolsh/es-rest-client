package com.fsolsh.es.common;

import java.util.List;

/**
 * 分页组件
 *
 * @param <T>
 */
public class ESPage<T> {

    public static final int MAX_COUNT_PER_PAGE = 1000, MAX_COUNT_PER_SEARCH = 10000;

    // 当前页码
    private int currPage = 1;
    // 每页数据
    private int pageSize = 10;
    // 总数据量
    private int rowCount = 0;
    // 总页码数
    private int pageCount = 0;
    // 本页数据
    private List<T> pageData = null;

    public ESPage() {
    }

    public ESPage(int currPage, int pageSize) {
        this(currPage, pageSize, 0, null);
    }

    public ESPage(int currPage, int pageSize, int rowCount, List<T> pageData) {

        if (currPage >= 0) {
            this.setCurrPage(currPage);
        }

        if (pageSize > 0 && pageSize <= MAX_COUNT_PER_SEARCH) {
            this.setPageSize(pageSize);
        }

        rebuild(rowCount, pageData);
    }

    public ESPage<T> rebuild(int rowCount, List<T> pageData) {
        if (rowCount > 0) {
            this.setRowCount(rowCount);
        }
        this.setPageData(pageData);
        return this;
    }

    public int getCurrPage() {
        return currPage;
    }

    public void setCurrPage(int currPage) {
        this.currPage = currPage;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    public int getRowCount() {
        return rowCount;
    }

    public void setRowCount(int rowCount) {
        this.rowCount = rowCount;
        this.pageCount = (int) Math.ceil(this.rowCount / (this.pageSize * 1.0));
    }

    public int getPageCount() {
        return pageCount;
    }

    public void setPageCount(int pageCount) {
        this.pageCount = pageCount;
    }

    public List getPageData() {
        return pageData;
    }

    public void setPageData(List pageData) {
        this.pageData = pageData;
    }
}
