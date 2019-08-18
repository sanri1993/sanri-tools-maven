package com.sanri.frame;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import javax.annotation.PostConstruct;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.reflect.MethodUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.client.HttpClient;
import org.apache.http.client.utils.HttpClientUtils;

import com.sanri.app.servlet.PostmanServlet;
import com.sanri.app.filefetch.SSHService;
import sanri.utils.PathUtil;

/**
 * 
 * 作者:sanri <br/>
 * 时间:2017-7-26下午8:56:47<br/>
 * 功能:上下文监听器,当窗口销毁时,销毁创建的全局变量 <br/>
 * 当调试时点击 eclipse 结束 tomcat 并不是正常结束 ,不会调用 contextDestroyed ; 但关闭进程会关闭进程的所有资源,所以也会释放
 */
public class ContextLoaderListener implements ServletContextListener{

	private Log logger = LogFactory.getLog(ContextLoaderListener.class);
	ExecutorService executorService = Executors.newFixedThreadPool(10);
	
	@Override
	public void contextDestroyed(ServletContextEvent servletContextEvent) {
		//目前只要销毁 PostmanServlet 中的所有 httpclient,以后需要使用注释来批量释放
		Iterator<Entry<String, HttpClient>> httpClientIterator = PostmanServlet.clientMap.entrySet().iterator();
		while(httpClientIterator.hasNext()){
			Entry<String, HttpClient> httpClientEntry = httpClientIterator.next();
			String key = httpClientEntry.getKey();
			logger.info("释放 http 会话:"+key);
			HttpClient httpClient = httpClientEntry.getValue();
			HttpClientUtils.closeQuietly(httpClient);
		}

		//释放所有 linux 连接
		logger.info("释放 linux 连接:");
		SSHService.closeAll();
	}

	@Override
	public void contextInitialized(ServletContextEvent servletContextEvent) {
		String initExec = "com.sanri.initexec";
		String pkgPath = PathUtil.pkgPath(initExec);
		File pkgDir = new File(pkgPath);
		String[] fileNames = pkgDir.list();
		for (String fileName : fileNames) {
			String baseName = FilenameUtils.getBaseName(fileName);
			try {
				Class clazz = ClassUtils.getClass(initExec + "." + baseName);
				if(clazz.isAnonymousClass() || clazz.isMemberClass()){
					continue;
				}
				Object newInstance = clazz.newInstance();
				List<Method> methodsListWithAnnotation = MethodUtils.getMethodsListWithAnnotation(clazz, PostConstruct.class);
				for (Method method : methodsListWithAnnotation) {
					executorService.submit(()->{
						try {
							MethodUtils.invokeMethod(newInstance,method.getName());
						} catch (NoSuchMethodException e) {
							e.printStackTrace();
						} catch (IllegalAccessException e) {
							e.printStackTrace();
						} catch (InvocationTargetException e) {
							e.printStackTrace();
						}
					});

				}
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			} catch (InstantiationException e) {
				e.printStackTrace();
			}
		}

//		String quartzConfig = "/com/sanri/config/quartz.properties";
//		logger.info("定时任务初始化,加载属性配置文件:"+quartzConfig);
//		//初始化定时任务,加载属性文件,初始化
//		InputStream loadStream = PathUtil.loadStream(quartzConfig);
//		Properties properties = new Properties();
//		try {
//			properties.load(loadStream);
//			String value = properties.getProperty("org.quartz.threadPool.threadCount");
//			if(value == null || value.equals("")){
//				logger.info("获取propeties为空!");
//			    return;
//			}
//			SchedulerFactory sf = new StdSchedulerFactory(properties);
//			Scheduler sched  = sf.getScheduler();
//			sched.start();
//		} catch (IOException | SchedulerException e) {
//			e.printStackTrace();
//		}
	}

}
