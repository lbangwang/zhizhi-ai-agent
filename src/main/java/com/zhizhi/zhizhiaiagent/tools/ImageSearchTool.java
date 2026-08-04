package com.zhizhi.zhizhiaiagent.tools;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 图片搜索（Pexels）。MCP 未启用时作为本地兜底工具，名称与 MCP searchImage 对齐。
 */
public class ImageSearchTool {

    private static final String API_URL = "https://api.pexels.com/v1/search";

    private final String apiKey;

    public ImageSearchTool(String apiKey) {
        this.apiKey = apiKey;
    }

    @Tool(description = "Search images from the web (Pexels). Returns comma-separated image URLs.")
    public String searchImage(@ToolParam(description = "Search query keyword") String query) {
        if (StrUtil.isBlank(query)) {
            return "Please enter a search query.";
        }
        if (!StringUtils.hasText(apiKey)) {
            return "Error: PEXELS_API_KEY 未配置，无法搜索图片";
        }
        try {
            return String.join(",", searchMediumImages(query.trim()));
        } catch (Exception e) {
            return "Error search image: " + e.getMessage();
        }
    }

    private List<String> searchMediumImages(String query) {
        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", apiKey);
        Map<String, Object> params = new HashMap<>();
        params.put("query", query);
        params.put("per_page", 5);
        String response = HttpUtil.createGet(API_URL)
                .addHeaders(headers)
                .form(params)
                .execute()
                .body();
        return JSONUtil.parseObj(response)
                .getJSONArray("photos")
                .stream()
                .map(photoObj -> (JSONObject) photoObj)
                .map(photoObj -> photoObj.getJSONObject("src"))
                .map(photo -> photo.getStr("medium"))
                .filter(StrUtil::isNotBlank)
                .collect(Collectors.toList());
    }
}
