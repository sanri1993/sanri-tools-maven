package learntest.rabbitmq.simple;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import learntest.rabbitmq.RabbitmqUtil;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

public class Sender {
    public static void main(String[] args) throws IOException, TimeoutException {
        Connection connection = RabbitmqUtil.connection();

        Channel channel = connection.createChannel();
        channel.queueDeclare("test",false,false,false,null);
        for (int i = 0; i <10000; i++) {
            String sendmsg = RandomStringUtils.randomAlphanumeric(i);
            channel.basicPublish("","test",null, sendmsg.getBytes());
            System.out.println("send msg["+i+"]:"+sendmsg);
        }


        channel.close();
        connection.close();
    }
}
