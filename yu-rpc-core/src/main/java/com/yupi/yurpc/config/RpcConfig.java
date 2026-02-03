package com.yupi.yurpc.config;

import com.yupi.yurpc.fault.retry.RetryStrategyKeys;
import com.yupi.yurpc.fault.tolerant.TolerantStrategyKeys;
import com.yupi.yurpc.loadbalancer.LoadBalancerKeys;
import com.yupi.yurpc.serializer.SerializerKeys;
import lombok.Data;

/**
 * RPC 配置，附带默认值
 */
@Data
public class RpcConfig {

    /**
     * RPC服务名称
     */
    private String name = "yu-rpc";

    /**
     * RPC服务版本
     */
    private String version = "1.0";

    /**
     * 服务器主机号
     */
    private String serverHost = "localhost";

    /**
     * 服务器端口号
     */
    private Integer serverPort = 8080;

    /**
     * 是否开启模拟调用
     */
    private boolean mock = false;

    /**
     * 序列化器
     */
    private String serializer = SerializerKeys.JDK;

    /**
     * 注册中心配置
     */
    private RegistryConfig registryConfig = new RegistryConfig();

    /**
     * 负载均衡器
     */
    private String loadBalancer = LoadBalancerKeys.ROUND_ROBIN;

    /**
     * 重试策略
     */
    private String retryStrategy = RetryStrategyKeys.NO;

    /**
     * 重试参数配置
     */
    private RetryConfig retryConfig = new RetryConfig();

    /**
     * 容错策略
     */
    private String tolerantStrategy = TolerantStrategyKeys.FAIL_FAST;

    /**
     * 内部配置类：专门管理重试的参数
     */
    @Data
    public static class RetryConfig {
        /**
         * 初始重试间隔 (ms)
         */
        private long initialInterval = 1000L;

        /**
         * 最大重试间隔 (ms)
         */
        private long maxInterval = 30000L;

        /**
         * 最大重试次数
         */
        private int maxAttempts = 5;

        /**
         * 乘数 (下一轮等待时间 = 当前 * multiplier)
         */
        private double multiplier = 1.5;

        /**
         * 随机抖动因子 (0.0 - 1.0)
         */
        private double jitter = 0.2;
    }
}
