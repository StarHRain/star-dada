package com.star.stardada.config;

import org.jetbrains.annotations.NotNull;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.util.concurrent.*;

/**
 * @author 千树星雨
 * @date 2024 年 07 月 04 日
 */
@Configuration
public class ThreadPoolConfig{

    /**
     * 核心线程数
     */
    private static final Integer CORE_POOL_SIZE = 15;

    /**
     * 最大线程数
     */
    private static final Integer MAXIMUM_POOL_SIZE = 15;

    /**
     * 空闲时长
     */
    private static final Integer KEEP_ALIVE_TIME = 60;

    /**
     * 阻塞队列容量
     */
    private static final Integer QUEUE_CAPACITY = 10000;

    @Bean
    public ThreadPoolExecutor threadPoolExecutor(){
        ThreadFactory threadFactory = new ThreadFactory(){
            private int count = 1;

            @Override
            public Thread newThread(@NotNull Runnable r) {
                Thread thread = new Thread(r);
                thread.setName("线程" + count++);
                return thread;
            }
        };

        // 创建一个有界阻塞队列
        BlockingQueue<Runnable> queue = new ArrayBlockingQueue<>(QUEUE_CAPACITY);

        // 创建自定义线程池
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                CORE_POOL_SIZE,
                MAXIMUM_POOL_SIZE,
                KEEP_ALIVE_TIME,
                TimeUnit.SECONDS,
                queue,
                threadFactory,
                new ThreadPoolExecutor.CallerRunsPolicy()
        );

        return executor;
    }

    @PostConstruct
    public void init(){
        //初始化完成后立即启动核心线程
    }
}
