package com.wxw.cloud.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wxw.cloud.bo.SkuBO;
import com.wxw.cloud.bo.SpuBO;
import com.wxw.cloud.dao.*;
import com.wxw.cloud.domain.*;
import com.wxw.cloud.result.PageResult;
import com.wxw.cloud.service.ICategoryService;
import com.wxw.cloud.service.IGoodsService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wxw.cloud.service.ISpecGroupService;
import com.wxw.cloud.service.ISpecParamService;
import com.wxw.cloud.tools.ListPageUtil;
import com.wxw.cloud.vo.SkuVO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import javax.annotation.Resource;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * <p>
 *  商品管理 实现类
 * </p>
 *
 * @author twx
 * @since 2026-05-6
 */
@Slf4j
@Service
public class GoodsServiceImpl implements IGoodsService {

    private static final List<String> IMAGE_CONTENT_TYPES = Arrays.asList("image/jpeg", "image/gif", "image/png");

    @Resource
    private SpuMapper spuMapper;

    @Resource
    private SpuDetailMapper spuDetailMapper;

    @Resource
    private BrandMapper brandMapper;

    @Resource
    private ICategoryService categoryService;

    @Resource
    private ISpecGroupService specGroupService;

    @Resource
    private ISpecParamService specParamService;



    @Resource
    private SkuMapper skuMapper;

    @Resource
    private StockMapper stockMapper;

    @Resource
    private AmqpTemplate amqpTemplate;

    @Value("${app.sku-image.local-dir:${user.dir}/sku-images}")
    private String skuImageDir;

    @Value("${app.sku-image.url-prefix:/api/item/sku/images/}")
    private String skuImageUrlPrefix;

    /**
     * 根据条件分页查询SPU
     */
    @Override
    public PageResult<SpuBO> querySpuByPage(String key, Boolean saleable, Integer page, Integer rows) {

        if (page == null || page < 1) {
            page = 1;
        }
        if (rows == null || rows < 1) {
            rows = 5;
        }
        QueryWrapper<Spu> wrapper = new QueryWrapper<>();
        // 添加查询条件
        if (StringUtils.isNotBlank(key)){
            wrapper.like("title", key);
        }
        // 添加上下架的过滤条件
        if(saleable != null){
            wrapper.eq("saleable", saleable);
        }
        wrapper.eq("valid", true);
        // 添加分页条件
        Page<Spu> pageParam = new Page<>(page,rows);
        // 执行查询获取SPU集合
        Page<Spu> spuPage = this.spuMapper.selectPage(pageParam, wrapper);
        List<Spu> spus = spuPage.getRecords();
        // SPU集合转化为SpuBO的集合
        List<SpuBO> spuBOs = spus.stream().map(spu -> {
            SpuBO spuBO = new SpuBO();
            BeanUtils.copyProperties(spu, spuBO);
            // 查询分类名称
            List<String> names = this.categoryService.queryNamesByIds(Arrays.asList(spu.getCid1(), spu.getCid2(), spu.getCid3()));
            spuBO.setCname(CollectionUtils.isEmpty(names) ? "" : StringUtils.join(names, "-"));
            Brand brand = this.brandMapper.selectById(spu.getBrandId());
            // 查询品牌名称
            spuBO.setBname(brand == null ? "" : brand.getName());
            return spuBO;
        }).collect(Collectors.toList());
        // 返回PageResult<SpuBO>
        return new PageResult<>(spuPage.getTotal(),spuBOs);
    }

    /**
     *  新增商品
     * @param spuBO
     */
    @Transactional
    @Override
    public void saveGoods(SpuBO spuBO) {

        // 新增spu
        spuBO.setId(null);
        spuBO.setSaleable(true);
        spuBO.setValid(true);
        spuBO.setCreateTime(LocalDateTime.now());
        spuBO.setLastUpdateTime(spuBO.getCreateTime());
        this.spuMapper.insert(spuBO);

        // 新增spuDetail
        SpuDetail spuDetail = spuBO.getSpuDetail();
        spuDetail.setSpuId(spuBO.getId());
        this.spuDetailMapper.insert(spuDetail);
        // 新增sku and stock
        this.saveSkuAndStock(spuBO);

        // 发送消息
        sendMsg("insert",spuBO.getId());
    }

    /**
     *  MQ 监听发送消息
     * @param type
     * @param id
     */
    private void sendMsg(String type,Long id) {
        try {
            this.amqpTemplate.convertAndSend("item."+type,id);
        } catch (AmqpException e) {
            log.info("新增商品消息发送失败：=>{}");
        }
    }

    // 新增sku and stock
    private void saveSkuAndStock(SpuBO spuBO) {
        if (spuBO.getSkus() == null || spuBO.getSkus().isEmpty()) {
            Sku sku = new Sku();
            sku.setTitle(spuBO.getTitle());
            sku.setImages("");
            sku.setPrice(0L);
            sku.setIndexes("0");
            sku.setOwnSpec("{}");
            sku.setEnable(true);
            sku.setStock(0);
            spuBO.setSkus(Collections.singletonList(sku));
        }
        spuBO.getSkus().forEach(sku -> {
            // 新增sku
            sku.setId(null);
            sku.setSpuId(spuBO.getId());
            if (StringUtils.isBlank(sku.getTitle())) {
                sku.setTitle(spuBO.getTitle());
            }
            if (sku.getPrice() == null) {
                sku.setPrice(0L);
            }
            if (sku.getStock() == null) {
                sku.setStock(0);
            }
            if (sku.getEnable() == null) {
                sku.setEnable(true);
            }
            if (StringUtils.isBlank(sku.getIndexes())) {
                sku.setIndexes("0");
            }
            if (StringUtils.isBlank(sku.getOwnSpec())) {
                sku.setOwnSpec("{}");
            }
            if (sku.getImages() == null) {
                sku.setImages("");
            }
            sku.setCreateTime(LocalDateTime.now());
            sku.setLastUpdateTime(sku.getCreateTime());
            this.skuMapper.insert(sku);
            // 新增stock
            Stock stock = new Stock();
            stock.setSkuId(sku.getId());
            stock.setStock(sku.getStock());
            stock.setSeckillStock(0);
            stock.setSeckillTotal(0);
            this.stockMapper.insert(stock);

        });
    }

    /**
     * 根据spuId查询SpuDetail信息
     */
    @Override
    public SpuDetail querySpuDetailBySpuId(Long spuId) {
        return this.spuDetailMapper.selectById(spuId);
    }

    @Override
    public List<Sku> querySkusBySpuId(Long spuId) {
       QueryWrapper<Sku> wrapper = new QueryWrapper<>();
       wrapper.eq("spu_id", spuId);
       wrapper.and(w -> w.isNull("own_spec").or().notLike("own_spec", "\"_homeArea\""));
        List<Sku> skus = this.skuMapper.selectList(wrapper);
        skus.forEach(sku -> {
            Stock stock = this.stockMapper.selectById(sku.getId());
            if(stock != null){
                sku.setStock(stock.getStock());
            }
        });
        return skus;
    }

    @Override
    public List<SkuVO> querySkusByCid(Long cid) {
        QueryWrapper<Spu> spuWrapper = new QueryWrapper<>();
        spuWrapper.eq("valid", true);
        spuWrapper.eq("saleable", true);
        spuWrapper.and(wrapper -> wrapper.eq("cid1", cid).or().eq("cid2", cid).or().eq("cid3", cid));
        List<Spu> spus = this.spuMapper.selectList(spuWrapper);
        if (CollectionUtils.isEmpty(spus)) {
            return Collections.emptyList();
        }
        List<SkuVO> result = new ArrayList<>();
        spus.forEach(spu -> {
            QueryWrapper<Sku> skuWrapper = new QueryWrapper<>();
            skuWrapper.eq("spu_id", spu.getId());
            skuWrapper.eq("enable", true);
            skuWrapper.and(w -> w.isNull("own_spec").or().notLike("own_spec", "\"_homeArea\""));
            List<Sku> skus = this.skuMapper.selectList(skuWrapper);
            skus.forEach(sku -> {
                fillStock(sku);
                SkuVO skuVO = new SkuVO();
                BeanUtils.copyProperties(sku, skuVO);
                skuVO.setSubtitle(spu.getSubTitle());
                result.add(skuVO);
            });
        });
        return result;
    }

    @Override
    public List<SkuVO> queryHomeSkus(String area) {
        QueryWrapper<Sku> skuWrapper = new QueryWrapper<>();
        skuWrapper.eq("enable", true);
        skuWrapper.like("own_spec", "\"_homeArea\":\"" + area + "\"");
        skuWrapper.orderByDesc("last_update_time");
        List<Sku> skus = this.skuMapper.selectList(skuWrapper);
        if (CollectionUtils.isEmpty(skus)) {
            return Collections.emptyList();
        }
        List<SkuVO> result = new ArrayList<>();
        skus.forEach(sku -> {
            fillStock(sku);
            SkuVO skuVO = new SkuVO();
            BeanUtils.copyProperties(sku, skuVO);
            Spu spu = this.spuMapper.selectById(sku.getSpuId());
            if (spu != null) {
                skuVO.setSubtitle(spu.getSubTitle());
            }
            result.add(skuVO);
        });
        return result;
    }

    /**
     * 修改商品信息
     * @param spuBO
     */
    @Transactional
    @Override
    public void updateGoods(SpuBO spuBO) {

        // 根据spuId 查询要删除的sku
        QueryWrapper<Sku> wrapper = new QueryWrapper<>();
        wrapper.eq("spu_id", spuBO.getId());
        wrapper.and(w -> w.isNull("own_spec").or().notLike("own_spec", "\"_homeArea\""));
        List<Sku> skus = this.skuMapper.selectList(wrapper);
        skus.forEach(sku -> {
            // 删除 stock
            this.stockMapper.deleteById(sku.getId());
        });

        // 删除sku
        QueryWrapper<Sku> wrapper1 = new QueryWrapper<>();
        wrapper1.eq("spu_id", spuBO.getId());
        wrapper1.and(w -> w.isNull("own_spec").or().notLike("own_spec", "\"_homeArea\""));
        this.skuMapper.delete(wrapper1);

        // 新增sku and stock
        this.saveSkuAndStock(spuBO);
        // 更新 spu和spuDetail
        spuBO.setCreateTime(null);
        spuBO.setLastUpdateTime(LocalDateTime.now());
        spuBO.setValid(null);
        spuBO.setSaleable(null);
        this.spuMapper.updateById(spuBO);
        this.spuDetailMapper.updateById(spuBO.getSpuDetail());
        sendMsg("update", spuBO.getId());
    }

    @Transactional
    @Override
    public void deleteGoods(Long id) {
        Spu spu = new Spu();
        spu.setId(id);
        spu.setValid(false);
        spu.setSaleable(false);
        spu.setLastUpdateTime(LocalDateTime.now());
        this.spuMapper.updateById(spu);
        sendMsg("delete", id);
    }

    @Override
    public void updateSaleable(Long id, Boolean saleable) {
        Spu spu = new Spu();
        spu.setId(id);
        spu.setSaleable(saleable);
        spu.setLastUpdateTime(LocalDateTime.now());
        this.spuMapper.updateById(spu);
        sendMsg("update", id);
    }

    @Override
    public Spu querySpuById(Long id) {
        return this.spuMapper.selectById(id);
    }

    @Override
    public Sku querySkuBySkuId(Long skuId) {
        Sku sku = this.skuMapper.selectById(skuId);
        if (sku != null) {
            fillStock(sku);
        }
        return sku;
    }

    @Override
    public PageResult<Sku> querySkuPage(String key, Boolean enable, Integer page, Integer rows, Boolean homeOnly) {
        QueryWrapper<Sku> wrapper = new QueryWrapper<>();
        if (StringUtils.isNotBlank(key)) {
            wrapper.like("title", key);
        }
        if (enable != null) {
            wrapper.eq("enable", enable);
        }
        if (Boolean.TRUE.equals(homeOnly)) {
            wrapper.like("own_spec", "\"_homeArea\"");
        } else {
            wrapper.and(w -> w.isNull("own_spec").or().notLike("own_spec", "\"_homeArea\""));
        }
        wrapper.orderByDesc("last_update_time");
        Page<Sku> pageParam = new Page<>(page, rows);
        Page<Sku> skuPage = this.skuMapper.selectPage(pageParam, wrapper);
        skuPage.getRecords().forEach(this::fillStock);
        return new PageResult<>(skuPage.getTotal(), skuPage.getRecords());
    }

    @Transactional
    @Override
    public void saveSku(Sku sku) {
        if (sku == null || sku.getSpuId() == null) {
            throw new IllegalArgumentException("spuId must not be null");
        }
        sku.setId(null);
        if (StringUtils.isBlank(sku.getTitle())) {
            Spu spu = this.spuMapper.selectById(sku.getSpuId());
            sku.setTitle(spu == null ? "前台商品" : spu.getTitle());
        }
        if (sku.getPrice() == null) {
            sku.setPrice(0L);
        }
        if (sku.getEnable() == null) {
            sku.setEnable(true);
        }
        if (StringUtils.isBlank(sku.getIndexes())) {
            sku.setIndexes("0");
        }
        if (StringUtils.isBlank(sku.getOwnSpec())) {
            sku.setOwnSpec("{}");
        }
        if (sku.getImages() == null) {
            sku.setImages("");
        }
        sku.setCreateTime(LocalDateTime.now());
        sku.setLastUpdateTime(sku.getCreateTime());
        this.skuMapper.insert(sku);

        Stock stock = new Stock();
        stock.setSkuId(sku.getId());
        stock.setStock(sku.getStock() == null ? 0 : sku.getStock());
        stock.setSeckillStock(0);
        stock.setSeckillTotal(0);
        this.stockMapper.insert(stock);
        sendMsg("update", sku.getSpuId());
    }

    @Transactional
    @Override
    public void updateSku(Sku sku) {
        Sku old = this.skuMapper.selectById(sku.getId());
        sku.setCreateTime(null);
        sku.setLastUpdateTime(LocalDateTime.now());
        this.skuMapper.updateById(sku);

        Stock stock = new Stock();
        stock.setSkuId(sku.getId());
        stock.setStock(sku.getStock() == null ? 0 : sku.getStock());
        if (this.stockMapper.selectById(sku.getId()) == null) {
            this.stockMapper.insert(stock);
        } else {
            this.stockMapper.updateById(stock);
        }
        Long spuId = sku.getSpuId() != null ? sku.getSpuId() : (old == null ? null : old.getSpuId());
        if (spuId != null) {
            sendMsg("update", spuId);
        }
    }

    @Override
    public void updateSkuEnable(Long id, Boolean enable) {
        Sku old = this.skuMapper.selectById(id);
        Sku sku = new Sku();
        sku.setId(id);
        sku.setEnable(enable);
        sku.setLastUpdateTime(LocalDateTime.now());
        this.skuMapper.updateById(sku);
        if (old != null && old.getSpuId() != null) {
            sendMsg("update", old.getSpuId());
        }
    }

    @Override
    public void deleteSku(Long id) {
        updateSkuEnable(id, false);
    }

    @Override
    public SkuBO getSkusByBrand(Long brandId, Integer page, Integer rows) {
        if (page == null || page < 1) {
            page = 1;
        }
        if (rows == null || rows < 1) {
            rows = 10;
        }
        SkuBO skuBO = new SkuBO();
        PageResult<SkuVO> pageResult = new PageResult<>();
        if (brandId == null) {
            pageResult.setItems(Collections.emptyList());
            pageResult.setTotal(0L);
            pageResult.setTotalPage(0L);
            skuBO.setSkus(pageResult);
            skuBO.setCid3s(Collections.emptySet());
            skuBO.setCname(Collections.emptySet());
            return skuBO;
        }

        QueryWrapper<Spu> spuWrapper = new QueryWrapper<>();
        spuWrapper.eq("brand_id", brandId);
        spuWrapper.eq("valid", true);
        spuWrapper.eq("saleable", true);
        List<Spu> spus = this.spuMapper.selectList(spuWrapper);
        if (CollectionUtils.isEmpty(spus)) {
            pageResult.setItems(Collections.emptyList());
            pageResult.setTotal(0L);
            pageResult.setTotalPage(0L);
            skuBO.setSkus(pageResult);
            skuBO.setCid3s(Collections.emptySet());
            skuBO.setCname(Collections.emptySet());
            return skuBO;
        }

        List<SkuVO> skus = new ArrayList<>();
        List<Long> cid3s = new ArrayList<>();
        spus.forEach(spu -> {
            QueryWrapper<Sku> skuWrapper = new QueryWrapper<>();
            skuWrapper.eq("spu_id", spu.getId());
            skuWrapper.eq("enable", true);
            skuWrapper.and(w -> w.isNull("own_spec").or().notLike("own_spec", "\"_homeArea\""));
            List<Sku> list = this.skuMapper.selectList(skuWrapper);
            list.forEach(sku -> {
                fillStock(sku);
                SkuVO skuVO = new SkuVO();
                BeanUtils.copyProperties(sku, skuVO);
                skuVO.setSubtitle(spu.getSubTitle());
                skus.add(skuVO);
            });
            cid3s.add(spu.getCid3());
        });

        ListPageUtil<SkuVO> pager = new ListPageUtil<>(skus, rows);
        pageResult.setItems(pager.getPagedList(page));
        pageResult.setTotal((long) skus.size());
        pageResult.setTotalPage((long) pager.getPageCount());
        skuBO.setSkus(pageResult);
        skuBO.setCid3s(new HashSet<>(cid3s));
        skuBO.setCname(new HashSet<>(this.categoryService.queryNamesByIds(cid3s)));
        return skuBO;
    }

    @Override
    public String saveSkuImage(MultipartFile imageFile) {
        if (imageFile == null || imageFile.isEmpty()) {
            return null;
        }
        try {
            if (!IMAGE_CONTENT_TYPES.contains(imageFile.getContentType())) {
                return null;
            }
            BufferedImage image = ImageIO.read(imageFile.getInputStream());
            if (image == null) {
                return null;
            }
            File uploadDir = new File(skuImageDir).getAbsoluteFile();
            if (!uploadDir.exists() && !uploadDir.mkdirs()) {
                return null;
            }
            String originalFilename = imageFile.getOriginalFilename();
            String ext = StringUtils.substringAfterLast(originalFilename, ".");
            if (StringUtils.isBlank(ext)) {
                ext = "jpg";
            }
            String filename = UUID.randomUUID().toString().replace("-", "") + "." + ext;
            imageFile.transferTo(new File(uploadDir, filename));
            return skuImageUrlPrefix + filename;
        } catch (IOException e) {
            log.info("save sku image failed", e);
            return null;
        }
    }

    private void fillStock(Sku sku) {
        Stock stock = this.stockMapper.selectById(sku.getId());
        if (stock != null) {
            sku.setStock(stock.getStock());
        }
    }

    @Override
    public SkuBO getSkusAndCid3(Integer page, Integer rows, String categoryName, String key) {
        if (page == null || page < 1) {
            page = 1;
        }
        if (rows == null || rows < 1) {
            rows = 10;
        }
        SkuBO skuBO = new SkuBO();
        //1.查询SPU
        QueryWrapper<Spu> querySpu = new QueryWrapper<>();
        querySpu.eq("valid", true);
        querySpu.eq("saleable", true);
        List<Spu> spus = this.spuMapper.selectList(querySpu);
        //2.通过spu查询skulist
        PageResult<SkuVO> pageResult = new PageResult<>();
        // 商品集合
        List<SkuVO> skus = new ArrayList<>();
        List<Long> cid3s= new ArrayList<>();
        //log.info("SPU：{}",spus);
        spus.forEach(spu -> {
            List<String> categoryNames = this.categoryService.queryNamesByIds(Collections.singletonList(spu.getCid3()));
            String spuCategoryName = CollectionUtils.isEmpty(categoryNames) ? "" : categoryNames.get(0);
            if (StringUtils.isNotBlank(categoryName) && !"全部商品".equals(categoryName) && !categoryName.equals(spuCategoryName)) {
                return;
            }
            QueryWrapper<Sku> querySku = new QueryWrapper<>();
            querySku.eq("spu_id", spu.getId());
            querySku.eq("enable", true);
            List<Sku> list = this.skuMapper.selectList(querySku);
            if (StringUtils.isNotBlank(key)) {
                list = list.stream()
                        .filter(sku -> StringUtils.containsIgnoreCase(spu.getTitle(), key)
                                || StringUtils.containsIgnoreCase(spu.getSubTitle(), key)
                                || StringUtils.containsIgnoreCase(sku.getTitle(), key))
                        .collect(Collectors.toList());
            }
            //log.info("查询出的库存数量：{}",list);
            list.forEach(sku -> {
//                log.info("SKU ：{}",sku);
//                log.info("SKU ID：{}",sku.getId());
                Stock stock = this.stockMapper.selectById(sku.getId());

                if (stock != null){
                    sku.setStock(stock.getStock());
                }
                // 设置空库存
//                Stock add = new Stock();
//                add.setSkuId(sku.getId());
//                add.setStock(100);
//                this.stockMapper.updateById(add);
//                sku.setStock(add.getStock());
            });
            // sku 集合转换为 skuvo集合
            List<SkuVO> skuVOS = list.stream().map(sku -> {
                SkuVO skuVO = new SkuVO();
                skuVO.setSubtitle(spu.getSubTitle());
                skuVO.setStock(sku.getStock());
                skuVO.setId(sku.getId());
                skuVO.setSpuId(sku.getSpuId());
                skuVO.setImages(sku.getImages());
                skuVO.setPrice(sku.getPrice());
                skuVO.setTitle(sku.getTitle());
                return skuVO;
            }).collect(Collectors.toList());
            skus.addAll(skuVOS);
            cid3s.add(spu.getCid3());
        });
        // 分页处理
        ListPageUtil<SkuVO> pager = new ListPageUtil<>(skus,rows);
        List<SkuVO> pagedList = pager.getPagedList(page);
        pageResult.setItems(pagedList);
        pageResult.setTotal(new Long(skus.size()));
        pageResult.setTotalPage(new Long(pager.getPageCount()));
        skuBO.setSkus(pageResult);
        //3. 增加目录信息
        skuBO.setCid3s(new HashSet<>(cid3s));
        List<String> names = this.categoryService.queryNamesByIds(cid3s);
        skuBO.setCname(new HashSet<>(names));
        // 保存数据
        return skuBO;
    }

    // 商品详情页
    @Override
    public Map<String,Object> loadData(Long spuId){
        Map<String,Object> model = new HashMap<>();

        // 1. 根据spuId查询spu
        Spu spu = this.spuMapper.selectById(spuId);
        // 2. 查询spuDetail
        SpuDetail spuDetail = this.spuDetailMapper.selectById(spuId);
        // 3，查询分类 Map<String,Object>
        List<Long> cids = Arrays.asList(spu.getCid1(), spu.getCid2(), spu.getCid3());
        List<String> names = this.categoryService.queryNamesByIds(cids);
        // 4， 初始化一个分类的map
        List<Map<String,Object>> categories = new ArrayList<>();
        for (int i = 0; i < cids.size(); i++) {
            Map<String,Object> map = new HashMap<>();
            map.put("id",cids.get(i));
            map.put("name", names.get(i));
            categories.add(map);
        }
        // 5,查询品牌
        Brand brand = this.brandMapper.selectById(spu.getBrandId());

        // 6， 查询skus
        List<Sku> skus = this.querySkusBySpuId(spuId);

        // 7，查询规格参数组
        List<SpecGroup> groups = this.specGroupService.queryGroupswithParam(spu.getCid3());

        // 8, 查询特殊规格参数
        List<SpecParam> params = this.specParamService.queryParams(null, spu.getCid3(), false, null);
        // 初始化特殊规格参数map
        Map<Long,String> paramMap = new HashMap<>();
        params.forEach(param->{
              paramMap.put(param.getId(),param.getName());
        });
        model.put("spu",spu);
        model.put("spuDetail",spuDetail);
        model.put("categories",categories);
        model.put("brand",brand);
        model.put("skus",skus);
        model.put("groups",groups);
        model.put("paramMap",paramMap);
        return model;
    }

}
