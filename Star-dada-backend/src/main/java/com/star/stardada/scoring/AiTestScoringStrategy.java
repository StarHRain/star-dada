package com.star.stardada.scoring;

import cn.hutool.crypto.digest.DigestUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.star.stardada.manager.AiManager;
import com.star.stardada.model.dto.question.QuestionAnswerDTO;
import com.star.stardada.model.dto.question.QuestionContentDTO;
import com.star.stardada.model.entity.App;
import com.star.stardada.model.entity.Question;
import com.star.stardada.model.entity.UserAnswer;
import com.star.stardada.service.QuestionService;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author 千树星雨
 * @date 2024 年 06 月 29 日
 */
@ScoringStrategyConfig(appType = 1, scoringStrategy = 1)
public class AiTestScoringStrategy implements ScoringStrategy {

    @Resource
    private QuestionService questionService;

    @Resource
    private AiManager aiManager;

    @Resource
    private RedissonClient redissonClient;

    private static final String AI_ANSWER_LOCK ="AI_ANSWER_LOCK";

    /**
     * AI生成结果缓存（ 用户答案（md5哈希压缩），AI生成结果（JSON)）
     */
    private final Cache<String, String> answerCacheMap = Caffeine.newBuilder().initialCapacity(1024)
            .expireAfterAccess(5L, TimeUnit.MINUTES)
            .build();

    /**
     * 构建缓存 Key
     *
     * @param appid
     * @param choiceStr
     * @return
     */
    private String buildCacheKey(Long appid, String choiceStr) {
        return DigestUtil.md5Hex(appid + ":" + choiceStr);
    }

    /**
     * AI 评分所需的系统消息
     */
    private static final String AI_TEST_SCORING_SYSTEM_MESSAGE = "你是一位严谨的判题专家，我会给你如下信息：\n" +
            "```\n" +
            "应用名称，\n" +
            "【【【应用描述】】】，\n" +
            "题目和用户回答的列表：格式为 [{\"title\": \"题目\",\"answer\": \"用户回答\"}]\n" +
            "```\n" +
            "\n" +
            "请你根据上述信息，按照以下步骤来对用户进行评价：\n" +
            "1. 要求：需要给出一个明确的评价结果，包括评价名称（尽量简短,评价描述的总结）和评价描述（尽量详细，大于 200 字）\n" +
            "2. 严格按照下面的 json 格式输出评价名称和评价描述\n" +
            "```\n" +
            "{\"resultName\": \"评价名称\", \"resultDesc\": \"评价描述\"}\n" +
            "```\n" +
            "3. 返回格式必须为 JSON 对象";

    /**
     * 得到 AI 评分所需的 题目->答案 信息列表
     *
     * @param app
     * @param questionContentDTOList
     * @param choices
     * @return
     */
    public String getAiTestScoringMessage(App app, List<QuestionContentDTO> questionContentDTOList, List<String> choices) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(app.getAppName()).append("\n");
        stringBuilder.append(app.getAppDesc()).append("\n");
        List<QuestionAnswerDTO> questionAnswerDTOList = new ArrayList<>();
        for (int i = 0; i < questionContentDTOList.size(); i++) {
            QuestionAnswerDTO questionAnswerDTO = new QuestionAnswerDTO();
            String title = questionContentDTOList.get(i).getTitle();
            questionAnswerDTO.setTitle(title);
            questionAnswerDTO.setUserAnswer(choices.get(i));
            questionAnswerDTOList.add(questionAnswerDTO);
        }
        stringBuilder.append(JSONUtil.toJsonStr(questionAnswerDTOList)).append("\n");
        return stringBuilder.toString();
    }

    /**
     * AI 打分得到 结果和描述
     *
     * @param choices
     * @param app
     * @return
     * @throws Exception
     */
    @Override
    public UserAnswer doScore(List<String> choices, App app) throws Exception {

        // 命中缓存直接获取
        String choiceStr = JSONUtil.toJsonStr(choices);
        String cacheKey = buildCacheKey(app.getId(), choiceStr);
        String aiResultJson = answerCacheMap.getIfPresent(cacheKey);
        if(StringUtils.isNotBlank(aiResultJson)){
            UserAnswer userAnswer = JSONUtil.toBean(aiResultJson,UserAnswer.class);
            userAnswer.setAppId(app.getId());
            userAnswer.setAppType(app.getAppType());
            userAnswer.setScoringStrategy(app.getScoringStrategy());
            userAnswer.setChoices(choiceStr);
            return userAnswer;
        }

        RLock lock = redissonClient.getLock(AI_ANSWER_LOCK + cacheKey);
        UserAnswer userAnswer = new UserAnswer();
        try{
            boolean isSuccessful = lock.tryLock(3,15,TimeUnit.SECONDS);
            if(isSuccessful){
                Long appId = app.getId();
                Question question = questionService.getOne(
                        Wrappers.lambdaQuery(Question.class).eq(Question::getAppId, appId)
                );
                String questionContent = question.getQuestionContent();
                List<QuestionContentDTO> questionContentDTOList = JSONUtil.toList(questionContent, QuestionContentDTO.class);
                String aiTestScoringMessage = getAiTestScoringMessage(app, questionContentDTOList, choices);
                String result = aiManager.doSyncStableRequest(AI_TEST_SCORING_SYSTEM_MESSAGE, aiTestScoringMessage);

//      截取所需 Json 信息
                int start = result.indexOf("{");
                int end = result.lastIndexOf("}");
                String json = result.substring(start, end + 1);
//        本栋缓存
                answerCacheMap.put(cacheKey,json);

                userAnswer = JSONUtil.toBean(json, UserAnswer.class);
                userAnswer.setAppId(appId);
                userAnswer.setAppType(app.getAppType());
                userAnswer.setChoices(JSONUtil.toJsonStr(choices));
                userAnswer.setScoringStrategy(app.getScoringStrategy());
            }
        }finally {
            if(lock!=null&&lock.isLocked()){
                if(lock.isHeldByCurrentThread()){
                    lock.unlock();
                }
            }
        }
        return userAnswer;
    }
}
