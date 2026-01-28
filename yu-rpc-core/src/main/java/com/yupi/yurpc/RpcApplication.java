package com.yupi.yurpc;

import com.yupi.yurpc.config.RegistryConfig;
import com.yupi.yurpc.config.RpcConfig;
import com.yupi.yurpc.constant.RpcConstant;
import com.yupi.yurpc.registry.Registry;
import com.yupi.yurpc.registry.RegistryFactory;
import com.yupi.yurpc.utils.ConfigUtils;
import lombok.extern.slf4j.Slf4j;

/**
 * RPC 框架应用
 * 相当于holder，存放了项目全局用到的变量。双检锁单例模式实现
 * 以后 RPC 框架内只需要写一行代码，就能正确加载到配置：
 * RpcConfig rpc = RpcApplication.getRpcConfig();
 */
@Slf4j
public class RpcApplication {
    private static volatile RpcConfig rpcConfig;

    /**
     * 框架初始化，支持传入自定义配置
     *
     * @param newRpcConfig
     */
    public static void init(RpcConfig newRpcConfig) {
        rpcConfig = newRpcConfig;
        log.info("rpc init, config = {}", newRpcConfig.toString());

        // 注册中心初始化
        // 获取注册中心配置
        RegistryConfig registryConfig = rpcConfig.getRegistryConfig();
        // 通过工厂获取注册中心实例，参数为注册中心类型，由配置文件决定
        // 局部变量registry会在方法结束时失效，但其指向的具体的对象实例在工厂中活了下来
        Registry registry = RegistryFactory.getInstance(registryConfig.getRegistry());
        // 注册中心初始化
        registry.init(registryConfig);
        log.info("registry init, config = {}", registryConfig);
    }

    /**
     * 初始化
     */
    public static void init() {
        RpcConfig newRpcConfig;
        try {
            // 尝试从配置文件加载，默认前缀rpc，如rpc.name=yurpc
            newRpcConfig = ConfigUtils.loadConfig(RpcConfig.class, RpcConstant.DEFAULT_CONFIG_PREFIX);
        } catch (Exception e) {
            // 配置加载失败，使用默认值
            newRpcConfig = new RpcConfig();
        }
        init(newRpcConfig);
    }

    public static RpcConfig getRpcConfig() {
        // DCL 单例模式
        if (rpcConfig == null) {
            synchronized (RpcApplication.class) {
                if (rpcConfig == null) {
                    init();
                }
            }
        }

        return rpcConfig;
    }
}
