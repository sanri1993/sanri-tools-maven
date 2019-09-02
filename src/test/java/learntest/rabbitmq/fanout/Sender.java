package learntest.rabbitmq.fanout;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import learntest.rabbitmq.RabbitmqUtil;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

public class Sender {
    private static final String EXCHANGE_NAME = "test_exchange_fanout";

    public static void main(String[] args) throws IOException, TimeoutException {
        Connection connection = RabbitmqUtil.connection();
        Channel channel = connection.createChannel();

        // fanout 不处理 routingKey,第一次发送的时候创建 exchange ，消息丢失，因为没有绑定队列； 但是如果先启动消费者，则会因为没有 exchange 报错
        channel.exchangeDeclare(EXCHANGE_NAME,"fanout");
        channel.basicPublish(EXCHANGE_NAME,"",null,"exchange fanout ".getBytes());
        channel.basicPublish(EXCHANGE_NAME,"",null,"exchange fanout ".getBytes());

        channel.close();
        connection.close();
    }
}
