package sanri.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.commons.lang3.time.DateUtils;

import com.alibaba.fastjson.JSONObject;
/**
 * 
 * 创建时间:2016-9-24下午5:33:29<br/>
 * 创建者:sanri<br/>
 * 功能:扩展自 org.apache.commons.lang,加入一些源数据 <br/>
 */
public class RandomUtil extends RandomStringUtils {
	public static JSONObject AREANO_MAP;
	public static JSONObject CITY_LIST;
	public static final String FIRST_NAME="赵钱孙李周吴郑王冯陈褚卫蒋沈韩杨朱秦尤许何吕施张孔曹严华金魏陶姜戚谢邹喻柏水窦章云苏潘葛奚范彭郎鲁韦昌马苗凤花方俞任袁柳酆鲍史唐费廉岑薛雷贺倪汤滕殷罗毕郝邬安常乐于时傅皮卞齐康伍余元卜顾孟平黄和穆萧尹姚邵湛汪祁毛禹狄米贝明臧计伏成戴谈宋茅庞熊纪舒屈项祝董梁杜阮蓝闵席季麻强贾路娄危江童颜郭梅盛林刁钟徐邱骆高夏蔡田樊胡凌霍虞万支柯昝管卢莫经房裘缪干解应宗丁宣贲邓郁单杭洪包诸左石崔吉钮龚程嵇邢滑裴陆荣翁荀羊於惠甄麴家封芮羿储靳汲邴糜松井段富巫乌焦巴弓牧隗山谷车侯宓蓬全郗班仰秋仲伊宫宁仇栾暴甘钭厉戎祖武符刘景詹束龙叶幸司韶郜黎蓟薄印宿白怀蒲邰从鄂索咸籍赖卓蔺屠蒙池乔阴郁胥能苍双闻莘党翟谭贡劳逄姬申扶堵冉宰郦雍舄璩桑桂濮牛寿通边扈燕冀郏浦尚农温别庄晏柴瞿阎充慕连茹习宦艾鱼容向古易慎戈廖庾终暨居衡步都耿满弘匡国文寇广禄阙东殴殳沃利蔚越夔隆师巩厍聂晁勾敖融冷訾辛阚那简饶空曾毋沙乜养鞠须丰巢关蒯相查後荆红游竺权逯盖益桓公晋楚闫法汝鄢涂钦仉督岳帅缑亢况后有琴商牟佘佴伯赏墨哈谯笪年爱阳佟";
	public static final String GIRL="秀娟英华慧巧美娜静淑惠珠翠雅芝玉萍红娥玲芬芳燕彩春菊兰凤洁梅琳素云莲真环雪荣爱妹霞香月莺媛艳瑞凡佳嘉琼勤珍贞莉桂娣叶璧璐娅琦晶妍茜秋珊莎锦黛青倩婷姣婉娴瑾颖露瑶怡婵雁蓓纨仪荷丹蓉眉君琴蕊薇菁梦岚苑婕馨瑗琰韵融园艺咏卿聪澜纯毓悦昭冰爽琬茗羽希宁欣飘育滢馥筠柔竹霭凝晓欢霄枫芸菲寒伊亚宜可姬舒影荔枝思丽";  
    public static final String BOY="伟刚勇毅俊峰强军平保东文辉力明永健鸿世广万志义兴良海山仁波宁贵福生龙元全国胜学祥才发武新利清飞彬富顺信子杰涛昌成康星光天达安岩中茂进林有坚和彪博诚先敬震振壮会思群豪心邦承乐绍功松善厚庆磊民友裕河哲江超浩亮政谦亨奇固之轮翰朗伯宏言若鸣朋斌梁栋维启克伦翔旭鹏泽晨辰士以建家致树炎德行时泰盛雄琛钧冠策腾楠榕风航弘正日"; 
    public static final String[] EMAIL_SUFFIX="@gmail.com,@yahoo.com,@msn.com,@hotmail.com,@aol.com,@ask.com,@live.com,@qq.com,@0355.net,@163.com,@163.net,@263.net,@3721.net,@yeah.net,@googlemail.com,@126.com,@sina.com,@sohu.com,@yahoo.com.cn".split(",");
    public static String [] ADDRESS_LIST;
	static{
		String dataPath = PathUtil.pkgPath("sanri.utils.data");
		InputStreamReader reader = null;
		Charset charset = Charset.forName("utf-8");
		try {
			reader = new InputStreamReader(new FileInputStream(dataPath+File.separator+"address.string"),charset);
			String address = IOUtils.toString(reader);
			ADDRESS_LIST = address.split(",");
			reader = new InputStreamReader(new FileInputStream(dataPath+File.separator+"city.min.json"),charset);
			String citylist = IOUtils.toString(reader);
			CITY_LIST = JSONObject.parseObject(citylist);
			reader = new InputStreamReader(new FileInputStream(dataPath+File.separator+"idcodearea.json"),charset);
			String idcode = IOUtils.toString(reader);
			AREANO_MAP = JSONObject.parseObject(idcode);
		} catch (Exception e) {
			e.printStackTrace();
		}finally{
			IOUtils.closeQuietly(reader);
		}
	}
	/**
	 * 功能:生成 length 个中文 <br/>
	 * 创建时间:2016-4-16上午11:24:40 
	 * 作者：sanri 
	 * */
	public static String chinese(int length, String src) {
		String ret = "";
		if(!StringUtils.isBlank(src)){
			return random(length, src.toCharArray());
		}
		for (int i = 0; i < length; i++) {
			String str = null;
			int hightPos, lowPos; // 定义高低位
			Random random = new Random();
			hightPos = (176 + Math.abs(random.nextInt(39))); // 获取高位值
			lowPos = (161 + Math.abs(random.nextInt(93))); // 获取低位值
			byte[] b = new byte[2];
			b[0] = (new Integer(hightPos).byteValue());
			b[1] = (new Integer(lowPos).byteValue());
			try {
				str = new String(b, "GBk"); // 转成中文
			} catch (UnsupportedEncodingException ex) {
				ex.printStackTrace();
			}
			ret += str;
		}
		return ret;
	}
	
	/**
	 * 
	 * 功能:随机生成用户名<br/>
	 * 创建时间:2017-8-13上午8:04:32<br/>
	 * 作者：sanri<br/>
	 * @return<br/>
	 */
	public static String username(){
		boolean sex = (randomNumber(100) % 2 == 0 );
		int secondNameLength = (int) randomNumber(2);
		String firstName = random(1, FIRST_NAME);
		String srcChars = sex ? BOY : GIRL;
		String secondName = random(secondNameLength,srcChars );
		return firstName+secondName;
	}
	/**
	 * 
	 * 功能:给定格式 ,开始时间,结束时间,生成一个在开始和结束内的日期<br/>
	 * 创建时间:2016-4-16下午3:57:38<br/>
	 * 作者：sanri<br/>
	 * 入参说明:<br/>
	 * 出参说明：字符串日期类型由 format 格式化<br/>
	 * @throws ParseException<br/>
	 */
	public static String date(String format,String begin,String end) throws ParseException{
		if(StringUtils.isBlank(format)){
			format = "yyyyMMdd";
		}
		long timstamp = timstamp(format, begin, end);
		return DateFormatUtils.format(timstamp, format);
	}
	/**
	 * 
	 * 功能:得到由开始时间和结束时间内的一个时间戳<br/>
	 * 创建时间:2016-4-16下午4:07:31<br/>
	 * 作者：sanri<br/>
	 * 入参说明:<br/>
	 * 出参说明：如果时间给的不对,则是当前时间<br/>
	 * @param format
	 * @param begin
	 * @param end
	 * @return
	 * @throws ParseException<br/>
	 */
	public static long timstamp(String format,String begin,String end) throws ParseException{
		if(StringUtils.isBlank(format)){
			format = "yyyyMMdd";
		}
		Date now = new Date();
		if(StringUtils.isBlank(begin)){
			begin = DateFormatUtils.format(now,format);
		}
		if(StringUtils.isBlank(end)){
			end = DateFormatUtils.format(now,format);
		}
		String [] formats = new String []{format};
		long beginDateTime = DateUtils.parseDate(begin, formats).getTime();
		long endDateTime = DateUtils.parseDate(end, formats).getTime();
		if(beginDateTime > endDateTime){
			return now.getTime();
		}
		long random = randomNumber(endDateTime - beginDateTime);
		return random + beginDateTime;
	}
	/**
	 * 
	 * 功能:生成限制数字内的数字 0 ~ limit 包括 limit<br/>
	 * 创建时间:2016-9-24下午6:07:05<br/>
	 * 作者：sanri<br/>
	 */
	public static long randomNumber(long limit) {
		return Math.round(Math.random() * limit);
	}
	/**
	 * 
	 * 功能:生成身份证号<br/>
	 * 创建时间:2016-4-16下午2:31:37<br/>
	 * 作者：sanri<br/>
	 * 入参说明:[area:区域号][yyyyMMdd:出生日期][sex:偶=女,奇=男]<br/>
	 * 出参说明：330602 19770717 201 1<br/>
	 * 
	 * @param area
	 * @param yyyyMMdd
	 * @param sno
	 * @return<br/>
	 */
	public static String idcard(String area, String yyyyMMdd, String sno) {
		String idCard17 = area + yyyyMMdd + sno;
		int[] validas = { 7, 9, 10, 5, 8, 4, 2, 1, 6, 3, 7, 9, 10, 5, 8, 4, 2 };
		char[] idCards = idCard17.toCharArray();
		int count = 0;
		for (int i = 0; i < validas.length; i++) {
			count += Integer.valueOf(String.valueOf(idCards[i])) * validas[i];
		}
		String lastNum = String.valueOf((12 - count % 11) % 11);
		lastNum = "10".equals(lastNum) ? "x":lastNum; 
		return idCard17 + lastNum;
	}
	public static String idcard(String area){
		String format = "yyyyMMdd";
		SimpleDateFormat sdf = new SimpleDateFormat(format);
		try {
			String yyyyMMdd = date(format, "19990101", sdf.format(new Date()));
			String sno = randomNumeric(3);
			return idcard(area, yyyyMMdd, sno);
		} catch (ParseException e) {
			e.printStackTrace();
		}
		return "";
	}
	public static String idcard(){
		Set<String> areaNos = AREANO_MAP.keySet();
		List<String> areaList = new ArrayList<String>();
		areaList.addAll(areaNos);
		String area = areaList.get((int)randomNumber(areaList.size()));
		return idcard(area);
	}
	/**
	 * 
	 * 功能:随机生成地址<br/>
	 * 创建时间:2016-4-16下午6:19:14<br/>
	 * 作者：sanri<br/>
	 * 入参说明:<br/>
	 * 出参说明：<br/>
	 * @return<br/>
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static String address(){
		List<Map> cityList = (List<Map>) CITY_LIST.get("citylist");
		Map provinceEntry = cityList.get((int)randomNumber(cityList.size() - 1));
		String province = String.valueOf(provinceEntry.get("p"));
		List<Map> city = (List<Map>) provinceEntry.get("c");
		Map areaEntry = city.get((int)randomNumber(city.size() - 1));
		String area = String.valueOf(areaEntry.get("n"));
		List<Map> area2 = (List<Map>) areaEntry.get("a");
		String s = "";
		if(area2 != null){
			Map cityEntry = area2.get((int)randomNumber(area2.size() - 1));
			s = String.valueOf(cityEntry.get("s"));
		}
		return province + area + s + ADDRESS_LIST[(int)randomNumber(ADDRESS_LIST.length - 1)];
	}
	/**
	 * 
	 * 功能:随机邮件地址,length 指 用户名长度<br/>
	 * 创建时间:2016-9-24下午6:11:54<br/>
	 * 作者：sanri<br/>
	 */
	public static String email(int length){
		return randomAlphanumeric(length)+EMAIL_SUFFIX[(int)randomNumber(EMAIL_SUFFIX.length - 1)];
	}
	/**
	 * 
	 * 作者:sanri</br>
	 * 时间:2016-9-29上午9:25:54</br>
	 * 功能:给 bean 注入数据</br>
	 * @deprecated 方法有 bug 只能填充最简单的 bean <br/>
	 * 使用 jPopulator 来填充 javaBean <br/>
	 * 依赖于 apache commons-lang3,commons-beanutils,commons-logging
	 * <code>
	 * Populator populator = new PopulatorBuilder().build();
	 * List<Student> populateBeans = populator.populateBeans(Student.class, 10);
	 * </code>
	 */
//	public static <T> List<T> beanLoadData(Class<T> clazz,int size){
//		List<T> dataList = new ArrayList<T>();
//		List<Method> findMethod = BeanUtil.findMethod(clazz, BeanUtil.GET_METHOD_FILTER);
//		if(!Validate.isEmpty(findMethod)){
//			try{
//				for(int i=0;i<size;i++){
//					T obj = clazz.newInstance();
//					for (Method method : findMethod) {
//						Class<?> returnType = method.getReturnType();
//						Method setMethod = clazz.getMethod("set"+method.getTitle().substring(3),returnType );
//						setMethod.setAccessible(true);
//						if(returnType == String.class){
//							setMethod.invoke(obj, randomAlphanumeric(5));
//						}else if(returnType == Integer.class){
//							setMethod.invoke(obj, Integer.parseInt(randomNumeric(5)));
//						}else if(returnType == java.util.Date.class){
//							setMethod.invoke(obj, new Date(timstamp("yyyy-MM-dd", "2010-09-17", "2016-09-17")));
//						}else{
////							setMethod.invoke(obj, new Object());
//							System.err.println("类型:"+returnType+" 无法注入");
//						}
//					}
//					dataList.add(obj);
//				}
//			} catch (InstantiationException e) {
//				e.printStackTrace();
//			} catch (IllegalAccessException e) {
//				e.printStackTrace();
//			} catch (IllegalArgumentException e) {
//				e.printStackTrace();
//			} catch (InvocationTargetException e) {
//				e.printStackTrace();
//			} catch (ParseException e) {
//				e.printStackTrace();
//			} catch (NoSuchMethodException e) {
//				e.printStackTrace();
//			} catch (SecurityException e) {
//				e.printStackTrace();
//			}
//		}
//		return dataList;
//	}
	
}