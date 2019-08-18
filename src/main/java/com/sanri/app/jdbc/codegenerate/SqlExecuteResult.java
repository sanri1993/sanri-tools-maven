package com.sanri.app.jdbc.codegenerate;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.util.ArrayList;
import java.util.List;

public class SqlExecuteResult {
    private List<String> header = new ArrayList<String>();
    private List<String> columnType = new ArrayList<String>();
    private List<List<Object>> rows = new ArrayList<List<Object>>();

    public void addColumnType(String columnType){
        this.columnType.add(columnType);
    }

    /**
     * 添加头部
     * @param header
     */
    public void addHeader(String header){
        this.header.add(header);
    }

    /**
     * 添加一行数据
     * @param row
     */
    public void addRow(List<Object> row) {
        rows.add(row);
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this,ToStringStyle.SHORT_PREFIX_STYLE);
    }

    public List<String> getHeader() {
        return header;
    }

    public List<List<Object>> getRows() {
        return rows;
    }

    public List<String> getColumnType() {
        return columnType;
    }
}
