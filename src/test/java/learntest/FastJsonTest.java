package learntest;

import com.alibaba.fastjson.JSONObject;
import org.junit.Test;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;

public class FastJsonTest {
    @Test
    public void test() throws URISyntaxException {
        URL resource = FastJsonTest.class.getResource("/");
//        System.out.println(resource);
//        System.out.println(JSONObject.toJSONString(true));

        URI base = new URI("http://www.baidu.com/static");
        URI relative = new URI("image/uuid.png");
        URI whole = new URI("http://www.baidu.com/static/file/mm.zip");

        URI resolve = base.resolve(relative);
        System.out.println(resolve);
        System.out.println(base.relativize(whole));

    }
}
