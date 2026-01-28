package com.yupi.yurpc.constant;

/**
 * RPC 常量
 */
public interface RpcConstant {

    /**
     * 默认配置文件加载前缀
     *
     * 可以读取类似下面的配置
     * rpc.name=yurpc
     * rpc.version=2.0
     * rpc.serverPort=8081
     */
    String DEFAULT_CONFIG_PREFIX = "rpc";

    /**
     * 默认服务版本
     */
    String DEFAULT_SERVICE_VERSION = "1.0";
}
