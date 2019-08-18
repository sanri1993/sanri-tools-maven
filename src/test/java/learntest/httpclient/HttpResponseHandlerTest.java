package learntest.httpclient;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.esotericsoftware.kryo.io.Input;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpResponseException;
import org.apache.http.util.EntityUtils;
import org.junit.Test;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.utils.HttpClientUtils;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

public class HttpResponseHandlerTest {

    @Test
    public void testResponseHandler() throws IOException {
        CloseableHttpClient httpClient = HttpClients.createDefault();
        HttpUriRequest httpGet = new HttpGet("http://www.baidu.com");
        CloseableHttpResponse execute = httpClient.execute(httpGet);

        ResponseHandler<String> responseHandler = new BasicResponseHandler();
        String handleResponse = responseHandler.handleResponse(execute);
        System.out.println(handleResponse);

        HttpClientUtils.closeQuietly(execute);
        HttpClientUtils.closeQuietly(httpClient);
    }

    /**
     * json 响应处理
     */
    class JsonResponseHandler  implements ResponseHandler<JSON> {
        private ResponseHandler<String> basicResponse = new BasicResponseHandler();
        @Override
        public JSON handleResponse(HttpResponse response) throws ClientProtocolException, IOException {
            String handleResponse = basicResponse.handleResponse(response);
            if(StringUtils.isBlank(handleResponse)){
                return null;
            }
            Object parse = JSON.parse(handleResponse);
            return (JSON) parse;
        }
    }

    class StreamReponseHandler implements ResponseHandler<InputStream>{

        @Override
        public InputStream handleResponse(HttpResponse response) throws ClientProtocolException, IOException {
            final StatusLine statusLine = response.getStatusLine();
            final HttpEntity entity = response.getEntity();
            if (statusLine.getStatusCode() >= 300) {
                EntityUtils.consume(entity);
                throw new HttpResponseException(statusLine.getStatusCode(),
                        statusLine.getReasonPhrase());
            }
            return entity.getContent();
        }
    }

    @Test
    public void testJsonResponseHandler() throws IOException {
        CloseableHttpClient httpClient = HttpClients.createDefault();
        HttpUriRequest httpGet = new HttpGet("http://localhost:8081/sanritools/index/listTools");
        CloseableHttpResponse execute = httpClient.execute(httpGet);

        ResponseHandler<JSON> responseHandler = new JsonResponseHandler();
        JSON handleResponse = responseHandler.handleResponse(execute);
        System.out.println(handleResponse);

        HttpClientUtils.closeQuietly(execute);
        HttpClientUtils.closeQuietly(httpClient);
    }

    @Test
    public void testStreamResponseHandler() throws IOException {
        CloseableHttpClient httpClient = HttpClients.createDefault();
        HttpUriRequest httpGet = new HttpGet("http://localhost:8081/sanritools/index/listTools");
        CloseableHttpResponse execute = httpClient.execute(httpGet);

        ResponseHandler<InputStream> responseHandler = new StreamReponseHandler();
        InputStream inputStream = responseHandler.handleResponse(execute);
        IOUtils.copy(inputStream,new FileOutputStream("d:/test/xx.zip"));

        HttpClientUtils.closeQuietly(execute);
        HttpClientUtils.closeQuietly(httpClient);
    }
}

