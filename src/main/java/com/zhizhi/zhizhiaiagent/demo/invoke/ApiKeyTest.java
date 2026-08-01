package com.zhizhi.zhizhiaiagent.demo.invoke;

/**
 * Demo 用临时读取；正式配置请使用环境变量 / {@code application.yml} 占位符。
 */
public interface ApiKeyTest {
    String API_KEY_TEST = System.getenv().getOrDefault("DASHSCOPE_API_KEY", "");
}
