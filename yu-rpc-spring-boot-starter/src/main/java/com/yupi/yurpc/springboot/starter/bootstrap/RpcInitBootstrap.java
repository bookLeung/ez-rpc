package com.yupi.yurpc.springboot.starter.bootstrap;

import com.yupi.yurpc.RpcApplication;
import com.yupi.yurpc.config.RpcConfig;
import com.yupi.yurpc.registry.Registry;
import com.yupi.yurpc.server.tcp.VertxTcpServer;
import com.yupi.yurpc.springboot.starter.annotation.EnableRpc;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.type.AnnotationMetadata;

import java.util.Map;

/**
 * Rpc 框架启动，在 Spring 框架初始化时，获取 @EnableRpc 注解的属性，并初始化 RPC 框架。
 */
@Slf4j
public class RpcInitBootstrap implements ImportBeanDefinitionRegistrar {

    /**
     * Spring 初始化时执行，初始化 RPC 框架
     *
     * @param importingClassMetadata
     * @param registry
     */
    @Override
    public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
        log.info(">>> RpcInitBootstrap 开始执行");
        log.info(">>> metadata: {}", importingClassMetadata);
        // 1. 获取 EnableRpc 注解的属性值
        Map<String, Object> attributes = importingClassMetadata.getAnnotationAttributes(EnableRpc.class.getName());
        log.info(">>> attributes: {}", attributes);
        // 🔥 核心修复：增加判空逻辑
        if (attributes == null) {
            // 如果拿不到注解属性，说明不是通过 @EnableRpc 加载的，或者注解被擦除了
            // 这种情况下，我们通常选择不处理，或者给一个默认值
            // 这里我们选择不启动 Server，或者打印警告
            log.warn("未获取到 @EnableRpc 注解属性，跳过 RpcInitBootstrap 初始化");
            return;
        }
        boolean needServer = (boolean) attributes.get("needServer");

        // RPC 框架初始化（配置和注册中心）
        RpcApplication.init();

        // 全局配置
        final RpcConfig rpcConfig = RpcApplication.getRpcConfig();

        // 启动服务器
        if (needServer) {
            VertxTcpServer vertxTcpServer = new VertxTcpServer();
            vertxTcpServer.doStart(rpcConfig.getServerPort());
        } else {
            log.info("不启动 server");
        }
    }
}
