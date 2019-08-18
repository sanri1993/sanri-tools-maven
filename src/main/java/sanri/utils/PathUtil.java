package sanri.utils;

import org.apache.commons.lang3.StringUtils;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
/**
 * 
 * 创建时间:2016-9-24上午7:30:24<br/>
 * 创建者:sanri<br/>
 * 功能:路径工具处理类<br/>
 * ROOT 路径是指 WebRoot 路径,也即 web 服务根路径
 */
public class PathUtil {
	
	public static URL URL = PathUtil.class.getResource("/");
	public static URI ROOT;
	static{
		try {
			ROOT = URL.toURI().resolve("../../");
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * 相对于根的路径
	 * @param relativePath
	 * @return
	 */
	public static String getPath(String relativePath){
		if(StringUtils.isBlank(relativePath)){
			return ROOT.getPath();
		}
		if(relativePath.startsWith("/")){
			relativePath = relativePath.substring(1);
		}
		
		return ROOT.resolve(relativePath).getPath();
	}
	
	/**
	 * 
	 * 功能: webapps 路径,项目上层路径<br/>
	 * 非 tomcat 容器将找不到 webapps 路径<br/>
	 * 创建时间:2016-9-24上午7:33:29<br/>
	 * 作者：sanri<br/>
	 */
	public static String webAppsPath(){
		return getPath("../");
	}
	/**
	 * 
	 * 功能:项目的 WEB-INF 路径,必须使用名字 WEB-INF 的路径才能取到<br/>
	 * 创建时间:2016-9-24上午7:34:38<br/>
	 * 作者：sanri<br/>
	 */
	public static String webinfPath(){
		return getPath("/WEB-INF");
	}
	
	public static String webPath(){
		return ROOT.getPath();
	}
	/**
	 * 
	 * 功能:类路径<br/>
	 * 创建时间:2016-9-24上午7:35:15<br/>
	 * 作者：sanri<br/>
	 */
	public static String classPath(){
//		return getPath("/WEB-INF/classes");
		return URL.getPath();
	}
	
	/**
	 * 
	 * 功能:包路径<br/>
	 * 创建时间:2016-9-24上午7:35:55<br/>
	 * 作者：sanri<br/>
	 */
	public static String pkgPath(String package_){
		String relative = package_.replaceAll("\\.", "/");
//		return getPath("/WEB-INF/classes/"+relative);
		URI resolve = null;
		try {
			resolve = URL.toURI().resolve(relative);
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
		return resolve.getPath();
	}
	
	/**
	 * 
	 * 作者:sanri <br/>
	 * 时间:2018-6-24下午6:07:21<br/>
	 * 功能:从类路径加载资源  <br/>
	 * @param relative
	 * @return
	 */
	public static InputStream loadStream(String relative){
		InputStream resourceAsStream = PathUtil.class.getResourceAsStream(relative);
		return resourceAsStream;
	}

	public static Reader loadReader(String relative){
		InputStream inputStream = loadStream(relative);
		InputStreamReader inputStreamReader = null;
		try {
			inputStreamReader = new InputStreamReader(inputStream,"utf-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return inputStreamReader;
	}
}
