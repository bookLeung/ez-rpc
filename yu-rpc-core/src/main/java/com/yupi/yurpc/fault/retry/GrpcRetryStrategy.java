package com.yupi.yurpc.fault.retry;

import com.github.rholder.retry.*;
import com.yupi.yurpc.RpcApplication;
import com.yupi.yurpc.config.RpcConfig;
import com.yupi.yurpc.model.RpcResponse;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.Callable;

/**
 * 仿 gRPC 重试策略
 */
@Slf4j
public class GrpcRetryStrategy implements RetryStrategy{
    @Override
    public RpcResponse doRetry(Callable<RpcResponse> callable) throws Exception {
        // 动态获取配置
        RpcConfig.RetryConfig retryConfig = RpcApplication.getRpcConfig().getRetryConfig();
        // 构造重试器
        Retryer<RpcResponse> retryer = RetryerBuilder.<RpcResponse>newBuilder()
                .retryIfExceptionOfType(Exception.class)
                // 使用自定义的 Jitter 策略，使用配置中的参数
                // 初始 1s，最大 30s，增长倍数 1.5， 随机抖动 20%
                .withWaitStrategy(new JitterExponentialWaitStrategy(
                        retryConfig.getInitialInterval(),
                        retryConfig.getMaxInterval(),
                        retryConfig.getMultiplier(),
                        retryConfig.getJitter()
                ))
                // 停止策略：最多试 5 次
                .withStopStrategy(StopStrategies.stopAfterAttempt(retryConfig.getMaxAttempts()))
                .withRetryListener(new RetryListener() {
                    @Override
                    public <V> void onRetry(Attempt<V> attempt) {
                        if (attempt.hasException()) {
                            Throwable e = attempt.getExceptionCause();
                            log.info("第 {} 次重试, 距离上次失败耗时 {}ms，原因：{}",
                                    attempt.getAttemptNumber(),
                                    attempt.getDelaySinceFirstAttempt(),
                                    e.getMessage(),
                                    e);
                        }
                    }
                })
                .build();
        return retryer.call(callable);
    }
}
