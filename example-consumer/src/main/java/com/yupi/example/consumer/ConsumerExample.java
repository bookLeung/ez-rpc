package com.yupi.example.consumer;

import com.yupi.example.common.model.User;
import com.yupi.example.common.service.UserService;
import com.yupi.yurpc.RpcApplication;
import com.yupi.yurpc.config.RpcConfig;
import com.yupi.yurpc.proxy.ServiceProxyFactory;
import com.yupi.yurpc.utils.ConfigUtils;

/**
 * 拓展版RPC服务消费者示例
 */
public class ConsumerExample {
    public static void main(String[] args) {
//        RpcConfig rpc = ConfigUtils.loadConfig(RpcConfig.class, "rpc");
//        System.out.println(rpc.toString());

        // 动态代理
        UserService userService = ServiceProxyFactory.getProxy(UserService.class);
        User user = new User();
        user.setName("yupi");
//        // 调用3次
//        for (int i = 0; i < 3; i++) {
//            User newUser = userService.getUser(user);
//            if (newUser != null) {
//                System.out.println(newUser.getName());
//            } else {
//                System.out.println("user == null");
//            }
//        }
        User newUser = userService.getUser(user);
        if (newUser != null) {
            System.out.println(newUser.getName());
        } else {
            System.out.println("user == null");
        }

        // 测试mock
        // 假设开启mock，那么mockServiceProxy会返回另一个值
        // 假设不开启mock，并且服务提供者没有实现这个方法，服务端会继承用default，但与RPC无关
        short userAge = userService.getUserAge();
        System.out.println(userAge);
    }
}
