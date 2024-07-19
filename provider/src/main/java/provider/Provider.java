package provider;

import api.common.myURL;
import provider.http.HttpServer;
import provider.register.LocalRegister;
import api.HelloService;
import org.apache.commons.io.IOUtils;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.net.URL;
import java.util.Timer;
import java.util.TimerTask;

public class Provider {
    public static void main(String[] args) {
        String ip = "0.0.0.0";
        int port = 1000;

        // 解析命令行参数
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "-h":
                    printHelp();
                    return;
                case "-l":
                    if (i + 1 < args.length) {
                        ip = args[i + 1];
                    } else {
                        System.out.println("Error: -l option requires an IP address.");
                        return;
                    }
                    i++;
                    break;
                case "-p":
                    if (i + 1 < args.length) {
                        try {
                            port = Integer.parseInt(args[i + 1]);
                        } catch (NumberFormatException e) {
                            System.out.println("Error: Invalid port number.");
                            return;
                        }
                    } else {
                        System.out.println("Error: -p option requires a port number.");
                        return;
                    }
                    i++;
                    break;
                default:
                    System.out.println("Error: Unknown option " + args[i]);
                    return;
            }
        }

        if (port == -1) {
            System.out.println("Error: Port number is required.");
            return;
        }

        // 配置注册中心地址
        String registryIp = "localhost"; // 注册中心所在主机的 IP
        int registryPort = 3099; // 注册中心的端口

        String interfaceName = "api.HelloService";

        // 本地注册 注册名字-类实现
        LocalRegister.register(HelloService.class, HelloServiceImpl.class);

        // 远程注册 注册名字-服务端url
        myURL serviceMyUrl1 = new myURL(ip, port);

        registerInterfaceToURL(registryIp, registryPort, interfaceName, serviceMyUrl1);

        // 启动心跳检测
        startHeartbeat(registryIp, registryPort, interfaceName, serviceMyUrl1);

        // 启动Http服务器
        HttpServer httpServer = new HttpServer();
        httpServer.start(ip, port);
    }

    private static void printHelp() {
        System.out.println("Usage: java Provider [options]");
        System.out.println("Options:");
        System.out.println("  -h           Show this help message");
        System.out.println("  -l <ip>      Specify the IP address to listen on (default is 0.0.0.0)");
        System.out.println("  -p <port>    Specify the port to listen on (required)");
    }

    private static void registerInterfaceToURL(String registryIp, int registryPort, String interfaceName, myURL serviceMyUrl) {
        try {
            URL Url = new URL("http://" + registryIp + ":" + registryPort + "/registerInterfaceToURL");
            HttpURLConnection conn = (HttpURLConnection) Url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json");

            String payload = interfaceName + "|" + serviceMyUrl.toString();
            try (OutputStream os = conn.getOutputStream()) {
                IOUtils.write(payload, os, StandardCharsets.UTF_8);
                os.flush();
            }

            if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                System.out.println("注册接口URL成功：" + interfaceName + " -> " + serviceMyUrl);
            } else {
                System.out.println("注册接口URL失败：" + conn.getResponseMessage());
            }

            conn.disconnect();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void startHeartbeat(String registryIp, int registryPort, String interfaceName, myURL serviceMyUrl) {
        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                try {
                    URL url = new URL("http://" + registryIp + ":" + registryPort + "/heartbeat");
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("POST");
                    conn.setDoOutput(true);
                    conn.setRequestProperty("Content-Type", "application/json");

                    String payload = interfaceName + "|" + serviceMyUrl.toString();
                    try (OutputStream os = conn.getOutputStream()) {
                        IOUtils.write(payload, os, StandardCharsets.UTF_8);
                        os.flush();
                    }

                    if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                        System.out.println("心跳发送成功：" + interfaceName + " -> " + serviceMyUrl);
                    } else {
                        System.out.println("心跳发送失败：" + conn.getResponseMessage());
                    }

                    conn.disconnect();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }, 0, 5000); // 每5秒发送一次心跳
    }
}
