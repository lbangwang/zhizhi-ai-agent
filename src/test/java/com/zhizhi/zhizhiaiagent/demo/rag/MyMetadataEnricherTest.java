package com.zhizhi.zhizhiaiagent.demo.rag;

import com.zhizhi.zhizhiaiagent.rag.DocumentReaderConfig;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Slf4j
class MyMetadataEnricherTest {

    @Resource
    private DocumentReaderConfig documentReaderConfig;

    @Autowired
    private MyMetadataEnricher myMetadataEnricher;

    @Test
    void keywordMetadataEnricher() {
        List<Document> documents = documentReaderConfig.loadMarkdowns();
        List<Document> keywordMetadataEnricher = myMetadataEnricher.keywordMetadataEnricher(documents);
        log.info("keywordMetadataEnricher:{}",keywordMetadataEnricher);
        assertNotNull(keywordMetadataEnricher);
    }

    @Test
    void summaryMetadataEnricher() {
        List<Document> documents = documentReaderConfig.loadMarkdowns();
        List<Document> summaryMetadataEnricher = myMetadataEnricher.summaryMetadataEnricher(documents);
        log.info("summaryMetadataEnricher:{}",summaryMetadataEnricher);
        assertNotNull( summaryMetadataEnricher);
    }
}