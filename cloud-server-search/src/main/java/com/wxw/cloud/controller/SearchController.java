package com.wxw.cloud.controller;

import com.wxw.cloud.domain.Goods;
import com.wxw.cloud.domain.SearchRequest;
import com.wxw.cloud.domain.SearchResult;
import com.wxw.cloud.result.PageResult;
import com.wxw.cloud.service.SearchService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.io.IOException;

/**
 * @author twx
 * @create:   2026-5-16
 */
@Api(tags = "SearchController",description = "搜索微服务")
@RestController
@RequestMapping("search")
public class SearchController {

    @Resource
    private SearchService searchService;

    @ApiOperation("健康检查")
    @GetMapping("health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("OK");
    }

    @ApiOperation("基本搜索入口")
    @PostMapping("page")
    public ResponseEntity<SearchResult> search(@RequestBody SearchRequest searchRequest){
        SearchResult result=this.searchService.search(searchRequest);
        if (result==null|| CollectionUtils.isEmpty(result.getItems())){
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(result);
    }
    @ApiOperation("初始化搜索索引")
    @PostMapping("init")
    public ResponseEntity<Void> init() throws IOException {
        this.searchService.initAll();
        return ResponseEntity.ok().build();
    }

}
