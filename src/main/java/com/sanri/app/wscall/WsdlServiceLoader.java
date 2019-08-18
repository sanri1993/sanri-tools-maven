package com.sanri.app.wscall;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import javax.wsdl.Definition;
import javax.wsdl.Service;
import javax.wsdl.WSDLException;
import javax.wsdl.factory.WSDLFactory;
import javax.wsdl.xml.WSDLReader;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * 
 * 作者:sanri <br/>
 * 时间:2017-6-21上午11:17:34<br/>
 * 功能:加载 webservice 主类 <br/>
 */
public class WsdlServiceLoader {
	private final static Log logger = LogFactory.getLog(WsdlServiceLoader.class);
	
	private static WSDLFactory wsdlFactory = null;
	static{
		try {
			wsdlFactory = WSDLFactory.newInstance();
		} catch (WSDLException e) {
			e.printStackTrace();
		}
	}
	/**
	 * 
	 * 作者:sanri <br/>
	 * 时间:2017-6-21上午11:18:41<br/>
	 * 功能:从一个 url 地址加载 webservice  <br/>
	 * @param wsdlAddress
	 * @return
	 */
	public static WsdlService loadService(String webserviceUrl){
		URL fixedDeployDocumentURL = fixedDeployDocumentURL(webserviceUrl);
		return loadService(fixedDeployDocumentURL);
	}
	
	/**
	 * 
	 * 作者:sanri <br/>
	 * 时间:2017-6-21下午2:18:07<br/>
	 * 功能:从网络路径加载 webservice<br/>
	 * @param url
	 * @return
	 */
	public static WsdlService loadService(URL url){
		if(url == null){
			throw new IllegalArgumentException("wsdl 地址为空");
		}
		String query = url.getQuery();
		if(query != null && !query.endsWith("wsdl")){
			throw new IllegalArgumentException("wsdl 文档地址需要以 wsdl 结尾 url:"+url);
		}
		WsdlContext wsdlContext = parserWsdlContext(url);
		WsdlService wsdlService = new WsdlService();
		wsdlService.setWsdlContext(wsdlContext);
		Service findAllService = findAllService(wsdlContext);
		wsdlService.setService(findAllService);
		wsdlService.parserPorts();
		return wsdlService;
	}
	
	/**
	 * 
	 * 作者:sanri <br/>
	 * 时间:2017-6-21下午2:47:32<br/>
	 * 功能: 只会解析第一个服务,有多个服务类以后再说 TODO  <br/>
	 * @param wsdlContext
	 * @return
	 */
	@SuppressWarnings("unchecked")
	private static Service findAllService(WsdlContext wsdlContext) {
		Definition definition = wsdlContext.getDefinition();
		Map<String,Service> allServices = definition.getAllServices();
		Iterator<Entry<String, Service>> iterator = allServices.entrySet().iterator();
		while(iterator.hasNext()){
			Entry<String, Service> serviceEntry = iterator.next();
			Service service = serviceEntry.getValue();
			return service;
		}
		return null;
	}

	/**
	 * 
	 * 作者:sanri <br/>
	 * 时间:2017-6-21下午2:30:12<br/>
	 * 功能:从文件系统加载 webservice <br/>
	 * @param file
	 * @return
	 */
	public static WsdlService loadService(File file){
		if(file == null || !file.exists()){
			throw new IllegalArgumentException("wsdl 文件不存在");
		}
		try {
			URI fileURI = file.toURI();
			URL fileURL = fileURI.toURL();
			return loadService(fileURL);
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * 
	 * 作者:sanri <br/>
	 * 时间:2017-6-21下午2:25:45<br/>
	 * 功能:解析 webservice 上下文方法 <br/>
	 * @param url
	 */
	private static WsdlContext parserWsdlContext(URL url) {
		WSDLReader wsdlReader = wsdlFactory.newWSDLReader();
		wsdlReader.setFeature("javax.wsdl.verbose", true);
		wsdlReader.setFeature("javax.wsdl.importDocuments", true);
		try {
			logger.info("读取 wsdl 文件:"+url);
			Definition definition = wsdlReader.readWSDL(url.toString());
			WsdlContext wsdlContext = new WsdlContext();
			wsdlContext.setDefinition(definition);
			wsdlContext.setWsdlURL(url);
			String targetNamespace = definition.getTargetNamespace();
			wsdlContext.setTargetNamespace(targetNamespace);
			wsdlContext.processTypes();
			return wsdlContext;
		} catch (WSDLException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	/**
	 * 
	 * 功能:修正为 wsdl 正确地址<br/>
	 * 创建时间:2017-6-7下午9:02:54<br/>
	 * 作者：sanri<br/>
	 * @param url 传入的 url 要么不带一个参数,要么带 ?wsdl ,其它情况不支持,暂时不会报错,以后将要修改方法使其抛出异常
	 * @return<br/>
	 */
	private static URL fixedDeployDocumentURL(String webserviceUrl){
		if(StringUtils.isBlank(webserviceUrl)){
			throw new IllegalArgumentException("请传入合法的 wsdl 地址,目前传入为:"+webserviceUrl);
		}
		if(!webserviceUrl.endsWith("?wsdl")){
			webserviceUrl = webserviceUrl + "?wsdl";
			logger.warn("修正 wsdl 地址为:"+webserviceUrl);
		}
		URL url = null;
		try {
			url = new URL(webserviceUrl);
			return url;
		} catch (MalformedURLException e) {
			logger.error("传入路径不合法:"+webserviceUrl+",请使用 wsdl 文档地址");
			e.printStackTrace();
		}
		return null;
	}
}
