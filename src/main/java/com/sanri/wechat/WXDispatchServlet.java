package com.sanri.wechat;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.Consts;

import sanri.utils.PathUtil;
import sanri.utils.SignUtil;

import com.sanri.wechat.handler.MessageHandler;
import com.sanri.wechat.messages.TextMessage;
import com.sanri.wechat.messages.WechatMessage;
import com.sanri.wechat.utils.MessageUtil;

/**
 * 
 * 作者:sanri <br/>
 * 时间:2017-8-25下午1:30:44<br/>
 * 功能:微信消息派发器
 * 对于微信来说,get 请求只有在验证签名的时候调用<br/>
 * post 请求在粉丝向公众号发送请求的时候调用我们服务器指定的 url ,内容格式是固定的(xml 格式,有个消息类型字段)
 * 初始化后需要启动一个定时器,每隔一段时间向微信服务器请求 access_token 值,因为这个值每 2 小时失效 <br/>
 */
public class WXDispatchServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
	// 微信 token ,用于生成签名
	private static String TOKEN;
	private static Log logger = LogFactory.getLog(WXDispatchServlet.class);
	static PropertiesConfiguration propertiesConfiguration;
	
	//初始创建 10 个线程的线程池,所有线程往线程池中提交
	protected static ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1);
	
	private final static Map<String,MessageHandler> MESSAGE_HANDLER_MAP = new HashMap<String, MessageHandler>();

	/**
	 * 微信 get 请求
	 * get 请求只做签名验证
	 * 流程:使用配置的 token,微信方提供的 timestamp,nonce生成签名(字典序排序,然后使用 sha 加密 )
	 * 如果签名和微信方生的签名相同,则接入成功,返回微信方给出的随机字符串,否则 返回空
	 */
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String signature = request.getParameter("signature"); // 微信方生成的签名

		String timestamp = request.getParameter("timestamp"); // 时间戳
		String nonce = request.getParameter("nonce"); // 随机数
		String echostr = request.getParameter("echostr"); // 随机字符串

		PrintWriter writer = response.getWriter();
		// 验证过程
		String sysSignture = SignUtil.signature(TOKEN, timestamp, nonce);
		logger.debug("生成的签名为 :" + sysSignture + ",传入的签名为:" + signature);
		if (StringUtils.isNotBlank(sysSignture) && sysSignture.equals(signature)) {
			// 如果签名成功返回给过来的随机字符串
			writer.write(echostr);
		} else {
			writer.write("");
		}
		writer.flush();
		writer.close();
	}
	
	@Override
	public void init() throws ServletException {
		//加载配置
		String configPath = PathUtil.pkgPath("com.sanri.config");
		File configFile = new File(configPath+"/wechat.properties");
		InputStreamReader inputStreamReader  = null;
		try {
			propertiesConfiguration = new PropertiesConfiguration(configFile);
			TOKEN = propertiesConfiguration.getString("token");
			//加载各种消息的处理器
			Properties properties = new Properties();
			inputStreamReader = new InputStreamReader(new FileInputStream(configFile),Consts.UTF_8);
			properties.load(inputStreamReader);
			Iterator<Entry<Object, Object>> iterator = properties.entrySet().iterator();
			while(iterator.hasNext()){
				Entry<Object, Object> kvEntry = iterator.next();
				String key = ObjectUtils.toString(kvEntry.getKey());
				String value =ObjectUtils.toString(kvEntry.getValue());
				if(key.startsWith("handler")){
					//对于以 handler 开头的 key ,认为是消息处理器
					String handler = key.split("\\.")[1];
					Class<?> forName = Class.forName(value);
					Object newInstance = forName.newInstance();
					if(newInstance instanceof MessageHandler){
						MESSAGE_HANDLER_MAP.put(handler, (MessageHandler)newInstance);
					}else{
						throw new Exception(handler+" 消息处理器必须实现 com.sanri.wechat.handler.MessageHandler 接口");
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			throw new ServletException("配置错误");
		} finally{
			IOUtils.closeQuietly(inputStreamReader);
		}
		//执行获取 access_token 任务
		Runnable refreshTask = new RefreshAccessTokenThread();
		//一秒后执行,然后等任务执行完后的 1 个小时 50 分钟后执行
//		executorService.scheduleWithFixedDelay(refreshTask, 1000, 219000000, TimeUnit.MILLISECONDS);
	}

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// 调用核心业务类接收消息、处理消息
		String respMessage = processRequest(request);
		logger.debug("响应的消息为:"+respMessage);
		// 响应消息
		ServletOutputStream outputStream = response.getOutputStream();
		outputStream.write(respMessage.getBytes(Consts.UTF_8));
	}

	/**
	 * 
	 * 作者:sanri <br/>
	 * 时间:2017-8-25下午1:31:04<br/>
	 * 功能:请求处理过程 <br/>
	 * @param request
	 * @return
	 */
	private String processRequest(HttpServletRequest request) {
		String xmlResp = "";
		try {
			// xml请求解析
			Map<String, String> requestMap = MessageUtil.parseXml(request);
			// 开发者微信号
			String toUserName = requestMap.get("ToUserName");

			// 发送方帐号（一个OpenID）
			String fromUserName = requestMap.get("FromUserName");
			// 消息类型
			String msgType = requestMap.get("MsgType");
			
			//获取处理器
			MessageHandler messageHandler = MESSAGE_HANDLER_MAP.get(msgType);
			if(messageHandler == null){
				TextMessage textMessage = new TextMessage();
				textMessage.populate(fromUserName, toUserName);
				textMessage.setContent("此消息类型暂时不支持:"+msgType);
				return textMessage.toXML();
			}

			// 消息处理
			WechatMessage wechatMessage = messageHandler.execute(requestMap);
			wechatMessage.populate(fromUserName, toUserName);
			wechatMessage.setMsgType(msgType);			//写入原来的消息类型
			return wechatMessage.toXML();				//返回 xml 数据
		} catch (Exception e) {
			e.printStackTrace();
		}
		return xmlResp;
	}

}
