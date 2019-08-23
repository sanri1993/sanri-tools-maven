//package learntest;
//
//import org.apache.kafka.clients.consumer.ConsumerRecord;
//import org.apache.kafka.clients.consumer.ConsumerRecords;
//import org.apache.kafka.clients.consumer.KafkaConsumer;
//import org.apache.kafka.common.TopicPartition;
//
//import java.util.Collections;
//import java.util.List;
//import java.util.Map;
//import java.util.Properties;
//
///**
// * @author: wangjian
// * @date: 2018-08-01 15:19
// */
//public class KafkaTestMain {
////    public static void main(String[] args) {
////        Properties props = new Properties();
////
////        props.put("bootstrap.servers", "14.17.100.215:9093");
////        //每个消费者分配独立的组号
////        props.put("group.id", "console");
////
////        //如果value合法，则自动提交偏移量
////        props.put("enable.auto.commit", "true");
////
////        //设置多久一次更新被消费消息的偏移量
////        props.put("auto.commit.interval.ms", "1000");
////
////        //设置会话响应的时间，超过这个时间kafka可以选择放弃消费或者消费下一条消息
////        props.put("session.timeout.ms", "30000");
////
////        //该参数表示从头开始消费该主题
////        props.put("auto.offset.reset", "earliest");
////
////        //注意反序列化方式为ByteArrayDeserializer
////        props.put("key.deserializer",
////                "org.apache.kafka.common.serialization.ByteArrayDeserializer");
////        props.put("value.deserializer",
////                "org.apache.kafka.common.serialization.ByteArrayDeserializer");
////
////                 //计算得到分组的分区
////        int abs = Math.abs("zhangjing".hashCode() % 50);
////
////        KafkaConsumer<byte[], byte[]> consumer = new KafkaConsumer<byte[], byte[]>(props);
////
////
////        TopicPartition topicPartition = new TopicPartition("__consumer_offsets",abs);
////        //指定消费某个主题的某个分区
////        consumer.assign(Collections.singleton(topicPartition));
////        //从 topic 分区的某个 offset 开始消费
////        Map<TopicPartition, Long> endOffsets = consumer.endOffsets(Collections.singleton(topicPartition));
////        Long endOffset = endOffsets.get(topicPartition);
////        consumer.seek(topicPartition,endOffset - 1);
////
////        ConsumerRecords<byte[], byte[]> consumerRecords = consumer.poll(100);
////        Iterable<ConsumerRecord<byte[], byte[]>> consumer_offsets = consumerRecords.records("__consumer_offsets");
////        Iterator<ConsumerRecord<byte[], byte[]>> iterator = consumer_offsets.iterator();
////        while (iterator.hasNext()){
////            ConsumerRecord<byte[], byte[]> consumerRecord = iterator.next();
////            ByteBuffer wrap = ByteBuffer.wrap(consumerRecord.value());
////            OffsetAndMetadata offsetAndMetadata = GroupMetadataManager.readOffsetMessageValue(wrap);
////            OffsetMetadata offsetMetadata = offsetAndMetadata.offsetMetadata();
////            System.out.println(offsetMetadata);
////
////            //获取 key 信息
////            ByteBuffer keyBuffer = ByteBuffer.wrap(consumerRecord.key());
////            BaseKey baseKey = GroupMetadataManager.readMessageKey(keyBuffer);
////            System.out.println(baseKey);
////        }
////    }
//public static void main(String[] args) {
//     Properties props = new Properties();
//
//        props.put("bootstrap.servers", "14.17.100.215:9093");
//        //每个消费者分配独立的组号
//        props.put("group.id", "console");
//
//        //如果value合法，则自动提交偏移量
//        props.put("enable.auto.commit", "true");
//
//        //设置多久一次更新被消费消息的偏移量
//        props.put("auto.commit.interval.ms", "1000");
//
//        //设置会话响应的时间，超过这个时间kafka可以选择放弃消费或者消费下一条消息
//        props.put("session.timeout.ms", "30000");
//
//        //该参数表示从头开始消费该主题
//        props.put("auto.offset.reset", "earliest");
//
//        //注意反序列化方式为ByteArrayDeserializer
//        props.put("key.deserializer",
//                "org.apache.kafka.common.serialization.ByteArrayDeserializer");
//        props.put("value.deserializer",
//                "org.apache.kafka.common.serialization.ByteArrayDeserializer");
//
//        KafkaConsumer<byte[], byte[]> consumer = new KafkaConsumer<byte[], byte[]>(props);
//
//        TopicPartition topicPartition = new TopicPartition("gpsstd_carry_passenger", 0);
//        consumer.assign(Collections.singletonList(topicPartition));
//
//        Map<TopicPartition, Long> endOffsets = consumer.endOffsets(Collections.singletonList(topicPartition));
//        Long endOffset = endOffsets.get(topicPartition);
//        endOffset = endOffset - 100;
//        if(endOffset < 0){endOffset =0L ;}
//        consumer.seek(topicPartition,endOffset);
//
//        ConsumerRecords<byte[], byte[]> consumerRecords = consumer.poll(100);
//        List<ConsumerRecord<byte[], byte[]>> records = consumerRecords.records(topicPartition);
//        int size = records.size();
//        System.out.println(size);
//        for (ConsumerRecord<byte[], byte[]> record : records) {
//            byte[] value = record.value();
//            System.out.println("-----------"+value);
//        }
//}
//
//
//}