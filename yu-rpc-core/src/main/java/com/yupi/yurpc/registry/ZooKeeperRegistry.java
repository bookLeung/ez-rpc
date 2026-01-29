package com.yupi.yurpc.registry;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.ConcurrentHashSet;
import com.yupi.yurpc.config.RegistryConfig;
import com.yupi.yurpc.model.ServiceMetaInfo;
import io.etcd.jetcd.ByteSequence;
import io.etcd.jetcd.watch.WatchEvent;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.cache.CuratorCache;
import org.apache.curator.framework.recipes.cache.CuratorCacheListener;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.curator.x.discovery.ServiceDiscovery;
import org.apache.curator.x.discovery.ServiceDiscoveryBuilder;
import org.apache.curator.x.discovery.ServiceInstance;
import org.apache.curator.x.discovery.details.JsonInstanceSerializer;

import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * ZooKeeper 注册中心
 */
@Slf4j
public class ZooKeeperRegistry implements Registry {

    private CuratorFramework client;

    private ServiceDiscovery<ServiceMetaInfo> serviceDiscovery;

    /**
     * zookeeper 根路径
     */
    private static final String ZK_ROOT_PATH = "/rpc/zk";

    /**
     * 本机注册的节点 key 集合（用于维护续期）
     */
    private final Set<String> localRegisterNodeKeySet = new HashSet<>();

    /**
     * 正在监听的 key 集合（用于维护监听）
     */
    private final Set<String> watchingKeySet = new ConcurrentHashSet<>();

    /**
     * 注册中心服务缓存
     */
    private final RegistryServiceCache registryServiceCache = new RegistryServiceCache();

    @Override
    public void init(RegistryConfig registryConfig) {
        // 构建 client 实例
        client = CuratorFrameworkFactory
                .builder()
                .connectString(registryConfig.getAddress())
                .retryPolicy(new ExponentialBackoffRetry(Math.toIntExact(registryConfig.getTimeout()), 3))
                .build();

        // 构建 serviceDiscovery 实例
        serviceDiscovery = ServiceDiscoveryBuilder.builder(ServiceMetaInfo.class)
                .client(client)
                .basePath(ZK_ROOT_PATH)
                .serializer(new JsonInstanceSerializer<>(ServiceMetaInfo.class))
                .build();

        // 启动
        try {
            client.start();
            serviceDiscovery.start();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void register(ServiceMetaInfo serviceMetaInfo) throws Exception {
        // 注册到 zk
        serviceDiscovery.registerService(buildServiceInstance(serviceMetaInfo));
        // 添加节点信息到本地缓存记录
        String registerKey = ZK_ROOT_PATH + "/" + serviceMetaInfo.getServiceKey() + "/" + serviceMetaInfo.getServiceNodeKey();
        localRegisterNodeKeySet.add(registerKey);
    }

    private ServiceInstance<ServiceMetaInfo> buildServiceInstance(ServiceMetaInfo serviceMetaInfo) {
        String serviceAddress = serviceMetaInfo.getServiceHost() + ":" + serviceMetaInfo.getServicePort();
        try {
            return ServiceInstance
                    .<ServiceMetaInfo>builder()
                    .id(serviceAddress)
                    .name(serviceMetaInfo.getServiceKey())
                    .address(serviceAddress)
                    .port(serviceMetaInfo.getServicePort())
                    .payload(serviceMetaInfo)
                    .build();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void unRegister(ServiceMetaInfo serviceMetaInfo) {
        try {
            serviceDiscovery.unregisterService(buildServiceInstance(serviceMetaInfo));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        // 从本地缓存移除
        String registerKey = ZK_ROOT_PATH + "/" + serviceMetaInfo.getServiceKey() + "/" + serviceMetaInfo.getServiceNodeKey();
        localRegisterNodeKeySet.remove(registerKey);
    }

    @Override
    public List<ServiceMetaInfo> serviceDiscovery(String serviceKey) {
        // 优先从缓存获取服务
        List<ServiceMetaInfo> cachedServiceMetaInfoList = registryServiceCache.readCache();
        if (CollUtil.isNotEmpty(cachedServiceMetaInfoList)) {
            return cachedServiceMetaInfoList;
        }

        try {
            // 查询服务信息
            Collection<ServiceInstance<ServiceMetaInfo>> serviceInstances = serviceDiscovery.queryForInstances(serviceKey);

            // 解析服务信息
            List<ServiceMetaInfo> serviceMetaInfoList = serviceInstances.stream()
                    .map(ServiceInstance::getPayload)
                    .toList();

            // 写入服务缓存
            registryServiceCache.writeCache(serviceMetaInfoList);
            return serviceMetaInfoList;
        } catch (Exception e) {
            throw new RuntimeException("获取服务列表失败", e);
        }
    }

    @Override
    public void destroy() {
        // 下线节点（这一步可以不做，因为都是临时节点，服务下线，自然就被删掉了）
        log.info("当前节点下线");

        // 下线节点，遍历本节点所有key
        for (String key : localRegisterNodeKeySet) {
            try {
                client.delete().guaranteed().forPath(key);
            } catch (Exception e) {
                throw new RuntimeException(key + "节点下线失败", e);
            }
        }

        // 释放资源
        if (client != null) {
            client.close();
        }
    }

    @Override
    public void heartBeat() {
        // 不需要心跳机制，建立了临时节点，如果服务器故障，则临时节点直接丢失
    }

    @Override
    public void watch(String serviceNodeKey) {
        String watchKey = ZK_ROOT_PATH + "/" + serviceNodeKey;
        // 之前未被监听，开启监听
        boolean newWatch = watchingKeySet.add(serviceNodeKey);
        if (newWatch) {
            CuratorCache curatorCache = CuratorCache.builder(client, watchKey).build();
            curatorCache.start();
            curatorCache.listenable().addListener(
                    CuratorCacheListener
                            .builder()
                            .forDeletes(childData -> registryServiceCache.clearCache())
                            .forChanges((oldData, newData) -> registryServiceCache.clearCache())
                            .build()
            );
        }
    }
}
