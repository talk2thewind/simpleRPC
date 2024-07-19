package rpc.register;

import rpc.common.myURL;

import java.io.*;
import java.util.*;

public class Register {
    private static final Object lock = new Object();
    private static final Map<String, Long> heartbeatTimestamps = new HashMap<>();
    private static final long HEARTBEAT_TIMEOUT = 15000; // 15秒超时

    static {
        // 启动心跳监测线程
        new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(5000); // 每5秒检查一次
                    checkHeartbeats();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    public static void regist(String interfaceName, myURL myUrl) {
        synchronized (lock) {
            List<myURL> myUrls = getURLs(interfaceName);
            myUrls.add(myUrl);
            saveURLs(interfaceName, myUrls);
            updateHeartbeat(interfaceName, myUrl);
        }
        System.out.println("注册服务URL：" + interfaceName + " -> " + myUrl);
    }

    public static List<myURL> getURLs(String interfaceName) {
        synchronized (lock) {
            List<myURL> myUrls = loadURLs(interfaceName);
            List<myURL> validUrls = new ArrayList<>();
            long currentTime = System.currentTimeMillis();

            for (myURL url : myUrls) {
                String key = interfaceName + "|" + url.toString();
                if (heartbeatTimestamps.containsKey(key) && currentTime - heartbeatTimestamps.get(key) <= HEARTBEAT_TIMEOUT) {
                    validUrls.add(url);
                }
            }

            return validUrls;
        }
    }

    private static void saveURLs(String interfaceName, List<myURL> myUrls) {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(interfaceName + "_urls.txt"))) {
            oos.writeObject(myUrls);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static List<myURL> loadURLs(String interfaceName) {
        List<myURL> myUrls = new ArrayList<>();
        File file = new File(interfaceName + "_urls.txt");
        if (!file.exists() || file.length() == 0) {
            return myUrls;
        }
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
            myUrls = (List<myURL>) ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return myUrls;
    }

    public static void updateHeartbeat(String interfaceName, myURL myUrl) {
        synchronized (lock) {
            String key = interfaceName + "|" + myUrl.toString();
            heartbeatTimestamps.put(key, System.currentTimeMillis());
        }
    }

    private static void checkHeartbeats() {
        synchronized (lock) {
            long currentTime = System.currentTimeMillis();
            List<String> toRemove = new ArrayList<>();

            for (Map.Entry<String, Long> entry : heartbeatTimestamps.entrySet()) {
                if (currentTime - entry.getValue() > HEARTBEAT_TIMEOUT) {
                    toRemove.add(entry.getKey());
                }
            }

            for (String key : toRemove) {
                heartbeatTimestamps.remove(key);
                String[] parts = key.split("\\|");
                if (parts.length == 2) {
                    String interfaceName = parts[0];
                    myURL myUrl = new myURL(parts[1]);

                    List<myURL> myUrls = loadURLs(interfaceName);
                    myUrls.remove(myUrl);
                    saveURLs(interfaceName, myUrls);
                    System.out.println("移除超时服务URL：" + interfaceName + " -> " + myUrl);
                }
            }
        }
    }
}
