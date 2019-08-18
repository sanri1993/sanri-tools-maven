package com.sanri.app.servlet;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.UnknownServiceException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

import sanri.utils.PathUtil;

import com.alibaba.fastjson.JSONObject;
import com.sanri.app.BaseServlet;
import com.sanri.app.wscall.WsdlOperation;
import com.sanri.app.wscall.WsdlParam;
import com.sanri.app.wscall.WsdlPort;
import com.sanri.app.wscall.WsdlService;
import com.sanri.app.wscall.WsdlServiceLoader;
import com.sanri.frame.RequestMapping;

/**
 * 
 * 作者:sanri <br/>
 * 时间:2017-6-7下午4:27:41<br/>
 * 功能: webservice 调用 <br/>
 */
@RequestMapping("/wscall")
public class WSCallServlet extends BaseServlet {
	
	private final static Map<String,WsdlService> wsdlServiceMap = new HashMap<String, WsdlService>();
	private final static Map<String,String> supportPortMap  = new HashMap<String, String>();
	private final static Map<String,String> wsdlKeyAddressMap = new HashMap<String, String>();
	
	static{
		//已知的 wsdl 地址读取
		String pkgPath = PathUtil.pkgPath("com.sanri.config");
		Properties properties = new Properties();
		FileReader configFileReader = null;
		try {
			configFileReader = new FileReader(new File(pkgPath+"/wsdl.properties"));
			properties.load(configFileReader);
			
			Iterator<Object> iterator = properties.keySet().iterator();
			while(iterator.hasNext()){
				String propertieKey = ObjectUtils.toString(iterator.next());
				String[] keySplit = propertieKey.split("\\.");
				String serviceKey = keySplit[0];
				String item = keySplit[1];
				String propertieValue = ObjectUtils.toString(properties.get(propertieKey));

				if("wsdlURL".equalsIgnoreCase(item)){
					WsdlService loadService = WsdlServiceLoader.loadService(propertieValue);
					wsdlServiceMap.put(serviceKey, loadService);
					wsdlKeyAddressMap.put(propertieValue, serviceKey);
				}else if("soap".equalsIgnoreCase(item)){
					supportPortMap.put(serviceKey, propertieValue);
				}
				
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally{
			IOUtils.closeQuietly(configFileReader);
		}
	}
	
	/**
	 * 
	 * 作者:sanri <br/>
	 * 时间:2017-6-9下午9:08:56<br/>
	 * 功能:列出目前支持的所有 wsdl 地址,及其键值 <br/>
	 * @return
	 */
	public Map<String,String> listAllAddress(){
		Map<String,String> addressMap = new HashMap<String,String>();
		Iterator<Entry<String, WsdlService>> iterator = wsdlServiceMap.entrySet().iterator();
		while(iterator.hasNext()){
			Entry<String, WsdlService> wsdlServiceEntry = iterator.next();
			String wsdlKey = wsdlServiceEntry.getKey();
			WsdlService wsdlService = wsdlServiceEntry.getValue();
			String wsdlURL = wsdlService.getWsdlContext().getWsdlURL().toString();
			addressMap.put(wsdlKey, wsdlURL);
		}
		return addressMap;
	}
	
	/**
	 * 
	 * 功能:查找已经存在的 webservice 信息<br/>
	 * 创建时间:2017-6-24上午9:50:56<br/>
	 * 作者：sanri<br/>
	 * @param wsdkKey
	 * @return<br/>
	 * 数据量太大了 @Deprecated
	 */
	@Deprecated
	public WsdlService loadService(String wsdlAddress){
		boolean containsKey = wsdlKeyAddressMap.containsKey(wsdlAddress);
		if(containsKey){
			String wsdlKey = wsdlKeyAddressMap.get(wsdlAddress);
			return wsdlServiceMap.get(wsdlKey);
		}
		WsdlService loadService = WsdlServiceLoader.loadService(wsdlAddress);
		//存储临时的 webservice 
		String wsdlKey = String.valueOf(System.currentTimeMillis());
		wsdlServiceMap.put(wsdlKey, loadService);
		return loadService;
	}
	
	/**
	 * 
	 * 功能:加载临时的 webservice<br/>
	 * 创建时间:2017-6-24上午10:08:02<br/>
	 * 作者：sanri<br/>
	 * @param wsdlAddress<br/>
	 * @return  返回 wsdlKey 对于临时用的,返回的是临时的 key
	 */
	public String loadServiceInfo(String wsdlAddress){
		boolean containsKey = wsdlKeyAddressMap.containsKey(wsdlAddress);
		if(containsKey){
			String wsdlKey = wsdlKeyAddressMap.get(wsdlAddress);
			return wsdlKey;
		}
		WsdlService loadService = WsdlServiceLoader.loadService(wsdlAddress);
		//存储临时的 webservice 
		String wsdlKey = String.valueOf(System.currentTimeMillis());
		wsdlServiceMap.put(wsdlKey, loadService);
		wsdlKeyAddressMap.put(wsdlAddress, wsdlKey);		//保存临时解析过的,不要重复解析
		return wsdlKey;
	}
	
	/**
	 * 
	 * 功能:<br/>
	 * 创建时间:2017-6-24上午8:27:35<br/>
	 * 作者：sanri<br/>
	 * @return<br/>
	 * @throws UnknownServiceException 
	 */
	public Set<String> listAllPort(String wsdlKey) throws UnknownServiceException{
		if(StringUtils.isBlank(wsdlKey)){
			throw new IllegalArgumentException("需提供 wsdlKey:"+wsdlKey);
		}
		WsdlService wsdlService = wsdlServiceMap.get(wsdlKey);
		if(wsdlService == null){
			throw new UnknownServiceException("未知服务 :"+wsdlKey);
		}
		Map<String, WsdlPort> wsdlPortMap = wsdlService.getWsdlPortMap();
		return wsdlPortMap.keySet();
	}
	
	/**
	 * 
	 * 功能:判断给定的 portName 是否被支持<br/>
	 * 创建时间:2017-6-24上午8:36:34<br/>
	 * 作者：sanri<br/>
	 * @param wsdlKey wsdl Key 值
	 * @param portName port 名称
	 * @return<br/>
	 */
	public boolean isPortSupport(String wsdlKey,String portName){
		if(StringUtils.isBlank(wsdlKey) || StringUtils.isBlank(portName)){
			throw new IllegalArgumentException("参数错误 wsdlKey:"+wsdlKey+",portName:"+portName);
		}
		String supportPortName = supportPortMap.get(wsdlKey);
		if(StringUtils.isNotBlank(supportPortName) && supportPortName.equals(portName)){
			return true;
		}
		return false;
	}
	
	/**
	 * 
	 * 作者:sanri <br/>
	 * 时间:2017-6-9下午9:09:54<br/>
	 * 功能: 根据 wsdl 地址和 portName 获取所有的方法<br/>
	 * @param wsdl
	 * @param portName
	 * @return
	 */
	public Set<String> listAllMethods(String wsdlKey,String portName){
		if(StringUtils.isBlank(wsdlKey) || StringUtils.isBlank(portName)){
			throw new IllegalArgumentException("参数错误 wsdlKey:"+wsdlKey+",portName:"+portName);
		}
//		String supportPortName = supportPortMap.get(wsdlKey);
//		if(StringUtils.isBlank(supportPortName) || !supportPortName.equals(portName)){
//			throw new UnSupportPortNameException("不支持的 portName:"+portName);
//		}
		WsdlService wsdlService = wsdlServiceMap.get(wsdlKey);
		if(wsdlService != null){
			WsdlPort wsdlPort = wsdlService.getWsdlPort(portName);
			if(wsdlPort != null){
				Map<String, WsdlOperation> wsdlOperationMap = wsdlPort.getWsdlOperationMap();
				return wsdlOperationMap.keySet();
			}
		}
		return null;
	}
	
	/**
	 * 
	 * 作者:sanri <br/>
	 * 时间:2017-6-9下午9:11:14<br/>
	 * 功能:列出方法的所有输入参数 <br/>
	 * @param wsdl
	 * @param methodName
	 * @return
	 */
	public WsdlParam methodInputParams(String wsdlKey,String portName,String operationName){
		if(StringUtils.isBlank(wsdlKey) || StringUtils.isBlank(portName) || StringUtils.isBlank(operationName)){
			throw new IllegalArgumentException("参数错误 wsdlKey:"+wsdlKey+",portName:"+portName+",operationName:"+operationName);
		}
//		String supportPortName = supportPortMap.get(wsdlKey);
//		if(StringUtils.isBlank(supportPortName) || !supportPortName.equals(portName)){
//			throw new UnSupportPortNameException("不支持的 portName:"+portName);
//		}
		WsdlService wsdlService = wsdlServiceMap.get(wsdlKey);
		if(wsdlService != null){
			WsdlPort wsdlPort = wsdlService.getWsdlPort(portName);
			if(wsdlPort != null){
				WsdlOperation wsdlOperation = wsdlPort.getWsdlOperation(operationName);
				if(wsdlOperation != null){
					return wsdlOperation.getInput();
				}
			}
		}
		return null;
	}
	
	/**
	 * 
	 * 作者:sanri <br/>
	 * 时间:2017-6-9下午9:11:14<br/>
	 * 功能:列出方法的所有输出参数 <br/>
	 * @param wsdl
	 * @param methodName
	 * @return
	 */
	public WsdlParam methodOutputParams(String wsdlKey,String portName,String operationName){
		if(StringUtils.isBlank(wsdlKey) || StringUtils.isBlank(portName) || StringUtils.isBlank(operationName)){
			throw new IllegalArgumentException("参数错误 wsdlKey:"+wsdlKey+",portName:"+portName+",operationName:"+operationName);
		}
//		String supportPortName = supportPortMap.get(wsdlKey);
//		if(StringUtils.isBlank(supportPortName) || !supportPortName.equals(portName)){
//			throw new UnSupportPortNameException("不支持的 portName:"+portName);
//		}
		WsdlService wsdlService = wsdlServiceMap.get(wsdlKey);
		if(wsdlService != null){
			WsdlPort wsdlPort = wsdlService.getWsdlPort(portName);
			if(wsdlPort != null){
				WsdlOperation wsdlOperation = wsdlPort.getWsdlOperation(operationName);
				if(wsdlOperation != null){
					return wsdlOperation.getOutput();
				}
			}
		}
		return null;
	}
	
	/**
	 * 
	 * 功能:构建  soap 模板消息<br/>
	 * 创建时间:2017-6-24上午8:51:07<br/>
	 * 作者：sanri<br/>
	 * @param wsdlKey 
	 * @param portNameport 名称
	 * @param operationName 方法名称
	 * @return<br/> 模板字符串 xml 格式
	 */
	public String buildSoapMessage(String wsdlKey,String portName,String operationName){
		if(StringUtils.isBlank(wsdlKey) || StringUtils.isBlank(portName) || StringUtils.isBlank(operationName)){
			throw new IllegalArgumentException("参数错误 wsdlKey:"+wsdlKey+",portName:"+portName+",operationName:"+operationName);
		}
//		String supportPortName = supportPortMap.get(wsdlKey);
//		if(StringUtils.isBlank(supportPortName) || !supportPortName.equals(portName)){
//			throw new UnSupportPortNameException("不支持的 portName:"+portName);
//		}
		WsdlService wsdlService = wsdlServiceMap.get(wsdlKey);
		if(wsdlService != null){
			WsdlPort wsdlPort = wsdlService.getWsdlPort(portName);
			if(wsdlPort != null){
				WsdlOperation wsdlOperation = wsdlPort.getWsdlOperation(operationName);
				if(wsdlOperation != null){
					return wsdlOperation.buildRequestTemplate();
				}
			}
		}
		return null;
	}
	
	/**
	 * 
	 * 功能:使用原生 xml 字符串来调用方法<br/>
	 * 创建时间:2017-6-24上午8:52:17<br/>
	 * 作者：sanri<br/>
	 * @return<br/> xml 返回结果
	 */
	public String invokeMethod(String wsdlKey,String portName,String operationName,String soapMessage){
		if(StringUtils.isBlank(wsdlKey) || StringUtils.isBlank(portName) || StringUtils.isBlank(operationName)){
			throw new IllegalArgumentException("参数错误 wsdlKey:"+wsdlKey+",portName:"+portName+",operationName:"+operationName);
		}
//		String supportPortName = supportPortMap.get(wsdlKey);
//		if(StringUtils.isBlank(supportPortName) || !supportPortName.equals(portName)){
//			throw new UnSupportPortNameException("不支持的 portName:"+portName);
//		}
		WsdlService wsdlService = wsdlServiceMap.get(wsdlKey);
		if(wsdlService != null){
			WsdlPort wsdlPort = wsdlService.getWsdlPort(portName);
			if(wsdlPort != null){
				WsdlOperation wsdlOperation = wsdlPort.getWsdlOperation(operationName);
				if(wsdlOperation != null){
					try {
						String soapResult = wsdlOperation.invoke(soapMessage);
						return soapResult;
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		}
		return null;
	}
	
	/**
	 * 
	 * 作者:sanri <br/>
	 * 时间:2017-6-9下午9:12:11<br/>
	 * 功能:调用方法 <br/>
	 * @param wsdlKey 
	 * @param portName port 名称
	 * @param operationName  方法名称
	 * @param params 方法入参
	 * @return 输出 参数 
	 */
	public JSONObject callMethod(String wsdlKey,String portName,String operationName,JSONObject params){
		if(StringUtils.isBlank(wsdlKey) || StringUtils.isBlank(portName) || StringUtils.isBlank(operationName)){
			throw new IllegalArgumentException("参数错误 wsdlKey:"+wsdlKey+",portName:"+portName+",operationName:"+operationName);
		}
//		String supportPortName = supportPortMap.get(wsdlKey);
//		if(StringUtils.isBlank(supportPortName) || !supportPortName.equals(portName)){
//			throw new UnSupportPortNameException("不支持的 portName:"+portName);
//		}
		WsdlService wsdlService = wsdlServiceMap.get(wsdlKey);
		if(wsdlService != null){
			WsdlPort wsdlPort = wsdlService.getWsdlPort(portName);
			if(wsdlPort != null){
				WsdlOperation wsdlOperation = wsdlPort.getWsdlOperation(operationName);
				if(wsdlOperation != null){
					JSONObject retValue = wsdlOperation.invoke(params);
					return retValue;
				}
			}
		}
		return null;
	}
}
