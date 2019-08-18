package com.sanri.app.xsd.node;

import org.apache.commons.lang3.StringUtils;
import org.dom4j.Element;
import org.dom4j.QName;

import com.sanri.app.xsd.XsdContext;

import sanri.utils.DOMUtil;

/**
 * 
 * 作者:sanri <br/>
 * 时间:2017-8-17下午1:17:51<br/>
 * 功能:xsd attribute 元素 <br/>
 */
public class XsdAttribute extends XsdNode{
	public XsdAttribute(Element element) {
		super(element);
	}
	private XsdType type;
	private String use;
	private String defaultValue;
	
	public XsdType getType() {
		return type;
	}
	public String getUse() {
		return use;
	}
	@Override
	public void parse() {
		super.parse();
		//解析出 use ,default 属性和 type 属性
		String typeName = DOMUtil.elementAttrValue(element, "type");
		if(xsdContext.getTypeMap().containsKey(typeName)){
			XsdType xsdType = xsdContext.getTypeMap().get(typeName);
			this.type = xsdType;
		}else{
			if(StringUtils.isBlank(typeName)){
				//有可能是内部简单类型
				Element attrSimpletypeElement = element.element(new QName(XsdContext.SIMPLE_TYPE_NAME,namespace));
				XsdSimpleType xsdSimpleType = new XsdSimpleType(attrSimpletypeElement);
				xsdSimpleType.setNamespace(namespace);
				xsdSimpleType.setXsdContext(xsdContext);
				xsdSimpleType.parse();
				this.type = xsdSimpleType;
			}else{
				logger.error("找不到类型:"+typeName+" 在:"+xsdContext.getXsdUrl());
			}
			
		}
		String useName = DOMUtil.elementAttrValue(element, "use");
		this.use = useName;
		String defaultValue = DOMUtil.elementAttrValue(element, "default");
		this.defaultValue = defaultValue;
	}
	public String getDefaultValue() {
		return defaultValue;
	}
}
