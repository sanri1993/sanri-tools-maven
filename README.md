# 9420 开发工具包
---
sanri-tools-maven 是一个开源的用于企业开发的工具包，重点想解决项目开发中一些比较麻烦的问题

可在线生成entity、xml、dao、service、html、js、sql代码，减少70%以上的开发任务

常用的 kafka,zookeeper 数据监控等

---


## 工具理念

1. 轻量级,只依赖于文件系统
2. 小工具,大作用,减少模板代码的手工编写
3. 自定义框架,加快项目启动速度 ,目前项目启动时间为 600 ms 左右

## 已经有的工具

已经存在的工具可以在 /src/main/resources/com/sanri/config/tools.properties 中查看

1. 方法或变量取名
2. 数据提取
3. 生份证号码生成与验证
4. kafka  监控和 offset 设置,支持新旧版本 kafka
5. zookeeper 数据监控
6. 模板代码生成,根据列字段 
7. 列字段比较 
8. 数据库表字段,注释,名称查询,及后续模板代码操作
9. webservice 调试工具,只要输入 wsdl 地址,自动解析并构建 xml 消息 
10. 下划线转驼峰,驼峰转下划线工具
11. 图片转 base64 ,base64 转图片
12. SQL 客户端,已经支持 mysql,postgresql,oracle ; 可自定义实现其它数据库 
   * 表结构查询
   * pojo,xml  生成
   * 项目模板代码生成
   * 数据导出

13. 数据表处理工具（SQL 客户端升级版 ）
    * 可以根据变量自定义模板
    * 由多个模板组成一种方案
    * 单表使用模板生成，然后生成多种模板的代码后统一下载
    * 单表使用方案生成
    * 多表使用方案生成

## 扩展自己的工具

* 除前端交互 servlet 必须写在 com.sanri.app.servlet 包中以外,其它随便自己定制
* servlet 中的代码由于框架 javassist 的原因 ,不支持 java8 的 lambada 表达式

## 如何搭建环境 

1. 通过git下载源码

2. 修改部分配置，配置项说明在第 6 点

3. jdk 版本需 7 以上

4. 除了使用 maven 下载包,还需要依赖三个第三方包,执行如下命令安装 

   [下载依赖包](https://github.com/sanri1993/resources/tree/master/sanri-tools-maven)

   ```shell
   mvn install:install-file -Dfile=d:\IKAnalyzer2012FF_u1.jar -DgroupId=org.wltea.analyzer -DartifactId=IKAnalyzer -Dversion=2012FF_u1 -Dpackaging=jar
   mvn install:install-file -Dfile=d:\diamond-utils-2.0.5.5.jar -DgroupId=com.taobao.diamond -DartifactId=diamond-utils -Dversion=2.0.5.5 -Dpackaging=jar
   mvn install:install-file -Dfile=d:\diamond-client-2.0.5.5.jar -DgroupId=com.taobao.diamond -DartifactId=diamond-client -Dversion=2.0.5.5 -Dpackaging=jar
   ```

5. 启动

```shell
mvn jetty:run
```



6. 重要配置项说明 （所有的配置项都在 com.sanri.config 目录下）

* function.open.properties 用于配置临时文件路径和产生的配置路径 

* tools.properties  配置当前环境可以展示哪些工具，里面是所有工具的配置信息
* jdbcdefault.properties 项目初始化时加载的默认 jdbc 连接 ,可将你的数据库配置到这里
* mapper_jdbc_java.properties  这个是生成 java 实体类时，数据库类型映射到 java 类型
* db_mapper_mybatis_type.properties  这个是数据库类型映射到 mybatis 类型的映射表

7. 隐私说明 

* 由于有些功能用到了个人帐号，我目前还是留在配置文件中，请勿用于非法用途
* 如果有能力，请用私人帐号代替我的帐号
* 此工具纯属个人爱好创作，请勿用于商业用途

### 如何交流、反馈、参与贡献？

* Git仓库：https://github.com/sanri1993/sanri-tools-maven
* 官方QQ群：645576465
* 技术讨论、二次开发等咨询、问题和建议，请移步到 QQ 群，我会在第一时间进行解答和回复
* 如需关注项目最新动态，请Watch、Star项目，同时也是对项目最好的支持
* 微信扫码并关注我，获得项目最新动态及更新提醒

![我的微信](http://m.qpic.cn/psb?/V14Rorzr338mDG/78jM2YnEeTr4k549DS1U3w2UC5R5u.i7mRjj3dnT8m8!/b/dIMAAAAAAAAA&bo=ZABdAAAAAAARBwk!&rf=viewer_4)

### 演示效果图

![](http://m.qpic.cn/psb?/V14Rorzr338mDG/tKlN8Gz4dXJMvWATg4VCpHxXJ7ahO7SN8C*MuNDK.u4!/b/dMMAAAAAAAAA&bo=VgVgAgAAAAADBxM!&rf=viewer_4)



![](http://m.qpic.cn/psb?/V14Rorzr338mDG/btFWmDMeCvYOpR.JtSc3xokPTxM52TJbTyt3lXH9c*U!/b/dIMAAAAAAAAA&bo=xQRNAgAAAAADJ4w!&rf=viewer_4)