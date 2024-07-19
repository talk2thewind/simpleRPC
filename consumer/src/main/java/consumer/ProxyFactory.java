package consumer;

import api.common.Invocation;
import api.common.myURL;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

public class ProxyFactory {

    public static <T> T getProxy(Class interfaceClass, myURL url) {
        Object proxyInstance = Proxy.newProxyInstance(interfaceClass.getClassLoader(), new Class[]{interfaceClass}, new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) {
                String mock = System.getProperty("mock");
                if (mock != null && mock.startsWith("return:")) {
                    return mock.replace("return:", "");
                }

                Invocation invocation = new Invocation(interfaceClass.getName(), method.getName(),
                        method.getParameterTypes(), args);
                HttpClient httpClient = new HttpClient();

                String result = "";
                int maxRetries = 3;
                int retryCount = 0;

                while (retryCount < maxRetries) {
                    try {
                        result = httpClient.send(url.getHostName(), url.getPort(), invocation);
                        break; // 如果成功，则跳出循环
                    } catch (Exception e) {
                        retryCount++;
                        System.out.println("Exception occurred when httpClient.send, retrying... (Retry count: " + retryCount + ")");
                        e.printStackTrace();
                    }
                }
                return result;
            }
        });
        return (T) proxyInstance;
    }
}
