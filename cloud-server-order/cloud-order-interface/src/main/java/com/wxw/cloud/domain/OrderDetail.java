package com.wxw.cloud.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * <p>
 * 订单详情表
 * </p>
 *
 * @author twx
 * @since 2026-05-6
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("tb_order_detail")
public class OrderDetail implements Serializable {

    private static final long serialVersionUID=1L;

    /**
     * 订单详情id 
     */
    @JsonSerialize(using = ToStringSerializer.class)
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 订单id
     */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long orderId;

    /**
     * sku商品id
     */
    private Long skuId;

    /**
     * 购买数量
     */
    private Integer num;

    /**
     * 商品标题
     */
    private String title;

    /**
     * 商品动态属性键值集
     */
    private String ownSpec;

    /**
     * 价格，单位为元
     */
    private Long price;

    /**
     * 商品图片
     */
    private String image;


}
