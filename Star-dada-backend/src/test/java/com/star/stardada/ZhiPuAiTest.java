package com.star.stardada;

import com.star.stardada.manager.AiManager;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;

/**
 * @author 千树星雨
 * @date 2024 年 06 月 29 日
 */
@SpringBootTest
public class ZhiPuAiTest {

    @Resource
    AiManager aiManager;

    @Test
    public void test(){
        String anser = aiManager.doRequest("回答1", "是否", Boolean.FALSE, null);
        System.out.println(anser);
    }
}
