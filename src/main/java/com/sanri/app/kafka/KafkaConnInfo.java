package com.sanri.app.kafka;

public class KafkaConnInfo {
    private String clusterName;
    private String clusterVersion;
    private String zkConnectStrings;
    private String chroot = "/";

    private String saslMechanism = "GSSAPI";

    private String username;
    private String password;
    private String loginModel;

    public KafkaConnInfo() {
    }

    public KafkaConnInfo(String clusterName, String clusterVersion, String zkConnectStrings, String chroot, String saslMechanism) {
        this.clusterName = clusterName;
        this.clusterVersion = clusterVersion;
        this.zkConnectStrings = zkConnectStrings;
        this.chroot = chroot;
        this.saslMechanism = saslMechanism;
    }

    public KafkaConnInfo(String clusterName, String clusterVersion, String zkConnectStrings) {
        this.clusterName = clusterName;
        this.clusterVersion = clusterVersion;
        this.zkConnectStrings = zkConnectStrings;
    }

    public String getClusterName() {
        return clusterName;
    }

    public void setClusterName(String clusterName) {
        this.clusterName = clusterName;
    }

    public String getClusterVersion() {
        return clusterVersion;
    }

    public void setClusterVersion(String clusterVersion) {
        this.clusterVersion = clusterVersion;
    }

    public String getZkConnectStrings() {
        return zkConnectStrings;
    }

    public void setZkConnectStrings(String zkConnectStrings) {
        this.zkConnectStrings = zkConnectStrings;
    }

    public String getChroot() {
        return chroot;
    }

    public void setChroot(String chroot) {
        this.chroot = chroot;
    }

    public String getSaslMechanism() {
        return saslMechanism;
    }

    public void setSaslMechanism(String saslMechanism) {
        this.saslMechanism = saslMechanism;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getLoginModel() {
        return loginModel;
    }

    public void setLoginModel(String loginModel) {
        this.loginModel = loginModel;
    }
}
