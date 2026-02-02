package com.yupi.yurpc.fault.retry;

import com.yupi.yurpc.model.RpcResponse;

import java.util.concurrent.Callable;

/**
 * 重试策略
 */
public interface RetryStrategy {

    /**
     * 执行重试
     *
     * @param callable
     * @return
     */
    RpcResponse doRetry(Callable<RpcResponse> callable) throws Exception;

}
