package com.yupi.yurpc.registry;

import com.yupi.yurpc.serializer.JdkSerializer;
import com.yupi.yurpc.serializer.Serializer;
import com.yupi.yurpc.spi.SpiLoader;

/**
 * 注册中心工厂，用于获取注册中心实例
 */
public class RegistryFactory {

    static {
        SpiLoader.load(Registry.class);
    }

    /**
     * 默认注册中心
     */
    private static final Registry DEFAULT_REGISTRY = new EtcdRegistry();

    /**
     * 获取注册中心实例
     *
     * @param key 注册中心类型
     * @return 注册中心实例
     */
    public static Registry getInstance(String key) {
        return SpiLoader.getInstance(Registry.class, key);
    }
}
