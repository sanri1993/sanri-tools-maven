package com.sanri.app.translate;

import com.sanri.app.BaseServlet;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TranslateSupport {
    static File translateDir ;
    static {
        translateDir = BaseServlet.mkConfigPath("translate");
    }

    /**
     * 获取翻译路径
     * @return
     */
    public static File getTranslateDir() {
        return translateDir;
    }

    public static File mkFile(String biz) throws IOException {
        File file = new File(translateDir, biz);
        if(!file.exists()) {
            file.createNewFile();
        }
        return file;
    }

    /**
     * 创建子目录
     * @param childs
     * @return
     */
    public static File mkChildDir(String childs){
        File file = new File(translateDir, childs);
        if(!file.exists()){
            file.mkdir();
        }
        return file;
    }

    /**
     * 读取配置
     * @param file
     * @return
     * @throws IOException
     */
    public static String readConfig(File file) throws IOException {
        return FileUtils.readFileToString(file);
    }
    public static String readConfig(String biz) throws IOException {
        return readConfig(new File(translateDir,biz));
    }

    /**
     * 读取 json 映射配置
     * @param biz
     * @return
     */
    public static Map<String,String> readConfig2Map(String biz) throws IOException {
        Map<String,String> result = new HashMap<String, String>();

        File file = new File(translateDir, biz);
        String configs = StringUtils.trim(readConfig(file));
        String[] lines = StringUtils.split("\n");
        for (String line : lines) {
            String[] keyValue = StringUtils.split(line,"=");
            if(keyValue.length == 2){
                result.put(keyValue[0],keyValue[1]);
            }
        }
        return result;
    }

    /**
     * 写入配置
     * @param biz
     * @param configs
     */
    public static void writeConfig(String biz,String configs) throws IOException {
        File file = new File(translateDir, biz);
        FileUtils.writeStringToFile(file,configs);
    }

    /**
     * 使用映射翻译的通用代码
     * @param translateCharSequence
     * @param mirror
     */
    public static void mirrorTranslate(TranslateCharSequence translateCharSequence,Map<String,String> mirror){
        Set<String> needTranslateWords = translateCharSequence.getNeedTranslateWords();
        for (String needTranslateWord : needTranslateWords) {
            String commonMirror = mirror.get(needTranslateWord);
            if (StringUtils.isNotBlank(commonMirror)) {
                translateCharSequence.addTranslate(needTranslateWord, commonMirror);
                translateCharSequence.setTranslate(true, needTranslateWord);
            }
        }
    }

    /**
     * 将翻译结果转成驼峰式
     * @param source
     * @return
     */
    public static String convert2aB(String source){
        if(StringUtils.isBlank(source)){
            return "";
        }
        String[] split = StringUtils.split(source, " ");
        StringBuffer stringBuffer = new StringBuffer(StringUtils.uncapitalize(split[0]));
        if(split.length > 1){
            for (int i = 1; i < split.length; i++) {
                stringBuffer.append(StringUtils.capitalize(split[i]));
            }
        }
        return stringBuffer.toString();
    }
}
