package com.yupi.example.provider;

import com.yupi.example.common.service.UserService;
import com.yupi.yurpc.RpcApplication;
import com.yupi.yurpc.config.RegistryConfig;
import com.yupi.yurpc.config.RpcConfig;
import com.yupi.yurpc.model.ServiceMetaInfo;
import com.yupi.yurpc.registry.LocalRegistry;
import com.yupi.yurpc.registry.Registry;
import com.yupi.yurpc.registry.RegistryFactory;
import com.yupi.yurpc.server.HttpServer;
import com.yupi.yurpc.server.VertxHttpServer;

/**
 * 拓展版RPC服务提供者示例
 */
public class ProviderExample {
    public static void main(String[] args) {
        // RPC 框架初始化
        RpcApplication.init();

        String serviceName = UserService.class.getName();

        // 注册服务到本地，即使搞了注册中心，这里也要保留
        LocalRegistry.register(serviceName, UserServiceImpl.class);

        // 注册服务到注册中心
        // 获取 RPC 配置
        RpcConfig rpcConfig = RpcApplication.getRpcConfig();
        // 先获取注册中心
        // 获取注册中心配置
        RegistryConfig registryConfig = rpcConfig.getRegistryConfig();
        // 通过工厂获取注册中心实例，参数为注册中心类型，由配置文件决定
        Registry registry = RegistryFactory.getInstance(registryConfig.getRegistry());
        // 构造服务元信息
        ServiceMetaInfo serviceMetaInfo = new ServiceMetaInfo();
        serviceMetaInfo.setServiceName(serviceName);
        serviceMetaInfo.setServiceHost(rpcConfig.getServerHost());
        serviceMetaInfo.setServicePort(rpcConfig.getServerPort());
        // 注册服务到注册中心
        try{
            registry.register(serviceMetaInfo);
        }catch (Exception e) {
            throw new RuntimeException(e);
        }

        // 启动web服务
        HttpServer httpServer = new VertxHttpServer();
        httpServer.doStart(rpcConfig.getServerPort());
    }
}
