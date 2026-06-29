package com.wxw.cloud.client;

import com.wxw.cloud.domain.User;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * @author twx
 * @create:   2026-5-16
 */
@RequestMapping()
public interface UserClient {

    //@ApiOperation("根据用户名和密码查询指定用户")
    @GetMapping("/user/query")
    public User volidCode(@RequestParam("username") String username, @RequestParam("password") String password);
}
