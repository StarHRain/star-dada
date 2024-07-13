package com.star.stardada;

import java.util.Date;

import com.star.stardada.mapper.UserMapper;
import com.star.stardada.model.entity.Question;
import com.star.stardada.model.entity.User;
import com.star.stardada.service.QuestionService;
import com.star.stardada.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.util.StopWatch;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * @author 千树星雨
 * @date 2024 年 07 月 04 日
 */
@SpringBootTest
public class onceTest {

    @Resource
    private QuestionService questionService;

    @Resource
    private ThreadPoolExecutor threadPoolExecutor;

    @Test
    public void doIntegerUsers() {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        final int INSERT_NUM = 10000;
        //
        int batchSize = 10000;
        int j = 0;
        List<CompletableFuture<Void>> futureList = new ArrayList<>();

        for (int i = 0; i < 100; i++) {
            CopyOnWriteArrayList<Question> questionsList = new CopyOnWriteArrayList<>();
            while (true) {
                Question question = new Question();
                question.setQuestionContent("wwww"+j);
                question.setAppId((long) j);
                question.setUserId(0L);
                questionsList.add(question);
                j++;
                if (j % batchSize == 0) {
                    break;
                }
            }
            //并发执行
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                System.out.println(Thread.currentThread().getName());
                questionService.saveBatch(questionsList, batchSize);
            }, threadPoolExecutor);
            futureList.add(future);
        }
        CompletableFuture.allOf(futureList.toArray(new CompletableFuture[]{})).join();
        stopWatch.stop();
        System.out.println(stopWatch.getTotalTimeMillis());
    }
}
