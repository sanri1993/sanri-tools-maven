package com.sanri.tools.modules.mybatis.service;

import com.alibaba.fastjson.JSONObject;
import com.sanri.tools.modules.core.dtos.PluginDto;
import com.sanri.tools.modules.core.service.classloader.ClassloaderService;
import com.sanri.tools.modules.core.service.file.FileManager;
import com.sanri.tools.modules.core.service.plugin.PluginManager;
import com.sanri.tools.modules.database.dtos.DynamicQueryDto;
import com.sanri.tools.modules.database.service.JdbcService;
import com.sanri.tools.modules.mybatis.dtos.BoundSqlParam;
import com.sanri.tools.modules.mybatis.dtos.BoundSqlResponse;
import com.sanri.tools.modules.mybatis.dtos.ProjectDto;
import org.apache.commons.dbutils.DbUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.scripting.defaults.DefaultParameterHandler;
import org.apache.ibatis.session.Configuration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class MybatisService {
    @Autowired
    private FileManager fileManager;

    @Autowired
    private PluginManager pluginManager;

    @Autowired
    private ClassloaderService classloaderService;

    @Autowired
    private JdbcService jdbcService;

    public static final String module = "mybatis";

    // projectName => Configuration
    private Map<String, Configuration> projectConfigurationMap = new ConcurrentHashMap<>();

    // projectName ==> classloaderName ??????????????????, ?????????????????????????????????????????????????????????????????????
    private Map<String,String> classloaderBind = new ConcurrentHashMap<>();

    /**
     * ?????????????????????,????????????????????????
     */
    public List<ProjectDto> projects(){
        List<ProjectDto> projectDtos = new ArrayList<>();
        Iterator<String> iterator = projectConfigurationMap.keySet().iterator();
        while (iterator.hasNext()){
            String project = iterator.next();
            String classloaderName = classloaderBind.get(project);
            ProjectDto projectDto = new ProjectDto(project, classloaderName);
            projectDtos.add(projectDto);
        }
        return projectDtos;
    }

    /**
     * ?????????????????? mapper ?????????????????????
     * @param project
     * @param file
     */
    public void newProjectFile(String project,String classloaderName, MultipartFile file) throws IOException {
        File projectDir = fileManager.mkTmpDir(module + "/" + project);
        file.transferTo(new File(projectDir,file.getOriginalFilename()));

        loadMapperFile(project,file.getOriginalFilename(),classloaderName);
    }

    /**
     * ?????????????????? mapper ??????
     * @param project
     * @param fileName
     * @throws IOException
     */
    public void loadMapperFile(String project,String fileName,String classloaderName) throws IOException {
        // ??????????????????
        ClassLoader classloader = classloaderService.getClassloader(classloaderName);
        Resources.setDefaultClassLoader(classloader);

        Resource resource = fileManager.relativeResource(module + "/" + project + "/" + fileName);
        InputStream inputStream = resource.getInputStream();
        Configuration configuration = projectConfigurationMap.computeIfAbsent(project, k -> {
            classloaderBind.put(k,classloaderName);
            serializer();
            return new Configuration();
        });

        XMLMapperBuilder mapperParser = new XMLMapperBuilder(inputStream, configuration, resource.getFilename(),configuration.getSqlFragments());
        mapperParser.parse();
        inputStream.close();

        Resources.setDefaultClassLoader(ClassLoader.getSystemClassLoader());
    }

    /**
     * ???????????????????????????????????????,??????????????????
     */
    private void serializer() {
        String collect = classloaderBind.entrySet().stream().map(entry -> StringUtils.join(Arrays.asList(entry.getKey(), entry.getValue()), ':')).collect(Collectors.joining("\n"));
        File moduleDir = fileManager.mkTmpDir(module);
        File bindClassloader = new File(moduleDir, "bindClassloader");
        try {
            FileUtils.writeStringToFile(bindClassloader,collect);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * ???????????????????????????????????? sqlId
     * @param project
     * @return
     */
    public List<String> statementIds(String project){
        Configuration configuration = projectConfigurationMap.computeIfAbsent(project, k -> new Configuration());
        Collection<MappedStatement> mappedStatements = configuration.getMappedStatements();
        List<String> collect = mappedStatements.stream().map(MappedStatement::getId).collect(Collectors.toList());
        return collect;
    }

    /**
     * ?????? statement ??????????????????
     * @param project
     * @param statementId
     * @return
     */
    public List<ParameterMapping> statemenetParams(String project, String statementId){
        Configuration configuration = projectConfigurationMap.computeIfAbsent(project, k -> new Configuration());
        MappedStatement mappedStatement = configuration.getMappedStatement(statementId);

        BoundSql boundSql1 = mappedStatement.getBoundSql(new HashMap<>());
        List<ParameterMapping> parameterMappings = boundSql1.getParameterMappings();
        return parameterMappings;
    }

    /**
     * ??????????????? sql ??????
     * @return
     */
    public BoundSqlResponse boundSql(BoundSqlParam boundSqlParam) throws ClassNotFoundException, IOException, SQLException {
        String project = boundSqlParam.getProject();
        String statementId = boundSqlParam.getStatementId();
        Configuration configuration = projectConfigurationMap.computeIfAbsent(project, k -> new Configuration());
        MappedStatement mappedStatement = configuration.getMappedStatement(statementId);

        String classloaderName = boundSqlParam.getClassloaderName();
        ClassLoader classloader = classloaderService.getClassloader(classloaderName);
        if (classloader == null)classloader = ClassLoader.getSystemClassLoader();

        JSONObject arg = boundSqlParam.getArg();
        String className = boundSqlParam.getClassName();
        Class<?> clazz = classloader.loadClass(className);
        BoundSql boundSql = null;
        if (clazz.isPrimitive() || clazz == String.class){
            String value = arg.getString("value");
            boundSql= mappedStatement.getBoundSql(arg);
        }else {
            Object parameterObject = arg.getObject("value", clazz);
            boundSql = mappedStatement.getBoundSql(parameterObject);
        }

        // ?????? sql ??????, ??????????????????????????????
        Connection connection = jdbcService.connection(boundSqlParam.getConnName());
        PreparedStatement statement = null;ResultSet resultSet = null;
        BoundSqlResponse boundSqlResponse;
        try {
            DefaultParameterHandler defaultParameterHandler = new DefaultParameterHandler(mappedStatement,boundSql.getParameterObject(),boundSql);
            statement = connection.prepareStatement(boundSql.getSql());
            defaultParameterHandler.setParameters(statement);

            // ????????? select ?????? ,???????????????????????????,?????????????????? sql ??????
            boundSqlResponse = null;
            SqlCommandType sqlCommandType = mappedStatement.getSqlCommandType();
            if (sqlCommandType == SqlCommandType.SELECT){
                resultSet = statement.executeQuery();
                DynamicQueryDto dynamicQueryDto = jdbcService.dynamicQueryProcessor.handle(resultSet);
                dynamicQueryDto.setSql(statement.toString());
                boundSqlResponse = new BoundSqlResponse(sqlCommandType,dynamicQueryDto);
            }else{
                DynamicQueryDto dynamicQueryDto = new DynamicQueryDto(statement.toString());
                boundSqlResponse = new BoundSqlResponse(sqlCommandType,dynamicQueryDto);
            }
        } finally {
            DbUtils.closeQuietly(connection,statement,resultSet);
        }

        return boundSqlResponse;
    }

    /**
     * ??????????????????,????????????????????????????????? mybatis ????????????
     * ????????????????????????????????????????????????,????????????????????????????????????????????????
     */
    public void reload() throws IOException {
        projectConfigurationMap.clear();
        File moduleDir = fileManager.mkTmpDir(module);
        // ????????????????????????
        File bind = new File(moduleDir, "bindClassloader");
        List<String> lines = FileUtils.readLines(bind, StandardCharsets.UTF_8);
        this.classloaderBind = lines.stream().map(line -> StringUtils.split(line, ':')).collect(Collectors.toMap(arr -> arr[0], arr -> arr[1]));

        File[] files = moduleDir.listFiles();
        for (File project : files) {
            if (project.isFile()){
                continue;
            }
            String projectName = project.getName();
            String classloaderName = classloaderBind.get(projectName);
            File[] mapperFiles = project.listFiles();
            for (File mapperFile : mapperFiles) {
                try {
                    loadMapperFile(projectName,mapperFile.getName(),classloaderName);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @PostConstruct
    public void register(){
        pluginManager.register(PluginDto.builder().module(module).name("main").build());
    }
}
