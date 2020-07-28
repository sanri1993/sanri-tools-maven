package com.sanri.tools.modules.protocol.param;

import lombok.Data;

import javax.sql.DataSource;
import java.util.Properties;

@Data
public class DatabaseConnectParam extends AbstractConnectParam{
    private AuthParam authParam;
    private String dbType;
    private String database;
    private String spellingRule = "lower";

    public static final String dbType_mysql = "mysql";
    public static final String dbType_postgresql = "postgresql";
    public static final String dbType_oracle = "oracle";

    public String driverClass(){
        switch (dbType){
            case dbType_mysql:
                return "com.mysql.jdbc.Driver";
            case dbType_postgresql:

        }

        return null;
    }

    public String url(){
        switch (dbType){
            case dbType_mysql:

                break;

        }

        return null;
    }

    public Properties properties(){
        Properties properties = new Properties();
        properties.put("user", authParam.getUsername());
        properties.put("password", authParam.getPassword());
        properties.put("remarksReporting","true");

        switch (dbType){
            case dbType_mysql:
                properties.setProperty("remarks", "true");
                properties.setProperty("useInformationSchema", "true");
                break;
        }

        return properties;
    }

}
