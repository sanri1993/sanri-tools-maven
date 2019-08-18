package com.sanri.app.xsd;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.XMLConstants;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.Namespace;
import org.dom4j.QName;

import sanri.utils.DOMUtil;

import com.sanri.app.xsd.node.XsdComplexType;
import com.sanri.app.xsd.node.XsdElement;
import com.sanri.app.xsd.node.XsdNode;
import com.sanri.app.xsd.node.XsdSimpleType;
import com.sanri.app.xsd.node.XsdType;

/**
 * 
 * 创建时间:2017-8-16上午7:09:32<br/>
 * 创建者:sanri<br/>
 * 功能:xsd 文件解析上下文<br/>
 */
public class XsdContext extends XsdNode{
	private Log logger = LogFactory.getLog(getClass());
	private Element rootElement;
	private Namespace namespace;
	
	public XsdContext(Document document){
		super(document.getRootElement());
		//获取命名空间等属性
		Element rootElement = document.getRootElement();
		this.rootElement = rootElement;
		Namespace xsdNamespace = rootElement.getNamespaceForURI(XMLConstants.W3C_XML_SCHEMA_NS_URI);
		this.namespace = xsdNamespace;
		
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public void parse() {
		//初始化简单类型 token 也是一种基本类型,	不包含换行符、回车或制表符、开头或结尾空格或者多个连续空格的字符串
		String [] simpleTypes = {"string","int","long","boolean","token"};
		for (String simpleType : simpleTypes) {
			XsdSimpleType xsdSimpleType = new XsdSimpleType(null);
			xsdSimpleType.setName(simpleType);
			xsdSimpleType.setNamespace(namespace);
			xsdSimpleType.setXsdContext(this);
			typeMap.put(simpleType, xsdSimpleType);
		}
		
		//解析导入的文档
		Iterator<Element> importElementIterator = rootElement.elementIterator(new QName(IMPORT_NAME, namespace));
		while(importElementIterator.hasNext()){
			Element importElement = importElementIterator.next();
			String schemaLocation = DOMUtil.elementAttrValue(importElement, "schemaLocation");
			if(StringUtils.isBlank(schemaLocation)){continue;}
			try {
				String namespace = DOMUtil.elementAttrValue(importElement, "namespace");
				File xsdFile = XsdLoader.getXsdFile(namespace);
				XsdContext importXsdContext = null;
				if(xsdFile != null){
					importXsdContext = XsdLoader.loadXsdFromFile(xsdFile);
				}else{
					importXsdContext = XsdLoader.loadXsdFromUrl(schemaLocation);
				}
				this.importMap.put(schemaLocation, importXsdContext);
			} catch (MalformedURLException e) {
				e.printStackTrace();
			} catch (DocumentException e) {
				e.printStackTrace();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
		}
		
		//所有导入文档都解析完成后,接下来可以解析类型元素
		
		//先解析简单类型
		Iterator<Element> simpleTypeIterator = rootElement.elementIterator(new QName(SIMPLE_TYPE_NAME,namespace));
		while(simpleTypeIterator.hasNext()){
			Element simpleTypeElement = simpleTypeIterator.next();
			XsdSimpleType xsdSimpleType = new XsdSimpleType(simpleTypeElement);
			xsdSimpleType.setNamespace(namespace);
			xsdSimpleType.setXsdContext(this);
			xsdSimpleType.parse();
			typeMap.put(xsdSimpleType.getName(), xsdSimpleType);
		}
		//后解析复杂类型
		Iterator<Element> typeIterator = rootElement.elementIterator(new QName(COMPLEX_TYPE_NAME, namespace));
		while(typeIterator.hasNext()){
			Element typeElement = typeIterator.next();
			String elementName = DOMUtil.elementAttrValue(typeElement, "name");
			if(typeMap.containsKey(elementName)){
				logger.debug("类型元素:"+elementName+",已经被解析,跳过");
				continue;
			}
			XsdComplexType xsdComplexType = new XsdComplexType(typeElement);
			xsdComplexType.setXsdContext(this);
			xsdComplexType.setNamespace(namespace);
			xsdComplexType.parse();
			typeMap.put(xsdComplexType.getName(), xsdComplexType);
		}
		
		//解析文档中所有的元素
		Iterator<Element> elementIterator = rootElement.elementIterator(new QName(ELEMENT_NAME,namespace));
		while(elementIterator.hasNext()){
			Element element = elementIterator.next();
			String elementName = DOMUtil.elementAttrValue(element, "name");
			if(elementMap.containsKey(elementName)){
				//如果元素已经被解析,则跳过;因为类型有可能引用元素,所以有可能在之前就解析了
				logger.debug("元素 :"+elementName+"已经被解析,已跳过");
				continue;
			}
			XsdElement xsdElement = new XsdElement(element);
			xsdElement.setXsdContext(this);
			xsdElement.setNamespace(namespace);
			xsdElement.parse();
			elementMap.put(xsdElement.getName(), xsdElement);
		}
	}
	
	/**
	 * 
	 * 功能:临时解析当前文档中的指定元素,并加入 element 列表<br/>
	 * 创建时间:2017-8-18上午10:55:51<br/>
	 * 作者：sanri<br/>
	 * @param name
	 * @return<br/>
	 */
	@SuppressWarnings("unchecked")
	public XsdElement parseElement(String name){
		List<Element> elements = this.rootElement.elements(new QName(ELEMENT_NAME,namespace));
		if(elements != null){
			for (Element element : elements) {
				String elementName = DOMUtil.elementAttrValue(element, "name");
				if(StringUtils.isNotBlank(elementName) && elementName.equals(name)){
					//找到元素,开始解析
					XsdElement xsdElement = new XsdElement(element);
					xsdElement.setNamespace(namespace);
					xsdElement.setXsdContext(this);
					xsdElement.parse();
					this.elementMap.put(xsdElement.getName(), xsdElement);
					return xsdElement;
				}
			}
		}
		return null;
	}
	
	/**
	 * 
	 * 功能:临时解析文档中指定类型,并加入类型列表<br/>
	 * 创建时间:2017-8-20上午9:32:42<br/>
	 * 作者：sanri<br/>
	 * @param typeName
	 * @return<br/>
	 */
	@SuppressWarnings("unchecked")
	public XsdType parseComplexType(String typeName) {
		List<Element> elements = this.rootElement.elements(new QName(COMPLEX_TYPE_NAME,namespace));
		if(elements != null){
			for (Element element : elements) {
				String elementName = DOMUtil.elementAttrValue(element, "name");
				if(StringUtils.isNotBlank(elementName) && elementName.equals(typeName)){
					//找到元素,开始解析
					XsdComplexType xsdComplexType = new XsdComplexType(element);
					xsdComplexType.setNamespace(namespace);
					xsdComplexType.setXsdContext(this);
					xsdComplexType.parse();
					this.typeMap.put(xsdComplexType.getName(), xsdComplexType);
					return xsdComplexType;
				}
			}
		}
		return null;
	}
	
	public final static String IMPORT_NAME = "import";
	public final static String COMPLEX_TYPE_NAME = "complexType";
	public final static String COMPLEX_TYPE_CONTENT_NAME = "complexContent";
	public final static String ELEMENT_NAME = "element";
	public final static String ATTRIBUTE_NAME = "attribute";
	public final static String SIMPLE_TYPE_NAME = "simpleType";
	public final static String RESTRICTION_TYPE_NAME = "restriction";
	public final static String ENUMERATION_TYPE_NAME = "enumeration";
	public final static String EXTENSION_NAME = "extension";
	public final static String SEQUENCE_NAME = "sequence";
	public final static String ANNOTATION_NAME = "annotation";
	public final static String DOCUMENTATION_NAME = "documentation";
	public final static String CHOICE_NAME = "choice";
	
	// xsd 地址,可以是文件协议,http 协议,有可能为空,如果是通过 xml 字符串加载的话
	private URL xsdUrl;
	// 参数列表 参数名=->参数类型
	Map<String,XsdElement> elementMap = new HashMap<String, XsdElement>();
	// 类型列表 类型名==>类型,如果无类型名,则使用上级参数名
	Map<String,XsdType> typeMap = new HashMap<String, XsdType>();
	//导入的 xsd 文件列表 uri ==> xsdContext
	Map<String,XsdContext> importMap = new HashMap<String, XsdContext>();
	public URL getXsdUrl() {
		return xsdUrl;
	}
	void setXsdUrl(URL xsdUrl) {
		this.xsdUrl = xsdUrl;
	}
	public Map<String, XsdElement> getElementMap() {
		return elementMap;
	}
	public Map<String, XsdType> getTypeMap() {
		return typeMap;
	}
	public Map<String, XsdContext> getImportMap() {
		return importMap;
	}


}
