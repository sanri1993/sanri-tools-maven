package com.sanri.app.jdbc.codegenerate;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import sanri.utils.RegexValidate;

import com.sanri.app.jdbc.Column;
import com.sanri.app.jdbc.Table;
/**
 * 
 * @author sanri
 * 传入类型时,如果不是 java.lang 类型或基本类型时,需完整类名
 */
public class JavaBean{
	//codes
	private String packageName;
	private String className;
	private Map<String,String> propertys;
	private String extendsName;
	private List<String> excludeColumns;
	//comments
	private String classComment;
	private Map<String,String> propertysComments = new HashMap<String, String>();

	//content mark
	private Table table;
	private Map<String,Column> columns;
	
	//the constants 
	public static final String PLACEHOLDER="\t";
	public static final String STATEMENT_END=";";
	public static final String N = "\n";
	public static final String COMMENTS_LINE = "//";
	public static final String COMMENTS_MULTI_LINE_BEGIN = "/**";
	public static final String COMMENTS_MULTI_LINE_END = " */";
	public static final String BODY_BEGIN = "{";
	public static final String BODY_END = "}";

	/**
	 * 功能:构建 javabean 代码,并把代码一行一行放到 List<String> 中
	 * 创建时间:2016-4-15下午9:40:09
	 * 作者：sanri
	 * 入参说明:it self 
	 *　出参说明：List<String> 一行一行的代码
	 * @return
	 */
	public List<String> build(){
		
		List<String> classCode = new ArrayList<String>();
		Set<String> headList = new LinkedHashSet<String>();
		List<String> body = new ArrayList<String>();
		List<String> fields = new ArrayList<String>();
		List<String> methods = new ArrayList<String>();
		
		headList.add("package "+packageName+STATEMENT_END+N);
//		headList.add("import java.io.Serializable"+STATEMENT_END); //到基类中实现序列化
		if(!StringUtils.isBlank(classComment)){
			body.add(COMMENTS_MULTI_LINE_BEGIN);
			body.add(" * "+classComment);
			body.add(COMMENTS_MULTI_LINE_END);
		}
		StringBuffer line = new StringBuffer("public class "+className);
//		body.add("public class "+className);
		if(!StringUtils.isBlank(extendsName)){
			String extendsBeanName = extendsName;
			if(extendsName.indexOf(".") != -1){
				headList.add("import "+extendsName+STATEMENT_END);
				extendsBeanName = extendsName.substring(extendsName.lastIndexOf(".") + 1);
			}
			line.append(" extends "+extendsBeanName);
		}
//		line.append(" implements Serializable ");
		body.add(line.toString()+BODY_BEGIN);
		if(propertys != null && !propertys.isEmpty()){
			for (Iterator<Entry<String, String>> it =  propertys.entrySet().iterator();it.hasNext();) {
				Entry<String, String> columnEntry = it.next();
				String column = columnEntry.getKey();
				if(!RegexValidate.isEmpty(excludeColumns) && excludeColumns.contains(column)){continue;}
				String colType = columnEntry.getValue();
				String colComment = propertysComments.get(column);
				
				if(!StringUtils.isBlank(colComment)){
					fields.add(PLACEHOLDER+COMMENTS_LINE+" "+colComment);
				}
				//引入其它类型
				String typeName = colType; 
				if(colType.indexOf(".") != -1){
					headList.add("import "+colType+STATEMENT_END);
					typeName = colType.substring(colType.lastIndexOf(".") + 1);
				}
				fields.add(PLACEHOLDER+"private "+typeName+ " "+column+STATEMENT_END);
				buildMethod(methods,column,typeName);
			}
		}
		body.addAll(fields);
		body.addAll(methods);
		body.add(BODY_END);
		classCode.addAll(headList);
		classCode.addAll(body);
		
		return classCode;
	}
	/*
	 * 构造 set & get 方法
	 */
	private void buildMethod(List<String> methods, String column, String colType) {
		//set method
		methods.add(PLACEHOLDER+"public void set"+StringUtils.capitalize(column)+"("+colType+" "+column+"){");
		methods.add(PLACEHOLDER+PLACEHOLDER+"this."+column+" = "+column+STATEMENT_END);
		methods.add(PLACEHOLDER+"}");
		//get method
		methods.add(PLACEHOLDER+"public "+colType+" get"+StringUtils.capitalize(column)+"(){");
		methods.add(PLACEHOLDER+PLACEHOLDER+"return this."+column+STATEMENT_END);
		methods.add(PLACEHOLDER+"}");
	}
	
	/**
	 * 功能:将代码列表写出到文件<br/>
	 * 创建时间:2016-9-25下午2:33:27<br/>
	 * 作者：sanri<br/>
	 * 入参说明:代码列表<br/>
	 * 出参说明：写出的文件<br/>
	 */
	public File writerBean(List<String> javaClass,File outputDir){
		File javaFile = new File(outputDir, className+".java");
		FileWriter fr = null;
		try {
			fr = new FileWriter(javaFile); 
			IOUtils.writeLines(javaClass, N, fr);
		} catch (IOException e) {
			e.printStackTrace();
		}finally{
			IOUtils.closeQuietly(fr);
		}
		
		return javaFile;
	}
	
	public File writerBean(List<String> javaClass,String outputPath){
		File outputDir = new File(outputPath);
		if(!outputDir.exists())outputDir.mkdirs();
		return writerBean(javaClass, outputDir);
	}

	public String getPackageName() {
		return packageName;
	}
	public void setPackageName(String packageName) {
		this.packageName = packageName;
	}
	public String getClassName() {
		return className;
	}
	public void setClassName(String className) {
		this.className = className;
	}
	public String getClassComment() {
		return classComment;
	}
	public void setClassComment(String classComment) {
		this.classComment = classComment;
	}

	
	public static void main(String[] args) {
		JavaBean javaBean = new JavaBean();
		javaBean.setClassName("HelloWord");
		javaBean.setClassComment("@table hello_word");
		javaBean.setPackageName("com.sanri");
		Map<String,String> propertys = new HashMap<String, String>();
		Map<String,String> propertysComments = new HashMap<String, String>();
		javaBean.setPropertysComments(propertysComments);
		javaBean.setPropertys(propertys);
		
		propertys.put("username", "String");
		propertys.put("age", "int");
		propertys.put("time", "java.util.Date");
		propertysComments.put("username", "用户名,可重复");
		propertysComments.put("time", "格式 yyyy-MM-dd");
		
		List<String> build = javaBean.build();
		File writerBean = javaBean.writerBean(build, "d:/abcd");
		System.out.println(writerBean);
		
	}
	public Table getTable() {
		return table;
	}
	public void setTable(Table table) {
		this.table = table;
	}
	public String getExtendsName() {
		return extendsName;
	}
	public void setExtendsName(String extendsName) {
		this.extendsName = extendsName;
	}
	public List<String> getExcludeColumns() {
		return excludeColumns;
	}
	public void setExcludeColumns(List<String> excludeColumns) {
		this.excludeColumns = excludeColumns;
	}
	public Map<String, String> getPropertys() {
		return propertys;
	}
	public void setPropertys(Map<String, String> property) {
		this.propertys = property;
	}
	public Map<String, Column> getColumns() {
		return columns;
	}
	public void setColumns(Map<String, Column> columns) {
		this.columns = columns;
	}
	public Map<String, String> getPropertysComments() {
		return propertysComments;
	}
	public void setPropertysComments(Map<String, String> propertysComments) {
		this.propertysComments = propertysComments;
	}
}
