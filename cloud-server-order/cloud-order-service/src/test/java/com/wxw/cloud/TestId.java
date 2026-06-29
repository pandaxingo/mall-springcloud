package com.wxw.cloud;

import cn.hutool.core.lang.Snowflake;
import cn.hutool.core.util.IdUtil;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * @author twx
 * @create:   2026-5-16
 */
@SpringBootTest
public class TestId {

    @Test
    public void testId(){
        String idStr = IdUtil.objectId().toUpperCase();
        System.out.println("id = " + idStr);
    }
}
