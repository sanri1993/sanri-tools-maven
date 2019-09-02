package learntest.rabbitmq.route;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import learntest.rabbitmq.RabbitmqUtil;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

public class DirectSender {
    private static final String EXCHANGE_NAME = "test_direct_exchange";

    public static void main(String[] args) throws IOException, TimeoutException {
        Connection connection = RabbitmqUtil.connection();
        Channel channel = connection.createChannel();

        channel.exchangeDeclare(EXCHANGE_NAME,"direct");

        channel.basicPublish(EXCHANGE_NAME,"info",null,"info message".getBytes());
        channel.basicPublish(EXCHANGE_NAME,"error",null,"error message".getBytes());

        channel.close();
        connection.close();
    }
}
