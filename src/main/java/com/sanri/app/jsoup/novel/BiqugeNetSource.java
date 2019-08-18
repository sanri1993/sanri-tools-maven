package com.sanri.app.jsoup.novel;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.Consts;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.util.EntityUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.junit.Test;
import sanri.utils.HttpUtil;

import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 创建人     : sanri
 * 创建时间   : 2018/11/16-21:26
 * 功能       :
 */
public class BiqugeNetSource extends NovelNetSource {
    static final private String source = "https://www.biqugex.com";
    @Override
    public List<Novel> search(String keyword) throws IOException {
        String sourcePhp = url+"/s.php";

        Map<String,String> params = new HashMap<String, String>();
        params.put("ie","utf-8");
//        params.put("s",System.currentTimeMillis()+"");
        params.put("siteid","biqugex.com");
        params.put("q",keyword);

        List<NameValuePair> nameValuePairs = HttpUtil.transferParam(params);
        HttpEntity urlEncodedFormEntity = new UrlEncodedFormEntity(nameValuePairs, Consts.UTF_8);
        String query = EntityUtils.toString(urlEncodedFormEntity, Consts.UTF_8);

        Document document = Jsoup.connect(sourcePhp+"?"+query)
                .userAgent("Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/70.0.3538.102 Safari/537.36")
                .timeout(5000).get();

        return parserSearchDoucment(document);
    }

    /**
     * 解析文档信息
     * @param document
     * @return
     */
    private List<Novel> parserSearchDoucment(Document document) {
        List<Novel> novels = new ArrayList<Novel>();
        Element elementById = document.getElementById("search-main");
        Elements links = elementById.getElementsByTag("a");
        for (int i=0;i<links.size();i++){
            Element $a = links.get(i);
            String text = $a.text();
            String href = $a.attr("href");

            Element $author = $a.parent().nextElementSibling();
            Novel novel = new Novel(text, href);
            novel.setAuthor($author.text());
            novels.add(novel);
        }
        return novels;
    }


    @Override
    public List<Chapter> chapterCatalog(Novel novel) throws IOException {
        String chapterUrl = novel.getChapterUrl();
        Document document = Jsoup.connect(chapterUrl).timeout(10000).get();

//        //获取书箱信息
//        Element $info = document.select(".info").get(0);
//
//        //logo
//        Element $cover = $info.select(".cover>img").get(0);
//        novel.setLogo(url+$cover.attr("src"));
//
//        Elements $mostInfo = $info.select(".small>span");
//        //分类
//        Element $classify = $mostInfo.get(1);
//        String classify = StringUtils.split($classify.text(),"：")[1];
//        novel.setCategory(classify);
//
//        //作者
//        Element $author = $mostInfo.get(0);
//        String author = StringUtils.split($classify.text(),"：")[1];
//        novel.setAuthor(author);

        Element chapterCatalogEl = document.select(".listmain").get(0);
        return parserChapterCatalog(chapterCatalogEl);
    }

    @Override
    public List<Chapter> newest10Chapter(Novel novel) throws IOException {
        return null;
    }

    static  final Pattern lastChapterPattern = Pattern.compile("第\\w+章");
    /**
     * 解析章节目录
     * @param chapterCatalogEl
     */
    private List<Chapter> parserChapterCatalog(Element chapterCatalogEl) {
        List<Chapter> chapters = new ArrayList<Chapter>();

        Elements chapterEls = chapterCatalogEl.select("a");
        Iterator<Element> iterator = chapterEls.iterator();
        int i=0;//用于排除前 6 章
        while (iterator.hasNext()){
            Element chapterEl = iterator.next();
            if(i++ < 6){
                continue;
            }

            //解析章节,标题,和路径
            String chapterAText = chapterEl.text();
            String chapterUri = chapterEl.attr("href");
            Matcher matcher = lastChapterPattern.matcher(chapterAText);
            String chapterSequence = matcher.find() ? matcher.group():"";
            String chapterTitle =  StringUtils.trim(chapterAText.substring(chapterSequence.length()));

            Chapter chapter = new Chapter();
            chapter.setSequence(chapterSequence);
            chapter.setTitle(chapterTitle);
            chapter.setUrl(source+chapterUri);

            chapters.add(chapter);
        }

        return chapters;
    }

    @Override
    public String content(Novel novel, Chapter chapter) throws IOException {
        String url = chapter.getUrl();
        Document document = Jsoup.connect(url).timeout(1000).get();
        Element contentEl = document.getElementById("content");
        String html = contentEl.html();
        return html;
    }
}
