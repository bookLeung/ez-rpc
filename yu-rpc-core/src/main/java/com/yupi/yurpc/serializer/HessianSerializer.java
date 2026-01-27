package com.yupi.yurpc.serializer;

import cn.hutool.core.convert.Convert;
import com.caucho.hessian.io.HessianInput;
import com.caucho.hessian.io.HessianOutput;
import com.yupi.yurpc.model.RpcResponse;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Hessian 序列化器
 */
public class HessianSerializer implements Serializer {

    @Override
    public <T> byte[] serialize(T object) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        // 【核心点 1】直接创建 HessianOutput 包装流
        HessianOutput ho = new HessianOutput(bos);
        ho.writeObject(object);
        return bos.toByteArray();
    }

    @Override
    public <T> T deserialize(byte[] bytes, Class<T> tClass) throws IOException {
        ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
        // 【核心点 2】直接创建 HessianInput 包装流
        HessianInput hi = new HessianInput(bis);
        T obj = (T) hi.readObject(tClass);

        // ✅ 核心修复：单独处理 RpcResponse，解决 Integer -> Short/Byte 的转换问题
        if (obj instanceof RpcResponse) {
            RpcResponse rpcResponse = (RpcResponse) obj;
            Class<?> dataType = rpcResponse.getDataType();
            Object data = rpcResponse.getData();

            // 如果数据存在，且类型不匹配（比如 data是Integer，但 dataType是Short）
            if (data != null && dataType != null && !dataType.isAssignableFrom(data.getClass())) {
                // 利用 Hutool 的 Convert 工具类进行万能转换
                // 它能自动把 Integer 转成 Short, Byte, Long 等等
                Object convertedData = Convert.convert(dataType, data);
                rpcResponse.setData(convertedData);
            }
        }

        return obj;
    }
}