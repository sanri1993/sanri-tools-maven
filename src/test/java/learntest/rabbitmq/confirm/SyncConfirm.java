package learntest.rabbitmq.confirm;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.ConfirmListener;
import com.rabbitmq.client.Connection;
import learntest.rabbitmq.RabbitmqUtil;

import java.io.IOException;

public class SyncConfirm {
    
    public static void main(String[] args) throws Exception {
        Connection connection = RabbitmqUtil.connection();
        Channel channel = connection.createChannel();
        String exchangeName = "test_confirm_exchange";
        String routingKey = "confirm.save";
        String msg = "Hello RabbitMQ Send confirm message!";

        //开启 channel 的 confirm 模式
        channel.confirmSelect();
        channel.exchangeDeclare(exchangeName,"fanout");
        channel.basicPublish(exchangeName, routingKey, null, msg.getBytes());

        channel.addConfirmListener(new ConfirmListener() {
            //消息失败处理
            @Override
            public void handleNack(long deliveryTag, boolean multiple) throws IOException {
                //deliveryTag；唯一消息标签
                //multiple：是否批量
                System.err.println("-------no ack!-----------");
            }
            //消息成功处理
            @Override
            public void handleAck(long deliveryTag, boolean multiple) throws IOException {
                System.err.println("-------ack!-----------");
            }
        });

    }
}