package com.sanri.app.servlet;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;

import sanri.utils.ZipUtil;

import com.alibaba.fastjson.JSONObject;
import com.sanri.app.BaseServlet;
import com.sanri.app.filefetch.FetchConfigItem;
import com.sanri.app.filefetch.FindFilesResult;
import com.sanri.app.filefetch.ItemProperties;
import com.sanri.frame.RequestMapping;

/**
 * 
 * 作者:sanri <br/>
 * 时间:2018-5-8下午1:38:53<br/>
 * 功能:新文件抓取; 由于旧的不兼容多路径支持对 xml 配置文件支持不好 <br/>
 */
@RequestMapping("/fetch")
public class NewFileFetchServlet extends BaseServlet {
	private static File generatePath = null;
	public static Map<String, ItemProperties> itemPropertiesMap  = null;
	static{
		//配置信息读取(以点分隔读取)
		try {
			itemPropertiesMap = ItemProperties.loadProperties("/com/sanri/config/fetch");
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		generatePath = new File(dataTempPath,"filefetch");
		if(!generatePath.exists()){
			generatePath.mkdirs();
		}
	}
	
	/**
	 * 
	 * 作者:sanri <br/>
	 * 时间:2018-5-8下午3:54:53<br/>
	 * 功能:重新加载配置 <br/>
	 * @throws IOException 
	 */
	public void reloadConfig() throws IOException{
		itemPropertiesMap = ItemProperties.loadProperties("/com/sanri/config/fetch");
		System.out.println(JSONObject.toJSONString(itemPropertiesMap));
	}
	
	/**
	 * 
	 * 作者:sanri <br/>
	 * 时间:2018-5-8下午1:38:47<br/>
	 * 功能: <br/>
	 * @param connName
	 * @param version
	 * @param files
	 * @return
	 */
	public FindFilesResult findfiles(String connName,String version,String files){
		if(StringUtils.isBlank(files)){
			throw new IllegalArgumentException("输入要抓取的文件列表");
		}
		if(StringUtils.isBlank(files.trim())){
			throw new IllegalArgumentException("输入要抓取的文件列表,文件列表只有空格");
		}
		ItemProperties itemProperties = itemPropertiesMap.get(connName);
		if(itemProperties == null){
			throw new IllegalArgumentException("找不到配置:"+connName);
		}
		FetchConfigItem fetchConfigItem = FetchConfigItem.parser(itemProperties);
		
		//处理版本信息,时分秒格式化版本
		if(StringUtils.isBlank(version)){
			version = "unversion";
		}
		String pattern = "yyyyMMddHHmmss";
		version += "_"+DateFormatUtils.format(System.currentTimeMillis(), pattern);		//输出目录加上年月日时分秒
		//创建输出目录 
		File versionPath = new File(generatePath,version);
		if(!versionPath.exists()){
			versionPath.mkdir();
		}
		File outputDir = new File(versionPath,fetchConfigItem.getProject());
		if(!outputDir.exists()){
			outputDir.mkdir();
		}
		
		//开始处理文件列表
		FindFilesResult findFilesResult = new FindFilesResult();
		String[]  fileArray = files.split("\n");
		List<String> errorFiles = new ArrayList<String>();
		if(fileArray != null && fileArray.length > 0){
			for (String filePath : fileArray) {
				if(StringUtils.isBlank(filePath)){
					continue;
				}
				try {
					//处理为编译后正确路径 
					filePath = handlerFilePath(filePath,fetchConfigItem);
					
					//资源路径
					String sourcePath = fetchConfigItem.getSourcePath();
					File sourcePathFile = new File(sourcePath);
					
					copyFiles(filePath,sourcePathFile,outputDir,errorFiles);
				} catch (IllegalArgumentException e) {
					errorFiles.add(filePath);
					e.printStackTrace();
				} catch (FileNotFoundException e) {
					errorFiles.add(filePath);
					e.printStackTrace();
				}
			}
			
			//复制完所有文件后进行打包,并删除原来文件夹
			File zipFile = new File(generatePath,versionPath.getName()+".zip");
			ZipUtil.zip(versionPath,zipFile );
			try {
				FileUtils.deleteDirectory(versionPath);
			} catch (IOException e) {
				e.printStackTrace();
			}
			findFilesResult.setFilename(zipFile.getName());
			findFilesResult.setErrorFiles(errorFiles);
		}
		return findFilesResult;
	}

	/**
	 * 
	 * 作者:sanri <br/>
	 * 时间:2018-5-9上午10:46:45<br/>
	 * 功能:文件路径处理 ; 这里换成了真实的编译后路径;但有可能路径找不到,对于 maven 编译后的 classes 类是正常的 <br/>
	 * @param filePath
	 * @return
	 */
	private String handlerFilePath(String filePath,FetchConfigItem fiConfigItem) throws IllegalArgumentException{
		filePath = filePath.trim();				//去掉两边空格处理
		//如果是以 src 开头,则让其开始于 /src
		if(filePath.startsWith("src")){filePath = "/"+filePath ;}
		
		//如果 /src 之前有路径 ,则切掉
		int indexOf = filePath.indexOf("/src");
		if(indexOf != 0 && indexOf != -1){
			filePath = filePath.substring(indexOf);
		}
		
		//对于 classpath 的路径,替换成 WEB-INF/classes,对于 resourcePath 的路径,替换到根路径 
		String compilePath = fiConfigItem.compilePath(filePath);
		
		return compilePath;
	}

	/**
	 * 
	 * 作者:sanri <br/>
	 * 时间:2018-5-9下午2:32:50<br/>
	 * 功能:复制文件到终极目录 <br/>
	 * @param compilePath
	 * @param outputDir
	 * @param errorFiles
	 * @throws FileNotFoundException 
	 */
	private void copyFiles(String compilePath,File sourcePathFile,  File outputDir, List<String> errorFiles) throws FileNotFoundException {
		if(StringUtils.isBlank(compilePath)){
			return ;
		}
		if(!sourcePathFile.exists()){
			throw new IllegalArgumentException("源路径不存在");
		}
		String extension  =  FilenameUtils.getExtension(compilePath);
		//由于 java 编译后有内部类文件,需要特别处理
		if("java".equalsIgnoreCase(extension)){
			Map<String, File> pathMap = new HashMap<String, File>();
			handlerJavaPath(compilePath, sourcePathFile, pathMap);
			File outputClassPath = new File(outputDir,"WEB-INF/classes");
			if(!outputClassPath.exists()){
				outputClassPath.mkdirs();
			}
			copyClassFile(outputClassPath,pathMap,errorFiles);
		}else{
			File destFile = new File(outputDir,compilePath);
			File sourceFile = new File(sourcePathFile,compilePath);
			copySingleFile(sourceFile, destFile.getParentFile(), errorFiles);
		}
	}

	/**
	 * 
	 * 作者:sanri <br/>
	 * 时间:2018-5-9下午6:59:32<br/>
	 * 功能:复制类文件 <br/>
	 * @param outputClassPath
	 * @param pathFileMap
	 * @param errorFiles
	 */
	private void copyClassFile(File outputClassPath, Map<String, File> pathFileMap, List<String> errorFiles) {
		Iterator<Entry<String, File>> iterator = pathFileMap.entrySet().iterator();
		while(iterator.hasNext()){
			Entry<String, File> pathMapEntry = iterator.next();
			String key = pathMapEntry.getKey();
			File value = pathMapEntry.getValue();
			
			File currentClassFilePath = new File(outputClassPath,key).getParentFile();
			copySingleFile(value, currentClassFilePath, errorFiles);
		}
	}
	
	/**
	 * 
	 * 作者:sanri <br/>
	 * 时间:2017-5-22上午10:24:52<br/>
	 * 功能:复制单个文件 <br/>
	 * @param sourcePath 源路径
	 * @param outputDir 输出目录
	 * @param pkgPath 包路径(相对于输出目录的路径文件)
	 * @param errorFiles 
	 */
	private void copySingleFile(File sourceFile, File destDir,List<String> errorFiles) {
		FileInputStream fileInputStream = null;
		FileOutputStream fileOutputStream = null;
		
		if(!destDir.exists()){
			destDir.mkdirs();
		}
		try {
			fileInputStream = new FileInputStream(sourceFile);
			fileOutputStream = new FileOutputStream(new File(destDir,sourceFile.getName()));
			IOUtils.copy(fileInputStream, fileOutputStream);
		} catch (FileNotFoundException e) {
			errorFiles.add(sourceFile.getAbsolutePath());
			e.printStackTrace();
		} catch (IOException e) {
			errorFiles.add(sourceFile.getAbsolutePath());
			e.printStackTrace();
		} finally{
			IOUtils.closeQuietly(fileInputStream);
			IOUtils.closeQuietly(fileOutputStream);
		}
		
	}

	private void handlerJavaPath(String compilePath, File sourcePathFile, Map<String, File> pathMap) throws FileNotFoundException {
		if(!sourcePathFile.exists()){
			throw new IllegalArgumentException("源文件不存在:"+sourcePathFile);
		}
		File classpathFile = null;
		//查看 sourcePathfile 的第一个子级目录,如果是WEB-INF 则编译路径是对的,否则编译路径需要去掉 WEB-INF 
		File webInfFile = new File(sourcePathFile,"WEB-INF");
		if(!webInfFile.exists()){
			compilePath = compilePath.replace("WEB-INF/", "");
			classpathFile = new File(sourcePathFile,"classes");
		}else{
			classpathFile = new File(sourcePathFile,"WEB-INF/classes");
		}
		if(!classpathFile.exists()){
			throw new IllegalArgumentException("类路径找不到,资源路径为:"+sourcePathFile);
		}
	
		String javaCompilePath = compilePath.replace(".java", ".class");	//找到对应的 class 文件
		File javaFile = new File(sourcePathFile,javaCompilePath);
		
		//得到基本名称,内部类是以 本类基本名$类部类名,如果是匿名的,则为数字
		final String fileName = javaFile.getName();
		final String fileBaseName = FilenameUtils.getBaseName(fileName);
		
		File parentFile = javaFile.getParentFile();
		File[] listFiles = parentFile.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				//add by sanri at 2017/05/25  必须要有 $ ,以标识是内部类
				if(name.startsWith(fileBaseName) && name.indexOf("$") != -1){
					return true;
				}
				if(name.equals(fileName)){
					return true;
				}
				return false;
			}
		});
		//获取到文件 java 包路径 ==>类文件
		if(ArrayUtils.isNotEmpty(listFiles)){
			for (File file : listFiles) {
				String absolutePath = file.getPath();
				String pkgFilePath = absolutePath.replace(classpathFile.getPath(), "");
				pathMap.put(pkgFilePath, file);
			}
		}else{
			throw new FileNotFoundException("文件未找到"+javaFile);
		}
	}
}
