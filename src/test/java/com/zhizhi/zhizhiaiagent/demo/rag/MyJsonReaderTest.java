package com.zhizhi.zhizhiaiagent.demo.rag;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Slf4j
class MyJsonReaderTest {

    @Autowired
    private MyJsonReader myJsonReader;
    @Test
    void loadJsonAsDocuments() {
//        List<Document> documents = myJsonReader.loadJsonWithPointer();
//        List<Document> documents = myJsonReader.loadJsonWithSpecificFields();
        List<Document> documents = myJsonReader.loadBasicJsonDocuments();
        log.info("文档内容：{}",documents);
        assertNotNull(documents);
    }
}