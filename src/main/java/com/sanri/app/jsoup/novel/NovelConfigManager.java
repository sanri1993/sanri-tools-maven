package com.sanri.app.jsoup.novel;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.cglib.core.ReflectUtils;
import org.springframework.util.ReflectionUtils;
import sanri.utils.PathUtil;

import java.beans.PropertyDescriptor;
import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Method;
import java.util.*;

/**
 * 创建人     : sanri
 * 创建时间   : 2018/11/16-21:28
 * 功能       : 小说配置管理,实例管理;
 * 1.加载配置
 * 2.实例化网源实例,注入配置
 */
public class NovelConfigManager {
    private static Log logger = LogFactory.getLog(NovelConfigManager.class);
    private static final  Map<String,Map<String,String>> config = new HashMap<String, Map<String,String>>();
    // key ==>
    public  static  Map<String,NovelNetSource> novelNetSourceMap = new HashMap<String, NovelNetSource>();

   static {
       Reader reader = PathUtil.loadReader("/com/sanri/config/novelnetsource.properties");
       Properties allProperties = new Properties();
       try {
           allProperties.load(reader);

           Enumeration<Object> keys = allProperties.keys();
           while (keys.hasMoreElements()){
               String wholeKey = ObjectUtils.toString(keys.nextElement());
               if("open".equals(wholeKey)){
                   boolean open = Boolean.parseBoolean(ObjectUtils.toString(allProperties.get(wholeKey)));
                   if(open){continue;}else{ break;}
               }

               if(StringUtils.isNotBlank(wholeKey)){
                   String[] split = wholeKey.split("\\.");
                   String netSourceKey = split[0];
                   String key = split[1];
                   String value = ObjectUtils.toString(allProperties.get(wholeKey));

                   Map<String,String> properties = config.get(netSourceKey);
                   if(properties == null){
                       properties = new HashMap<String, String>();
                       config.put(netSourceKey,properties);
                   }

                   properties.put(key,value);

               }
           }

       } catch (IOException e) {
           e.printStackTrace();
       } finally {
           IOUtils.closeQuietly(reader);
           // 实例化类
           instanceNetSources();
       }
   }

    private static void instanceNetSources() {
        Iterator<String> iterator = config.keySet().iterator();
//        Iterator<Map<String,String>> iterator = config.keys().iterator();
        while(iterator.hasNext()){
            String key = iterator.next();
            Map<String,String> properties = config.get(key);
            String serviceImpl = properties.get("serviceImpl");
            try {
                Class serviceClass = ClassUtils.getClass(serviceImpl);
                Class superclass = serviceClass.getSuperclass();
                if(superclass !=  NovelNetSource.class){
                    logger.error("类加载错误,必须是 NovelNetSource 的实例:"+serviceImpl+",属于网源:"+properties.get("url"));
                    continue;
                }

                Object serviceClassInstance = ReflectUtils.newInstance(serviceClass);
                novelNetSourceMap.put(key, (NovelNetSource) serviceClassInstance);

                //注入属性
                PropertyDescriptor[] beanSetters = ReflectUtils.getBeanSetters(serviceClass);
                for (PropertyDescriptor beanSetter : beanSetters) {
                    Method writeMethod = beanSetter.getWriteMethod();
                    String name = beanSetter.getName();
                    String value = properties.get(name);
                    ReflectionUtils.invokeMethod(writeMethod,serviceClassInstance,value);
                }

            } catch (ClassNotFoundException e) {
                logger.error("类加载错误:"+serviceImpl+",属于网源:"+properties.get("url"));
                continue;
            }
        }
    }


   public static  NovelNetSource getNovelNetSource(String name){
       return novelNetSourceMap.get(name);
   }

}
