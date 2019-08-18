package sanri.utils.excel;

import java.lang.reflect.Method;

import org.apache.commons.lang3.StringUtils;

/**
 * 
 * 作者:sanri <br/>
 * 时间:2017-8-12下午2:47:48<br/>
 * 功能:列配置,用于排序 <br/>
 */
public class ColumnConfig implements Comparable<ColumnConfig>{
	private boolean hidden;
	private boolean chineseWidth;
	private String pattern;
	private String chinese;
	private int width;
	private int charWidth;
	private int pxWidth;
	private int index;
	//属性名,必须不为空
	private String propertyName;
	private Method readMethod;
	private Method writeMethod;
	private Class<?> dataType;
	
	public ColumnConfig(String propertyName, Method readMethod, Method writeMethod) {
		super();
		this.propertyName = propertyName;
		this.readMethod = readMethod;
		this.writeMethod = writeMethod;
	}
	
	/**
	 * 
	 * 作者:sanri <br/>
	 * 时间:2017-8-12下午2:56:02<br/>
	 * 功能:配置所有的配置,如果传入值为非默认值,则配置 <br/>
	 * @param chinese
	 * @param width
	 * @param index
	 * @param hidden
	 * @param pattern
	 */
	public void config(String chinese,int width,int charWidth,int pxWidth,int index,boolean hidden,String pattern,boolean chineseWidth){
		if(StringUtils.isNotBlank(chinese)){
			this.chinese = chinese;
		}
		this.width = width;
		this.charWidth = charWidth;
		this.pxWidth = pxWidth;
		if(index != -1){
			this.index = index;
		}
		this.hidden = hidden;
		if(StringUtils.isNotBlank(pattern)){
			this.pattern = pattern;
		}
		this.chineseWidth = chineseWidth;
	}

	public boolean isHidden() {
		return hidden;
	}

	public void setHidden(boolean hidden) {
		this.hidden = hidden;
	}

	public String getPattern() {
		return pattern;
	}

	public void setPattern(String pattern) {
		this.pattern = pattern;
	}

	public String getChinese() {
		return chinese;
	}

	public void setChinese(String chinese) {
		this.chinese = chinese;
	}

	public int getWidth() {
		return width;
	}

	public void setWidth(int width) {
		this.width = width;
	}

	public int getIndex() {
		return index;
	}

	public void setIndex(int index) {
		this.index = index;
	}

	public String getPropertyName() {
		return propertyName;
	}

	public void setPropertyName(String propertyName) {
		this.propertyName = propertyName;
	}

	public Method getReadMethod() {
		return readMethod;
	}

	public void setReadMethod(Method readMethod) {
		this.readMethod = readMethod;
	}

	public Method getWriteMethod() {
		return writeMethod;
	}

	public void setWriteMethod(Method writeMethod) {
		this.writeMethod = writeMethod;
	}
	
	@Override
	public int compareTo(ColumnConfig o) {
		if(this.index != -1 && o.index != -1){
			return this.index - o.index;
		}
		return this.propertyName.compareTo(o.propertyName);
	}

	public Class<?> getDataType() {
		return dataType;
	}

	public void setDataType(Class<?> dataType) {
		this.dataType = dataType;
	}

	public boolean isChineseWidth() {
		return chineseWidth;
	}

	public void setChineseWidth(boolean chineseWidth) {
		this.chineseWidth = chineseWidth;
	}

	public int getCharWidth() {
		return charWidth;
	}

	public void setCharWidth(int charWidth) {
		this.charWidth = charWidth;
	}

	public int getPxWidth() {
		return pxWidth;
	}

	public void setPxWidth(int pxWidth) {
		this.pxWidth = pxWidth;
	}
}
