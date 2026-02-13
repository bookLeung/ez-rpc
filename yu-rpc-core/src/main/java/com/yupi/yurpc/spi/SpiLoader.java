package com.yupi.yurpc.spi;

import cn.hutool.core.io.resource.ResourceUtil;
import com.yupi.yurpc.serializer.Serializer;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SPI 加载器（支持键值对映射）
 * 核心职责：读取配置文件 -> 加载类 -> 实例化对象 -> 缓存
 */
@Slf4j
public class SpiLoader {

    /**
     * 存储已加载的类：接口名 =>（Key => 实现类）
     * 示例：Serializer.class.getName() => { "json": JsonSerializer.class, "kryo": KryoSerializer.class }
     */
    private static Map<String, Map<String, Class<?>>> loaderMap = new ConcurrentHashMap<>();

    /**
     * 对象实例缓存（避免重复 new），类路径 => 对象实例，单例模式
     * 示例：com.yupi.JsonSerializer => JsonSerializerImplInstance
     */
    private static Map<String, Object> instanceCache = new ConcurrentHashMap<>();

    /**
     * 系统 SPI 目录 (框架自带的)，注意最后斜杠不能省略
     */
    private static final String RPC_SYSTEM_SPI_DIR = "META-INF/rpc/system/";

    /**
     * 用户 SPI 目录 (用户自定义的)，注意最后斜杠不能省略
     */
    private static final String RPC_CUSTOM_SPI_DIR = "META-INF/rpc/custom/";

    /**
     * 扫描路径
     */
    private static final String[] SCAN_DIRS = new String[]{RPC_SYSTEM_SPI_DIR, RPC_CUSTOM_SPI_DIR};

    /**
     * 动态加载的类列表（目前只加载 Serializer，后续可以加其他接口）
     */
    private static final List<Class<?>> LOAD_CLASS_LIST = Arrays.asList(Serializer.class);

    /**
     * 加载所有类型 (入口方法)
     */
    public static void loadAll() {
        log.info("Load all SPI");
        for (Class<?> aClass : LOAD_CLASS_LIST) {
            load(aClass);
        }
    }

    /**
     * 加载某个类型，仅加载类型，不实例化
     *
     * @param loadClass 接口类
     * @return 加载到的映射关系
     */
    public static Map<String, Class<?>> load(Class<?> loadClass) {
//        log.info("Load SPI: {}", loadClass.getName());
        // 扫描路径，用户定义的SPI优先级高于系统SPI
        Map<String, Class<?>> keyClassMap = new HashMap<>();

        // 遍历目录，先system后custom
        for (String scanDir : SCAN_DIRS) {
            // 使用了 ResourceUtil.getResources ，而不是通过文件路径获取。
            // 因为如果框架作为依赖被引入，是无法得到正确文件路径的。
            // ResourceUtil.getResources 是 Hutool 的工具，可以读取 classpath 下的所有资源
            // 比如 scanDir + loadClass.getName() 就是 "META-INF/rpc/system/com.yupi.yurpc.serializer.Serializer"
            List<URL> resources = ResourceUtil.getResources(scanDir + loadClass.getName());

            // 读取每个资源文件
            for (URL resource : resources) {
                try {
                    InputStreamReader inputStreamReader = new InputStreamReader(resource.openStream());
                    BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                    String line;
                    while ((line = bufferedReader.readLine()) != null) {
                        String[] strArray = line.split("=");
                        if (strArray.length > 1) {
                            String key = strArray[0];
                            String className = strArray[1];
                            // 类加载
                            keyClassMap.put(key, Class.forName(className));
                        }
                    }
                } catch (Exception e) {
                    log.error("Load SPI error: {}", e.getMessage());
                }
            }
        }
        loaderMap.put(loadClass.getName(), keyClassMap);
        return keyClassMap;
    }

    /**
     * 获取指定类型的实例
     *
     * @param tClass 类型
     * @param key    键
     * @param <T>    类型
     * @return 实例
     */
    public static <T> T getInstance(Class<?> tClass, String key) {
        String tClassName = tClass.getName();
        Map<String, Class<?>> keyClassMap = loaderMap.get(tClassName);

        // 懒加载设计：如果没加载过，尝试加载一次
        if (keyClassMap == null) {
            load(tClass);
            keyClassMap = loaderMap.get(tClassName);
        }

        if (keyClassMap == null) {
            throw new RuntimeException(String.format("SpiLoader 未加载 %s 类型", tClassName));
        }

        if (!keyClassMap.containsKey(key)) {
            throw new RuntimeException(String.format("SpiLoader 的 %s 不存在 key=%s 的实现", tClassName, key));
        }

        // 获取到要加载的实现类型
        Class<?> implClass = keyClassMap.get(key);
        // 从实例缓存中加载指定类型的实例
        String implClassName = implClass.getName();

        // 双检锁单例模式（简化版，使用 Map 的 computeIfAbsent 也可以）
        if (!instanceCache.containsKey(implClassName)) {
            try {
                instanceCache.put(implClassName, implClass.newInstance());
            } catch (InstantiationException | IllegalAccessException e) {
                String errorMsg = String.format("SpiLoader 创建 %s 类实例失败", implClassName);
                throw new RuntimeException(errorMsg, e);
            }
        }

        return (T) instanceCache.get(implClassName);
    }
}