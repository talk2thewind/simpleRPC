package rpc.protocol;

import rpc.common.myURL;
import rpc.loadBalance.LoadBalance;
import rpc.register.Register;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class RegistrationServer {
    public static void main(String[] args) throws IOException {
        int port = 3099;
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);

        server.createContext("/registerInterfaceToURL", new RegisterInterfaceToURLHandler());
        server.createContext("/geturl", new GetURLHandler());
        server.createContext("/heartbeat", new HeartbeatHandler()); // 添加心跳处理端点

        server.setExecutor(null);
        server.start();
        System.out.println("HTTP 服务在端口 " + port + "上运行");
    }

    static class RegisterInterfaceToURLHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("POST".equals(exchange.getRequestMethod())) {
                InputStream is = exchange.getRequestBody();
                String requestBody = IOUtils.toString(is, StandardCharsets.UTF_8);

                String[] parts = requestBody.split("\\|");
                String interfaceName = parts[0];
                String serviceUrlStr = parts[1];

                myURL serviceMyUrl = new myURL(serviceUrlStr);

                Register.regist(interfaceName, serviceMyUrl);

                String response = "注册接口URL成功：" + interfaceName + " -> " + serviceMyUrl;
                exchange.sendResponseHeaders(200, response.length());
                OutputStream os = exchange.getResponseBody();
                os.write(response.getBytes());
                os.close();
            } else {
                exchange.sendResponseHeaders(405, -1);
            }
        }
    }

    static class GetURLHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            System.out.println("收到来自 " + exchange.getRemoteAddress() + " 的GetURL请求");

            if ("POST".equals(exchange.getRequestMethod())) {
                InputStream is = exchange.getRequestBody();
                String requestBody = IOUtils.toString(is, StandardCharsets.UTF_8);
                System.out.println("请求体内容: " + requestBody);

                String[] requestParts = requestBody.split(":");
                String interfaceClassName = requestParts[1].replace("\"", "").replace("}", "").trim();
                String loadBalanceMethod = requestParts[0].replace("\"", "").replace("{", "").trim();
                System.out.println("请求的服务接口类名: " + interfaceClassName);
                System.out.println("指定的负载均衡方法: " + loadBalanceMethod);

                List<myURL> urls = Register.getURLs(interfaceClassName);
                myURL url = null;

                switch (loadBalanceMethod) {
                    case "roundRobin":
                        url = LoadBalance.roundRobin(urls);
                        break;
                    case "random":
                        url = LoadBalance.random(urls);
                        break;
                    case "weightedRoundRobin":
                        url = LoadBalance.weightedRoundRobin(urls);
                        break;
                    default:
                        System.out.println("未知的负载均衡方法: " + loadBalanceMethod);
                        exchange.sendResponseHeaders(400, -1);
                        return;
                }

                if (url != null) {
                    String response = url.toString();
                    System.out.println("返回服务地址: " + response);
                    exchange.sendResponseHeaders(200, response.length());
                    OutputStream os = exchange.getResponseBody();
                    os.write(response.getBytes());
                    os.close();
                } else {
                    System.out.println("未找到服务接口: " + interfaceClassName);
                    exchange.sendResponseHeaders(404, -1);
                }
            } else {
                System.out.println("不支持的请求方法: " + exchange.getRequestMethod());
                exchange.sendResponseHeaders(405, -1);
            }
        }
    }


    static class HeartbeatHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("POST".equals(exchange.getRequestMethod())) {
                InputStream is = exchange.getRequestBody();
                String requestBody = IOUtils.toString(is, StandardCharsets.UTF_8);

                String[] parts = requestBody.split("\\|");
                String interfaceName = parts[0];
                String serviceUrlStr = parts[1];

                myURL serviceMyUrl = new myURL(serviceUrlStr);

                Register.updateHeartbeat(interfaceName, serviceMyUrl);

                String response = "心跳更新成功：" + interfaceName + " -> " + serviceMyUrl;
                exchange.sendResponseHeaders(200, response.length());
                OutputStream os = exchange.getResponseBody();
                os.write(response.getBytes());
                os.close();
            } else {
                exchange.sendResponseHeaders(405, -1);
            }
        }
    }
}
