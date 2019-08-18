//package sanri.utils;
//
//import java.io.BufferedReader;
//import java.io.File;
//import java.io.IOException;
//import java.io.InputStream;
//import java.io.InputStreamReader;
//import java.util.HashMap;
//import java.util.Map;
//import java.util.Properties;
//
//import javax.activation.DataSource;
//import javax.mail.Folder;
//import javax.mail.Message;
//import javax.mail.MessagingException;
//import javax.mail.NoSuchProviderException;
//import javax.mail.Session;
//import javax.mail.Store;
//import javax.mail.util.ByteArrayDataSource;
//
//import org.apache.commons.configuration.ConfigurationException;
//import org.apache.commons.configuration.PropertiesConfiguration;
//import org.apache.commons.io.FilenameUtils;
//import org.apache.commons.io.IOUtils;
//import org.apache.commons.lang3.StringUtils;
//import org.apache.commons.logging.Log;
//import org.apache.commons.logging.LogFactory;
//import org.apache.commons.mail.EmailAttachment;
//import org.apache.commons.mail.EmailException;
//import org.apache.commons.mail.HtmlEmail;
//import org.apache.commons.mail.ImageHtmlEmail;
//import org.apache.commons.mail.MultiPartEmail;
//import org.apache.commons.mail.SimpleEmail;
//
//import com.sanri.exception.SystemException;
//
///**
// *
// * 作者:sanri <br/>
// * 时间:2017-9-3上午10:07:24<br/>
// * 功能:用于简单的邮件发送 ,固定发送人的<br/>
// */
//public class MailUtil {
//	private static String HOST;
//	private static String SENDER_NAME;
//	private static String SENDER_EMAIL;
//	private static String SENDER_PASSWORD;
//
//	private final static String charset = "utf-8";
//
//	private final static Log logger = LogFactory.getLog(MailUtil.class);
//	public final static Map<String, String> MIME_MAP = new HashMap<String, String>();
//
//	static {
//		loadMIME();
//		String pkgPath = PathUtil.pkgPath("com.sanri.config");
//		File configFile = new File(pkgPath + "/mail.properties");
//		try {
//			PropertiesConfiguration propertiesConfiguration = new PropertiesConfiguration(configFile);
//			HOST = propertiesConfiguration.getString("mail.host");
//			SENDER_NAME = propertiesConfiguration.getString("sender.name");
//			SENDER_EMAIL = propertiesConfiguration.getString("sender.email");
//			SENDER_PASSWORD = propertiesConfiguration.getString("sender.password");
//		} catch (ConfigurationException e) {
//			e.printStackTrace();
//		}
//	}
//
//	/**
//	 *
//	 * 作者:sanri <br/>
//	 * 时间:2017-9-3上午10:11:22<br/>
//	 * 功能:普通无附件邮件发送 <br/>
//	 *
//	 * @param to
//	 *            发送给谁
//	 * @param subject
//	 *            主题
//	 * @param content
//	 *            内容
//	 */
//	public static void sendMail(String to, String subject, String content) {
//		SimpleEmail email = new SimpleEmail();
//		email.setHostName(HOST);
//		email.setAuthentication(SENDER_NAME, SENDER_PASSWORD);
//		email.setCharset(charset);
//
//		try {
//			// 设置内容,并发送
//			email.setFrom(SENDER_EMAIL, SENDER_NAME);
//			email.addTo(to);
//			email.setSubject(subject);
//			email.setMsg(content);
//			email.send();
//		} catch (EmailException e) {
//			throw new SystemException("发送邮件出错", e);
//		}
//	}
//
//	/**
//	 *
//	 * 作者:sanri <br/>
//	 * 时间:2017-9-3下午1:41:06<br/>
//	 * 功能:发送带附件的邮件 <br/>
//	 *
//	 * @param to
//	 *            发送给谁
//	 * @param subject
//	 *            主题
//	 * @param content
//	 *            内容
//	 * @param in
//	 *            文件输入流,内部关流
//	 * @param filename
//	 */
//	public static void sendMail(String to, String subject, String content, InputStream in, String filename) throws SystemException {
//		MultiPartEmail email = new MultiPartEmail();
//		try {
//			// 设置主机,发送者信息
//			email.setHostName(HOST);
//			email.setAuthentication(SENDER_NAME, SENDER_PASSWORD);
//			email.setCharset(charset);
//			try {
//				String mime = parseMIME(filename);
//				DataSource ds = new ByteArrayDataSource(in, mime);
//				email.attach(ds, filename, null, EmailAttachment.ATTACHMENT);
//			} catch (IOException e) {
//				logger.error("添加附件时出错,附件有可能找不到或丢失:" + filename);
//				e.printStackTrace();
//			}
//
//			// 设置内容,并发送
//			email.setFrom(SENDER_EMAIL, SENDER_NAME);
//			email.addTo(to);
//			email.setSubject(subject);
//			email.setMsg(content);
//			email.send();
//
//		} catch (EmailException e) {
//			throw new SystemException("发送邮件出错", e);
//		} finally {
//			IOUtils.closeQuietly(in);
//		}
//	}
//
//	/**
//	 *
//	 * 作者:sanri <br/>
//	 * 时间:2017-9-4下午12:46:09<br/>
//	 * 功能:根据文件名自动解析出 mime <br/>
//	 * 解析不出时使用 txt 类型
//	 *
//	 * @param filename
//	 * @return
//	 */
//	private static String parseMIME(String filename) {
//		String extension = FilenameUtils.getExtension(filename);
//		String mime = MIME_MAP.get(extension.toLowerCase());
//		if (StringUtils.isBlank(mime)) {
//			logger.warn("未找到文件:" + filename + " 的 mime 类型,自动使用文本类型");
//			mime = MIME_MAP.get("txt"); // 找不到 mime 时自动使用文本类型
//		}
//		return mime;
//	}
//
//	/**
//	 *
//	 * 作者:sanri <br/>
//	 * 时间:2017-9-3下午3:23:08<br/>
//	 * 功能:发送 html 内容的邮件 <br/>
//	 *
//	 * @param to
//	 * @param subject
//	 * @param htmlContent
//	 */
//	public static void sendHtmlMail(String to, String subject, String htmlContent) {
//		HtmlEmail email = new HtmlEmail();
//		email.setHostName(HOST);
//		email.setAuthentication(SENDER_NAME, SENDER_PASSWORD);
//		email.setCharset(charset);
//
//		try {
//			// 设置内容,并发送
//			email.setFrom(SENDER_EMAIL, SENDER_NAME);
//			email.addTo(to);
//			email.setSubject(subject);
//			email.setMsg(htmlContent);
//			email.send();
//		} catch (EmailException e) {
//			throw new SystemException("发送邮件出错", e);
//		}
//	}
//
//	/**
//	 *
//	 * 作者:sanri <br/>
//	 * 时间:2017-9-3下午3:23:08<br/>
//	 * 功能:发送 html 内容的邮件 <br/>
//	 *
//	 * @param to
//	 * @param subject
//	 * @param htmlContent
//	 * @param in
//	 * @param filename
//	 */
//	public static void sendHtmlMail(String to, String subject, String htmlContent, InputStream in, String filename) {
//		HtmlEmail email = new HtmlEmail();
//		email.setHostName(HOST);
//		email.setAuthentication(SENDER_NAME, SENDER_PASSWORD);
//		email.setCharset(charset);
//
//		try {
//			// 设置内容,并发送
//			email.setFrom(SENDER_EMAIL, SENDER_NAME);
//			email.addTo(to);
//			email.setSubject(subject);
//			email.setMsg(htmlContent);
//
//			try {
//				String mime = parseMIME(filename);
//				DataSource ds = new ByteArrayDataSource(in, mime);
//				email.attach(ds, filename, null, EmailAttachment.ATTACHMENT);
//			} catch (IOException e) {
//				logger.error("添加附件时出错,附件有可能找不到或丢失:" + filename);
//				e.printStackTrace();
//			}
//			email.send();
//		} catch (EmailException e) {
//			throw new SystemException("发送邮件出错", e);
//		}
//	}
//
//	/**
//	 *
//	 * 作者:sanri <br/>
//	 * 时间:2018-6-29上午10:35:31<br/>
//	 * 功能:扩展的邮件发送 <br/>
//	 * @param to 可以直接是单个邮箱，也可以是组合
//	 * @param cc 抄送人
//	 * @param subject 主题
//	 * @param htmlContent 邮件内容
//	 * @param in 输入流
//	 * @param filename 文件 名
//	 */
//	public static void sendHtmlMail(String to,String cc,String subject,String htmlContent, InputStream in, String filename){
//	}
//
//	/**
//	 *
//	 * 作者:sanri <br/>
//	 * 时间:2017-9-3下午3:27:32<br/>
//	 * 功能:发送带图片的 html 邮件 <br/>
//	 *
//	 * @param to
//	 * @param subject
//	 * @param htmlContent
//	 */
//	public static void sendImageHtmlMail(String to, String subject, String htmlContent) {
//		ImageHtmlEmail email = new ImageHtmlEmail();
//		email.setHostName(HOST);
//		email.setAuthentication(SENDER_NAME, SENDER_PASSWORD);
//		email.setCharset(charset);
//
//		try {
//			// 设置内容,并发送
//			email.setFrom(SENDER_EMAIL, SENDER_NAME);
//			email.addTo(to);
//			email.setSubject(subject);
//			email.setMsg(htmlContent);
//			email.send();
//		} catch (EmailException e) {
//			throw new SystemException("发送邮件出错", e);
//		}
//	}
//
//	/**
//	 *
//	 * 作者:sanri <br/>
//	 * 时间:2017-9-3下午3:31:36<br/>
//	 * 功能:发送带图片和附件的邮件 <br/>
//	 *
//	 * @param to
//	 * @param subject
//	 * @param htmlContent
//	 * @param in
//	 */
//	public static void sendImageHtmlMail(String to, String subject, String htmlContent, InputStream in, String filename) {
//		ImageHtmlEmail email = new ImageHtmlEmail();
//		email.setHostName(HOST);
//		email.setAuthentication(SENDER_NAME, SENDER_PASSWORD);
//		email.setCharset(charset);
//
//		try {
//			// 设置内容,并发送
//			email.setFrom(SENDER_EMAIL, SENDER_NAME);
//			email.addTo(to);
//			email.setSubject(subject);
//			email.setMsg(htmlContent);
//			try {
//				String mime = parseMIME(filename);
//				DataSource ds = new ByteArrayDataSource(in, mime);
//				email.attach(ds, filename, null, EmailAttachment.ATTACHMENT);
//			} catch (IOException e) {
//				logger.error("添加附件时出错,附件有可能找不到或丢失:" + filename);
//				e.printStackTrace();
//			}
//			email.send();
//		} catch (EmailException e) {
//			throw new SystemException("发送邮件出错", e);
//		}
//	}
//
//	/**
//	 *
//	 * 作者:sanri <br/>
//	 * 时间:2017-9-4下午12:40:23<br/>
//	 * 功能:加载所有支持的 mime <br/>
//	 */
//	private static void loadMIME() {
//		MIME_MAP.put("txt", "text/plain");
//		MIME_MAP.put("xls", "application/vnd.ms-excel");
//		MIME_MAP.put("xlsx", "application/vnd.ms-excel");
//		MIME_MAP.put("csv", "application/vnd.ms-excel");
//	}
//
//	public static void loadEmails() throws IOException {
//		// 创建一个有具体连接信息的Properties对象
//		Properties props = new Properties();
//		InputStream in = MailUtil.class.getResourceAsStream("/com/sanri/config/mail.properties");
//		props.load(in);
//
////		Properties props = new Properties();
////		props.setProperty("mail.store.protocol", "pop3");
////		props.setProperty("mail.pop3.host", HOST);
//
//		Session session = Session.getInstance(props);
//		session.setDebug(true);
//
//		try {
//			// 利用Session对象获得Store对象，并连接pop3服务器
//			Store store = session.getStore();
//			store.connect("pop3.163.com", SENDER_EMAIL, SENDER_PASSWORD);
//
//			// 获得邮箱内的邮件夹Folder对象，以"只读"打开
//			Folder folder = store.getFolder("inbox");
//			folder.open(Folder.READ_ONLY);
//
//			// 获得邮件夹Folder内的所有邮件Message对象
//			Message[] messages = folder.getMessages();
//
//			int mailCounts = messages.length;
//	        for(int i = 0; i < mailCounts; i++) {
//
//	            String subject = messages[i].getSubject();
//	            String from = (messages[i].getFrom()[0]).toString();
//
//	            System.out.println("第 " + (i+1) + "封邮件的主题：" + subject);
//	            System.out.println("第 " + (i+1) + "封邮件的发件人地址：" + from);
//
//
//	        }
//	        folder.close(false);
//	        store.close();
//		} catch (NoSuchProviderException e) {
//			e.printStackTrace();
//		} catch (MessagingException e) {
//			e.printStackTrace();
//		}
//	}
//}
