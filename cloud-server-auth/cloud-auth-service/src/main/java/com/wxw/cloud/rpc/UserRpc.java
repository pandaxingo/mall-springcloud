package com.wxw.cloud.rpc;

import com.wxw.cloud.client.UserClient;
import org.springframework.cloud.openfeign.FeignClient;

/**
 * @author twx
 * @create:   2026-5-16
 */
@FeignClient("cloud-user-service")
public interface UserRpc extends UserClient {

}
