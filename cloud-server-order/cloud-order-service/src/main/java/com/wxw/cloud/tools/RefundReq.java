package com.wxw.cloud.tools;

import lombok.Data;

@Data
public class RefundReq {

    /** 订单编号 */
    private String out_trade_no;

    /** 支付宝交易号，和订单编号二选一 */
    private String trade_no;

    /** 退款原因 */
    private String refund_reason;

    /** 退款金额，单位：元 */
    private String refund_amount;

    /** 一次退款请求标识，同一笔交易多次退款时必须唯一 */
    private String out_request_no;
}
