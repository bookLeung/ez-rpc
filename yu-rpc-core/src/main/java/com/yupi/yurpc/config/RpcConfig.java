package com.yupi.yurpc.config;

import lombok.Data;

/**
 * RPC 配置，附带默认值
 */
@Data
public class RpcConfig {

    /**
     * RPC服务名称
     */
    private String name = "yu-rpc";

    /**
     * RPC服务版本
     */
    private String version = "1.0";

    /**
     * 服务器主机号
     */
    private String serverHost = "localhost";

    /**
     * 服务器端口号
     */
    private Integer serverPort = 8080;
}
