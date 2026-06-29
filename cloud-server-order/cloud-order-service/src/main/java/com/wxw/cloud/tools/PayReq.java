package com.wxw.cloud.tools;

import lombok.Data;

@Data
public class PayReq {

    /** 订单标题 */
    private String subject;

    /** 商户网站唯一订单号 */
    private String out_trade_no;

    /** 总金额，单位：元 */
    private String total_amount;

    /** 用户付款中途退出返回商户网站的地址 */
    private String quit_url;

    /** 销售产品码，电脑网站支付固定为 FAST_INSTANT_TRADE_PAY */
    private String product_code;

    /** 商品编号 */
    private String goods_id;

    /** 商品名称 */
    private String goods_name;

    /** 商品数量 */
    private String quantity;

    /** 商品单价，单位：元 */
    private String price;

    /** 商品描述 */
    private String body;

    /** 最晚付款时间 */
    private String timeout_express;
}
