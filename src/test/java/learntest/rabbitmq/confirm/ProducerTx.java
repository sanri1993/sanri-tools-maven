package learntest.rabbitmq.confirm;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import learntest.rabbitmq.RabbitmqUtil;
import org.apache.commons.lang3.RandomUtils;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

public class ProducerTx {
    public static void main(String[] args) throws IOException, TimeoutException {
        Connection connection = RabbitmqUtil.connection();
        Channel channel = connection.createChannel();

        String queueName = "tx_queue";

        channel.queueDeclare(queueName,false,false,false,null);
        channel.txSelect();
        try {
            channel.basicPublish("", queueName, null, "message".getBytes());
            channel.txCommit();
            int j = RandomUtils.nextInt(0,2);
            int i=1/j;
            System.out.println("消息发送成功 ");
        }catch (Exception e){
            channel.txRollback();
            System.out.println("消息发送失败");
        }

        channel.close();
        connection.close();
    }
}
