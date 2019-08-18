package com.sanri.algorithm;

import java.util.Random;

import org.apache.commons.lang3.ArrayUtils;
/**
 * 
 * 创建时间:2016-10-2上午9:02:56<br/>
 * 创建者:sanri<br/>
 * 功能:发红包功能,给的钱是 5 的倍数,每个人分得的钱也是 5 的倍数,然后每个人得到的钱是随机的<br/>
 * 算法可以做下改进,就是不是每个每个人排除获取红包,而是每个人来抽红包,这样会更加均匀点,不至于到最后的人都只有 五块<br/>
 * 改进2:每个人分得的随机数是在平均数范围浮动,可采用先平方加浮动再开方来使其更均匀点;注:这个还没做
 */
public class Thb2 {
	private int allmoney;
	private int peoples;
	private static final int MIN = 5;
	private static final int MAX = 500;
	private int groups;
	
	public Thb2(int allmoney,int peoples){
		this.allmoney = allmoney;
		this.peoples = peoples;
		this.groups = allmoney / 5;
		
		if(this.groups < this.peoples){
			throw new RuntimeException("不能分钱"); 
		}
	}
	Random random = new Random();
	public int [] split(){
		int [] peoplesMoney = new int[peoples];
		if(this.groups == this.peoples){
			for (int i = 0; i < peoplesMoney.length; i++) {
				peoplesMoney[i] = 5;
			}
			return peoplesMoney;
		}
		int sum = 0;
		for (int i=0;i<peoples;i++) {
			//先算一下当前人最大可以分到多少组
			int groups = (this.allmoney - sum - (peoples - (i+1)) * 5)/5;
			int rand = 0;
			while(rand < MIN / 5 || rand > MAX / 5){
				rand = random.nextInt(groups) + 1;
			}
			
			peoplesMoney [i] = rand * 5;
			sum += peoplesMoney[i];
		}
		return peoplesMoney;
	}
	
	public static void main(String[] args) {
		Thb2 thb2 = new Thb2(25, 4);
		int[] split = thb2.split();
		System.out.println(ArrayUtils.toString(split));
	}
}
