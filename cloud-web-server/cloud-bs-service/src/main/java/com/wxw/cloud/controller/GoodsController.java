package com.wxw.cloud.controller;


import com.wxw.cloud.bo.SkuBO;
import com.wxw.cloud.bo.SpuBO;
import com.wxw.cloud.domain.Sku;
import com.wxw.cloud.domain.Spu;
import com.wxw.cloud.domain.SpuDetail;
import com.wxw.cloud.result.PageResult;
import com.wxw.cloud.service.IGoodsService;
import com.wxw.cloud.vo.SkuVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * spu表，该表描述的是一个抽象性的商品，比如 iphone8 前端控制器
 * spu_detail
 * </p>
 *
 * @author twx
 * @since 2026-05-6
 */
@Api(tags = "GoodsController",description = "商品管理")
@RestController
@RequestMapping
public class GoodsController {

    @Resource
    private IGoodsService goodsService;

    @GetMapping("spu/page")
    @ApiOperation("根据条件分页查询SPU")
    public ResponseEntity<PageResult<SpuBO>> querySpuByPage(
            @RequestParam(value = "key",required = false)String key,
            @RequestParam(value = "page",defaultValue = "1")Integer page,
            @RequestParam(value = "rows",defaultValue = "5")Integer rows,
            @RequestParam(value = "saleable",required = false)Boolean saleable
    ){
        PageResult<SpuBO> result = this.goodsService.querySpuByPage(key,saleable,page,rows);
        if (result == null){
            return ResponseEntity.ok(new PageResult<>(0L, Collections.emptyList()));
        }
        return ResponseEntity.ok(result);
    }

    @ApiOperation("新增商品")
    @PostMapping("goods")
    public ResponseEntity<Void> saveGoods(@RequestBody SpuBO spuBO){
        this.goodsService.saveGoods(spuBO);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    /**
     * ===========编辑商品回显
     */

    @ApiOperation("根据spuId查询SpuDetail信息")
    @GetMapping("spu/detail/{spuId}")
    public ResponseEntity<SpuDetail> querySpuDetailBySpuId(@PathVariable("spuId")Long spuId){
        SpuDetail spuDetail = this.goodsService.querySpuDetailBySpuId(spuId);
        if (spuDetail == null){
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(spuDetail);
    }

    @ApiOperation("根据 spuId查询SKU集合")
    @GetMapping("sku/list")
    public ResponseEntity<List<Sku>> querySkusBySpuId(@RequestParam("id")Long spuId){
        List<Sku> skus = this.goodsService.querySkusBySpuId(spuId);
        return ResponseEntity.ok(skus);
    }

    @ApiOperation("鏍规嵁鍒嗙被ID鏌ヨSKU闆嗗悎")
    @GetMapping("sku/cid/{cid}")
    public ResponseEntity<List<SkuVO>> querySkusByCid(@PathVariable("cid") Long cid) {
        List<SkuVO> skus = this.goodsService.querySkusByCid(cid);
        return ResponseEntity.ok(skus);
    }

    @ApiOperation("根据首页区域查询前台商品")
    @GetMapping("sku/home/{area}")
    public ResponseEntity<List<SkuVO>> queryHomeSkus(@PathVariable("area") String area) {
        List<SkuVO> skus = this.goodsService.queryHomeSkus(area);
        return ResponseEntity.ok(skus);
    }

    @ApiOperation("修改商品信息")
    @PutMapping("goods")
    public ResponseEntity<Void> updateGoods(@RequestBody SpuBO spuBO){
        this.goodsService.updateGoods(spuBO);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @ApiOperation("删除商品")
    @DeleteMapping("goods/{id}")
    public ResponseEntity<Void> deleteGoods(@PathVariable("id") Long id) {
        this.goodsService.deleteGoods(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @ApiOperation("修改商品上下架状态")
    @PutMapping("goods/{id}/saleable/{saleable}")
    public ResponseEntity<Void> updateSaleable(@PathVariable("id") Long id, @PathVariable("saleable") Boolean saleable) {
        this.goodsService.updateSaleable(id, saleable);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }


    @ApiOperation("根据spuId查询spu信息")
    @GetMapping("{id}")
    public ResponseEntity<Spu> querySpuById(@PathVariable("id")Long id){
        Spu spu = this.goodsService.querySpuById(id);
        if (spu == null){
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(spu);
    }

    /**
     *  购物车
     */
    @GetMapping("sku/{skuId}")
    @ApiOperation("根据skuId查询SKU商品信息")
    public ResponseEntity<Sku> querySkuBySkuId(@PathVariable("skuId")Long skuId){
        Sku sku = this.goodsService.querySkuBySkuId(skuId);
        if (sku == null){
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(sku);
    }

    // 分页查询SKU集合信息 包括三级类目 - 全部商品展示
    @ApiOperation("分页查询SKU集合信息 包括三级类目")
    @GetMapping("sku/page")
    public ResponseEntity<PageResult<Sku>> querySkuPage(
            @RequestParam(value = "key", required = false) String key,
            @RequestParam(value = "enable", required = false) Boolean enable,
            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @RequestParam(value = "rows", defaultValue = "10") Integer rows,
            @RequestParam(value = "homeOnly", required = false) Boolean homeOnly) {
        PageResult<Sku> result = this.goodsService.querySkuPage(key, enable, page, rows, homeOnly);
        return ResponseEntity.ok(result);
    }

    @PutMapping("sku")
    public ResponseEntity<Void> updateSku(@RequestBody Sku sku) {
        this.goodsService.updateSku(sku);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @PostMapping("sku")
    public ResponseEntity<Void> saveSku(@RequestBody Sku sku) {
        this.goodsService.saveSku(sku);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PutMapping("sku/{id}/enable/{enable}")
    public ResponseEntity<Void> updateSkuEnable(@PathVariable("id") Long id, @PathVariable("enable") Boolean enable) {
        this.goodsService.updateSkuEnable(id, enable);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @DeleteMapping("sku/{id}")
    public ResponseEntity<Void> deleteSku(@PathVariable("id") Long id) {
        this.goodsService.deleteSku(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @PostMapping("sku/image")
    public ResponseEntity<String> uploadSkuImage(@RequestParam("file") MultipartFile imageFile) {
        String url = this.goodsService.saveSkuImage(imageFile);
        if (url == null || url.trim().length() == 0) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(url);
    }

    @GetMapping("/skucidlist")
    public ResponseEntity<SkuBO> getSkusAndCid3(
            @RequestParam(value = "page",defaultValue = "1") Integer page,
            @RequestParam(value = "rows",defaultValue = "10") Integer rows,
            @RequestParam(value = "categoryName", required = false) String categoryName,
            @RequestParam(value = "key", required = false) String key){
          SkuBO pageskuList = this.goodsService.getSkusAndCid3(page, rows, categoryName, key);
        if (pageskuList== null|| pageskuList.getSkus() == null){
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
        return ResponseEntity.ok(pageskuList);
    }

    @GetMapping("/skubrandlist")
    public ResponseEntity<SkuBO> getSkusByBrand(
            @RequestParam("brandId") Long brandId,
            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @RequestParam(value = "rows", defaultValue = "10") Integer rows) {
        SkuBO pageskuList = this.goodsService.getSkusByBrand(brandId, page, rows);
        return ResponseEntity.ok(pageskuList);
    }

    // 商品详情页
    @ApiOperation("商品详情页")
    @GetMapping("item/detail")
    public ModelMap getDetails(@RequestParam("spuId")Long spuId, Model model){
        ModelMap modelMap = new ModelMap();
        Map<String, Object> map = this.goodsService.loadData(spuId);
        modelMap.addAllAttributes(map);
        return modelMap;
    }


}
