package com.yupi.yurpc.spi;

import com.yupi.yurpc.serializer.Serializer;
import com.yupi.yurpc.serializer.JsonSerializer;
import com.yupi.yurpc.serializer.KryoSerializer;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class SpiLoaderTest {

    @Before
    public void setUp() {
        // 显式加载一下，确保 SPI 机制初始化
        SpiLoader.loadAll();
    }

    /**
     * 测试场景 1：Key 存在，正常加载系统配置
     */
    @Test
    public void testLoadSystemSpi() {
        // 请求 "json"
        Serializer serializer = SpiLoader.getInstance(Serializer.class, "json");
        System.out.println("testLoadSystemSpi: " + serializer);

        // 断言它不为空，且类型确实是 JsonSerializer
        Assert.assertNotNull(serializer);
        Assert.assertTrue(serializer instanceof JsonSerializer);
    }

    /**
     * 测试场景 2：Key 存在，正常加载自定义配置
     */
    @Test
    public void testLoadCustomSpi() {
        // 请求 "t1" (我们在 custom 目录下定义的)
        Serializer serializer = SpiLoader.getInstance(Serializer.class, "t1");
        System.out.println("testLoadCustomSpi: " + serializer);

        Assert.assertNotNull(serializer);
        Assert.assertTrue(serializer instanceof JsonSerializer);
    }

    /**
     * 测试场景 3：Key 不存在，抛出异常
     */
    @Test(expected = RuntimeException.class)
    public void testLoadNotExistentKey() {
        // 请求一个根本没定义的 key
        SpiLoader.getInstance(Serializer.class, "not_exists_key_123");
    }

    /**
     * 测试场景 4：Key 相同，自定义配置(Custom) 覆盖 系统配置(System)
     * 【核心测试点】
     */
    @Test
    public void testSystemSpiOverride() {
        // 在 system 中，jdk=JdkSerializer
        // 在 custom 中，jdk=KryoSerializer (我们故意改的)

        // 调用 loader 获取 "jdk"
        Serializer serializer = SpiLoader.getInstance(Serializer.class, "jdk");
        System.out.println("testSystemSpiOverride: " + serializer);

        // 断言：应该拿到 KryoSerializer，而不是 JdkSerializer
        Assert.assertNotNull(serializer);
        Assert.assertTrue(serializer instanceof KryoSerializer);
        Assert.assertFalse(serializer instanceof com.yupi.yurpc.serializer.JdkSerializer);
    }
}