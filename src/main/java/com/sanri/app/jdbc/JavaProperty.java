package com.sanri.app.jdbc;

import org.apache.commons.lang3.StringUtils;

/**
 * 数据映射到 java 属性列信息，用于代码生成
 */
public class JavaProperty {
    private String name;
    private String type;
    private String jdbcType;
    private String columnName;
    private String capitalizeName;
    private String comments;
    private boolean primaryKey;

    public JavaProperty() {
    }

    public JavaProperty(String name, String type, String columnName) {
        this.name = name;
        this.capitalizeName = StringUtils.capitalize(name);
        this.type = type;
        this.columnName = columnName;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
        this.capitalizeName = StringUtils.capitalize(name);
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getColumnName() {
        return columnName;
    }

    public void setColumnName(String columnName) {
        this.columnName = columnName;
    }

    public String getCapitalizeName() {
        return capitalizeName;
    }

    public void setCapitalizeName(String capitalizeName) {
        this.capitalizeName = capitalizeName;
    }

    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }

    public String getJdbcType() {
        return jdbcType;
    }

    public void setJdbcType(String jdbcType) {
        this.jdbcType = jdbcType;
    }

    public boolean isPrimaryKey() {
        return primaryKey;
    }

    public void setPrimaryKey(boolean primaryKey) {
        this.primaryKey = primaryKey;
    }
}
