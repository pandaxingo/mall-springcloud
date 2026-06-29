package com.wxw.cloud.domain;

import lombok.Data;

/*
 * @ClassName Cart
 * @author twx
 * @Description
 * @Date   2026-5-16
 **/
@Data
public class Cart {
    private Long userId;// 用户id
    private Long skuId;// 商品id
    private String title;// 标题
    private String image;// 图片
    private Long price;// 加入购物车时的价格
    private Integer num;// 购买数量
    private String ownSpec;// 商品规格参数
}
