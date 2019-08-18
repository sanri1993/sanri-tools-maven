package com.sanri.app.servlet;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.sanri.app.BaseServlet;
import com.sanri.frame.RequestMapping;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.lang3.StringUtils;
import sanri.utils.HttpUtil;
import sanri.utils.SignUtil;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * spring cloud 的配置中心数据读取
 */
@RequestMapping("/config")
public class CloudConfigServlet extends BaseServlet {
    static File configCenter = null;
    //保存在每个连接下面的第一个文件,文件名为 address 保存着配置中心地址
    static final String addressFileName = "address";

    static {
        configCenter = mkConfigPath("configcenter");
    }
    /**
     * 读取 springcloud 配置
     * @param configUrl 配置地址
     * @param modul     模块名
     * @param env       环境名
     * @param branch    分支名
     * @return
     */
    public String readConfig(String conn,String modul,String env,String branch) throws IOException {
        String configUrl = address(conn);
        //拼接请求路径
        String[] urlParts = {configUrl,modul,env,branch};
        String finalPath = StringUtils.join(urlParts, '/');
        JSONObject jsonObject = HttpUtil.getJSON(finalPath,null);
//        String string = jsonObject.getJSONObject("propertySources").getString("source");
        JSONArray propertySources = jsonObject.getJSONArray("propertySources");
        if(!propertySources.isEmpty()){
            JSONObject configInfoObject = propertySources.getJSONObject(0);
            JSONObject source = configInfoObject.getJSONObject("source");
            return source.toJSONString();
        }
        return "";
    }

    /**
     * 加载配置列表
     * @return
     */
    public List<String> loadConfigs(){
        File[] files = configCenter.listFiles((FileFilter) DirectoryFileFilter.INSTANCE);
        List<String> dirNames = new ArrayList<String>();
        for (File file : files) {
            dirNames.add(file.getName());
        }
        return dirNames;
    }

    /**
     * 获取地址信息
     * @param conn
     * @return
     * @throws IOException
     */
    public String address(String conn) throws IOException {
        File file = new File(configCenter, conn + "/" + addressFileName);
        return FileUtils.readFileToString(file);
    }

    /**
     * 新连接
     * @param conn
     * @param address
     * @return
     */
    public int newConn(String conn,String address) throws IOException {
        File file = new File(configCenter, conn);
        if(!file.exists()){
            file.mkdir();
        }
        FileUtils.writeStringToFile(new File(file,addressFileName),address);
        return 0;
    }

    /**
     * 加载一个连接下的配置列表
     * @param conn
     * @return
     * @throws IOException
     */
    public JSONArray configs(String conn) throws IOException {
        File file = new File(configCenter, conn);
        File[] files = file.listFiles();
        JSONArray jsonArray = new JSONArray();
        for (File configFile : files) {
            String name = configFile.getName();
            if(name.equals(addressFileName)){
                continue;
            }
            String jsonData = FileUtils.readFileToString(configFile);
            jsonArray.add(JSONObject.parse(jsonData));
        }

        return jsonArray;
    }

    /**
     * 保存配置信息
     * @param modul
     * @param env
     * @param branch
     * @return
     */
    public int saveConfig(String conn,String modul,String env,String branch) throws IOException {
        File connDir = new File(configCenter, conn);
        Map<String,String> datas = new HashMap<String, String>();
        datas.put("modul",modul);
        datas.put("env",env);
        datas.put("branch",branch);
        FileUtils.writeStringToFile(new File(connDir,SignUtil.uniqueTimestamp()),JSONObject.toJSONString(datas));
        return 0;
    }
}
