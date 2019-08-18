package com.sanri.app;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.util.*;

public class ToolModel {
    private String key;
    private String title;
    private String url;
    private String logo;
    private String author;
    private String desc;
    private int totalCalls;
    private long lastCallTime;
    //用于配置在哪些环境可以访问此工具
    private Set<String> envs = new HashSet<>();

    public ToolModel(long initLastCallTime) {
        this.lastCallTime = initLastCallTime;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getLogo() {
        return logo;
    }

    public void setLogo(String logo) {
        this.logo = logo;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public int getTotalCalls() {
        return totalCalls;
    }

    public void setTotalCalls(int totalCalls) {
        this.totalCalls = totalCalls;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this,ToStringStyle.SHORT_PREFIX_STYLE);
    }

    public long getLastCallTime() {
        return lastCallTime;
    }

    public void setLastCallTime(long lastCallTime) {
        this.lastCallTime = lastCallTime;
    }

    public synchronized void visited() {
        this.lastCallTime = System.currentTimeMillis();
        this.totalCalls++;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public void addEnv(String env){
        envs.add(env);
    }

    public void configEnvs(String[] envsList) {
        this.envs = new HashSet<>(Arrays.asList(envsList));
    }

    public boolean contains(String env) {
        return envs.contains(env);
    }

    public void removeEnv(String env) {
        envs.remove(env);
    }
}
