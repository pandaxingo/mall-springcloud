package com.wxw.cloud.service;

import cn.hutool.captcha.LineCaptcha;
import com.baomidou.mybatisplus.extension.service.IService;
import com.wxw.cloud.domain.User;

import java.io.OutputStream;
import java.util.List;

/**
 * <p>
 * 用户表 服务类
 * </p>
 *
 * @author twx
 * @since 2026-05-6
 */
public interface IUserService extends IService<User> {

    Boolean checkUser(String data, Integer type);

    LineCaptcha saveVerifyCode();

    void register(User user, String code);

    User queryUser(String username, String password);

    List<User> getUserList();

    void saveAdminUser(User user);

    void updateAdminUser(User user);

    void deleteAdminUser(Long id);
}
