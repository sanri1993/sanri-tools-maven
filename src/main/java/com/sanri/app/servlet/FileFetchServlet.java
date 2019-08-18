
package com.sanri.app.servlet;

import com.sanri.app.BaseServlet;
import com.sanri.app.filefetch.FilePathFilter;
import com.sanri.app.filefetch.FindFilesResult;
import com.sanri.app.filefetch.MirrorFilePathFilter;
import com.sanri.frame.RequestMapping;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import sanri.utils.ZipUtil;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.ResourceBundle;
import java.util.Set;

/**
 * 
 * 创建时间:2017-5-14下午5:41:37<br/>
 * 创建者:sanri<br/>
 * 功能:对文件进行抓取<br/>
 */
@RequestMapping("/filefetch")
public class FileFetchServlet extends BaseServlet{
	private static final  Map<String,Map<String,String>> fileFetchConfigMap = new HashMap<String, Map<String,String>>();
	private static File generatePath = null;
	private FilePathFilter mirrorPathFilter = new MirrorFilePathFilter();
	
	static{
		//读取默认配置
		ResourceBundle resource = ResourceBundle.getBundle("com/sanri/config/filefetch");
		Set<String> keySet = resource.keySet();
		Iterator<String> iterator = keySet.iterator();
		while(iterator.hasNext()){
			String preKey = iterator.next();
			String[] preKeySplit = preKey.split("\\.");
			
			//得到本次的键和属性 
			String key = preKeySplit[0];
			String property = StringUtils.join(preKeySplit, "", 1, preKeySplit.length);
			String value = resource.getString(preKey);
			
			//加入属性映射 
			Map<String, String> propertyValue = fileFetchConfigMap.get(key);
			if(propertyValue == null){
				propertyValue = new HashMap<String, String>();
				fileFetchConfigMap.put(key, propertyValue);
			}
			propertyValue.put(property, value);
		}
		
		generatePath = new File(dataTempPath,"filefetch");
		if(!generatePath.exists()){
			generatePath.mkdirs();
		}
	}
	
	/**
	 * 
	 * 作者:sanri <br/>
	 * 时间:2017-8-11上午10:20:08<br/>
	 * 功能:版本合并,取以版本号开头的所有文件,从后到前排序,然后合并 <br/>
	 * @param version
	 * @return
	 * @throws IOException 
	 */
	public String mergeVersion(final String version) throws IOException{
		if(StringUtils.isBlank(version)){
			throw new IllegalArgumentException("版本号不能为空:"+version);
		}
		//删除之前创建的文件
		File mergePath = new File(generatePath,"merge"+version);
		if(mergePath.exists()){
			FileUtils.deleteDirectory(mergePath);
		}
		File zipFile = new File(generatePath,"merge"+version+".zip");
		if(zipFile.exists()){
			FileUtils.deleteQuietly(zipFile);
		}
		//重新合并并压缩
		mergePath.mkdir();
		File[] listFiles = generatePath.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				//只提取以当前版本开头的文件,并且是以 zip 结尾的
				if(name.startsWith(version) && name.toUpperCase().endsWith(".ZIP")){
					return true;
				}
				return false;
			}
		});
		//排序文件,以时间倒序排序 
		Arrays.sort(listFiles, new Comparator<File>() {
			@Override
			public int compare(File o1, File o2) {
				return (int) (o1.lastModified()  - o2.lastModified());
			}
		});
		if(listFiles != null && listFiles.length > 0){
			for (File snapshotFile : listFiles) {
				//合并所有的快照文件,都是 zip 文件
				ZipUtil.unzip(snapshotFile, mergePath.getPath());
				//提取里面的文件到外层
				String dirName = FilenameUtils.getBaseName(snapshotFile.getName());
				File snapshotDir = new File(mergePath,dirName);
				File[] projects = snapshotDir.listFiles();
				//目前只支持单项目
				File projectDir = projects[0];
				//复制项目到 mergePath 
				FileUtils.copyDirectoryToDirectory(projectDir, mergePath);
			}
		}
		//压缩合并后的文件,并打包
		File zip = ZipUtil.zip(mergePath);
		FileUtils.deleteDirectory(mergePath);
		return zip.getName();
	}
	
	/**
	 * 
	 * 作者:sanri <br/>
	 * 时间:2017-5-17上午11:33:59<br/>
	 * 功能: 1.查询需要提取的文件 <br/>
	 * 		2.压缩提取的文件,并返回文件名称
	 * 入参: <br/>
	 */
	public FindFilesResult findfiles(String connName,String version,String files){
		if(StringUtils.isBlank(files)){
			throw new IllegalArgumentException("输入要抓取的文件列表");
		}
		if(StringUtils.isBlank(files.trim())){
			throw new IllegalArgumentException("输入要抓取的文件列表,文件列表只有空格");
		}
		//获取项目的配置 
		Map<String, String> config = fileFetchConfigMap.get(connName);
		String sourcepath = config.get("sourcepath");		//文件来源
		if(StringUtils.isBlank(sourcepath)){
			throw new IllegalArgumentException("源未配置,需要配置 ["+connName+"] 的文件来源路径,即 "+connName+".sourcepath=" );
		}
		File sourceBaseDir = new File(sourcepath);
		if(!sourceBaseDir.exists()){
			throw new IllegalArgumentException("源路径不存在,请重新设置: "+sourceBaseDir);
		}
		FindFilesResult findFilesResult = new FindFilesResult();
		//设置配置信息
		mirrorPathFilter.setConfig(config);
		//输出目录
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
		
		File outputDir = new File(versionPath,config.get("project"));
		if(!outputDir.exists()){
			outputDir.mkdir();
		}
		
		//开始处理文件列表
		String[]  fileArray = files.split("\n");
		List<String> errorFiles = new ArrayList<String>();
		if(fileArray != null && fileArray.length > 0){
			for (String filePath : fileArray) {
				if(StringUtils.isBlank(filePath)){
					continue;
				}
				filePath = filePath.trim();				//去掉两边空格处理
				copyFiles(filePath,outputDir,errorFiles);
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
	 * 时间:2017-5-22上午9:39:22<br/>
	 * 功能:复制指定文件列表(这里单个 java 文件可能有多个编译后的类文件)到指定目录 <br/>
	 * @param filePath java 文件路径
	 * @param outputDir 输出目录
	 * 路径: 先根据 java 文件路径找到编译后的所有类路径,然后复制到输出目录对应目录
	 * @param errorFiles 
	 */
	private void copyFiles(String filePath, File outputDir, List<String> errorFiles) {
		try{
			Map<String, File> mappingSourcePath = mirrorPathFilter.mappingPkgSourcePath(filePath);
			if(mappingSourcePath != null && mappingSourcePath.size() > 0){
				Iterator<Entry<String, File>> iterator = mappingSourcePath.entrySet().iterator();
				while(iterator.hasNext()){
					Entry<String, File> mirrorEntry = iterator.next();
					String pkgPath = mirrorEntry.getKey();
					File sourcePath = mirrorEntry.getValue();
					
					copySingleFile(sourcePath,outputDir,pkgPath,errorFiles);
				}
			}
		}catch(IllegalArgumentException e){
			e.printStackTrace();
			errorFiles.add(filePath);
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
	private void copySingleFile(File sourcePath, File outputDir, String pkgPath, List<String> errorFiles) {
		File outputDestFile = new File(outputDir,pkgPath);
		File parentFile = outputDestFile.getParentFile();
		if(!parentFile.exists()){
			parentFile.mkdirs();
		}
		FileInputStream fileInputStream = null;
		FileOutputStream fileOutputStream = null;
		
		try {
			fileInputStream = new FileInputStream(sourcePath);
			fileOutputStream = new FileOutputStream(outputDestFile);
			IOUtils.copy(fileInputStream, fileOutputStream);
		} catch (FileNotFoundException e) {
			errorFiles.add(sourcePath.getAbsolutePath());
			e.printStackTrace();
		} catch (IOException e) {
			errorFiles.add(sourcePath.getAbsolutePath());
			e.printStackTrace();
		} finally{
			IOUtils.closeQuietly(fileInputStream);
			IOUtils.closeQuietly(fileOutputStream);
		}
		
	}

	/**
	 * 
	 * 作者:sanri <br/>
	 * 时间:2017-5-17上午11:36:11<br/>
	 * 功能:下载文件 <br/>
	 * 入参: <br/>
	 * @throws IOException 
	 */
	public void downFile(HttpServletRequest request,HttpServletResponse response,String filename) throws IOException{
		File downloadFile = new File(generatePath,filename );
		FileInputStream fileInputStream = new FileInputStream(downloadFile);
		try {
			if(downloadFile.exists()){
				download(fileInputStream, MimeType.AUTO, downloadFile.getName(), request, response);
				return ;
			}
			logger.error("文件不存在 file:"+downloadFile);
		}finally{
			IOUtils.closeQuietly(fileInputStream);
		}
	}
	
	/**
	 * 
	 * 作者:sanri <br/>
	 * 时间:2017-5-17上午11:37:42<br/>
	 * 功能:列出可供下载的所有文件,只看所有的 zip 文件,还没有生成的就不管了 <br/>
	 * 入参: <br/>
	 */
	public List<String> listAllFiles(){
		List<String> files = new ArrayList<String>();
		if(generatePath == null || !generatePath.exists()){
			return files;
		}
		File[] listFiles = generatePath.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				if(StringUtils.isNotBlank(name)){
					String extension = FilenameUtils.getExtension(name);
					if("zip".equalsIgnoreCase(extension)){
						return true;
					}
				}
				return false;
			}
		});
		if(listFiles != null && listFiles.length > 0){
			for (File file : listFiles) {
				files.add(file.getName());
			}
		}
		return files;
	}
}
