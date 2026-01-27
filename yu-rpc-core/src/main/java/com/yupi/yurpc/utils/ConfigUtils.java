package com.yupi.yurpc.utils;

import cn.hutool.core.util.StrUtil;
import cn.hutool.setting.dialect.Props;

/**
 * 配置工具类
 */
public class ConfigUtils {

    /**
     * 从配置文件读取配置，以加载配置对象
     *
     * @param tClass
     * @param prefix
     * @return 配置对象
     * @param <T>
     */
    public static <T> T loadConfig(Class<T> tClass, String prefix) {
        return loadConfig(tClass, prefix, "");
    }

    /**
     * 从配置文件读取配置，以加载配置对象，支持区分环境
     *
     * @param tClass 配置类.class
     * @param prefix
     * @param environment
     * @return 配置对象
     * @param <T> 配置类
     */
    public static <T> T loadConfig(Class<T> tClass, String prefix, String environment) {
        // 一般如application-prod.properties
        StringBuilder configFileBuilder = new StringBuilder("application");
        if (StrUtil.isNotBlank(environment)) {
            configFileBuilder.append("-").append(environment);
        }

        // TODO 支持其他格式配置文件
        configFileBuilder.append(".properties");
        Props props = new Props(configFileBuilder.toString());
        return props.toBean(tClass, prefix);
    }
}
