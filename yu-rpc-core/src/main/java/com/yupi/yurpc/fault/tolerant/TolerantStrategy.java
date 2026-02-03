package com.yupi.yurpc.fault.tolerant;

import com.yupi.yurpc.model.RpcResponse;

import java.util.Map;

/**
 * 容错策略接口
 */
public interface TolerantStrategy {

    /**
     * 容错处理
     *
     * @param context 上下文，用于传递数据
     * @param e       异常
     * @return 容错处理结果
     */
    RpcResponse doTolerant(Map<String, Object> context, Exception e);
}
