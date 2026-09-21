package zju.cst.aces.runner;

import java.util.HashMap;
import java.util.Map;


//时间统计
public class Timer {
    private final Map<String, Long> startTimeMap;

    public Timer() {
        startTimeMap = new HashMap<>();
    }

    /**
     * 记录一个阶段的开始时间
     *
     * @param stage 阶段名称
     */
    public void start(String stage) {
        startTimeMap.put(stage, System.currentTimeMillis());
        System.out.println(stage + " 开始时间: " + startTimeMap.get(stage));
    }

    /**
     * 计算并返回一个阶段的耗时（秒）
     *
     * @param stage 阶段名称
     * @return 耗时（秒）
     */
    public double end(String stage) {
        Long startTime = startTimeMap.get(stage);
        if (startTime == null) {
            System.err.println("阶段 " + stage + " 未记录开始时间");
            return -1;
        }
        long endTime = System.currentTimeMillis();
        System.out.println(stage + " 结束时间: " + endTime);
        double duration = (endTime - startTime) / 1000.0;
        System.out.println(stage + " 耗时: " + duration + " 秒");
        return duration;
    }
}
