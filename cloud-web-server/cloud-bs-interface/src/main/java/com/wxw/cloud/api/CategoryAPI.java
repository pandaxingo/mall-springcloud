package com.wxw.cloud.api;


import com.wxw.cloud.domain.Category;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

/**
 * @author twx
 * @since 2026-05-6
 */
@RequestMapping("/category")
public interface CategoryAPI {

    /**
     * 搜索微服务使用
     * 根据多个分类ID查询 分类名称
     */
    @GetMapping
    public List<String> queryNamesByIds(@RequestParam("ids")List<Long> ids);



}

