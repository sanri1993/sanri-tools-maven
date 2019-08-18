package com.sanri.algorithm;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * 
 * 创建时间:2016-9-29下午8:58:23<br/>
 * 创建者:sanri<br/>
 * 功能:算法来自 http://www.zuidaima.com/share/2701646695058432.htm<br/>
 */
public class RoateNum {
	private static Log logger = LogFactory.getLog(RoateNum.class);
	
	int [][] array = {{1,2,3},{4,5,6},{7,8,9}};
//	static int [][] target = {{4,6,1},{9,2,3},{5,7,8}};
//	static int [][] target = {{9,4,7},{8,5,2},{3,6,1}};
	int [][] target = {{1,5,3},{4,2,6},{7,8,9}};
	/**
	 * 正旋
	 * @param row
	 * @param col
	 * @return 
	 */
	public int[][] r(int [][] arr,int row,int col){
		int temp = arr[row + 1][col];
		arr[row + 1][col] = arr[row + 1][col + 1];
		arr[row + 1][col + 1] = arr[row][col + 1];
		arr[row][col + 1] = arr[row][col];
		arr[row][col] = temp;
		return arr;
	}
	
	/**
	 * 逆转
	 * @param row
	 * @param col
	 * @return 
	 */
	public int[][] R(int [][] arr,int row,int col){

		int temp = arr[row][col];
		arr[row][col] = arr[row][col + 1];
		arr[row][col + 1] = arr[row + 1][col + 1];
		arr[row + 1][col + 1] = arr[row + 1][col];
		arr[row + 1][col] = temp;
		
		return arr;
	}

	public int [][] copyArray(){
		int [][] arr = new int [3][3];
		for (int i = 0; i < arr.length; i++) {
			for (int j = 0; j < arr[i].length; j++) {
				arr[i][j] = array[i][j];
			}
		}
		return arr;
	}
	public static void print(int [][] arr){
		for (int i = 0; i < arr.length; i++) {
			for (int j = 0; j < arr[i].length; j++) {
				System.out.print(arr[i][j] + " ");
			}
			System.out.println();
		}
	}
	
	static int [] command = {0,1,2,3,4,5,6,7};//八个命令,分别是 正转 1,2,3,4 反转 1,2,3,4
	
	/**
	 * 解释命令字符串
	 */
	public String parse(String strCmd){
		if(StringUtils.isBlank(strCmd)){
			return "";
		}
		String[] split = strCmd.split("");
		String point = "", method = "";
		StringBuffer sb = new StringBuffer();
		for(int i=1;i<split.length;i++){
			int cmd = Integer.parseInt(split[i]);
			if(cmd >=4){
				method = " 反转 ";
			}else{
				method = " 正转 ";
			}
			point = String.valueOf((cmd % 4 ) + 1);
			
//			System.out.println(method + point);
			sb.append(method + point + "\n");
		}
		return sb.toString();
	}
	
	/**
	 * 执行命令字符串,num
	 * @param cmds
	 * @return
	 */
	public int [][] execCmdNum(String cmdStr){
		int[][] copyArray = copyArray();
		if("".equals(cmdStr)){
			return copyArray;
		}
		String[] split = cmdStr.split("");
		for (int i = 1; i < split.length; i++) {
			int cmd = Integer.parseInt(split[i]);
			switch (cmd) {
			case 0:
				copyArray = this.r(copyArray, 0, 0);
				break;
			case 1:
				copyArray = this.r(copyArray, 0, 1);
				break;
			case 2:
				copyArray = this.r(copyArray, 1, 0);
				break;
			case 3:
				copyArray = this.r(copyArray, 1, 1);
				break;
			case 4:
				copyArray = this.R(copyArray, 0, 0);
				break;
			case 5:
				copyArray = this.R(copyArray, 0, 1);
				break;
			case 6:
				copyArray = this.R(copyArray, 1, 0);
				break;
			case 7:
				copyArray = this.R(copyArray, 1, 1);
				break;
			}
		}
		return copyArray;
	}
	
//	public static void main(String[] args) {
//		T t = new T();
////		int[][] execCmd = t.execCmdNum("0716");
////		t.print(execCmd);
//		Map<String,int [][]> resList = new HashMap<String,int [][]>();
//		for(int i=0;i<8;i++){
//			List<String> cmds = startCmds(i);
//			for (String cmd : cmds) {
//				int[][] execCmdNum = t.execCmdNum(cmd);
//				if (t.checkEqual(execCmdNum)) {
//					System.out.println("成功  :"+cmd);
//				}
//				resList.put(cmd, execCmdNum);
//			}
//		}
//		Set<String> keySet = resList.keySet();
//		for (String cmd : keySet) {
//			System.out.println("成功 :"+cmd);
//		}
//	}
	
//	public static void main(String[] args) {
//		T ttt =  new T();
//		List<int[][]> allResult = new ArrayList<int[][]>();
//		List<String> cmds = new ArrayList<String>();
//		for(int i=0;i<7;i++){ //从 0 到 7 分别代表 正转0 90 180 270 反转 90 180 270 度 i,j,k,l 代表四个点
//			for(int j=0;j<7;j++){
//				for(int k=0;k<7;k++){
//					for(int l=0;l<7;l++){
//						
//						for (int mi = 0; mi < 4; mi++) {
//							for (int mj = 0; mj < 4; mj++) {
//								for (int mk = 0; mk < 4; mk++) {
//									for (int ml = 0; ml < 4; ml++) {
//										if(mi == mj || mi == mk || mi == ml || mj== mk || mj == ml || mk == ml){
//											continue;
//										}
//										// 24 种 
//										
//										String totalCmd = "";
//										if(i != 0){
//											totalCmd += oneCmd(i,mi);
//										}
//										if(j != 0){
//											totalCmd +=oneCmd(j, mj);
//										}
//										if(k != 0){
//											totalCmd +=oneCmd(k, mk);
//										}
//										if(l != 0){
//											totalCmd +=oneCmd(l, ml);
//										}
//										if(cmds.contains(totalCmd)){continue;}
//										int[][] execCmdNum = ttt.execCmdNum(totalCmd);
//										cmds.add(totalCmd);
//										allResult.add(execCmdNum);
//									}
//								}
//							}
//						}
//					}	
//				}	
//			}	
//		}
//		for (int oo = 0;oo <allResult.size();oo++) {
//			int[][] is  =  allResult.get(oo);
//			if(checkEqual(is)){
//				System.out.println("成功匹配:"+cmds.get(oo));
//			}
//		}
////		for (String string : cmds) {
//////			System.out.println(string);
////			logger.error(string);
////		}
//	}
	
	//{0,1,2,3,4,5,6,7};//八个命令,分别是 正转 1,2,3,4 反转 1,2,3,4
	public static void main(String[] args) {
		RoateNum t = new RoateNum();
		print(t.array);
		int[][] execCmdNum = t.execCmdNum("7744");
//		System.out.println("----------------");
//		print(execCmdNum);
		execCmdNum = t.execCmdNum("07114310016452025006103145471202456");
		System.out.println("--------------------");
		print(execCmdNum);
//		String parse = t.parse("07114310016452025006103145471202456");
//		System.out.println("07114310016452025006103145471202456".length());
//		System.out.println(parse);
	}
	
//	public static void main(String[] args) {
//		T  t = new T();
//		StringBuffer sb = new StringBuffer();
//		sb.append(t.swapCmd(0,0,2,2))
//		.append(t.swapCmd(0, 1, 1, 0))
//		.append(t.swapCmd(0, 2, 2, 0))
//		.append(t.swapCmd(1, 0, 2, 1))
//		.append(t.swapCmd(1, 2, 2, 1));
//		
//		System.out.println(sb.toString());
//	}
	
	//正反转哪个点的命令是什么 
	/**
	 * 
	 * @param i 1~3 代表正转 一次二次三次 4~6 代表反转 一次二次三次
	 * @param which 0 1 2 3 分别代表四个点
	 * @see command : int[] {0,1,2,3,4,5,6,7};//八个命令,分别是 正转 1,2,3,4 反转 1,2,3,4
	 * @return
	 */
	private static String oneCmd(int i,int which) {
		StringBuffer cmd = new StringBuffer();
		if(i <= 3){
			//正转 
			for (int m=1;m<=i;m++) {
				cmd.append(which);
			}
		}else{
			//反转
			for(int m=4;m<=i;m++){
				cmd.append(which + 4);
			}
		}
		return cmd.toString();
	}

	private static List<String> startCmds(int i) {
		List<String> cmds = new ArrayList<String>();
		if(i == 0){
			for (int j = 0; j < command.length; j++) {
				cmds.add(String.valueOf(command[j]));
			}
		}else{
			List<String> startCmds = startCmds(i - 1);
			for (String pre : startCmds) {
				for (int j = 0; j < command.length; j++) {
					cmds.add(pre + command[j]);
				}
			}
		}
		return cmds;
	}

	public boolean checkEqual(int [][] res){
		for (int i = 0; i < res.length; i++) {
			int [] tar = target[i];
			for (int j = 0; j < res[i].length; j++) {
				if(tar[j] != res[i][j]){
					return false;
				}
			}
		}
		return true;
	}
	
	public boolean checkEqual(int [][] res,int [][]target){
		for (int i = 0; i < res.length; i++) {
			int [] tar = target[i];
			for (int j = 0; j < res[i].length; j++) {
				if(tar[j] != res[i][j]){
					return false;
				}
			}
		}
		return true;
	}
	
	public String swapCmd(int si,int sj,int di,int dj){
		int [][] target = copyArray();
		int temp = target[si][sj];
		target[si][sj] = target[di][dj];
		target[di][dj] = temp;
		String resCmd = "";
		s:for(int i=0;i<8;i++){
			List<String> cmds = startCmds(i);
			for (String cmd : cmds) {
				int[][] execCmdNum = execCmdNum(cmd);
				if (checkEqual(execCmdNum,target)) {
//					System.out.println("成功  :"+cmd);
					resCmd = cmd;
					break s;
				}
			}
		}
		return resCmd;
	}
}
