package learntest.rabbitmq.simple;

import com.rabbitmq.client.*;
import learntest.rabbitmq.RabbitmqUtil;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

public class Recive {
    public static void main(String[] args) throws IOException, TimeoutException {
        Connection connection = RabbitmqUtil.connection();
        Channel channel = connection.createChannel();

        //durable 是否持久化；用于保证消息的可靠
        //队列创建后不可以更改队列配置
        channel.queueDeclare("test",false,false,false,null);

        DefaultConsumer defaultConsumer = new DefaultConsumer(channel) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
                String msg = new String(body);
                System.out.println(msg);
            }
        };

        channel.basicConsume("test",true,defaultConsumer);
//        channel.close();
//        connection.close();;
    }
}
