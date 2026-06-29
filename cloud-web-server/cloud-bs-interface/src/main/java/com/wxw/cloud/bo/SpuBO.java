package com.wxw.cloud.bo;

import com.wxw.cloud.domain.Sku;
import com.wxw.cloud.domain.Spu;
import com.wxw.cloud.domain.SpuDetail;
import lombok.Data;

import java.util.List;

/**
 * @author twx
 * @create:   2026-5-16
 * 扩展spu对象
 */
@Data
public class SpuBO extends Spu {

    private String cname;

    private String bname;

    private SpuDetail spuDetail;

    private List<Sku> skus;


}