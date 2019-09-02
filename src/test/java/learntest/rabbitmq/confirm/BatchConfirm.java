package learntest.rabbitmq.confirm;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import learntest.rabbitmq.RabbitmqUtil;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

public class BatchConfirm {
    public static void main(String[] args) throws IOException, TimeoutException, InterruptedException {
        Connection connection = RabbitmqUtil.connection();
        Channel channel = connection.createChannel();

        String queueName = "confirm_batch_queue";
        channel.queueDeclare(queueName,false,false,false,null);
        channel.confirmSelect();
        //匿名 exchange
        for (int i = 0; i < 10; i++) {
            channel.basicPublish("",queueName,null,"message".getBytes());
        }

        boolean waitForConfirms = channel.waitForConfirms();
        if(waitForConfirms){
            System.out.println("消息发送成功");
        }
        System.out.println("发送成功后关闭连接,同步操作");
        channel.close();
        connection.close();
    }
}
