package com.yupi.yurpc.fault.retry;

/**
 * 重试策略键名常量
 */
public interface RetryStrategyKeys {

    /**
     * 不重试
     */
    String NO = "no";

    /**
     * 固定间隔重试
     */
    String FIXED_INTERVAL = "fixed_interval";

    /**
     * 仿 gRPC 重试策略
     */
    String GRPC = "grpc";
}
