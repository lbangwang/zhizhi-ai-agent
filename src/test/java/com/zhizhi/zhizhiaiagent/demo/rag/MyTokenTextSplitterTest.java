package com.zhizhi.zhizhiaiagent.demo.rag;

import com.zhizhi.zhizhiaiagent.rag.DocumentReaderConfig;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class MyTokenTextSplitterTest {

    @Autowired
    private MyTokenTextSplitter myTokenTextSplitter;

    @Resource
    private DocumentReaderConfig documentReaderConfig;

    @Test
    void splitCustomized() {
        List<Document> documents = myTokenTextSplitter.splitCustomized(documentReaderConfig.loadMarkdowns());

        assertNotNull(documents);
    }
}