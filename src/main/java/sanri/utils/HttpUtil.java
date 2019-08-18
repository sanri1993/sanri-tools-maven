package sanri.utils;

import com.alibaba.fastjson.JSONObject;
import com.sanri.app.wscall.WsdlPort.SOAPType;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.Consts;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.NoHttpResponseException;
import org.apache.http.ParseException;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.client.utils.HttpClientUtils;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.config.SocketConfig;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.LayeredConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLContexts;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLHandshakeException;
import javax.xml.ws.http.HTTPException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class HttpUtil {

    private static final Log logger = LogFactory.getLog(HttpUtil.class);

    static final int CONNECTION_TIMEOUT = 10000; // 设置请求超时 10 秒钟 根据业务调整
    static final int SOCKET_TIMEOUT = 180000; // 数据传输时间 3 分钟
    static final int SEARCH_CONNECTION_TIMEOUT = 500; // 连接不够用的时候等待超时时间,不设置默认等于 CONNECTION_TIMEOUT

    public static final ContentType XML_UTF8 = ContentType.create("text/xml", "utf-8");
    public static final ContentType XML_GBK = ContentType.create("text/xml","gbk");
    public static final ContentType JSON_UTF8 = ContentType.create("application/json", "utf-8");
    public static final ContentType MULTIPART_FORM_DATA  =  ContentType.MULTIPART_FORM_DATA.withCharset("utf-8");
    public static ContentType SOAP12_UTF8 = ContentType.create("application/xml+soap","utf-8");

    static PoolingHttpClientConnectionManager httpClientConnectionManager = null;
    static class AllHostNameTrustStrategy implements TrustStrategy{
        @Override
        public boolean isTrusted(X509Certificate[] chain, String authType) throws CertificateException {
            return true;
        }
    }
    static final AllHostNameTrustStrategy allHostNameTrustStrategy = new AllHostNameTrustStrategy();

    static {
        LayeredConnectionSocketFactory layeredConnectionSocketFactory = null;
        try {
            KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            SSLContext sslContext = SSLContexts.custom().loadTrustMaterial(keyStore, allHostNameTrustStrategy).build();
            layeredConnectionSocketFactory = new SSLConnectionSocketFactory(sslContext,SSLConnectionSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (KeyStoreException e) {
            e.printStackTrace();
        } catch (KeyManagementException e) {
            e.printStackTrace();
        }

        Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder.<ConnectionSocketFactory> create()
                .register("https", layeredConnectionSocketFactory)
                .register("http", new PlainConnectionSocketFactory()).build();

        httpClientConnectionManager = new PoolingHttpClientConnectionManager(socketFactoryRegistry);
        httpClientConnectionManager.setMaxTotal(200); // 总的连接池的大小
        httpClientConnectionManager.setDefaultMaxPerRoute(20); // 对每个主机的最大连接大小,每个主机最大20 个连接,总共 200 个连接,支持 10 台主机
    }

    public static CloseableHttpClient getHttpClient() {
        RequestConfig requestConfig = RequestConfig.custom().setConnectTimeout(CONNECTION_TIMEOUT).setSocketTimeout(SOCKET_TIMEOUT)
                .setConnectionRequestTimeout(SEARCH_CONNECTION_TIMEOUT).build();

        SocketConfig socketConfig = SocketConfig.custom()
                .setSoKeepAlive(true)
                .setTcpNoDelay(true).build();

        // 请求重试处理
        HttpRequestRetryHandler httpRequestRetryHandler = new HttpRequestRetryHandler() {
            public boolean retryRequest(IOException exception, int executionCount, HttpContext context) {
                if (executionCount >= 3) {// 如果已经重试了 3 次，就放弃
                    return false;
                }
                if (exception instanceof NoHttpResponseException) {// 如果服务器丢掉了连接，那么就重试
                    return true;
                }
                if (exception instanceof SSLHandshakeException) {// 不要重试SSL握手异常
                    return false;
                }
                if (exception instanceof InterruptedIOException) {// 超时
                    return false;
                }
                if (exception instanceof UnknownHostException) {// 目标服务器不可达
                    return false;
                }
                if (exception instanceof ConnectTimeoutException) {// 连接被拒绝
                    return false;
                }
                if (exception instanceof SSLException) {// SSL握手异常
                    return false;
                }

                HttpClientContext clientContext = HttpClientContext.adapt(context);
                HttpRequest request = clientContext.getRequest();
                // 如果请求是幂等的，就再次尝试
                if (!(request instanceof HttpEntityEnclosingRequest)) {
                    return true;
                }
                return false;
            }
        };

        CloseableHttpClient httpClient = HttpClients.custom().setConnectionManager(httpClientConnectionManager)
                .setDefaultRequestConfig(requestConfig)
                .setRetryHandler(httpRequestRetryHandler)
                .setDefaultSocketConfig(socketConfig).build();

        return httpClient;
    }

    /**
     *
     * 作者:sanri <br/>
     * 时间:2017-7-26下午3:47:02<br/>
     * 功能:将 map 型 的参数转换为NameValuePair 类型  <br/>
     * @param params
     * 注:日期将会转格式为  yyyy-MM-dd
     * @return
     */
    public static List<NameValuePair> transferParam(Map<String, ? extends Object> params) {
        return transferParam(params,"yyyy-MM-dd");
    }
    public static List<NameValuePair> transferParam(Map<String, ? extends Object> params,String dateFormat) {
        List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
        if(params != null && !params.isEmpty()){
            Set<?> entrySet = params.entrySet();
            Iterator<?> paramIterator = entrySet.iterator();
            while(paramIterator.hasNext()){
                Map.Entry<String, ? extends Object> param = (Map.Entry<String, ? extends Object>) paramIterator.next();
                Object value = param.getValue();
                if(value == null){
                    nameValuePairs.add(new BasicNameValuePair(param.getKey(), null));
                }else{
                    if(value instanceof Date ){
                        Date date = (Date) value;
                        String dateString = "";
                        if(StringUtils.isBlank(dateFormat)){
                            dateString = date.toString();
                        }else{
//							dateString = DateUtil.formatDate(date,dateFormat);
                            dateString = DateFormatUtils.format(date, dateFormat);
                        }
                        nameValuePairs.add(new BasicNameValuePair(param.getKey(),dateString ));
                        continue;
                    }
                    nameValuePairs.add(new BasicNameValuePair(param.getKey(), ObjectUtils.toString(value)));
                }
            }
        }
        return nameValuePairs;
    }

    /**
	 * 
	 * 作者:sanri <br/>
	 * 时间:2017-7-26下午3:36:40<br/>
	 * 功能:兼容以前的功能，传入路径和参数，返回字符串的返回结果 <br/>
	 * @param url
	 * @param params
	 * @return
	 * @throws IOException 
	 */
	public static String get(String url,Map<String,String> params) throws IOException,IllegalArgumentException{
		CloseableHttpClient httpClient = getHttpClient();
		List<NameValuePair> nameValuePairs = transferParam(params);
		HttpGet getMethod = null;
		if(!RegexValidate.isEmpty(nameValuePairs)){
			HttpEntity urlEncodedFormEntity = new UrlEncodedFormEntity(nameValuePairs,Consts.UTF_8);
			String keyValueParams = EntityUtils.toString(urlEncodedFormEntity ,Consts.UTF_8);
			getMethod = new HttpGet(url+"?"+keyValueParams);
		}else{
			getMethod = new HttpGet(url);
		}
		getMethod.addHeader("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/70.0.3538.102 Safari/537.36");
		//输出url 参数日期
		logger.debug("请求数据为:"+params);
		HttpResponse response  = null;
		try {
			response = httpClient.execute(getMethod);
			HttpEntity msgEntity = response.getEntity();
			String message = EntityUtils.toString(msgEntity);
			return message;
		} catch (ParseException e) {
			e.printStackTrace();
		} finally{
			HttpClientUtils.closeQuietly(response);
		}
		return "";
	}

	/**
     *
     * 作者:sanri <br/>
     * 时间:2018-3-28下午8:57:50<br/>
     * 功能: 请求地址获取数据<br/>
     * @param url
     * @param urlEncoded
     * @return
     * @throws IOException
     * @throws ParseException
     */
    public static String getData(final String url,final String urlEncoded) throws IOException{
        HttpClient httpClient = getHttpClient();

        String queryUrl = url;
        if(StringUtils.isNotBlank(urlEncoded)){
            queryUrl+= "?"+urlEncoded;
        }

        logger.debug("请求地址:"+queryUrl);
        HttpGet httpGet = new HttpGet(queryUrl);
        httpGet.addHeader("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/70.0.3538.102 Safari/537.36");
        HttpResponse response  = null;
        try {
            response = httpClient.execute(httpGet);
            StatusLine statusLine = response.getStatusLine();
            int statusCode = statusLine.getStatusCode();
            if(statusCode != HttpStatus.SC_OK){
                //解决地址重定向问题
                if(statusCode == HttpStatus.SC_MOVED_TEMPORARILY){
                    Header firstHeader = response.getFirstHeader("location");
                    String location = firstHeader.getValue();
                    return getData(location, urlEncoded);
                }

                HTTPException exception = new HTTPException(statusCode);
                logger.error("请求地址"+url+"出错,http 状态码为:"+statusCode,exception);
                throw exception;
            }

            HttpEntity msgEntity = response.getEntity();
            String message = EntityUtils.toString(msgEntity);
            return message;
        } catch (ClientProtocolException e) {
            throw new IllegalArgumentException("客户端协议错误 ，检查 url 配置 url: "+url,e);
        }finally{
            HttpClientUtils.closeQuietly(response);
            
        }

    }

    /**
     *
     * 作者:sanri <br/>
     * 时间:2017-7-26下午3:36:40<br/>
     * 功能:兼容以前的功能，传入路径和参数，返回字符串的返回结果 <br/>
     * @param url
     * @param params
     * @return
     * @throws IOException
     */
    public static String getData(String url,Map<String,String> params,Charset charset) throws IOException{
        List<NameValuePair> nameValuePairs = transferParam(params);
        HttpEntity urlEncodedFormEntity = new UrlEncodedFormEntity(nameValuePairs,charset);
        String urlEncoded = EntityUtils.toString(urlEncodedFormEntity ,charset);
        String data = getData(url, urlEncoded);
        return data;
    }

    /**
     *
     * 作者:sanri <br/>
     * 时间:2017-11-7下午6:24:30<br/>
     * 功能:调用地址获取 json 数据 <br/>
     * @param url
     * @param params
     * @return
     * @throws IllegalArgumentException
     * @throws IOException
     */
    public static JSONObject getJSON(String url,Map<String,String> params) throws  IOException{
        return getJSON(url, params, Consts.UTF_8);
    }
    public static JSONObject getJSON(String url,Map<String,String> params,Charset charset) throws  IOException{
        String retData = getData(url, params, charset);
        if(StringUtils.isNotBlank(retData)){
            return JSONObject.parseObject(retData);
        }
        return new JSONObject();
    }

    /**
	 *
	 * 作者:sanri <br/>
	 * 时间:2017-10-19下午2:25:40<br/>
	 * 功能:向指定 url 提交数据 <br/>
	 * @param url
	 * @param data
	 * @param contentType
	 * @return
	 * @throws IOException
	 */
	public static String postData(String url,String data,ContentType contentType) throws IOException,HTTPException {
	    HttpClient httpClient = getHttpClient();
	
	    //请求头,请求体数据封装
	    HttpPost postMethod = new HttpPost(url);
	    if(contentType != null){
	        postMethod.addHeader("Content-Type", contentType.toString());
	    }
	    postMethod.addHeader("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/70.0.3538.102 Safari/537.36");
	    StringEntity dataEntity = new StringEntity(data, contentType);
	    postMethod.setEntity(dataEntity);
	    HttpResponse response = null;
	
	    try {
	        //开始请求,记录请求数据和请求时间
	        response = httpClient.execute(postMethod);
	
	        //获取响应
	        HttpEntity entity = response.getEntity();
	        StatusLine statusLine = response.getStatusLine();
	        int statusCode = statusLine.getStatusCode();
	        if(statusCode != 200){
	            //解决地址重定向问题
	            if(statusCode == 302){
	                Header firstHeader = response.getFirstHeader("location");
	                String location = firstHeader.getValue();
	                return postData(location, data, contentType);
	            }
	
	            HTTPException exception = new HTTPException(statusCode);
	            logger.error("请求地址"+url+"出错,http 状态码为:"+statusCode,exception);
	            throw exception;
	        }
	
	        String retData = EntityUtils.toString(entity,contentType.getCharset());
	        return retData;
	    } catch (ClientProtocolException e) {
	        throw new IllegalArgumentException("客户端协议错误 ，检查 url 配置 url: "+url,e);
	    } finally{
	        HttpClientUtils.closeQuietly(response);
	    }
	}

	/**
     *
     * 作者:sanri <br/>
     * 时间:2017-7-26下午3:36:40<br/>
     * 功能:提交表单数据,返回字符串的返回结果 <br/>
     * @param url
     * @param params
     * @return
     * @throws IOException
     */
    public static String postFormData(String url,Map<String,String> params) throws IOException{
        return postFormData(url, params, Consts.UTF_8);
    }
    public static String postFormData(String url,Map<String,String> params,Charset charset) throws IOException{
        CloseableHttpClient httpClient = getHttpClient();
        List<NameValuePair> nameValuePairs = transferParam(params);
        HttpPost postMethod = new HttpPost(url);
        if(CollectionUtils.isNotEmpty(nameValuePairs)){
            HttpEntity entity = new UrlEncodedFormEntity(nameValuePairs,charset);
            postMethod.setEntity(entity );
        }
        postMethod.addHeader("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/61.0.3163.100 Safari/537.36");
        HttpResponse response  = null;
        try {
            response = httpClient.execute(postMethod);
            StatusLine statusLine = response.getStatusLine();
            int statusCode = statusLine.getStatusCode();
            if(statusCode != 200){
                //解决地址重定向问题
                if(statusCode == 302){
                    Header firstHeader = response.getFirstHeader("location");
                    String location = firstHeader.getValue();
                    return postFormData(location,params,charset);
                }

                HTTPException exception = new HTTPException(statusCode);
                logger.error("请求地址"+url+"出错,http 状态码为:"+statusCode,exception);
                throw exception;
            }
            HttpEntity msgEntity = response.getEntity();
            return  EntityUtils.toString(msgEntity,charset);
        } finally{
            HttpClientUtils.closeQuietly(response);
        }
    }

    /**
     *
     * 作者:sanri <br/>
     * 时间:2017-7-26下午5:59:57<br/>
     * 功能:向路径提交 xml 信息 <br/>
     * @param url
     * @param xml
     * @return
     * @throws IOException
     * @throws IllegalArgumentException
     */
    public static String postXml(String url,String xml) throws IllegalArgumentException, IOException{return postXml(url, xml, XML_UTF8);}
    public static String postXml(String url,String xml,ContentType contentType) throws IOException,IllegalArgumentException{
        return postData(url, xml,contentType);
    }

    /**
     *
     * 作者:sanri <br/>
     * 时间:2017-10-19下午2:23:53<br/>
     * 功能:json 请求 <br/>
     * @param url
     * @param jsonObject
     * @return
     * @throws IOException
     */
    public static JSONObject postJSON(String url,JSONObject jsonObject) throws IOException{return postJSON(url, jsonObject,JSON_UTF8);}
    public static JSONObject postJSON(String url,JSONObject jsonObject,ContentType contentType) throws IOException{
        String postData = jsonObject.toJSONString();
        String retJsonData = postData(url,postData,contentType);
        if(StringUtils.isNotBlank(retJsonData)){
            return JSONObject.parseObject(retJsonData);
        }
        return null;
    }

    /**
     *
     * 作者:sanri <br/>
     * 时间:2018-3-28下午8:58:38<br/>
     * 功能:获取流 <br/>
     * @param url
     * @param urlEncoded
     * 注: 调用此方法需要关流
     * @return
     * @throws IOException
     */
    public static InputStream getStream(String url,String urlEncoded) throws IOException{
        CloseableHttpClient httpClient = getHttpClient();

        String queryUrl = url;
        if(StringUtils.isNotBlank(urlEncoded)){
            queryUrl+= "?"+urlEncoded;
        }

        HttpGet httpGet = new HttpGet(queryUrl);
        httpGet.addHeader("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/70.0.3538.102 Safari/537.36");
        CloseableHttpResponse response  = null;
        try {
            response = httpClient.execute(httpGet);
            StatusLine statusLine = response.getStatusLine();
            int statusCode = statusLine.getStatusCode();
            if(statusCode != HttpStatus.SC_OK){
                //解决地址重定向问题
                if(statusCode == HttpStatus.SC_MOVED_TEMPORARILY){
                    Header firstHeader = response.getFirstHeader("location");
                    String location = firstHeader.getValue();
                    return getStream(location,urlEncoded);
                }

                HTTPException exception = new HTTPException(statusCode);
                logger.error("请求地址"+url+"出错,http 状态码为:"+statusCode,exception);
                throw exception;
            }

            HttpEntity entity = response.getEntity();

            return entity.getContent();
        } catch (ClientProtocolException e) {
            throw new IllegalArgumentException("客户端协议错误 ，检查 url 配置 url: "+url,e);
        }finally{
            if(response != null){
                response.close();
            }
        }
    }
    
    /**
	 * 
	 * 作者:sanri <br/>
	 * 时间:2017-7-26下午4:37:25<br/>
	 * 功能:发送 soap 消息  <br/>
	 * @param url 请求路径 
	 * @param rawString xml 字符串
	 * @param soapType soap 协议类型
	 * @param soapAction soapAction
	 * @return
	 * @throws IOException
	 * @throws IllegalArgumentException
	 */
	public static String postSoap(String url,String rawString,SOAPType soapType,String soapAction)throws IOException,IllegalArgumentException{
		CloseableHttpClient httpClient = getHttpClient();
		HttpPost postMethod = new HttpPost(url);
		postMethod.addHeader("Content-Type", "text/xml; charset=utf-8");
		postMethod.addHeader("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/70.0.3538.102 Safari/537.36");
		postMethod.addHeader("SOAPAction", soapAction);
		//获取请求内容 
		HttpEntity xmlEntity = null;
		if(soapType == SOAPType.SOAP11){
			xmlEntity = new StringEntity(rawString,XML_UTF8);
		}else if(soapType == SOAPType.SOAP12){
			xmlEntity = new StringEntity(rawString,SOAP12_UTF8);
		}else{
			throw new IllegalArgumentException("不支持的　soap 类型 "+soapType);
		}
		postMethod.setEntity(xmlEntity);
		HttpResponse response = null;
		long startTime = System.currentTimeMillis();
		try {
			response = httpClient.execute(postMethod);
			logger.info("请求:"+url+" 所花时间 :"+(System.currentTimeMillis() - startTime));
			HttpEntity entity = response.getEntity();
			return EntityUtils.toString(entity,Consts.UTF_8);
		} catch (ClientProtocolException e) {
			e.printStackTrace();
			throw new IllegalArgumentException("客户端协议错误 ，检查 url 配置 url: "+url);
		} finally{
			HttpClientUtils.closeQuietly(response);
		}
	}

    /**
     * 上传文件
     * @param url
     * @param fileName
     * @param inputStream
     */
	public static String postStream(String url,String fileName,InputStream inputStream){
	    CloseableHttpClient httpClient = getHttpClient();
		HttpPost postMethod = new HttpPost(url);
		postMethod.addHeader("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/70.0.3538.102 Safari/537.36");
        HttpEntity httpEntity = MultipartEntityBuilder.create()
                .addBinaryBody(fileName, inputStream,HttpUtil.MULTIPART_FORM_DATA,fileName)
                .setCharset(Charset.forName("utf-8"))
                .build();
        postMethod.addHeader("Cookie","Hm_lvt_ea4269d8a00e95fdb9ee61e3041a8f98=1541318920; PHPSESSID=ffr5sik57rhj9ua7rjsj09oh81; Hm_lvt_74847c14327db1f3a2f88c2583154efe=1553666165,1554778795,1554860900; Hm_lpvt_74847c14327db1f3a2f88c2583154efe=1554862099");
        postMethod.addHeader("Accept","text/plain, */*; q=0.01");
        postMethod.addHeader("Accept-Encoding","gzip, deflate");
        postMethod.addHeader("Accept-Language","zh-CN,zh;q=0.9,und;q=0.8");
        postMethod.addHeader("Origin","http://tool.mkblog.cn");
        postMethod.addHeader("Referer","http://tool.mkblog.cn/ocr/");
        postMethod.setEntity(httpEntity);
        HttpResponse response = null;
        try {
            response = httpClient.execute(postMethod);
            StatusLine statusLine = response.getStatusLine();
            int statusCode = statusLine.getStatusCode();
            if(statusCode != HttpStatus.SC_OK){
                logger.error("上传文件["+url+"]失败:"+statusCode);
                return "";
            }
            HttpEntity entity = response.getEntity();

            String result = IOUtils.toString(entity.getContent());
            return result;
        } catch (IOException e) {
            logger.error("上传文件出错",e);
        }finally {
//            HttpClientUtils.closeQuietly(response);
        }

        return "";
    }
}