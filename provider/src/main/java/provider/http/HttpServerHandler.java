package provider.http;

import provider.register.LocalRegister;
import api.common.Invocation;
import org.apache.commons.io.IOUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.lang.reflect.Method;
import java.util.concurrent.*;

public class HttpServerHandler {

    private static final ExecutorService executorService = new ThreadPoolExecutor(
            10, // 核心线程数
            20, // 最大线程数
            60L, // 空闲线程存活时间
            TimeUnit.SECONDS, // 存活时间的单位
            new ArrayBlockingQueue<>(4) // 阻塞队列大小
    );

    private static final long TIMEOUT = 5000; // 超时时间设置为5秒

    public void handler(HttpServletRequest req, HttpServletResponse resp) {
        try {
            // 读取输入流
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            IOUtils.copy(req.getInputStream(), baos);
            byte[] requestBytes = baos.toByteArray();

            // 复制响应流
            ByteArrayOutputStream responseBaos = new ByteArrayOutputStream();

            Future<Void> future = executorService.submit(() -> {
                try (ObjectInputStream input = new ObjectInputStream(new ByteArrayInputStream(requestBytes));
                     OutputStream output = responseBaos) {
                    System.out.println("接收到请求");
                    Invocation invocation = (Invocation) input.readObject();
                    System.out.println("调用信息: " + invocation);

                    // 使用接口名称来获取实现类
                    System.out.println(invocation.getInterfaceName());
                    Class<?> classImpl = LocalRegister.get(Class.forName(invocation.getInterfaceName()));
                    System.out.println("实现类: " + classImpl);

                    // 获取方法
                    Method method = classImpl.getMethod(invocation.getMethodName(), invocation.getParameterTypes());
                    Object result = method.invoke(classImpl.getDeclaredConstructor().newInstance(), invocation.getParameters());
                    System.out.println("结果: " + result);

                    // 将结果写入响应
                    IOUtils.write(result.toString(), output, "UTF-8");

                } catch (ClassNotFoundException e) {
                    System.err.println("未找到类: " + e.getMessage());
                    throw new RuntimeException("类未找到错误", e);
                } catch (NoSuchMethodException e) {
                    System.err.println("未找到方法: " + e.getMessage());
                    throw new RuntimeException("未找到方法错误", e);
                } catch (IllegalAccessException e) {
                    System.err.println("非法访问: " + e.getMessage());
                    throw new RuntimeException("非法访问错误", e);
                } catch (InstantiationException e) {
                    System.err.println("实例化错误: " + e.getMessage());
                    throw new RuntimeException("实例化错误", e);
                } catch (IOException e) {
                    System.err.println("IO 错误: " + e.getMessage());
                    throw new RuntimeException("IO 错误", e);
                }
                return null;
            });

            try {
                // 等待任务完成，并设置超时时间
                future.get(TIMEOUT, TimeUnit.MILLISECONDS);

                // 将响应写回客户端
                IOUtils.copy(new ByteArrayInputStream(responseBaos.toByteArray()), resp.getOutputStream());

            } catch (TimeoutException e) {
                future.cancel(true); // 超时后取消任务
                System.err.println("请求超时: " + e.getMessage());
                resp.sendError(HttpServletResponse.SC_GATEWAY_TIMEOUT, "请求超时");
            } catch (ExecutionException e) {
                System.err.println("执行错误: " + e.getMessage());
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "内部服务器错误");
            } catch (InterruptedException e) {
                System.err.println("中断错误: " + e.getMessage());
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "内部服务器错误");
            }

        } catch (IOException e) {
            System.err.println("IO 错误: " + e.getMessage());
            try {
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "内部服务器错误");
            } catch (IOException ioException) {
                System.err.println("发送错误响应失败: " + ioException.getMessage());
            }
        }
    }
}
