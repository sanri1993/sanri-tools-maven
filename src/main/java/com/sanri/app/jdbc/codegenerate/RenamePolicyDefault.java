package com.sanri.app.jdbc.codegenerate;

import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;

/**
 * 
 * 作者:sanri</br> 
 * 时间:2016-9-26下午2:04:07</br> 
 * 功能:默认命名策略<br/>
 */
public class RenamePolicyDefault implements RenamePolicy {
	private Map<String,Map<String,String>> typeMirror = new HashMap<>();

	public RenamePolicyDefault(Map<String, Map<String, String>> typeMirror) {
		this.typeMirror = typeMirror;
	}

	@Override
	public String mapperClassName(String tableName) {
		if (!StringUtils.isBlank(tableName)) {
			tableName = tableName.toLowerCase();
			String[] part = tableName.split("_");
			String className = "";
			if(part.length > 1){		//去掉前缀
				for (int i = 1; i < part.length; i++) {
					className += StringUtils.capitalize(part[i]);
				}
			}else{
				className = part[0];
			}

			return className;
		}
		return tableName;
	}

	@Override
	public String mapperPropertyName(String columnName) {
		if (!StringUtils.isBlank(columnName)) {
			columnName = columnName.toLowerCase();
			//如果列无下线线,则直接返回原来的列名
			if(!columnName.contains("_")){
				return columnName;
			}

			//如果有下划线,则对每一部分转驼峰
			String[] part = columnName.split("_");
			String newColumnName = part[0];
			for (int i = 1; i < part.length; i++) {
				newColumnName += StringUtils.capitalize(part[i]);
			}
			return newColumnName;
		}
		return columnName;
	}

	@Override
	public String mapperPropertyType(String columnType,String dbType) {
		Map<String, String> typeMirrorMap = typeMirror.get(dbType);
		if(typeMirrorMap == null)return "";
		Matcher matcher = RenamePolicyMybatisExtend.pattern.matcher(columnType);
		String typeName = "";
		if(matcher.find()){
			typeName = matcher.group(1);
		}
		return typeMirrorMap.get(typeName);
	}
}