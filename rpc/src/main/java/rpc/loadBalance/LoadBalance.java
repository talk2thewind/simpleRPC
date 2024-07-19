package rpc.loadBalance;

import rpc.common.myURL;

import java.util.List;
import java.util.Random;

public class LoadBalance {

    private static int currentIndex = 0;
    private static final Random random = new Random();

    // 轮询
    public static myURL roundRobin(List<myURL> myUrls) {
        synchronized (LoadBalance.class) {
            if (myUrls.isEmpty()) {
                return null;
            }
            myURL selectedURL = myUrls.get(currentIndex);
            currentIndex = (currentIndex + 1) % myUrls.size(); // 循环递增索引
            return selectedURL;
        }
    }

    // 随机选择
    public static myURL random(List<myURL> myUrls) {
        if (myUrls.isEmpty()) {
            return null;
        }
        int randomIndex = random.nextInt(myUrls.size());
        return myUrls.get(randomIndex);
    }

    // 加权轮询
    public static myURL weightedRoundRobin(List<myURL> myUrls) {
        if (myUrls.isEmpty()) {
            return null;
        }

        int totalWeight = myUrls.stream().mapToInt(myURL::getCurrentWeight).sum();

        if (totalWeight == 0) {
            // 如果总权重为0，所有权重重置为初始值
            myUrls.forEach(url -> url.setCurrentWeight(url.getInitialWeight()));
            totalWeight = myUrls.stream().mapToInt(myURL::getCurrentWeight).sum();
        }

        int randomWeight = random.nextInt(totalWeight);
        for (myURL url : myUrls) {
            randomWeight -= getWeight(url);
            if (randomWeight < 0) {
                // 选中当前URL后，减少其权重
                url.setCurrentWeight(Math.max(0, url.getCurrentWeight() - 1));
                return url;
            }
        }
        return myUrls.get(0); // 如果没有找到合适的，返回第一个
    }

    // 获取当前权重
    private static int getWeight(myURL url) {
        return url.getCurrentWeight();
    }
}
