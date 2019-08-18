package com.sanri.app.kafka;

import com.sanri.app.BaseServlet;
import com.sanri.app.servlet.DiamondServlet;
import com.sanri.app.servlet.KafkaServlet;
import com.sanri.app.servlet.ZkServlet;
import com.sanri.frame.DispatchServlet;
import kafka.common.OffsetAndMetadata;
import kafka.coordinator.*;
import kafka.utils.ZkUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.PartitionInfo;
import org.apache.kafka.common.TopicPartition;
import sanri.utils.NumberUtil;
import scala.collection.Seq;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.*;

public class NewKafkaMonitor extends BaseKafkaMonitor{
    static Map<String, KafkaConsumer> kafkaConsumerMap = new HashMap<String, KafkaConsumer>();
    // kafka 是否在运行监控，做一个开关控制
    static Map<String,Boolean> kafkaConsumerBoolMap = new HashMap<String, Boolean>();

    //记录当前元数据 offset 文件夹名,不做为分组的一部分
    static final String $offsetDirName = "$~offset";

    static final String metaOffsetName = "__consumer_offsets";
    static final int defaultPartitions = 50;

    /**
     * 查询所有分组列表
     * @param name
     * @return
     * @throws IOException
     */
    public List<String> groups(String name) throws IOException {
        Boolean run = kafkaConsumerBoolMap.get(name);
        if(!run){startConsumer(name);}

        File file = new File(KafkaServlet.newKafkaConfigDir, name);
        String[] list = file.list(); if(list == null){list = new String[0];}
        List<String> results = new ArrayList<String>();
        Collections.addAll(results,list);
        results.remove($offsetDirName);

       return results;
    }

    /**
     * 查看分组订阅的主题列表
     * @param name
     * @param group
     * @return
     */
    public List<String> groupSubscribeTopics(String name,String group){
        File connDir = new File(KafkaServlet.newKafkaConfigDir, name);
        File groupDir = new File(connDir, group);
        return Arrays.asList(groupDir.list());
    }

    /**
     *  查询订阅的主题的分区消费 offset 列表
     * @param name
     * @param group
     * @param topic
     * @return
     */
    public Map<String,Long> offsets(String name,String group,String topic) throws IOException {
        Map<String,Long> results = new LinkedHashMap<String, Long>();
        File topicDir = new File(KafkaServlet.newKafkaConfigDir, name + "/" + group + "/" + topic);
        File[] files = topicDir.listFiles();
        for (File file : files) {
            String partition = file.getName();
            String offset = FileUtils.readFileToString(file);
            results.put(partition,NumberUtil.toLong(offset));
        }
        return results;
    }

    /**
     * 查看主题分区数
     * @param name
     * @param topic
     * @return
     */
    public int partitions(String name,String topic){
        KafkaConsumer kafkaConsumer = kafkaConsumerMap.get(name);
        List list = kafkaConsumer.partitionsFor(topic);
        return list.size();
    }


    /**
     * 查看 logSize
     * @param name
     * @param topic
     * @param partition
     * @return
     */
    public long logSize(String name,String topic,int partition) throws IOException {
//        KafkaConsumer kafkaConsumer = kafkaConsumerMap.get(name);
//        return kafkaConsumer.position(new TopicPartition(topic,partition));
        KafkaConsumer<byte[], byte[]> consumer = createConsumer(name);
        TopicPartition topicPartition = new TopicPartition(topic, partition);
        consumer.assign(Collections.singletonList(topicPartition));
        long position = consumer.endOffsets(Collections.singletonList(topicPartition)).get(topicPartition);
        consumer.close();
        return position;
    }

    public Map<String,Long> logSizes(String name,String topic) throws IOException {
        KafkaConsumer<byte[], byte[]> consumer = createConsumer(name);

        List<TopicPartition> topicPartitions = new ArrayList<TopicPartition>();

        // partitionsFor 导致卡死，改为从 zk 上获取 2018/7/29
//        List<PartitionInfo> partitionInfos = consumer.partitionsFor(topic);
//        for (PartitionInfo partitionInfo : partitionInfos) {
//            TopicPartition topicPartition = new TopicPartition(topic, partitionInfo.partition());
//            topicPartitions.add(topicPartition);
//        }
        int partitions = partitionsFromZk(name,topic);
        for (int i = 0; i < partitions; i++) {
            TopicPartition topicPartition = new TopicPartition(topic, i);
            topicPartitions.add(topicPartition);
        }

        consumer.assign(topicPartitions);

        Map<TopicPartition, Long> topicPartitionLongMap = consumer.endOffsets(topicPartitions);
        consumer.close();

        Map<String,Long> logSizes = new HashMap<String, Long>();
        Iterator<Map.Entry<TopicPartition, Long>> iterator = topicPartitionLongMap.entrySet().iterator();
        while (iterator.hasNext()){
            Map.Entry<TopicPartition, Long> entry = iterator.next();
            TopicPartition topicPartition = entry.getKey();
            Long value = entry.getValue();
            logSizes.put(topicPartition.partition()+"",value);
        }
        return logSizes;
    }

    /**
     * 从 zk 中拿取分区数
     * @param name
     * @param topic
     * @return
     * @throws IOException
     */
    private int partitionsFromZk(String name, String topic) throws IOException {
        ZkUtils zkUtils = KafkaServlet.zkUtilsMap.get(name);
        Seq<String> children = zkUtils.getChildren("/brokers/topics/" + topic + "/partitions");
        return children.length();
    }

    /**
     * 初始化消费者
     * @param name
     * @throws IOException
     */
    public void initConsumer(String name) throws IOException {
        kafkaConsumerBoolMap.put(name,false);
        KafkaConsumer<byte[], byte[]> consumer = createConsumer(name);
        kafkaConsumerMap.put(name,consumer);
    }

    /**
     * 停止消费
     * @param name
     */
    public void stopConsumer(String name){
        kafkaConsumerBoolMap.put(name,false);
    }

    /**
     * 开启消费
     * @param name
     * @throws IOException
     */
    public void startConsumer(final String name) throws IOException {
        stopConsumer(name);

        kafkaConsumerBoolMap.put(name,true);
        final KafkaConsumer consumer = kafkaConsumerMap.get(name);

        final File connDir = new File(KafkaServlet.newKafkaConfigDir, name);

        //监控元数据 50 个分区数据情况
        List<TopicPartition> topicPartitions = new ArrayList<TopicPartition>();
        for (int i=0;i<defaultPartitions;i++) {
            TopicPartition topicPartition = new TopicPartition(metaOffsetName,i);
            topicPartitions.add(topicPartition);
        }

        //指定消费某个主题的某个分区
        consumer.assign(topicPartitions);

        //对每个分区找到以前读的位置开始读取; 然后在读取的过程中记住位置,方便后面读取
        final Map<Integer,Long> metaOffsetChanges = new HashMap<Integer, Long>();
        final File metaOffsetDir = new File(connDir, $offsetDirName);
        if(ArrayUtils.isNotEmpty(metaOffsetDir.list())) {
            for (int i = 0; i < defaultPartitions; i++) {
                TopicPartition topicPartition = topicPartitions.get(i);
                File metaOffsetFile = new File(metaOffsetDir, i + "");
                long lastOffset = NumberUtil.toLong(FileUtils.readFileToString(metaOffsetFile), 0);
                metaOffsetChanges.put(i,lastOffset);
                consumer.seek(topicPartition,lastOffset);
            }
        }

        new Thread(){
            @Override
            public void run() {
                while (true) {
                    Boolean run = kafkaConsumerBoolMap.get(name);
                    if(!run){
                        //对每个分区写入新的 offset ,然后停止
                        Iterator<Map.Entry<Integer, Long>> iterator = metaOffsetChanges.entrySet().iterator();
                        while (iterator.hasNext()){
                            Map.Entry<Integer, Long> entry = iterator.next();
                            Integer key = entry.getKey();
                            Long value = entry.getValue();
                            try {
                                FileUtils.writeStringToFile(new File(metaOffsetDir,key+""),value+"");
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                        break;
                    }

                    ConsumerRecords<byte[], byte[]> consumerRecords = consumer.poll(100);
                    Iterable<ConsumerRecord<byte[], byte[]>> records = consumerRecords.records(metaOffsetName);
                    Iterator<ConsumerRecord<byte[], byte[]>> iterator = records.iterator();
                    while (iterator.hasNext()){
                        ConsumerRecord<byte[], byte[]> consumerRecord = iterator.next();
                        int metaPartition = consumerRecord.partition();
                        long metaOffset = consumerRecord.offset();
                        //记录每个分区新的 offset
                        metaOffsetChanges.put(metaPartition,metaOffset);

                        try {
                            handlerConsumerRecord(connDir, consumerRecord);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }

                }
            }
        }.start();

    }

    /**
     * @param connDir
     * @param consumerRecord
     * @throws IOException
     */
    private void handlerConsumerRecord(File connDir, ConsumerRecord<byte[], byte[]> consumerRecord) throws IOException {
        //处理每个分组,每个主题,每个分区的 offset
        ByteBuffer keyBuffer = ByteBuffer.wrap(consumerRecord.key());
        BaseKey baseKey = GroupMetadataManager.readMessageKey(keyBuffer);
        if(baseKey instanceof OffsetKey){
            GroupTopicPartition groupTopicPartition = (GroupTopicPartition) baseKey.key();

            String group = groupTopicPartition.group();
            String topic = groupTopicPartition.topicPartition().topic();
            int partition = groupTopicPartition.topicPartition().partition();

            //创建 group,topic ,partition 文件目录
            File groupDir = new File(connDir, group);
            File topicDir = new File(groupDir,topic);
            File partitionFile = new File(topicDir,partition+"");
            if(!groupDir.exists()){groupDir.mkdir();}
            if(!topicDir.exists()){topicDir.mkdir();}

            //获取 offset value
            byte[] value = consumerRecord.value();
            if(value != null) {
                OffsetAndMetadata offsetAndMetadata = GroupMetadataManager.readOffsetMessageValue(ByteBuffer.wrap(value));
                long offset = offsetAndMetadata.offset();

                FileUtils.writeStringToFile(partitionFile, offset + "");
            }
        }else if(baseKey instanceof GroupMetadataKey){

        }
    }

    /**
     * 创建消费者
     * @param name
     * @return
     * @throws IOException
     */
    private KafkaConsumer<byte[], byte[]> createConsumer(String name) throws IOException {
        //读取 broker 地址
        String broker = broker(name);

        Properties props = new Properties();
        props.put("bootstrap.servers", broker);
        //每个消费者分配独立的组号
        props.put("group.id", "console-"+name);
        //如果value合法，则自动提交偏移量
        props.put("enable.auto.commit", "true");
        //设置多久一次更新被消费消息的偏移量
        props.put("auto.commit.interval.ms", "1000");
        //设置会话响应的时间，超过这个时间kafka可以选择放弃消费或者消费下一条消息
        props.put("session.timeout.ms", "30000");
        //该参数表示从头开始消费该主题
        props.put("auto.offset.reset", "earliest");
        //注意反序列化方式为ByteArrayDeserializer
        props.put("key.deserializer","org.apache.kafka.common.serialization.ByteArrayDeserializer");
        props.put("value.deserializer","org.apache.kafka.common.serialization.ByteArrayDeserializer");
        return new KafkaConsumer<byte[], byte[]>(props);
    }

    /**
     *  @param name
     * @param group
     */
    public List<TopicOffset> offsetMonitor(String name, String group) throws IOException {
        Boolean run = kafkaConsumerBoolMap.get(name);
        if(!run){startConsumer(name);}

        File connDir = new File(KafkaServlet.newKafkaConfigDir, name);
        File groupDir = new File(connDir, group);
        if(!groupDir.exists()){
            return null;
        }

        List<TopicPartition> topicPartitions = new ArrayList<TopicPartition>();
        Map<String,TopicOffset> topicOffsetMap = new HashMap<String,TopicOffset>();

        File[] topicDirs = groupDir.listFiles();
        for (File topicDir : topicDirs) {
            String topic = topicDir.getName();
            File[] partitionFiles = topicDir.listFiles();

            TopicOffset topicOffset = new TopicOffset(group, topic, partitionFiles.length);
            topicOffsetMap.put(topic,topicOffset);
            for (File partitionFile : partitionFiles) {
                String partition = partitionFile.getName();
                topicPartitions.add(new TopicPartition(topic,NumberUtil.toInt(partition)));
            }
        }

        //获取 logSize
        KafkaConsumer<byte[], byte[]> consumer = createConsumer(name);
        Map<TopicPartition, Long> topicPartitionLongMap = consumer.endOffsets(topicPartitions);
        Iterator<Map.Entry<TopicPartition, Long>> iterator = topicPartitionLongMap.entrySet().iterator();
        while (iterator.hasNext()){
            Map.Entry<TopicPartition, Long> entry = iterator.next();
            TopicPartition topicPartition = entry.getKey();
            Long logSize = entry.getValue();
            String topic = topicPartition.topic();
            int partition = topicPartition.partition();

            String offset = FileUtils.readFileToString(new File(groupDir,topic+"/"+partition));
            OffsetShow offsetShow = new OffsetShow(topic, partition,NumberUtil.toLong(offset),logSize);
            topicOffsetMap.get(topic).addPartitionOffset(offsetShow);
        }

        //总偏移量计算
        Iterator<TopicOffset> topicOffsetIterator = topicOffsetMap.values().iterator();
        while (topicOffsetIterator.hasNext()){
            TopicOffset topicOffset = topicOffsetIterator.next();
            topicOffset.totalLagCalc();
        }

        return new ArrayList<TopicOffset>(topicOffsetMap.values());
    }

    /**
     *
     * 作者:sanri <br/>
     * 时间:2018-10-25下午2:21:16<br/>
     * 功能:某一个分组的某一个主题的消费监控 <br/>
     * @param group
     * @param topic
     * @return
     */
    public  List<OffsetShow> groupTopicMonitor(String name,String group,String topic) throws IOException {
        Boolean run = kafkaConsumerBoolMap.get(name);
        if(!run){startConsumer(name);}

        File connDir = new File(KafkaServlet.newKafkaConfigDir, name);
        File topicDir = new File(new File(connDir, group),topic);

        List<TopicPartition> topicPartitions = new ArrayList<TopicPartition>();
        Map<String,String> partitionOffset = new HashMap<String, String>();

        File[] partitionFiles = topicDir.listFiles();
        for (File partitionFile : partitionFiles) {
            String partitionFileName = partitionFile.getName();
            topicPartitions.add(new TopicPartition(topic,NumberUtil.toInt(partitionFileName)));
            partitionOffset.put(partitionFileName,FileUtils.readFileToString(partitionFile));
        }

        KafkaConsumer<byte[], byte[]> consumer = createConsumer(name);
        consumer.assign(topicPartitions);

        List<OffsetShow> offsetShows = new ArrayList<OffsetShow>();

        Map<TopicPartition, Long> topicPartitionLongMap = consumer.endOffsets(topicPartitions);
        Iterator<Map.Entry<TopicPartition, Long>> iterator = topicPartitionLongMap.entrySet().iterator();
        while (iterator.hasNext()){
            Map.Entry<TopicPartition, Long> entry = iterator.next();
            TopicPartition topicPartition = entry.getKey();

            Long logSize = entry.getValue();
            int partition = topicPartition.partition();
            String offset = partitionOffset.get(partition + "");
            offsetShows.add(new OffsetShow(topic,partition,NumberUtil.toLong(offset),logSize));
        }
        return offsetShows;
    }

    /**
     * 消费某一主题数据; 采用 ByteArray 返回
     * @param group
     * @param name
     * @param topic
     * @param partition
     */
    public Map<String,byte[]> nearbyDatas(String name, String topic, int partition, long offset) throws IOException {
        KafkaConsumer<byte[], byte[]> consumer = createConsumer(name);
        TopicPartition topicPartition = new TopicPartition(topic, partition);
        consumer.assign(Collections.singletonList(topicPartition));
        // 查询前 100 条,后 100 条
        long seekOffset = offset - 100;
        if(seekOffset < 0){seekOffset = 0;}

        consumer.seek(topicPartition,seekOffset);

        Map<TopicPartition, Long> endOffsets = consumer.endOffsets(Collections.singletonList(topicPartition));
        Long endOffset = endOffsets.get(topicPartition);
        long seekEndOffset = offset + 100;
        if(seekEndOffset > endOffset){seekEndOffset = endOffset;}

        Map<String,byte[]> datas = new LinkedHashMap<String, byte[]>();

        while(true) {
            ConsumerRecords<byte[], byte[]> consumerRecords = consumer.poll(100);
            List<ConsumerRecord<byte[], byte[]>> records = consumerRecords.records(topicPartition);
            long currOffset = seekOffset;
            if(CollectionUtils.isEmpty(records)){
                logger.info("["+name+"]["+topic+"]["+partition+"]["+seekOffset+"]读取到数据量为 0 ");
                break;
            }
            for (ConsumerRecord<byte[], byte[]> record : records) {
                currOffset = record.offset();
                byte[] value = record.value();
                datas.put(currOffset+"",value);
            }
            if(currOffset >= seekEndOffset){
                break;
            }
        }

        consumer.close();

        return datas;
    }

    /**
     * 查询最后 100 条数据
     * @param name
     * @param topic
     * @param partition
     * @return
     */
    public Map<String, byte[]> lastDatas(String name, String topic, int partition) throws IOException {
        KafkaConsumer<byte[], byte[]> consumer = createConsumer(name);
        TopicPartition topicPartition = new TopicPartition(topic, partition);
        consumer.assign(Collections.singletonList(topicPartition));

        Map<TopicPartition, Long> endOffsets = consumer.endOffsets(Collections.singletonList(topicPartition));
        Long endOffset = endOffsets.get(topicPartition);
        long seekOffset =  endOffset - 100;
        if(seekOffset < 0) {seekOffset = 0L;}

        consumer.seek(topicPartition,seekOffset);

         Map<String,byte[]> datas = new LinkedHashMap<String, byte[]>();
         while(true){
             ConsumerRecords<byte[], byte[]> consumerRecords = consumer.poll(100);
             List<ConsumerRecord<byte[], byte[]>> records = consumerRecords.records(topicPartition);
             long currOffset = seekOffset;
             if(CollectionUtils.isEmpty(records)){
                logger.info("["+name+"]["+topic+"]["+partition+"]["+seekOffset+"]读取到数据量为 0 ");
                break;
            }
             for (ConsumerRecord<byte[], byte[]> record : records) {
                 long offset = record.offset();
                 currOffset = offset;

                 byte[] value = record.value();
                 datas.put(offset+"",value);
             }
             if(currOffset >= endOffset){
                 break;
             }
         }

         return datas;
    }
}
