package com.sanri.app.servlet;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.sanri.app.BaseServlet;
import com.sanri.app.kafka.KafkaConnInfo;
import com.sanri.app.kafka.OffsetShow;
import com.sanri.app.kafka.TopicOffset;
import com.sanri.frame.RequestMapping;
import org.I0Itec.zkclient.serialize.ZkSerializer;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.admin.*;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.KafkaFuture;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.TopicPartitionInfo;
import org.apache.kafka.common.config.SaslConfigs;
import sanri.utils.NumberUtil;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.sanri.app.servlet.ZkServlet.zkSerializerMap;

@RequestMapping("/kafka")
public class KafkaServlet extends BaseServlet{

    // 依赖于 zookeeper,filemanager
    private ZkServlet zkServlet;
    private FileManagerServlet fileManagerServlet;
    private static final String modul = "kafka";
    private static final String relativeBrokerPath = "/brokers/ids";

    private static final Map<String, AdminClient> adminClientMap = new HashMap<>();

    //构造注入,目前仅支持注入 servlet
    public KafkaServlet(ZkServlet zkServlet,FileManagerServlet fileManagerServlet){
        this.zkServlet = zkServlet;
        this.fileManagerServlet = fileManagerServlet;
    }

    /**
     * 保存配置,新连接
     * @param zkConn
     * @param kafkaConnInfo
     * @throws IOException
     */
    public int writeConfig(String zkConn, KafkaConnInfo kafkaConnInfo) throws IOException {
        String zkConnStrings = fileManagerServlet.readConfig(ZkServlet.modul, zkConn);
        kafkaConnInfo.setZkConnectStrings(zkConnStrings);
        kafkaConnInfo.setClusterName(zkConn);
        String chroot = kafkaConnInfo.getChroot();
        fileManagerServlet.writeConfig(modul,zkConn, JSONObject.toJSONString(kafkaConnInfo));
        return 0;
    }

    /**
     * 读取 kafka 连接配置
     * @param clusterName
     * @return
     * @throws IOException
     */
    public KafkaConnInfo readConfig(String clusterName) throws IOException {
        String kafkaConnInfoJson = fileManagerServlet.readConfig(modul,clusterName);
        return JSONObject.parseObject(kafkaConnInfoJson,KafkaConnInfo.class);
    }

    /**
     * 获取所有的节点连接信息
     * @param clusterName
     * @return
     * @throws IOException
     */
    public Collection<String> brokers(String clusterName) throws IOException {
        return brokers(readConfig(clusterName)).values();
    }

    /**
     * 查询所有分组
     * @param clusterName
     * @return
     * @throws IOException
     * @throws ExecutionException
     * @throws InterruptedException
     */
    public List<String> groups(String clusterName) throws IOException, ExecutionException, InterruptedException {
        AdminClient adminClient = loadAdminClient(clusterName);
        List<String> groupNames = new ArrayList<>();

        ListConsumerGroupsResult listConsumerGroupsResult = adminClient.listConsumerGroups();
        Collection<ConsumerGroupListing> consumerGroupListings = listConsumerGroupsResult.all().get();
        for (ConsumerGroupListing consumerGroupListing : consumerGroupListings) {
            String groupId = consumerGroupListing.groupId();
            groupNames.add(groupId);
        }
        return groupNames;
    }

    /**
     * 所有主题查询
     *
     * @return
     */
    public Map<String, Integer> topics(String clusterName) throws IOException, ExecutionException, InterruptedException {
        Map<String, Integer> result = new HashMap<>();

        AdminClient adminClient = loadAdminClient(clusterName);

        ListTopicsResult listTopicsResult = adminClient.listTopics();
        Set<String> topics = listTopicsResult.names().get();
        DescribeTopicsResult describeTopicsResult = adminClient.describeTopics(topics);
        Map<String, KafkaFuture<TopicDescription>> values = describeTopicsResult.values();
        Iterator<Map.Entry<String, KafkaFuture<TopicDescription>>> iterator = values.entrySet().iterator();
        while (iterator.hasNext()){
            Map.Entry<String, KafkaFuture<TopicDescription>> topicDescriptionEntry = iterator.next();
            String topic = topicDescriptionEntry.getKey();
            TopicDescription topicDescription = topicDescriptionEntry.getValue().get();
            List<TopicPartitionInfo> partitions = topicDescription.partitions();
            result.put(topic,partitions.size());
        }
        return result;
    }

    public int topicPartitions(String clusterName,String topic) throws IOException, ExecutionException, InterruptedException {
        AdminClient adminClient = loadAdminClient(clusterName);
        DescribeTopicsResult describeTopicsResult = adminClient.describeTopics(Collections.singletonList(topic));
        TopicDescription topicDescription = describeTopicsResult.values().get(topic).get();
        return topicDescription.partitions().size();
    }

    /**
     * 查询分组订阅的主题列表
     * @param clusterName
     * @param group
     * @return
     * @throws IOException
     */
    public Set<String> groupSubscribeTopics(String clusterName, String group) throws IOException, ExecutionException, InterruptedException {
        AdminClient adminClient = loadAdminClient(clusterName);
        Set<String> subscribeTopics = new HashSet<>();

        DescribeConsumerGroupsResult describeConsumerGroupsResult = adminClient.describeConsumerGroups(Collections.singletonList(group));
        Map<String, KafkaFuture<ConsumerGroupDescription>> stringKafkaFutureMap = describeConsumerGroupsResult.describedGroups();
        ConsumerGroupDescription consumerGroupDescription = stringKafkaFutureMap.get(group).get();
        Collection<MemberDescription> members = consumerGroupDescription.members();
        for (MemberDescription member : members) {
            MemberAssignment assignment = member.assignment();
            Set<TopicPartition> topicPartitions = assignment.topicPartitions();
            Iterator<TopicPartition> iterator = topicPartitions.iterator();
            while (iterator.hasNext()){
                TopicPartition topicPartition = iterator.next();
                subscribeTopics.add(topicPartition.topic());
            }
        }
        return subscribeTopics;
    }

    /**
     * 消费组订阅主题消费情况查询
     * @param clusterName
     * @param group
     * @return
     * @throws IOException
     */
    public List<TopicOffset> groupSubscribeTopicsMonitor(String clusterName, String group) throws IOException, ExecutionException, InterruptedException {
        AdminClient adminClient = loadAdminClient(clusterName);

        //创建 KafkaConsumer 来获取 offset ,lag,logsize
        Properties properties = kafkaProperties(clusterName);
        KafkaConsumer<byte[], byte[]> consumer = new KafkaConsumer<byte[], byte[]>(properties);

        List<TopicPartition> topicPartitionsQuery = new ArrayList<>();
        DescribeConsumerGroupsResult describeConsumerGroupsResult = adminClient.describeConsumerGroups(Collections.singletonList(group));
        Map<String, KafkaFuture<ConsumerGroupDescription>> stringKafkaFutureMap = describeConsumerGroupsResult.describedGroups();
        ConsumerGroupDescription consumerGroupDescription = stringKafkaFutureMap.get(group).get();
        Collection<MemberDescription> members = consumerGroupDescription.members();
        List<TopicPartition> allTopicPartition = new ArrayList<>();
        for (MemberDescription member : members) {
            MemberAssignment assignment = member.assignment();
            Set<TopicPartition> topicPartitions = assignment.topicPartitions();
            allTopicPartition.addAll(topicPartitions);
        }

        //查询  offset 信息
        Map<TopicPartition, OffsetAndMetadata> offsetAndMetadataMap = adminClient.listConsumerGroupOffsets(group).partitionsToOffsetAndMetadata().get();

        Map<TopicPartition, Long> topicPartitionLongMap = consumer.endOffsets(allTopicPartition);
        Iterator<Map.Entry<TopicPartition, Long>> iterator = topicPartitionLongMap.entrySet().iterator();
        Map<String,TopicOffset> topicOffsets = new HashMap<>();
        while (iterator.hasNext()){
            Map.Entry<TopicPartition, Long> entry = iterator.next();
            TopicPartition topicPartition = entry.getKey();
            String topic = topicPartition.topic();
            int partition = topicPartition.partition();
            Long logSize = entry.getValue();
            long offset = offsetAndMetadataMap.get(topicPartition).offset();
            long lag = logSize - offset;
//            long offset = consumer.position(topicPartition);
//            long lag = logSize - offset;

            TopicOffset topicOffset = topicOffsets.get(topic);
            if(topicOffset == null){
                topicOffset = new TopicOffset(group,topic);
                topicOffsets.put(topic,topicOffset);
            }
            topicOffset.addPartitionOffset(new OffsetShow(topic,partition,offset,logSize));
        }
        return new ArrayList<>(topicOffsets.values());
    }

    /**
     * 消费组主题信息监控
     * @param clusterName
     * @param group
     * @param topic
     * @return
     * @throws IOException
     * @throws ExecutionException
     * @throws InterruptedException
     */
    public List<OffsetShow> groupTopicMonitor(String clusterName, String group, String topic) throws IOException, ExecutionException, InterruptedException {
        List<OffsetShow> offsetShows = new ArrayList<>();
        AdminClient adminClient = loadAdminClient(clusterName);
        Properties properties = kafkaProperties(clusterName);
        KafkaConsumer<byte[], byte[]> consumer = new KafkaConsumer<>(properties);

        DescribeTopicsResult describeTopicsResult = adminClient.describeTopics(Collections.singletonList(topic));
        TopicDescription topicDescription = describeTopicsResult.values().get(topic).get();
        List<TopicPartitionInfo> partitions = topicDescription.partitions();
        List<TopicPartition> topicPartitions = new ArrayList<>();
        for (TopicPartitionInfo partition : partitions) {
            TopicPartition topicPartition = new TopicPartition(topic, partition.partition());
            topicPartitions.add(topicPartition);
        }

        //查询  offset 信息
        ListConsumerGroupOffsetsOptions listConsumerGroupOffsetsOptions = new ListConsumerGroupOffsetsOptions();
        listConsumerGroupOffsetsOptions.topicPartitions(topicPartitions);

        Map<TopicPartition, OffsetAndMetadata> offsetAndMetadataMap = adminClient.listConsumerGroupOffsets(group,listConsumerGroupOffsetsOptions).partitionsToOffsetAndMetadata().get();

        Map<TopicPartition, Long> topicPartitionLongMap = consumer.endOffsets(topicPartitions);
        Iterator<Map.Entry<TopicPartition, Long>> iterator = topicPartitionLongMap.entrySet().iterator();
        while (iterator.hasNext()){
            Map.Entry<TopicPartition, Long> entry = iterator.next();
            TopicPartition topicPartition = entry.getKey();
            Long logSize = entry.getValue();
//            long offset = consumer.position(topicPartition);
            OffsetAndMetadata offsetAndMetadata = offsetAndMetadataMap.get(topicPartition);
            long offset = offsetAndMetadata.offset();
            int partition = topicPartition.partition();
            long lag = logSize - offset;
            OffsetShow offsetShow = new OffsetShow(topic, partition, offset, logSize);
            offsetShows.add(offsetShow);
        }

        Collections.sort(offsetShows);
        return offsetShows;
    }

    public Map<String, Long> logSizes(String clusterName, String topic) throws IOException, ExecutionException, InterruptedException {
        Map<String, Long> results = new HashMap<>();
        int partitions = topicPartitions(clusterName, topic);

        Properties properties = kafkaProperties(clusterName);
        KafkaConsumer<byte[], byte[]> consumer = new KafkaConsumer<>(properties);

        List<TopicPartition> topicPartitions = new ArrayList<>();
        for (int i = 0; i < partitions; i++) {
            topicPartitions.add(new TopicPartition(topic,i));
        }

        Map<TopicPartition, Long> topicPartitionLongMap = consumer.endOffsets(topicPartitions);
        Iterator<Map.Entry<TopicPartition, Long>> iterator = topicPartitionLongMap.entrySet().iterator();
        while (iterator.hasNext()){
            Map.Entry<TopicPartition, Long> entry = iterator.next();
            TopicPartition topicPartition = entry.getKey();
            Long logSize = entry.getValue();

            results.put(topicPartition.partition()+"",logSize);
        }

        return results;
    }

    /**
     * 消费某一分区最后的数据,最后 100 条
     * @param name
     * @param topic
     * @param partition
     * @param serialize
     * @return
     */
    public Map<String,Object> lastDatas(String clusterName,String topic,int partition,String serialize) throws IOException {
        Properties properties = kafkaProperties(clusterName);
        KafkaConsumer<byte[], byte[]> consumer = new KafkaConsumer<byte[], byte[]>(properties);
        Map<String,Object> datas = new LinkedHashMap<String,Object>();
        try {
            TopicPartition topicPartition = new TopicPartition(topic, partition);
            consumer.assign(Collections.singletonList(topicPartition));

            Map<TopicPartition, Long> endOffsets = consumer.endOffsets(Collections.singletonList(topicPartition));
            Long endOffset = endOffsets.get(topicPartition);
            long seekOffset =  endOffset - 100;
            if(seekOffset < 0) {seekOffset = 0L;}

            consumer.seek(topicPartition,seekOffset);

            ZkSerializer zkSerializer = zkSerializerMap.get(serialize);
            while (true) {
                ConsumerRecords<byte[], byte[]> consumerRecords = consumer.poll(100);
                List<ConsumerRecord<byte[], byte[]>> records = consumerRecords.records(topicPartition);
                long currOffset = seekOffset;
                if (CollectionUtils.isEmpty(records)) {
                    logger.info("[" + clusterName + "][" + topic + "][" + partition + "][" + seekOffset + "]读取到数据量为 0 ");
                    break;
                }
                for (ConsumerRecord<byte[], byte[]> record : records) {
                    long offset = record.offset();
                    currOffset = offset;

                    byte[] value = record.value();
                    Object deserialize = zkSerializer.deserialize(value);
                    datas.put(offset + "", deserialize);
                }
                if (currOffset >= endOffset) {
                    break;
                }
            }
        }finally {
            if(consumer != null)
                consumer.close();
        }

        return datas;
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
    public Map<String,Object> nearbyDatas(String clusterName,String topic,int partition,long offset,String serialize) throws IOException {
        Properties properties = kafkaProperties(clusterName);
        KafkaConsumer<byte[], byte[]> consumer = new KafkaConsumer<byte[], byte[]>(properties);
        Map<String, Object> datas = new LinkedHashMap<String,Object>();
        try {
            TopicPartition topicPartition = new TopicPartition(topic, partition);
            consumer.assign(Collections.singletonList(topicPartition));
            // 查询前 100 条,后 100 条
            long seekOffset = offset - 100;
            if (seekOffset < 0) {
                seekOffset = 0;
            }

            consumer.seek(topicPartition, seekOffset);

            Map<TopicPartition, Long> endOffsets = consumer.endOffsets(Collections.singletonList(topicPartition));
            Long endOffset = endOffsets.get(topicPartition);
            long seekEndOffset = offset + 100;
            if (seekEndOffset > endOffset) {
                seekEndOffset = endOffset;
            }
            ZkSerializer zkSerializer = zkSerializerMap.get(serialize);
            while (true) {
                ConsumerRecords<byte[], byte[]> consumerRecords = consumer.poll(100);
                List<ConsumerRecord<byte[], byte[]>> records = consumerRecords.records(topicPartition);
                long currOffset = seekOffset;
                if (CollectionUtils.isEmpty(records)) {
                    logger.info("[" + clusterName + "][" + topic + "][" + partition + "][" + seekOffset + "]读取到数据量为 0 ");
                    break;
                }
                for (ConsumerRecord<byte[], byte[]> record : records) {
                    currOffset = record.offset();
                    byte[] value = record.value();
                    Object deserialize = zkSerializer.deserialize(value);
                    datas.put(currOffset + "", deserialize);
                }
                if (currOffset >= seekEndOffset) {
                    break;
                }
            }
        }finally {
            if(consumer != null)
                consumer.close();
        }
        return datas;
    }

//    private Collection<DescribeLogDirsResponse.LogDirInfo> logDirsInfosxxx(String clusterName) throws IOException, InterruptedException, ExecutionException {
//        AdminClient adminClient = loadAdminClient(clusterName);
//
//        Map<String, String> brokers = brokers(readConfig(clusterName));
//        List<String> brokerIds = new ArrayList<>(brokers.keySet());
//        Integer firstBrokerId = NumberUtil.toInt(brokerIds.get(0));
//
//        DescribeLogDirsResult describeLogDirsResult = adminClient.describeLogDirs(Collections.singletonList(firstBrokerId));
//        Map<Integer, KafkaFuture<Map<String, DescribeLogDirsResponse.LogDirInfo>>> values = describeLogDirsResult.values();
//        Map<String, DescribeLogDirsResponse.LogDirInfo> stringLogDirInfoMap = values.get(firstBrokerId).get();
//        return stringLogDirInfoMap.values();
//    }

    static Pattern ipPort = Pattern.compile("(\\d+\\.\\d+\\.\\d+\\.\\d+):(\\d+)");
    private Map<String,String> brokers(KafkaConnInfo kafkaConnInfo) throws IOException {
        Map<String,String> brokers = new HashMap<>();
        String clusterName = kafkaConnInfo.getClusterName();
        String chroot = kafkaConnInfo.getChroot();

        List<String> childrens = zkServlet.childrens(clusterName, chroot + relativeBrokerPath);
        for (String children : childrens) {
            String brokerInfo = Objects.toString(zkServlet.readData(clusterName, chroot + relativeBrokerPath + "/" + children, "string"),"");
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

            brokers.put(children,host+":"+port);
        }
        return brokers;
    }

    AdminClient loadAdminClient(String clusterName) throws IOException {
        AdminClient adminClient = adminClientMap.get(clusterName);
        if(adminClient == null){
            Properties properties = kafkaProperties(clusterName);

            adminClient =  KafkaAdminClient.create(properties);
            adminClientMap.put(clusterName,adminClient);
        }

        return adminClient;
    }

    private Properties kafkaProperties(String clusterName) throws IOException {
        KafkaConnInfo kafkaConnInfo = readConfig(clusterName);

        Properties properties = createDefaultConfig(clusterName);

        //从 zk 中拿到 bootstrapServers
        Collection<String> brokers = brokers(kafkaConnInfo).values();
        String bootstrapServers = StringUtils.join(brokers,',');
        properties.put("bootstrap.servers", bootstrapServers);

        if(StringUtils.isNotBlank(kafkaConnInfo.getSaslMechanism())) {
            properties.put(SaslConfigs.SASL_MECHANISM, kafkaConnInfo.getSaslMechanism());
        }
        if(StringUtils.isNotBlank(kafkaConnInfo.getLoginModel())) {
            properties.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SASL_PLAINTEXT");
            properties.put("sasl.jaas.config",kafkaConnInfo.getLoginModel()+" username=\""+kafkaConnInfo.getUsername()+"\" password=\""+kafkaConnInfo.getPassword()+"\";");
        }
        return properties;
    }


    private Properties createDefaultConfig(String clusterName) {
        Properties properties = new Properties();
        final String consumerGroup = "console-"+clusterName;
        //每个消费者分配独立的组号
        properties.put("group.id", consumerGroup);
        //如果value合法，则自动提交偏移量
        properties.put("enable.auto.commit", "true");
        //设置多久一次更新被消费消息的偏移量
        properties.put("auto.commit.interval.ms", "1000");
        //设置会话响应的时间，超过这个时间kafka可以选择放弃消费或者消费下一条消息
        properties.put("session.timeout.ms", "30000");
        //该参数表示从头开始消费该主题
        properties.put("auto.offset.reset", "earliest");
        //注意反序列化方式为ByteArrayDeserializer
        properties.put("key.deserializer","org.apache.kafka.common.serialization.ByteArrayDeserializer");
        properties.put("value.deserializer","org.apache.kafka.common.serialization.ByteArrayDeserializer");
        return properties;
    }
}