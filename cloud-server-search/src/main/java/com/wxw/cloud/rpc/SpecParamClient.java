package com.wxw.cloud.rpc;


import com.wxw.cloud.api.SpecParamAPI;
import com.wxw.cloud.domain.SpecParam;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * @author twx
 * @since 2026-05-6
 */
@FeignClient("cloud-bs-service")
public interface SpecParamClient extends SpecParamAPI {

}

