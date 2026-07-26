package com.zhizhi.zhizhiaiagent.advisor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.advisor.api.*;
import org.springframework.ai.chat.model.MessageAggregator;
import reactor.core.publisher.Flux;

/**
 * 自定义日志MyLogAdvisor
 * 建议同时实现同步（即非流式）和流式
 */
@Slf4j
public class MyLogAdvisor implements CallAroundAdvisor, StreamAroundAdvisor {

    /**
     * 处理请求
     * @param advisedRequest
     * @return
     */
    private AdvisedRequest processRequest(AdvisedRequest advisedRequest){
        log.info("advisedRequest {} ", advisedRequest);
        return advisedRequest;
    }

    /**
     * 处理响应
     * @param advisedResponse
     * @return
     */
    private AdvisedResponse processResponse(AdvisedResponse advisedResponse){
        log.info("advisedResponse {} ", advisedResponse);
        return advisedResponse;
    }

    @Override
    public AdvisedResponse aroundCall(AdvisedRequest advisedRequest, CallAroundAdvisorChain chain) {
        //处理请求
        processRequest(advisedRequest);
        //Ai调用
        AdvisedResponse advisedResponse = chain.nextAroundCall(advisedRequest);
        //处理响应
        processResponse(advisedResponse);
        return advisedResponse;
    }

    @Override
    public Flux<AdvisedResponse> aroundStream(AdvisedRequest advisedRequest, StreamAroundAdvisorChain chain) {
        //处理请求
        processRequest(advisedRequest);
        //Ai调用
        Flux<AdvisedResponse> advisedResponse = chain.nextAroundStream(advisedRequest);
        //将响应流失聚合成单个响应进行处理
        return (new MessageAggregator()).aggregateAdvisedResponse(advisedResponse,this::processResponse);
    }

    @Override
    public String getName() {
        return "MyLogAdvisor";
    }

    @Override
    public int getOrder() {
        return 0;
    }
}
