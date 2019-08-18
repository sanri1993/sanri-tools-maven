//package sanri.utils;
//
//import java.beans.BeanInfo;
//import java.beans.IntrospectionException;
//import java.beans.Introspector;
//import java.beans.PropertyDescriptor;
//import java.lang.reflect.InvocationTargetException;
//import java.lang.reflect.Method;
//import java.text.ParseException;
//import java.util.ArrayList;
//import java.util.Date;
//import java.util.List;
//import java.util.Map;
//
//import org.apache.commons.beanutils.BeanUtils;
//import org.apache.commons.beanutils.ConvertUtils;
//import org.apache.commons.beanutils.Converter;
//import org.apache.commons.beanutils.PropertyUtils;
//import org.apache.commons.lang3.ObjectUtils;
//import org.apache.commons.lang3.StringUtils;
//import org.apache.commons.lang3.time.DateFormatUtils;
//import org.apache.commons.lang3.time.DateUtils;
//import org.apache.commons.logging.Log;
//import org.apache.commons.logging.LogFactory;
//
//import com.sanri.exception.BussinessException;
//
///**
// *
// * 作者:sanri <br/>
// * 时间:2017-8-28上午10:00:50<br/>
// * 功能:参数验证,参数复制功能 <br/>
// */
//public class ParamUtil {
//	private ParamUtil(){}
//	private static final Log logger = LogFactory.getLog(ParamUtil.class);
//
//	/**
//	 *
//	 * 作者:sanri <br/>
//	 * 时间:2017-8-28下午1:18:17<br/>
//	 * 功能:日期转换器 ,将字符串转为日期<br/>
//	 */
//	@SuppressWarnings("rawtypes")
//	final static class String2DateConverter implements Converter{
//		private String [] parsePatterns = {"yyyy-MM-dd"};
//		private String pattern = "yyyy-MM-dd";
//
//		@Override
//		public Object convert(Class clazz, Object obj) {
//			if(obj == null || clazz  == obj.getClass()){
//				//如果对象为空,或对象和 clazz 对象一样,则不需要转换
//				return obj;
//			}
//			if(clazz == Date.class){
//				//需要将 obj 转成 日期对象,断言 obj 是字符串对象
//				String dateString = ObjectUtils.toString(obj);
//				Date finalyDate = null;
//				try {
//					finalyDate = DateUtils.parseDate(dateString, parsePatterns);
//				} catch (ParseException e) {
//					e.printStackTrace();
//					throw new BussinessException(-420, "日期转换出错,原日期格式不正确");
//				}
//				return finalyDate;
//			}else if(clazz == String.class){
//				//需要将 obj 转字符串对象,断言 obj 是日期对象
//				Date date = (Date) obj;
//				String format = DateFormatUtils.format(date, pattern);
//				return format;
//			}
//			logger.error("转换失败:源对象 class 为:"+obj.getClass()+",目标对象 class 为:"+clazz);
//			return obj;
//		}
//
//	}
//	final static Converter string2DateConverter = new String2DateConverter();
//
//	static {
//		//类初始化时注册转换器
//		ConvertUtils.register(string2DateConverter, Date.class);
//		ConvertUtils.register(string2DateConverter, String.class);
//	}
//
//	/**
//	 *
//	 * 作者:sanri <br/>
//	 * 时间:2017-8-28上午10:02:27<br/>
//	 * 功能:验证参数是否有任何一个参数名是空值,如果有任何一个是空值,则抛出异常<br/>
//	 * 对于 String 类型,规则为 StringUtils.isBlank<br/>
//	 * <del>对于 原始型,规则为是否为原始值</del> 不验证<br/>
//	 * 对于 原始型包装类和对象类型,验证是否为 null<br/>
//	 * 数组不验证<br/>
//	 * 方法名解释: 断言不会有任何一个参数名的值是空的,即所有的都不会为空,只要有任何一个参数是空值,则抛出异常
//	 *  <br/>
//	 * @param param 参数对象
//	 * @param paramNames 参数名列表,如果参数名不传,则验证所有参数
//	 */
//	public static void assertNotAnyNull(Object param,String... paramNames) throws BussinessException{
//		if(param == null){
//			throw new BussinessException(-410, "参数对象为空");
//		}
//		try {
//			List<PropertyDescriptor> validteProperty = findValidateProperty(param, paramNames);
//			//验证需要验证的属性是否有空值
//			for (PropertyDescriptor propertyDescriptor : validteProperty) {
//				Method method = propertyDescriptor.getReadMethod();
//				method.setAccessible(true);
//				Object paramValue = method.invoke(param);
//				if(paramValue == null){
//					throw new BussinessException(-400, "参数"+propertyDescriptor.getName()+",参数不能为空值");
//				}
//				Class<? extends Object> valueClazz = paramValue.getClass();
//				if(valueClazz.isArray() || valueClazz.isPrimitive()){
//					continue;
//				}
//				if(valueClazz == String.class){
//					String trueValue = ObjectUtils.toString(paramValue);
//					if(StringUtils.isBlank(trueValue)){
//						throw new BussinessException(-400, "参数"+propertyDescriptor.getName()+",参数不能为空值");
//					}
//				}
//			}
//		} catch (IntrospectionException e) {
//			e.printStackTrace();
//		} catch (IllegalAccessException e) {
//			e.printStackTrace();
//		} catch (IllegalArgumentException e) {
//			e.printStackTrace();
//		} catch (InvocationTargetException e) {
//			e.printStackTrace();
//		} catch (NoSuchMethodException e) {			//调试阶段需要避免此错误
//			e.printStackTrace();
//		}
//	}
//
//	/**
//	 *
//	 * 作者:sanri <br/>
//	 * 时间:2017-8-28上午11:43:28<br/>
//	 * 功能: 验证所有的都为空,则抛出异常<br/>
//	 * 方法名解释 :　断言不是所有的参数都是空的，肯定有一个不为空，当所有的参数都为空时，抛出异常
//	 * @param param 验证对象
//	 * @param paramNames 需要验证的参数是否全为空,必须提供
//	 */
//	public static void assertNotAllNull(Object param,String ...paramNames) throws BussinessException{
//		if(param == null){
//			throw new BussinessException(-410, "参数对象为空");
//		}
//		try {
//			if(paramNames == null || paramNames.length == 0){
//				throw new BussinessException(-411, "需要提供判断的参数列表");
//			}
//			List<PropertyDescriptor> validteProperty = findValidateProperty(param, paramNames);
//			if(validteProperty.size() == 0){
//				//如果没有需要验证的参数,不做校验
//				return ;
//			}
//			int nullValueSize = 0;
//			//验证需要验证的属性是否有空值
//			for (PropertyDescriptor propertyDescriptor : validteProperty) {
//				Method method = propertyDescriptor.getReadMethod();
//				method.setAccessible(true);
//				Object paramValue = method.invoke(param);
//				if(paramValue == null){
//					nullValueSize ++;
//					continue;
//				}
//				Class<? extends Object> valueClazz = paramValue.getClass();
//				if(valueClazz.isArray() || valueClazz.isPrimitive()){
//					continue;
//				}
//				if(valueClazz == String.class){
//					String trueValue = ObjectUtils.toString(paramValue);
//					if(StringUtils.isBlank(trueValue)){
//						nullValueSize++;
//					}
//				}
//			}
//			//如果所有参数都为空,抛出参数异常
//			if(nullValueSize == validteProperty.size()){
//				String validateParamNames = StringUtils.join(paramNames,",");
//				throw new BussinessException(-400, "["+validateParamNames+"]不能同时为空");
//			}
//		} catch (IntrospectionException e) {
//			e.printStackTrace();
//		} catch (IllegalAccessException e) {
//			e.printStackTrace();
//		} catch (IllegalArgumentException e) {
//			e.printStackTrace();
//		} catch (InvocationTargetException e) {
//			e.printStackTrace();
//		} catch (NoSuchMethodException e) {			//调试阶段需要避免此错误
//			e.printStackTrace();
//		}
//	}
//
//	/**
//	 *
//	 * 作者:sanri <br/>
//	 * 时间:2017-8-28下午12:34:33<br/>
//	 * 功能:属性复制,使用 BeanUtil.copyProperty <br/>
//	 * BeanUtil.copyProperties 和 PropertyUtils.copyProperties 区别 <br/>
//	 * BeanUtil 会进行类型转换,默认初始化属性值
//	 * PropertyUtils 不支持类型转换
//	 * BeanUtil 支持类型:
//	 * 	* java.lang.BigDecimal
//		* java.lang.BigInteger
//		* boolean and java.lang.Boolean
//		* byte and java.lang.Byte
//		* char and java.lang.Character
//		* java.lang.Class
//		* double and java.lang.Double
//		* float and java.lang.Float
//		* int and java.lang.Integer
//		* long and java.lang.Long
//		* short and java.lang.Short
//		* java.lang.String
//		* java.sql.Date
//		* java.sql.Time
//		* java.sql.Timestamp
//		* 此方法扩展支持 java.util.Date 的转换
//	 */
//	public static void copyProperties(Object dest,Object orig){
//		try {
//			BeanUtils.copyProperties(dest, orig);
//		} catch (IllegalAccessException e) {
//			e.printStackTrace();
//		} catch (InvocationTargetException e) {
//			e.printStackTrace();
//		}
//	}
//
//	/**
//	 *
//	 * 作者:sanri <br/>
//	 * 时间:2017-8-28下午3:11:42<br/>
//	 * 功能:增加日期转换的 bean 复制 <br/>
//	 * @param bean
//	 * @param properties
//	 */
//	public static void populate(Object bean,Map properties){
//		try {
//			BeanUtils.populate(bean, properties);
//		} catch (IllegalAccessException e) {
//			e.printStackTrace();
//		} catch (InvocationTargetException e) {
//			e.printStackTrace();
//		}
//	}
//
//
//	/**
//	 *
//	 * 作者:sanri <br/>
//	 * 时间:2017-8-28下午12:15:50<br/>
//	 * 功能:查找需要验证的参数 <br/>
//	 * @param param
//	 * @param paramNames
//	 * @return
//	 * @throws IntrospectionException
//	 * @throws IllegalAccessException
//	 * @throws InvocationTargetException
//	 * @throws NoSuchMethodException
//	 */
//	private static List<PropertyDescriptor> findValidateProperty(Object param, String... paramNames) throws IntrospectionException, IllegalAccessException,
//			InvocationTargetException, NoSuchMethodException {
//		List<PropertyDescriptor> validteProperty = new ArrayList<PropertyDescriptor>();
//		//获取参数的所有 get 方法,判断指定参数名是否空
//		Class<? extends Object> clazz = param.getClass();
//		BeanInfo beanInfo = Introspector.getBeanInfo(clazz, Object.class);
//		PropertyDescriptor[] propertyDescriptors = beanInfo.getPropertyDescriptors();
//		//获取所有需要验证的 get 方法
//		if(propertyDescriptors != null && propertyDescriptors.length > 0){
//			//如果没有指定参数名数组,取全部参数
//			if(paramNames == null || paramNames.length == 0){
//				for (PropertyDescriptor propertyDescriptor : validteProperty) {
//					validteProperty.add(propertyDescriptor);
//				}
//			}
//			//否则取部分参数
//			for (String paramName : paramNames) {
//				PropertyDescriptor propertyDescriptor = PropertyUtils.getPropertyDescriptor(param, paramName);
//				validteProperty.add(propertyDescriptor);
//			}
//		}
//		return validteProperty;
//	}
//
//}
