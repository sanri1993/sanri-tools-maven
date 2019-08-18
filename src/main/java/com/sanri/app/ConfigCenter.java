package com.sanri.app;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import sanri.utils.NumberUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class ConfigCenter {
    private static final String configDir = "/com/sanri/config";

    private static final Map<String,Properties> propertiesMap =  new HashMap<String, Properties>();
    private static ConfigNode configNode ;
    private static ConfigCenter configCenter = new ConfigCenter();
    private ConfigCenter(){}

    public  static ConfigCenter getInstance(){
        return configCenter;
    }

    static class ConfigNode{
        //节点名称
        private String name;

        //父配置节点
        private ConfigNode parent;

        //子配置节点
        private List<ConfigNode> childs = new ArrayList<ConfigNode>();

        private boolean isValue;

        public ConfigNode() {
        }

        public ConfigNode(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public ConfigNode getParent() {
            return parent;
        }

        public void setParent(ConfigNode parent) {
            this.parent = parent;
        }

        public void addChild(ConfigNode configNode){
            childs.add(configNode);
        }

        /**
         * 根据名称查询子节点
         * @param name
         * @return
         */
        public ConfigNode findChild(String name){
            for (ConfigNode child : childs) {
                if(child.name.equals(name)){
                    return child;
                }
            }
            return null;
        }

        @Override
        public String toString() {
            return ToStringBuilder.reflectionToString(this,ToStringStyle.SHORT_PREFIX_STYLE);
        }

        public boolean isValue() {
            return isValue;
        }

        public void setValue(boolean value) {
            isValue = value;
        }

        public List<ConfigNode> getPath(){
            ConfigNode configNode = this;
            if(this.isValue){
                configNode = this.parent;
            }
            List<ConfigNode> configNodes = new ArrayList<ConfigNode>();
            configNodes.add(configNode);

            while((configNode = configNode.parent) != null){
               configNodes.add(configNode);
            }
            return configNodes;
        }
    }

    static {
        URL resource = ConfigCenter.class.getResource(configDir);
        String path = resource.getPath();
        File dir = new File(path);
        File[] files = dir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(".properties");
            }
        });

        if(ArrayUtils.isNotEmpty(files)) {
            for (File file : files) {
                Properties properties = new Properties();
                InputStreamReader inputStreamReader = null;
                try {
                    inputStreamReader = new InputStreamReader(new FileInputStream(file));
                    properties.load(inputStreamReader);
                    String baseName = FilenameUtils.getBaseName(file.getName());
                    propertiesMap.put(baseName,properties);
                } catch (java.io.IOException e) {
                    e.printStackTrace();
                } finally {
                    IOUtils.closeQuietly(inputStreamReader);
                }
            }
        }

        //配置转换成树结构
        ConfigNode superRoot = new ConfigNode("super");

        Iterator<Map.Entry<String, Properties>> iterator = propertiesMap.entrySet().iterator();
        while (iterator.hasNext()){
            Map.Entry<String, Properties> entry = iterator.next();
            String baseName = entry.getKey();
            Properties properties = entry.getValue();

            ConfigNode root = new ConfigNode(baseName);
            root.setParent(superRoot);
            superRoot.addChild(root);

            //属性转换成树结构
            convert2Tree(root,properties);
        }

        configNode = superRoot;
    }

    private static void convert2Tree(ConfigNode root, Properties properties) {
        Iterator<String> iterator = properties.stringPropertyNames().iterator();
        while (iterator.hasNext()){
            String key = iterator.next();
            String[] keyDots = StringUtils.split(key, '.');
            ConfigNode leafNode = converKey2Tree(root, keyDots, 0);

            String value = properties.getProperty(key);
            //追加值节点
            ConfigNode configNode = new ConfigNode(value);
            configNode.isValue = true;
            leafNode.addChild(configNode);
            configNode.parent = leafNode;

        }
    }

    /**
     * 键转成树,拿最后叶节点
     * @param root
     * @param keyDots
     * @param deep
     * @return
     */
    private static ConfigNode converKey2Tree(ConfigNode root, String [] keyDots, int deep) {
        if(deep >= keyDots.length ){
            return root;
        }

        String currKey = keyDots[deep];
        ConfigNode configNode = root.findChild(currKey);
        if(configNode == null) {
            //追加节点
            configNode = new ConfigNode(currKey);
            configNode.parent = root;
            root.addChild(configNode);
        }

        return  converKey2Tree(configNode, keyDots, ++deep);
    }

    public String getString(String baseName,String key){
        Properties properties = propertiesMap.get(baseName);
        String value = properties.getProperty(key);
        if(StringUtils.isNotBlank(value)){
            return value.trim();
        }
        return "";
    }

    /**
     * 获取 bool 值
     * @param baseName
     * @param key
     * @return
     */
    public boolean getBoolean(String baseName,String key){
        return Boolean.parseBoolean(getString(baseName,key));
    }

    public int getInt(String baseName,String key){
        Properties properties = propertiesMap.get(baseName);
        String value = properties.getProperty(key,"-1");
        return NumberUtil.toInt(value);
    }

    public long getLong(String baseName,String key){
        Properties properties = propertiesMap.get(baseName);
        String value = properties.getProperty(key,"-1");
        return NumberUtil.toLong(value);
    }

    public double getDouble(String baseName,String key){
        Properties properties = propertiesMap.get(baseName);
        String value = properties.getProperty(key,"-1");
        return NumberUtil.toDouble(value);
    }

    public float getFloat(String baseName,String key){
        Properties properties = propertiesMap.get(baseName);
        String value = properties.getProperty(key,"-1");
        return NumberUtil.toFloat(value);
    }

    /**
     * 获取列表数据
     * @param mapper_jdbc_java
     * @param s
     * @return
     */
    public <T> List<T> getList(String baseName,String key,Class<T> clazz) {
        Properties properties = propertiesMap.get(baseName);
        String property = properties.getProperty(key);
        String[] split = StringUtils.split(property, ',');
        List<T> list = new ArrayList<>();
        for (String part : split) {
            list.add(clazz.cast(part));
        }
        return list;
    }

    /**
     * 根据前缀获取 map 数据
     * @param baseName
     * @param prefix
     * @return
     */
    public Map<String,String> getSubConfigs(String baseName,String prefix){
        Properties properties = propertiesMap.get(baseName);

        Map<String,String> config = new HashMap<String, String>();

        if(properties != null){
            Iterator<Object> iterator = properties.keySet().iterator();
            while (iterator.hasNext()){
                String wholeKey = ObjectUtils.toString(iterator.next());
                if(wholeKey.startsWith(prefix)){
                    String value = StringUtils.trim(properties.getProperty(wholeKey));
                    String key = wholeKey.substring(prefix.length() + 1);
                    config.put(key,value);
                }
            }
        }

        return config;
    }
}
