package rpc.loadBalance;

import rpc.common.myURL;

import java.util.List;

public class LoadBalance {

    private static int currentIndex = 0;

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
}
