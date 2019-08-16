package com.sanri.app.jsoup.novel;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

/**
 * 创建人     : sanri
 * 创建时间   : 2018/11/16-21:15
 * 功能       : 小说实体类
 */
public class Novel {
    private String name;
    private String bookId;
    // 小说类型
    private String category;
    private String author;
    private String logo;

    //最新章节地址 & 最新章节标题 & 最新更新时间
    private String lastChapter;
    private String lastChapterTitle;
    private String lastUpdateTime;
    //小说介绍或地新章节
    private String introduce;

    // 小说详情地址
    private String chapterUrl;
    private String netSource;

    public Novel() {
    }

    public Novel(String name, String chapterUrl) {
        this.name = name;
        this.chapterUrl = chapterUrl;
    }

    public String getNetSource() {
        return netSource;
    }

    public void setNetSource(String netSource) {
        this.netSource = netSource;
    }

    public String getBookId() {
        return bookId;
    }

    public void setBookId(String bookId) {
        this.bookId = bookId;
    }

    public String getChapterUrl() {
        return chapterUrl;
    }

    public void setChapterUrl(String chapterUrl) {
        this.chapterUrl = chapterUrl;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getLogo() {
        return logo;
    }

    public void setLogo(String logo) {
        this.logo = logo;
    }

    public String getIntroduce() {
        return introduce;
    }

    public void setIntroduce(String introduce) {
        this.introduce = introduce;
    }

    public String getLastChapter() {
        return lastChapter;
    }

    public void setLastChapter(String lastChapter) {
        this.lastChapter = lastChapter;
    }

    public String getLastChapterTitle() {
        return lastChapterTitle;
    }

    public void setLastChapterTitle(String lastChapterTitle) {
        this.lastChapterTitle = lastChapterTitle;
    }

    public String getLastUpdateTime() {
        return lastUpdateTime;
    }

    public void setLastUpdateTime(String lastUpdateTime) {
        this.lastUpdateTime = lastUpdateTime;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }

    @Override
    public boolean equals(Object obj) {
        if(obj == null){
            return false;
        }

        if(!(obj instanceof  Novel)){
            return false;
        }

        Novel other = (Novel) obj;
        return other.chapterUrl.equalsIgnoreCase(this.chapterUrl);
    }
}
