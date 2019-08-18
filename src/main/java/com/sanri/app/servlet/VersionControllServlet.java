package com.sanri.app.servlet;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import sanri.utils.ZipUtil;

import com.sanri.app.BaseServlet;
import com.sanri.app.filefetch.FindFilesResult;
import com.sanri.app.filefetch.ItemProperties;
import com.sanri.app.filefetch.SSHService;
import com.sanri.frame.RequestMapping;

/**
 * 
 * 作者:sanri <br/>
 * 时间:2018-6-17上午11:42:25<br/>
 * 功能:版本上传至服务器 <br/>
 * 依赖 : NewFileFetchServlet
 */
@RequestMapping("/version")
public class VersionControllServlet extends BaseServlet{
	private static File generatePath = null;
	
	static{
		generatePath = new File(dataTempPath,"filefetch");
	}
	
	/**
	 * 
	 * 作者:sanri <br/>
	 * 时间:2018-6-17下午4:48:21<br/>
	 * 功能:列出本次版本需要上传的文件 <br/>
	 * @param filename
	 * @throws IOException 
	 */
	public FindFilesResult listUploadFiles(String filename) throws IOException{
		FindFilesResult filesResult = new FindFilesResult();
		filesResult.setFilename(filename);
		if(StringUtils.isBlank(filename)){
			return filesResult;
		}
		
		//解压文件
		File zipFile = new File(generatePath,filename);
		ZipUtil.unzip(zipFile, "");
		
		String baseName = FilenameUtils.getBaseName(filename);
		File directory = new File(generatePath,baseName);
		
		if(!directory.exists()){
			throw new IllegalArgumentException(directory+" 目录不存在");
		}
		
		Collection<File> listFiles = FileUtils.listFiles(directory, null, true);
		
		List<String> filenames = new ArrayList<String>();
		filesResult.setErrorFiles(filenames);
		
		if(CollectionUtils.isNotEmpty(listFiles)){
			for (File file : listFiles) {
				//获取相对于基础路径的路径 
				URI uri = file.toURI();
				URI baseURI = directory.toURI();
				URI relativizeURI = baseURI.relativize(uri);
				filenames.add(relativizeURI.toString());
			}
		}
		
		return filesResult;
	}
	
	/**
	 * 
	 * 作者:sanri <br/>
	 * 时间:2018-6-17下午5:23:31<br/>
	 * 功能: 执行上传并解压 <br/>
	 * @param filename
	 * @throws IOException 
	 */
	public void upload(String connName,String filename,String version) throws IOException{
		if(StringUtils.isBlank(connName) || StringUtils.isBlank(filename) || StringUtils.isBlank(version)){
			throw new IllegalStateException("需要提供连接名["+connName+"]和文件名["+filename+"]和版本号["+version+"]");
		}
		ItemProperties itemProperties = NewFileFetchServlet.itemPropertiesMap.get(connName);
		
		String baseName = FilenameUtils.getBaseName(filename);
		File dir = new File(generatePath,baseName);
		if(!dir.exists()){
			//如果不存在目录,解压文件
			File zipFile = new File(generatePath,filename);
			ZipUtil.unzip(zipFile, "");
		}
		
		//进入 filename 路径 ,将里面的项目再进行打包
		String project = itemProperties.getString("project");
		File projectUpload = new File(dir,project);
		File projectUploadZipFile = ZipUtil.zip(projectUpload);
		
		String filepath = itemProperties.getString("filepath");
		SSHService sshService = csshService(itemProperties);
		
		FileInputStream fileInputStream = new FileInputStream(projectUploadZipFile);
		sshService.upload(fileInputStream, filename, filepath, true);
		IOUtils.closeQuietly(fileInputStream);
		
		sshService.unzip(filepath+"/"+filename);
		
		//结尾清理工作
		//删除本地临时目录
		try {
			FileUtils.deleteDirectory(dir);
		} catch (IOException e) {
			e.printStackTrace();
		}
//		//清除服务器上的文件
//		sshService.rm(filepath+"/"+filename);
		
		//保留服务器版本信息 version
		sshService.mkdir(filepath+"/"+version);
		sshService.mv(filepath+"/"+filename,filepath+"/"+version+"/"+filename);
	}

	private SSHService csshService(ItemProperties itemProperties) throws IOException {
		String host = itemProperties.getString("host");
		int port = itemProperties.getInt("port");
		String username = itemProperties.getString("username");
		String password = itemProperties.getString("password");
		
		SSHService sshService = SSHService.createSession(host, port, username, password);
		return sshService;
	}
	
	/**
	 * 
	 * 作者:sanri <br/>
	 * 时间:2018-6-18下午1:05:44<br/>
	 * 功能:服务器上合并版本 <br/>
	 * @param version
	 * @param connName
	 * @throws IOException 
	 */
	public String merge(String version,String connName) throws IOException{
		if(StringUtils.isBlank(connName) || StringUtils.isBlank(version)){
			throw new IllegalStateException("需要提供连接名["+connName+"]和版本号["+version+"]");
		}
		ItemProperties itemProperties = NewFileFetchServlet.itemPropertiesMap.get(connName);
		String filepath = itemProperties.getString("filepath");
		//filepath+"/"+version 下所有文件按照时间升序解压
		SSHService sshService = csshService(itemProperties);
		sshService.exec("ls "+( filepath+"/"+version) +" | xargs -n 1 unzip -o ");
		try {
			//等待 5 秒的解压时间后
			Thread.sleep(3000);
			//压缩项目 ,并下载到本地路径 ,提供文件名可下载
			String project = itemProperties.getString("project");
			sshService.targz(filepath+"/"+version+"/"+project+".tar.gz", filepath+"/"+version+"/"+project);
			Thread.sleep(2000);
			InputStream download = sshService.download(filepath+"/"+version+"/"+project+".tar.gz");
			
			//存放目录为 generatePath+"/"+version
			File finalPath = new File(generatePath,version+"release"+System.currentTimeMillis());
			if(!finalPath.exists()){
				finalPath.mkdirs();
			}
			
			File finalVersionFile = new File(finalPath,project+".tar.gz");
			FileOutputStream fileOutputStream = new FileOutputStream(finalVersionFile);
			IOUtils.copy(download, fileOutputStream);
			IOUtils.closeQuietly(fileOutputStream);
			
			//删除服务器文件,此操作需要小心 
			String rmDir = filepath+"/"+version+"/";
			if(StringUtils.isNotBlank(rmDir) && !"/".equals(rmDir) && StringUtils.isNotBlank(filepath) && StringUtils.isNotBlank(version)){
				sshService.exec("rm -rf "+rmDir);
			}
			
			//返回相对于 generatePath 的文件路径 
			URI generatePathURI = generatePath.toURI();
			URI fileURI = finalVersionFile.toURI();
			URI relativizeURI = generatePathURI.relativize(fileURI);
			
			return relativizeURI.toString();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		return "";
	}

	/**
	 * 
	 * 作者:sanri <br/>
	 * 时间:2018-6-18下午2:12:22<br/>
	 * 功能:合并后的文件下载<br/>
	 * @param relativizeURI
	 * @throws IOException 
	 */
	public void release(String filename,HttpServletRequest request,HttpServletResponse response) throws IOException{
		File releaseFile = new File(generatePath,filename);
		if(releaseFile.exists()){
			FileInputStream fileInputStream = new FileInputStream(releaseFile);
			download(fileInputStream, MimeType.AUTO, releaseFile.getName(), request, response);
		}
		
	}
	
}
