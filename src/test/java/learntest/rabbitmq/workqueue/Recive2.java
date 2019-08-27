package learntest.rabbitmq.workqueue;

import com.rabbitmq.client.*;
import learntest.rabbitmq.RabbitmqUtil;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

public class Recive2 {
    private static final String  QUEUE_NAME = "test_work";

    public static void main(String[] args) throws IOException, TimeoutException {
        Connection connection = RabbitmqUtil.connection();
        Channel channel = connection.createChannel();

        channel.queueDeclare(QUEUE_NAME,false,false,false,null);

        //保证一次只发一个
        channel.basicQos(1);


        DefaultConsumer defaultConsumer = new DefaultConsumer(channel) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
                try {
                    String msg = new String(body);
                    System.out.println(msg);

//                    Thread.sleep(1000);
//                } catch (InterruptedException e) {}
                }finally{
                    //发送 ack 确认
                    channel.basicAck(envelope.getDeliveryTag(),false);
                }

            }
        };

        boolean autoAck = false;
        channel.basicConsume(QUEUE_NAME,autoAck,defaultConsumer);
//        channel.close();
//        connection.close();;
    }
}
