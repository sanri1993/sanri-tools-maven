package learntest.rabbitmq.route;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import learntest.rabbitmq.RabbitmqUtil;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

public class TopicSender {
    private static final String EXCHANGE_NAME = "test_exchange_topic";

    public static void main(String[] args) throws IOException, TimeoutException {
        Connection connection = RabbitmqUtil.connection();
        Channel channel = connection.createChannel();

        channel.exchangeDeclare(EXCHANGE_NAME,"topic");
        channel.basicPublish(EXCHANGE_NAME,"goods.add",null,"exchange add ".getBytes());
        channel.basicPublish(EXCHANGE_NAME,"goods.delete",null,"exchange delete ".getBytes());

        channel.close();
        connection.close();
    }
}
