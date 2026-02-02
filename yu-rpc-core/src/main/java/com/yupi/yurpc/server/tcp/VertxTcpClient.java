package com.yupi.yurpc.server.tcp;

import cn.hutool.core.util.IdUtil;
import com.yupi.yurpc.RpcApplication;
import com.yupi.yurpc.model.RpcRequest;
import com.yupi.yurpc.model.RpcResponse;
import com.yupi.yurpc.model.ServiceMetaInfo;
import com.yupi.yurpc.protocol.*;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.net.NetClient;
import io.vertx.core.net.NetSocket;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Vert.x TCP 请求客户端
 */
@Slf4j
public class VertxTcpClient {

    public static RpcResponse doRequest(RpcRequest rpcRequest, ServiceMetaInfo serviceMetaInfo) throws ExecutionException, InterruptedException {
        // 发送 TCP 请求
        // 复用 Vert.x 实例
        Vertx vertx = RpcApplication.getVertx();
        // 创建 TCP 客户端
        NetClient netClient = vertx.createNetClient();
        CompletableFuture<RpcResponse> responseFuture = new CompletableFuture<>();

        netClient.connect(serviceMetaInfo.getServicePort(), serviceMetaInfo.getServiceHost(),
                result -> {
                    // 0.连接失败处理
                    if (result.failed()) {
                        responseFuture.completeExceptionally(new RuntimeException("TCP 连接失败"));
                        return;
                    }
                    NetSocket socket = result.result();
                    // 1.构造协议消息
                    ProtocolMessage<RpcRequest> protocolMessage = new ProtocolMessage<>();
                    ProtocolMessage.Header header = new ProtocolMessage.Header();
                    header.setMagic(ProtocolConstant.PROTOCOL_MAGIC);
                    header.setVersion(ProtocolConstant.PROTOCOL_VERSION);
                    header.setSerializer((byte) ProtocolMessageSerializerEnum
                            .getEnumByValue(RpcApplication.getRpcConfig().getSerializer())
                            .getKey());
                    header.setType((byte) ProtocolMessageTypeEnum.REQUEST.getKey());
                    // 生成全局唯一请求ID
                    header.setRequestId(IdUtil.getSnowflakeNextId());
                    protocolMessage.setHeader(header);
                    protocolMessage.setBody(rpcRequest);

                    // 2.编码请求，发送请求
                    try {
                        Buffer encodeBuffer = ProtocolMessageEncoder.encode(protocolMessage);
                        socket.write(encodeBuffer);
                    } catch (IOException e) {
                        responseFuture.completeExceptionally(new RuntimeException("协议消息编码错误"));
                        return;
                    }

                    // 3.接收响应
                    // 【关键点】这里也要使用装饰者模式封装 Handler，解决响应的粘包半包问题
                    TcpBufferHandlerWrapper tcpBufferHandlerWrapper = new TcpBufferHandlerWrapper(
                            buffer -> {
//                                log.info("客户端收到响应数据包，长度：{}", buffer.length());
                                try {
                                    ProtocolMessage<RpcResponse> response = (ProtocolMessage<RpcResponse>) ProtocolMessageDecoder.decode(buffer);
                                    responseFuture.complete(response.getBody());
                                } catch (IOException e) {
                                    responseFuture.completeExceptionally(new RuntimeException("协议消息解码错误"));
                                }
                            });
                    socket.handler(tcpBufferHandlerWrapper);
                });
        // 阻塞，直到响应完成，才会继续向下执行
        RpcResponse rpcResponse = null;
        try {
            rpcResponse = responseFuture.get(3, TimeUnit.SECONDS);
            // 记得关闭连接
            netClient.close();
            return rpcResponse;
        } catch (TimeoutException e) {
            throw new RuntimeException("RPC 调用超时", e);
        }
    }
}
