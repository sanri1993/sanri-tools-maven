package com.sanri.app.jdbc.codegenerate;

import org.apache.commons.lang3.StringUtils;

import java.util.Map;

/**
 * 扩展的驼峰命名,对于某某id ,某某no 更改原来的规则
 */
public class RenamePolicyaBExtend extends RenamePolicyDefault {

    public RenamePolicyaBExtend(Map<String, Map<String, String>> typeMirror) {
        super(typeMirror);
    }

    @Override
    public String mapperPropertyName(String columnName) {
        String propertyName = super.mapperPropertyName(columnName);
        if((propertyName.endsWith("id") || propertyName.endsWith("no")) && propertyName.length() != 2 && !"uuid".equals(propertyName)){
            String firstPart = propertyName.substring(0,propertyName.length() - 2);
            String lastpart = propertyName.substring(firstPart.length());
            return firstPart+StringUtils.capitalize(lastpart);
        }
        return propertyName;
    }
}
