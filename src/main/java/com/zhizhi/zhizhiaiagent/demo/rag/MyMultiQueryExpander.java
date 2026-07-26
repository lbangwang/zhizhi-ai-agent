package com.zhizhi.zhizhiaiagent.demo.rag;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.preretrieval.query.expansion.MultiQueryExpander;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 自定义 MultiQueryExpander，查询扩展
 */
@Component
@Slf4j
public class MyMultiQueryExpander {

    public List<Query> multiQueryExpander(ChatClient.Builder chatClientBuilder){
        MultiQueryExpander queryExpander = MultiQueryExpander.builder()
                .chatClientBuilder(chatClientBuilder)
                .numberOfQueries(3)
                .build();
        List<Query> queries = queryExpander.expand(new Query("恋爱是什么？它是啥感觉？"));

        log.info("queries: {}",queries);
        return queries;
    }

}
