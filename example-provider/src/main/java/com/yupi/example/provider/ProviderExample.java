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
import com.yupi.yurpc.server.tcp.VertxTcpServer;
import com.yupi.yurpc.utils.ConfigUtils;

/**
 * 拓展版RPC服务提供者示例
 */
public class ProviderExample {
    public static void main(String[] args) {
//        // 0.RPC 框架初始化
//        RpcApplication.init();

        // 1. 手动加载配置（原本是 init 方法里自动加载的，现在我们提出来）
        RpcConfig rpcConfig = ConfigUtils.loadConfig(RpcConfig.class, "rpc");

        // 2. 【核心修改】检查命令行参数，如果有参数，就覆盖端口
        // 假设 args[0] 是端口号
        if (args.length > 0) {
            String port = args[0];
            rpcConfig.setServerPort(Integer.parseInt(port));
            System.out.println("使用命令行参数指定的端口启动: " + port);
        }

        // 3. 使用修改后的配置初始化框架
        RpcApplication.init(rpcConfig);

        // 1.注册服务
        String serviceName = UserService.class.getName();
        // 1.1注册服务到本地，即使搞了注册中心，这里也要保留
        // 因为注册中心只是路由到 ip:port，还需要本地注册中心内部路由到具体服务实现类
        LocalRegistry.register(serviceName, UserServiceImpl.class);
        // 1.2注册服务到注册中心
        // 1.2.1获取 RPC 配置
//        RpcConfig rpcConfig = RpcApplication.getRpcConfig();
        // 1.2.2先获取注册中心
        // 获取注册中心配置
        RegistryConfig registryConfig = rpcConfig.getRegistryConfig();
        // 通过工厂获取注册中心实例，参数为注册中心类型，由配置文件决定
        Registry registry = RegistryFactory.getInstance(registryConfig.getRegistry());
        // 1.2.3构造服务元信息
        ServiceMetaInfo serviceMetaInfo = new ServiceMetaInfo();
        serviceMetaInfo.setServiceName(serviceName);
        serviceMetaInfo.setServiceHost(rpcConfig.getServerHost());
        serviceMetaInfo.setServicePort(rpcConfig.getServerPort());
        // 1.2.4注册服务到注册中心
        try{
            registry.register(serviceMetaInfo);
        }catch (Exception e) {
            throw new RuntimeException(e);
        }

//        // 启动web服务
//        HttpServer httpServer = new VertxHttpServer();
//        httpServer.doStart(rpcConfig.getServerPort());

        // 2.启动 TCP 服务
        VertxTcpServer vertxTcpServer = new VertxTcpServer();
        vertxTcpServer.doStart(rpcConfig.getServerPort());
    }
}
