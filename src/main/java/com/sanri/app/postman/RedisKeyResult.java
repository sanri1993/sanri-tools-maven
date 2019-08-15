package com.sanri.app.postman;

public class RedisKeyResult {
    private String key;
    private String type;
    //过期时间秒值
    private long ttl;
    //过期时间毫秒值
    private long pttl;
    private long length;

    public RedisKeyResult() {
    }

    public RedisKeyResult(String key, String type) {
        this.key = key;
        this.type = type;
    }

    public RedisKeyResult(String key, String type, long ttl, long pttl) {
        this.key = key;
        this.type = type;
        this.ttl = ttl;
        this.pttl = pttl;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public long getTtl() {
        return ttl;
    }

    public void setTtl(long ttl) {
        this.ttl = ttl;
    }

    public long getPttl() {
        return pttl;
    }

    public void setPttl(long pttl) {
        this.pttl = pttl;
    }

    public long getLength() {
        return length;
    }

    public void setLength(long length) {
        this.length = length;
    }
}
