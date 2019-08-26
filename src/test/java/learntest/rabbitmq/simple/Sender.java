package learntest.rabbitmq.simple;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import learntest.rabbitmq.RabbitmqUtil;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

public class Sender {
    public static void main(String[] args) throws IOException, TimeoutException {
        Connection connection = RabbitmqUtil.connection();

        Channel channel = connection.createChannel();
        channel.queueDeclare("test",false,false,false,null);
        channel.basicPublish("","test",null,"mm".getBytes());

        channel.close();
        connection.close();
    }
}
