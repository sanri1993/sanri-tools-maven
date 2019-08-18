package learntest.apache.commons;

import org.apache.commons.lang3.math.NumberUtils;
import org.junit.Test;

public class NumberUtilsTest {
    @Test
    public void test() {
        System.out.println(NumberUtils.toInt("02"));        //2
        System.out.println(NumberUtils.toInt("b01"));       // 0
    }
}
