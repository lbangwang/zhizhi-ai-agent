package com.zhizhi.zhizhiaiagent.tools;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
public class ResourceDownloadToolTest {

    @Test
    public void testDownloadResource() {
        ResourceDownloadTool tool = new ResourceDownloadTool();
        String url = "https://img0.baidu.com/it/u=981972702,3500445350&fm=253&app=138&f=JPEG?w=800&h=1354";
        String fileName = "dog.png";
        String result = tool.downloadResource(url, fileName);
        assertNotNull(result);
    }
}
