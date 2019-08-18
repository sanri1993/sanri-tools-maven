package com.sanri.app.filefetch;

import java.io.File;
import java.io.FilenameFilter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

import com.alibaba.fastjson.JSONObject;

/**
 * 
 * 作者:sanri <br/>
 * 时间:2017-5-17上午11:43:32<br/>
 * 功能:文件路径映射处理 <br/>
 */
public class MirrorFilePathFilter implements FilePathFilter {
	private String commonMirrorReplaceBefore;
	private String commonMirrorReplaceAfter;
	private JSONObject otherMirror;
	
	private Map<String,String> config;
	
	/**
	 * 映射原 java 文件路径为 
	 * 包类路径 <==> 原文件路径
	 */
	@Override
	public Map<String, File> mappingPkgSourcePath(String handlePath) throws IllegalArgumentException{
		String projectDeployPath = config.get("sourcepath");
		File projectDeployFile = new File(projectDeployPath);
		if(!projectDeployFile.exists()){
			throw new IllegalArgumentException("源路径不存在");
		}
		Map<String, File> pathMap = new HashMap<String, File>();
		if(StringUtils.isBlank(handlePath)){
			return pathMap;
		}
		
		String extension  =  FilenameUtils.getExtension(handlePath);
		//对不同文件区别对待
		// ("js$html$css$svg$eot$ttf$woff$gif$png$jpg")
		if(StringUtils.isNotBlank(extension)){
			extension = extension.toLowerCase();
			if("java".equals(extension)){
				handlerJavaPath(handlePath, projectDeployFile, pathMap);
			}else{
				//处理其它文件
				String pkgPath = "";
				File destFile = null;
				
				Set<String> keySet = otherMirror.keySet();
				if(keySet.contains(extension)){
					JSONObject jsonObject = otherMirror.getJSONObject(extension);
					String replaceBefore = "",replaceAfter = "";
					Iterator<Entry<String, Object>> iterator = jsonObject.entrySet().iterator();
					while(iterator.hasNext()){
						Entry<String, Object> replaceEntry = iterator.next();
						replaceBefore = replaceEntry.getKey();
						replaceAfter = String.valueOf(replaceEntry.getValue());
						break;
					}
					pkgPath = handlePath.replace(replaceBefore, replaceAfter);
					destFile = new File(projectDeployFile,pkgPath);
					
					//pkgPath不一定是原路径
					pkgPath = pkgPath.replace(replaceAfter, "");
				}else{
					//通用处理
					pkgPath = handlePath.replace(commonMirrorReplaceBefore, commonMirrorReplaceAfter);
					destFile = new File(projectDeployFile,pkgPath);
					
					//pkgPath不一定是原路径
					pkgPath = pkgPath.replace(commonMirrorReplaceAfter, "");
				}
				
				pathMap.put(pkgPath, destFile);
			}
		}else{
			//无扩展名的,直接给出错误文件(有可能是目录,有可能是文件,由提供人员自己判断)
			throw new IllegalArgumentException("源路径不存在,file:"+handlePath); 
		}
		
		return pathMap;
	}

	/**
	 * 
	 * 作者:sanri <br/>
	 * 时间:2017-5-25上午9:37:25<br/>
	 * 功能:处理 java 文件 <br/>
	 * @param handlePath
	 * @param projectDeployFile
	 * @param pathMap
	 * @throws IllegalArgumentException
	 */
	private void handlerJavaPath(String handlePath, File projectDeployFile, Map<String, File> pathMap) throws IllegalArgumentException {
		JSONObject jsonObject = otherMirror.getJSONObject("java");
//		String pkgAppend = config.get("javapkgAppend");
				
		String beforePath = "",afterPath = "";
		Iterator<Entry<String, Object>> iterator = jsonObject.entrySet().iterator();
		while(iterator.hasNext()){
			Entry<String, Object> mirrorEntry = iterator.next();
			beforePath = mirrorEntry.getKey();
			afterPath = ObjectUtils.toString(mirrorEntry.getValue());
		}
		
		String packagePath = handlePath.replace(beforePath, afterPath).replace(".java", ".class");	//找到对应的 class 文件
		//找到所有有关的内部类文件
		File sourcePathFile = new File(projectDeployFile,packagePath);
		if(!sourcePathFile.exists()){
			throw new IllegalArgumentException("源文件不存在:"+sourcePathFile);
		}
		//得到基本名称,内部类是以 本类基本名$类部类名,如果是匿名的,则为数字
		final String fileName = sourcePathFile.getName();
		final String fileBaseName = FilenameUtils.getBaseName(fileName);
		
		File parentFile = sourcePathFile.getParentFile();
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
		//获取到文件
		for (File file : listFiles) {
			String absolutePath = file.getAbsolutePath();
			String pkgPath = absolutePath.replace(projectDeployFile.getPath(), "");
			pathMap.put(pkgPath, file);
		}
	}

	public void setConfig(Map<String, String> config) {
		this.config = config;
		//获取所有映射 
		String pathMirror = config.get("pathMirror");
		JSONObject otherMirror = JSONObject.parseObject(pathMirror);
		JSONObject commonMirror = otherMirror.getJSONObject("*");
		//获取通用映射的键值对
		Iterator<Entry<String, Object>>  it = commonMirror.entrySet().iterator();
		while(it.hasNext()){
			Entry<String, Object> next = it.next();
			commonMirrorReplaceBefore = next.getKey();
			commonMirrorReplaceAfter = String.valueOf(next.getValue());
			break;
		}
		otherMirror.remove("*");
		this.otherMirror = otherMirror;
	}
}
