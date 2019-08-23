package com.sanri.app.servlet;

import com.sanri.app.BaseServlet;
import com.sanri.app.postman.ZooNodeACL;
import com.sanri.app.serializer.*;
import com.sanri.frame.RequestMapping;
import org.I0Itec.zkclient.ZkClient;
import org.I0Itec.zkclient.serialize.BytesPushThroughSerializer;
import org.I0Itec.zkclient.serialize.ZkSerializer;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.zookeeper.data.ACL;
import org.apache.zookeeper.data.Id;
import org.apache.zookeeper.data.Stat;
import sanri.utils.NumberUtil;
import sanri.utils.ReachableUtil;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * zookeeper 管理
 */
@RequestMapping("/zk")
public class ZkServlet extends BaseServlet {
    static File zookeeperConfigDir ;
    static Map<String,ZkClient> zkClientMap = new HashMap<String, ZkClient>();
    public static int sessionTimeout = 30000;
    public static int connectionTimeout = 5000;     //至少需要 5 秒
    //预置序列化工具
    private static ZkSerializer zkSerializer = new BytesPushThroughSerializer();
    private static StringSerializer stringSerializer = new StringSerializer();
    private static FastJsonSerializer fastJsonSerializer = new FastJsonSerializer();
    private static KryoSerializer kryoSerializer = new KryoSerializer();
    private static JdkSerializer jdkSerializer = new JdkSerializer();
    private static HessianSerializer hessianSerializer = new HessianSerializer();
    private static HexSerializer hexSerializer = new HexSerializer();

    static Map<String,ZkSerializer> zkSerializerMap = new LinkedHashMap<>();
    public static final String modul = "zookeeper";

    static {
        zookeeperConfigDir = mkConfigPath(modul);
        zkSerializerMap.put("string",stringSerializer);
        zkSerializerMap.put("fastJson",fastJsonSerializer);
        zkSerializerMap.put("jdk",jdkSerializer);
        zkSerializerMap.put("kryo",kryoSerializer);
        zkSerializerMap.put("hessian",hessianSerializer);
        zkSerializerMap.put("hex",hexSerializer);
    }

    /**
     * 读取连接字符串
     * @return
     */
    String detail(String name) throws IOException {
        File file = new File(zookeeperConfigDir, name);
        return FileUtils.readFileToString(file);
    }

    /**
     *  列出直接子节点
     * @return
     */
    public List<String> childrens(String name,String path) throws IOException {
        ZkClient zkClient = zkClient(name);
        List<String> children = zkClient.getChildren(path);
        return children;
    }

    /**
     * 获取序列化工具列表
     * @return
     */
    public Set<String> serializes(){
        return zkSerializerMap.keySet();
    }

    /**
     * 读取数据
     * @param name
     * @param path
     * @param deserialize 反序列化工具
     * @return
     */
    public Object readData(String name,String path,String deserialize) throws IOException {
        ZkClient zkClient = zkClient(name);
        Object data = zkClient.readData(path, true);
        if(data == null){
            return "";
        }
        byte [] dataBytes = (byte[]) data;
        ZkSerializer zkSerializer = zkSerializerMap.get(deserialize);
        Object object = zkSerializer.deserialize(dataBytes);
        return object;
    }

    /**
     * 获取节点元数据
     * @param name
     * @param path
     * @return
     */
    public Stat meta(String name,String path) throws IOException {
        ZkClient zkClient = zkClient(name);
        Map.Entry<List<ACL>, Stat> acl = zkClient.getAcl(path);
        Stat value = acl.getValue();
        return value;
    }

    /**
     * 获取 acl 权限列表
     * @param name
     * @param path
     * @return
     */
    public List<ZooNodeACL> acls(String name,String path) throws IOException{
        ZkClient zkClient = zkClient(name);
        Map.Entry<List<ACL>, Stat> entry = zkClient.getAcl(path);
        List<ACL> acls = entry.getKey();

        List<ZooNodeACL> zooNodeACLS = new ArrayList<ZooNodeACL>();
        if(CollectionUtils.isNotEmpty(acls)){
            for (ACL acl : acls) {
                Id id = acl.getId();
                ZooNodeACL zooNodeACL = new ZooNodeACL(id.getScheme(), id.getId(), acl.getPerms());
                zooNodeACLS.add(zooNodeACL);
            }
        }
        return zooNodeACLS;
    }

    /**
     * 获取一个客户端
     * @return
     */
    ZkClient zkClient(String name) throws IOException {
        ZkClient zkClient = zkClientMap.get(name);
        if(zkClient == null){
            String serverString = detail(name);
            zkClient = new ZkClient(serverString,sessionTimeout, connectionTimeout,zkSerializer);
            zkClientMap.put(name,zkClient);
        }
        return zkClient;
    }

    /**
     * 创建子节点
     * @param name
     * @param path
     * @param child
     * @return
     */
    public int createNode(String name,String path,String child) throws IOException {
        ZkClient zkClient = zkClient(name);
//        zkClient.createEphemeral();
//        zkClient.createPersistent();
//        zkClient.createEphemeralSequential();
//        zkClient.createPersistentSequential()
        return 0;
    }

    /**
     * 删除节点
     * @param name
     * @param path
     * @return
     * @throws IOException
     */
    public int deleteNode(String name,String path) throws IOException{
        ZkClient zkClient = zkClient(name);
        zkClient.deleteRecursive(path);
        return 0;
    }
}
