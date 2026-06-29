package com.wxw.cloud.controller;


import com.wxw.cloud.domain.Brand;
import com.wxw.cloud.result.PageResult;
import com.wxw.cloud.service.IBrandService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.ibatis.annotations.Delete;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * <p>
 * 品牌表，一个品牌下有多个商品（spu），一对多关系 前端控制器
 * </p>
 *
 * @author twx
 * @since 2026-05-6
 */
@Api(tags = "BrandController", description = "商品品牌管理")
@RestController
@RequestMapping("/brand")
public class BrandController {

    @Autowired
    private IBrandService brandService;

    /**
     *  根据查询条件分页并排序查询品牌信息
     * @param key
     * @param page
     * @param rows
     * @param sortBy
     * @param desc
     * @return
     */
    @ApiOperation("根据查询条件分页并排序查询品牌信息")
    @GetMapping("page")
    public ResponseEntity<PageResult<Brand>> queryBrandsByPage(
            @RequestParam(value = "key",required = false)String key,
            @RequestParam(value = "page",defaultValue = "1")Integer page,
            @RequestParam(value = "rows", defaultValue = "5")Integer rows,
            @RequestParam(value = "sortBy",required = false)String sortBy,
            @RequestParam(value = "desc",required = false)Boolean desc
    ){
        PageResult<Brand> result = this.brandService.queryBrandsByPage(key,page,rows,sortBy,desc);
        if (result == null || CollectionUtils.isEmpty(result.getItems())){
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(result);
    }

    @ApiOperation("新增品牌")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> saveBrand(
            Brand brand,
            @RequestParam("cids") String cids,
            @RequestParam(value = "imageFile", required = false) MultipartFile imageFile){
        if (brand.getId() == null) {
            this.brandService.saveBrand(brand,parseCids(cids), imageFile);
        } else {
            this.brandService.updateBrand(brand,parseCids(cids), imageFile);
        }
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @ApiOperation("修改品牌")
    @PutMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> updateBrand(
            Brand brand,
            @RequestParam("cids") String cids,
            @RequestParam(value = "imageFile", required = false) MultipartFile imageFile){
        this.brandService.updateBrand(brand,parseCids(cids), imageFile);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @ApiOperation("删除品牌")
    @DeleteMapping("/{brandId}")
    public ResponseEntity<Void> deleteBrand(@PathVariable("brandId") Long brandId){
        this.brandService.deleteBrand(brandId);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @ApiOperation("根据分类cid查询品牌列表")
    @GetMapping("cid/{cid}")
    public ResponseEntity<List<Brand>> queryBrandsByCid(@PathVariable("cid")Long cid){
        List<Brand> brands = this.brandService.queryBrandsByCid(cid);
        if (CollectionUtils.isEmpty(brands)){
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        return ResponseEntity.ok(brands);
    }

    /**
     *  搜索微服务使用
     * @param id
     * @return
     */
    @ApiOperation("根据商品Id查询商品信息")
    @GetMapping("{id}")
    public ResponseEntity<Brand> queryBrandById(@PathVariable("id")Long id){
        Brand brand = this.brandService.queryBrandById(id);
        if (brand == null){
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(brand);
    }

    private List<Long> parseCids(String cids) {
        return Arrays.stream(cids.split(","))
                .filter(cid -> cid != null && cid.trim().length() > 0)
                .map(cid -> Long.valueOf(cid.trim()))
                .collect(Collectors.toList());
    }




}
