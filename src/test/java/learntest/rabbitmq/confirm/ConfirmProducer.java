package learntest.rabbitmq.confirm;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.ConfirmListener;
import com.rabbitmq.client.Connection;
import learntest.rabbitmq.RabbitmqUtil;

import java.io.IOException;

public class ConfirmProducer {
    
    public static void main(String[] args) throws Exception {
        Connection connection = RabbitmqUtil.connection();

        //3 通过Connection创建一个新的Channel
        Channel channel = connection.createChannel();
        
        //4 指定我们的消息投递模式: 消息的确认模式 
        channel.confirmSelect();
        
        String exchangeName = "test_confirm_exchange";
        String routingKey = "confirm.save";
        
        //5 发送一条消息
        String msg = "Hello RabbitMQ Send confirm message!";
        channel.basicPublish(exchangeName, routingKey, null, msg.getBytes());
        
        //6 添加一个确认监听
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