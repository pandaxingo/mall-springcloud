package com.wxw.cloud.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.api.request.AlipayTradeCloseRequest;
import com.alipay.api.request.AlipayTradeFastpayRefundQueryRequest;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.request.AlipayTradeRefundRequest;
import com.alipay.api.response.AlipayTradeCloseResponse;
import com.alipay.api.response.AlipayTradeFastpayRefundQueryResponse;
import com.alipay.api.response.AlipayTradePagePayResponse;
import com.alipay.api.response.AlipayTradeQueryResponse;
import com.alipay.api.response.AlipayTradeRefundResponse;
import com.wxw.cloud.config.AliPayProperties;
import com.wxw.cloud.domain.Order;
import com.wxw.cloud.domain.OrderDetail;
import com.wxw.cloud.service.AlipayService;
import com.wxw.cloud.service.IPayLogService;
import com.wxw.cloud.tools.PayReq;
import com.wxw.cloud.tools.RefundReq;
import com.wxw.cloud.tools.TradeStatus;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.Iterator;
import java.util.Map;

@Slf4j
@Service
@EnableConfigurationProperties(AliPayProperties.class)
public class AlipayServiceImpl implements AlipayService {

    @Resource
    private AliPayProperties aliPayProperties;

    @Resource
    private IPayLogService payLogService;

    @Override
    public String refund(String orderId, String refundReason, String refundAmount, String reqNo) throws AlipayApiException {
        AlipayClient alipayClient = aliPayProperties.build();
        AlipayTradeRefundRequest alipayRequest = new AlipayTradeRefundRequest();
        RefundReq refundReq = new RefundReq();
        refundReq.setOut_trade_no(orderId);
        refundReq.setRefund_reason(refundReason);
        refundReq.setRefund_amount(refundAmount);
        refundReq.setOut_request_no(reqNo);
        alipayRequest.setBizContent(JSON.toJSONString(refundReq));
        AlipayTradeRefundResponse tradeRefund = alipayClient.execute(alipayRequest);
        if (!tradeRefund.isSuccess()) {
            throw new AlipayApiException(tradeRefund.getSubMsg());
        }
        return tradeRefund.getBody();
    }

    @Override
    public String query(String orderId) throws AlipayApiException {
        AlipayClient alipayClient = aliPayProperties.build();
        AlipayTradeQueryRequest alipayRequest = new AlipayTradeQueryRequest();
        PayReq payReq = new PayReq();
        payReq.setOut_trade_no(orderId);
        alipayRequest.setBizContent(JSON.toJSONString(payReq));
        AlipayTradeQueryResponse tradeQuery = alipayClient.execute(alipayRequest);
        if (!tradeQuery.isSuccess()) {
            throw new AlipayApiException(tradeQuery.getSubMsg());
        }
        updatePayStatusFromQuery(orderId, tradeQuery.getBody());
        return tradeQuery.getBody();
    }

    @Override
    public String close(String orderId) throws AlipayApiException {
        AlipayClient alipayClient = aliPayProperties.build();
        AlipayTradeCloseRequest alipayRequest = new AlipayTradeCloseRequest();
        PayReq payReq = new PayReq();
        payReq.setOut_trade_no(orderId);
        alipayRequest.setBizContent(JSON.toJSONString(payReq));
        AlipayTradeCloseResponse closeResponse = alipayClient.execute(alipayRequest);
        if (!closeResponse.isSuccess()) {
            throw new AlipayApiException(closeResponse.getSubMsg());
        }
        return closeResponse.getBody();
    }

    @Override
    public String refundQuery(String orderId, String reqNo) throws AlipayApiException {
        AlipayClient alipayClient = aliPayProperties.build();
        AlipayTradeFastpayRefundQueryRequest alipayRequest = new AlipayTradeFastpayRefundQueryRequest();
        RefundReq refundReq = new RefundReq();
        refundReq.setOut_trade_no(orderId);
        refundReq.setOut_request_no(reqNo);
        alipayRequest.setBizContent(JSON.toJSONString(refundReq));
        AlipayTradeFastpayRefundQueryResponse refundQuery = alipayClient.execute(alipayRequest);
        if (!refundQuery.isSuccess()) {
            throw new AlipayApiException("退款查询发生异常");
        }
        return refundQuery.getBody();
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public String getAliPayClient(Order order, OrderDetail orderDetail, String returnUrl) throws Exception {
        if (order == null || order.getOrderId() == null || orderDetail == null) {
            throw new AlipayApiException("订单信息为空，无法发起支付");
        }

        PayReq payReq = new PayReq();
        payReq.setProduct_code("FAST_INSTANT_TRADE_PAY");
        payReq.setOut_trade_no(order.getOrderId().toString());
        payReq.setSubject(buildPaySubject(orderDetail));
        payReq.setBody("用户订购商品数量: " + (orderDetail.getNum() == null ? 1 : orderDetail.getNum()));
        payReq.setTotal_amount(formatPayAmount(order.getActualPay()));
        payReq.setTimeout_express("30m");

        AlipayTradePagePayRequest alipayRequest = new AlipayTradePagePayRequest();
        alipayRequest.setReturnUrl(StringUtils.isBlank(returnUrl) ? aliPayProperties.getReturnUrl() : returnUrl);
        if (StringUtils.isNotBlank(aliPayProperties.getNotifyUrl())) {
            alipayRequest.setNotifyUrl(aliPayProperties.getNotifyUrl());
        }
        alipayRequest.setBizContent(JSON.toJSONString(payReq));

        log.info("支付宝沙盒支付入参：{}", JSON.toJSONString(payReq));
        AlipayTradePagePayResponse pagePay = aliPayProperties.build().pageExecute(alipayRequest);
        if (!pagePay.isSuccess()) {
            throw new AlipayApiException(pagePay.getSubMsg());
        }
        return pagePay.getBody();
    }

    @Override
    public Boolean getReturnUrl(HttpServletRequest request, Map<String, String> params, Map<String, String[]> requestParams) {
        try {
            fillParams(params, requestParams, true);
            boolean signVerified = AlipaySignature.rsaCheckV1(
                    params,
                    aliPayProperties.getPublicKey(),
                    aliPayProperties.getCharset(),
                    aliPayProperties.getSignType()
            );
            if (!signVerified) {
                log.info("支付宝同步通知验签失败");
                return false;
            }
            String outTradeNo = getUtf8Parameter(request, "out_trade_no");
            String tradeNo = getUtf8Parameter(request, "trade_no");
            String totalAmount = getUtf8Parameter(request, "total_amount");
            this.payLogService.updatePayLog(outTradeNo, 3, tradeNo, totalAmount);
            return true;
        } catch (Exception e) {
            log.info("支付宝同步通知处理异常：{}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    public Boolean getNotifyUrl(HttpServletRequest request, Map<String, String> params, Map<String, String[]> requestParams) throws Exception {
        try {
            fillParams(params, requestParams, false);
            boolean signVerified = AlipaySignature.rsaCheckV1(
                    params,
                    aliPayProperties.getPublicKey(),
                    aliPayProperties.getCharset(),
                    aliPayProperties.getSignType()
            );
            if (!signVerified) {
                log.info("支付宝异步通知验签失败");
                return false;
            }
            String outTradeNo = getUtf8Parameter(request, "out_trade_no");
            String tradeNo = getUtf8Parameter(request, "trade_no");
            String tradeStatus = getUtf8Parameter(request, "trade_status");
            String totalAmount = getUtf8Parameter(request, "total_amount");
            if ("TRADE_FINISHED".equals(tradeStatus)) {
                return false;
            }
            if ("TRADE_SUCCESS".equals(tradeStatus)) {
                this.payLogService.updatePayLog(outTradeNo, TradeStatus.getkey(tradeStatus), tradeNo, totalAmount);
            }
            return true;
        } catch (Exception e) {
            log.info("支付宝异步通知处理异常：{}", e.getMessage(), e);
            return false;
        }
    }

    private String buildPaySubject(OrderDetail orderDetail) {
        String title = orderDetail.getTitle();
        if (title == null || title.trim().isEmpty()) {
            title = "商城订单支付";
        }
        return title.length() > 128 ? title.substring(0, 128) : title;
    }

    private void updatePayStatusFromQuery(String orderId, String responseBody) {
        JSONObject root = JSON.parseObject(responseBody);
        JSONObject response = root.getJSONObject("alipay_trade_query_response");
        if (response == null) {
            return;
        }
        String tradeStatus = response.getString("trade_status");
        if (!"TRADE_SUCCESS".equals(tradeStatus) && !"TRADE_FINISHED".equals(tradeStatus)) {
            return;
        }
        String tradeNo = response.getString("trade_no");
        String totalAmount = response.getString("total_amount");
        this.payLogService.updatePayLog(orderId, TradeStatus.getkey(tradeStatus), tradeNo, totalAmount);
    }

    private String formatPayAmount(Long amount) throws AlipayApiException {
        if (amount == null || amount <= 0) {
            throw new AlipayApiException("订单金额必须大于0");
        }
        return new BigDecimal(amount).toPlainString();
    }

    private void fillParams(Map<String, String> params, Map<String, String[]> requestParams, boolean convertEncoding) throws Exception {
        for (Iterator<String> iter = requestParams.keySet().iterator(); iter.hasNext(); ) {
            String name = iter.next();
            String[] values = requestParams.get(name);
            StringBuilder valueStr = new StringBuilder();
            for (int i = 0; i < values.length; i++) {
                valueStr.append(values[i]);
                if (i != values.length - 1) {
                    valueStr.append(",");
                }
            }
            String value = valueStr.toString();
            if (convertEncoding) {
                value = new String(value.getBytes("ISO-8859-1"), "UTF-8");
            }
            params.put(name, value);
        }
    }

    private String getUtf8Parameter(HttpServletRequest request, String name) throws Exception {
        String value = request.getParameter(name);
        if (value == null) {
            return "";
        }
        return new String(value.getBytes("ISO-8859-1"), "UTF-8");
    }
}
