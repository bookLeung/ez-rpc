package com.yupi.yurpc.fault.retry;

import com.yupi.yurpc.spi.SpiLoader;

/**
 * 重试策略工厂（用于获取重试器对象）
 */
public class RetryStrategyFactory {

    static {
        SpiLoader.load(RetryStrategy.class);
    }

    /**
     * 默认重试策略
     */
    private static final RetryStrategy DEFAULT_RetryStrategy = new NoRetryStrategy();

    /**
     * 获取重试策略实例
     *
     * @param key 重试策略类型
     * @return 重试策略实例
     */
    public static RetryStrategy getInstance(String key) {
        return SpiLoader.getInstance(RetryStrategy.class, key);
    }
}
