package com.yupi.yurpc.server.tcp;

import com.yupi.yurpc.model.RpcResponse;
import com.yupi.yurpc.protocol.ProtocolMessage;
import com.yupi.yurpc.protocol.ProtocolMessageDecoder;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

/**
 * TCP 客户端处理器（用于处理响应）
 */
public class TcpClientHandler implements Handler<Buffer> {

    @Override
    public void handle(Buffer buffer) {
        // 🔍 埋点 3
//        System.out.println("【客户端】收到响应数据，长度：" + buffer.length());
        // 1. 解码响应数据
        ProtocolMessage<RpcResponse> rpcResponseProtocolMessage;
        try {
            rpcResponseProtocolMessage = (ProtocolMessage<RpcResponse>) ProtocolMessageDecoder.decode(buffer);
        } catch (IOException e) {
            throw new RuntimeException("协议消息解码错误");
        }

        // 2. 获取 RequestId
        RpcResponse rpcResponse = rpcResponseProtocolMessage.getBody();
        long requestId = rpcResponseProtocolMessage.getHeader().getRequestId();
//        System.out.println("DEBUG: 响应 ID = " + requestId);

        // 3. 【关键】去全局 Pending Map 中找到对应的 Future，并完成它
        // 这样发起请求的那个线程就会从 .get() 中苏醒，拿到结果
        // remove 表示用完即销毁，防止内存泄漏
        // 🔥🔥🔥 核心修复 🔥🔥🔥
        // 1. 从 Map 中移除并获取 Future
        CompletableFuture<RpcResponse> future = VertxTcpClient.PENDING_REQUEST_MAP.remove(requestId);

        // 2. 如果不为 null，说明主线程还在等，唤醒它
        if (future != null) {
            future.complete(rpcResponse);
        } else {
            // 这种情况可能是超时了，Future 已经被主线程的 remove 逻辑移除了
            System.out.println("DEBUG: 收到响应，但 Map 中无对应 Future，可能已超时。ID=" + requestId);
        }
    }
}