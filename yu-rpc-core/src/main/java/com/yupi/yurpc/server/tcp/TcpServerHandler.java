package com.yupi.yurpc.server.tcp;

import com.yupi.yurpc.model.RpcRequest;
import com.yupi.yurpc.model.RpcResponse;
import com.yupi.yurpc.protocol.*;
import com.yupi.yurpc.registry.LocalRegistry;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.net.NetSocket;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.lang.reflect.Method;

@Slf4j
public class TcpServerHandler implements Handler<NetSocket> {

    /**
     * 处理请求
     *
     * @param socket
     */
    @Override
    public void handle(NetSocket socket) {
        // 1. 监听连接关闭
        socket.closeHandler(v -> {
            System.out.println("DEBUG: 服务端检测到连接断开");
        });
        // 1. 使用 Wrapper 包装原始的 Handler，自动解决粘包半包
        TcpBufferHandlerWrapper tcpBufferHandlerWrapper = new TcpBufferHandlerWrapper(buffer -> {
            // 🔍 埋点 1
//            System.out.println("【服务端】收到数据包，长度：" + buffer.length());
            // Lambda表达式，这里是new Handler<Buffer>()
            // 2. 这里的 buffer 已经是完整的消息了（头+体）
            // 处理连接
            // 3.接受请求，解码
            ProtocolMessage<RpcRequest> protocolMessage;
            try {
                protocolMessage = (ProtocolMessage<RpcRequest>) ProtocolMessageDecoder.decode(buffer);
//                System.out.println("DEBUG: 服务端解码出的 Request ID = " + protocolMessage.getHeader().getRequestId());
            } catch (IOException e) {
                throw new RuntimeException("协议消息解码错误", e);
            }
            RpcRequest rpcRequest = protocolMessage.getBody();

            // 4.处理请求（通过反射调用）
            RpcResponse rpcResponse = new RpcResponse();
            try {
                // 获取要调用的服务实现类，通过反射调用
                Class<?> implClass = LocalRegistry.get(rpcRequest.getServiceName());
                Method method = implClass.getMethod(rpcRequest.getMethodName(), rpcRequest.getParameterTypes());
                Object result = method.invoke(implClass.newInstance(), rpcRequest.getArgs());
                // 封装返回结果
                rpcResponse.setData(result);
                rpcResponse.setDataType(method.getReturnType());
                rpcResponse.setMessage("ok");
            } catch (Exception e) {
                rpcResponse.setMessage(e.getMessage());
                rpcResponse.setException(e);
            }

            // 5.编码响应并发送
            // 构造响应消息头 (复用请求头的大部分信息，如 RequestId)
            ProtocolMessage.Header header = protocolMessage.getHeader();
            header.setType((byte) ProtocolMessageTypeEnum.RESPONSE.getKey());
            ProtocolMessage<RpcResponse> responseProtocolMessage = new ProtocolMessage<>(header, rpcResponse);
            try {
                Buffer encode = ProtocolMessageEncoder.encode(responseProtocolMessage);
                // 🔍 埋点 2
//                System.out.println("【服务端】业务处理完毕，准备发送响应，长度：" + encode.length());
                socket.write(encode);
            } catch (IOException e) {
                throw new RuntimeException("协议消息编码错误");
            }
        });
        socket.handler(tcpBufferHandlerWrapper);
    }
}
