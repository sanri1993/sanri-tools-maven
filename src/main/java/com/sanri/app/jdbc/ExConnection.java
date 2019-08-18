package com.sanri.app.jdbc;

import com.sanri.app.jdbc.codegenerate.RenamePolicyMybatisExtend;
import com.sanri.app.postman.JdbcConnDetail;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.ResultSetHandler;
import org.apache.commons.lang3.ObjectUtils;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public abstract class ExConnection {
    private String dbType;
    protected DataSource dataSource;
    protected QueryRunner mainQueryRunner;

    private String connName;
    protected  Map<String,Schema> schemas = new HashMap<String, Schema>();

    public ExConnection(DataSource dataSource) throws SQLException {
        this.mainQueryRunner = new QueryRunner(dataSource);
        this.dataSource = dataSource;
        init();
    }

    /**
     * 创建一个连接实例
     * @param dbType
     * @param connName
     * @param dataSource
     * @return
     */
    public static ExConnection newInstance(String dbType, String connName, DataSource dataSource) throws SQLException {
        ExConnection exConnection = null;
        if(MysqlExConnection.dbType.equalsIgnoreCase(dbType)) {
            exConnection = new MysqlExConnection(dataSource);
        }else if(PostgreSqlExConnection.dbType.equalsIgnoreCase(dbType)){
            exConnection = new PostgreSqlExConnection(dataSource);
        }else if(OracleExConnection.dbType.equalsIgnoreCase(dbType)){
            exConnection = new OracleExConnection(dataSource);
        }
        if(exConnection != null) {
            exConnection.connName = connName;
        }
        return exConnection;
    }

    protected void init() throws SQLException {
        List<Schema> schemas = refreshSchemas();
        for (Schema schema : schemas) {
            //初始化数据源,需要区分数据源类型
            DataSource copyDataSource = copyDataSource(schema.getSchemaName());
            schema.setDataSource(copyDataSource);
            this.schemas.put(schema.getSchemaName(),schema);
        }
    }

    /**
     * 获取连接
     * @return
     * @throws SQLException
     */
    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    /**
     * 获取一个数据库
     * @param schemaName
     * @return
     */
    public Schema getSchema(String schemaName){
        return schemas.get(schemaName);
    }

    /**
     * 获取一张表
     * @param schemaName
     * @param tableName
     * @return
     */
    public Table getTable(String schemaName,String tableName){
        Schema schema = getSchema(schemaName);
        if(schema != null){
            return schema.getTable(tableName);
        }
        return null;
    }

    /**
     * 获取一列
     * @param schemaName
     * @param tableName
     * @param columnName
     * @return
     */
    public Column getColumn(String schemaName,String tableName,String columnName){
        Table table = getTable(schemaName, tableName);
        if(table != null){
            return table.getColumn(columnName);
        }
        return null;
    }

    /**
     * 获取所有的数据库
     * @param refresh
     * @return
     * @throws SQLException
     */
    public List<Schema> schemas(boolean refresh) throws SQLException {
        if(refresh || schemas.isEmpty()){
            this.schemas.clear();

            List<Schema> schemas = refreshSchemas();
            for (Schema schema : schemas) {
                //初始化数据源,需要区分数据源类型
                DataSource copyDataSource = copyDataSource(schema.getSchemaName());
                schema.setDataSource(copyDataSource);
                this.schemas.put(schema.getSchemaName(),schema);
            }
        }
        return new ArrayList<Schema>(schemas.values());
    }

    protected abstract DataSource copyDataSource(String schemaName) throws SQLException;

    /**
     * 获取某个数据库所有的表
     * @param schemaName
     * @param refresh
     * @return
     */
    public List<Table> tables(String schemaName,boolean refresh) throws SQLException {
        Schema schema = getSchema(schemaName);
        if(schema != null){
            if(refresh || schema.isEmptyTables()){
                schema.clearTables();

                List<Table> tables = refreshTables(schemaName);
                if(CollectionUtils.isNotEmpty(tables)) {
                    for (Table table : tables) {
                        schema.addTable(table);
                    }
                }
            }

            return new ArrayList<Table>(schema.getTables().values());
        }
        return null;
    }

    /**
     * 获取表所有的列
     * @param schemaName
     * @param tableName
     * @param refresh
     * @return
     */
    public List<Column> columns(String schemaName,String tableName,boolean refresh) throws SQLException {
        Table table = getTable(schemaName, tableName);
        //表为空,有可能表信息未刷新,先刷新一遍表
        if(table == null){
            refreshTables(schemaName);
            table = getTable(schemaName,tableName);
        }

        if(table != null){
            if(refresh || table.isEmptyColumns()){
                table.clearColums();

                List<Column> columns = refreshColumns(schemaName, tableName);
                for (Column column : columns) {
                    table.addColumn(column);
                }
            }

            return table.getColumns();
        }
        return null;
    }

    /**
     * 刷新数据库/模式
     * @return
     * @throws SQLException
     */
    protected abstract List<Schema> refreshSchemas() throws SQLException;

    /**
     * 刷新数据库的所有表
     * @param schemaName
     * @return
     */
    protected abstract List<Table> refreshTables(String schemaName) throws SQLException;

    /**
     * 刷新数据库某表的所有列
     * @param schemaName
     * @param tableName
     * @return
     */
    protected abstract List<Column> refreshColumns(String schemaName,String tableName) throws SQLException;

    public abstract String getDatabase();

    public DataSource getDataSource() {
        return dataSource;
    }

    /**
     * 获取某一个数据源的数据源
     * @param schemaName
     * @return
     */
    public DataSource getDataSource(String schemaName){
        Schema schema = schemas.get(schemaName);
        return schema.dataSource();
    }

    /**
     * 获取某一个数据库的 queryRunner
     * @param schemaName
     * @return
     */
    public QueryRunner getQueryRunner(String schemaName){
        return new QueryRunner(getDataSource(schemaName));
    }

    /**
     * 获取连接信息详情
     * @return
     */
    public abstract JdbcConnDetail getConnDetail();

    /**
     * 显示数据定义信息
     * @param schemaName
     * @param tableName
     * @return
     */
    public abstract String ddL(String schemaName, String tableName) throws SQLException;

    public abstract RenamePolicyMybatisExtend getRenamePolicyMybatis();

    public abstract String getDbType();

    public Map<String, Set<String>> queryPrimaryKeys(QueryRunner queryRunner,String primaryKeySql) throws SQLException {
        Map<String, Set<String>> tablePrimaryMap = queryRunner.query(primaryKeySql, new ResultSetHandler<Map<String, Set<String>>>() {
            @Override
            public Map<String, Set<String>> handle(ResultSet resultSet) throws SQLException {
                Map<String, Set<String>> tablePrimaryKeyMap = new HashMap<>();
                while (resultSet.next()) {
                    String primaryKey = ObjectUtils.toString(resultSet.getString(2));
                    String tableName = ObjectUtils.toString(resultSet.getString(1));
                    Set<String> strings = tablePrimaryKeyMap.get(tableName);
                    if(strings == null){
                        strings = new HashSet<>();
                        tablePrimaryKeyMap.put(tableName,strings);
                    }
                    strings.add(primaryKey);
                }

                return tablePrimaryKeyMap;
            }
        });

        return tablePrimaryMap;
    }
}
