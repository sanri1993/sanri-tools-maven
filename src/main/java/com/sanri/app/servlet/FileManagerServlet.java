package com.sanri.app.servlet;

import com.sanri.app.BaseServlet;
import com.sanri.app.postman.ConfigPath;
import com.sanri.frame.RequestMapping;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import sanri.utils.ZipUtil;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@RequestMapping("/file/manager")
public class FileManagerServlet extends BaseServlet {
    /**
     * 返回所有模块
     * @return
     */
    public List<String> moduls(){
        return Arrays.asList(dataConfigPath.list());
    }

    /**
     * 写入配置信息
     * @param modul 模块路径
     * @param baseName 基础文件名称 可使用子路径 a/b
     * @param configs 配置信息
     */
    public int writeConfig(String modul,String baseName,String content) throws IOException {
        //content 可能有编码操作，需要解密
        content = URLDecoder.decode(content,"utf-8");
        File modulDir = new File(dataConfigPath, modul);
        // check modul exists
        if(!modulDir.exists())modulDir.mkdir();

        File configFile = new File(modulDir, baseName);
        FileUtils.writeStringToFile(configFile,content);
        return 0;
    }

    /**
     * 简单配置名列表
     * @param modul
     * @return
     */
    public List<String> simpleConfigNames(String modul){
        List<ConfigPath> configPaths = configNames(modul);
        List<String> names = new ArrayList<>();
        for (ConfigPath configPath : configPaths) {
            names.add(configPath.getPathName());
        }
        return names;
    }

    /**
     * 读取模块配置列表/顶层
     * @param modul
     * @return
     */
    public List<ConfigPath> configNames(String modul){
        File modulDir = new File(dataConfigPath, modul);
        // check modul exists
        if(!modulDir.exists())modulDir.mkdir();

        List<ConfigPath> configPaths = convertDir2ConfigPaths(modulDir);
        return configPaths;
    }

    /**
     * 一层一层来展示模块子项列表
     * @param modul
     * @param baseName
     * @return
     */
    public List<ConfigPath> configChildNames(String modul,String baseName){
        if(StringUtils.isBlank(baseName)){
            return configNames(modul);
        }
        File modulDir = new File(dataConfigPath, modul);
        // check modul exists
        if(!modulDir.exists())modulDir.mkdir();

        File targetDir = new File(modulDir, baseName);
        List<ConfigPath> configPaths = convertDir2ConfigPaths(targetDir);
        return configPaths;
    }

    /**
     * 读取配置
     * @param modul
     * @param baseName
     * @return
     */
    public String readConfig(String modul,String baseName) throws IOException {
        if(StringUtils.isBlank(baseName))return "";
        File modulDir = new File(dataConfigPath, modul);
        // check modul exists
        if(!modulDir.exists())modulDir.mkdir();
        File file = new File(modulDir, baseName);
        return FileUtils.readFileToString(file);
    }

    /**
     * 这个主要读取大文件或目录，为 tmp 路径的文件
     * @param baseName
     */
    public void downloadPath(String modul,String baseName, HttpServletRequest request, HttpServletResponse response) throws IOException {
        File modulDir = new File(dataTempPath, modul);
        File path = new File(modulDir,baseName);

        File targetFile = path;
        if(path.isDirectory()){
            targetFile = new File(path.getParent(),path.getName()+".zip");
            ZipUtil.zip(path, targetFile);
        }
        FileInputStream fileInputStream = null;
        try {
            fileInputStream = new FileInputStream(targetFile);
            download(fileInputStream, MimeType.AUTO, targetFile.getName(), request, response);
        }finally{
            IOUtils.closeQuietly(fileInputStream);
        }
    }

    private List<ConfigPath> convertDir2ConfigPaths(File modulDir) {
        List<ConfigPath> configPaths = new ArrayList<>();
        File[] files = modulDir.listFiles();
        for (File file : files) {
            String name = file.getName();
            boolean directory = file.isDirectory();
            configPaths.add(new ConfigPath(name,directory));
        }
        return configPaths;
    }

}
