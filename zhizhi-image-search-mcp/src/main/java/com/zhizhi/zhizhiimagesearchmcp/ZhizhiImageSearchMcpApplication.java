package com.zhizhi.zhizhiimagesearchmcp;

import com.zhizhi.zhizhiimagesearchmcp.tools.ImageSearchTool;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class ZhizhiImageSearchMcpApplication {

    public static void main(String[] args) {
        SpringApplication.run(ZhizhiImageSearchMcpApplication.class, args);
    }

    @Bean
    public ToolCallbackProvider toolCallbackProvider(ImageSearchTool tool) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(tool)
                .build();
    }

}
