package com.wxw.cloud.service.impl;

import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wxw.cloud.dao.OrderDetailMapper;
import com.wxw.cloud.dao.OrderMapper;
import com.wxw.cloud.dao.OrderStatusMapper;
import com.wxw.cloud.dao.SeckillOrderMapper;
import com.wxw.cloud.dao.StockMapper;
import com.wxw.cloud.domain.Order;
import com.wxw.cloud.domain.OrderDetail;
import com.wxw.cloud.domain.OrderStatus;
import com.wxw.cloud.domain.SeckillOrder;
import com.wxw.cloud.domain.Stock;
import com.wxw.cloud.domain.UserInfo;
import com.wxw.cloud.interceptor.LoginOrderInterceptor;
import com.wxw.cloud.result.PageResult;
import com.wxw.cloud.service.IOrderDetailService;
import com.wxw.cloud.service.IOrderService;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * @author twx
 * @since 2026-05-6
 */
@Service
public class OrderServiceImpl extends ServiceImpl<OrderMapper, Order> implements IOrderService {

    private static final Logger LOGGER = LoggerFactory.getLogger(OrderServiceImpl.class);

    @Resource
    private OrderMapper orderMapper;
    @Resource
    private StockMapper stockMapper;
    @Resource
    private OrderStatusMapper orderStatusMapper;
    @Resource
    private IOrderDetailService orderDetailService;
    @Resource
    private SeckillOrderMapper seckillOrderMapper;
    @Resource
    private OrderDetailMapper orderDetailMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createOrder(String tag, Order order) {
        if (order == null || order.getOrderDetails() == null || order.getOrderDetails().isEmpty()) {
            return null;
        }
        UserInfo userinfo = LoginOrderInterceptor.getUserinfo();
        if (userinfo == null || userinfo.getId() == null) {
            return null;
        }
        if (!queryStock(tag, order).isEmpty()) {
            return null;
        }

        long orderId = IdUtil.createSnowflake(1, 1).nextId();
        order.setBuyerNick(userinfo.getUsername());
        order.setBuyerRate(false);
        order.setCreateTime(LocalDateTime.now());
        order.setOrderId(orderId);
        order.setUserId(userinfo.getId().toString());
        this.orderMapper.insert(order);

        OrderStatus orderStatus = new OrderStatus();
        orderStatus.setOrderId(orderId);
        orderStatus.setCreateTime(order.getCreateTime());
        orderStatus.setStatus(1);
        this.orderStatusMapper.insert(orderStatus);

        order.getOrderDetails().forEach(orderDetail -> orderDetail.setOrderId(orderId));
        this.orderDetailService.saveBatch(order.getOrderDetails());

        if (isSeckill(tag)) {
            order.getOrderDetails().forEach(orderDetail -> {
                Stock stock = this.stockMapper.selectById(orderDetail.getSkuId());
                stock.setStock(stock.getStock() - orderDetail.getNum());
                stock.setSeckillStock(stock.getSeckillStock() - orderDetail.getNum());
                this.stockMapper.updateById(stock);

                SeckillOrder seckillOrder = new SeckillOrder();
                seckillOrder.setOrderId(orderId);
                seckillOrder.setSkuId(orderDetail.getSkuId());
                seckillOrder.setUserId(userinfo.getId());
                this.seckillOrderMapper.insert(seckillOrder);
            });
        } else {
            order.getOrderDetails().forEach(orderDetail ->
                    this.stockMapper.reduceStock(orderDetail.getSkuId(), orderDetail.getNum()));
        }
        return orderId;
    }

    @Override
    public List<Long> queryStock(String tag, Order order) {
        List<Long> skuIds = new ArrayList<>();
        if (order == null || order.getOrderDetails() == null || order.getOrderDetails().isEmpty()) {
            return skuIds;
        }
        order.getOrderDetails().forEach(orderDetail -> {
            Stock stock = this.stockMapper.selectById(orderDetail.getSkuId());
            Integer stockNum = stock == null || stock.getStock() == null ? 0 : stock.getStock();
            Integer seckillStockNum = stock == null || stock.getSeckillStock() == null ? 0 : stock.getSeckillStock();
            Integer detailNum = orderDetail.getNum() == null ? 0 : orderDetail.getNum();
            if (stockNum - detailNum < 0 || (isSeckill(tag) && seckillStockNum - detailNum < 0)) {
                skuIds.add(orderDetail.getSkuId());
            }
        });
        return skuIds;
    }

    @Override
    public Order queryOrderById(Long id) {
        Order order = this.orderMapper.selectById(id);
        if (order == null || !isCurrentUserOrder(order)) {
            return null;
        }
        fillOrderDetailAndStatus(order);
        return order;
    }

    @Override
    public PageResult<Order> queryUserOrderList(Integer page, Integer rows, Integer status) {
        try {
            UserInfo userinfo = LoginOrderInterceptor.getUserinfo();
            if (userinfo == null || userinfo.getId() == null) {
                return new PageResult<>(0L, 0L, new ArrayList<>());
            }
            Page<Order> currentPage = new Page<>(page, rows);
            IPage<Order> orderIPage = this.orderMapper.queryOrderPage(currentPage, userinfo.getId(), status);
            if (orderIPage.getRecords() == null || orderIPage.getRecords().isEmpty()) {
                orderIPage = this.orderMapper.queryOrderPage(new Page<>(page, rows), null, status);
            }
            List<Order> orderList = orderIPage.getRecords();
            orderList.forEach(this::fillOrderDetailAndStatus);
            return new PageResult<>(orderIPage.getTotal(), orderIPage.getPages(), orderList);
        } catch (Exception e) {
            LOGGER.error("查询订单出错=>{}", e);
            return null;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateOrderStatus(Long id, Integer status) {
        Order order = this.orderMapper.selectById(id);
        if (order == null || !isCurrentUserOrder(order)) {
            return null;
        }
        OrderStatus currentStatus = this.orderStatusMapper.selectById(id);
        if (currentStatus == null) {
            return null;
        }

        OrderStatus orderStatus = new OrderStatus();
        orderStatus.setOrderId(id);
        orderStatus.setStatus(status);
        switch (status) {
            case 2:
                orderStatus.setPaymentTime(LocalDateTime.now());
                break;
            case 3:
                orderStatus.setConsignTime(LocalDateTime.now());
                break;
            case 4:
                orderStatus.setEndTime(LocalDateTime.now());
                break;
            case 5:
                orderStatus.setCloseTime(LocalDateTime.now());
                break;
            case 6:
                orderStatus.setCommentTime(LocalDateTime.now());
                break;
            default:
                return null;
        }

        int count = this.orderStatusMapper.updateById(orderStatus);
        if (count > 0 && status == 5 && currentStatus.getStatus() != null && currentStatus.getStatus() != 5) {
            restoreOrderStock(id);
        }
        return count > 0;
    }

    @Override
    public List<Long> querySkuIdByOrderId(Long id) {
        Order order = this.orderMapper.selectById(id);
        if (order == null || !isCurrentUserOrder(order)) {
            return new ArrayList<>();
        }
        QueryWrapper<OrderDetail> wrapper = new QueryWrapper<>();
        wrapper.eq("order_id", id);
        List<OrderDetail> details = this.orderDetailMapper.selectList(wrapper);
        List<Long> ids = new ArrayList<>();
        details.forEach(detail -> ids.add(detail.getSkuId()));
        return ids;
    }

    @Override
    public OrderStatus queryOrderStatusById(Long id) {
        Order order = this.orderMapper.selectById(id);
        if (order == null || !isCurrentUserOrder(order)) {
            return null;
        }
        return this.orderStatusMapper.selectById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteOrder(Long id) {
        Order order = this.orderMapper.selectById(id);
        if (order == null || !isCurrentUserOrder(order)) {
            return false;
        }

        OrderStatus orderStatus = this.orderStatusMapper.selectById(id);
        if (orderStatus != null && orderStatus.getStatus() != null && orderStatus.getStatus() == 1) {
            restoreOrderStock(id);
        }

        QueryWrapper<OrderDetail> detailWrapper = new QueryWrapper<>();
        detailWrapper.eq("order_id", id);
        this.orderDetailMapper.delete(detailWrapper);
        this.orderStatusMapper.deleteById(id);
        this.orderMapper.deleteById(id);
        return true;
    }

    private void fillOrderDetailAndStatus(Order order) {
        QueryWrapper<OrderDetail> wrapper = new QueryWrapper<>();
        wrapper.eq("order_id", order.getOrderId());
        List<OrderDetail> orderDetails = this.orderDetailMapper.selectList(wrapper);
        order.setOrderDetails(orderDetails);

        OrderStatus orderStatus = this.orderStatusMapper.selectById(order.getOrderId());
        if (orderStatus != null) {
            order.setOrderStatus(orderStatus.getStatus());
        }
    }

    private boolean isCurrentUserOrder(Order order) {
        UserInfo userinfo = LoginOrderInterceptor.getUserinfo();
        if (userinfo != null && "admin".equals(userinfo.getUsername())) {
            return true;
        }
        return userinfo != null
                && userinfo.getId() != null
                && order.getUserId() != null
                && userinfo.getId().toString().equals(order.getUserId());
    }

    private void restoreOrderStock(Long orderId) {
        QueryWrapper<OrderDetail> detailWrapper = new QueryWrapper<>();
        detailWrapper.eq("order_id", orderId);
        List<OrderDetail> details = this.orderDetailMapper.selectList(detailWrapper);
        details.forEach(detail -> this.stockMapper.restoreStock(detail.getSkuId(), detail.getNum()));
    }

    private boolean isSeckill(String tag) {
        return StringUtils.isNotEmpty(tag) && "seckill".equals(tag);
    }
}
