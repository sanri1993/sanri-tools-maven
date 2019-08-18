package learntest.apache.commons;

import org.apache.commons.lang3.StringUtils;
import org.junit.Test;

public class StringUtilsTest {
    @Test
    public void leftPad(){
        String s = StringUtils.leftPad("1", 3, '0');
        System.out.println(s);
    }

    @Test
    public void testSplit(){
        String s = "1$2$$";
        System.out.println(s.split("\\$").length);
        System.out.println(s.split("\\$",3).length);
        System.out.println(StringUtils.split(s,"\\$",3).length);
    }
}
