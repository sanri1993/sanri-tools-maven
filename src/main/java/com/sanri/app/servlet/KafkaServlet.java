package com.sanri.app.servlet;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.sanri.app.BaseServlet;
import com.sanri.app.kafka.KafkaConnInfo;
import com.sanri.app.kafka.ModelTopicAndPartition;
import com.sanri.app.kafka.NewKafkaMonitor;
import com.sanri.app.kafka.OffsetShow;
import com.sanri.app.kafka.OffsetThirdpartMonitor;
import com.sanri.app.kafka.OldKakaMonitor;
import com.sanri.app.kafka.TopicOffset;
import com.sanri.app.serializer.FastJsonSerializer;
import com.sanri.app.serializer.StringSerializer;
import com.sanri.frame.DispatchServlet;
import com.sanri.frame.RequestMapping;
import kafka.common.TopicAndPartition;
import kafka.utils.ZkUtils;
import org.I0Itec.zkclient.ZkClient;
import org.I0Itec.zkclient.serialize.ZkSerializer;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.common.security.JaasUtils;
import sanri.utils.NumberUtil;
import scala.collection.JavaConversions;
import scala.collection.Set;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.sanri.app.servlet.ZkServlet.zkSerializerMap;

@RequestMapping("/kafka")
public class KafkaServlet extends BaseServlet {
    public static File kafkaConnDir;
    public static File newKafkaConfigDir;

    public static Map<String, ZkUtils> zkUtilsMap = new HashMap<String, ZkUtils>();
    public static Map<String, ZkClient> zkClientMap = new HashMap<String, ZkClient>();

    private OldKakaMonitor oldKakaMonitor = new OldKakaMonitor();
    private NewKafkaMonitor newKafkaMonitor = new NewKafkaMonitor();
    private OffsetThirdpartMonitor offsetThirdpartMonitor = new OffsetThirdpartMonitor();

    private ZkSerializer fastJsonSerialize = new FastJsonSerializer();
    private ZkSerializer zkSerializer = new StringSerializer();

    static {
        kafkaConnDir = mkConfigPath("kafka/conns");
        newKafkaConfigDir = mkConfigPath("/kafka/configs/new");
    }

    {
        //新 kafka 的初始化
        File[] files = kafkaConnDir.listFiles();
        for (File file : files) {
            try {
                String contentJson = FileUtils.readFileToString(file);
                KafkaConnInfo kafkaConnInfo = JSONObject.parseObject(contentJson, KafkaConnInfo.class);
                KafkaConnInfo.KafkaVersion kafkaVersion = kafkaConnInfo.getKafkaVersion();
                if (kafkaVersion == KafkaConnInfo.KafkaVersion.NEW) {
                    newKafkaMonitor.initConsumer(file.getName());
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 创建 kafka 连接
     *
     * @param name     zk 连接名 -> zkServlet 中的连接名称; 前端从 zk 连接中选择
     * @param rootPath 如果直接是挂在根路径的,直接为空,否则填具体路径
     * @param version  [new,old] 0.8.1.1 及以前的版本为旧版本; 之后的为新版本
     * @return
     */
    static Pattern ipPort = Pattern.compile("(\\d+\\.\\d+\\.\\d+\\.\\d+):(\\d+)");
    public int createConn(String name, String rootPath, String version) throws IOException {
        ZkClient zkClient = zkClient(name);

        //读取 zk kafka 配置信息
//        String path = rootPath + "/brokers/ids/0"; // 这次没有看到 0 节点，先暂时换成 1 节点 2019/7/29
        String path = rootPath + "/brokers/ids/1";
        String brokerInfo = ObjectUtils.toString(zkClient.readData(path, true));
        JSONObject brokerJson = JSONObject.parseObject(brokerInfo);
        String host = brokerJson.getString("host");
        int port = brokerJson.getIntValue("port");

        if(StringUtils.isBlank(host)){
            //如果没有提供 host 和 port 信息，则从 endpoints 中拿取信息
            JSONArray endpoints = brokerJson.getJSONArray("endpoints");
            String endpoint = endpoints.getString(0);
            Matcher matcher = ipPort.matcher(endpoint);
            if(matcher.find()) {
                host = matcher.group(1);
                port = NumberUtil.toInt(matcher.group(2));
            }
        }

        //读取 zk 设置
        String detail = _getZkServlet().detail(name);

        //写入配置信息到文件
        KafkaConnInfo kafkaConnInfo = new KafkaConnInfo(host + ":" + port, name, version, detail);
        FileUtils.writeStringToFile(new File(kafkaConnDir, name), JSONObject.toJSONString(kafkaConnInfo));

        //如果是新版 kafka,创建元数据目录
        KafkaConnInfo.KafkaVersion kafkaVersion = kafkaConnInfo.getKafkaVersion();
        if (kafkaVersion == KafkaConnInfo.KafkaVersion.NEW) {
            File dir = new File(newKafkaConfigDir, name);
            dir.mkdir();
            newKafkaMonitor.initConsumer(name);
        }
        return 0;
    }

    /**
     * 添加第三方工具监控地址
     *
     * @param name
     * @param address
     * @return
     */
    public int setThirdpartTool(String name, String address) throws IOException {
        KafkaConnInfo kafkaConnInfo = detail(name);
        kafkaConnInfo.setThirdpartTool(address);
        FileUtils.writeStringToFile(new File(kafkaConnDir, name), JSONObject.toJSONString(kafkaConnInfo));
        return 0;
    }

    public String[] connNames() {
        return kafkaConnDir.list();
    }

    public KafkaConnInfo detail(String name) throws IOException {
        return newKafkaMonitor.connInfo(name);
    }

    /**
     * 创建主题
     *
     * @return
     */
    public int createTopic(String name, String topic, String partitions, String replication) throws IOException {
        ZkUtils zkUtils = zkUtils(name);
        newKafkaMonitor.createTopic(zkUtils, topic, NumberUtil.toInt(partitions), NumberUtil.toInt(replication));
        return 0;
    }

    /**
     * 删除主题
     *
     * @param topic
     * @return
     */
    public int deleteTopic(String name, String topic) throws IOException {
        ZkUtils zkUtils = zkUtils(name);
        newKafkaMonitor.deleteTopic(zkUtils, topic);
        return 0;
    }

    /**
     * 所有主题查询
     *
     * @return
     */
    public Map<String, List<ModelTopicAndPartition>> topics(String name) throws IOException {
        ZkUtils zkUtils = zkUtils(name);
        Set<TopicAndPartition> allPartitions = zkUtils.getAllPartitions();
        java.util.Set<TopicAndPartition> topicAndPartitions = JavaConversions.asJavaSet(allPartitions);

        Map<String, List<ModelTopicAndPartition>> modelTopicAndPartitions = new HashMap<String, List<ModelTopicAndPartition>>();
        for (TopicAndPartition topicAndPartition : topicAndPartitions) {
            List<ModelTopicAndPartition> list = modelTopicAndPartitions.get(topicAndPartition.topic());
            if (list == null) {
                list = new ArrayList<ModelTopicAndPartition>();
                modelTopicAndPartitions.put(topicAndPartition.topic(), list);
            }
            list.add(new ModelTopicAndPartition(topicAndPartition));
        }
        return modelTopicAndPartitions;
    }

    /**
     * 分组信息查询
     *
     * @param name
     * @return
     * @throws IOException
     */
    public List<String> groups(String name) throws IOException {
        KafkaConnInfo kafkaConnInfo = detail(name);
        KafkaConnInfo.KafkaVersion kafkaVersion = kafkaConnInfo.getKafkaVersion();
        if (kafkaVersion == KafkaConnInfo.KafkaVersion.NEW) {
            return newKafkaMonitor.groups(name);
        }
        return oldKakaMonitor.groups(zkUtils(name));
    }

    /**
     * 作者:sanri <br/>
     * 时间:2018-10-23上午11:28:12<br/>
     * 功能:消费组订阅的主题列表 <br/>
     *
     * @param group
     * @return
     */
    public List<String> groupSubscribeTopics(String name, String group) throws IOException {
        KafkaConnInfo kafkaConnInfo = detail(name);
        KafkaConnInfo.KafkaVersion kafkaVersion = kafkaConnInfo.getKafkaVersion();
        if (kafkaVersion == KafkaConnInfo.KafkaVersion.NEW) {
            return newKafkaMonitor.groupSubscribeTopics(name, group);
        }
        return oldKakaMonitor.groupSubscribeTopics(zkUtils(name), group);
    }

    /**
     * offset 监控
     *
     * @param group
     * @return
     * @throws IOException
     */
    public List<TopicOffset> autoSelectMonitor(String name, String group) throws IOException {
        KafkaConnInfo kafkaConnInfo = detail(name);
        String thirdpartTool = kafkaConnInfo.getThirdpartTool();

        if (StringUtils.isNotBlank(thirdpartTool)) {
            return offsetThirdpartMonitor.offsetMonitor(name, group);
        }

        KafkaConnInfo.KafkaVersion kafkaVersion = kafkaConnInfo.getKafkaVersion();
        if (kafkaVersion == KafkaConnInfo.KafkaVersion.NEW) {
            return newKafkaMonitor.offsetMonitor(name, group);
        }
        return oldKakaMonitor.offsetMonitor(zkUtils(name), group);
    }

    /**
     * 作者:sanri <br/>
     * 时间:2018-10-25下午2:21:16<br/>
     * 功能:某一个分组的某一个主题的消费监控 <br/>
     *
     * @param group
     * @param topic
     * @return
     */
    public List<OffsetShow> groupTopicMonitor(String name, String group, String topic) throws IOException {
        KafkaConnInfo kafkaConnInfo = detail(name);
        KafkaConnInfo.KafkaVersion kafkaVersion = kafkaConnInfo.getKafkaVersion();
        if (kafkaVersion == KafkaConnInfo.KafkaVersion.NEW) {
            return newKafkaMonitor.groupTopicMonitor(name, group, topic);
        }
        return oldKakaMonitor.groupTopicMonitor(zkUtils(name), group, topic);
    }

    /**
     * 设置 offset ; 只支持旧版本 kafka
     *
     * @param name
     * @param group
     * @param topic
     * @param partition
     * @param offset
     * @return
     */
    public int editOffset(String name, String group, String topic, String partition, String offset) throws IOException {
        KafkaConnInfo kafkaConnInfo = detail(name);
        KafkaConnInfo.KafkaVersion kafkaVersion = kafkaConnInfo.getKafkaVersion();
        if (kafkaVersion == KafkaConnInfo.KafkaVersion.NEW) {
            throw new IllegalArgumentException("新版本不支持设置偏移量");
        }
        ZkUtils zkUtils = zkUtils(name);
        oldKakaMonitor.editOffset(zkUtils,name, group, topic, NumberUtil.toInt(partition), NumberUtil.toLong(offset));
        return 0;
    }

    /**
     * 总日志大小
     *
     * @param name
     * @param topic
     * @return
     */
    public Map<String, Long> logSizes(String name, String topic) throws IOException {
        KafkaConnInfo kafkaConnInfo = detail(name);
        KafkaConnInfo.KafkaVersion kafkaVersion = kafkaConnInfo.getKafkaVersion();
        if (kafkaVersion == KafkaConnInfo.KafkaVersion.NEW) {
            return newKafkaMonitor.logSizes(name, topic);
        }
        return oldKakaMonitor.logSizes(zkUtils(name), topic);
    }

    /**
     * 停止消费
     * @param name
     * @return
     * @throws IOException
     */
    public int stopConsumer(String name) throws IOException {
        KafkaConnInfo kafkaConnInfo = detail(name);
        KafkaConnInfo.KafkaVersion kafkaVersion = kafkaConnInfo.getKafkaVersion();
        if(kafkaVersion == KafkaConnInfo.KafkaVersion.NEW) {
            newKafkaMonitor.stopConsumer(name);
        }

        return 0;
    }

    /**
     * 消费某一分区最后的数据,最后 100 条
     * @param name
     * @param topic
     * @param partition
     * @param serialize
     * @return
     */
    public Map<String,Object> lastDatas(String name,String topic,String partition,String serialize) throws IOException {
        KafkaConnInfo kafkaConnInfo = detail(name);
        KafkaConnInfo.KafkaVersion kafkaVersion = kafkaConnInfo.getKafkaVersion();
        ZkSerializer zkSerializer = zkSerializerMap.get(serialize);
        Map<String,byte[]> datas = new LinkedHashMap<String, byte[]>();
        if(kafkaVersion == KafkaConnInfo.KafkaVersion.NEW){
            datas = newKafkaMonitor.lastDatas(name, topic, NumberUtil.toInt(partition));
        }else{

        }
        return getStringObjectMap(serialize,datas);
    }

    /**
     * 消费某一分区附近数据; 前 100 条,后 100 条
     * @param name
     * @param group
     * @param topic
     * @param partition
     * @param serialize
     * @return offset => data
     */
    public Map<String,Object> nearbyDatas(String name,String topic,String partition,long offset,String serialize) throws IOException {
        KafkaConnInfo kafkaConnInfo = detail(name);
        KafkaConnInfo.KafkaVersion kafkaVersion = kafkaConnInfo.getKafkaVersion();
        Map<String,byte[]> datas = new LinkedHashMap<String, byte[]>();
        if(kafkaVersion == KafkaConnInfo.KafkaVersion.NEW){
            datas = newKafkaMonitor.nearbyDatas(name, topic, NumberUtil.toInt(partition), offset);
        }else{

        }

        //处理结果,反序列化为对象
        return getStringObjectMap(serialize, datas);
    }

    private Map<String, Object> getStringObjectMap(String serialize, Map<String, byte[]> datas) {
        ZkSerializer zkSerializer = zkSerializerMap.get(serialize);

        Map<String,Object> results = new LinkedHashMap<String, Object>();
        Iterator<String> iterator = datas.keySet().iterator();
        while (iterator.hasNext()){
            String currOffset = iterator.next();
            byte[] data = datas.get(currOffset);
            if(zkSerializer != null){
                results.put(currOffset, zkSerializer.deserialize(data));
            }else{
                results.put(currOffset, new String(Hex.encodeHex(data)));
            }
        }

        return results;
    }

    /**
     * 获取 zk 工具
     *
     * @param name
     * @return
     * @throws IOException
     */
    private ZkUtils zkUtils(String name) throws IOException {
        ZkUtils zkUtils = zkUtilsMap.get(name);
        if (zkUtils == null) {
            ZkClient zkClient = zkClient(name);
            zkUtils = ZkUtils.apply(zkClient, JaasUtils.isZkSecurityEnabled());
            zkUtilsMap.put(name, zkUtils);
        }
        return zkUtils;
    }

    private ZkClient zkClient(String name) throws IOException {
        ZkClient zkClient = zkClientMap.get(name);
        if (zkClient == null) {
            String serverString = _getZkServlet().detail(name);
            zkClient = new ZkClient(serverString, ZkServlet.sessionTimeout, ZkServlet.connectionTimeout, zkSerializer);
            zkClientMap.put(name, zkClient);
        }
        return zkClient;
    }

    private ZkServlet _getZkServlet() {
        return DispatchServlet.getServlet(ZkServlet.class);
    }

}
