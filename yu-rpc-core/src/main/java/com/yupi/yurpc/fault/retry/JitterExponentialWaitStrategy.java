package com.yupi.yurpc.fault.retry;

import com.github.rholder.retry.Attempt;
import com.github.rholder.retry.WaitStrategy;

import java.util.Random;

/**
 * 随机抖动指数退避等待策略
 * 参考 gRPC 的重试算法：
 * Sleep = min(Max, Previous * Multiplier) * (1 ± Random(Jitter))
 */
public class JitterExponentialWaitStrategy implements WaitStrategy {

    private final long initialIntervalMillis;
    private final long maxIntervalMillis;
    private final double multiplier;
    private final double jitter;
    private final Random random;

    /**
     * 构造函数
     *
     * @param initialIntervalMillis 初始间隔时间（ms）
     * @param maxIntervalMillis     最大间隔时间（ms）
     * @param multiplier            乘数 (例如 1.5 或 2.0)
     * @param jitter                随机抖动因子 (例如 0.2 表示上下浮动 20%)
     */
    public JitterExponentialWaitStrategy(long initialIntervalMillis,
                                         long maxIntervalMillis,
                                         double multiplier,
                                         double jitter) {
        this.initialIntervalMillis = initialIntervalMillis;
        this.maxIntervalMillis = maxIntervalMillis;
        this.multiplier = multiplier;
        this.jitter = jitter;
        this.random = new Random();
    }

    @Override
    public long computeSleepTime(Attempt failedAttempt) {
        long attemptNumber = failedAttempt.getAttemptNumber();

        // 1.计算基础指数等待时间：initial * (multiplier ^ (n-1))
        double computedInterval = initialIntervalMillis * Math.pow(multiplier, attemptNumber - 1);

        // 2.限制最大值
        computedInterval = Math.min(maxIntervalMillis, computedInterval);

        // 3.引入抖动（Jitter）
        // 计算抖动范围：[-jitter * interval, +jitter * interval]
        // 例如 interval=1000, jitter=0.2 => 范围 [-200, 200]
        double jitterRange = computedInterval * jitter;

        // 生成随机抖动值：random.nextDouble() 生成 [0.0, 1.0)
        // 转换逻辑：(random * 2 * jitterRange) - jitterRange
        // random == 1.0 时，randomJitter = jitterRange; random == 0.0 时，randomJitter = -jitterRange;
        double randomJitter = (random.nextDouble() * 2 * jitterRange) - jitterRange;

        // 4.最终时间 = 基础时间 + 随机抖动
        long finalWaitTime = (long) (computedInterval + randomJitter);

        // 确保时间不小于 0
        return Math.max(0, finalWaitTime);
    }
}
