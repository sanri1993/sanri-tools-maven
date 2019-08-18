package com.sanri.app.xsd.node;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.Element;
import org.dom4j.Namespace;
import org.dom4j.QName;

import sanri.utils.DOMUtil;

import com.sanri.app.xsd.XsdContext;

public abstract class XsdNode {
	/**
	 * 节点的文档说明信息
	 */
	protected String documentation;
	/**
	 * 节点名称
	 */
	protected String name;
	/**
	 * 当前文档节点的命名空间
	 */
	protected Namespace namespace;
	/**
	 * 上下文
	 */
	protected XsdContext xsdContext;
	/**
	 * dom4j 元素
	 */
	protected Element element;
	
	private long minOccurs;
	private long maxOccurs;
	
	protected Log logger = LogFactory.getLog(getClass());
	
	public XsdNode(Element element){
		this.element = element;
	}
	public void parse(){
		//解析名称
		String name = DOMUtil.elementAttrValue(element, "name");
		this.name = name;
		//解析文档标注
		Element annotationElement = element.element(new QName(XsdContext.ANNOTATION_NAME,namespace));
		if(annotationElement != null){
			//取出文档元素,获取内容
			Element documentElement = annotationElement.element(new QName(XsdContext.DOCUMENTATION_NAME,namespace));
			if(documentElement != null){
				String text = documentElement.getText();
				this.documentation = text;
			}
		}
		String minOccurs = DOMUtil.elementAttrValue(element, "minOccurs");
		String maxOccurs = DOMUtil.elementAttrValue(element, "maxOccurs");
		if(StringUtils.isNotBlank(minOccurs)){
			if("unbounded".equals(minOccurs)){
				this.minOccurs = Long.MAX_VALUE;
			}else{
				this.minOccurs = Long.parseLong(minOccurs);
			}
		}
		if(StringUtils.isNotBlank(maxOccurs)){
			if("unbounded".equals(maxOccurs)){
				this.maxOccurs = Long.MAX_VALUE;
			}else{
				this.maxOccurs = Long.parseLong(maxOccurs);
			}
		}
	}
	
	public XsdContext getXsdContext() {
		return xsdContext;
	}
	public void setXsdContext(XsdContext xsdContext) {
		this.xsdContext = xsdContext;
	}
	public void setNamespace(Namespace namespace) {
		this.namespace = namespace;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getName() {
		return name;
	}
	public long getMinOccurs() {
		return minOccurs;
	}
	public long getMaxOccurs() {
		return maxOccurs;
	}
}
