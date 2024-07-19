package consumer;

import api.common.Invocation;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.concurrent.TimeoutException;

public class HttpClient {

    private static final int CONNECT_TIMEOUT = 5000; // 连接超时设置为5秒
    private static final int READ_TIMEOUT = 5000; // 读取超时设置为5秒

    public static String send(String hostname, Integer port, Invocation invocation) throws IOException, TimeoutException {
        try {
            URI uri = new URI("http", null, hostname, port, "/", null, null);
            URL url = uri.toURL();
            HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();

            httpURLConnection.setRequestMethod("POST");
            httpURLConnection.setDoOutput(true);
            httpURLConnection.setRequestProperty("Content-Type", "application/octet-stream");
            httpURLConnection.setConnectTimeout(CONNECT_TIMEOUT);
            httpURLConnection.setReadTimeout(READ_TIMEOUT);

            try (OutputStream outputStream = httpURLConnection.getOutputStream();
                 ObjectOutputStream oos = new ObjectOutputStream(outputStream)) {
                oos.writeObject(invocation);
                oos.flush();
            } catch (IOException e) {
                throw new IOException("发送请求时出现错误", e);
            }

            try (InputStream inputStream = httpURLConnection.getInputStream()) {
                return IOUtils.toString(inputStream);
            } catch (IOException e) {
                throw new IOException("接收响应时出现错误", e);
            }
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("请求处理过程中出现错误", e);
        }
    }
}
