package com.zhizhi.zhizhiaiagent.config;

import com.zhizhi.zhizhiaiagent.agent.hitl.HitlApprovalService;
import com.zhizhi.zhizhiaiagent.agent.hitl.HitlContext;
import com.zhizhi.zhizhiaiagent.agent.hitl.HitlGuardedToolCallback;
import com.zhizhi.zhizhiaiagent.tools.FileOperationTool;
import com.zhizhi.zhizhiaiagent.tools.ImageSearchTool;
import com.zhizhi.zhizhiaiagent.tools.PDFGenerationTool;
import com.zhizhi.zhizhiaiagent.tools.ResourceDownloadTool;
import com.zhizhi.zhizhiaiagent.tools.TerminalOperationTool;
import com.zhizhi.zhizhiaiagent.tools.TerminateTool;
import com.zhizhi.zhizhiaiagent.tools.WebScrapingTool;
import com.zhizhi.zhizhiaiagent.tools.WebSearchTool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbacks;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Configuration
public class ToolRegistration {

    @Value("${search-api.api-key:}")
    private String searchApiKey;

    @Value("${app.pexels.api-key:}")
    private String pexelsApiKey;

    @Value("${app.hitl.timeout-seconds:120}")
    private long hitlTimeoutSeconds;

    @Value("${app.mcp.merge-tools:true}")
    private boolean mergeMcpTools;

    @Bean
    public ToolCallback[] allTools(
            HitlApprovalService hitlApprovalService,
            ObjectProvider<SyncMcpToolCallbackProvider> mcpToolCallbackProvider) {
        FileOperationTool fileOperationTool = new FileOperationTool();
        WebSearchTool webSearchTool = new WebSearchTool(searchApiKey);
        WebScrapingTool webScrapingTool = new WebScrapingTool();
        ResourceDownloadTool resourceDownloadTool = new ResourceDownloadTool();
        TerminalOperationTool terminalOperationTool = new TerminalOperationTool();
        PDFGenerationTool pdfGenerationTool = new PDFGenerationTool();
        TerminateTool terminateTool = new TerminateTool();

        List<ToolCallback> tools = new ArrayList<>(Arrays.asList(ToolCallbacks.from(
                fileOperationTool,
                webSearchTool,
                webScrapingTool,
                resourceDownloadTool,
                terminalOperationTool,
                pdfGenerationTool,
                terminateTool
        )));

        Set<String> names = new HashSet<>();
        for (ToolCallback cb : tools) {
            names.add(cb.getToolDefinition().name());
        }

        // 优先合并 MCP 工具；若无 MCP 或无 searchImage，则用本地 Pexels 兜底
        boolean hasSearchImage = false;
        if (mergeMcpTools) {
            SyncMcpToolCallbackProvider mcp = mcpToolCallbackProvider.getIfAvailable();
            if (mcp != null) {
                ToolCallback[] mcpTools = mcp.getToolCallbacks();
                if (mcpTools != null) {
                    for (ToolCallback cb : mcpTools) {
                        String name = cb.getToolDefinition().name();
                        if (names.add(name)) {
                            tools.add(cb);
                            log.info("MCP tool registered: {}", name);
                        }
                        if ("searchImage".equals(name)) {
                            hasSearchImage = true;
                        }
                    }
                }
            }
        }
        if (!hasSearchImage && StringUtils.hasText(pexelsApiKey)) {
            ToolCallback[] localImage = ToolCallbacks.from(new ImageSearchTool(pexelsApiKey));
            tools.addAll(Arrays.asList(localImage));
            log.info("Local ImageSearchTool registered (MCP searchImage absent)");
        }

        //危险工具套 HITL 包装
        List<ToolCallback> guarded = new ArrayList<>(tools.size());
        for (ToolCallback cb : tools) {
            String name = cb.getToolDefinition().name();
            if (HitlContext.isDangerous(name)) {
                guarded.add(new HitlGuardedToolCallback(cb, hitlApprovalService, hitlTimeoutSeconds));
            } else {
                guarded.add(cb);
            }
        }
        return guarded.toArray(ToolCallback[]::new);
    }
}
