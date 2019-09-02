package sanri.utils;

import java.io.*;
import java.net.SocketException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.junit.Test;

/**
 * 
 * 作者:sanri <br/>
 * 时间:2017-8-14下午2:59:12<br/>
 * 功能: ftp 工具类 <br/>
 */
public class FtpClientUtil {

	@Test
	public void testlistFiles() throws IOException {
		FTPClient ftpClient = new FTPClient();
		ftpClient.connect("localhost");
		boolean login = ftpClient.login("sanri", "123");
		FTPFile[] ftpFiles = ftpClient.listFiles("/a/n/c/");
		for (FTPFile ftpFile : ftpFiles) {
			System.out.println(ftpFile);
		}

		ftpClient.disconnect();
	}

	@Test
	public void testGetFile() throws IOException {
		FTPClient ftpClient = new FTPClient();
		ftpClient.connect("localhost");
		boolean login = ftpClient.login("sanri", "123");
		InputStream inputStream = ftpClient.retrieveFileStream("/a/n/c/mm.mp4");
		FileOutputStream fileOutputStream = new FileOutputStream("d:/test/a.mp4");
		IOUtils.copy(inputStream,fileOutputStream);
		fileOutputStream.close();
		ftpClient.disconnect();
	}

	/**
	 * 测试文件上传
	 * @throws IOException
	 */
	@Test
	public void testStoreFile() throws IOException {
		FTPClient ftpClient = new FTPClient();
		ftpClient.connect("localhost");
		boolean login = ftpClient.login("sanri", "123");
		if (login){
			File file = new File("C:\\Users\\091795960\\Desktop/168aa273ab7d1a331c0350f460095b26.mp4");

			//循环创建路径，并添加文件
			File target = new File("a/n/c/mm.mp4");
			Path path = target.getParentFile().toPath();
			Iterator<Path> iterator = path.iterator();
			StringBuffer root = new StringBuffer("");
			while (iterator.hasNext()){
				Path next = iterator.next();
				root.append("/").append(next);

				//尝试切入目录
				boolean success = ftpClient.changeWorkingDirectory(root.toString());
				if(!success){
					ftpClient.makeDirectory(next.toString());
					ftpClient.changeWorkingDirectory(root.toString());
				}
			}
			System.out.println(ftpClient.printWorkingDirectory());
			ftpClient.storeFile(target.getName(),FileUtils.openInputStream(file));
		}
	}
}
