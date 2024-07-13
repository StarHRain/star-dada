package com.star.stardada.scoring;

import com.star.stardada.common.ErrorCode;
import com.star.stardada.exception.BusinessException;
import com.star.stardada.model.entity.App;
import com.star.stardada.model.entity.UserAnswer;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;

/**
 * @author 千树星雨
 * @date 2024 年 06 月 27 日
 */
@Service
public class ScoringStrategyExecutor {

    // 策略列表
    @Resource
    private List<ScoringStrategy> scoringStrategyList;

    public UserAnswer doScore(List<String> choiceList, App app) throws Exception{

        Integer appType = app.getAppType();
        Integer appScoringStrategy = app.getScoringStrategy();
        if(appType ==null||appScoringStrategy==null){
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"应用配置有误，未找到相关策略");
        }
        // 根据注解获取策略
        for(ScoringStrategy strategy: scoringStrategyList){
            if(strategy.getClass().isAnnotationPresent(ScoringStrategyConfig.class)){
                ScoringStrategyConfig scoringStrategyConfig = strategy.getClass().getAnnotation(ScoringStrategyConfig.class);
                if(scoringStrategyConfig.appType()==appType&&scoringStrategyConfig.scoringStrategy()==appScoringStrategy){
                    return strategy.doScore(choiceList,app);
                }
            }
        }

        throw new BusinessException(ErrorCode.SYSTEM_ERROR, "应用配置有误，未找到匹配的策略");
    }
}
