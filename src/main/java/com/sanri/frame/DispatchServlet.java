package com.sanri.frame;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.sanri.app.BaseServlet;
import javassist.ClassClassPath;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.Modifier;
import javassist.NotFoundException;
import javassist.bytecode.CodeAttribute;
import javassist.bytecode.LocalVariableAttribute;
import javassist.bytecode.MethodInfo;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.core.LocalVariableTableParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import sanri.utils.NumberUtil;
import sanri.utils.PathUtil;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 
 * 作者:sanri <br/>
 * 时间:2017-4-19下午4:30:33<br/>
 * 功能: servlet 派发器<br/>
 * 使用:
 * 	1.将本 servlet 注册到 web.xml 
 * 	2.配置初始化参数 component servlet 扫描路径
 * 	3.配置初始化参数 regex 来查找  servlet 匹配 servlet 的正则表达式
 * 	4.配置本 servlet 随容器一起启动
 * 	5.配置初始化参数  异常处理类 handlerExceptionResolver 异常处理类需实现  HandlerExceptionResolver
 * 	6.配置文件上传最大大小 maxUploadSize 以 Byte 为单位
 * 	7.配置文件上传临时路径(相对于 webapps 路径) tempPath,每当超过 1024*1024 Byte 时,文件将暂时缓存在临时区
 */
public class DispatchServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;

	protected Log logger = LogFactory.getLog(getClass());
	
	private String component;
	private String regex;
	private Map<String,NativeMethod> mappings = new HashMap<String, NativeMethod>();
	private HandlerExceptionResolver handlerExceptionResolver;
	
	//文件上传配置
	private long maxUploadSize;
	private String tempPath;
	private DiskFileItemFactory factory = new DiskFileItemFactory();
	//add by sanri at 2017/06/30 存储类的实例,使用单例调用 servlet
	private  static final Map<String,Object> CLASS_INSTANCE = new HashMap<String,Object>();
	
	@Override
	public void init(ServletConfig servletConfig) throws ServletException {
		long startTime = System.currentTimeMillis();
		//包扫描配置
		this.component = servletConfig.getInitParameter("component");
		this.regex = servletConfig.getInitParameter("regex");
		//文件上传配置
		String maxUploadSize_ = servletConfig.getInitParameter("maxUploadSize");
		if(!StringUtils.isBlank(maxUploadSize_)){
			this.maxUploadSize = Long.parseLong(maxUploadSize_);
		}
		this.tempPath = servletConfig.getInitParameter("tempPath");
		if(!StringUtils.isBlank(this.tempPath)){					//不设置 tempPath 会将所有文件放入内存中,可能造成内存溢出
			factory.setSizeThreshold(1024 * 1024);		//当超过
			File tempPathFile = BaseServlet.mkTmpPath("upload.temp");
			factory.setRepository(tempPathFile);
		}
		
		//异常处理类
		String exceptionResolverClass = servletConfig.getInitParameter("handlerExceptionResolver");
		if(!StringUtils.isBlank(exceptionResolverClass)){
			try {
				Class<?> exceptionClazz = Class.forName(exceptionResolverClass);
				boolean isExceptionResolver = false;
				Class<?>[] interfaces = exceptionClazz.getInterfaces();
				if(interfaces != null && interfaces.length > 0){
					for (Class<?> clazz : interfaces) {
						if(clazz == HandlerExceptionResolver.class){
							isExceptionResolver  = true;
						}
					}
				}
				if(!isExceptionResolver){
					throw new IllegalStateException("异常处理类类型不匹配,异常处理类需实现  HandlerExceptionResolver 接口");
				}
				handlerExceptionResolver = (HandlerExceptionResolver) exceptionClazz.newInstance();
			} catch (ClassNotFoundException e) {
				logger.error("异常类找不到,无法处理异常");
				e.printStackTrace();
			}catch (InstantiationException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			}
		}
		final String regex = this.regex;
		
		if(StringUtils.isBlank(component)){
			throw new ServletException("包 "+component+" 路径不存在,请检查配置");
		}
		String pkgPath = PathUtil.pkgPath(component);
		File pkgDir = new File(pkgPath);
		if(!pkgDir.exists()){
			throw new ServletException("包 "+component+" 路径不存在,请检查配置");
		}
		
		List<Class<?>> classList = new ArrayList<Class<?>>();
		//查询 servlet 类
		FileFilter filenameFilter = new FileFilter() {
			@Override
			public boolean accept(File file) {
				if(file.isDirectory() || StringUtils.isBlank(regex)){
					return true;
				}
				String baseName = FilenameUtils.getBaseName(file.getName());
				if("DispatchServlet".equals(baseName) || baseName.startsWith("DispatchServlet")){
					return false;
				}
				Pattern pattern = Pattern.compile(regex);
				Matcher matcher = pattern.matcher(baseName);
				if(matcher != null && matcher.find()){
					return true;
				}
				return false;
			}
		};
		findClass(pkgDir,filenameFilter,classList);
		//日志记录找到的类
		StringBuffer findClassList = new StringBuffer();
		if(classList != null && classList.size() > 0){
			for (Class<?> class1 : classList) {
				findClassList.append(class1.getSimpleName()).append(" ");
				//存储类实例
				try{
					Object newInstance = class1.newInstance();
					CLASS_INSTANCE.put(class1.getSimpleName(), newInstance);

//					Method[] methods = class1.getMethods();
//					if(ArrayUtils.isNotEmpty(methods)){
//						for (Method method : methods) {
//							PostConstruct postConstruct = method.getAnnotation(PostConstruct.class);
//							if(postConstruct != null){
//								MethodUtils.invokeMethod(newInstance,method.getName(),new Object[]{});
//							}
//						}
//					}
				}catch(Exception e){
					e.printStackTrace();
				}
			}
		}
		logger.info("查找到并实例化类("+classList.size()+"):"+findClassList.toString());
		//查找所有映射 
		try {
			mapping(classList);
			logger.info("映射接口数:"+mappings.size()+",框架启动用时: "+(System.currentTimeMillis() -startTime) + " ms");
		} catch (NotFoundException e) {
			logger.error("映射路径类 class 异常...");
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * 
	 * 作者:sanri <br/>
	 * 时间:2017-4-17下午4:20:27<br/>
	 * 功能:映射路径 <br/>
	 * 入参: <br/>
	 */
	static ClassPool classPool = ClassPool.getDefault();  
	private void mapping(List<Class<?>> classList) throws NotFoundException, ClassNotFoundException {
		if(classList != null && classList.size() > 0){
			classPool.insertClassPath(new ClassClassPath(getClass()));		//解决 tomcat 找不到类问题
			//add by sanri at 2017/05/17 加入父类的公共方法映射,并避免重复映射  TODO 根路径无法处理,等待后续解决
			Set<Class<?>> fatherClasses = new HashSet<Class<?>>();
			for (Class<?> clazz : classList) {
				CtClass ctClass = classPool.get(clazz.getName());
				RequestMapping rootPathMapping = (RequestMapping) ctClass.getAnnotation(RequestMapping.class);
				if(rootPathMapping == null){
					mappingMethods(clazz,ctClass,"/");
					continue;
				}
				String [] rootPathValues = rootPathMapping.value();
				for(String rootPath:rootPathValues){
					mappingMethods(clazz,ctClass,rootPath);
				} 
			} //end for classList

		}
	}

	// add by sanri at 2018/11/08 使用  spring 的参数名查找方法
	ParameterNameDiscoverer parameterNameDiscoverer = new LocalVariableTableParameterNameDiscoverer();

	//TODO 内部类方法也被映射了
	private void mappingMethods(Class<?> clazz, CtClass ctClass, String rootPath) throws ClassNotFoundException, NotFoundException {
		Method[] declaredMethods = clazz.getDeclaredMethods();
//		Method[] declaredMethods = clazz.getMethods();
		if(declaredMethods != null && declaredMethods.length > 0){
			for (Method method : declaredMethods) {
				Class<?> declaringClass = method.getDeclaringClass();
				if(declaringClass.isAnonymousClass()){
					//匿名类时不需要进行映射 add by sanri at 2017/04/21
					continue;
				}
				String methodName = method.getName();
				if(methodName.contains("$")){
					//解决有内部类时里面有线程之类的类时报错问题 add by sanri at 2017/04/21
					continue;
				}
				//add by sanri at 2017/05/16 只有公共方法和受保护方法(父类方法)需要映射,私有的,和默认的不需要
				int modifiers = method.getModifiers();
				if(modifiers != java.lang.reflect.Modifier.PUBLIC){
					continue;
				}
				Class<?>[] parameterTypes = method.getParameterTypes();
				CtMethod ctMethod = ctClass.getDeclaredMethod(methodName);
				MethodInfo methodInfo = ctMethod.getMethodInfo();
				CodeAttribute codeAttribute = methodInfo.getCodeAttribute();
				LocalVariableAttribute  localVariableAttribute = (LocalVariableAttribute) codeAttribute.getAttribute(LocalVariableAttribute.tag);
				if(localVariableAttribute == null){
					logger.error("localVariableAttribute 为 null ,当前方法名:"+methodName);
					continue;
				}
				String[] parameterNames = parameterNameDiscoverer.getParameterNames(method);

				int pos = Modifier.isStatic(ctMethod.getModifiers()) ? 0 : 1;
				String[] paramNames =  parameterNames;//new String [parameterTypes.length];
				List<MethodParam> methodParams = new ArrayList<MethodParam>();
				for (int i = 0; i < paramNames.length; i++) {
//					paramNames[i] = localVariableAttribute.variableName(i + pos);
//					//临时解决参数名异常问题,如果参数名为 e,this 带 $ ,则取下一个参数名
//					if("e".equals(paramNames[i]) || "this".equals(paramNames[i]) || paramNames[i].contains("$")){
//						i -- ;
//						pos ++;
//
//						continue;
//					}
					MethodParam methodParam = new MethodParam();
					methodParam.setClazz(clazz);
					methodParam.setMethod(method);
					methodParam.setName(paramNames[i]);
					methodParam.setParamClazz(parameterTypes[i]);
					methodParams.add(methodParam);
				}
				
				NativeMethod nativeMethod = new NativeMethod();
				nativeMethod.setClazz(clazz);
				nativeMethod.setMethod(method);
				nativeMethod.setReturnType(method.getReturnType());
				nativeMethod.setCtMethod(ctMethod);
				nativeMethod.setMethodParams(methodParams);
				
				RequestMapping pathMapping = (RequestMapping) ctMethod.getAnnotation(RequestMapping.class);
				if(pathMapping == null){
					// 直接用方法名进行映射
//					mappings.put(rootPath, nativeMethod);
					String urlPath = rootPath+"/"+ctMethod.getName();
                    if(urlPath.startsWith("//")){       //如果带了两个 / 的话,只用一个
                        urlPath =  urlPath.substring(1);
                    }
					mappings.put(urlPath, nativeMethod);
					logger.info(urlPath+" <=> "+nativeMethod);
					continue;
				}
				
				String[] pathValues = pathMapping.value();
				for(String path:pathValues){
				    if(path.endsWith("base64img")){
                        System.out.println();
                    }
				    String urlPath = rootPath+path;
                    if(urlPath.startsWith("//")){       //如果带了两个 / 的话,只用一个
                        urlPath =  urlPath.substring(1);
                    }
					mappings.put(urlPath, nativeMethod);
					logger.info(urlPath+" <=> "+nativeMethod);
				}
			}
		}
	}

	/**
	 * 
	 * 作者:sanri <br/>
	 * 时间:2017-4-17下午3:41:50<br/>
	 * 功能:查找所有的类 <br/>
	 * 入参: <br/>
	 */
	private void findClass(File pkgDir, FileFilter filenameFilter,  List<Class<?>> classList) {
		try {
			if(pkgDir.isFile()){
//				String name = FilenameUtils.getTitle(pkgDir.getTitle());
				String baseName = FilenameUtils.getBaseName(pkgDir.getName());
				if(baseName.contains("$")){		//add by sanri at 2017/04/21 解决内部类问题
					return ;
				}
				String class_ = component + "."+baseName;
				classList.add(Class.forName(class_));
			}else{
				File[] listFiles = pkgDir.listFiles(filenameFilter);
				for (File file : listFiles) {
					findClass(file, filenameFilter, classList);
				}
			}
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException ,IllegalArgumentException{
		processRequest(req,resp);
	}
	
	private void processRequest(HttpServletRequest request, HttpServletResponse response) throws IllegalArgumentException, ServletException {
		String methodName;
		try {
			long startTime = System.currentTimeMillis();
			
			//解析出调用方法名
			String requestURI = request.getRequestURI();
			URI uri = new URI(requestURI);
			ServletContext servletContext = request.getSession().getServletContext();
			String contextPath = servletContext.getContextPath();
			URI prefix = new URI(contextPath);
			methodName = prefix.relativize(uri).toString();
			if(StringUtils.isBlank(methodName)){
				//如果调用空方法,则为请求网站路径,则重定向到欢迎页
//				System.out.println();
				// 暂时先直接重定向到根路径下的 index.html
				response.sendRedirect(contextPath+"/index.html");

				return ;
			}else{
				//执行调用
				InvokeResult invoke = invoke("/" + methodName, request, response);
				if(!invoke.isSuccess()){
					logger.error("方法 "+methodName + " 执行失败,原因为:"+invoke.getReson());
					return ;
				}
				String ip = BaseServlet.remortIPInfo(request);

				//对于文件管理方法特殊标记是哪个模块的，方法查找问题
				if(methodName.startsWith("file/manager")){
					Map<String, Object> paramsValues = invoke.getParamsValues();
					methodName = methodName+"?modul=" + paramsValues.get("modul");
				}
				Method method = invoke.getMethod();
				IgnoreSpendTime ignoreSpendTime = method.getAnnotation(IgnoreSpendTime.class);
				if(ignoreSpendTime == null){
					logger.info(ip +" 调用 " +"方法 /" + methodName + " 调用时间: " + (System.currentTimeMillis() - startTime) + " ms");
				}
			}
		} catch (URISyntaxException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (FileUploadException e) {
			e.printStackTrace();
		} 
	}
	
	/**
	 * 
	 * 作者:sanri <br/>
	 * 时间:2017-4-19下午3:44:06<br/>
	 * 功能:解析请求参数 <br/>
	 * 注: 当前端以 ajax get 请求 传输 data 为一个对象(这时传字符串是错的),get 请求不用理会 contentType
	 * 	相当于在地址栏拼接如下信息
	 * 		connectionInfo[name]=conn1&connectionInfo[host]=localhost&connectionInfo[port]=3306&abc=黄正晶
	 * 	前端 ajax post 请求传输入 data 为一个对象(这时传字符串是错的)  contentType=application/x-www-form-urlencoded; charset=UTF-8
	 * 		传输入格式为 formdata 形式内容为:
	 * 		connectionInfo[name]=conn1&connectionInfo[host]=localhost&connectionInfo[port]=3306&abc=黄正晶
	 * 入参: <br/>
	 * @param response 
	 * @param nativeMethod 
	 * @throws IOException 
	 * @throws FileUploadException 
	 */
	@SuppressWarnings("unchecked")
	private JSONObject parserParams(HttpServletRequest request, HttpServletResponse response, NativeMethod nativeMethod) throws IOException,IllegalArgumentException, FileUploadException {
		String method = request.getMethod();
		String contentType = request.getContentType();
		
		if(StringUtils.isBlank(method)){
			response.sendError(400, "错误的请求,请传送 contentType 和 Method 属性");
			throw new IllegalArgumentException("前端请求参数错误");
		}
		if("GET".equalsIgnoreCase(method)){
			//get 请求不用理 contentType ,全部从 formdata 中拿数据,因为数据全部是拼接到地址栏上的
			return parserFormDataParams(request,nativeMethod);
		}else if("POST".equalsIgnoreCase(method) && !StringUtils.isBlank(contentType)){
			if(contentType.startsWith("application/x-www-form-urlencoded")){
				//使用 formdata 方式提交表单
				return parserFormDataParams(request,nativeMethod);
			}else if(contentType.startsWith("application/json")){
				//使用流方式提交表单
				String jsonString = IOUtils.toString(request.getInputStream(), "utf-8");
				return  JSONObject.parseObject(jsonString);
			}else if(contentType.startsWith("multipart/form-data")){
				ServletFileUpload servletFileUpload = new ServletFileUpload(factory);
				List<FileItem> parseRequest = servletFileUpload.parseRequest(request);
				JSONObject jsonObject = new JSONObject();
				Set<String> markTransfer = new HashSet<String>();			//标记需要转换成 String[] 的域 ,因为下面并不能确定元素数量 
				if(parseRequest != null && parseRequest.size() > 0){
					for (FileItem fileItem : parseRequest) {
						if(fileItem.isFormField()){
							//是普通表单对象
							String fieldName = fileItem.getFieldName();
							String value = fileItem.getString();
							if(jsonObject.containsKey(fieldName)){			//处理 checkbox 的情况
								if(!markTransfer.contains(fieldName)){
									markTransfer.add(fieldName);
								}
								Object object = jsonObject.get(fieldName);
								if(object  instanceof ArrayList){
									List<String> values = (ArrayList<String>) object;
									values.add(value);
								}else{
									//object 暂时还是 String 的情况
									String preValue = jsonObject.getString(fieldName);
									List<String> values = new ArrayList<String>();
									values.add(preValue);
									values.add(value);
									jsonObject.put(fieldName, values);			//覆盖之前的 String 的值
								}
							}else{
								jsonObject.put(fieldName, value);
							}
						}else{
							//文件上传表单域
							String fieldName = fileItem.getFieldName();
							long fileSize = fileItem.getSize();
							if(fileSize > this.maxUploadSize && this.maxUploadSize != 0){
								throw new IllegalArgumentException(fileItem.getName()+" 文件过大,限制大小:"+this.maxUploadSize+" B 当前文件大小:"+fileSize+" B");
							}
							if(jsonObject.containsKey(fieldName)){
								Object object = jsonObject.get(fieldName);
								if(object instanceof FileItem){
									FileItem item = (FileItem) object;
									List<FileItem> fileItems = new ArrayList<FileItem>();
									fileItems.add(item);
									fileItems.add(fileItem);
									jsonObject.put(fieldName, fileItems);
								}else{
									ArrayList<FileItem> fileItems = (ArrayList<FileItem>) object;
									fileItems.add(fileItem);
								}
							}else{
								jsonObject.put(fieldName, fileItem);
							}
						}
					}
					//将之前用 List<String> 选的 checkbox 选项转换成 String[]
					if(markTransfer != null && markTransfer.size() > 0){
						for (String fieldName : markTransfer) {
							List<String> valueList = (ArrayList<String>)jsonObject.get(fieldName);
							jsonObject.put(fieldName, valueList.toArray(new String[]{}));
						}
					}
				}
				return jsonObject;
			}
		}
		
		response.sendError(415, "不支持的媒体类型");
		throw new IllegalArgumentException("不支持的媒体类型 415");
	}

	/**
	 * 
	 * 作者:sanri <br/>
	 * 时间:2017-4-19下午4:41:56<br/>
	 * 功能:以 formdata 方式提交的表单 <br/>
	 * 入参: <br/>
	 * @param nativeMethod 
	 */
	private JSONObject parserFormDataParams(HttpServletRequest request, NativeMethod nativeMethod) {
		JSONObject jsonObject = new JSONObject();
		List<MethodParam> methodParams = nativeMethod.getMethodParams();
		if(methodParams != null && methodParams.size() > 0){
			for (MethodParam methodParam : methodParams) {
				String name = methodParam.getName();
				Class<?> paramClazz = methodParam.getParamClazz();
				if(paramClazz.isArray()){
					String[] parameterValues = request.getParameterValues(name);
					methodParam.setValue(parameterValues);
					jsonObject.put(name, parameterValues);
				}else{
					String parameter = request.getParameter(name);
					methodParam.setValue(parameter);
					jsonObject.put(name, parameter);
				}
			}
		}
		return jsonObject;
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException,IllegalArgumentException {
		processRequest(req,resp);
	}


	/**
	 * 
	 * 作者:sanri <br/>
	 * 时间:2017-4-17下午5:22:46<br/>
	 * 功能: 找到调用的类方法,注入参数,并实现调用,得到返回值<br/>
	 * 入参: <br/>
	 * @param resp 
	 * @param req 
	 * @throws IOException 
	 * @throws IllegalArgumentException 
	 * @throws FileUploadException 
	 * @throws ServletException 
	 */
	private InvokeResult invoke(String methodName, HttpServletRequest request, HttpServletResponse response) throws IllegalArgumentException, IOException, FileUploadException, ServletException {
		NativeMethod nativeMethod = mappings.get(methodName);
		if(nativeMethod == null){
			throw new ServletException("can not find method :"+methodName);		//add by sanri at 2017/04/24 解决找不到映射问题
		}
		//解析参数
		JSONObject jsonObject = parserParams(request, response,nativeMethod);
		Method method = nativeMethod.getMethod();
//		CtMethod ctMethod = mirror.getCtMethod();
		Class<?> callClass = nativeMethod.getClazz();
//		Class<?> returnType = mirror.getReturnType();
		Map<String,Object> paramsValues = new LinkedHashMap<String, Object>();
		List<MethodParam> methodParams = nativeMethod.getMethodParams();
		Writer writer = null;
		try {
			if(methodParams != null && !methodParams.isEmpty()){
				Iterator<MethodParam> iterator = methodParams.iterator();
				while(iterator.hasNext()){
					MethodParam methodParam = iterator.next();
					String paramName = methodParam.getName();
					Class<?> valueClazz = methodParam.getParamClazz();
					Object object = jsonObject.get(paramName);

					if(valueClazz == HttpServletRequest.class){
						paramsValues.put(paramName, request);
						continue;
					}else if(valueClazz == HttpServletResponse.class){
						paramsValues.put(paramName, response);
						continue;
					}else if(valueClazz == HttpSession.class){
						paramsValues.put(paramName, request.getSession());
						continue;
					}else if(valueClazz.isPrimitive()){
						String stringValue = ObjectUtils.toString(object);
						if(valueClazz == int.class){
							paramsValues.put(paramName, NumberUtil.toInt(stringValue,0));
						}else if(valueClazz == long.class){
							paramsValues.put(paramName,NumberUtil.toLong(stringValue,0));
						}else if(valueClazz == double.class){
							paramsValues.put(paramName,Double.parseDouble(stringValue));
						}else if(valueClazz == Float.class){
							paramsValues.put(paramName,Float.parseFloat(stringValue));
						}else if(valueClazz == Boolean.class){
							paramsValues.put(paramName,Boolean.parseBoolean(stringValue));
						}
						continue;
					}

					Object newInstance  = invokeParam(valueClazz,object);
					paramsValues.put(paramName,newInstance );
				}
			}
//			Object newInstance = callClass.newInstance();			//每次调用方法都是新对象,不存在线程安全问题
			Object newInstance = CLASS_INSTANCE.get(callClass.getSimpleName());
			if(newInstance == null){
				String reson = "can not find instance :"+callClass.getSimpleName();
				logger.error(reson);
				return InvokeResult.error(reson);
			}
			method.setAccessible(true);
			Object returnValue = null;
			if(paramsValues.values() != null && !paramsValues.values().isEmpty()){
				returnValue = method.invoke(newInstance,paramsValues.values().toArray());
			}else{
				returnValue = method.invoke(newInstance);
			}
			if(returnValue != null){
				response.setContentType("text/html;charset=UTF-8");
				writer = response.getWriter();
				writer.write(JSONObject.toJSONString(returnValue));
			}

			//如果返回为空,处理返回
			Class<?> returnType = method.getReturnType();
			if(returnType == void.class){
				response.setStatus(200);
			}

			InvokeResult invokeResult = new InvokeResult(method);
			invokeResult.setParamsValues(paramsValues);
			return invokeResult;
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			handleException(request, response, writer, e);
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e){
			Throwable targetException = e.getTargetException();
			handleException(request, response, writer, targetException);

			//有捕获的程序异常
			return InvokeResult.error(targetException.getMessage());
		}finally{
			if(writer != null){
				try {
					writer.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

		return InvokeResult.error("未捕获的程序异常");
	}

	/**
	 * 
	 * 作者:sanri <br/>
	 * 时间:2017-5-22下午3:31:08<br/>
	 * 功能: 处理可能的异常 <br/>
	 * @param request
	 * @param response
	 * @param writer
	 * @param e
	 * @param targetException
	 * @throws IOException
	 */
	private void handleException(HttpServletRequest request, HttpServletResponse response, Writer writer, Throwable targetException)
			throws IOException {
		if(handlerExceptionResolver != null){
			ModelAndView resolveException = handlerExceptionResolver.resolveException(request, response, targetException);
			if(resolveException != null){
				String url = resolveException.getUrl();
				Object params = resolveException.getParams();
				if(!StringUtils.isBlank(url)){
					if(params != null){
						//有数据的话,只能是转发
						request.setAttribute("params", params);
						try {
							request.getRequestDispatcher(url).forward(request, response);
						} catch (ServletException e1) {
							e1.printStackTrace();
						}
					}else{
						//其它情况重定向
						response.sendRedirect(url);
					}
				}else{
					if(params != null){
						//否则就是写 json 数据到前端
						if(writer == null){		// writer 有可能为空,避免空指针异常
							writer = response.getWriter();
						}
						writer.write(JSONObject.toJSONString(params));
					}
				}
			}
		}else{
			targetException.printStackTrace();
		}
	}

	/**
	 * 
	 * 作者:sanri <br/>
	 * 时间:2017-4-18下午3:49:47<br/>
	 * 功能:给参数注入值 <br/>
	 * 入参: <br/>
	 * @return 
	 * @throws IllegalAccessException 
	 * @throws InstantiationException 
	 */
	private Object invokeParam(Class<?> valueClazz, Object object) throws InstantiationException, IllegalAccessException {
		if(object == null || valueClazz == String.class || valueClazz.isPrimitive()){
			return object;
		}
		if(object.getClass().isArray()){			//处理 formdata 数据数组内容
			return object;
		}
		
		Object  javaObject = null;
		if(object instanceof JSONObject){
			javaObject = JSONObject.toJavaObject((JSONObject) object, valueClazz);
		}else if(object instanceof JSONArray){
			javaObject = JSONObject.toJavaObject((JSONArray)object, valueClazz);
		}else{
			//remove by sanri at 2017/04/23 对于普通参数不需要,对于其它对象,目前来看就文件对象了
//			javaObject = JSONObject.toJavaObject((JSONObject)JSONObject.toJSON(object), valueClazz);
			
			return object;
		}
		return javaObject;
	}

	/**
	 * 获取指定 servlet
	 * @param clazz
	 * @param <T>
	 * @return
	 */
	public  static <T> T getServlet(Class<T> clazz){
		String simpleName = clazz.getSimpleName();
		return clazz.cast(CLASS_INSTANCE.get(simpleName));
	}
}
