package com.wxw.cloud.api;


import com.wxw.cloud.domain.Brand;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author twx
 * @since 2026-05-6
 */
@RequestMapping("brand")
public interface BrandAPI {

    /**
     *  搜索微服务使用
     */
    @GetMapping("{id}")
    public Brand queryBrandById(@PathVariable("id")Long id);






}

