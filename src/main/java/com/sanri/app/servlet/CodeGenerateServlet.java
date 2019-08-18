package com.sanri.app.servlet;

import com.sanri.app.BaseServlet;
import com.sanri.app.ConfigCenter;
import com.sanri.app.jdbc.Column;
import com.sanri.app.jdbc.ExConnection;
import com.sanri.app.jdbc.JavaProperty;
import com.sanri.app.jdbc.Table;
import com.sanri.app.jdbc.codegenerate.GenerateConfig;
import com.sanri.app.jdbc.codegenerate.JavaPojo;
import com.sanri.app.jdbc.codegenerate.MybatisTypeMapper;
import com.sanri.app.jdbc.codegenerate.RenamePolicy;
import com.sanri.app.jdbc.codegenerate.RenamePolicyDefault;
import com.sanri.app.jdbc.codegenerate.RenamePolicyMybatisExtend;
import com.sanri.app.jdbc.codegenerate.RenamePolicyaBExtend;
import com.sanri.app.postman.JdbcConnDetail;
import com.sanri.frame.DispatchServlet;
import com.sanri.frame.RequestMapping;
import com.sanri.initexec.InitJdbcConnections;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import sanri.utils.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.SQLException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RequestMapping("/code")
@SuppressWarnings({ "unchecked", "rawtypes" })
public class CodeGenerateServlet extends BaseServlet {

	//类型映射配置
	public static final  Map<String,Map<String,String>> TYPE_MIRROR_MAP = new HashMap<>();
	private static File pojoPath = null;
	private static File mybatisPath = null;
	private static File projectPath = null;
	private static File projectCodePath = null;
	private static File templateCodePath = null;
	private static File tableTemplateCodePath = null;

	static{
		//读取数据库类型到java类型映射配置
		ConfigCenter configCenter = ConfigCenter.getInstance();
		List<String> dbTypes = configCenter.getList("mapper_jdbc_java","supports.dbType",String.class);
		for (String dbType : dbTypes) {
			Map<String, String> typeMirror = configCenter.getSubConfigs("mapper_jdbc_java", dbType);
			TYPE_MIRROR_MAP.put(dbType,typeMirror);
		}

		pojoPath = mkTmpPath("generate/pojo");
		mybatisPath = mkTmpPath("generate/mybatisPath");
		projectPath = mkTmpPath("generate/projectPath");
		projectCodePath = mkTmpPath("generate/projectCodePath");
		tableTemplateCodePath = mkTmpPath("generate/tableTemplateCodePath");

		templateCodePath = mkConfigPath("templateCodePath");
	}

	/**
	 *
	 * 功能: 构建 java pojo 对象<br/>
	 * 创建时间:2017-7-8下午5:20:46<br/>
	 * 作者：sanri<br/>
	 * @param connName 		连接名
	 * @param dbName		库名
	 * @param tableName		表名
	 * @param model			生成模式
	 * @param packageName   包名
	 * @param baseEntity	基类
	 * @param interfaces	实现的接口
	 * @param excludeColums 排除的列
	 * @param 命名策略 ,因为暂时只有下划线转驼峰,所以暂时不管
	 * <br/>
	 */
	//使用默认命名策略
	private RenamePolicy renamePolicy = new RenamePolicyDefault(TYPE_MIRROR_MAP);
	private RenamePolicy extendRenamePolicy = new RenamePolicyaBExtend(TYPE_MIRROR_MAP);
	@RequestMapping("/build/javabean")
	public String buildJavaBean(String connName,String dbName,String tableName,String model,String packageName,String baseEntity,String [] interfaces,String[] excludeColumns,String [] supports){
        ExConnection exConnection = InitJdbcConnections.CONNECTIONS.get(connName);
        Table table = exConnection.getTable(dbName, tableName);
        if(table == null){
			return "";
		}
		JavaPojo javapojo = new JavaPojo(model, packageName, table,extendRenamePolicy,supports);
		if(StringUtils.isNotBlank(baseEntity)){
			javapojo.setExtendsName(baseEntity);
		}
		if(!RegexValidate.isEmpty(interfaces)){
			javapojo.setInterfaces(Arrays.asList(interfaces));
		}
		if(!RegexValidate.isEmpty(excludeColumns)){
			javapojo.setExcludeColumns(Arrays.asList(excludeColumns));
		}
		List<String> javaCode = javapojo.build();
		File writerBean = javapojo.writerBean(javaCode,pojoPath);
		return writerBean.getName();
	}

	/**
	 * 构建 xml 文件
	 * @param connName 		连接名
	 * @param dbName		库名
	 * @param tableName		表名
	 * @param namespace		xml 中的命名空间  (全路径 )
	 * @param beanType		xml 中的实体类类型 (全路径)
	 * @param initSql
	 * @return
	 */
	@RequestMapping("/build/mybatis")
	public String buildMybatisCode(String connName, String dbName, String tableName, String namespace, String beanType, Map<String,String> initSql){
        ExConnection exConnection = InitJdbcConnections.CONNECTIONS.get(connName);
        Table table = exConnection.getTable(dbName, tableName);
        if(table == null){
			return "";
		}

		Map<String,Object> context = new HashMap<String, Object>();
		context.put("namespace",namespace);
		context.put("beanType",beanType);
		context.put("table",table);
		context.put("renamePolicy",extendRenamePolicy);
		context.put("initSql",initSql);
		context.put("typeMapper",exConnection.getRenamePolicyMybatis());
		String className = extendRenamePolicy.mapperClassName(tableName);
		File file = new File(mybatisPath, className + ".xml");
		try {
			String formatString = VelocityUtil.formatFile("/com/sanri/config/templates/mybatis.tpl",charset, context);
			FileUtils.writeStringToFile(file, formatString);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return file.getName();
	}

	@RequestMapping("/mapper/className")
	public String mapperClassName(String tableName){
		return extendRenamePolicy.mapperClassName(tableName);
	}

	/**
	 *
	 * 功能:下载文件<br/>
	 * 创建时间:2017-7-9上午8:20:32<br/>
	 * 作者：sanri<br/>
	 * @param typeName 类型名称 pojo/project/projectCode
	 * @param fileName 文件名称
	 * @param request
	 * @param response<br/>
	 * @throws IOException
	 */
	public void downFile(String typeName,String fileName,HttpServletRequest request,HttpServletResponse response) throws IOException{
		File filePath = null;
		if("pojo".equals(typeName)){
			filePath = pojoPath;
		}else if("project".equals(typeName)){
			filePath = projectPath;
		}else if("projectCode".equals(typeName)){
			filePath = projectCodePath;
		}else if("mybatis".equals(typeName)){
			filePath = mybatisPath;
		}else{
			throw new IllegalArgumentException("不支持的类型");
		}
		File downFile = new File(filePath,fileName);
		if(!downFile.exists()){
			throw new IllegalArgumentException("文件不存在");
		}
		File targetFile = downFile;
		if(downFile.isDirectory()){
			targetFile = new File(downFile.getParent(),downFile.getName()+".zip");
			ZipUtil.zip(downFile, targetFile);
		}
		FileInputStream fileInputStream = null;
		try {
			fileInputStream = new FileInputStream(targetFile);
			download(fileInputStream, MimeType.AUTO, targetFile.getName(), request, response);
		}finally{
			IOUtils.closeQuietly(fileInputStream);
		}
	}

	/**
	 *
	 * 功能:加载模板<br/>
	 * 创建时间:2017-7-15上午10:09:43<br/>
	 * 作者：sanri<br/>
	 * @return<br/>
	 * @throws IOException
	 */
	public Map<String,String> loadTemplate(String frameworkName,String templateName) throws IOException{
		String templatePath = PathUtil.pkgPath("com.sanri.config.templates");
		Map<String,String> ret = new HashMap<String, String>();
		File tplFile = new File(templatePath,templateName+".tpl");
		if(!tplFile.exists()){
			File templateDir = new File(templatePath,frameworkName);
			tplFile = new File(templateDir,templateName+".tpl");
			if(!tplFile.exists()){
				ret.put("result", "-1");
				return ret;
			}
		}
		String readFileToString = FileUtils.readFileToString(tplFile,"utf-8");
		ret.put("result", "0");
		ret.put("template", readFileToString);
		return ret;
	}

	/***
	 *
	 * 作者:sanri <br/>
	 * 时间:2017-7-21下午4:43:36<br/>
	 * 功能:项目构建  <br/>
	 * @param generateConfig
	 * @return
	 */
	public String buildProject(GenerateConfig generateConfig) throws IllegalArgumentException, SQLException {
		String model = generateConfig.getModel();
		String framework = generateConfig.getFramework();
		String connName = generateConfig.getConnName();
		String dbName = generateConfig.getDbName();
		if(StringUtils.isBlank(model) || StringUtils.isBlank(framework) || StringUtils.isBlank(connName) || StringUtils.isBlank(dbName)){
			throw new IllegalArgumentException("参数错误:model:"+model+",framework:"+framework+",connName:"+connName+",dbName:"+dbName);
		}
		List<String> tables = generateConfig.getTables();
		if(RegexValidate.isEmpty(tables)){
			//没有需要生成的数据
			return "";
		}
		//获取元数据信息
        ExConnection exConnection = InitJdbcConnections.CONNECTIONS.get(connName);
        List<Table> tableMetas = exConnection.tables(dbName, false);
		//生成的代码命名方式为 connName_dbName_时间戳
		String generateFileName = connName +"_"+dbName+System.currentTimeMillis();
		File generateFileDir = new File(projectCodePath,generateFileName);
		generateFileDir.mkdir();
		//开始生成
		if(!RegexValidate.isEmpty(tableMetas)){
			if("ssm$ssh".indexOf(framework) == -1){
				throw new IllegalArgumentException("不支持的框架:"+framework);
			}
			//生成 controller,entity,service,serviceimpl,dao,daoimpl,xml 目录
			File controllerDir = buildPackageDir(generateFileDir,generateConfig.getControllerPackage());
			File entityDir = buildPackageDir(generateFileDir,generateConfig.getEntityPackage());
			File serviceDir = buildPackageDir(generateFileDir,generateConfig.getServicePackage());
			File serviceImplDir = buildPackageDir(generateFileDir,generateConfig.getServiceimplPackage());
			File daoDir = buildPackageDir(generateFileDir,generateConfig.getDaoPackage());
			File daoImplDir = buildPackageDir(generateFileDir,generateConfig.getDaoimplPackage());
			File xmlDir = new File(generateFileDir,"xml");

			for (Table table : tableMetas) {
				//对于每一个表,先生成 javaPojo
				JavaPojo javaPojo = new JavaPojo(model, generateConfig.getEntityPackage(), table, extendRenamePolicy);
				String excludeColumns = generateConfig.getExcludeColumns();
				if(StringUtils.isNotBlank(excludeColumns)){
					String[] excludeColumnArray = excludeColumns.split(",");
					javaPojo.setExcludeColumns(Arrays.asList(excludeColumnArray));
				}
				String baseEntity = generateConfig.getBaseEntity();
				if(StringUtils.isNotBlank(baseEntity)){
					javaPojo.setExtendsName(baseEntity);
				}else{
					//如果有继承,就不需要写实现
					String interfaces = generateConfig.getInterfaces();
					if(StringUtils.isNotBlank(interfaces)){
						String[] interfaceArray = interfaces.split(",");
						javaPojo.setInterfaces(Arrays.asList(interfaceArray));
					}
				}

				List<String> pojoJavaCode = javaPojo.build();
				javaPojo.writerBean(pojoJavaCode, entityDir);

				//获取上下文信息
				Map<String,Object> context = new HashMap<String, Object>();
				String className = javaPojo.getClassName();
				String lowEntityName = StringUtils.uncapitalize(className);
				context.put("lowEntity", lowEntityName);
				context.put("entity", className);
				context.put("basePackage", generateConfig.getBasePackage());
				context.put("controllerPackage", generateConfig.getControllerPackage());
				context.put("servicePackage", generateConfig.getServicePackage());
				context.put("serviceImplPackage", generateConfig.getServiceimplPackage());
				context.put("daoPackage", generateConfig.getDaoPackage());
				context.put("daoImplPackage", generateConfig.getDaoimplPackage());
				context.put("entityPackage", generateConfig.getEntityPackage());
				context.put("tableName", table.getTableName());
				context.put("chineseEntity",table.getComments());
				context.put("datetime", DateFormatUtils.format(System.currentTimeMillis(), datetimePattern));

				logger.info("正在以 "+generateConfig.getFramework()+" 框架生成表 :"+table.getTableName()+" 的文件代码,使用上下文:"+context);

				//先生成通用的 controller,service,serviceImpl
				Map<String, String> templates = generateConfig.getTemplates();
				String controllerCode = templates.get("controller");
				String serviceCode = templates.get("service");
				String serviceImplCode = templates.get("serviceimpl");
				try {
					controllerCode = VelocityUtil.formatString(controllerCode, context);
					serviceCode = VelocityUtil.formatString(serviceCode,context);
					serviceImplCode = VelocityUtil.formatString(serviceImplCode, context);
				} catch (Exception e1) {
					logger.error("模板文件未找到["+controllerCode+","+serviceCode+","+serviceImplCode+"]");
					e1.printStackTrace();
				}

				try {
					FileUtils.writeStringToFile(new File(controllerDir,className+"Controller.java"), controllerCode);
					FileUtils.writeStringToFile(new File(serviceDir,className+"Service.java"), serviceCode);
					FileUtils.writeStringToFile(new File(serviceImplDir,className+"ServiceImpl.java"), serviceImplCode);
				} catch (IOException e) {
					e.printStackTrace();
				}

				//其它文件用模板生成
				if("ssm".equalsIgnoreCase(framework)){
					ssmGenerate(generateConfig,context,javaPojo,daoDir,xmlDir);
				}else if("ssh".equalsIgnoreCase(framework)){
					sshGenerate(generateConfig,context,javaPojo,daoDir,daoImplDir);
				}
			}
		}

		String fileName = generateFileDir.getName();
		File zipProjectCodeFile = new File(generateFileDir.getParentFile(), fileName+".zip");
		ZipUtil.zip(generateFileDir,zipProjectCodeFile);
		return zipProjectCodeFile.getName();
	}

	/**
	 *
	 * 作者:sanri <br/>
	 * 时间:2017-7-21下午6:38:09<br/>
	 * 功能:建立包路径 <br/>
	 * @param baseDir
	 * @param package_
	 * @return
	 */
	private File buildPackageDir(File baseDir,String package_){
		if(StringUtils.isBlank(package_)){
			return baseDir;
		}
		String[] dirs = package_.split("\\.");
		if(!RegexValidate.isEmpty(dirs)){
			File parentDir = baseDir;
			for (String dir : dirs) {
				parentDir = new File(parentDir,dir);
				if(!parentDir.exists()){
					parentDir.mkdir();
				}
			}
			return parentDir;
		}
		return baseDir;
	}

	/**
	 *
	 * 作者:sanri <br/>
	 * 时间:2017-7-21下午6:02:25<br/>
	 * 功能:ssh 项目代码生成 <br/>
	 * @param generateConfig
	 * @param javaPojo
	 * @param daoImplDir
	 * @param daoDir
	 */
	private void sshGenerate(GenerateConfig generateConfig,Map<String,Object> context,JavaPojo javaPojo, File daoDir, File daoImplDir) {
		Map<String, String> templates = generateConfig.getTemplates();
		try {
			String daoCode = VelocityUtil.formatString(templates.get("dao"), context);
			String daoImplCode = VelocityUtil.formatString(templates.get("daoimpl"), context);
			String className = javaPojo.getClassName();
			FileUtils.writeStringToFile(new File(daoDir,className+"Dao.java"),daoCode );
			FileUtils.writeStringToFile(new File(daoImplDir,className+"DaoImpl.java"), daoImplCode);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 *
	 * 作者:sanri <br/>
	 * 时间:2017-7-21下午6:02:37<br/>
	 * 功能:ssm 项目代码生成 <br/>
	 * @param generateConfig
	 * @param javaPojo
	 * @param xmlDir
	 * @param mapperDir
	 * @return
	 */
	private void ssmGenerate(GenerateConfig generateConfig,Map<String,Object> context,JavaPojo javaPojo,  File mapperDir, File xmlDir) {
		Map<String, String> templates = generateConfig.getTemplates();

		try {
			String daoCode = VelocityUtil.formatString(templates.get("mapper"), context);
			String daoImplCode = VelocityUtil.formatString(templates.get("xml"), context);
			String className = javaPojo.getClassName();
			FileUtils.writeStringToFile(new File(mapperDir,className+"Mapper.java"),daoCode );
			FileUtils.writeStringToFile(new File(xmlDir,className+".xml"), daoImplCode);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * 新版本 spring boot 生成方式
	 */
	private void springBootGenerate(){

	}

	/**
	 * 保存一个模板代码
	 * @param codename
	 * @param codesource
	 */
	public int saveTemplate(String codename,String codesource) throws IOException {
		File file = new File(templateCodePath, codename);
		FileUtils.writeStringToFile(file,codesource);
		return 0;
	}

	/**
	 * 查询模板代码列表
	 * @return
	 */
	public String[] templateCodes(){
		return templateCodePath.list();
	}

	/**
	 * 读取某一个模板代码
	 * @param codename
	 * @return
	 * @throws IOException
	 */
	public String loadTemplateCode(String codename) throws IOException {
		File file = new File(templateCodePath, codename);
		return FileUtils.readFileToString(file);
	}

	/**
	 * 模板代码转换预览
	 * @param templateName
	 * @param connName
	 * @param schemaName
	 * @param tableName
	 * @return
	 */
	public String codeConvertPreview(String templateName,String connName,String schemaName,String tableName) throws IOException {
		ExConnection exConnection = InitJdbcConnections.CONNECTIONS.get(connName);
		Table table = exConnection.getTable(schemaName, tableName);
		String className = extendRenamePolicy.mapperClassName(tableName);
		List<JavaProperty> javaProperties = new ArrayList<>();
		List<Column> columns = table.getColumns();
		for (Column column : columns) {
			String columnName = column.getColumnName();
			String propertyName = extendRenamePolicy.mapperPropertyName(columnName);
			String propertyType = renamePolicy.mapperPropertyType(column.getColumnType().getDataType(),exConnection.getDbType());
			String jdbcTypeName = exConnection.getRenamePolicyMybatis().mapperJdbcTypeName(column.getColumnType().getDataType());

			JavaProperty javaProperty = new JavaProperty(propertyName, propertyType, columnName);
			javaProperty.setComments(column.getComments());
			javaProperty.setJdbcType(jdbcTypeName);
			javaProperty.setPrimaryKey(column.isPrimaryKey());
			javaProperties.add(javaProperty);
		}

		//设置所有上下文参数
		Map<String,Object> context = new HashMap<>();
		context.put("DATE",DateFormatUtils.ISO_DATE_FORMAT.format(System.currentTimeMillis()));
		context.put("TIME",DateFormatUtils.ISO_TIME_NO_T_FORMAT.format(System.currentTimeMillis()));
		context.put("AUTHOR",System.getProperty("user.name"));

		JdbcConnDetail connDetail = exConnection.getConnDetail();
		context.put("CONNECTION",connName);
		context.put("IPPORT",connDetail.getHost()+":"+connDetail.getPort());
		context.put("SCHEMA",schemaName);
		context.put("TABLE",tableName);
		context.put("TABLE_COMMENTS",table.getComments());
		context.put("COLUMNS",columns);

		context.put("PO_NAME",className);
		context.put("PO_NAME_LOWER",StringUtils.uncapitalize(className));
		context.put("PROPERTIES",javaProperties);

		//获取模板,生成最终代码文件
		FileManagerServlet projectConfigServlet = DispatchServlet.getServlet(FileManagerServlet.class);
		String templateCode = projectConfigServlet.readConfig(modul, templateName);
		String formatCode = VelocityUtil.formatString(templateCode, context);
		return formatCode;
	}

	/**
	 * 单表模板代码 由模板得到文件;这个生成好不能修改，可以再生成一个
	 * @param ticket 本次生成的文件路径,最后做统一下载
	 * @param templateName
	 * @param connName
	 * @param schemaName
	 * @param tableName
	 * @return
	 */
	final String modul = "tableTemplate";
	Pattern pattern = Pattern.compile("public\\s+(class|interface)\\s+(\\w+)");
	public String templateConvert(String ticket,String templateName,String connName,String schemaName,String tableName) throws IOException {
		File codePath = null;
		if(StringUtils.isBlank(ticket)){
			ticket = SignUtil.uniqueTimestamp();
		}
		codePath = new File(tableTemplateCodePath,ticket);
		if(!codePath.exists()){codePath.mkdir();}

		String formatCode = codeConvertPreview(templateName, connName, schemaName, tableName);
		String publicClassName = matchClassName(templateName,tableName, formatCode);

		File javaFile = new File(codePath, publicClassName + ".java");
		FileUtils.writeStringToFile(javaFile,formatCode);

		return ticket;
	}

	private String matchClassName(String templateName,String tableName, String formatCode) {
		//提取文件名,这里不能区分内部类 TODO ,所以模板文件中不要写内部类
		Matcher matcher = pattern.matcher(formatCode);
		String publicClassName = tableName+'.'+templateName;
		if(matcher.find()){
			publicClassName = matcher.group(2);
		}
		return publicClassName;
	}

	/**
	 * 多表方案生成
	 * @param connName
	 * @param schemaName
	 * @param tableNames
	 * @param codeSchemaName
	 * @return
	 */
	public String multiTableSchemaConvert(String connName,String schemaName,List<String> tableNames,String codeSchemaName) throws SQLException, IOException {
		String partPath = SignUtil.uniqueTimestamp();
		File codePath = new File(tableTemplateCodePath,partPath);
		codePath.mkdir();

		String[] templateNames = StringUtils.split(codeSchemaName, '+');

		for (String tableName : tableNames) {
			for (String templateName : templateNames) {
				String partCode = codeConvertPreview(templateName, connName, schemaName, tableName);
				String className = matchClassName(templateName, tableName, partCode);
				File file = new File(codePath, className + ".java");
				FileUtils.writeStringToFile(file,partCode);
			}
		}

		return partPath;
	}
}
