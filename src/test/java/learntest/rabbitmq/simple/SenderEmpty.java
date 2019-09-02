package learntest.rabbitmq.simple;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import learntest.rabbitmq.RabbitmqUtil;
import org.apache.commons.lang3.RandomStringUtils;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

/**
 * 测试发消息到空队列
 * 这时候会成一个随机名称的队列
 */
public class SenderEmpty {
    public static void main(String[] args) throws IOException, TimeoutException {
        Connection connection = RabbitmqUtil.connection();

        Channel channel = connection.createChannel();
        channel.queueDeclare("",false,false,false,null);
//        for (int i = 0; i <100; i++) {
//            String sendmsg = RandomStringUtils.randomAlphanumeric(i);
//            channel.basicPublish("","",null, sendmsg.getBytes());
//            System.out.println("send msg["+i+"]:"+sendmsg);
//        }

        channel.close();
        connection.close();
    }
}
