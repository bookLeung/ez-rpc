package com.yupi.yurpc.fault.retry;

import com.yupi.yurpc.model.RpcResponse;
import org.junit.Test;

import java.util.concurrent.Callable;

/**
 * 重试策略测试
 */
public class RetryStrategyTest {

//    RetryStrategy retryStrategy = new FixedIntervalRetryStrategy();
    RetryStrategy retryStrategy = new GrpcRetryStrategy();

    @Test
    public void doRetry() {
        try {
//            RpcResponse rpcResponse = retryStrategy.doRetry(new Callable<RpcResponse>() {
//                @Override
//                public RpcResponse call() throws Exception {
//                    System.out.println("测试重试");
//                    throw new RuntimeException("模拟重试失败");
//                }
//            });
            RpcResponse rpcResponse = retryStrategy.doRetry(() -> {
                // 是上面这段的lambda表达式
                System.out.println("测试重试");
                throw new RuntimeException("模拟重试失败");
            });

            System.out.println(rpcResponse.toString());
        } catch (Exception e) {
            System.out.println("重试多次失败");
            e.printStackTrace();
        }
    }
}
