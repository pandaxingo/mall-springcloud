package com.wxw.cloud.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wxw.cloud.dao.BrandMapper;
import com.wxw.cloud.domain.Brand;
import com.wxw.cloud.result.PageResult;
import com.wxw.cloud.service.IBrandService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * <p>
 * 品牌表，一个品牌下有多个商品（spu），一对多关系 服务实现类
 * </p>
 *
 * @author twx
 * @since 2026-05-6
 */
@Service
public class BrandServiceImpl extends ServiceImpl<BrandMapper, Brand> implements IBrandService {

    private static final List<String> IMAGE_CONTENT_TYPES = Arrays.asList("image/jpeg", "image/png", "image/gif");

    @Autowired
    private BrandMapper brandMapper;

    @Value("${app.brand-image.local-dir:${user.dir}/brand-images}")
    private String brandImageDir;

    @Value("${app.brand-image.url-prefix:http://localhost:10010/api/item/brand/images/}")
    private String brandImageUrlPrefix;

    /**
     *  根据查询条件分页并排序查询品牌信息
     */
    @Override
    public PageResult<Brand> queryBrandsByPage(String key, Integer page, Integer rows, String sortBy, Boolean desc) {

         // 初始化查询对象
        QueryWrapper<Brand> queryWrapper = new QueryWrapper<>();
        // 1. key 根据name模糊查询，或者根据首字母查询
          if (StringUtils.isNotBlank(key)){
              queryWrapper.like("name", key).or().like("letter", key);
          }
        // 2. 添加分页条件
        Page<Brand> pageParam = new Page<>(page,rows);
        // 3.添加排序条件
        if (StringUtils.isNotBlank(sortBy)){
            queryWrapper.orderBy(desc,false, sortBy);
        }
        Page<Brand> brandPage = this.brandMapper.selectPage(pageParam, queryWrapper);
        // 包装分页结果集
        return new PageResult<>(brandPage.getTotal(),brandPage.getPages(),brandPage.getRecords());
    }

    /**
     * 新增商品
     * @param brand  商品信息
     * @param cids   类目id
     */
    @Transactional
    @Override
    public void saveBrand(Brand brand, List<Long> cids, MultipartFile imageFile) {
        String imageUrl = this.saveBrandImage(imageFile);
        if (StringUtils.isNotBlank(imageUrl)) {
            brand.setImage(imageUrl);
        }
        // 新增brand
        this.brandMapper.insert(brand);
        // 新增中间表
        cids.forEach(cid -> {
            this.brandMapper.insertCategoryAndBrand(cid, brand.getId());
        });
    }
    /**
     * 商品修改
     * @param brand
     * @param cids
     */
    @Transactional
    @Override
    public void updateBrand(Brand brand, List<Long> cids, MultipartFile imageFile) {
        String imageUrl = this.saveBrandImage(imageFile);
        if (StringUtils.isNotBlank(imageUrl)) {
            brand.setImage(imageUrl);
        }
        //先修改 brand
        this.brandMapper.updateById(brand);
        //删除关联的中间表
        this.brandMapper.deleteCategoryBrandId(brand.getId());
        // 添加中间表
        cids.forEach(cid->{
            this.brandMapper.insertCategoryAndBrand(cid, brand.getId());
        });
    }

    /**
     * 删除商品
     * @param brandId 商品ID
     */
    @Transactional
    @Override
    public void deleteBrand(Long brandId) {
        //删除商品表  删除其实是下架
        this.brandMapper.deleteById(brandId);
        //删除中间表的关联
        this.brandMapper.deleteCategoryBrandId(brandId);
    }

    /**
     * 根据分类cid查询品牌列表
     * @param cid
     * @return
     */
    @Override
    public List<Brand> queryBrandsByCid(Long cid) {
        return this.brandMapper.queryBrandsByCid(cid);
    }

    @Override
    public Brand queryBrandById(Long id) {
        return this.brandMapper.selectById(id);
    }

    private String saveBrandImage(MultipartFile imageFile) {
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

            File uploadDir = new File(brandImageDir).getAbsoluteFile();
            if (!uploadDir.exists() && !uploadDir.mkdirs()) {
                return null;
            }

            String originalFilename = imageFile.getOriginalFilename();
            String ext = "jpg";
            if (StringUtils.isNotBlank(originalFilename) && originalFilename.lastIndexOf(".") > -1) {
                ext = originalFilename.substring(originalFilename.lastIndexOf(".") + 1);
            }
            String filename = UUID.randomUUID().toString().replace("-", "") + "." + ext;
            imageFile.transferTo(new File(uploadDir, filename));
            return brandImageUrlPrefix + filename;
        } catch (IOException e) {
            return null;
        }
    }
}
