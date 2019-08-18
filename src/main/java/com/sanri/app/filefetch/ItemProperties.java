package com.sanri.app.filefetch;

import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import sanri.utils.NumberUtil;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

public class ItemProperties {
	private String item;
	private Map<String,String> properties = new HashMap<String, String>();
	
	public ItemProperties(String item) {
		super();
		this.item = item;
	}
	public String getItem() {
		return item;
	}
	
	public String getString(String key){
		String value = properties.get(key);
		return ObjectUtils.toString(value).trim();
	}
	
	public int getInt(String key){
		String value = properties.get(key);
		return NumberUtil.toInt(value,Integer.MIN_VALUE);
	}
	
	public long getLong(String key){
		String value = properties.get(key);
		return NumberUtil.toLong(value,Long.MIN_VALUE);
	}
	
	public List<String> getList(String key){
		String value = properties.get(key);
		if(StringUtils.isBlank(value)){
			return new ArrayList<String>();
		}
		String[] split = value.split(",");
		return Arrays.asList(split);
	}
	
	public JSONObject getJSON(String key){
		String value = properties.get(key);
		return JSONObject.parseObject(value);
	}
	
	public static ItemProperties loadProperties(InputStream inputStream) {
		throw new RuntimeException("未实现");
	}
	
	public static Map<String,ItemProperties> loadProperties(String classPath) throws IOException{
		Map<String,ItemProperties> itemPropertiesMap = new HashMap<String, ItemProperties>();
		
		//使用 resourceBundle 不能实时加载
//		ResourceBundle resource = ResourceBundle.getBundle(classPath);
		Properties properties = new Properties();
		properties.load(ItemProperties.class.getResourceAsStream(classPath+".properties"));
		Set<Object> keySet = properties.keySet();
		Iterator<Object> iterator = keySet.iterator();
		while(iterator.hasNext()){
			String fullKey = ObjectUtils.toString(iterator.next());
			if(StringUtils.isBlank(fullKey)){
				continue;
			}
			int indexOffirstPoint = fullKey.indexOf(".");
			String item = fullKey.substring(0,indexOffirstPoint);
			String key = fullKey.substring(indexOffirstPoint+1,fullKey.length());
			String value = properties.getProperty(fullKey);
			
			ItemProperties itemProperties = itemPropertiesMap.get(item);
			if(itemProperties == null){
				itemProperties = new ItemProperties(item);
				itemPropertiesMap.put(item, itemProperties);
			}
			itemProperties.properties.put(key, value);
			
		}
		return itemPropertiesMap;
	}
	
	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
	}
//	public Map<String, String> getProperties() {
//		return properties;
//	}
}
