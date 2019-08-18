package com.sanri.app.servlet;

import com.sanri.app.BaseServlet;
import com.sanri.app.ConfigCenter;
import com.sanri.app.jdbc.ExConnection;
import com.sanri.frame.RequestMapping;
import com.sanri.initexec.InitJdbcConnections;
import com.taobao.diamond.manager.DiamondManager;
import com.taobao.diamond.manager.ManagerListener;
import com.taobao.diamond.manager.impl.DefaultDiamondManager;
import org.apache.commons.dbutils.DbUtils;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.ResultSetHandler;
import org.apache.commons.lang3.StringUtils;
import sanri.utils.HttpUtil;

import java.io.IOException;
import java.nio.charset.Charset;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.Executor;

/**
 * 加载钻石配置,修改钻石配置;
 * 钻石读取数据库配置,这里依赖于数据库连接
 */
@RequestMapping("/diamond")
public class DiamondServlet extends BaseServlet {
    private  String connName = "diamond";

    // group-dataId
    Queue<String> latestUse = new ArrayDeque<String>(10);

    public String current(){
        return connName;
    }

    /**
     * 列出最近使用
     * @return
     */
    public Queue<String> latestUse(){
        return latestUse;
    }

    /**
     * 列出所有组
     * @return
     */
    public List<String> groups(){
        String sql = "select group_id from config_info group by group_id ";
        return listQuery(sql);
    }

    /**
     * 组内的所有 dataId 列表
     * @param group
     * @return
     */
    public List<String> dataIds(String group){
        String sql = "select data_id from config_info where group_id='"+group+"'";
        return listQuery(sql);
    }

    /**
     * 内容查询
     * @param group
     * @param dataId
     * @return
     */
    public String content(String group,String dataId){
         //添加最近使用
        int size = latestUse.size();
        if(size >= 10){
            latestUse.poll();
        }
        String key = group+"@"+dataId;
        if(!latestUse.contains(key)) {
            latestUse.add(key);
        }

        ExConnection exConnection = InitJdbcConnections.CONNECTIONS.get(connName);
        QueryRunner mainQueryRunner = new QueryRunner();
        String sql = "select content from config_info where group_id='%s' and data_id = '%s'";
        String formatSql = String.format(sql, group, dataId);
        Connection connection = null;
        try {
            connection = exConnection.getConnection();
            String content = mainQueryRunner.query(connection, formatSql, new ResultSetHandler<String>() {

                @Override
                public String handle(ResultSet resultSet) throws SQLException {
                    return resultSet.next() ? resultSet.getString(1) : "";
                }
            });
            return content;
        } catch (SQLException e) {
            e.printStackTrace();
        }finally {
            DbUtils.closeQuietly(connection);
        }
        return null;
    }

    private List<String> listQuery(String sql){
        Connection connection = null;
        QueryRunner mainQueryRunner = new QueryRunner();
        try {
            ExConnection exConnection = InitJdbcConnections.CONNECTIONS.get(connName);
            connection = exConnection.getConnection();
            List<String> groups = mainQueryRunner.query(connection, sql, new ResultSetHandler<List<String>>() {
                @Override
                public List<String> handle(ResultSet resultSet) throws SQLException {
                    List<String> groups = new ArrayList<String>();
                    while(resultSet.next()){
                        String group = resultSet.getString(1);
                        groups.add(group);
                    }
                    return groups;
                }
            });

            return groups;
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            DbUtils.closeQuietly(connection);
        }
        return null;
    }

    /**
     * 获取远程配置
     * @param group
     * @param dataId
     * @param env
     * @return
     */
    final String remoteConnect = "http://14.17.100.220:8081/sanritools/";
    final String remoteRoute = "/diamond/dispatch/config/%s";
    public String remoteConfigContent(String group,String dataId,String env) throws IOException {
        String address = remoteConnect + String.format(remoteRoute,env);
        //frms 特别对待
        if("hhx-gps-dev".equals(dataId)){
            group = "frms";
            dataId = "gps-dev";
        }
        if("qurong".equals(group)){
            group = "frms";
        }
        Map<String,String> params = new HashMap<String, String>();
        params.put("group",group);
        params.put("dataId",dataId);
        String data = HttpUtil.getData(address, params,Charset.forName("utf-8"));
        if(StringUtils.isNotBlank(data)) {
            data = data.substring(1, data.length() - 1).replaceAll("\\\\r\\\\n", "\n");
        }
        return data;
    }

    /**
     * 获取测试同名配置,主要是部署在远程机做转发接口操作
     * @param group
     * @param dataId
     * @return
     */
    @RequestMapping("/dispatch/config/captureDish")
    public String testConfig(String group,String dataId){
        DiamondManager diamondManager = getDiamondManager(group, dataId,"captureDish");
        String configureInfomation = diamondManager.getAvailableConfigureInfomation(6000);
        return configureInfomation;
    }

    /**
     * 获取生产同名配置,主要是部署在远程机做转发接口操作
     * @param group
     * @param dataId
     * @return
     */
    @RequestMapping("/dispatch/config/release")
    public String releaseConfig(String group,String dataId){
        DiamondManager diamondManager = getDiamondManager(group, dataId,"release");
        String configureInfomation = diamondManager.getAvailableConfigureInfomation(6000);
        return configureInfomation;
    }

    static  Map<String,DiamondManager> diamondManagerMap = new HashMap<String, DiamondManager>();

    private DiamondManager getDiamondManager(String group, String dataId,String env) {
        int position = dataId.lastIndexOf("-");
        String dataIdPrefix = dataId.substring(0,position);
        String realDataId = dataIdPrefix + "-"+env;

        String key = group+dataId+env;
        DiamondManager diamondManager = diamondManagerMap.get(key);
        if(diamondManager != null){
            return diamondManager;
        }

        DefaultDiamondManager defaultDiamondManager = new DefaultDiamondManager(group, realDataId, new ManagerListener() {
            @Override
            public Executor getExecutor() {
                return null;
            }

            @Override
            public void receiveConfigInfo(String conifig) {

            }
        });
        diamondManagerMap.put(key,defaultDiamondManager);
        return defaultDiamondManager;
    }
}
