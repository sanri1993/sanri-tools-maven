package com.sanri.tools.modules.database.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.dbutils.ResultSetHandler;
import org.apache.commons.dbutils.handlers.ColumnListHandler;
import org.apache.commons.dbutils.handlers.ScalarHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.util.List;

@Service
@Slf4j
public class ConfigDataService {
    @Autowired
    private JdbcService jdbcService;

    /**
     * 查询分组信息
     * @param connName
     * @param schemaName
     * @return
     * @throws SQLException
     */
    public List<String> groups(String connName, String schemaName) throws SQLException {
        String sql = "select group_id from "+schemaName+".config_info group by group_id ";
        return listQuery(connName,sql);
    }

    /**
     * 组内的所有 dataId 列表
     * @param group
     * @return
     */
    public List<String> dataIds(String connName, String schemaName,String group) throws SQLException {
        String sql = "select data_id from "+schemaName+".config_info where group_id='"+group+"'";
        return listQuery(connName,sql);
    }

    /**
     * 配置内容查询
     * @param group
     * @param dataId
     * @return
     */
    public String content(String connName,String schemaName,String group,String dataId) throws SQLException {
        String sql = "select content from "+schemaName+".config_info where group_id='%s' and data_id = '%s'";
        String formatSql = String.format(sql, group, dataId);
        String executeQuery = jdbcService.executeQuery(connName, sql, new ScalarHandler<String>(1));
        return executeQuery;
    }

    /**
     * 查询单列字符串的查询
     * @param connName
     * @param sql
     * @return
     */
    private List<String> listQuery(String connName,String sql) throws SQLException {
        ResultSetHandler<List<String>> resultSetHandler = new ColumnListHandler<String>(1);
        List<String> executeQuery = jdbcService.executeQuery(connName, sql, resultSetHandler);
        return executeQuery;
    }
}
