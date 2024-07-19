package consumer;

import api.common.myURL;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;

public class consumer {
    public static void main(String[] args) {
        // 检查命令行参数
        if (args.length == 0) {
            printHelp();
            System.exit(0);
        }

        String registryIp = null;
        int registryPort = -1;
        String loadBalanceMethod = "roundRobin"; // 默认负载均衡方法

        // 解析命令行参数
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "-h":
                    printHelp();
                    System.exit(0);
                    break;
                case "-i":
                    if (i + 1 < args.length) {
                        registryIp = args[++i];
                    } else {
                        System.out.println("错误: -i 参数需要一个 IP 地址");
                        System.exit(1);
                    }
                    break;
                case "-p":
                    if (i + 1 < args.length) {
                        try {
                            registryPort = Integer.parseInt(args[++i]);
                        } catch (NumberFormatException e) {
                            System.out.println("错误: -p 参数需要一个有效的端口号");
                            System.exit(1);
                        }
                    } else {
                        System.out.println("错误: -p 参数需要一个端口号");
                        System.exit(1);
                    }
                    break;
                case "-lb":
                    if (i + 1 < args.length) {
                        loadBalanceMethod = args[++i];
                    } else {
                        System.out.println("错误: -lb 参数需要一个负载均衡方法");
                        System.exit(1);
                    }
                    break;
                default:
                    System.out.println("未知参数: " + args[i]);
                    printHelp();
                    System.exit(1);
                    break;
            }
        }

        // 检查必要的参数是否已提供
        if (registryIp == null || registryPort == -1) {
            System.out.println("错误: 必须提供 -i 和 -p 参数");
            printHelp();
            System.exit(1);
        }

        // 服务接口类名
        String serviceClassName = "api.HelloService"; // 服务接口的类名

        // 从注册中心获取服务的 URL
        myURL serviceUrl = getServiceURLFromRegistry(registryIp, registryPort, serviceClassName, loadBalanceMethod);

        if (serviceUrl == null) {
            System.out.println("未找到可用的服务 URL");
            return;
        }

        System.out.println("从注册中心获取到服务 URL: " + serviceUrl);

        // 获取代理对象
        api.HelloService helloService = ProxyFactory.getProxy(api.HelloService.class, serviceUrl);

        // 调用方法
        try {
            String result = helloService.sayHello("hello");
            System.out.println(result);
        } catch (Exception e) {
            System.out.println("调用服务方法时发生错误: " + e.getMessage());
            e.printStackTrace();
        }

        System.exit(0);
    }

    private static myURL getServiceURLFromRegistry(String registryIp, int registryPort, String interfaceClass, String loadBalanceMethod) {
        try {
            // 构建注册中心的URL
            String registryUrl = "http://" + registryIp + ":" + registryPort + "/geturl";
            URI uri = new URI(registryUrl);
            java.net.URL url = uri.toURL();

            // 打开连接
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json");

            // 设置请求体
            String payload = "{\"" + loadBalanceMethod + "\": \"" + interfaceClass + "\"}";
            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = payload.getBytes("utf-8");
                os.write(input, 0, input.length);
                System.out.println("发送请求到注册中心，接口类名: " + interfaceClass);
            }

            // 获取响应
            int status = connection.getResponseCode();
            if (status == 200) {
                try (BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream(), "utf-8"))) {
                    StringBuilder response = new StringBuilder();
                    String responseLine;
                    while ((responseLine = br.readLine()) != null) {
                        response.append(responseLine.trim());
                    }
                    // 解析响应中的 URL 字符串并返回自定义的 URL 对象
                    System.out.println("从注册中心收到响应: " + response.toString());
                    String[] parts = response.toString().split(":");
                    if (parts.length == 2) {
                        return new myURL(parts[0], Integer.parseInt(parts[1]));
                    } else {
                        System.out.println("从注册中心收到的URL格式无效");
                        throw new RuntimeException("Invalid URL format received from registry");
                    }
                }
            } else {
                System.out.println("从注册中心获取服务 URL 失败，状态码: " + status);
                throw new RuntimeException("Failed to get service URL from registry, status code: " + status);
            }
        } catch (Exception e) {
            System.out.println("与注册中心通信失败: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    // 打印帮助信息
    private static void printHelp() {
        System.out.println("Usage: consumer -i <registryIp> -p <registryPort> -lb <loadBalanceMethod>");
        System.out.println("Options:");
        System.out.println("  -h               显示帮助信息");
        System.out.println("  -i <registryIp>  注册中心的 IP 地址 (IPv4 或 IPv6)");
        System.out.println("  -p <registryPort> 注册中心的端口号");
        System.out.println("  -lb <loadBalanceMethod> 负载均衡方法 (roundRobin, random, weightedRoundRobin)");
    }
}
