package com.yupi.yurpc.fault.retry;

import com.github.rholder.retry.*;
import com.yupi.yurpc.RpcApplication;
import com.yupi.yurpc.config.RpcConfig;
import com.yupi.yurpc.model.RpcResponse;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

/**
 * 固定间隔重试策略
 */
@Slf4j
public class FixedIntervalRetryStrategy implements RetryStrategy{

    /**
     * 执行重试
     *
     * @param callable
     * @return
     */
    @Override
    public RpcResponse doRetry(Callable<RpcResponse> callable) throws Exception {
        RpcConfig.RetryConfig retryConfig = RpcApplication.getRpcConfig().getRetryConfig();
        // 利用 guava 的 retryer
        Retryer<RpcResponse> retryer = RetryerBuilder.<RpcResponse>newBuilder()
                .retryIfExceptionOfType(Exception.class)
                .withWaitStrategy(WaitStrategies.fixedWait(retryConfig.getInitialInterval(), TimeUnit.MILLISECONDS))
                .withStopStrategy(StopStrategies.stopAfterAttempt(retryConfig.getMaxAttempts()))
                .withRetryListener(new RetryListener() {
                    @Override
                    public <V> void onRetry(Attempt<V> attempt) {
                        // 第一次也算重试
                        log.info("第{}次重试", attempt.getAttemptNumber());
                    }
                })
                .build();
        // Retryer 会接管你的 callable（即 RPC 请求逻辑）。
        // 它会自动在一个 do-while 循环中执行你的代码，如果失败了就按上面的策略 sleep，如果次数超了就 throw
        return retryer.call(callable);
    }
}
