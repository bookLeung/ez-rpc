package com.yupi.yurpc.fault.retry;

import com.yupi.yurpc.model.RpcResponse;

import java.util.concurrent.Callable;

/**
 * 不重试 - 重试策略
 */
public class NoRetryStrategy implements RetryStrategy{

    /**
     * 执行重试 - 不重试
     *
     * @param callable
     * @return
     */
    @Override
    public RpcResponse doRetry(Callable<RpcResponse> callable) throws Exception {
        return callable.call();
    }
}
