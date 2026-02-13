package com.yupi.provider;

import cn.hutool.http.HttpRequest;
import cn.hutool.json.JSONUtil;
import com.yupi.example.common.model.User;
import com.yupi.example.common.service.UserService;
import com.yupi.yurpc.proxy.ServiceProxyFactory;
import com.yupi.yurpc.model.RpcRequest;
//import com.yupi.yurpc.springboot.starter.annotation.RpcReference;
import org.junit.Test;

public class RpcBenchmarkTest {

    // 假设 HTTP 服务跑在 8081 (你需要单独启动旧版 Server)
    private static final String HTTP_URL = "http://localhost:8081";

    // 假设 TCP 服务跑在 8080 (新版 Server)
    // 并且使用了 @RpcReference 自动注入，或者手动获取代理
    private final UserService tcpUserService = ServiceProxyFactory.getProxy(UserService.class);

    @Test
    public void compare() {
        int requests = 10000; // 调用次数

        // 1. 测试 HTTP 耗时
        long httpStart = System.currentTimeMillis();
        for (int i = 0; i < requests; i++) {
            callByHttp();
        }
        long httpEnd = System.currentTimeMillis();
        System.out.println("HTTP (JSON) 调用 " + requests + " 次耗时: " + (httpEnd - httpStart) + " ms");

        // 2. 测试 TCP (自定义协议 + Kryo) 耗时
        long tcpStart = System.currentTimeMillis();
        for (int i = 0; i < requests; i++) {
            callByTcp();
        }
        long tcpEnd = System.currentTimeMillis();
        System.out.println("TCP (Kryo)  调用 " + requests + " 次耗时: " + (tcpEnd - tcpStart) + " ms");
    }

    private void callByHttp() {
        User user = new User();
        user.setName("yupi");
        // 构造 RpcRequest
        RpcRequest rpcRequest = RpcRequest.builder()
                .serviceName("com.yupi.example.common.service.UserService")
                .methodName("getUser")
                .parameterTypes(new Class[]{User.class})
                .args(new Object[]{user})
                .build();

        // 使用 Hutool 发送 HTTP 请求
        String json = JSONUtil.toJsonStr(rpcRequest);
        HttpRequest.post(HTTP_URL)
                .body(json)
//                .header("Connection", "close")
                .execute()
                .body();
    }

    private void callByTcp() {
        // 像调本地方法一样调用
        User user = new User();
        user.setName("yupi");
        tcpUserService.getUser(user);
    }
}