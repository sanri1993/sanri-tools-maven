package com.sanri.app.xsd.node;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.dom4j.Element;
import org.dom4j.QName;

import com.sanri.app.xsd.XsdContext;

import sanri.utils.DOMUtil;

/**
 * 
 * 作者:sanri <br/>
 * 时间:2017-8-17下午11:10:55<br/>
 * 功能:xsd 元素节点 <br/>
 */
public class XsdElement extends XsdNode{
	public XsdElement(Element element) {
		super(element);
	}
	private XsdType type;
	@Override
	public void parse() {
		super.parse();
		//如果元素已经存在,则不需要再解析
		if(xsdContext.getElementMap().containsKey(this.name)){
			logger.debug("当前元素已经被解析过了:"+this.name);
			return ;
		}
		String ref = DOMUtil.elementAttrValue(element, "ref");
		if(StringUtils.isNotBlank(ref) ){
			//TODO 有循环引用的问题,暂时先写死跳过
			if("beans".equals(ref)){
				return ;
			}
			//如果有引用其它元素,则获取其它元素
			boolean containsKey = xsdContext.getElementMap().containsKey(ref);
			if(!containsKey){
				//如查找不到元素,则从其它的上下文中查找元素
				Map<String, XsdContext> importMap = xsdContext.getImportMap();
				Collection<XsdContext> values = importMap.values();
				Iterator<XsdContext> iterator = values.iterator();
				while(iterator.hasNext()){
					XsdContext importContext = iterator.next();
					boolean importContains = importContext.getElementMap().containsKey(ref);
					if(importContains){
						return ;
					}
				}
//				logger.warn("未解析的元素"+ref+",可能在后面定义的,尝试从当前文档解析:"+ref+",文档地址:"+xsdContext.getXsdUrl());
				XsdElement xsdElement = xsdContext.parseElement(ref);
				if(xsdElement != null){
					return ;
				}
				logger.error("当前文档找不到元素:"+ref+",文档地址:"+xsdContext.getXsdUrl());
				return ;
			}
			// 引用其它元素的,直接解析完毕
			return ;
		}
		String typeName = DOMUtil.elementAttrValue(element, "type");
		if(xsdContext.getTypeMap().containsKey(typeName)){
			XsdType xsdType = xsdContext.getTypeMap().get(typeName);
			this.type = xsdType;
		}else{
			if(StringUtils.isBlank(typeName)){
				//内部有复杂类型
				Element complexTypeElement = element.element(new QName(XsdContext.COMPLEX_TYPE_NAME,namespace));
				if(complexTypeElement != null){
					XsdComplexType xsdComplexType = new XsdComplexType(complexTypeElement);
					xsdComplexType.setNamespace(namespace);
					xsdComplexType.setXsdContext(xsdContext);
					xsdComplexType.parse();
					this.type = xsdComplexType;
				}else{
					//内部有简单类型
					Element simpleTypeElement = element.element(new QName(XsdContext.SIMPLE_TYPE_NAME, namespace));
					if(simpleTypeElement != null){
						XsdSimpleType xsdSimpleType = new XsdSimpleType(simpleTypeElement);
						xsdSimpleType.setNamespace(namespace);
						xsdSimpleType.setXsdContext(xsdContext);
						xsdSimpleType.parse();
						this.type = xsdSimpleType;
					}else{
						logger.warn(this.name+":内部即不是复杂类型,也不是简单类型,类型字段也是空的,有可能是空元素,即没有属性,又没有子元素");
					}
				}
			}else{
				logger.error("未找到类型:"+typeName);
			}
		}
	}
	public XsdType getType() {
		return type;
	}
}
