package sanri.utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.commons.lang3.time.FastDateFormat;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;

/**
 * 
 * 作者:sanri <br/>
 * 时间:2017-7-21下午6:47:59<br/>
 * 功能:模板生成工具 <br/>
 */
public class VelocityUtil {

	static{
		try {
			Properties properties = new Properties();
			properties.load(VelocityUtil.class.getResourceAsStream("/velocity.properties"));
			Velocity.init(properties);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * 从模板路径读取文件
	 * @param tmplPath
	 * @param charset
	 * @param context
	 * @return
	 * @throws IOException
	 */
	public static String formatFile(String tmplPath,Charset charset,Map<String,Object> context) throws IOException {
		InputStream inputStream = VelocityUtil.class.getResourceAsStream(tmplPath);
		InputStreamReader inputStreamReader = new InputStreamReader(inputStream, charset);
		return formatReader(inputStreamReader,context);
	}

	/**
	 * 读取 reader 中的内容,用 context 内容合并
	 * @param reader
	 * @param context
	 * @return
	 */
	public static String formatReader(Reader reader,Map<String,Object> context) throws IOException {
		StringWriter stringWriter = new StringWriter();
		IOUtils.copy(reader,stringWriter);
		String source = stringWriter.toString();
		return formatString(source,context);
	}

	/**
	 * 格式化字符串中的内容
	 * @param source
	 * @param context
	 * @return
	 */
	final static String DATETIME_PATTERN = "yyyy-MM-dd HH:mm:ss";
	public static String formatString(String source,Map<String,Object> data){
		if(StringUtils.isBlank(source)){
			return source;
		}
		VelocityContext context = new VelocityContext();
		context.put("dateutil", DateFormatUtils.ISO_DATE_FORMAT);
		context.put("datetimeutil", FastDateFormat.getInstance(DATETIME_PATTERN));
		if(data != null && data.size() > 0){
			Iterator<Entry<String, Object>> dataIterator = data.entrySet().iterator();
			while(dataIterator.hasNext()){
				Entry<String, Object> dataEntry = dataIterator.next();
				String key = dataEntry.getKey();
				Object value = dataEntry.getValue();

				context.put(key, value);
			}
		}
		// 输出流
		StringWriter writer = new StringWriter();
		Velocity.evaluate(context, writer, "StringFormat", source);
		return writer.toString();
	}
}
