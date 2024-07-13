package com.star.stardada.scoring;

import com.star.stardada.model.entity.App;
import com.star.stardada.model.entity.UserAnswer;

import java.util.List;

/**
 * 评分策略
 */
public interface ScoringStrategy {
    /**
     * 执行评分
     * @param choices
     * @param app
     * @return
     * @throws Exception
     */
    UserAnswer doScore(List<String> choices, App app) throws Exception;
}
