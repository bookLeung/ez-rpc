package com.yupi.yurpc.springboot.starter.bootstrap;

import com.yupi.yurpc.RpcApplication;
import com.yupi.yurpc.config.RegistryConfig;
import com.yupi.yurpc.config.RpcConfig;
import com.yupi.yurpc.model.ServiceMetaInfo;
import com.yupi.yurpc.registry.LocalRegistry;
import com.yupi.yurpc.registry.Registry;
import com.yupi.yurpc.registry.RegistryFactory;
import com.yupi.yurpc.springboot.starter.annotation.RpcService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.BeanPostProcessor;

/**
 * RPC 服务提供者启动类
 * 获取到所有包含 @RpcService 注解的类，并且通过注解的属性和反射机制，获取到要注册的服务信息，并且完成服务注册。
 * 利用 Spring 的特性监听 Bean 的加载，能直接获取到服务提供者类的 Bean 对象。
 * 只需要让启动类实现 BeanPostProcessor 接口的 postProcessAfterInitialization 方法，
 * 就可以在某个服务提供者 Bean 初始化后，执行注册服务等操作了。
 */
@Slf4j
public class RpcProviderBootstrap implements BeanPostProcessor {

    /**
     * Bean 初始化后执行，服务注册。
     *
     * @param bean
     * @param beanName
     * @return
     */
    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) {
        Class<?> beanClass = bean.getClass();
        RpcService rpcService = beanClass.getAnnotation(RpcService.class);
        if (rpcService != null) {
            // 需要注册服务
            // 1.获取服务基本信息
            Class<?> interfaceClass = rpcService.interfaceClass();
            // 默认值处理
            if (interfaceClass == void.class) {
                interfaceClass = beanClass.getInterfaces()[0];
            }
            String serviceName = interfaceClass.getName();
            String serviceVersion = rpcService.serviceVersion();

            // 2.注册服务
            // 2.1.本地注册
            LocalRegistry.register(serviceName, beanClass);
            // 2.2.注册服务到注册中心
            final RpcConfig rpcConfig = RpcApplication.getRpcConfig();
            // 注册中心配置
            RegistryConfig registryConfig = rpcConfig.getRegistryConfig();
            // 注册中心实例
            Registry registry = RegistryFactory.getInstance(registryConfig.getRegistry());
            // 构造服务元信息
            ServiceMetaInfo serviceMetaInfo = new ServiceMetaInfo();
            serviceMetaInfo.setServiceName(serviceName);
            serviceMetaInfo.setServiceVersion(serviceVersion);
            serviceMetaInfo.setServiceHost(rpcConfig.getServerHost());
            serviceMetaInfo.setServicePort(rpcConfig.getServerPort());
            try {
                // 注册服务
                registry.register(serviceMetaInfo);
            } catch (Exception e) {
                throw new RuntimeException(serviceName + " 服务注册失败", e);
            }
        }
        return BeanPostProcessor.super.postProcessAfterInitialization(bean, beanName);
    }
}
