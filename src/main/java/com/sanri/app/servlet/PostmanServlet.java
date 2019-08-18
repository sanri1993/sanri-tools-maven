package com.sanri.app.servlet;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.http.Consts;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.ParseException;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.HttpClientUtils;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import sanri.utils.HttpUtil;
import sanri.utils.RandomUtil;
import sanri.utils.RegexValidate;
import sanri.utils.image.QRCodeUtil;
import sanri.utils.image.VerifyCodeUtil;

import com.alibaba.fastjson.JSONObject;
import com.sanri.app.BaseServlet;
import com.sanri.app.postman.MsgEntity;
import com.sanri.frame.RequestMapping;

/**
 * 
 * 作者:sanri <br/>
 * 时间:2017-7-26下午5:39:22<br/>
 * 功能:请求服务 <br/>
 */
@RequestMapping("/postman")
public class PostmanServlet extends BaseServlet {
	// sessionId <==> client,一个浏览器会对应一个 httpclient 管理
	public final static Map<String,HttpClient> clientMap = new HashMap<String, HttpClient>();
	
	/**
	 * 
	 * 功能:验证码示例<br/>
	 * 创建时间:2017-8-5下午8:53:00<br/>
	 * 作者：sanri<br/><br/>
	 * @throws IOException 
	 */
	@RequestMapping("/example/verifyCode")
	public void exampleVerifyCode(HttpServletResponse response,HttpSession session) throws IOException{
		String randomCode = RandomUtil.random(4, "ABCDEFGHJKLMNPQRSTUVWXYZ23456789");
		session.setAttribute("verifyCode", randomCode);
		BufferedImage generateImage = VerifyCodeUtil.generateImage(85, 30,randomCode);
		VerifyCodeUtil.writeImage(generateImage, response.getOutputStream());
	}
	
	/**
	 * 
	 * 功能:生成二维码<br/>
	 * 创建时间:2017-8-13上午10:34:45<br/>
	 * 作者：sanri<br/>
	 * @param text
	 * @param response
	 * @param session<br/>
	 * @throws IOException 
	 */
	@RequestMapping("/example/qrcode")
	public void exampleQRCode(String text,HttpServletResponse response,HttpSession session) throws IOException{
		BufferedImage generateImage = QRCodeUtil.generateImage(text, 250, 250);
		QRCodeUtil.writeImage(generateImage, response.getOutputStream());
	}
	
	@RequestMapping("/example/login")
	public Map<String,String> exampleLogin(String userName,String passWord,String checkCode,HttpSession session){
		Map<String,String> msg = new HashMap<String,String>();
		if("admin".equals(userName) && "password".equals(passWord)){
			String verifyCode = ObjectUtils.toString(session.getAttribute("verifyCode"));
			if(verifyCode.equals(checkCode)){
				msg.put("errorCode", "0");
				return msg;
			}
			msg.put("errorCode", "2");
			msg.put("msg", "验证码错误");
			return msg;
		}
		msg.put("errorCode", "1");
		msg.put("msg", "用户名密码错误");
		return msg;
	}
	
	/**
	 * 
	 * 作者:sanri <br/>
	 * 时间:2017-8-7下午1:09:50<br/>
	 * 功能: 请求数据<br/>
	 * @param url 请求全路径
	 * @param method 方式,支持 GET/POST
	 * @param msgEntity 消息实体
	 * 注:请求数据类型和返回数据类型是一致的
	 * @return
	 */
	public MsgEntity request(String url,String method,MsgEntity msgEntity,HttpSession session){
		String contentType = msgEntity.getContentType();
		MsgEntity retMsgEntity = new MsgEntity(contentType);
		HttpClient sessionHttpClient = findSessionHttpClient(session);
		HttpResponse httpResponse = null;
		try {
			if(StringUtils.isBlank(contentType)){
				List<NameValuePair> parseKeyValueBody = parseKeyValueBody(msgEntity.getBody());
				httpResponse = requestGet(url,parseKeyValueBody,sessionHttpClient);
			}else{
				if(contentType.startsWith("application/x-www-form-urlencoded")){
					if("GET".equalsIgnoreCase(method)){
						List<NameValuePair> parseKeyValueBody = parseKeyValueBody(msgEntity.getBody());
						httpResponse = requestGet(url, parseKeyValueBody, sessionHttpClient);
					}else{
						httpResponse = requestPost(url,msgEntity,sessionHttpClient);
					}
				}else{
					httpResponse = requestPost(url,msgEntity,sessionHttpClient);
				}
			}
			StatusLine statusLine = httpResponse.getStatusLine();
			retMsgEntity.setStatusCode(statusLine.getStatusCode());
			if(statusLine.getStatusCode() == 200){
				HttpEntity entity = httpResponse.getEntity();
				//如果请求成功,解析返回值,直接返回原始数据,注: 无法处理流数据 
				retMsgEntity.setBody(EntityUtils.toString(entity));
			}
		} catch (ParseException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally{
			HttpClientUtils.closeQuietly(httpResponse);
		}
		return retMsgEntity;
	}
	
	/**
	 * 
	 * 作者:sanri <br/>
	 * 时间:2017-8-7下午1:46:48<br/>
	 * 功能:登录指定系统请求 <br/>
	 * @param url
	 * @param msgEntity
	 * @param session
	 * @return
	 */
	public MsgEntity login(String url,MsgEntity msgEntity,HttpSession session){
		msgEntity.setContentType("application/json");
		return request(url, "POST", msgEntity, session);
	}
	
	/**
	 * 
	 * 作者:sanri <br/>
	 * 时间:2017-7-26下午9:25:01<br/>
	 * 功能:获取验证码 <br/>
	 * @param url 验证码路径
	 */
	public void verifyCode(String url,HttpSession session,HttpServletResponse response){
		HttpClient httpClient = findSessionHttpClient(session);
		HttpGet getMethod = new HttpGet(url);
		HttpResponse httpResponse = null;
		ServletOutputStream outputStream = null;
		try {
			httpResponse = httpClient.execute(getMethod);
			HttpEntity entity = httpResponse.getEntity();
			InputStream contentInputStream = entity.getContent();
			outputStream = response.getOutputStream();
			IOUtils.copy(contentInputStream, outputStream);
		} catch (IllegalStateException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally{
			IOUtils.closeQuietly(outputStream);
			HttpClientUtils.closeQuietly(httpResponse);
		}
	}
	
	/*
	 * 查找当前请求浏览器的 httpclient
	 */
	private HttpClient findSessionHttpClient(HttpSession session){
		String id = session.getId();
		HttpClient httpClient = clientMap.get(id);
		if(httpClient == null){
			httpClient = HttpClients.createDefault();
			clientMap.put(id, httpClient);
		}
		return httpClient;
	}

	/**
	 * @作者: sanri
	 * @时间: 2017/8/3 18:18
	 * @功能: 解析 keyvalue 形式的参数 body
	 * @param body
	 */
	private List<NameValuePair> parseKeyValueBody(String body) {
		List<NameValuePair> params = new ArrayList<NameValuePair>();
		if(StringUtils.isNotBlank(body)){
			String[] keyvalueParamsArray = body.split("&");
			if(!RegexValidate.isEmpty(keyvalueParamsArray)){
				for (String keyValue : keyvalueParamsArray) {
					if(StringUtils.isBlank(keyValue) || !keyValue.contains("=")){
						continue;
					}
					String[] kv = keyValue.split("=");
					params.add(new BasicNameValuePair(kv[0],kv[1]));
				}
			}
		}
		return params;
	}

	/**
	 * 
	 * 作者:sanri <br/>
	 * 时间:2017-8-7下午1:35:56<br/>
	 * 功能:post 请求 <br/>
	 * @param url
	 * @param msgEntity
	 * @param sessionHttpClient
	 * @return 
	 * @throws IOException 
	 * @throws ClientProtocolException 
	 */
	@SuppressWarnings("unchecked")
	private HttpResponse requestPost(String url, MsgEntity msgEntity, HttpClient sessionHttpClient) throws ClientProtocolException, IOException {
		String contentType = msgEntity.getContentType();
		HttpPost httpPost = new HttpPost(url);
		String body = msgEntity.getBody();
		if(contentType.startsWith("text/xml")){
			StringEntity stringEntity = new StringEntity(body, ContentType.create("text/xml",Consts.UTF_8));
			httpPost.setEntity(stringEntity);
		}else if(contentType.startsWith("application/x-www-form-urlencoded")){
			//键值对数据的处理
			List<NameValuePair> parseKeyValueBody = parseKeyValueBody(body);
			httpPost.setEntity(new UrlEncodedFormEntity(parseKeyValueBody));
		}else{
			//其它肯定是 application/json
			if(StringUtils.isNotBlank(body)){
				HashMap<String,String> mapData = JSONObject.parseObject(body, HashMap.class);
				List<NameValuePair> transferParam = HttpUtil.transferParam(mapData);
				httpPost.setEntity(new UrlEncodedFormEntity(transferParam));
			}
		}
		HttpResponse httpResponse = sessionHttpClient.execute(httpPost);
		return httpResponse;
	}

	/**
	 * 
	 * 作者:sanri <br/>
	 * 时间:2017-8-7下午1:24:56<br/>
	 * 功能:向指定路径发送 get 请求 <br/>
	 * @param url
	 * @param msgEntity
	 * @param sessionHttpClient
	 * 解析请求参数,只会以键值对方式解析
	 * @return 
	 * @throws IOException 
	 * @throws ParseException 
	 */
	private HttpResponse requestGet(String url, List<NameValuePair> nameValuePairs, HttpClient sessionHttpClient) throws ParseException, IOException {
		UrlEncodedFormEntity urlEncodedFormEntity = null;
		if(nameValuePairs != null){
			urlEncodedFormEntity = new UrlEncodedFormEntity(nameValuePairs,Consts.UTF_8);
		}
		if(urlEncodedFormEntity != null){
            url+="?"+EntityUtils.toString(urlEncodedFormEntity,Consts.UTF_8);
        }
        HttpGet httpGet = new HttpGet(url);
		HttpResponse httpResponse = sessionHttpClient.execute(httpGet);
		return httpResponse;
	}
}
