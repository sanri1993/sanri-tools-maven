package com.sanri.app;

import eu.bitwalker.useragentutils.UserAgent;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 
 * 作者:sanri <br/>
 * 时间:2017-7-22下午2:42:25<br/>
 * 功能:通用 servlet 实现一些通用功能 <br/>
 */
public class BaseServlet {
	protected Log logger = LogFactory.getLog(getClass());
	private static Log currLogger = LogFactory.getLog(BaseServlet.class);
	// 数据临时目录
	protected static File dataTempPath = null;
	// 配置路径
	protected static File dataConfigPath = null;
	static{
	    String systemconfig = "function.open";

        ConfigCenter configCenter = ConfigCenter.getInstance();
        File javaIoTmpDir = SystemUtils.getJavaIoTmpDir();
        String configPath = configCenter.getString(systemconfig, "data.config.path");
        String tempPath = configCenter.getString(systemconfig, "data.temp.path");

        if(StringUtils.isBlank(configPath)){
           dataConfigPath = new File(javaIoTmpDir,"config");
        }else{
            dataConfigPath = new File(configPath);
        }
        if(StringUtils.isBlank(tempPath)){
            dataTempPath = new File(javaIoTmpDir,"temp");
        }else{
            dataTempPath = new File(tempPath);
        }
//        dataTempPath = new File(PathUtil.webAppsPath()+"/temp");
		if(!dataTempPath.exists()){
			dataTempPath.mkdirs();
		}
		if(dataConfigPath.exists()){
            dataConfigPath.mkdirs();
        }

        currLogger.info("项目配置文件路径:"+dataConfigPath);
        currLogger.info("临时文件路径:"+dataTempPath);
	}

	//初始创建 10 个线程的线程池,所有线程往线程池中提交
	protected static ExecutorService executorService = Executors.newFixedThreadPool(10);
	
	public static final  String datetimePattern  = "yyyy-MM-dd HH:mm:ss";
	public static final  String default_path = "/";
	public static final  int default_age = 365 * 24 * 3600;
	public static final  Charset charset = Charset.forName("utf-8");

	/**
	 * 
	 * 作者:sanri <br/>
	 * 时间:2017-7-22下午2:19:45<br/>
	 * 功能:向客户端添加缓存数据 <br/>
	 * @param key 键
	 * @param value 值
	 * @param response response 对象
	 * @param age 有效期以秒为单位
	 */
	protected void addCookie(HttpServletResponse response,String key, String value, int age,String path) {
		Cookie cookie = new Cookie(key,new String(Base64.encodeBase64(value.getBytes(charset))));
		cookie.setMaxAge(age);
		cookie.setPath(path);
		response.addCookie(cookie);
	}
	protected void addCookie(HttpServletResponse response,String key, String value,String path) {
		addCookie(response,key, value,default_age,path);
	}
	protected void addCookie(HttpServletResponse response,String key, String value) {
		addCookie(response,key, value,default_age,default_path);
	}
	
	/**
	 * 
	 * 作者:sanri <br/>
	 * 时间:2017-7-22下午2:25:34<br/>
	 * 功能:删除客户端缓存<br/>
	 * @param key 缓存键
	 * @param response
	 * 思路:建立同名 cookie 来覆盖删除,直接写在 / 目录,才能保证删除
	 */
	protected void deleteCookie(HttpServletResponse response,String key) {
		Cookie cookie = new Cookie(key, "");
		cookie.setMaxAge(0);
		cookie.setPath(default_path);
		response.addCookie(cookie);
	}
	
	/**
	 * 作者:sanri <br/>
	 * 时间:2017-7-22下午2:23:00<br/>
	 * 功能:查找客户端缓存数据 <br/>
	 * @param key 缓存键
	 * @param request
	 * @return
	 * @throws UnsupportedEncodingException
	 */
	protected Cookie findCookie(HttpServletRequest request,String key) throws UnsupportedEncodingException {
		Cookie[] cookies = request.getCookies();
		if (cookies != null) {
			for (int i = 0; i < cookies.length; i++) {
				Cookie cookie = cookies[i];
				if (cookie.getName().equals(key)) {
					return cookie;
				}
			}
		}
		return null;
	}
	/**
	 * 
	 * 作者:sanri <br/>
	 * 时间:2017-7-22下午2:30:53<br/>
	 * 功能:获取到 cookie 中存储的值解码 base64 <br/>
	 * @param cookie
	 * @return
	 */
	protected String cookieValue(Cookie cookie){
		return new String(Base64.decodeBase64(cookie.getValue().getBytes()),charset);
	}
	
	/**
	 * 
	 * 作者:sanri <br/>
	 * 时间:2017-7-22下午2:43:43<br/>
	 * 功能:获取客户端请求 ip <br/>
	 * @param request
	 * @return
	 */
    public static String remortIPInfo(HttpServletRequest request) {
        String ip = request.getHeader("x-forwarded-for");
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
    
    /**
     * 
     * 作者:sanri <br/>
     * 时间:2017-7-22下午2:53:20<br/>
     * 功能:获取客户端信息,有可能客户端不是用浏览器访问而返回空  <br/>
     * 可以获取客户端操作系统,浏览器类型,浏览器版本等信息
     * @param request
     * @return 
     */
    protected UserAgent remoteAgentInfo(HttpServletRequest request){
    	String userAgentString = request.getHeader("User-Agent");
		if(StringUtils.isBlank(userAgentString)){
			return null;
		}
		userAgentString = userAgentString.toLowerCase();
		UserAgent userAgent = UserAgent.parseUserAgentString(userAgentString);
		return userAgent;
    }

	/**
     * 作者:sanri <br/>
     * 时间:2017-10-31下午4:10:50<br/>
     * 功能:预览 <br/>
     *
     * @param input
     * @param mime
     * @throws IOException e
     */
    protected void preview(InputStream input, MimeType mime, HttpServletResponse response) throws IOException {
        if (input == null) {
            return;
        }
        response.setContentType(mime.getContentType());
        response.setHeader("Set-Cookie", "fileDownload=true; path=/");
        ServletOutputStream output = response.getOutputStream();
        IOUtils.copy(input, output);
        output.flush();
    }

    /**
     * 作者:sanri <br/>
     * 时间:2017-10-31下午4:18:54<br/>
     * 功能:下载 <br/>
     *
     * @param input
     * @param mime
     * @param fileName
     * @param response
     * @throws IOException
     */
    protected void download(InputStream input, MimeType mime, String fileName,HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (input == null) {
            return;
        }
        boolean isAuto = false;
        if(mime == MimeType.AUTO){
        	mime = parseMIME( fileName);
        	isAuto = true;
        }
        response.setContentType(mime.getContentType());
        response.setHeader("Set-Cookie", "fileDownload=true; path=/");
       
        String suffix = mime.getSuffix();
        String encodeFileName = encodeFilename(fileName, request);
        if(StringUtils.isNotBlank(suffix) && !isAuto){
        	encodeFileName  += ("."+ mime.getSuffix());
        }
        
        response.setHeader("Content-Disposition", "attachment;filename=\"" + encodeFileName + "\"");
        long length = input.available();
        if (length != -1) {
            response.setContentLength((int) length);
        }
        ServletOutputStream output = response.getOutputStream();
        IOUtils.copy(input, output);
        output.flush();
    }
    
    /**
     * 
     * 作者:sanri <br/>
     * 时间:2018-5-28下午5:08:06<br/>
     * 功能:mime 类型解析  <br/>
     * @param fileName
     * @return
     */
	private MimeType parseMIME( String fileName) {
    	String extension = FilenameUtils.getExtension(fileName);
    	MimeType parseMIME = MimeType.parseMIME(extension);
    	if(parseMIME != null){
    		return parseMIME;
    	}
    	throw new IllegalArgumentException("不支持的 mime 类型，文件名为:"+fileName);
	}

    private static String encodeFilename(String filename, HttpServletRequest request) {
        /**
         * 获取客户端浏览器和操作系统信息
         * 在IE浏览器中得到的是：User-Agent=Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1; SV1; Maxthon; Alexa Toolbar)
         * 在Firefox中得到的是：User-Agent=Mozilla/5.0 (Windows; U; Windows NT 5.1; zh-CN; rv:1.7.10) Gecko/20050717 Firefox/1.0.6
         */
        String agent = request.getHeader("USER-AGENT");
        try {
            if ((agent != null) && (-1 != agent.indexOf("MSIE"))) {
                String newFileName = URLEncoder.encode(filename, "UTF-8");
                newFileName = StringUtils.replace(newFileName, "+", "%20");
                if (newFileName.length() > 150) {
                    newFileName = new String(filename.getBytes("GB2312"), "ISO8859-1");
                    newFileName = StringUtils.replace(newFileName, " ", "%20");
                }
                return newFileName;
            }
            if ((agent != null) && (-1 != agent.indexOf("Mozilla"))) {
                return new String(filename.getBytes("UTF-8"), "ISO8859-1");
            }
            return filename;
        } catch (Exception ex) {
            return filename;
        }
    }
    
    public enum MimeType {
    	AUTO("自动获取",""),
        STREAM("application/octet-stream", "jpg"),

        PDF("application/pdf", "pdf"),
        ZIP("application/zip", "zip"),
        RAR("application/zip", "rar"),
        EXCEL2003("application/vnd.ms-excel", "xls"),
        EXCEL2007("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "xlsx"),
        EXE("application/octet-stream", "exe"),

        TXT("text/plain", "txt"),
        JAVA("text/plain", "java"),
        PYTHON("text/plain", "py"),
        JAVASCRIPT("text/plain", "js"),
        CSS("text/plain", "css"),
        XML("text/xml","xml"),
        SQL("text/plain","sql"),

        JPG("application/x-jpg", "jpg"),
        JPEG("image/jpeg", "jpg"),
        GIF("image/gif", "gif"),
        PNG("application/x-png", "png"),
    	GZ("application/octet-stream","gz");
    	
    	
        private String contentType;
        private static final String CHARSET = "UTF-8";
        private String suffix;

        public String getContentType() {
            return contentType + ";charset=" + CHARSET;
        }

        private MimeType(String contentType, String suffix) {
            this.contentType = contentType;
            this.suffix = suffix;
        }

        public String getSuffix() {
            return suffix;
        }

        /**
         * &#x4f5c;&#x8005;:sanri <br/>
         * &#x65f6;&#x95f4;:2017-10-31&#x4e0b;&#x5348;4:36:17<br/>
         * &#x529f;&#x80fd;:&#x89e3;&#x6790;&#x51fa; mime &#x7c7b;&#x578b; <br/>
         *
         * @param fileType
         * @return
         */
        public static MimeType parseMIME(String fileType) {
            if (StringUtils.isBlank(fileType)) {
                throw new IllegalArgumentException("不支持的 MIME类型");
            }
            MimeType[] values = MimeType.values();
            for (MimeType mimeType : values) {
                if (mimeType.getSuffix().equalsIgnoreCase(fileType)) {
                    return mimeType;
                }
            }
            throw new IllegalArgumentException("不支持的 MIME类型");
        }
    }

    /**
     * 创建临时目录 可传多级路径
     * @param childs
     */
    public static File mkTmpPath(String childs){
        File file = new File(dataTempPath, childs);
        if(!file.exists()){
            file.mkdirs();
        }
        return file;
    }

    /**
     * 创建配置路径
     * @param childs
     */
    public static File mkConfigPath(String childs){
        File file = new File(dataConfigPath,childs);
        if(!file.exists()){
            file.mkdirs();
        }
        return file;
    }

}
