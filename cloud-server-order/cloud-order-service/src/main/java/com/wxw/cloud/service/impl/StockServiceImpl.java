package com.wxw.cloud.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wxw.cloud.dao.StockMapper;
import com.wxw.cloud.domain.Stock;
import com.wxw.cloud.service.IStockService;
import org.springframework.stereotype.Service;

/**
 * @author twx
 * @create:   2026-5-16
 */
@Service
public class StockServiceImpl extends ServiceImpl<StockMapper, Stock> implements IStockService {
}
