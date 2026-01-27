package com.yupi.example.common.service;

import com.yupi.example.common.model.User;

/**
 * 用户服务
 */
public interface UserService {

    /**
     * 获取用户
     *
     * @param user
     * @return
     */
    User getUser(User user);

    /**
     * 获取用户年龄，用于mock测试
     *
     * @return
     */
    default short getUserAge() {
        return 18;
    }
}
