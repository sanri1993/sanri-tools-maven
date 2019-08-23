package com.sanri.initexec;

import com.sanri.app.BaseServlet;
import org.jsoup.Connection;

import javax.annotation.PostConstruct;

public class InitConfigPath extends BaseServlet {
    @PostConstruct
    public void paths(){
        //创建所有模块配置路径
        mkConfigPath("tempcode");
        mkConfigPath("configcenter");
        mkConfigPath("zookeeper");
        mkConfigPath("translate");
        mkConfigPath("sql");
        mkConfigPath("redis");
        mkConfigPath("exportSql");
        mkConfigPath("templateCodePath");
        mkConfigPath("tableTemplate");
        mkConfigPath("codeSchema");

        mkTmpPath("exportTmp");
    }
}
