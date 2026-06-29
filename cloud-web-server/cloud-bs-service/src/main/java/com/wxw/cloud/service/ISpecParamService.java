package com.wxw.cloud.service;

import com.wxw.cloud.domain.SpecGroup;
import com.wxw.cloud.domain.SpecParam;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * <p>
 * 规格参数组下的参数名 服务类
 * </p>
 *
 * @author twx
 * @since 2026-05-6
 */
public interface ISpecParamService extends IService<SpecParam> {

    /**
     * 根据条件查询规格参数
     * @param gid
     * @return
     */
    List<SpecParam> queryParams(Long gid, Long cid, Boolean generic, Boolean searching);
}
