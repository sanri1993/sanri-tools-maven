package com.sanri.app.jdbc;

import com.mysql.jdbc.jdbc2.optional.MysqlDataSource;
import com.sanri.app.jdbc.codegenerate.RenamePolicyMybatisExtend;
import com.sanri.app.postman.JdbcConnDetail;
import oracle.jdbc.pool.OracleDataSource;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.ResultSetHandler;
import org.apache.commons.dbutils.handlers.ScalarHandler;
import org.apache.commons.lang3.ObjectUtils;
import sanri.utils.PropertyEditUtil;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class OracleExConnection extends ExConnection {
    public static final String dbType = "oracle";

    public OracleExConnection(DataSource dataSource) throws SQLException {
        super(dataSource);
    }

    @Override
    protected DataSource copyDataSource(String schemaName) throws SQLException {
//        OracleDataSource oracleDataSource = new OracleDataSource();
//        PropertyEditUtil.copyExclude(oracleDataSource,dataSource);
//        oracleDataSource.setDatabaseName(schemaName);
        return dataSource;
    }

    @Override
    protected List<Schema> refreshSchemas() throws SQLException {
//        TypeListHandler<String> resultSetHandler = new TypeListHandler<String>();
//        List<String> databases = mainQueryRunner.query("select * from v$database",resultSetHandler );
        List<Schema> schemas = new ArrayList<Schema>();
//        for (String database : databases) {
//            schemas.add(new Schema(database));
//        }

        schemas.add(new Schema(((OracleDataSource)dataSource).getUser()));
        return schemas;
    }

    @Override
    protected List<Table> refreshTables(String schemaName) throws SQLException {
        Schema schema = schemas.get(schemaName);
        OracleDataSource dataSource = (OracleDataSource) schema.dataSource();
        QueryRunner queryRunner = new QueryRunner(dataSource);
        //查询当前数据库所有表的主键信息
        String primaryKeySql ="select b.TABLE_NAME  as tableName,b.COLUMN_NAME as primaryKey from user_constraints a " +
                "inner join user_cons_columns b on a.CONSTRAINT_NAME = b.CONSTRAINT_NAME and a.CONSTRAINT_TYPE = 'P' ";
        Map<String, Set<String>> tablePrimaryMap = queryPrimaryKeys(queryRunner, primaryKeySql);

        List<Table> tables = queryRunner.query("select dt.table_name \"name\",dtc.comments \"comment\" " +
                "from user_tables dt,user_tab_comments dtc,user_objects uo " +
                "where dt.table_name = dtc.table_name and dt.table_name = uo.object_name and uo.object_type='TABLE'  ", new ResultSetHandler<List<Table>>() {
            @Override
            public List<Table> handle(ResultSet resultSet) throws SQLException {
                List<Table> tables = new ArrayList<Table>();
                while (resultSet.next()) {
                    String tableName = ObjectUtils.toString(resultSet.getString("name"));
                    String comments = resultSet.getString("comment");

                    Table table = new Table(tableName, comments);
                    Set<String> primaryKeys = tablePrimaryMap.get(tableName);
                    if(primaryKeys != null) {
                        table.setPrimaryKeys(primaryKeys);
                    }
                    tables.add(table);
                }
                return tables;
            }
        });
        return tables;
    }

    @Override
    protected List<Column> refreshColumns(String schemaName, String tableName) throws SQLException {
        Schema schema = schemas.get(schemaName);
        Table table = schema.getTable(tableName);
        Set<String> primaryKeys = table.getPrimaryKeys();

        QueryRunner queryRunner = new QueryRunner(schema.dataSource());
        String sql = "select a.COLUMN_NAME,a.DATA_TYPE,b.COMMENTS,a.DATA_PRECISION,a.DATA_SCALE,a.DATA_LENGTH from user_tab_columns a " +
                "inner join user_col_comments b on a.COLUMN_NAME = b.COLUMN_NAME " +
                "where a.TABLE_NAME = '"+tableName.toUpperCase()+"'";
        List<Column> columns = queryRunner.query(sql, new ResultSetHandler<List<Column>>() {
            @Override
            public List<Column> handle(ResultSet resultSet) throws SQLException {
                List<Column> columns = new ArrayList<Column>();
                while (resultSet.next()){
                    String columnName = resultSet.getString(1);
                    String dataType = resultSet.getString(2);
                    String comment = resultSet.getString(3);
                    int precision = resultSet.getInt(4);
                    int scale = resultSet.getInt(5);
                    long varcharLength = resultSet.getLong(6);
                    boolean isPrimaryKey = primaryKeys.contains(columnName);

                    ColumnType columnType = new ColumnType(dataType, precision, scale, varcharLength);
                    Column column = new Column(columnName, columnType, comment);
                    column.setPrimaryKey(isPrimaryKey);
                    columns.add(column);
                }
                return columns;
            }
        });
        return columns;
    }

    @Override
    public String getDatabase() {
        return ((OracleDataSource)dataSource).getDatabaseName();
    }

    @Override
    public JdbcConnDetail getConnDetail() {
        OracleDataSource dataSource = (OracleDataSource) getDataSource();
        JdbcConnDetail jdbcConnDetail = new JdbcConnDetail();
        jdbcConnDetail.config(dbType,dataSource.getServerName(),dataSource.getPortNumber(),dataSource.getUser(),dataSource.getDatabaseName());
//        jdbcConnDetail.setUserpass(getPassword(dataSource));
        return jdbcConnDetail;
    }

    @Override
    public String ddL(String schemaName, String tableName) throws SQLException {
        String sql = "select dbms_metadata.get_ddl('"+tableName+"','COMMUNITY') from dual";
        Schema schema = schemas.get(schemaName);
        QueryRunner queryRunner = new QueryRunner(schema.dataSource());
        return queryRunner.query(sql, new ScalarHandler<String>(1));
    }

    static RenamePolicyMybatisExtend renamePolicyMybatisExtend = new RenamePolicyMybatisExtend(dbType);
    @Override
    public RenamePolicyMybatisExtend getRenamePolicyMybatis() {
        return renamePolicyMybatisExtend;
    }

    @Override
    public String getDbType() {
        return dbType;
    }
}
