package sanri.utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.net.SocketException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;

/**
 * 
 * 作者:sanri <br/>
 * 时间:2017-8-14下午2:59:12<br/>
 * 功能: ftp 工具类 <br/>
 */
public class FtpClientUtil {
	// ftp 客户端列表 主机_用户 =>客户端
	public static final Map<String, FTPClient> ftpClients = new HashMap<String, FTPClient>();

	public void init() {

	}

	// 销毁后结束所有客户端
	public void closeClients() {
		Iterator<FTPClient> iterator = ftpClients.values().iterator();
		while (iterator.hasNext()) {
			FTPClient next = iterator.next();
			try {
				next.disconnect();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * 
	 * 作者:sanri <br/>
	 * 时间:2017-8-14下午3:05:32<br/>
	 * 功能:查询 fptclient <br/>
	 * 
	 * @param host
	 * @param username
	 * @return
	 */
	public static FTPClient findFtpClient(String host, String username, String password) {
		FTPClient ftpClient = ftpClients.get(host + "_" + username);
		if (ftpClient == null) {
			ftpClient = new FTPClient();
			ftpClients.put(host + "_" + username, ftpClient);
		}
		try {
			ftpClient.connect(host);
			ftpClient.login(username, password);
		} catch (SocketException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return ftpClient;
	}

	/**
	 * 
	 * 作者:sanri <br/>
	 * 时间:2017-8-14下午3:15:17<br/>
	 * 功能:将 ftp 文件流输出到本地<br/>
	 * 
	 * @param ftpClient
	 * @param path
	 *            文件完整路径
	 * @param out
	 * @throws FileNotFoundException 
	 */
	public static void transferTo(FTPClient ftpClient, String path, OutputStream out) throws FileNotFoundException {
		File file = new File(path);
		String filename = file.getName();
		String filePath = file.getParent();
		try {
			// String filePath="//20170816";
			ftpClient.changeWorkingDirectory(filePath);
			FTPFile[] listFiles = ftpClient.listFiles();
			boolean find = false;
			if (listFiles != null && listFiles.length > 0) {
				for (FTPFile ftpFile : listFiles) {
					if (ftpFile.getName().equals(filename)) {
						find = true;
						try {
							ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
							ftpClient.retrieveFile(ftpFile.getName(), out);
						} catch (Exception e) {
							e.printStackTrace();
						}
						return;
					}
				}
			}
			if (!find) {
				throw new FileNotFoundException("文件未找到:" + path);
			}
		}catch(FileNotFoundException e){
			throw e;
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				ftpClient.logout();
				ftpClient.disconnect();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}


}
