package learntest.locktest.mike;

import org.apache.commons.lang3.time.DateFormatUtils;

public class OrderServiceImpl implements OrderService {
    OrderCodeGenerator orderCodeGenerator = new OrderCodeGenerator();

    @Override
    public void createOrder() {
        int orderCode = orderCodeGenerator.getOrderCode();
        System.out.println(Thread.currentThread().getName() +" "+ DateFormatUtils.ISO_DATETIME_FORMAT.format(System.currentTimeMillis()) + " ------->"+orderCode);
    }
}
