package com.yupi.example.provider;

import com.yupi.example.common.model.User;
import com.yupi.example.common.service.UserService;
import lombok.extern.slf4j.Slf4j;

/**
 * 用户服务实现类
 */
@Slf4j
public class UserServiceImpl implements UserService {

    public User getUser(User user) {
//        System.out.println("用户名：" + user.getName());
//        log.info("Call method getUser(), userName: {}", user.getName());
        return user;
    }
}

