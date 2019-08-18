package com.sanri.app.jsoup.netsource;

import org.apache.commons.lang3.StringUtils;
import org.jsoup.Connection;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * 盘搜搜资源抓取
 */
public class PansosoSpider {
    //关键字搜索基础路径
    private static final String baseSearchUrl = "http://www.pansoso.com/zh/";
    //资源统计基础路径
    private static final String baseUrl = "http://www.pansoso.com";
    //搜索超时时间
    private static final int searchTimeout = 10000;
    private static final int singlePageOpenTimeout = 10000;
    //userAgent
    private static final String userAgent = "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/70.0.3538.110 Safari/537.36";

    private Logger logger = LoggerFactory.getLogger(getClass());
    /**
     * 搜索资源
     * @param keyword
     * @return
     */
    public PageResult<SourceModel> searchResource(String keyword, int pageInt) throws IOException {
        List<SourceModel> sourceModels = new ArrayList<SourceModel>();
        PageResult<SourceModel> sourceModelPageResult = new PageResult<SourceModel>(pageInt);

        String searchUrl = baseSearchUrl+keyword;
        if(pageInt != 1){
            searchUrl = baseSearchUrl+keyword+"_"+pageInt;
        }

        Document searchDocument = Jsoup.connect(searchUrl)
                .timeout(searchTimeout)
                .userAgent(userAgent)
                .get();

        Element $content = searchDocument.getElementById("content");
        Elements results = $content.select("div.pss");
        if(!results.isEmpty()){
            for (Element result : results) {
                Element $link = result.select("h2>a").get(0);
                String title = $link.text();
                String relativeLink = $link.attr("href");

                //打开统计页
                SourceModel sourceModel = new SourceModel(title);
                sourceModels.add(sourceModel);
                loadCurrentResult(relativeLink, sourceModel);
            }
        }

        //检查资源是否有效
        Iterator<SourceModel> iterator = sourceModels.iterator();
        while (iterator.hasNext()){
            SourceModel sourceModel = iterator.next();
            boolean effectiveness = checkTheEffectivenessOfResources(sourceModel);
            if(!effectiveness){
                iterator.remove();
            }
        }

        sourceModelPageResult.setData(sourceModels);

        //检测资源是否还有下一页
        Element $page = searchDocument.getElementById("sopage");
        Elements $pageList = $page.select("a");
        for (int i = 0; i < $pageList.size(); i++) {
            Element $currPage  =  $pageList.get(i);
            String pageNum = $currPage.text();
            if(pageNum.trim().equals(pageInt+"")){
                if(i < $pageList.size() - 1){
                    sourceModelPageResult.setHasNext(true);
                    break;
                }
            }
        }
        return sourceModelPageResult;
    }

    /**
     * 检查资源是否有效
     * @param sourceModel
     * @return
     */
    private boolean checkTheEffectivenessOfResources(SourceModel sourceModel) throws IOException {
        String panUrl = sourceModel.getPanUrl();
        Document document = null;
        try{
            document = Jsoup.connect(panUrl)
                    .userAgent(userAgent)
                    .timeout(singlePageOpenTimeout)
                    .get();
        }catch (Exception e){
            return false;
        }
        Element $shareNotFound = document.getElementById("share_nofound_des");
        if($shareNotFound != null){
            return false;
        }
        return true;
    }

    /**
     * 加载某一个结果
     * @param relativeLink
     * @param sourceModel
     * @return
     * @throws IOException
     */
    private SourceModel loadCurrentResult(String relativeLink, SourceModel sourceModel) throws IOException {
        String sumPage = baseUrl+relativeLink;
        Document sumDocument = Jsoup.connect(sumPage)
                .userAgent(userAgent)
                .timeout(singlePageOpenTimeout)
                .get();

        Element $con = sumDocument.getElementById("con");

        //读取文件相关属性
        Element $info = $con.select(".info").get(0);
        Elements $dds = $info.select("dd");
        String source = lookForText($dds.get(0));
        String shareMan = lookForText($dds.get(1));
        String fileSize = lookForText($dds.get(2));
        String shareTime = lookForText($dds.get(3));
        String visits = lookForText($dds.get(4));
        String recordTime = lookForText($dds.get(5));
        sourceModel.config(source,shareMan,fileSize,shareTime,visits,recordTime);

        //开始解析下载地址
        String panUrl = lookForPanUrl($con);
        sourceModel.setPanUrl(panUrl);
        return sourceModel;
    }

    /**
     * 查询百度盘地址
     * @param $con
     * @return
     * @throws IOException
     */
    private String lookForPanUrl(Element $con) throws IOException {
        Element $link = $con.select(".down").select("a[class=red]").get(0);
        String href = $link.attr("href");
        // 替换未知字符
        href = href.replace("amp;","");

        Document document = Jsoup
                .connect(href)
                .userAgent(userAgent)
                .timeout(singlePageOpenTimeout)
                .get();

        Element $linkPan = document.select("a.btn-download").get(0);
        String gotoPan = $linkPan.attr("href");

        Connection.Response response = null;
        try {
            response = Jsoup.connect(gotoPan)
                    .userAgent(userAgent)
                    .timeout(singlePageOpenTimeout)
                    .execute();
        }catch (HttpStatusException e){
            int statusCode = e.getStatusCode();
            if(statusCode != 400){
                throw e;
            }
            logger.error("调用地址:"+gotoPan+"  "+statusCode);
        }
        if(response == null){
            return "";
        }
      return  response.url().toString();
    }

    /**
     * 加载分隔文本
     * @param element
     * @return
     */
    private String lookForText(Element element) {
        String text = element.text();
        String[] split = StringUtils.split(text,"：");
        if(split.length >= 2 ){
            return split[1];
        }
        return text;
    }

    @Test
    public void test() throws IOException {
        PageResult<SourceModel> springcloud = searchResource("springcloud", 1);
        System.out.println(springcloud);

//        Document document = Jsoup.connect("https://pan.baidu.com/s/1KX9Y_pJrLh83uS0kMKppLg")
//                .userAgent(userAgent)
//                .timeout(singlePageOpenTimeout)
//                .get();
//        Element $shareNotFound = document.getElementById("share_nofound_des");
//        if($shareNotFound != null){
//            System.out.println("链接已经失效");
//        }
    }
}
