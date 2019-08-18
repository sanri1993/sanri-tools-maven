package sanri.utils;

import org.apache.commons.collections.map.HashedMap;
import org.apache.commons.lang3.StringUtils;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * url uri 工具;功能列表如下
 * 1:相对化地址
 * 2:绝对化地址
 * 3:解析参数列表
 * 4:解析出每一段路径
 */
public class URLUtil {

    /**
     * 相对化地址
     * /a/b/c 相对于 /a ==> b/c
     * 相对化后的地址不以 / 开头
     * @return
     */
    public static URI relativize(String basePath, String path) throws URISyntaxException {
        URI pathURI = new URI(path);
        URI basePathURI = new URI(basePath);
        URI relativize = basePathURI.relativize(pathURI);
        return relativize;
    }

    /**
     * 绝对化地址
     * basePath 必须要是以 协议开头的地址 例 http://xx/xx/xx
     * 例:
     *  http://www.baidu.com/c/d  ocr /a/b ==> http://www.baidu.com/a/b
     *  http://www.baidu.com/c/d/m  ocr  ../b ==> http://www.baidu.com/c/b
     * @param basePath
     * @param path
     * @return
     * @throws URISyntaxException
     */
    public static URI resolve(String basePath,String path) throws URISyntaxException {
        URI basePathURI = new URI(basePath);
        URI resolve = basePathURI.resolve(path);
        return resolve;
    }

    /**
     * 解析出所有查询参数
     * @param uri
     * @return
     * @throws URISyntaxException
     */
    public static Map<String,String> params(String uri) throws URISyntaxException {
        URI originURI = new URI(uri);
        String rawQuery = originURI.getRawQuery();
        String query = originURI.getQuery();

        Map<String,String> params = new HashedMap();
        if(StringUtils.isNotBlank(query)){
            String[] keyValues = query.split("&");
            for (String keyValue : keyValues) {
                if(keyValue.contains("=")){
                    String[] keyAndValue = keyValue.split("=",2);
                    params.put(keyAndValue[0],keyAndValue[1]);
                }
            }
        }
        return params;
    }

    /**
     * 获取参数列表中的某一个参数值
     * @param uri
     * @param paramName
     * @return
     * @throws URISyntaxException
     */
    public static String param(String uri,String paramName) throws URISyntaxException {
        Map<String, String> params = params(uri);
        return params.get(paramName);
    }

    /**
     * 解析出路径列表
     * @param uri
     * @return
     */
    public static List<String> paths(String uri) throws URISyntaxException {
        URI originURI = new URI(uri);
        String path = originURI.getPath();
        if(StringUtils.isNotBlank(path)){
            String[] pathArray = path.split("/");
            return Arrays.asList(pathArray);
        }
        return new ArrayList<String>();
    }

    /**
     * 获取某一级路径
     * @param uri
     * @param index
     * @return
     * @throws URISyntaxException
     */
    public static String path(String uri,int index) throws URISyntaxException {
        return paths(uri).get(index);
    }

    /**
     * 获取最后一级路径
     * @param uri
     * @return
     * @throws URISyntaxException
     */
    public static String pathLast(String uri) throws URISyntaxException {
        URI originURI = new URI(uri);
        String path = originURI.getPath();
        String lastPath = StringUtils.substringAfterLast(path, "/");
        return lastPath;
    }
}
