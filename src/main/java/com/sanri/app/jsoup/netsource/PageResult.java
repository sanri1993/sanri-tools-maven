package com.sanri.app.jsoup.netsource;

import org.apache.commons.lang3.builder.ToStringBuilder;

import java.util.ArrayList;
import java.util.List;

public class PageResult<T> {
    private int page = 0 ;
    private List<T> data  = new ArrayList<T>();
    private boolean hasNext;

    public PageResult() {
    }

    public PageResult(int page) {
        this.page = page;
    }

    public PageResult(int page, List<T> data) {
        this.page = page;
        this.data = data;
    }

    public int getPage() {
        return page;
    }

    public List<T> getData() {
        return data;
    }

    public void setData(List<T> data) {
        this.data = data;
    }

    public void setHasNext(boolean hasNext) {
        this.hasNext = hasNext;
    }

    public boolean isHasNext() {
        return hasNext;
    }

    @Override
    public String toString() {
        if(data == null)return "[]";
        return data.toString();
    }
}
