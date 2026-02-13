package com.yupi.yurpc.server.tcp;

import cn.hutool.core.util.IdUtil;
import com.yupi.yurpc.RpcApplication;
import com.yupi.yurpc.model.RpcRequest;
import com.yupi.yurpc.model.RpcResponse;
import com.yupi.yurpc.model.ServiceMetaInfo;
import com.yupi.yurpc.protocol.ProtocolConstant;
import com.yupi.yurpc.protocol.ProtocolMessage;
import com.yupi.yurpc.protocol.ProtocolMessageEncoder;
import com.yupi.yurpc.protocol.ProtocolMessageSerializerEnum;
import com.yupi.yurpc.protocol.ProtocolMessageTypeEnum;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.net.NetClient;
import io.vertx.core.net.NetClientOptions;
import io.vertx.core.net.NetSocket;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Vert.x TCP 客户端。
 * 目标：
 * 1. 对同一服务地址复用长连接，避免每次请求都创建/销毁连接；
 * 2. 基于 requestId 进行请求-响应关联；
 * 3. 在写失败、超时、连接异常时主动淘汰坏连接，保证后续可重连。
 */
@Slf4j
public class VertxTcpClient {

    /**
     * 全局单例 NetClient，应用内复用。
     * 客户端“连接工厂/管理器”，用于发起 TCP 连接（connect）、承载全局网络配置（keepalive、超时等）。
     */
    private static final NetClient NET_CLIENT;

    static {
        Vertx vertx = RpcApplication.getVertx();
        // 开启 TCP keepalive，降低空闲连接被网络设备回收后“半开”不可见的概率。
        NetClientOptions options = new NetClientOptions().setTcpKeepAlive(true);
        NET_CLIENT = vertx.createNetClient(options);
    }

    /**
     * 连接缓存池：serviceAddress -> 连接持有者
     * 例如：localhost:8080 -> ConnectionHolder
     */
    private static final ConcurrentHashMap<String, ConnectionHolder> KEY_CHANNEL_MAP = new ConcurrentHashMap<>();

    /**
     * 待响应请求表：requestId -> Future
     * 发送请求后先放入该表，收到响应后由 TcpClientHandler 按 requestId 取出并完成。
     */
    public static final ConcurrentHashMap<Long, CompletableFuture<RpcResponse>> PENDING_REQUEST_MAP = new ConcurrentHashMap<>();

    private VertxTcpClient() {
    }

    private static class ConnectionHolder {
        /**
         * 实际 TCP 连接。
         * 一次具体 TCP 连接本体，用于 write、收包 handler、关闭/异常回调。
         * 简单说：NetClient 负责“建连接”，NetSocket 负责“用连接”。
         */
        private final NetSocket socket;

        /**
         * 连接是否可用。由 closeHandler / exceptionHandler 驱动变更。
         */
        private volatile boolean active = true;

        /**
         * 最后活跃时间（最近一次读写时间）。
         * 当前阶段主要用于观测，后续可用于空闲连接回收策略。
         */
        private volatile long lastActiveTime = System.currentTimeMillis();

        private ConnectionHolder(NetSocket socket) {
            this.socket = socket;
        }

        private void touch() {
            lastActiveTime = System.currentTimeMillis();
        }
    }

    /**
     * 发送 RPC 请求并同步等待响应。
     */
    public static RpcResponse doRequest(RpcRequest rpcRequest, ServiceMetaInfo serviceMetaInfo)
            throws ExecutionException, InterruptedException {
        String serviceAddress = serviceMetaInfo.getServiceHost() + ":" + serviceMetaInfo.getServicePort();

        // 1) 获取可复用连接；如果缓存不存在或已失活，则创建新连接。
        ConnectionHolder holder = getOrCreateConnection(serviceAddress, serviceMetaInfo);
        NetSocket socket = holder.socket;
        holder.touch();

        // 2) 构造协议消息，请求头内写入全局 requestId。
        ProtocolMessage<RpcRequest> protocolMessage = new ProtocolMessage<>();
        ProtocolMessage.Header header = new ProtocolMessage.Header();
        header.setMagic(ProtocolConstant.PROTOCOL_MAGIC);
        header.setVersion(ProtocolConstant.PROTOCOL_VERSION);
        header.setSerializer((byte) ProtocolMessageSerializerEnum
                .getEnumByValue(RpcApplication.getRpcConfig().getSerializer())
                .getKey());
        header.setType((byte) ProtocolMessageTypeEnum.REQUEST.getKey());

        long requestId = IdUtil.getSnowflakeNextId();
        header.setRequestId(requestId);
        protocolMessage.setHeader(header);
        protocolMessage.setBody(rpcRequest);

        // 3) 先注册 future，再发送请求，避免响应先到导致 future 丢失。
        CompletableFuture<RpcResponse> responseFuture = new CompletableFuture<>();
        PENDING_REQUEST_MAP.put(requestId, responseFuture);

        try {
            Buffer encodeBuffer = ProtocolMessageEncoder.encode(protocolMessage);
            socket.write(encodeBuffer, writeResult -> {
                if (!writeResult.succeeded()) {
                    // 写失败通常意味着连接已经不可用，需清理 pending 并淘汰连接。
                    PENDING_REQUEST_MAP.remove(requestId);
                    responseFuture.completeExceptionally(writeResult.cause());
                    evictAndClose(serviceAddress, holder);
                    log.warn("TCP 写入失败，连接已淘汰：{}", serviceAddress, writeResult.cause());
                } else {
                    holder.touch();
                }
            });
        } catch (IOException e) {
            PENDING_REQUEST_MAP.remove(requestId);
            throw new RuntimeException("协议消息编码错误", e);
        }

        try {
            Long timeout = RpcApplication.getRpcConfig().getRegistryConfig().getTimeout();
            if (timeout == null || timeout <= 0) {
                // 超时未配置时使用兜底值，避免永久阻塞。
                timeout = 3000L;
            }
            return responseFuture.get(timeout, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            // 超时后主动淘汰并关闭当前连接，避免复用“半死连接”。
            PENDING_REQUEST_MAP.remove(requestId);
            evictAndClose(serviceAddress, holder);
            throw new RuntimeException("RPC 调用超时", e);
        }
    }

    /**
     * 获取或创建连接（双重检查 + 同步串行创建）。
     */
    private static ConnectionHolder getOrCreateConnection(String serviceAddress, ServiceMetaInfo serviceMetaInfo) {
        ConnectionHolder holder = KEY_CHANNEL_MAP.get(serviceAddress);
        if (holder != null && holder.active) {
//            log.debug("复用缓存连接：{}", serviceAddress);
            return holder;
        }

        synchronized (VertxTcpClient.class) {
            holder = KEY_CHANNEL_MAP.get(serviceAddress);
            if (holder != null && holder.active) {
                log.debug("复用缓存连接（双检命中）：{}", serviceAddress);
                return holder;
            }

            // 未命中，创建新连接
            CompletableFuture<ConnectionHolder> connectFuture = new CompletableFuture<>();

            NET_CLIENT.connect(serviceMetaInfo.getServicePort(), serviceMetaInfo.getServiceHost(), result -> {
                if (!result.succeeded()) {
                    connectFuture.completeExceptionally(
                            new RuntimeException("TCP 连接失败", result.cause()));
                    return;
                }

                NetSocket socket = result.result();
                ConnectionHolder newHolder = new ConnectionHolder(socket);

                socket.handler(new TcpBufferHandlerWrapper(new TcpClientHandler()));
                socket.closeHandler(v -> {
                    newHolder.active = false;
                    KEY_CHANNEL_MAP.remove(serviceAddress, newHolder);
                    log.info("TCP 连接关闭：{}", serviceAddress);
                });
                socket.exceptionHandler(throwable -> {
                    newHolder.active = false;
                    KEY_CHANNEL_MAP.remove(serviceAddress, newHolder);
                    log.warn("TCP 连接异常，已淘汰：{}", serviceAddress, throwable);
                });

                connectFuture.complete(newHolder);
            });

            try {
                ConnectionHolder newHolder = connectFuture.get(3, TimeUnit.SECONDS);
                KEY_CHANNEL_MAP.put(serviceAddress, newHolder);
                log.info("创建新的 TCP 连接成功：{}", serviceAddress);
                return newHolder;
            } catch (Exception e) {
                throw new RuntimeException("创建 TCP 连接超时或失败", e);
            }
        }
    }

    /**
     * 淘汰并关闭连接。
     * 使用 remove(key, value) 精确移除，避免误删并发下刚创建的新连接。
     */
    private static void evictAndClose(String serviceAddress, ConnectionHolder holder) {
        if (holder == null) {
            return;
        }
        holder.active = false;
        KEY_CHANNEL_MAP.remove(serviceAddress, holder);
        try {
            holder.socket.close();
        } catch (Exception e) {
            log.warn("关闭 TCP 连接失败：{}", serviceAddress, e);
        }
    }
}
