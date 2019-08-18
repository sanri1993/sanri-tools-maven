package learntest.locktest.mike;

import org.apache.commons.lang3.time.DateFormatUtils;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class OrderServiceWithLock implements OrderService{
    private Lock lock = new ReentrantLock();

    OrderCodeGenerator orderCodeGenerator = new OrderCodeGenerator();

    @Override
    public void createOrder() {
        lock.lock();

        try{
            int orderCode = orderCodeGenerator.getOrderCode();
            System.out.println(Thread.currentThread().getName() +" "+ DateFormatUtils.ISO_DATETIME_FORMAT.format(System.currentTimeMillis()) + " ------->"+orderCode);
        }finally {
            lock.unlock();
        }
    }
}
