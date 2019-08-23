package com.sanri.algorithm;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.commons.lang3.RandomUtils;
import org.junit.Before;
import org.junit.Test;


import java.util.*;

/**
 * 本类实现从 1000万个数中查找出前 100 个最大的数
 */
public class Top100 {

    List<Integer> generateNums = new ArrayList<Integer>();

    @Before
    public void before(){
        StopWatch stopWatch = new StopWatch();stopWatch.start();
        for (int i=0;i<10000000;i++){
            generateNums.add(RandomUtils.nextInt(0,Integer.MAX_VALUE));
        }
        System.out.println("生成 10000000 数字用时:"+stopWatch.getTime());
        stopWatch.stop();
    }

    @Test
    public void testFastSort(){
        StopWatch stopWatch = new StopWatch();stopWatch.start();
        Collections.sort(generateNums);
        List<Integer> top100 = new ArrayList<Integer>();
        for (Integer generateNum : generateNums) {
            top100.add(generateNum);
        }
        stopWatch.stop();
        System.out.println("选出前 100 个最大的数用时:"+stopWatch.getTime() + " ms ");
    }

    @Test
    public void testFirst(){
        StopWatch stopWatch = new StopWatch();stopWatch.start();
        List<Integer> top100 = new ArrayList<Integer>();
        boolean first100 = true;
        for (int i = 0; i < generateNums.size(); i++) {
            if(top100.size() < 100){
                top100.add(generateNums.get(i));
            }else if(top100.size() == 100 && first100){
                Collections.sort(top100);       //最小的在第一个
                first100 =  false;
            }else{
                if(generateNums.get(i) > top100.get(0)){
                    top100.set(0,generateNums.get(i));
                    Collections.sort(top100);
                }
            }
        }
        System.out.println("最大的 100 个数:"+top100);
        stopWatch.stop();
        System.out.println("选出前 100 个最大的数用时:"+stopWatch.getTime() + " ms ");
    }
}
