package com.sanri.app.servlet;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.sanri.app.BaseServlet;
import com.sanri.app.jdbc.*;
import com.sanri.app.jdbc.codegenerate.SqlExecuteResult;
import com.sanri.app.jdbc.datatrans.DataTransfer;
import com.sanri.app.jdbc.datatrans.ExportProcess;
import com.sanri.app.postman.FileInfo;
import com.sanri.app.postman.FileListResult;
import com.sanri.app.postman.JdbcConnDetail;
import com.sanri.app.task.BigDataWriteThread;
import com.sanri.frame.IgnoreSpendTime;
import com.sanri.frame.RequestMapping;
import com.sanri.initexec.InitJdbcConnections;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserManager;
import net.sf.jsqlparser.statement.select.Limit;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.dbutils.DbUtils;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.ResultSetHandler;
import org.apache.commons.dbutils.handlers.ScalarHandler;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import sanri.utils.NumberUtil;
import sanri.utils.SignUtil;
import sanri.utils.ZipUtil;
import sanri.utils.excel.ExcelUtil;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.sql.DataSource;
import java.io.*;
import java.net.URLDecoder;
import java.sql.*;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static sanri.utils.excel.ExcelUtil.BASE_HEIGHT_1_PX;

/**
 *
 * 作者:sanri <br/>
 * 时间:2017-4-17下午2:04:05<br/>
 * 功能:sql 客户端功能 <br/>
 */
@RequestMapping("/sqlclient")
public class SqlClientServlet extends BaseServlet{
	private static File sqlBaseDir = null;
	private static File exportSqlDir = null;
	private static File exportTmpDir = null;

	ExecutorService privateExecutorService = Executors.newFixedThreadPool(3);

	static{
		sqlBaseDir = mkConfigPath("sql");

		//导出 sql 临时目录
		exportSqlDir = mkConfigPath("exportSql");

		exportTmpDir = mkTmpPath("exportTmp");
	}
	/**
	 *
	 * 作者:sanri <br/>
	 * 时间:2017-4-21上午11:23:25<br/>
	 * 功能:创建连接 <br/>
	 * 入参: <br/>
	 */
	public String createConnection(JdbcConnDetail connectionInfo) throws SQLException {
		String name = connectionInfo.getName();
		if(existConnectionName(name)){
			return "";
		}
		InitJdbcConnections.saveConnection(connectionInfo);
		return name;
	}

	/**
	 *
	 * 作者:sanri <br/>
	 * 时间:2017-4-21上午11:23:14<br/>
	 * 功能:检测是否存在连接名 <br/>
	 * 入参: <br/>
	 */
	public boolean existConnectionName(String name){
		return InitJdbcConnections.CONNECTIONS.keySet().contains(name);
	}

	/**
	 *
	 * 作者:sanri <br/>
	 * 时间:2017-4-21上午11:23:04<br/>
	 * 功能:查询所有的连接 <br/>
	 * 入参: <br/>
	 */
	public Set<String> connections(){
		return InitJdbcConnections.CONNECTIONS.keySet();
	}

    /**
     * 获取连接串
     * @param connName
     * @return
     */
	public String connectString(String connName){
		ExConnection exConnection = InitJdbcConnections.CONNECTIONS.get(connName);
		return exConnection.toString();
	}

	/**
	 *
	 * 作者:sanri <br/>
	 * 时间:2017-4-21上午11:23:33<br/>
	 * 功能: 查询连接所有的表<br/>
	 * 入参: 只需要传连接名 <br/>
	 */
	public List<Table> tables(String connName,String schemaName) throws SQLException {
		ExConnection exConnection = InitJdbcConnections.CONNECTIONS.get(connName);
		List<Table> tables = exConnection.tables(schemaName, false);
		if(CollectionUtils.isNotEmpty(tables)){
			for (Table table : tables) {
				exConnection.columns(schemaName,table.getTableName(),false);
			}
		}
		return tables;
	}

	/**
	 * 刷新单张表信息; 主要用于列有修改的情况
	 * @param connName
	 * @param schemaName
	 * @param tableName
	 * @return
	 */
	public List<Column> refreshTable(String connName,String schemaName,String tableName) throws SQLException {
		ExConnection exConnection = InitJdbcConnections.CONNECTIONS.get(connName);
		List<Column> columns = exConnection.columns(schemaName, tableName, true);
		return columns;
	}

	/**
	 * 刷新数据库,会把所有的表都刷新一遍
	 * @param connName
	 * @param schemaName
	 * @return
	 * @throws SQLException
	 */
	public List<Table> refreshSchema(String connName,String schemaName) throws SQLException {
		ExConnection exConnection = InitJdbcConnections.CONNECTIONS.get(connName);
		List<Table> tables = exConnection.tables(schemaName, true);
		if(CollectionUtils.isNotEmpty(tables)){
			for (Table table : tables) {
				exConnection.columns(schemaName,table.getTableName(),true);
			}
		}
		return tables;
	}

	/**
	 * 刷新连接,重新获取库信息
	 * @param connName
	 * @return
	 * @throws SQLException
	 */
	public List<Schema> refreshConnection(String connName) throws SQLException {
		ExConnection exConnection = InitJdbcConnections.CONNECTIONS.get(connName);
		return exConnection.schemas(true);
	}

	/**
	 *
	 * 作者:sanri <br/>
	 * 时间:2017-4-21下午12:22:08<br/>
	 * 功能: 查询数据库列表 <br/>
	 * 入参: 连接名<br/>
	 * @return
	 */
	public Collection<Schema> schemas(String connName) throws SQLException {
		ExConnection exConnection = InitJdbcConnections.CONNECTIONS.get(connName);
		return exConnection.schemas(false);
	}

	/**
	 *
	 * 作者:sanri <br/>
	 * 时间:2017-5-27上午10:44:12<br/>
	 * 功能:得到连接信息 <br/>
	 * @param name
	 * @return
	 */
	public JdbcConnDetail connectionInfo(String name){
		ExConnection exConnection = InitJdbcConnections.CONNECTIONS.get(name);

		JdbcConnDetail connDetail = exConnection.getConnDetail();
		connDetail.setName(name);
		return connDetail;
	}

	/**
	 *
	 * 作者:sanri <br/>
	 * 时间:2017-4-21上午11:24:03<br/>
	 * 功能:执行 sql 语句 <br/>
	 * 入参: <br/>
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public Map<String,Object> executeSql(String connName,String database,String [] executorSqlArray) throws SQLException {
		Map<String,Object> result = new HashMap<String, Object>();
		Connection connection  = null;PreparedStatement ps = null;ResultSet rs = null;
		ExConnection exConnection = InitJdbcConnections.CONNECTIONS.get(connName);
		try {
			if(executorSqlArray != null && executorSqlArray.length > 0){
				QueryRunner mainQueryRunner  = new QueryRunner();
				DataSource dataSource = exConnection.getDataSource(database);
				connection =dataSource .getConnection();

				//循环执行 sql
				for (String sql : executorSqlArray) {
					if(StringUtils.isBlank(sql) || StringUtils.isBlank(sql.trim())){
						continue;
					}
						sql = sql.trim().toUpperCase();
						if(sql.startsWith("SELECT")){
							//查询语句
							ps = connection.prepareStatement(sql);
							rs = ps.executeQuery();
							//获取元数据,得到头信息
							ResultSetMetaData metaData = rs.getMetaData();
							int columnCount = metaData.getColumnCount();
							JSONArray head = new JSONArray();
							for (int i = 1; i <= columnCount; i++) {
								head.add(metaData.getColumnName(i));
							}
							JSONObject jsonObject = new JSONObject();
							jsonObject.put("head", head);
							//获取数据信息
							List<Map> data = new ArrayList<Map>();
							while(rs.next()){
								Map dataObj = new HashMap();
								for (int i = 1; i <= columnCount; i++) {
									dataObj.put(metaData.getColumnName(i), rs.getObject(i));
								}
								data.add(dataObj);
							}
							jsonObject.put("body", data);
							result.put(sql, jsonObject);
						}else{
							//修改语句不支持
							int update = mainQueryRunner.update(connection, sql);
							result.put(sql, update);
						}

				}
			}
		} catch (SQLException e){
			logger.error("当前执行的 sql 列表为:\n"+StringUtils.join(executorSqlArray,'\n'));
			throw e;
		}finally{
			DbUtils.closeQuietly(connection, ps, rs);
		}
		return result;
	}

	/**
	 *
	 * 作者:sanri <br/>
	 * 时间:2017-7-13下午3:30:45<br/>
	 * 功能:往数据库中添加数据 <br/>
	 * @param connName 连接名称
	 * @param database 数据库名称
	 * @param tableName 表名
	 * @param dataMap 数据集,使用列名==>列值的形式添加数据(列需要包含所有列,没数据留空),对于 id 是自动生成的,不要加 id
	 * @return
	 */
	public String writeData(String connName,String database,String tableName,Map<String,String> dataMap){
		ExConnection exConnection = InitJdbcConnections.CONNECTIONS.get(connName);

		StringBuffer sql = new StringBuffer("insert into ");
		sql.append(tableName);
		String columns = StringUtils.join(dataMap.keySet(),',');
		sql.append("(").append(columns).append(") values (");
		String values = StringUtils.join(dataMap.values(),"','");
		sql.append("'"+values+"'");
		sql.append(")");
		logger.debug("添加数据 sql:"+sql.toString());
		try {
			int update = exConnection.getQueryRunner(database).update(sql.toString());
			return "成功添加数据:"+update+" 条";
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return "添加数据失败";
	}

	/**
	 *
	 * 作者:sanri <br/>
	 * 时间:2017-7-14下午2:24:53<br/>
	 * 功能:随机生成大量数据 <br/>
	 * @param connName 连接名称
	 * @param database 数据库名称
	 * @param tableName 表名称
	 * @param count 生成数量
	 * @param dataMap 固定字段和值
	 * @return
	 */
	public void writeMultiData(String connName,String database,String tableName,String count,Map<String,String> dataMap) throws SQLException {

		int countInt = 0;
		if(StringUtils.isNotBlank(count)){
			countInt = Integer.parseInt(count);
		}
		ExConnection exConnection = InitJdbcConnections.CONNECTIONS.get(connName);
		Table table = exConnection.getTable(database,tableName);
		BigDataWriteThread bigDataWriteThread = new BigDataWriteThread(table, countInt);
		executorService.submit(bigDataWriteThread);
	}


	/********************sql 区*******************************/

	/**
	 *
	 * 作者:sanri <br/>
	 * 时间:2017-4-21上午11:43:19<br/>
	 * 功能:保存今天写过的 sql <br/>
	 * 入参: sql 语句 <br/>
	 * @throws IOException
	 */
	public int saveSql(String fileName,String sqls) throws IOException{
		File filePath = null;
		if(StringUtils.isBlank(fileName)){
			String nowDay = DateFormatUtils.format(System.currentTimeMillis(), "yyyy-MM-dd");
			filePath = new File(sqlBaseDir,nowDay+".sr");
		}else{
			filePath = new File(sqlBaseDir,fileName);
			if(!filePath.exists()){
				throw new IllegalArgumentException("文件路径不存在:"+filePath);
			}
		}
		FileUtils.writeStringToFile(filePath, sqls,"utf-8");
		return 0;
	}

	/**
	 *
	 * 作者:sanri <br/>
	 * 时间:2017-4-21上午11:44:47<br/>
	 * 功能: 读取保存过的 sql 列表<br/>
	 * 入参: <br/>
	 * @param currentPage 当前页 从 0 开始
	 * @param pageSize 每页大小
	 * @return
	 */
	public FileListResult sqlList(int currentPage, int pageSize){
		FileListResult fileListResult = new FileListResult();
		File[] listFiles = sqlBaseDir.listFiles(new FileFilter() {
			@Override
			public boolean accept(File pathname) {
				String name = pathname.getName();
				String extension = FilenameUtils.getExtension(name);
				return "sr".equals(extension);
			}
		});
		fileListResult.setTotal(listFiles.length);
		//按照日期排序文件
		Arrays.sort(listFiles,new Comparator<File>() {
			@Override
			public int compare(File file1, File file2) {
				long diff = file1.lastModified() - file2.lastModified();
				if(diff > 0){
					return 1;
				}
				return diff == 0 ? 0:-1;
			}
		});
		int fileCount = listFiles.length;
		int pageCount = (fileCount - 1)/pageSize + 1;
		if(currentPage < 0 || currentPage > pageCount){
			throw new IllegalArgumentException("页数超出范围");
		}
		//计算开始结束位置
		int startOffset = currentPage * pageSize;
		int endOffset = (currentPage + 1) * pageSize;
		if(endOffset > fileCount){endOffset = fileCount;}

		//获取到需要显示的文件列表
		List<FileInfo> showFileList = new ArrayList<FileInfo>();
		for(int i=startOffset;i<endOffset;i++){
			File currentFile = listFiles[i];
			FileInfo fileInfo = new FileInfo();
			fileInfo.setName(currentFile.getName());
			fileInfo.setLastModified(DateFormatUtils.format(currentFile.lastModified(), "yyyy-MM-dd HH:mm:ss"));
			fileInfo.setPath(currentFile.getParent());
			showFileList.add(fileInfo);
		}

		fileListResult.setFiles(showFileList);
		return fileListResult;
	}

	/**
	 *
	 * 作者:sanri <br/>
	 * 时间:2017-4-21下午2:27:08<br/>
	 * 功能:读取文件中的 sql 内容 <br/>
	 * 入参: <br/>
	 */
	public String readSqls(String fileName) throws IOException{
		File file = new File(sqlBaseDir+"/"+fileName);
		if(!file.exists()){
			return "文件不存在";
		}
		return FileUtils.readFileToString(file);
	}

	/*************************数据转移区*************************************/

	/**
	 *
	 * 作者:sanri <br/>
	 * 时间:2017-7-22下午3:22:47<br/>
	 * 功能:数据转移,将当前选中的表的数据,转移到指定连接=>库=>表,
	 * 需先写好处理类放入 com.sanri.app.jdbc.datatransfer.impl 路径中,然后才能使用 <br/>
	 * @param conn 数据源连接
	 * @param db 数据源库
	 * @param table 数据源表
	 * @param handlerClazz 处理类,需提供全路径
	 */
	public void transfer(String conn,String db,String table,String handlerClazz){
		if(StringUtils.isBlank(handlerClazz)){
			throw new IllegalArgumentException("请提供处理类");
		}
		//对于转移请求,需要排队,等上个请求处理完后再来下个请求
		synchronized (SqlClientServlet.class) {
			try {
				Class<?> handlerClazzImpl = Class.forName(handlerClazz);
				DataTransfer dataTransfer = (DataTransfer) handlerClazzImpl.newInstance();

				ExConnection exConnection = InitJdbcConnections.CONNECTIONS.get(conn);
				Connection connection = exConnection.getDataSource(db).getConnection();

				//先查询需要转移的数据有多少
				QueryRunner mainQueryRunner = new QueryRunner();
				Long recordCount = mainQueryRunner.query(connection, "select count(1) from "+table, new ResultSetHandler<Long>(){
					@Override
					public Long handle(ResultSet resultset) throws SQLException {
						while(resultset.next()){
							return resultset.getLong(1);
						}
						return null;
					}

				});

				//开始数据转移
				long pertransfer = 1000;										//每次转移 1000 条数据
				int transferCount = (int) ((recordCount - 1)/pertransfer + 1);	//需要转移的次数
				int currentTransfer = 0;
				logger.info("总共有 "+recordCount+" 条记录,计划每次转移 "+pertransfer+" 条数据,需转移 "+transferCount+" 次");
				Table tableEntity = exConnection.getTable(db,table);
				while(transferCount -- > 0){
					long startOffset = currentTransfer * pertransfer;
					long endOffset = (currentTransfer + 1) * pertransfer;
					if(endOffset > recordCount){endOffset = recordCount;}
					logger.info("转移第 "+(currentTransfer + 1) + " 批数据始末位置分别为 "+startOffset+":"+endOffset);

					List<Map<String, String>> queryData = mainQueryRunner.query(connection, "select * from "+table+" limit "+startOffset+","+endOffset, new ResultSetHandler<List<Map<String,String>>>(){
						@Override
						public List<Map<String, String>> handle(ResultSet resultset) throws SQLException {
							ResultSetMetaData metaData = resultset.getMetaData();
							int columnCount = metaData.getColumnCount();
							List<Map<String, String>> listMap = new ArrayList<Map<String,String>>();
							while(resultset.next()){
								Map<String,String> map = new HashMap<String, String>();
								for(int i=1;i<=columnCount;i++){
									String columnName = metaData.getColumnName(i);
									String columnValue = resultset.getString(i);
									map.put(columnName, columnValue);
								}
								listMap.add(map);
							}
							return listMap;
						}
					});
					dataTransfer.handler(queryData,tableEntity);
					currentTransfer ++;
				}
				logger.info("数据转移成功");
				DbUtils.close(connection);
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
				throw new IllegalArgumentException("处理类未找到");
			} catch (InstantiationException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			} catch (SQLException e) {
				e.printStackTrace();
			}

		}
	}

	// jsqlparser 解析
	CCJSqlParserManager parserManager = new CCJSqlParserManager();

	/**
	 * 数据导出预览
	 * @return
	 */
	public SqlExecuteResult exportPreview(String conn, String db, String sql, HttpSession session) throws SQLException, JSQLParserException {
		sql = new String (Base64.decodeBase64(sql.getBytes(charset)),charset);
		ExConnection exConnection = InitJdbcConnections.CONNECTIONS.get(conn);
		Connection connection = exConnection.getDataSource(db).getConnection();

		try{
			Select select = (Select) parserManager.parse(new StringReader(sql));
			PlainSelect plainSelect = (PlainSelect) select.getSelectBody();
			Limit limit = new Limit();
			limit.setOffset(0);
			limit.setRowCount(15);
			plainSelect.setLimit(limit);

			ExportProcess exportProcess = new ExportProcess(1,"总进度");
			SqlExecuteResult sqlExecuteResult = loadResult(select.toString(),connection,exportProcess, 1);

			return sqlExecuteResult;
		} finally {
			DbUtils.closeQuietly(connection);
		}
	}

	/**
	 * 数据总量查询
	 * @param conn
	 * @param db
	 * @param sql
	 * @return
	 */
	public long exportDataCount(String conn, String db, String sql) throws SQLException {
		sql = new String (Base64.decodeBase64(sql.getBytes(charset)),charset);
		ExConnection exConnection = InitJdbcConnections.CONNECTIONS.get(conn);
		Connection connection = exConnection.getDataSource(db).getConnection();

		try{
			QueryRunner mainQueryRunner = new QueryRunner();
			Object result = mainQueryRunner.query(connection,"select count(0) from (" + sql + ") b", new ScalarHandler(1));
			return NumberUtil.toLong(ObjectUtils.toString(result),0);
		}finally {
			DbUtils.closeQuietly(connection);
		}
	}

	/**
	 * 生成导出入场券
	 * @return
	 */
	public String generateExportTicket(){
		String ticket = SignUtil.uniqueString(15);
		return ticket;
	}

	/**
	 * 导出进度查询
	 * @return
	 */
	@IgnoreSpendTime
	public List<ExportProcess> exportProcessQuery(HttpSession session,String ticket){
		Object processes = session.getAttribute(ticket);
		if(processes != null) {
			// 单进程
			if(processes instanceof ExportProcess){
				ExportProcess exportProcess = (ExportProcess) processes;
				return Arrays.asList(exportProcess);
			}

			//多进程
			return (List<ExportProcess>) processes;
		}
		return new ArrayList<ExportProcess>();
	}

	/**
	 * 导出excel 表格
	 * @param response
	 */
	static final int exportPerLimit = 100000;
	public void exportProcess(HttpServletRequest request, HttpServletResponse response, String conn, String db, String sql,String ticket) throws IOException, SQLException {
		HttpSession session = request.getSession();
		//初始化进度
		ExportProcess exportProcess = new ExportProcess(1,"总进度",0, "初始化");
		session.setAttribute(ticket,exportProcess);

		ExConnection exConnection = InitJdbcConnections.CONNECTIONS.get(conn);
		Connection connection = exConnection.getDataSource(db).getConnection();

		try{
			QueryRunner mainQueryRunner = new QueryRunner();

			SqlExecuteResult sqlExecuteResult = loadResult(sql, connection,exportProcess,0.2);
			exportProcess.process(20,"数据加载完成,准备转换 Excel");

			//转成 Excel 文档
			Workbook workbook =  new SXSSFWorkbook(1000);
			Sheet sheet = workbook.createSheet(conn+"_"+db+"_"+DateFormatUtils.ISO_DATE_FORMAT.format(System.currentTimeMillis()));
			fillExcelSheet(sqlExecuteResult, sheet,exportProcess,0.7);

			//设置 90 进度
			exportProcess.process(90," Excel 生成完毕,准备下载");

			//开始下载
			InputStream inputStream = ExcelUtil.toInputStream(workbook);
			exportProcess.process(99,"Excel 流转换完毕,准备输出 ");

			download(inputStream,MimeType.EXCEL2007,conn+"_"+db+"_"+DateFormatUtils.ISO_DATE_FORMAT.format(System.currentTimeMillis()),request,response);
			exportProcess.process(100,"下载完毕");
		}finally {
			exportProcess.process(100,"下载完毕");
			DbUtils.closeQuietly(connection);
		}

	}

	/**
	 * 填充 excel sheet 页
	 * @param session
	 * @param sqlExecuteResult
	 * @param sheet
	 */
	private void fillExcelSheet( SqlExecuteResult sqlExecuteResult, Sheet sheet,ExportProcess exportProcess,double weight) {
		Row headRow = sheet.createRow(0);
		headRow.setHeight((short)(30 * BASE_HEIGHT_1_PX));
		//创建标题列
		List<String> header = sqlExecuteResult.getHeader();
		for (int i = 0; i < header.size(); i++) {
			String name = header.get(i);
			Cell headCell = headRow.createCell(i);
			headCell.setCellValue(name);
			headCell.setCellType(Cell.CELL_TYPE_STRING);
		}
		//创建数据列
		List<List<Object>> rows = sqlExecuteResult.getRows();
		List<String> columnType = sqlExecuteResult.getColumnType();

		//获取当前进度
		double nowPercent = exportProcess.getPercent();

		for (int i = 0; i < rows.size(); i++) {
			//设置进度
			double percent = NumberUtil.percent(i, rows.size(), 2);
			//计算增量进度
			double processIncrement = exportProcess.processCalc(percent, weight);
			String round = NumberUtil.round(nowPercent + processIncrement,2);		//double 的加法会导致精度问题
			exportProcess.process(NumberUtil.toDouble(round),"正在填充 Excel 表格 ");

			List<Object> objects = rows.get(i);
			Row dataRow = sheet.createRow(i + 1);
			for (int j = 0; j < objects.size(); j++) {
				String colType = columnType.get(j);
				Cell cell = dataRow.createCell(j);
				Object value = objects.get(j);

				if(value == null){
					// 空值
					cell.setCellType(Cell.CELL_TYPE_BLANK);
					continue;
				}
				if("char".equalsIgnoreCase(colType) || "varchar".equalsIgnoreCase(colType)) {
					cell.setCellValue(ObjectUtils.toString(value));
					cell.setCellType(Cell.CELL_TYPE_STRING);
				}else if ("datetime".equalsIgnoreCase(colType)){
					cell.setCellType(Cell.CELL_TYPE_STRING);
					Timestamp timestamp = (Timestamp) value;
					long time = timestamp.getTime();
					String format = DateFormatUtils.ISO_DATE_FORMAT.format(time);
					cell.setCellValue(format);
				}else if("int".equalsIgnoreCase(colType) || "decimal".equalsIgnoreCase(colType)){
					cell.setCellType(Cell.CELL_TYPE_NUMERIC);
					cell.setCellValue(NumberUtil.toLong(ObjectUtils.toString(value)));
				}else if ("date".equalsIgnoreCase(colType)){
					cell.setCellType(Cell.CELL_TYPE_STRING);
					cell.setCellValue(ObjectUtils.toString(value));
				}else if("TINYINT".equalsIgnoreCase(colType)){
					cell.setCellType(Cell.CELL_TYPE_STRING);
					cell.setCellValue(ObjectUtils.toString(value));
				}else {
					logger.error("不支持的数据库类型,需要添加类型支持:"+colType+",value:"+value);
				}
			}
		}

		//设置列宽; 自动列宽
		for (int i = 0; i < header.size(); i++) {
			sheet.autoSizeColumn(i);
		}
	}

	/**
	 * 数据库中加载数据
	 * @param sql
	 * @param connection
	 * @param exportProcess 进度
	 * @param weight		进度权重
	 * @return
	 * @throws SQLException
	 */
	private SqlExecuteResult loadResult(String sql, Connection connection, final ExportProcess exportProcess, final double weight) throws SQLException {
		QueryRunner mainQueryRunner = new QueryRunner();

		//获取数据总行数
		final Integer dataCount = mainQueryRunner.query(connection, sql, new ScalarHandler<Integer>(1));
		double plusProcess = exportProcess.processCalc(10, weight);
		exportProcess.plusProcess(plusProcess,"数据行数统计完成,正查询数据中");

		SqlExecuteResult query = mainQueryRunner.query(connection, sql, new ResultSetHandler<SqlExecuteResult>() {
			@Override
			public SqlExecuteResult handle(ResultSet rs) throws SQLException {
				//添加 50% 进度
				double plusProcess = exportProcess.processCalc(50, weight);
				exportProcess.plusProcess(plusProcess,"数据加载完成,正在组装");

				SqlExecuteResult sqlExecuteResult = new SqlExecuteResult();

				//添加头部
				ResultSetMetaData metaData = rs.getMetaData();
				int columnCount = metaData.getColumnCount();
				for (int i = 1; i <= columnCount; i++) {
					String columnLabel = metaData.getColumnLabel(i);
					sqlExecuteResult.addHeader(columnLabel);
					sqlExecuteResult.addColumnType(metaData.getColumnTypeName(i));
				}

				//数据头部添加完成,再加 5% 进度
				double plusProcess5 = exportProcess.processCalc(5, weight);
				exportProcess.plusProcess(plusProcess5,"数据头部组装完成,准备组装数据");

				//添加数据
				double nowPercent = exportProcess.getPercent();
				int j = 0;
				while (rs.next()) {
					List<Object> row = new ArrayList<Object>();
					for (int i = 1; i <= columnCount; i++) {

						Object columnData = rs.getObject(i);
						row.add(columnData);
					}

					//设置数据处理进度
					double percent = NumberUtil.percent(j++, dataCount, 2);
					double plusProcessIncrement = exportProcess.processCalc(percent * 0.35, weight);
					exportProcess.process(nowPercent + plusProcessIncrement,"数据加载中");
					sqlExecuteResult.addRow(row);
				}

				return sqlExecuteResult;
			}
		});

		return query;
	}

	/**
	 * 保存业务 sql
	 * @return
	 */
	public int saveExportSql(String subject,String sql,String db){
		try {
			sql = URLDecoder.decode(sql, charset.toString());
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		File dbDir = new File(exportSqlDir, db);
		if(!dbDir.exists()){
			dbDir.mkdir();
		}

		File file = new File(dbDir, subject + ".sql");
		try {
			FileUtils.writeStringToFile(file,sql,charset);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return 0;
	}

	/**
	 * 获取业务列表
	 * @return
	 */
	public List<String > exportSqls(String db){
		File[] files = new File(exportSqlDir,db).listFiles();
		List<String> fileNames = new ArrayList<String>();
		if(ArrayUtils.isNotEmpty(files)) {
			for (File file : files) {
				fileNames.add(FilenameUtils.getBaseName(file.getName()));
			}
		}
		return fileNames;
	}

	/**
	 * 加载 sql 数据
	 * @param subject
	 * @return
	 */
	public String loadExportSql(String subject,String db){
		File dbDir = new File(exportSqlDir, db);
		File file = new File(dbDir, subject + ".sql");
		if(file.exists()){
			try {
				return FileUtils.readFileToString(file,charset);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		return "";
	}

	/**
	 * 显示建表信息
	 * @return
	 */
	public String showCreateTable(String conn, String db,String table) throws SQLException {
		ExConnection exConnection = InitJdbcConnections.CONNECTIONS.get(conn);
		String result =  exConnection.ddL(db,table);
		return result;
	}

	/**
	 *  低内存多线程导出
	 * @param request
	 * @param response
	 * @param conn
	 * @param db
	 * @param sql
	 * @param dataTotal
	 * @throws IOException
	 * @throws SQLException
	 */
	public void exportLowMemoryMutiProcess(HttpServletRequest request, HttpServletResponse response, final String conn, final String db, final String sql,String ticket) throws IOException, SQLException, JSQLParserException {
		String exeSql = new String (Base64.decodeBase64(sql.getBytes(charset)),charset);
		//查询数据量; 如果数据量低于 10 万,采用原始方式导出; 否则多线程导出
		final long dataCount = exportDataCount(conn, db, sql);
		if(dataCount < exportPerLimit){
			exportProcess(request,response,conn,db,exeSql,ticket);
			return ;
		}

		//计算线程数
		final int threadCount = (int) ((dataCount - 1) / exportPerLimit + 1);

		//添加总进度
		final ExportProcess totalProcess = new ExportProcess(0,"总进度");
		final CountDownLatch countDownLatch = new CountDownLatch(threadCount);

		logger.info("启用多线程进度导出:"+ticket);
		HttpSession session = request.getSession();
		final List<ExportProcess> exportProcesses = new ArrayList<ExportProcess>();
		exportProcesses.add(totalProcess);
		session.setAttribute(ticket,exportProcesses);

		//创建临时目录
		final File tmpDir = new File(exportTmpDir, SignUtil.uniqueTimestamp());
		if(!tmpDir.exists()){
			tmpDir.mkdir();
		}
		logger.info("临时文件将输出到此目录:"+tmpDir);

		Select select = (Select) parserManager.parse(new StringReader(exeSql));
		PlainSelect plainSelect = (PlainSelect) select.getSelectBody();

		//多线程导出; 分每批 10 万 生成多个 Excel,每个 Excel 生成后;  释放到临时文件中,释放内存,最后将整个目录使用 zip 打包
		for (int i = 0; i < threadCount; i++) {
			int currentBatch = i;
			final long begin = currentBatch * exportPerLimit;
			long end = (currentBatch + 1) * exportPerLimit;
			if(end > dataCount){
				end = dataCount;
			}
			final long  finalEnd = end;
//			final String currentSql = exeSql + " limit "+begin+","+end;
			Limit limit = new Limit();
			limit.setOffset(begin);
			limit.setRowCount(end);
			plainSelect.setLimit(limit);
			final String currentSql = select.toString();

			final ExportProcess exportProcess = new ExportProcess((currentBatch + 1),"线程:"+i,0,"初始化");
			exportProcesses.add(exportProcess);

			privateExecutorService.submit(new Thread() {
				@Override
				public void run() {
					ExConnection exConnection = InitJdbcConnections.CONNECTIONS.get(conn);

					FileOutputStream fileOutputStream = null;
					try {
						Connection connection = exConnection.getDataSource(db).getConnection();
						SqlExecuteResult sqlExecuteResult = loadResult(currentSql, connection, exportProcess, 0.2);
						File excelPartFile = new File(tmpDir, conn + "_" + db + "_" + begin + "~" + finalEnd + ".xlsx");
						logger.info("Excel 部分文件 :"+excelPartFile.getName());

						Workbook workbook = new SXSSFWorkbook(1000);
						Sheet sheet = workbook.createSheet(conn + "_" + db + "_" + begin + "~" + finalEnd);
						fileOutputStream = new FileOutputStream(excelPartFile);
						fillExcelSheet(sqlExecuteResult,sheet,exportProcess,0.7);
						workbook.write(fileOutputStream);
					} catch (SQLException e) {
						e.printStackTrace();
					} catch (FileNotFoundException e) {
						e.printStackTrace();
					} catch (IOException e) {
						e.printStackTrace();
					} finally {
						IOUtils.closeQuietly(fileOutputStream);
						countDownLatch.countDown();
						double percent = NumberUtil.percent((threadCount - countDownLatch.getCount()), threadCount, 2);
						double totalPercent = totalProcess.processCalc(percent, 0.9);
						totalProcess.process(totalPercent,"数据处理中");

						//设置当前导出进度 95%
						exportProcess.process(95,"导出完毕,准备压缩");
					}
				}
			});

		}
		try {
			//等待处理完毕
			countDownLatch.await();

			//下载 zip 文件,完成导出
			File excelZip = ZipUtil.zip(tmpDir);
			FileInputStream fileInputStream = new FileInputStream(excelZip);

			//设置所有进度 99%
			for (int i = 0; i < exportProcesses.size(); i++) {
				ExportProcess exportProcess = exportProcesses.get(i);
				exportProcess.process(99,"打包压缩完毕,准备输出");
			}

			download(fileInputStream,MimeType.AUTO,excelZip.getName(),request,response);

			//设置所有进度 100%
			for (int i = 0; i < exportProcesses.size(); i++) {
				ExportProcess exportProcess = exportProcesses.get(i);
				exportProcess.process(100,"打包压缩完毕,准备输出");
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	/**
	 * 数据库结构导出
	 * @param request
	 * @param response
	 * @param conn
	 * @param db
	 * @return
	 */
	public String exportStruct(HttpServletRequest request, HttpServletResponse response, String conn, String db) throws SQLException {
		ExConnection exConnection = InitJdbcConnections.CONNECTIONS.get(conn);
		DataSource dataSource = exConnection.getDataSource(db);
		Connection connection = dataSource.getConnection();

		List<String> tableSchemas = new ArrayList<String>();

		try {
			List<Table> tables = tables(conn, db);
			QueryRunner mainQueryRunner = new QueryRunner();
			for (Table table : tables) {
				String tableName = table.getTableName();

				String tableSchema = mainQueryRunner.query(connection, "show create table " + tableName, new ScalarHandler<String>(2));
				tableSchemas.add(tableSchema);
			}
		}finally {
			DbUtils.closeQuietly(connection);
		}

		File structSqlFile = new File(exportTmpDir, conn + "_" + db + "_" + SignUtil.uniqueTimestamp()+".sql");
		String join = StringUtils.join(tableSchemas, ";\n\n");
		try {
			FileUtils.writeStringToFile(structSqlFile,join);
		} catch (IOException e) {
			e.printStackTrace();
		}

		return structSqlFile.getName();
	}

	/**
	 * 信息搜索,这在新看一个库的表的时候有用
	 * @param connName
	 * @param schemaName
	 * @param keyword 如果带冒号,说明为精确查找 两个精确模式 table column
	 */
	public List<Table> searchTables(String connName,String schemaName,String keyword) throws SQLException {
		String searchSchema = "";
		if(keyword.contains(":")){
			searchSchema = keyword.split(":")[0];
			keyword = keyword.split(":")[1];
		}

		//如果为 oracle ,搜索关键字转大写
		ExConnection exConnection = InitJdbcConnections.CONNECTIONS.get(connName);
		if(exConnection instanceof OracleExConnection){
			keyword = keyword.toUpperCase();
		}

		List<Table> tables = tables(connName, schemaName);
		if(StringUtils.isBlank(keyword)){
			//空搜索,列出所有表
			return tables;
		}

		List<Table> findTables = new ArrayList<Table>();
		if(CollectionUtils.isNotEmpty(tables)){
			for (Table table : tables) {
				String tableName = table.getTableName();
				String tableComments = table.getComments();
				if(StringUtils.isBlank(searchSchema) || "table".equalsIgnoreCase(searchSchema)) {
					if (tableName.contains(keyword) || (StringUtils.isNotBlank(tableComments) && tableComments.contains(keyword))) {
						findTables.add(table);
						continue;
					}
				}

				//再看是否有列是匹配的
				List<Column> columns = table.getColumns();
				if(CollectionUtils.isNotEmpty(columns)){
					for (Column column : columns) {
						String columnName = column.getColumnName();
						String columnComments = column.getComments();

						if(StringUtils.isBlank(searchSchema) || "column".equalsIgnoreCase(searchSchema)) {
							if (columnName.contains(keyword) || (StringUtils.isNotBlank(columnComments) && columnComments.contains(keyword))) {
								findTables.add(table);
							}
						}
					}
				}
			}
		}

		return findTables;
	}

	/**
	 * 文件下载,抄自 codeGenerate
	 * @param typeName
	 * @param fileName
	 * @param request
	 * @param response
	 * @throws IOException
	 */
	public void downFile(String typeName,String fileName,HttpServletRequest request,HttpServletResponse response) throws IOException{
		File filePath = null;
		if("export".equals(typeName)){
			filePath = exportTmpDir;
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
}
