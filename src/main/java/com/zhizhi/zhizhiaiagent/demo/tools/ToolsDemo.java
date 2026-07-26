package com.zhizhi.zhizhiaiagent.demo.tools;

import java.lang.reflect.Method;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.method.MethodToolCallback;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.util.ReflectionUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * spring-ai 定义工具调用
 */
public class ToolsDemo {

    /**
     * 注解式定义工具
     * @return
     */
    @Tool(description = "Get the current date and time in the user's timezone")
    public String getCurrentDateTime() {
        return LocalDateTime.now().atZone(LocaleContextHolder.getTimeZone().toZoneId()).toString();
    }

    /**
     * 注解式定义工具
     * @param time
     */
    @Tool(description = "Set a user alarm for the given time, provided in ISO-8601 format")
    public void setAlarm(String time) {
        LocalDateTime alarmTime = LocalDateTime.parse(time, DateTimeFormatter.ISO_DATE_TIME);
        System.out.println("Alarm set for " + alarmTime);
    }

    /**
     * 编程式定义，通过反射进行获取
     * @param city
     * @return
     */
    String getWeather(String city) {
        // 获取天气的实现逻辑
        return "北京今天晴朗，气温25°C";
    }

    public static void main(String[] args) {
        Method method = ReflectionUtils.findMethod(ToolsDemo.class, "getWeather", String.class);
        ToolCallback toolCallback = MethodToolCallback.builder()
                .toolDefinition(ToolDefinition.builder(method)
                        .description("获取指定城市的当前天气情况")
                        .build())
                .toolMethod(method)
                .toolObject(new ToolsDemo())
                .build();
    }
}
