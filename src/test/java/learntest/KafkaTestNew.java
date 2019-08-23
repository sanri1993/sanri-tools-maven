package learntest;

import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.admin.*;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.ConsumerGroupState;
import org.apache.kafka.common.KafkaFuture;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.config.SaslConfigs;
import org.apache.kafka.common.requests.DescribeLogDirsResponse;
import org.junit.Before;
import org.junit.Test;

import java.util.*;
import java.util.concurrent.ExecutionException;

public class KafkaTestNew {

    Properties properties = null;
    @Before
    public void init(){
        properties = new Properties();
        properties.put("bootstrap.servers", "10.101.72.43:9092 ");
        //每个消费者分配独立的组号
        properties.put("group.id", "console-company2");
        //如果value合法，则自动提交偏移量
        properties.put("enable.auto.commit", "true");
        //设置多久一次更新被消费消息的偏移量
        properties.put("auto.commit.interval.ms", "1000");
        //设置会话响应的时间，超过这个时间kafka可以选择放弃消费或者消费下一条消息
        properties.put("session.timeout.ms", "30000");
        //该参数表示从头开始消费该主题
        properties.put("auto.offset.reset", "earliest");
        properties.put("sasl.jaas.config","org.apache.kafka.common.security.plain.PlainLoginModule required username=\"iotbus\" password=\"iotbus-kafka\";");
        properties.put(SaslConfigs.SASL_MECHANISM,"PLAIN");
        properties.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG,"SASL_PLAINTEXT");
        //注意反序列化方式为ByteArrayDeserializer
        properties.put("key.deserializer","org.apache.kafka.common.serialization.ByteArrayDeserializer");
        properties.put("value.deserializer","org.apache.kafka.common.serialization.ByteArrayDeserializer");
    }

    @Test
    public void test() throws ExecutionException, InterruptedException {
        AdminClient adminClient =  KafkaAdminClient.create(properties);

        ListConsumerGroupOffsetsResult listConsumerGroupOffsetsResult1 = adminClient.listConsumerGroupOffsets("");

        //获取所有主题
        ListTopicsResult listTopicsResult = adminClient.listTopics();
        KafkaFuture<Set<String>> names = listTopicsResult.names();
        Set<String> strings = names.get();
        System.out.println(strings);

        //获取消费组
        ListConsumerGroupsResult listConsumerGroupsResult = adminClient.listConsumerGroups();
        Collection<ConsumerGroupListing> consumerGroupListings = listConsumerGroupsResult.all().get();
        for (ConsumerGroupListing consumerGroupListing : consumerGroupListings) {
            String s = consumerGroupListing.groupId();
            System.out.println(s);
        }

        //获取主题 partitions ,是否是内部主题
//        DescribeTopicsResult describeTopicsResult = adminClient.describeTopics(Collections.singletonList("MSG_EVENT_INFOSCREEN"));
//        Map<String, KafkaFuture<TopicDescription>> values = describeTopicsResult.values();
//        TopicDescription topicDescription = values.get("MSG_EVENT_INFOSCREEN").get();

        //获取消费组主题分配信息
        DescribeConsumerGroupsResult describeConsumerGroupsResult = adminClient.describeConsumerGroups(Collections.singletonList("scp-st-information_APP5892"));
        Map<String, KafkaFuture<ConsumerGroupDescription>> stringKafkaFutureMap = describeConsumerGroupsResult.describedGroups();
        ConsumerGroupDescription consumerGroupDescription = stringKafkaFutureMap.get("scp-st-information_APP5892").get();
        Collection<MemberDescription> members = consumerGroupDescription.members();
        ConsumerGroupState consumerGroupState = consumerGroupDescription.state();

        for (MemberDescription member : members) {
            String s = member.clientId();
            String s1 = member.consumerId();
            MemberAssignment assignment = member.assignment();
            Set<TopicPartition> topicPartitions = assignment.topicPartitions();
            Iterator<TopicPartition> iterator = topicPartitions.iterator();
            while (iterator.hasNext()){
                TopicPartition topicPartition = iterator.next();
                System.out.println("assignment:"+topicPartition);
            }
        }

        DescribeLogDirsResult describeLogDirsResult = adminClient.describeLogDirs(Collections.singletonList(1));
        Map<Integer, KafkaFuture<Map<String, DescribeLogDirsResponse.LogDirInfo>>> values = describeLogDirsResult.values();
        Map<String, DescribeLogDirsResponse.LogDirInfo> stringLogDirInfoMap = values.get(1).get();
        Iterator<Map.Entry<String, DescribeLogDirsResponse.LogDirInfo>> iterator1 = stringLogDirInfoMap.entrySet().iterator();
        while (iterator1.hasNext()){
            Map.Entry<String, DescribeLogDirsResponse.LogDirInfo> stringLogDirInfoEntry = iterator1.next();
            String key = stringLogDirInfoEntry.getKey();
            DescribeLogDirsResponse.LogDirInfo logDirInfo = stringLogDirInfoEntry.getValue();
            Map<TopicPartition, DescribeLogDirsResponse.ReplicaInfo> replicaInfos = logDirInfo.replicaInfos;
            Iterator<Map.Entry<TopicPartition, DescribeLogDirsResponse.ReplicaInfo>> entryIterator = replicaInfos.entrySet().iterator();
            while (entryIterator.hasNext()){
                Map.Entry<TopicPartition, DescribeLogDirsResponse.ReplicaInfo> topicPartitionReplicaInfoEntry = entryIterator.next();
                TopicPartition topicPartition = topicPartitionReplicaInfoEntry.getKey();
                String topic = topicPartition.topic();
                int partition = topicPartition.partition();
                DescribeLogDirsResponse.ReplicaInfo replicaInfo = topicPartitionReplicaInfoEntry.getValue();
                long size = replicaInfo.size;
                long offsetLag = replicaInfo.offsetLag;
                System.out.println(topic+":"+partition+":"+offsetLag+":"+size);
            }
        }


        //获取  topic,partition,offset
        ListConsumerGroupOffsetsResult listConsumerGroupOffsetsResult = adminClient.listConsumerGroupOffsets("scp-st-information_APP5892");
        Map<TopicPartition, OffsetAndMetadata> topicPartitionOffsetAndMetadataMap = listConsumerGroupOffsetsResult.partitionsToOffsetAndMetadata().get();
        Iterator<Map.Entry<TopicPartition, OffsetAndMetadata>> iterator = topicPartitionOffsetAndMetadataMap.entrySet().iterator();
        while (iterator.hasNext()){
            Map.Entry<TopicPartition, OffsetAndMetadata> topicPartitionOffsetAndMetadataEntry = iterator.next();
            TopicPartition topicPartition = topicPartitionOffsetAndMetadataEntry.getKey();
            OffsetAndMetadata offsetAndMetadata = topicPartitionOffsetAndMetadataEntry.getValue();
            String topic = topicPartition.topic();
            int partition = topicPartition.partition();
            long offset = offsetAndMetadata.offset();
            String metadata = offsetAndMetadata.metadata();
            System.out.println(topic+":"+partition+":"+offset+",metadata:"+metadata);
        }
    }
}
