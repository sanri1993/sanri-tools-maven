package com.sanri.app.jdbc;

import org.apache.commons.lang.StringUtils;

public class JavaProperty {
    private String name;
    private String type;
    private String columnName;
    private String capitalizeName;

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
}
