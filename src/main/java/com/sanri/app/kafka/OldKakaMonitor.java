package com.sanri.app.kafka;

import com.sanri.app.ConfigCenter;
import com.sanri.app.servlet.KafkaServlet;
import kafka.admin.AdminUtils;
import kafka.api.OffsetRequest;
import kafka.api.PartitionOffsetRequestInfo;
import kafka.cluster.Broker;
import kafka.cluster.BrokerEndPoint;
import kafka.cluster.EndPoint;
import kafka.common.TopicAndPartition;
import kafka.javaapi.OffsetResponse;
import kafka.javaapi.TopicMetadataRequest;
import kafka.javaapi.TopicMetadataResponse;
import kafka.javaapi.consumer.SimpleConsumer;
import kafka.utils.ZkUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.PartitionInfo;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.requests.MetadataResponse.PartitionMetadata;
import org.apache.kafka.common.requests.MetadataResponse.TopicMetadata;
import org.apache.kafka.common.security.JaasUtils;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.data.Stat;
import sanri.utils.NumberUtil;
import scala.Option;
import scala.Tuple2;
import scala.collection.JavaConversions;
import scala.collection.Seq;

import java.util.*;
import java.util.Map.Entry;

/**
 * 旧版本 kafka 监控
 */
public class OldKakaMonitor extends BaseKafkaMonitor{
    private static final int socketTimeout = 10000;
    private static final int bufferSize = 64 * 1024;
    /**
     * 获取所有分组
     * @param name
     * @return
     */
    public List<String> groups(ZkUtils zkUtils){
        Seq<String> consumerGroups = zkUtils.getConsumerGroups();
        return JavaConversions.asJavaList(consumerGroups);
    }

    /**
     *
     * 作者:sanri <br/>
     * 时间:2018-10-22下午4:11:53<br/>
     * 功能:分组订阅的主题列表 <br/>
     *
     * @param group
     * @return
     */
    public List<String> groupSubscribeTopics(ZkUtils zkUtils,String group) {
        Seq<String> topicsByConsumerGroup = zkUtils.getTopicsByConsumerGroup(group);
        List<String> asJavaList = JavaConversions.asJavaList(topicsByConsumerGroup);
        return asJavaList;
    }

    /**
     *
     * 作者:sanri <br/>
     * 时间:2018-10-22下午7:57:55<br/>
     * 功能:获取主题分区数 <br/>
     *
     * @param topic
     * @return
     */
    public int partitions(ZkUtils zkUtils,String topic) {
        TopicMetadata fetchTopicMetadataFromZk = AdminUtils.fetchTopicMetadataFromZk(topic, zkUtils);
        List<PartitionMetadata> partitionMetadatas = fetchTopicMetadataFromZk.partitionMetadata();
        return partitionMetadatas.size();
    }

    private String getZkData(ZkUtils zkUtils,String currentTopicPartitionOffsetPath) {
        Option<String> data = zkUtils.readDataMaybeNull(currentTopicPartitionOffsetPath)._1;
        if(data.isDefined()){
            return data.get();
        }
        return "";
    }

    public List<TopicOffset> offsetMonitor(ZkUtils zkUtils, String group) {
        //获取消费组的所有主题
        List<String> groupSubscribeTopics = groupSubscribeTopics(zkUtils,group);
        if(CollectionUtils.isEmpty(groupSubscribeTopics))return null;
        //获取消费者查询 offset,logsize
        String consumersPath = ZkUtils.ConsumersPath();

        List<TopicOffset>  topicOffsets = new ArrayList<TopicOffset>();
        for (String topic : groupSubscribeTopics) {
            TopicOffset topicOffset = new TopicOffset(group, topic);
            topicOffsets.add(topicOffset);

            int partitions = partitions(zkUtils,topic);
            topicOffset.setPartitions(partitions);

            //对每一个主题的 offset ,logsize,lag
            long totalLogSize = 0,totalOffset = 0 ,totalLag = 0;
            Map<String, Long> logSizeMap = logSizes(zkUtils, topic);
            Iterator<Entry<String, Long>> iterator = logSizeMap.entrySet().iterator();
            while(iterator.hasNext()){
                Entry<String, Long> endOffsetEntry = iterator.next();
                int partition = NumberUtil.toInt(endOffsetEntry.getKey());
                Long logSize = endOffsetEntry.getValue();

                //每个主题的每个分区的 offset 查询
                StringBuffer offsetPath = new StringBuffer();
                offsetPath.append(consumersPath).append("/").append(group).append("/offsets/").append(topic).append("/").append(partition);
                String offsetString = getZkData(zkUtils,offsetPath.toString());
                long offset = NumberUtil.toLong(offsetString, -1);

                long lag = logSize - offset;
                OffsetShow offsetShow = new OffsetShow(topic, partition, offset, logSize);
                topicOffset.addPartitionOffset(offsetShow);

                //总数计算
                totalLogSize += logSize;
                totalOffset += offset;
            }

            totalLag = totalLogSize - totalOffset;
            topicOffset.setLag(totalLag);
            topicOffset.setLogSize(totalLogSize);
            topicOffset.setOffset(totalOffset);
        }
        return topicOffsets;
    }

    public List<OffsetShow> groupTopicMonitor(ZkUtils zkUtils, String group, String topic) {
        List<OffsetShow> offsetShows = new ArrayList<OffsetShow>();

        String consumersPath = ZkUtils.ConsumersPath();
        int partitions = partitions(zkUtils, topic);

        for (int i=0;i<partitions;i++) {
            TopicAndPartition topicAndPartition = new TopicAndPartition(topic, i);
            SimpleConsumer simpleConsumer = createConsumer(zkUtils,"groupTopicMonitor");

            Map<TopicAndPartition, PartitionOffsetRequestInfo> requestInfo = new HashMap<TopicAndPartition, PartitionOffsetRequestInfo>();
            requestInfo.put(topicAndPartition, new PartitionOffsetRequestInfo(OffsetRequest.LatestTime(), 1));
            kafka.javaapi.OffsetRequest request = new kafka.javaapi.OffsetRequest(requestInfo, OffsetRequest.CurrentVersion(), UUID.randomUUID().toString());
            OffsetResponse response = simpleConsumer.getOffsetsBefore(request);
            long[] offsets = response.offsets(topic, i);
            long logSize = offsets[0];

            requestInfo = new HashMap<TopicAndPartition, PartitionOffsetRequestInfo>();
            requestInfo.put(topicAndPartition, new PartitionOffsetRequestInfo(OffsetRequest.EarliestTime(), 1));
            request = new kafka.javaapi.OffsetRequest(requestInfo, OffsetRequest.CurrentVersion(), UUID.randomUUID().toString());
            response = simpleConsumer.getOffsetsBefore(request);
            offsets = response.offsets(topic, i);
            long minOffset = offsets[0];

            StringBuffer offsetPath = new StringBuffer();
            offsetPath.append(consumersPath).append("/").append(group).append("/offsets/").append(topic).append("/").append(i);
            String currentTopicPartitionOffsetPath = offsetPath.toString();

            Tuple2<Option<String>, Stat> readDataMaybeNull = zkUtils.readDataMaybeNull(currentTopicPartitionOffsetPath);
            Option<String> data = readDataMaybeNull._1;
            long offset = 0,lag = 0;
            if(data.isDefined()){
                offset =  NumberUtil.toLong(data.get());
                lag = logSize - offset;
            }

            Stat stat = readDataMaybeNull._2;
            long mtime = stat.getMtime();


            OffsetShow offsetShow = new OffsetShow(topic, i, offset, logSize);
            offsetShow.setMinOffset(minOffset);
            offsetShow.setModified(mtime);
            offsetShows.add(offsetShow);
        }

        return offsetShows;
    }

    /**
     *
     * 作者:sanri <br/>
     * 时间:2018-10-22下午7:56:06<br/>
     * 功能:获取所有主机列表 <br/>
     *
     * @return
     */
    public Map<Integer, String> brokers(ZkUtils zkUtils) {
        Map<Integer, String> connectionconfigs = new HashMap<Integer, String>();
        Seq<Broker> allBrokersInCluster = zkUtils.getAllBrokersInCluster();
        List<Broker> brokers = JavaConversions.asJavaList(allBrokersInCluster);
        for (Broker broker : brokers) {
            int id = broker.id();
            Seq<EndPoint> endPointSeq = broker.endPoints();
            List<EndPoint> endPoints = JavaConversions.asJavaList(endPointSeq);
            for (EndPoint endPoint : endPoints) {
                String host = endPoint.host();
                int port = endPoint.port();
                connectionconfigs.put(id, host+":"+port);
            }
        }
        return connectionconfigs;
    }

    public Map<String, Long> logSizes(ZkUtils zkUtils, String topic) {
        String clientId = "logSizes_LookUp";
        Map<String, Long> logSizeMap = new HashMap<String, Long>();

        int partitions = partitions(zkUtils, topic);

        Map<TopicAndPartition, PartitionOffsetRequestInfo> requestInfo = new HashMap<TopicAndPartition, PartitionOffsetRequestInfo>();
        for (int i = 0; i < partitions; i++) {
            TopicAndPartition topicAndPartition = new TopicAndPartition(topic, i);
            requestInfo.put(topicAndPartition, new PartitionOffsetRequestInfo(OffsetRequest.LatestTime(), 1));
        }
        kafka.javaapi.OffsetRequest request = new kafka.javaapi.OffsetRequest(requestInfo, OffsetRequest.CurrentVersion(), clientId);
        SimpleConsumer consumer = createConsumer(zkUtils, clientId);
        OffsetResponse response = consumer.getOffsetsBefore(request);

        for (int i = 0; i < partitions; i++) {
            long offset = response.offsets(topic, i)[0];
            logSizeMap.put(i+"",offset);
        }
        return logSizeMap;
    }

    private Map<Integer,BrokerEndPoint> leaders(ZkUtils zkUtils, String topic) {
        String clientId = "Client_" + topic;
        SimpleConsumer consumer = createConsumer(zkUtils, clientId);

        Map<Integer,BrokerEndPoint> brokerEndPointMap = new HashMap<Integer, BrokerEndPoint>();

        TopicMetadataRequest request = new TopicMetadataRequest(Arrays.asList(topic));
        TopicMetadataResponse reponse = consumer.send(request);
        List<kafka.javaapi.TopicMetadata> topicMetadataList = reponse.topicsMetadata();
        for (kafka.javaapi.TopicMetadata topicMetadata : topicMetadataList) {
            List<kafka.javaapi.PartitionMetadata> partitionMetadata = topicMetadata.partitionsMetadata();
            for (kafka.javaapi.PartitionMetadata partitionMetadatum : partitionMetadata) {
                int partitionId = partitionMetadatum.partitionId();
                BrokerEndPoint leader = partitionMetadatum.leader();
                brokerEndPointMap.put(partitionId,leader);
            }
        }

        return brokerEndPointMap;
    }

    /**
     * 偏移量修改
     * @param name
     * @param group
     * @param topic
     * @param partition
     * @param offset
     */
    public void editOffset(ZkUtils zkUtils,String name, String group, String topic, int partition, long offset) {
        String consumersPath = ZkUtils.ConsumersPath();
        StringBuffer offsetPathBuffer = new StringBuffer(consumersPath).append("/").append(group).append("/offsets/").append(topic).append("/").append(partition);
        String offsetPath = offsetPathBuffer.toString();

        zkUtils.updateEphemeralPath(offsetPath, offset+"", ZooDefs.Ids.OPEN_ACL_UNSAFE);
    }

    public SimpleConsumer createConsumer(ZkUtils zkUtils,String clientId){
        Map<Integer, String> brokers = brokers(zkUtils);
        String broker = brokers.get(0);
        String[] split = StringUtils.split(broker,":");
        String host = split[0];
        int port = NumberUtil.toInt(split[1]);

        SimpleConsumer simpleConsumer = new SimpleConsumer(host,port,socketTimeout,bufferSize,clientId);
        return simpleConsumer;
    }
}
