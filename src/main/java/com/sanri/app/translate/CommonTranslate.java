package com.sanri.app.translate;

import com.sanri.app.BaseServlet;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Set;

/**
 * 通用翻译
 */
public class CommonTranslate implements Translate {
    public static final String COMMON = "common";

    static Map<String, String> commonBiz ;
    static {
        try {
            TranslateSupport.mkFile(COMMON);

            commonBiz = TranslateSupport.readConfig2Map(COMMON);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void doTranslate(TranslateCharSequence translateCharSequence, TranslateChain translateChain) {
        TranslateSupport.mirrorTranslate(translateCharSequence,commonBiz);
        translateChain.doTranslate(translateCharSequence,translateChain);
    }

    /**
     * 修改配置
     */
    public void writeConfigs(String configs){
        try {
            TranslateSupport.writeConfig(COMMON,configs);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
