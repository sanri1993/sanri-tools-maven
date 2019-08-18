//package com.sanri.app.jdbc.codegenerate;
//
//import java.io.File;
//import java.io.IOException;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//
//import com.sanri.app.jdbc.ExConnection;
//import com.sanri.initexec.InitJdbcConnections;
//import org.apache.commons.lang3.StringUtils;
//import org.apache.commons.lang3.time.DateFormatUtils;
//import org.apache.commons.logging.Log;
//import org.apache.commons.logging.LogFactory;
//
//import sanri.utils.VelocityUtil;
//
//import com.sanri.app.jdbc.Column;
//import com.sanri.app.jdbc.Table;
//
//public class SSMGenerate {
//	private static Log logger = LogFactory.getLog(SSMGenerate.class);
//	private GenerateConfig generateConfig;
//	private String connName;
//	private File basePath;				//生成的路径 到 generate
//	private List<String> tables;
//	private RenamePolicy renamePolicy;
//	private File buildPath;				//本次代码生成构建路径 表名_表数量_日期
//	private File zipFile;
//	private Map<String,List<String>> excludeColumnsMap;
//
//	/**
//	 *
//	 * 作者:sanri <br/>
//	 * 时间:2017-4-24下午2:00:46<br/>
//	 * 功能:构建 ssm 项目 <br/>
//	 * 入参: <br/>
//	 */
//	public File build(){
//		long startTime = System.currentTimeMillis();
//		//创建基础路径
//		if(!basePath.exists()){
//			basePath.mkdir();
//		}
//		List<Table> generateTables = MetaManager.tables(connName, tables);
//        ExConnection exConnection = InitJdbcConnections.CONNECTIONS.get(connName);
//        String now = DateFormatUtils.format(startTime, "yyyyMMddHHmmss");
//		File currentPath = new File(basePath,connName+"_"+tables.size()+"_"+now);
//		logger.info("ssm 项目生成 ,基路径为 "+currentPath+",需要的表为 :"+generateTables);
//		this.buildPath = currentPath;
//		//获取构建配置
//		String controllerPackage = generateConfig.getControllerPackage();
//		String servicePackage = generateConfig.getServicePackage();
//		String serviceImplPackage = generateConfig.getServiceImplPackage();
//		String daoPackage = generateConfig.getDaoPackage();
//		String daoImplPackage = generateConfig.getDaoImplPackage();
//		String voPackage = generateConfig.getVoPackage();
//		String modelPackage = generateConfig.getModelPackage();
//
//		if(!Validate.isEmpty(generateTables)){
//			//目录生成
//			File controllerDir = new File(currentPath,"controller");
//			File serviceDir = new File(currentPath,"service");
//			File serviceImplDir = new File(serviceDir,"impl");
//			File mapperDir = new File(currentPath,"mapper");
//			File xmlDir = new File(currentPath,"xml");
//			File voDir = new File(currentPath,"vo");
//			File modelDir = new File(currentPath,"model");
//			dirCreate(currentPath,controllerDir,serviceDir,serviceImplDir,mapperDir,xmlDir,voDir,modelDir);
//
//			for (Table table : generateTables) {
//				//javaBean 生成
//				buildJavaBean(table,voPackage,voDir);
//				String tableName = table.getTableName();
//				String tableComments = table.getComments();
//				if(!StringUtils.isBlank(tableComments)){
//					tableComments = tableName;
//				}
//				String className = renamePolicy.mapperClassName(tableName);
//				String lowEntityName = StringUtils.uncapitalize(className);
//
//				Map<String,Object> context = new HashMap<String, Object>();
//				context.put("lowEntity", lowEntityName);
//				context.put("entity", className);
//				context.put("controllerPackage", controllerPackage);
//				context.put("servicePackage", servicePackage);
//				context.put("serviceImplPackage", serviceImplPackage);
//				context.put("daoPackage", daoPackage);
//				context.put("daoImplPackage", daoImplPackage);
//				context.put("voPackage", voPackage);
//				context.put("modelPackage", modelPackage);
//				context.put("tableName", tableName);
//				context.put("chineseEntity",tableComments );
//
//				try {
//					//文件生成
//					File serviceFile = new File(serviceDir,className+"Service.java");
//					File serviceImplFile = new File(serviceImplDir,className+"ServiceImpl.java");
//					File mapperFile = new File(mapperDir,className+"Mapper.java");
//					File controllerFile = new File(controllerDir,className+"Controller.java");
//					File xmlFile = new File(xmlDir,className+"Mapper.xml");
////					File modelFile = new File(modelDir,className+"Model.java");
////					File voFile = new File(voDir,className+".java");
//
//					VelocityUtil.generateFile("/tpl/ssm/service.tpl", context, serviceFile);
//					VelocityUtil.generateFile("/tpl/ssm/serviceImpl.tpl", context, serviceImplFile);
//					VelocityUtil.generateFile("/tpl/ssm/mapper.tpl", context, mapperFile);
//					VelocityUtil.generateFile("/tpl/ssm/controller.tpl", context, controllerFile);
//					VelocityUtil.generateFile("/tpl/ssm/xml.tpl", context, xmlFile);
////					VelocityUtil.generateFile("/tpl/ssm/model.tpl", context, modelFile);
////					VelocityUtil.generateFile("/tpl/ssm/vo.tpl", context, voFile);
//
//					logFilePath(serviceFile,serviceImplFile,mapperFile,controllerFile,xmlFile);
//				} catch (IOException e) {
//					e.printStackTrace();
//				}
//			}
//		}
//		return currentPath;
//	}
//
//	/**
//	 *
//	 * 作者:sanri <br/>
//	 * 时间:2017-4-26下午4:28:56<br/>
//	 * 功能:构建 javabean <br/>
//	 * 入参: <br/>
//	 */
//	private void buildJavaBean(Table table, String voPackage, File voDir) {
//		String tableName = table.getTableName();
//		String comments = table.getComments();
//		if(StringUtils.isBlank(comments)){
//			comments = "generate from "+tableName;
//		}
//		List<Column> columns = table.getColumns();
//		Map<String,Column> columnMap = new HashMap<String, Column>();
//		Map<String,String> property = new HashMap<String, String>();
//		Map<String,String> propertysComments = new HashMap<String, String>();
//		if(!Validate.isEmpty(columns)){
//			for (Column column : columns) {
//				String columnName = column.getColumnName();
//				columnMap.put(columnName, column);
//				String propertyType = renamePolicy.mapperPropertyType(column.getDataType());
//				property.put(columnName, propertyType);
//				propertysComments.put(columnName, column.getComments());
//			}
//		}
//
//		String className = renamePolicy.mapperClassName(tableName);
//		JavaBean javaBean = new JavaBean();
//		javaBean.setClassName(className);
//		javaBean.setTable(table);
//		javaBean.setClassComment(comments);
//		javaBean.setColumns(columnMap);
//		if(excludeColumnsMap != null){
//			javaBean.setExcludeColumns(excludeColumnsMap.get(tableName));
//		}
//		javaBean.setExtendsName(generateConfig.getVoExtends());
//		javaBean.setPackageName(voPackage);
//		javaBean.setPropertys(property);
//		javaBean.setPropertysComments(propertysComments);
//		List<String> javaClass = javaBean.build();
//		File writerBean = javaBean.writerBean(javaClass, voDir);
//		logger.info("javaBean 生成:"+writerBean);
//	}
//
//	private static void logFilePath(File... files){
//		for (File file : files) {
//			logger.info("文件生成:"+file);
//		}
//	}
//
//	/*
//	 * 功能:目录循环创建,如果不存在,则创建<br/>
//	 * 注:必须父级目录存在</br/>
//	 * 创建时间:2016-9-30下午5:15:58<br/>
//	 * 作者：sanri<br/>
//	 */
//	private void dirCreate(File...files) {
//		for (int i = 0; i < files.length; i++) {
//			File currentFile = files[i];
//			if(currentFile != null && !currentFile.exists()){
//				logger.info("目录创建"+currentFile);
//				currentFile.mkdir();
//			}
//		}
//	}
//
//	/**
//	 *
//	 * 作者:sanri <br/>
//	 * 时间:2017-4-24下午2:04:52<br/>
//	 * 功能: 将生成的文件压缩成 zip 文件并读成流<br/>
//	 * 入参: <br/>
//	 * @throws
//	 */
////	public InputStream getInputStream() {
////		this.zipFile = new File(basePath,buildPath.getTitle()+".zip");
////		ZipUtil.zip(buildPath, this.zipFile);
////		try {
////			return new FileInputStream(this.zipFile);
////		} catch (FileNotFoundException e) {
////			e.printStackTrace();
////		}
////		return null;
////	}
//
//	public GenerateConfig getGenerateConfig() {
//		return generateConfig;
//	}
//	public void setGenerateConfig(GenerateConfig generateConfig) {
//		this.generateConfig = generateConfig;
//	}
//	public String getConnName() {
//		return connName;
//	}
//	public void setConnName(String connName) {
//		this.connName = connName;
//	}
//	public File getBasePath() {
//		return basePath;
//	}
//	public void setBasePath(File basePath) {
//		this.basePath = basePath;
//	}
//	public List<String> getTables() {
//		return tables;
//	}
//	public void setTables(List<String> tables) {
//		this.tables = tables;
//	}
//
//	public RenamePolicy getRenamePolicy() {
//		return renamePolicy;
//	}
//
//	public void setRenamePolicy(RenamePolicy renamePolicy) {
//		this.renamePolicy = renamePolicy;
//	}
//
//	public File getBuildPath() {
//		return buildPath;
//	}
//
//	public File getZipFile() {
//		return zipFile;
//	}
//
//	public void setZipFile(File zipFile) {
//		this.zipFile = zipFile;
//	}
//
//	public void setExcludeColumnsMap(Map<String, List<String>> excludeColumnsMap) {
//		this.excludeColumnsMap = excludeColumnsMap;
//	}
//
//
//}
