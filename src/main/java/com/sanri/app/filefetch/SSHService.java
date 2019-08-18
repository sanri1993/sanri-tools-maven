package com.sanri.app.filefetch;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import ch.ethz.ssh2.Connection;
import ch.ethz.ssh2.SCPClient;
import ch.ethz.ssh2.Session;

/**
 * 
 * 作者:sanri <br/>
 * 时间:2018-6-17上午11:45:12<br/>
 * 功能:上传文件服务,工厂实例  <br/>
 * 使用包 : ganymed-ssh2-build210.jar
 */
public class SSHService {
	private String host;
	private int port;
	private String username;
	private Connection connection;
	private Session session;
	
	// 会话列表 ip:port@username==> instance  或者 ip:port ==> instance
	final static Map<String,SSHService> instances = new HashMap<String, SSHService>();
	
	private SSHService(){}
	private SSHService(String host,int port,String username){
		this.host = host;
		this.port = port;
		this.username = username;
	}
	
	/**
	 * 
	 * 作者:sanri <br/>
	 * 时间:2018-6-17上午11:50:24<br/>
	 * 功能:创建一个会话连接 <br/>
	 * @param host
	 * @param username
	 * @param password
	 * @return
	 * @throws IOException
	 */
	public static SSHService createSession(String host,String username,String password) throws IOException{
		return createSession(host, 22, username, password);
	}
	public static SSHService createSession(String host,int port,String username,String password) throws IOException{
		//如果已经存在连接,则直接返回
		final String hostPort = host+":"+port;
		final String hostPortUsername = host+":"+port+"@"+username;
		SSHService sshService = instances.get(hostPortUsername);
		if(sshService != null){
			return sshService;
		}
		
		sshService = instances.get(hostPort);
		if(sshService != null){
			//这种是有连接,但用户名密码失败,需要重新连接
			return reConnect(sshService,username,password);
		}
		
		//创建连接,新建服务
		Connection conn = new Connection(host);
		conn.connect();
		sshService = new SSHService(host,port,username);
		instances.put(hostPort, sshService);
		sshService.connection = conn;
		
		//使用重连方法进行连接
		return reConnect(sshService, username, password);
		
	}
	
	/**
	 * 
	 * 作者:sanri <br/>
	 * 时间:2018-6-17下午12:05:38<br/>
	 * 功能:重新连接服务,使用新的用户名,密码 <br/>
	 * @param sshService
	 * @param username
	 * @param password
	 * @return
	 * @throws IOException 
	 */
	public static SSHService reConnect(SSHService sshService, String username, String password) throws IOException {
		Connection conn = sshService.connection;
		boolean isAuthenticated = conn.authenticateWithPassword(username, password);
		if(!isAuthenticated){
			throw new IllegalStateException("连接失败,用户名密码错误");
		}
		
		String host = sshService.host;
		int port = sshService.port;
		final String hostPort = host+":"+port;
		final String hostPortUsername = host+":"+port+"@"+username;
		
		//授权通过,修改连接信息
		SSHService currentSSHService = instances.remove(hostPort);
		instances.put(hostPortUsername, currentSSHService);
		
		Session session = conn.openSession();
		session.requestPTY("vt100", 80, 24, 640, 480, null); 
		currentSSHService.session = session;
		
		return sshService;
	}
	
	
	/**
	 * 
	 * 作者:sanri <br/>
	 * 时间:2018-6-17下午12:16:53<br/>
	 * 功能:执行命令; 这个包,每个 session 只能执行一条指令,坑 <br/>
	 * @return
	 * @throws IOException 
	 */
	public String exec(String cmd) throws IOException{
		this.session.execCommand(cmd);
		InputStream stdout = this.session.getStdout();
		List<String> readLines = IOUtils.readLines(stdout);
		
		//关闭 session ,重新打开 
		this.session.close();
		this.session = this.connection.openSession();
		
		return StringUtils.join(readLines,"\n");
	}
	
	/**
	 * 
	 * 作者:sanri <br/>
	 * 时间:2018-6-17下午12:28:53<br/>
	 * 功能:创建目录,递归创建 <br/>
	 * @param dir
	 * @throws IOException
	 */
	public void mkdir(String dir) throws IOException{
		exec("mkdir -p "+dir);
	}
	
	public void cd(String path) throws IOException{
		exec("cd "+path);
	}
	
	/**
	 * 
	 * 作者:sanri <br/>
	 * 时间:2018-6-17下午12:31:21<br/>
	 * 功能:当前目录 <br/>
	 * @return
	 * @throws IOException
	 */
	public String pwd() throws IOException{
		return exec("pwd");
	}
	
	/**
	 * 
	 * 作者:sanri <br/>
	 * 时间:2018-6-17下午12:30:34<br/>
	 * 功能: 文件或目录打包<br/>
	 * @param desc
	 * @param src
	 * @throws IOException
	 */
	public void targz(String desc,String src) throws IOException{
		exec("tar -zc -f "+desc +" "+src);
	}
	
	/**
	 * 
	 * 作者:sanri <br/>
	 * 时间:2018-6-17下午4:34:25<br/>
	 * 功能:强制解压覆盖文件 <br/>
	 * @param zipFile
	 * @throws IOException
	 */
	public void unzip(String zipFile) throws IOException{
		String currentDir = exec("dirname "+zipFile);
		String exec = exec("unzip -d  "+currentDir+" -o "+zipFile);
		System.out.println(exec);
	}
	
	/**
	 * 
	 * 作者:sanri <br/>
	 * 时间:2018-6-17下午2:33:36<br/>
	 * 功能:判断档名是否存在 <br/>
	 * @param path
	 * @return
	 * @throws IOException
	 */
	public boolean exist(String path) throws IOException{
		String exec = exec("captureDish -e "+path+" ; echo $?");
		if("1".equals(exec)){
			return false;
		}
		return true;
	}
	
	/**
	 * 
	 * 作者:sanri <br/>
	 * 时间:2018-6-17下午2:34:20<br/>
	 * 功能:判断档名是否存在,且为文件 <br/>
	 * @param path
	 * @return
	 * @throws IOException
	 */
	public boolean isFile(String path) throws IOException{
		String exec = exec("captureDish -f "+path+" ; echo $?");
		if("1".equals(exec)){
			return false;
		}
		return true;
	}

	/**
	 * 
	 * 作者:sanri <br/>
	 * 时间:2018-6-17下午2:35:16<br/>
	 * 功能:判断档名是否存在且为目录 <br/>
	 * @param path
	 * @return
	 * @throws IOException
	 */
	public boolean isDirectory(String path) throws IOException{
		String exec = exec("captureDish -d "+path+" ; echo $?");
		if("1".equals(exec)){
			return false;
		}
		return true;
	}
	
	/**
	 * 
	 * 作者:sanri <br/>
	 * 时间:2018-6-17下午2:38:54<br/>
	 * 功能:linux 执行 basename<br/>
	 * @param path
	 * @return
	 * @throws IOException 
	 */
	public String basename(String path) throws IOException{
		return exec("basename "+path);
	}
	
	/**
	 * 
	 * 作者:sanri <br/>
	 * 时间:2018-6-17下午12:24:59<br/>
	 * 功能:上传文件 <br/>
	 * 注: 适用于上传小文件
	 * @param in
	 * @param dir
	 * @param create
	 * @throws IOException 
	 */
	public void upload(InputStream in,String remoteFileName,String remoteTargetDirectory,boolean create) throws IOException{
		boolean file = isFile(remoteTargetDirectory);
		if(file){
			throw new IllegalArgumentException(remoteTargetDirectory+" 是个文件");
		}
		
		//判断目录是否存在,是否需要创建目录
		boolean exist = exist(remoteTargetDirectory);
		if(!exist){
			if(!create){
				throw new IllegalArgumentException(remoteTargetDirectory+" 目录不存在");
			}
			
			mkdir(remoteTargetDirectory);
		}
		
		SCPClient scpClient = new SCPClient(connection);
		byte [] data = new byte[ in.available()];
		in.read(data);
		scpClient.put(data, remoteFileName, remoteTargetDirectory);
	}
	
	/**
	 * 
	 * 作者:sanri <br/>
	 * 时间:2018-6-17下午12:26:25<br/>
	 * 功能:下载文件,只支持普通文件下载; <br/>
	 * 注:适合于下小文件,因为所有内容都是保存在内存中
	 * @param file
	 * @return
	 * @throws IOException 
	 */
	public InputStream download(String remoteFile) throws IOException{
		boolean file = isFile(remoteFile);
		if(!file){
			throw new IllegalArgumentException(remoteFile+" 不存在或不是文件");
		}
		
		SCPClient scpClient = new SCPClient(connection);
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		scpClient.get(remoteFile, outputStream);
		return new ByteArrayInputStream(outputStream.toByteArray());
	}
	
	/**
	 * 
	 * 作者:sanri <br/>
	 * 时间:2018-6-17下午12:27:44<br/>
	 * 功能:下载文件,如果是文件夹,将打包下载 <br/>
	 * @param fileOrDir
	 * 注:适合于下小文件
	 * @return
	 * @throws IOException 
	 */
	public InputStream downloadExtension(String fileOrDir) throws IOException{
		boolean exist = exist(fileOrDir);
		if(!exist){
			throw new IllegalArgumentException(fileOrDir+" 档案不存在");
		}
		boolean directory = isDirectory(fileOrDir);
		if(!directory){
			return download(fileOrDir);
		}
		//TODO 下目录有点小问题
		// 打包成 tar.gz 文件并下载
		String basename = basename(fileOrDir);
		String targetFile = fileOrDir+"/"+basename+".tar.gz";
		targz(targetFile, fileOrDir);
		
		return download(targetFile);
	}
	
	/**
	 * 
	 * 作者:sanri <br/>
	 * 时间:2018-6-17下午12:22:57<br/>
	 * 功能:关闭连接 <br/>
	 */
	public void close(){
		session.close();
		connection.close();
	}
	
	/**
	 * 
	 * 作者:sanri <br/>
	 * 时间:2018-6-17下午12:23:31<br/>
	 * 功能:关闭所有连接 <br/>
	 */
	public static void closeAll(){
		for (SSHService sshService : instances.values()) {
			try{
				sshService.close();
			}catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * 
	 * 作者:sanri <br/>
	 * 时间:2018-6-17下午7:06:41<br/>
	 * 功能: 只能删除文件<br/>
	 * @param string
	 * @throws IOException 
	 */
	public void rm(String path) throws IOException {
		exec("rm -f "+path);
	}
	
	/**
	 * 
	 * 作者:sanri <br/>
	 * 时间:2018-6-18下午1:03:52<br/>
	 * 功能:复制文件或目录 <br/>
	 * @param src
	 * @param dest
	 * @throws IOException
	 */
	public void cp(String src, String dest) throws IOException {
		exec("cp -r "+src +" "+dest);
	}
	
	/**
	 * 
	 * 作者:sanri <br/>
	 * 时间:2018-6-18下午2:34:12<br/>
	 * 功能: 移动文件或目录<br/>
	 * @param src
	 * @param dest
	 * @throws IOException
	 */
	public void mv(String src, String dest) throws IOException {
		exec("mv "+src +" "+dest);
	}
}
