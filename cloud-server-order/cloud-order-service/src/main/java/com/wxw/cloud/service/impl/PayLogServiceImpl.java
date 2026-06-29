package com.wxw.cloud.service.impl;

import cn.hutool.core.convert.Convert;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wxw.cloud.dao.OrderMapper;
import com.wxw.cloud.dao.OrderStatusMapper;
import com.wxw.cloud.dao.PayLogMapper;
import com.wxw.cloud.domain.Order;
import com.wxw.cloud.domain.OrderStatus;
import com.wxw.cloud.domain.PayLog;
import com.wxw.cloud.service.IPayLogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author twx
 * @since 2026-05-6
 */
@Slf4j
@Service
public class PayLogServiceImpl extends ServiceImpl<PayLogMapper, PayLog> implements IPayLogService {

    @Resource
    private OrderStatusMapper orderStatusMapper;

    @Resource
    private PayLogMapper payLogMapper;

    @Resource
    private OrderMapper orderMapper;

    @Transactional(rollbackFor = Exception.class)
    @Override
    public Boolean updatePayLog(String out_trade_no, Integer trade_status, String trade_no, String total_amount) {
        Long orderId = Convert.toLong(out_trade_no);
        OrderStatus orderStatus = this.orderStatusMapper.selectById(orderId);
        if (orderStatus == null) {
            log.info("订单状态信息不存在，订单号：{}", out_trade_no);
            return false;
        }

        PayLog payLog = this.payLogMapper.selectById(orderId);
        if (payLog == null) {
            payLog = new PayLog();
            payLog.setCreateTime(orderStatus.getCreateTime());
            payLog.setOrderId(orderId);
        }

        Order order = this.orderMapper.selectById(orderId);
        if (order != null && order.getUserId() != null) {
            payLog.setUserId(Convert.toLong(order.getUserId()));
        }
        payLog.setTotalFee(parseAmount(total_amount));
        payLog.setTransactionId(trade_no);
        payLog.setStatus(trade_status);
        payLog.setPayType(1);
        payLog.setPayTime(LocalDateTime.now());
        payLog.setBankType("支付宝沙盒");

        int count;
        if (this.payLogMapper.selectById(orderId) == null) {
            count = this.payLogMapper.insert(payLog);
        } else {
            count = this.payLogMapper.updateById(payLog);
        }

        if (isTradeSuccess(trade_status)) {
            orderStatus.setStatus(2);
            orderStatus.setPaymentTime(payLog.getPayTime());
        } else if (isTradeClosed(trade_status)) {
            orderStatus.setStatus(5);
            orderStatus.setCloseTime(LocalDateTime.now());
        }
        this.orderStatusMapper.updateById(orderStatus);
        return count > 0;
    }

    private boolean isTradeSuccess(Integer tradeStatus) {
        return tradeStatus != null && (tradeStatus == 1 || tradeStatus == 3 || tradeStatus == 4);
    }

    private boolean isTradeClosed(Integer tradeStatus) {
        return tradeStatus != null && tradeStatus == 2;
    }

    private Long parseAmount(String amount) {
        if (amount == null || amount.trim().isEmpty()) {
            return 0L;
        }
        return new BigDecimal(amount).setScale(0, RoundingMode.HALF_UP).longValue();
    }
}
