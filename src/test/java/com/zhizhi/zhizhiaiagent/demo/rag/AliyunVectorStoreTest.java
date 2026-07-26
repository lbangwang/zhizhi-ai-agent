package com.zhizhi.zhizhiaiagent.demo.rag;

import com.alibaba.cloud.ai.dashscope.rag.DashScopeCloudStore;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class AliyunVectorStoreTest {
    @Autowired
    private AliyunVectorStore aliyunVectorStore;

    @Test
    void getInstance() {
        DashScopeCloudStore instance = aliyunVectorStore.getInstance();
        assertNotNull( instance);
    }
}