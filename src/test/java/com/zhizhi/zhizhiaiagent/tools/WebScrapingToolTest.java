package com.zhizhi.zhizhiaiagent.tools;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class WebScrapingToolTest {

    @Test
    public void testScrapeWebPage() {
        WebScrapingTool tool = new WebScrapingTool();
        String url = "https://java2ai.com/docs/1.0.0-M6.1/integrations/tools/";
        String result = tool.scrapeWebPage(url);
        assertNotNull(result);
    }
}
