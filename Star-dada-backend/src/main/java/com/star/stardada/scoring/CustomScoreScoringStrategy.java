package com.star.stardada.scoring;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.star.stardada.model.dto.question.QuestionContentDTO;
import com.star.stardada.model.entity.App;
import com.star.stardada.model.entity.Question;
import com.star.stardada.model.entity.ScoringResult;
import com.star.stardada.model.entity.UserAnswer;
import com.star.stardada.model.vo.QuestionVO;
import com.star.stardada.service.QuestionService;
import com.star.stardada.service.ScoringResultService;

import javax.annotation.Resource;
import java.sql.Wrapper;
import java.util.List;
import java.util.Optional;

/**
 * @author 千树星雨
 * @date 2024 年 06 月 27 日
 */
@ScoringStrategyConfig(appType = 0, scoringStrategy = 0)
public class CustomScoreScoringStrategy implements ScoringStrategy{

    @Resource
    private QuestionService questionService;

    @Resource
    private ScoringResultService scoringResultService;

    @Override
    public UserAnswer doScore(List<String> choices, App app) throws Exception {
        Long appId = app.getId();
        // 1. 根据id查询到题目和题目结果信息（按分数降序排列
        Question question = questionService.getOne(
                Wrappers.lambdaQuery(Question.class).eq(Question::getAppId, appId)
        );
        List<ScoringResult> scoringResultList = scoringResultService.list(
                Wrappers.lambdaQuery(ScoringResult.class)
                        .eq(ScoringResult::getAppId, appId)
                        .orderByDesc(ScoringResult::getResultScoreRange)
        );

        // 2. 遍历用户答案，得到结果分数
        int totalScore = 0;
        QuestionVO questionVO = QuestionVO.objToVo(question);
        List<QuestionContentDTO> questionContent = questionVO.getQuestionContent();

        // 遍历题目
        for(QuestionContentDTO questionContentDTO : questionContent){
//            遍历答案
            for(String answer:choices){
//                答案和选项匹配
                for(QuestionContentDTO.Option option:questionContentDTO.getOptions()){
                    if(option.getKey().equals(answer)){
                        int score = Optional.of(option.getScore()).orElse(0);
                        totalScore += score;
                    }
                }
            }
        }

        // 3. 遍历得分结果，找到最大得分结果
        ScoringResult maxScoringResult = scoringResultList.get(0);
        for(ScoringResult scoringResult:scoringResultList){
            if(totalScore >= scoringResult.getResultScoreRange()){
                maxScoringResult=scoringResult;
                break;
            }
        }

        // 4. 构造返回值，填充答案对象属性
        UserAnswer userAnswer = new UserAnswer();
        userAnswer.setAppId(appId);
        userAnswer.setAppType(app.getAppType());
        userAnswer.setScoringStrategy(app.getScoringStrategy());
        userAnswer.setChoices(JSONUtil.toJsonStr(choices));
        userAnswer.setResultId(maxScoringResult.getId());
        userAnswer.setResultName(maxScoringResult.getResultName());
        userAnswer.setResultDesc(maxScoringResult.getResultDesc());
        userAnswer.setResultPicture(maxScoringResult.getResultPicture());
        userAnswer.setResultScore(maxScoringResult.getResultScoreRange());
        return userAnswer;
    }
}
