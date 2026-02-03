package com.yupi.yurpc.fault.tolerant;

import com.yupi.yurpc.spi.SpiLoader;

/**
 * 容错策略工厂（用于获取容错器对象）
 */
public class TolerantStrategyFactory {

    static {
        SpiLoader.load(TolerantStrategy.class);
    }

    /**
     * 默认容错策略
     */
    private static final TolerantStrategy DEFAULT_TolerantStrategy = new FailFastTolerantStrategy();

    /**
     * 获取容错策略实例
     *
     * @param key 容错策略类型
     * @return 容错策略实例
     */
    public static TolerantStrategy getInstance(String key) {
        return SpiLoader.getInstance(TolerantStrategy.class, key);
    }
}
