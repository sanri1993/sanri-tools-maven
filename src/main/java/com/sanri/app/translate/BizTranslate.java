package com.sanri.app.translate;

import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class BizTranslate implements Translate {
    private String biz;
    private Map<String,String> spectBizMap ;

    public BizTranslate(String biz) {
        this.biz = biz;

        //加载业务映射
        try {
            spectBizMap = TranslateSupport.readConfig2Map(biz);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void doTranslate(TranslateCharSequence translateCharSequence, TranslateChain translateChain) {
      TranslateSupport.mirrorTranslate(translateCharSequence,spectBizMap);
        translateChain.doTranslate(translateCharSequence,translateChain);
    }

    public void setBiz(String biz) {
        this.biz = biz;
    }
}
