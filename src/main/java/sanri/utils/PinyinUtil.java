package sanri.utils;

import net.sourceforge.pinyin4j.PinyinHelper;
import net.sourceforge.pinyin4j.format.HanyuPinyinCaseType;
import net.sourceforge.pinyin4j.format.HanyuPinyinOutputFormat;
import net.sourceforge.pinyin4j.format.HanyuPinyinToneType;
import net.sourceforge.pinyin4j.format.exception.BadHanyuPinyinOutputFormatCombination;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.helper.StringUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * 
 * 创建时间:2016-10-2上午9:48:28<br/>
 * 创建者:sanri<br/>
 * 功能:拼音处理工具类<br/>
 * <ul>
 * 	<li>HanyuPinyinToneType.WITH_TONE_NUMBER 用数字表示声调，例如：liu2</li>
 * 	<li>HanyuPinyinToneType.WITHOUT_TONE 无声调表示，例如：liu</li>
 * 	<li>HanyuPinyinToneType.WITH_TONE_MARK 用声调符号表示，例如：liú </li>
 * </ul>
 * 设置特殊拼音ü的显示格式： <br/>
 * <ul>
 * 	<li>HanyuPinyinVCharType.WITH_U_AND_COLON 以U和一个冒号表示该拼音，例如：lu: </li>
 *  <li>HanyuPinyinVCharType.WITH_V 以V表示该字符，例如：lv</li>
 *  <li>HanyuPinyinVCharType.WITH_U_UNICODE 以ü表示 lü</li>
 * </ul>
 */
public class PinyinUtil {
	private static PinyinUtil instance = new PinyinUtil();
	private HanyuPinyinOutputFormat default_format;
	
	private PinyinUtil(){
		default_format = new HanyuPinyinOutputFormat(); 
		default_format.setCaseType(HanyuPinyinCaseType.LOWERCASE);
		default_format.setToneType(HanyuPinyinToneType.WITHOUT_TONE);
//		DEFAULT_FORMAT.setVCharType(HanyuPinyinVCharType.WITH_V);
	}
	
	public static PinyinUtil getInstance(){
		return instance;
	}
	/**
	 * 
	 * 功能:需要特殊格式化的 pinyin 实例<br/>
	 * 创建时间:2016-10-2上午10:17:14<br/>
	 * 作者：sanri<br/>
	 */
	public static PinyinUtil getInstance(HanyuPinyinOutputFormat outputFormat){
		instance.default_format = outputFormat;
		return instance;
	}
	
	/**
	 * 
	 * 功能:得到全拼<br/>
	 * 创建时间:2016-10-2上午10:19:10<br/>
	 * 作者：sanri<br/>
	 */
	public String getPinyin(String source){
		if(StringUtils.isBlank(source)){
			return "";
		}
		char[] charArray = source.toCharArray();
		
		StringBuffer pinyinString = new StringBuffer();	//拼音字符串
		try{
			for (char sourceChar : charArray) {
				pinyinString.append(this.getPinyin(sourceChar));
			}
		}catch(BadHanyuPinyinOutputFormatCombination e){
			e.printStackTrace();
		}
		return pinyinString.toString();
	}
	/**
	 * 
	 * 功能:获取拼音首字母 <br/>
	 * 创建时间:2016-10-2上午10:46:50<br/>
	 * 作者：sanri<br/>
	 */
	public String getPinyinInitial(String source){
		if(StringUtil.isBlank(source)){
			return "";
		}
		char[] charArray = source.toCharArray();
		
		StringBuffer pinyinString = new StringBuffer();	//拼音字符串
		try{
			for (char sourceChar : charArray) {
				String pinyin = this.getPinyin(sourceChar);
				if(!StringUtil.isBlank(pinyin)){
					pinyinString.append(pinyin.charAt(0));
				}
			}
		}catch(BadHanyuPinyinOutputFormatCombination e){
			e.printStackTrace();
		}
		return pinyinString.toString();
	}
	
	/**
	 * 
	 * 功能:获取一个字符的拼音,非汉字返回原字符 <br/>
	 * 创建时间:2016-10-2上午10:26:26<br/>
	 * 作者：sanri<br/>
	 * @throws BadHanyuPinyinOutputFormatCombination 
	 */
	public String getPinyin(char source) throws BadHanyuPinyinOutputFormatCombination{
		String sourceCharString = Character.toString(source);
		if(RegexValidate.validate(sourceCharString, RegexValidate.CHINESE)){
			String[] hanyuPinyinStringArray = PinyinHelper.toHanyuPinyinStringArray(source,this.default_format);
			if(!RegexValidate.isEmpty(hanyuPinyinStringArray)){
				return hanyuPinyinStringArray[0];
			}
			return "";
		}
		return sourceCharString;
	}
	/**
	 * 
	 * 功能:获取所有拼音<br/>
	 * 创建时间:2016-10-2上午10:43:54<br/>
	 * 作者：sanri<br/>
	 */
	public List<String[]> getPinyinAll(String source){
		List<String[]> pinyinAll = new ArrayList<String[]>();
		if(StringUtil.isBlank(source)){
			return pinyinAll;
		}
		char[] charArray = source.toCharArray();
		try{
			for (char sourceChar : charArray) {
				pinyinAll.add(this.getPinyinAll(sourceChar));
			}
		}catch(BadHanyuPinyinOutputFormatCombination e){
			e.printStackTrace();
		}
		return pinyinAll;
	}
	/**
	 * 
	 * 功能:获取所有拼音<br/>
	 * 创建时间:2016-10-2上午10:43:54<br/>
	 * 作者：sanri<br/>
	 */
	public String[] getPinyinAll(char source) throws BadHanyuPinyinOutputFormatCombination{
		String sourceCharString = Character.toString(source);
		if(RegexValidate.validate(sourceCharString, RegexValidate.CHINESE)){
			return PinyinHelper.toHanyuPinyinStringArray(source,this.default_format);
		}
		return null;
	}
	
	public static void main(String[] args) {
		PinyinUtil instance2 = PinyinUtil.getInstance();
		String pinyin = instance2.getPinyinInitial("一地在要工sanri -- 上是中国同");
		System.out.println(pinyin);
	}
}
