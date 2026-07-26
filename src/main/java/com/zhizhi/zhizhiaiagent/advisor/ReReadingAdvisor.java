package com.zhizhi.zhizhiaiagent.advisor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.advisor.api.*;
import reactor.core.publisher.Flux;

import java.util.HashMap;
import java.util.Map;

/**
 * 自定义 rēadingAdvisor2 即重读
 * 提高模型的推理能力，但同时更消耗请求token
 */
@Slf4j
public class ReReadingAdvisor implements BaseAdvisor {

    private static final String DEFAULT_RE2_ADVISE_TEMPLATE = """
			{re2_input_query}
			Read the question again: {re2_input_query}
			""";

    private final String re2AdviseTemplate;


    public ReReadingAdvisor() {
        this(DEFAULT_RE2_ADVISE_TEMPLATE);
    }

    public ReReadingAdvisor(String re2AdviseTemplate) {
        this.re2AdviseTemplate = re2AdviseTemplate;
    }

    @Override
    public AdvisedResponse aroundCall(AdvisedRequest advisedRequest, CallAroundAdvisorChain chain) {
        before(advisedRequest);
        log.info("ReReadingAdvisor aroundCall advisedRequest: {}", advisedRequest);
        return chain.nextAroundCall(advisedRequest);
    }

    @Override
    public Flux<AdvisedResponse> aroundStream(AdvisedRequest advisedRequest, StreamAroundAdvisorChain chain) {
        before(advisedRequest);
        return chain.nextAroundStream(advisedRequest);
    }

    @Override
    public String getName() {
        return "ReReadingAdvisor";
    }

    @Override
    public AdvisedRequest before(AdvisedRequest advisedRequest) {
        Map<String, Object> advisedUserParams = new HashMap<>(advisedRequest.userParams());
        advisedUserParams.put("re2_input_query", advisedRequest.userText());

        return AdvisedRequest.from(advisedRequest)
                .userText("""
                        {re2_input_query}
                        Read the question again: {re2_input_query}
                        """)
                .userParams(advisedUserParams)
                .build();
    }

    @Override
    public AdvisedResponse after(AdvisedResponse advisedResponse) {
        return advisedResponse;
    }

    @Override
    public int getOrder() {
        return 0;
    }



}
