package com.zhizhi.zhizhiaiagent.demo.rag;

import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class MyJsonReaderTest {

    @Autowired
    private MyJsonReader myJsonReader;
    @Test
    void loadJsonAsDocuments() {
        List<Document> documents = myJsonReader.loadJsonWithPointer();
        assertNotNull(documents);
    }
}