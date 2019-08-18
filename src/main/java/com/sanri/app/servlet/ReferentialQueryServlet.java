package com.sanri.app.servlet;

import com.sanri.app.BaseServlet;
import com.sanri.app.jdbc.ExConnection;
import com.sanri.frame.RequestMapping;
import com.sanri.initexec.InitJdbcConnections;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.dbutils.DbUtils;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.handlers.MapListHandler;
import org.apache.commons.lang3.ObjectUtils;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * mysql 数据库的外键引用查询
 */
@RequestMapping("/refrence")
public class ReferentialQueryServlet extends BaseServlet {

    private String connName  = "default";

    public int setConnect(String connName){
        this.connName = connName;
        return 0;
    }

    public String current(){
        return connName;
    }

    /**
     * 查找表的上级外键关联
     * @param connName
     * @param database
     * @param tablename
     * @return
     */
    public List<String> parents(String tablename){
        Map<String, String> refrence = queryRefrence(tablename);
        Collection<String> values = refrence.values();
        HashSet<String> hashSet = new HashSet<String>(values);
        hashSet.remove(tablename);
        return new ArrayList<String>(hashSet);
    }


    /**
     * 查找表的子级外键关联
     * @param connName
     * @param database
     * @param tablename
     * @return
     */
    public List<String> childs(String tablename){
        Map<String, String> refrence = queryRefrence(tablename);
        Set<String> values = refrence.keySet();values.remove(tablename);
        return new ArrayList<String>(values);
    }

    /**
     * 查询引用表
     * @param connName
     * @param database
     * @param parents
     */
    private Map<String,String> queryRefrence(String tablename) {
        Connection connection = null;
        QueryRunner mainQueryRunner = new QueryRunner();

         Map<String,String> result = new HashMap<String,String>();
        try{
            ExConnection exConnection = InitJdbcConnections.CONNECTIONS.get(connName);
            connection = exConnection.getConnection();
            String sql = "SELECT TABLE_NAME,REFERENCED_TABLE_NAME from information_schema.referential_constraints where CONSTRAINT_SCHEMA = '"+exConnection.getDatabase()+"' and (REFERENCED_TABLE_NAME = '"+tablename+"' or TABLE_NAME = '"+tablename+"') ";
            List<Map<String, Object>> query = mainQueryRunner.query(connection, sql, new MapListHandler());

            if(CollectionUtils.isNotEmpty(query)){
                for (Map<String, Object> valueMap : query) {
                    Object tableName = valueMap.get("TABLE_NAME");
                    Object referencedTableName = valueMap.get("REFERENCED_TABLE_NAME");

                    result.put(ObjectUtils.toString(tableName),ObjectUtils.toString(referencedTableName));
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            DbUtils.closeQuietly(connection);
        }
        return result;
    }


    /**
     * 级联删除当前表; 所有与之关联的表都会全部删除
     * @param connName
     * @param database
     * @param tablename
     * @return
     */
    public int truncate(String tablename){
        Connection connection = null;
        QueryRunner mainQueryRunner = new QueryRunner();
        try {
            ExConnection exConnection = InitJdbcConnections.CONNECTIONS.get(connName);
            connection = exConnection.getConnection();
            int update = mainQueryRunner.update("truncate " + tablename);
            return update;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }
}
