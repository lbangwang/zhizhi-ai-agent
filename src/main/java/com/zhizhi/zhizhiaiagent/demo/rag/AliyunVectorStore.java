package com.zhizhi.zhizhiaiagent.demo.rag;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.rag.DashScopeCloudStore;
import com.alibaba.cloud.ai.dashscope.rag.DashScopeStoreOptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 使用阿里云的向量数据库 DashScopeCloudStore 来存储和查询文档向量。
 */
@Component
@Slf4j
public class AliyunVectorStore {

    @Value("${spring.ai.dashscope.api-key}")
    private String dashScopeApiKey;
    public DashScopeCloudStore getInstance(){
        DashScopeApi dashScopeApi = new DashScopeApi(dashScopeApiKey);
        DashScopeStoreOptions dashScopeStoreOptions = new DashScopeStoreOptions("AI开发面试小助手");
        DashScopeCloudStore dashScopeCloudStore = new DashScopeCloudStore(dashScopeApi,dashScopeStoreOptions);
        List<Document> documents = dashScopeCloudStore.similaritySearch("帮忙说一下MCP");
        log.info("查询结果：{}",documents);
        return dashScopeCloudStore;
    }

}
