package learntest.apache.commons;

import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.junit.Test;
import sanri.utils.DateUtil;

import java.util.Calendar;
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
//        Date round = DateUtils.round(new Date(), Calendar.DAY_OF_MONTH);
        Date truncate = DateUtils.truncate(new Date(), Calendar.YEAR);
        Date round = DateUtils.round(new Date(), Calendar.YEAR);
        System.out.println(DateFormatUtils.format(truncate,"yyyy-MM-dd HH:mm:ss.S"));
        System.out.println(DateFormatUtils.format(round,"yyyy-MM-dd HH:mm:ss.S"));
    }

    @Test
    public void testgetFragmentInDays(){
        System.out.println(DateUtils.getFragmentInDays(new Date(),Calendar.MONTH));
    }
}
