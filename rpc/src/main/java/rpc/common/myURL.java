package rpc.common;

import java.io.Serializable;

public class myURL implements Serializable {
    private static final long serialVersionUID = 1L;


    private String hostName;
    private Integer port;

    public String getHostName() {
        return hostName;
    }

    public void setHostName(String hostName) {
        this.hostName = hostName;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public myURL(String hostName, Integer port) {
        this.hostName = hostName;
        this.port = port;
    }

    // 新增的构造函数，用于从字符串中解析出 hostName 和 port
    public myURL(String urlString) {
        // 假设 URL 字符串格式为 "hostname:port"
        String[] parts = urlString.split(":");
        if (parts.length == 2) {
            this.hostName = parts[0];
            this.port = Integer.parseInt(parts[1]);
        } else {
            throw new IllegalArgumentException("Invalid URL format. Expected format: hostname:port");
        }
    }

    @Override
    public String toString() {
        return hostName + ":" + port;
    }
}
