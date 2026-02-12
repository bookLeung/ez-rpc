package com.yupi.yurpc.bootstrap;

import com.yupi.yurpc.RpcApplication;

/**
 * 消费者引导类/启动类/初始化
 */
public class ConsumerBootstrap {

    /**
     * 初始化
     */
    public static void init() {
        // 0.RPC 框架初始化（配置和注册中心）
        RpcApplication.init();
    }

}
