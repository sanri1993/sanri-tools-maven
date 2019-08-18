package com.sanri.wechat;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import sanri.utils.HttpUtil;

import com.alibaba.fastjson.JSONObject;
import com.sanri.wechat.exception.WXException;

/**
 * 
 * 作者:sanri <br/>
 * 时间:2017-8-22下午2:31:00<br/>
 * 功能:access_token 刷新任务 <br/>
 */
public class RefreshAccessTokenThread implements Runnable{

	private Log logger = LogFactory.getLog(getClass());
	//获取到的 access_token
	private static String accessToken = "";
	
	@Override
	public void run() {
		Map<String,String> param = new HashMap<String, String>();
		param.put("grant_type", "client_credential");
		//我的公众号
		param.put("appid", WXDispatchServlet.propertiesConfiguration.getString("appid"));
		param.put("secret", WXDispatchServlet.propertiesConfiguration.getString("secret"));
		try {
			String jsonString = HttpUtil.get(WXDispatchServlet.propertiesConfiguration.getString("access_token_url"), param);
			logger.debug("刷新 accessToken 返回数据为:"+jsonString);
			JSONObject parseObject = JSONObject.parseObject(jsonString);
			accessToken = parseObject.getString("access_token");
			logger.debug("获取到 accessToken 数据为 :"+accessToken);
			if(StringUtils.isBlank(accessToken)){
				String errcode = parseObject.getString("errcode");
				String errmsg = parseObject.getString("errmsg");
				throw new WXException("获取 access_token 失败 ",errcode, errmsg);
			}
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static String getAccessToken() {
		return accessToken;
	}

}
