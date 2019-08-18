package learntest.apache.commons;

import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.junit.Test;

import java.util.Date;

public class DateUtilsTest {
    @Test
    public void testTime(){
        String pattern = "yyyy-MM-dd HH:mm:ss";
        Date dateCurrent = new Date();
        Date whenSleepStart = DateUtils.setHours(dateCurrent, 5);
        Date whenSleepEnd = DateUtils.setHours(dateCurrent, 9);

        System.out.println(DateFormatUtils.format(dateCurrent, pattern));
        System.out.println(DateFormatUtils.format(whenSleepStart, pattern));
        System.out.println(DateFormatUtils.format(whenSleepEnd, pattern));
    }

    @Test
    public void testRound(){
    }
}
