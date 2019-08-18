package sanri.utils;

import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.commons.lang3.time.DateUtils;

/**
 * 
 * 作者:sanri <br/>
 * 时间:2017-9-15上午10:11:50<br/>
 * 功能: 扩展 apache 的日期类,增加对日期差的计算<br/>
 */
public final class DateUtil extends DateUtils {
	/**
	 * 
	 * 作者:sanri <br/>
	 * 时间:2017-10-17下午3:57:41<br/>
	 * 功能:获取日期的开始日期,精确到毫秒 <br/>
	 * 今天的开始日期为 yyyy-MM-dd 00:00:00
	 * 
	 * @param date
	 * @return
	 */
	public static Date dateStart(Date date) {
		return truncate(date, Calendar.DAY_OF_MONTH);
	}

	/**
	 * 
	 * 作者:sanri <br/>
	 * 时间:2017-10-17下午3:59:16<br/>
	 * 功能:获取日期的结束日期,实现为加一天的开始时间减一毫秒 <br/>
	 * 
	 * @param date
	 * @return
	 */
	public static Date dateEnd(Date date) {
		Date tomorrow = DateUtils.addDays(date, 1);
		Date tomorrowStart = dateStart(tomorrow);
		return new Date(tomorrowStart.getTime() - 1);
	}
	
	/**
	 * 
	 * 作者:sanri <br/>
	 * 时间:2017-9-15上午10:17:05<br/>
	 * 功能:计算两个日期的间隔,使用时间戳间隔然后取整 <br/>
	 * @param date1 日期 1
	 * @param date2 日期 2
	 * @param timeUnit
	 * @return
	 */
	public static long interval(Date date1,Date date2 ,TimeUnit timeUnit){
		return interval(date1.getTime(), date2.getTime(), timeUnit);
	}
	public static long interval(Calendar cal1,Calendar cal2 ,TimeUnit timeUnit){
		return interval(cal1.getTimeInMillis(),cal2.getTimeInMillis() , timeUnit);
	}
	public static long interval(long millis1,long millis2,TimeUnit timeUnit){
		long duration = Math.abs(millis2 - millis1);
		return interval(duration, timeUnit);
	}
	public static long interval(long duration,TimeUnit timeUnit){
		return timeUnit.convert(duration, TimeUnit.MILLISECONDS);
	}
	
	/**
	 * 
	 * 作者:sanri <br/>
	 * 时间:2017-9-15下午12:41:04<br/>
	 * 功能:计算两个日期间的自然日期间隔 从后一天开始计算,只算天数<br/>
	 * 注:此方法没有包含当天
	 * @param date1
	 * @param date2
	 * @return
	 */
	public static long naturalIntervalDay(Calendar cal1,Calendar cal2){
		int day1 = cal1.get(Calendar.DAY_OF_YEAR);
		int day2 = cal2.get(Calendar.DAY_OF_YEAR);
		
		//add by sanri at 2017/11/21 解决不同年,算出来的天数不一致的问题
		int year1 = cal1.get(Calendar.YEAR);
		int year2 = cal2.get(Calendar.YEAR);
		
		if(year1 == year2){
			//如果是同年,则直接可以时间相减
			return Math.abs(day2 - day1);
		}
		int yearSwap = year1;
		if(year1 > year2){
			//交换两个年
			year1 = year2;
			year2 = yearSwap;
		}
		int timeDistance = 0;
		for (int i = year1; i < year2; i++) {
			if(i % 4 == 0 && i % 100 != 0 || i % 400 == 0){	//闰年
				timeDistance += 366;
			}else{
				timeDistance += 365;
			}
		}
		
		return Math.abs(timeDistance + (day2 - day1));
	}
	public static long naturalIntervalDay(Date date1,Date date2){
		Calendar calendar1 = Calendar.getInstance();
		calendar1.setTime(date1);
		Calendar calendar2 = Calendar.getInstance();
		calendar2.setTime(date2);
		return naturalIntervalDay(calendar1, calendar2);
	}
	public static long naturalIntervalDay(long millis1,long millis2){
		Calendar calendar1 = Calendar.getInstance();
		calendar1.setTimeInMillis(millis1);
		Calendar calendar2 = Calendar.getInstance();
		calendar2.setTimeInMillis(millis2);
		return naturalIntervalDay(calendar1, calendar2);
	}
	
	/**
	 * 
	 * 作者:sanri <br/>
	 * 时间:2018-3-27下午5:44:29<br/>
	 * 功能:日期解析 <br/>
	 * 
	 * @param date
	 *            默认解析,格式为yyyy-MM-dd,yyyy-MM-dd HH:mm:ss
	 * @return
	 */
	public static final String[] PARSE_PATTERNS = { "yyyy-MM-dd","yyyy-MM-dd HH:mm:ss" };

	public static Date parseDate(String date) throws ParseException {
		return parseDate(date, PARSE_PATTERNS);
	}

	public static Date parseDate(String date, String format)throws ParseException {
		String[] parsePatterns = new String[] { format };
		return parseDate(date, parsePatterns);
	}

	/**
	 * 
	 * 作者:sanri <br/>
	 * 时间:2018-3-27下午5:50:43<br/>
	 * 功能:日期格式化,默认格式为 yyyy-MM-dd <br/>
	 * 
	 * @param date
	 * @return
	 */
	public static String PATTERN = "yyyy-MM-dd HH:mm:ss";

	public static String formatDateYmd(Date date) {
		return DateFormatUtils.ISO_DATE_FORMAT.format(date);
	}

	public static String formatDate(Date date, String pattern) {
		return DateFormatUtils.format(date, pattern);
	}

	public static String formatDateTime(Date date) {
		return DateFormatUtils.format(date, PATTERN);
	}

	/**
	 * 
	 * 作者:sanri <br/>
	 * 时间:2017-10-17下午4:03:46<br/>
	 * 功能:获取日期所在周的周一 <br/>
	 * 周一认为是一周的开始
	 * 
	 * @param date
	 * @return
	 */
	public static Date weekStart(Date date) {
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(date);
		calendar.add(Calendar.DAY_OF_MONTH, -1); // 解决周日会出现 并到下一周的情况
		calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
		Date time = calendar.getTime();
		return dateStart(time);
	}

	/**
	 * 
	 * 作者:sanri <br/>
	 * 时间:2017-10-17下午4:04:18<br/>
	 * 功能:获取日期所在周的周末,周日 <br/>
	 * 
	 * @param date
	 * @return
	 */
	public static Date weekEnd(Date date) {
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(date);
		calendar.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);
		calendar.add(Calendar.WEEK_OF_YEAR, 1);
		return dateEnd(calendar.getTime());
	}

	/**
	 * 
	 * 作者:sanri <br/>
	 * 时间:2017-10-17下午4:18:55<br/>
	 * 功能:获取日期所在月的月初 <br/>
	 * 
	 * @param date
	 * @return
	 */
	public static Date monthStart(Date date) {
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(date);
		calendar.set(Calendar.DAY_OF_MONTH,
				calendar.getActualMinimum(Calendar.DAY_OF_MONTH));
		return dateStart(calendar.getTime());
	}

	/**
	 * 
	 * 作者:sanri <br/>
	 * 时间:2017-10-17下午4:22:53<br/>
	 * 功能:获取日期所在月的月末 <br/>
	 * 
	 * @param date
	 * @return
	 */
	public static Date monthEnd(Date date) {
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(date);
		calendar.set(Calendar.DAY_OF_MONTH,
				calendar.getActualMaximum(Calendar.DAY_OF_MONTH));
		return dateEnd(calendar.getTime());
	}

	/**
	 * 昨天,明天,本周一,本周日,下周一,本月第一天,本月最后一天时间
	 */
	public static Date yesterday() {
		Date date = new Date();
		return addDays(date, -1);
	}

	public static Date tomorrow() {
		return addDays(new Date(), 1);
	}

	public static Date mondayOfThisWeek() {
		Calendar c = Calendar.getInstance();
		int dayofweek = c.get(Calendar.DAY_OF_WEEK) - 1;
		if (dayofweek == 0) {
			dayofweek = 7;
		}
		c.add(Calendar.DATE, -dayofweek + 1);
		return c.getTime();
	}

	public static Date sundayOfThisWeek() {
		Calendar cal = Calendar.getInstance();
		cal.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);
		cal.add(Calendar.WEEK_OF_YEAR, 1);
		return cal.getTime();
	}

	public static Date nextMondayOfWeek() {
		Calendar cal = Calendar.getInstance();
		cal.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);
		cal.add(Calendar.WEEK_OF_YEAR, 1);
		cal.add(Calendar.DATE, 1);
		return cal.getTime();
	}
	
	public static Date monthFirstDay(){
		return monthStart(new Date());
	}
	public static Date monthLastDay(){
		return monthEnd(new Date());
	}
}
