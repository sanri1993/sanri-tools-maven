package com.sanri.app.servlet;

import com.alibaba.fastjson.JSONObject;
import com.sanri.app.BaseServlet;
import com.sanri.app.postman.RedisKeyResult;
import com.sanri.frame.DispatchServlet;
import com.sanri.frame.RequestMapping;
import org.I0Itec.zkclient.serialize.ZkSerializer;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.ScanParams;
import redis.clients.jedis.ScanResult;
import redis.clients.jedis.Tuple;
import redis.clients.jedis.exceptions.JedisDataException;
import sanri.utils.NumberUtil;

import java.io.IOException;
import java.util.*;

import static com.sanri.app.servlet.ZkServlet.zkSerializerMap;

@RequestMapping("/redis")
public class RedisConnectServlet extends BaseServlet {
    static Map<String,Jedis> jedisMap = new HashMap<String, Jedis>();
    private String modul = "redis";

    /**
     * 获取连接信息
     * @param name
     * @return
     * @throws IOException
     */
    final static String [] sections = {"Server","Clients","Memory","Persistence","Stats","Replication","CPU","Cluster","Keyspace"};
    public Map<String,String> redisInfo(String name) throws IOException {
        Jedis jedis = jedis(name);
        Map<String,String> infos = new HashMap<>();
        for (String section : sections) {
            String info = jedis.info(section);
            infos.put(section,info.trim());
        }
        return infos;
    }

    /**
     * 展示数据库及其大小
     * @param name
     * @return
     * @throws IOException
     */
    public Map<String,Long> dbs(String name) throws IOException {
        Map<String,Long> dbsMap = new HashMap<>();

        Jedis jedis = jedis(name);
        List<String> databases = jedis.configGet("databases");
        int size = NumberUtil.toInt(databases.get(1));
        boolean cluster = false;
        for (int i = 0; i < size; i++) {
            if(cluster){    //集群环境下只能用 0 库
                dbsMap.put(i+"",0L);
                continue;
            }
            try {
                jedis.select(i);
                dbsMap.put(i+"",jedis.dbSize());
            }catch (JedisDataException e){
                dbsMap.put(0+"",jedis.dbSize());
                cluster = true;
            }

        }
        return dbsMap;
    }

    /**
     * 查询 key 信息
     * @param name
     * @param index
     * @param pattern
     * @param cursor
     * @param limit
     * @return
     * @throws IOException
     */
    public List<RedisKeyResult> scan(String name,int index, String pattern, int cursor, int limit) throws IOException {
        Jedis jedis = jedis(name);if(index != 0){jedis.select(index);};

        ScanParams scanParams = new ScanParams();
        scanParams.count(limit);
        if(StringUtils.isNotBlank(pattern)) {
            scanParams.match(pattern);
        }
        ScanResult<String> scanResult = jedis.scan(cursor+"", scanParams);
        List<String> result = scanResult.getResult();
        //如果搜索结果为空,则继承搜索,直到有值或搜索到末尾

        while (CollectionUtils.isEmpty(result) && cursor != 0){
            scanResult = jedis.scan(scanResult.getStringCursor(), scanParams);
            result = scanResult.getResult();
            cursor = scanResult.getCursor();
        }

        List<RedisKeyResult> redisKeyResults = new ArrayList<>();
        for (String item : result) {
            String type = jedis.type(item);
            Long ttl = jedis.ttl(item);
            Long pttl = jedis.pttl(item);
            RedisKeyResult redisKeyResult = new RedisKeyResult(item, type, ttl, pttl);
            RedisType redisType = RedisType.parse(type);
            switch (redisType){
                case string:
                    redisKeyResult.setLength(jedis.strlen(item));
                    break;
                case Set:
                case ZSet:
                case List:
                    redisKeyResult.setLength(jedis.llen(item));
                    break;
                case Hash:
                    redisKeyResult.setLength(jedis.hlen(item));
            }
            redisKeyResults.add(redisKeyResult);
        }
        return redisKeyResults;
    }

    /**
     * map 数据扫描
     * @param name
     * @param index
     * @param key
     * @param pattern
     * @param cursor
     * @param limit
     * @return
     * @throws IOException
     */
    public List<String> hscan(String name, int index, String key, String pattern, int cursor, int limit) throws IOException {
        Jedis jedis = jedis(name);if(index != 0){jedis.select(index);};

        ScanParams scanParams = new ScanParams();
        scanParams.count(limit);
        if(StringUtils.isNotBlank(pattern)) {
            scanParams.match(pattern);
        }

        ScanResult<Map.Entry<String, String>> scanResult = jedis.hscan(key, cursor+"", scanParams);

        List<Map.Entry<String, String>> result = scanResult.getResult();
        while (CollectionUtils.isEmpty(result) && cursor != 0){
            scanResult = jedis.hscan(key,scanResult.getStringCursor(), scanParams);
            result = scanResult.getResult();
            cursor = scanResult.getCursor();
        }

        System.out.println(result);
        return null;
    }

    /**
     * string 类型的数据读取
     * @param name
     * @param index
     * @param key
     * @param serialize
     * @return
     * @throws IOException
     */
    public Object readValue(String name,int index,String key,String serialize) throws IOException {
        Jedis jedis = jedis(name);if(index != 0){jedis.select(index);}

        //获取当前键类型
        String type = jedis.type(key);
        RedisType redisType = RedisType.parse(type);
        ZkSerializer zkSerializer = zkSerializerMap.get(serialize);
        if(zkSerializer == null){zkSerializer = hexSerizlize;}

        if(redisType == RedisType.string){
            byte[] bytes = jedis.get(key.getBytes(charset));
            return zkSerializer.deserialize(bytes);
        }

        throw new IllegalArgumentException("类型不支持读取数据:"+type);
    }

    Jedis jedis(String name) throws IOException {
        Jedis jedis = jedisMap.get(name);
        if(jedis == null){
            FileManagerServlet fileManagerServlet = DispatchServlet.getServlet(FileManagerServlet.class);
            String redisConnInfo = fileManagerServlet.readConfig(modul, name);

            JSONObject jsonObject = JSONObject.parseObject(redisConnInfo);
            String address = jsonObject.getString("connectStrings");
            String auth = jsonObject.getString("auth");

            String[] split = StringUtils.split(address, ':');
            jedis = new Jedis(split[0], NumberUtil.toInt(split[1]), 1000, 60000);
            if(StringUtils.isNotBlank(auth)){
                jedis.auth(auth);
            }
            jedisMap.put(name,jedis);
        }
        return jedis;
    }
    enum RedisType{
        string("string"),Set("set"),ZSet("zset"),Hash("hash"),List("list");
        private String value;

        RedisType(String value) {
            this.value = value;
        }

        public static RedisType parse(String type){
            RedisType[] values = RedisType.values();
            for (RedisType value : values) {
                if(value.value.equals(type)){
                    return value;
                }
            }
            return null;
        }
    }
    private static ZkSerializer hexSerizlize = zkSerializerMap.get("hex");

}
