package learntest.rabbitmq;

import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.sanri.app.sqlparser.ParserItem;
import org.apache.commons.io.IOUtils;
import org.junit.Test;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

public class RabbitmqUtil {

    public static final String HOST = "localhost";
    public static final int PORT = 5672;
    public static final String VHOST = "/test";
    public static final String USERNAME = "sanri";
    public static final String PASSWORD = "h123";

    static  ConnectionFactory connectionFactory = null;
    static {
        connectionFactory  = new ConnectionFactory();
        connectionFactory.setHost(HOST);
        connectionFactory.setPort(PORT);
        connectionFactory.setVirtualHost(VHOST);
        connectionFactory.setUsername(USERNAME);
        connectionFactory.setPassword(PASSWORD);
    }
    public static Connection connection() throws IOException, TimeoutException {
        return connectionFactory.newConnection();
    }

    @Test
    public void test() throws IOException, TimeoutException {
        System.out.println(connection());
    }

    public static void closeQuietly(Connection connection){
        if(connection != null){
            try {
                connection.close();
            } catch (IOException e) {
            }
        }
    }
}
