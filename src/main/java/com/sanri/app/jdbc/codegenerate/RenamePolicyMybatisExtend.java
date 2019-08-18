package com.sanri.app.jdbc.codegenerate;

import com.sanri.app.ConfigCenter;
import com.sanri.app.jdbc.MysqlExConnection;
import com.sanri.app.jdbc.PostgreSqlExConnection;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 扩展 mybatis 类型映射
 */
public class RenamePolicyMybatisExtend implements  MybatisTypeMapper{
    private static final String configFile = "db_mapper_mybatis_type";
    private static final Map<String, Map<String, String>> mappers = new HashMap<String,  Map<String, String>>();
    private String dbType = "mysql";

    static {
        //加载类型映射
        ConfigCenter configCenter = ConfigCenter.getInstance();
        String supports = configCenter.getString(configFile, "supports.dbType");
        String[] supportDbs = StringUtils.split(supports, ',');
        for (String supportDb : supportDbs) {
            Map<String, String> subConfigs = configCenter.getSubConfigs(configFile, supportDb);
            mappers.put(supportDb,subConfigs);
        }
    }

    public RenamePolicyMybatisExtend(String dbType) {
        this.dbType = dbType;
    }

    static Pattern pattern  = Pattern.compile("^(\\w+)");
    @Override
    public String mapperJdbcTypeName(String columnType) {
        Map<String, String> subConfigs = mappers.get(dbType);
        Matcher matcher = pattern.matcher(columnType);
        String typeName = "";
        if(matcher.find()){
            typeName = matcher.group(1);
        }
        return subConfigs.get(typeName);
    }

    /**
     * 注入数据库类型
     * @return
     */
    public String getDbType() {
        return dbType;
    }
}
