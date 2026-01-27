package com.yupi.yurpc.serializer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yupi.yurpc.model.RpcRequest;
import com.yupi.yurpc.model.RpcResponse;

import java.io.IOException;

/**
 * Json 序列化器
 */
public class JsonSerializer implements Serializer {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Override
    public <T> byte[] serialize(T obj) throws IOException {
        return OBJECT_MAPPER.writeValueAsBytes(obj);
    }

    @Override
    public <T> T deserialize(byte[] bytes, Class<T> classType) throws IOException {
        T obj = OBJECT_MAPPER.readValue(bytes, classType);
        if (obj instanceof RpcRequest) {
            return handleRequest((RpcRequest) obj, classType);
        }
        if (obj instanceof RpcResponse) {
            return handleResponse((RpcResponse) obj, classType);
        }
        return obj;
    }

    /**
     * 由于 Object 的原始对象会被擦除，导致反序列化时会被作为 LinkedHashMap 无法转换
     * 因此需要特殊处理 RpcRequest
     *
     * @param rpcRequest rpc 请求
     * @param type       类型
     * @return {@link T}
     * @throws IOException IO异常
     */
    private <T> T handleRequest(RpcRequest rpcRequest, Class<T> type) throws IOException {
        // 拿到真正的参数类型列表（这是 RPC 框架特意传过来的元数据）
        Class<?>[] parameterTypes = rpcRequest.getParameterTypes();
        Object[] args = rpcRequest.getArgs();

        // 遍历每一个参数
        for (int i = 0; i < parameterTypes.length; i++) {
            Class<?> clazz = parameterTypes[i];

            // 核心判断：如果 args[i] 的类型跟 parameterTypes[i] 不一样
            // 比如：args[i] 是 LinkedHashMap，但 clazz 是 User.class
            if (!clazz.isAssignableFrom(args[i].getClass())) {

                // 【骚操作开始】
                // 1. 把这个 Map 重新序列化成 JSON 字节
                byte[] argBytes = OBJECT_MAPPER.writeValueAsBytes(args[i]);
                // 2. 拿着 User.class (clazz) 指定类型，再反序列化一次
                args[i] = OBJECT_MAPPER.readValue(argBytes, clazz);
                // 现在 args[i] 变回了真正的 User 对象
            }
        }
        return type.cast(rpcRequest);
    }

    /**
     * 由于 Object 的原始对象会被擦除，导致反序列化时会被作为 LinkedHashMap 无法转换
     * 因此需要特殊处理 RpcResponse
     *
     * @param rpcResponse rpc 响应
     * @param type        类型
     * @return {@link T}
     * @throws IOException IO异常
     */
    private <T> T handleResponse(RpcResponse rpcResponse, Class<T> type) throws IOException {
        // 1. 把 data (可能是个 LinkedHashMap) 转回字节
        byte[] dataBytes = OBJECT_MAPPER.writeValueAsBytes(rpcResponse.getData());

        // 2. 根据 rpcResponse.getDataType() 指定的真实类型 (比如 User.class) 重新生成对象
        rpcResponse.setData(OBJECT_MAPPER.readValue(dataBytes, rpcResponse.getDataType()));

        return type.cast(rpcResponse);
    }
}