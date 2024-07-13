package com.star.stardada.manager;

import com.zhipu.oapi.ClientV4;
import com.zhipu.oapi.Constants;
import com.zhipu.oapi.service.v4.model.*;
import io.reactivex.Flowable;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * @author 千树星雨
 * @date 2024 年 06 月 29 日
 */
@Component
public class AiManager {

    @Resource
    private ClientV4 clientV4;

    private Float STABLE_TEMPERATURE = 0.05f;

    private Float UNSTABLE_TEMPERATURE = 0.99f;

    /**
     * 通用流式请求（简化消息传递）
     *
     * @param systemMessage
     * @param userMessage
     * @param temperature
     * @return
     */
    public Flowable<ModelData> doStreamRequest(String systemMessage, String userMessage, Float temperature) {
        // 构造请求
        List<ChatMessage> messages = new ArrayList<>();
        ChatMessage systemChatMessage = new ChatMessage(ChatMessageRole.SYSTEM.value(), systemMessage);
        ChatMessage userChatMessage = new ChatMessage(ChatMessageRole.USER.value(), userMessage);
        messages.add(systemChatMessage);
        messages.add(userChatMessage);
        return doStreamRequest(messages, temperature);
    }

    /**
     * 通用流式请求
     * @param messages
     * @param temperature
     * @return
     */
    public Flowable<ModelData> doStreamRequest(List<ChatMessage> messages, Float temperature) {
        ChatCompletionRequest chatCompletionRequest = ChatCompletionRequest.builder()
                .model(Constants.ModelChatGLM4)
                .stream(Boolean.TRUE)
                .temperature(temperature)
                .invokeMethod(Constants.invokeMethod)
                .messages(messages)
                .build();
        ModelApiResponse invokeModelApiResp = clientV4.invokeModelApi(chatCompletionRequest);
        return invokeModelApiResp.getFlowable();
    }

    /**
     * 同步请求（结果不稳定
     * @param systemMessage
     * @param userMessage
     * @return
     */
    public String doSyncUnStableRequest(String systemMessage,String userMessage){
        return doRequest(systemMessage,userMessage,Boolean.FALSE,UNSTABLE_TEMPERATURE);
    }

    /**
     * 同步请求（结果稳定
     * @param systemMessage
     * @param userMessage
     * @return
     */
    public String doSyncStableRequest(String systemMessage,String userMessage){
        return doRequest(systemMessage,userMessage,Boolean.FALSE,STABLE_TEMPERATURE);
    }

    /**
     * 同步请求
     * @param systemMessage
     * @param userMessage
     * @param temperature
     * @return
     */
    public String doSyncRequest(String systemMessage,String userMessage, Float temperature){
        return doRequest(systemMessage,userMessage,Boolean.FALSE,temperature);
    }
    /**
     * 通用请求（简化消息传递
     * @param systemMessage
     * @param userMessage
     * @param isStream
     * @param temperature
     * @return
     */
    public String doRequest(String systemMessage,String userMessage,Boolean isStream, Float temperature){
        ArrayList<ChatMessage> chatMessageList = new ArrayList<>();
        ChatMessage systemChatMessage = new ChatMessage(ChatMessageRole.SYSTEM.value(),
                systemMessage);
        chatMessageList.add(systemChatMessage);
        ChatMessage userChatMessage = new ChatMessage(ChatMessageRole.USER.value(), userMessage);
        chatMessageList.add(userChatMessage);
        return doRequest(chatMessageList,isStream,temperature);
    }

    /**
     * 通用请求
     * @param messages
     * @param isStream
     * @param temperature
     * @return
     */
    public String doRequest(List<ChatMessage> messages, Boolean isStream,Float temperature) {
        ChatCompletionRequest chatCompletionRequest = ChatCompletionRequest.builder()
                .model(Constants.ModelChatGLM4)
                .stream(isStream)
                .temperature(temperature)
                .invokeMethod(Constants.invokeMethod)
                .messages(messages)
                .build();
        ModelApiResponse invokeModelApiResp = clientV4.invokeModelApi(chatCompletionRequest);
        String ans = invokeModelApiResp.getData().getChoices().get(0).getMessage().getContent().toString();
        return ans;
    }
}
