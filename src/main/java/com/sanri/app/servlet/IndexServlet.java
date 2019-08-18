package com.sanri.app.servlet;

import com.sanri.app.BaseServlet;
import com.sanri.app.OrderedProperties;
import com.sanri.app.ToolModel;
import com.sanri.frame.RequestMapping;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sanri.utils.NumberUtil;
import sanri.utils.PropertyEditUtil;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RequestMapping("/index")
public class IndexServlet extends BaseServlet {
    private static final Map<String,ToolModel> toolModelMap = new LinkedHashMap<String, ToolModel>();
    private static final OrderedProperties properties = new OrderedProperties();

    static {
       loadConfig();
    }

    private static void loadConfig() {
        InputStream resourceAsStream = IndexServlet.class.getResourceAsStream("/com/sanri/config/tools.properties");
        InputStreamReader inputStreamReader  = null;
        try {
           inputStreamReader  = new InputStreamReader(resourceAsStream,"utf-8");
           properties.load(inputStreamReader);

           parserProperties();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            IOUtils.closeQuietly(inputStreamReader);
        }
    }

    static  final long minutes = 60 * 1000;

    public int toolCount(){
        return toolModelMap.size();
    }

    public int reloadConfig(){
        loadConfig();
        return 0;
    }

    public int visited(String url){
        ToolModel toolModel = toolModelMap.get(url);
        if(toolModel == null){
            return 0;
        }
        if(toolModel != null){
            toolModel.visited();
        }
        properties.setProperty(toolModel.getKey()+toolModel.getTotalCalls(),toolModel.getTotalCalls()+"");

        return 0;
    }

    /**
     * 工具名列表
     * @return
     */
    public Map<String,String> toolNames(){
//        return toolModelMap.values().stream().collect(Collectors.toMap(toolModel -> toolModel.getName(), toolModel -> toolModel.getUrl()));
        Map<String,String> toolNames = new HashMap<>();
        Iterator<Map.Entry<String, ToolModel>> iterator = toolModelMap.entrySet().iterator();
        while (iterator.hasNext()){
            Map.Entry<String, ToolModel> toolModelEntry = iterator.next();
            ToolModel toolModel = toolModelEntry.getValue();
            toolNames.put(toolModel.getTitle(),toolModel.getUrl());
        }
        return toolNames;
    }

    /**
     * 获取工具详情
     * @param url
     * @return
     */
    public ToolModel toolInfo(String url){
        return toolModelMap.get(url);
    }

    /**
     * 添加工具到环境
     * @param url
     * @return
     */
    public int addTool(String url){
        ToolModel toolModel = toolModelMap.get(url);
        String env = System.getenv("env");
        if(StringUtils.isBlank(env)){env = "default" ;}
        toolModel.addEnv(env);
        return 0;
    }

    /**
     * 删除当前工具
     * @param url
     * @return
     */
    public int removeTool(String url){
        ToolModel toolModel = toolModelMap.get(url);
        String env = System.getenv("env");
        if(StringUtils.isBlank(env)){env = "default" ;}
        toolModel.removeEnv(env);
        return 0;
    }

    public List<ToolModel> listTools(){
//        parserProperties();
        Collection<ToolModel> values = toolModelMap.values();
        ArrayList<ToolModel> toolModels = new ArrayList<ToolModel>(values);

        List<ToolModel> last5MinutesCalls = new ArrayList<ToolModel>();
        Iterator<ToolModel> iterator = toolModels.iterator();
        String env = System.getenv("env");if(StringUtils.isBlank(env)){env = "default";}
        while (iterator.hasNext()){
            ToolModel current = iterator.next();
            //根据当前系统环境来决定要展示哪些工具
            if(!current.contains(env)){
                iterator.remove();
                continue;
            }

            long lastCallTime = current.getLastCallTime();
            if((System.currentTimeMillis() - lastCallTime ) <= 5 * minutes){
                last5MinutesCalls.add(current);
                iterator.remove();
            }
        }

        Collections.sort(last5MinutesCalls, new Comparator<ToolModel>() {
            @Override
            public int compare(ToolModel o1, ToolModel o2) {
                long sub = o2.getLastCallTime() - o1.getLastCallTime();
                int finallyValue =  0;
                if(sub >= Integer.MAX_VALUE){finallyValue  = 1;}
                else if(sub <= Integer.MIN_VALUE){finallyValue  = - 1;}
                else {finallyValue = (int) sub;}

                return finallyValue;
            }
        });

        Collections.sort(toolModels, new Comparator<ToolModel>() {
            @Override
            public int compare(ToolModel o1, ToolModel o2) {
                return o2.getTotalCalls() - o1.getTotalCalls();
            }
        });

        last5MinutesCalls.addAll(toolModels);

        return last5MinutesCalls;
    }

//    private static Log logger = LogFactory.getLog(IndexServlet.class);
    private static final Logger logger = LoggerFactory.getLogger(IndexServlet.class);

    private static void parserProperties() {
        Set<String> propertyNames =  properties.stringPropertyNames();
        Iterator<String> propertyNamesIterator = propertyNames.iterator();
        Map<String,Map<String,String>> beansMap = new LinkedHashMap<String, Map<String, String>>();
        while(propertyNamesIterator.hasNext()){
            String complexKey = propertyNamesIterator.next();
            String value = properties.getProperty(complexKey);
            if(StringUtils.isNotBlank(complexKey)){
                String[] complexKeyArray = complexKey.split("\\.");
                if(complexKeyArray.length != 2){
                    logger.error("复杂键不符合规范:"+complexKey);
                    continue;
                }
                String key = complexKeyArray[0];
                String property = complexKeyArray[1];

                Map<String, String> stringStringMap = beansMap.get(key);
                if(stringStringMap == null){
                    stringStringMap = new HashMap<String, String>();
                    stringStringMap.put("key",key);
                    beansMap.put(key,stringStringMap);
                }
                stringStringMap.put(property,value);
            }
        }

        Iterator<Map<String, String>> iterator = beansMap.values().iterator();
        long initLastCallTime = System.currentTimeMillis();
        while (iterator.hasNext()){
            Map mapData = iterator.next();
            String url = ObjectUtils.toString(mapData.get("url"));
            ToolModel toolModel = new ToolModel(initLastCallTime);
            PropertyEditUtil.populateMapData(toolModel,mapData,"totalCalls","lastCallTime","envs");
            toolModel.setTotalCalls(NumberUtil.toInt(ObjectUtils.toString(mapData.get("totalCalls")),0));

            //由环境来决定是否加入配置
            String envs = ObjectUtils.toString(mapData.get("envs"));
            if(StringUtils.isNotBlank(envs)) {
                String[] envsList = StringUtils.split(envs, ",");
                toolModel.configEnvs(envsList);
            }
            toolModelMap.put(url,toolModel);
        }

    }
}
