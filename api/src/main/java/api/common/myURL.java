package api.common;

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

    @Override
    public String toString() {
        return hostName + ":" + port;
    }
}
