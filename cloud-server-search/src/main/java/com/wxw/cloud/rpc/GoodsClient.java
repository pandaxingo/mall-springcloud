package com.wxw.cloud.rpc;

import com.wxw.cloud.api.GoodsAPI;
import com.wxw.cloud.domain.Spu;
import com.wxw.cloud.result.PageResult;
import org.springframework.cloud.openfeign.FeignClient;

/**
 * @author twx
 * @create:   2026-5-16
 */
@FeignClient("cloud-bs-service")
public interface GoodsClient extends GoodsAPI {


}
