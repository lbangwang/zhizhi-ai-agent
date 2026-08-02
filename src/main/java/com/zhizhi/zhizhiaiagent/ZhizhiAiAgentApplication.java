package com.zhizhi.zhizhiaiagent;

import com.zhizhi.zhizhiaiagent.config.DotEnvLoader;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * persistence 包由 {@link com.zhizhi.zhizhiaiagent.config.MysqlMybatisConfig} 在 MYSQL_ENABLED=true 时按需扫描。
 * 未启用 MySQL / Redis 时动态排除对应自动配置，避免启动失败。
 */
@SpringBootApplication
@ComponentScan(
        basePackages = "com.zhizhi.zhizhiaiagent",
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.REGEX,
                pattern = "com\\.zhizhi\\.zhizhiaiagent\\.persistence\\..*"
        )
)
public class ZhizhiAiAgentApplication {

    public static void main(String[] args) {
        DotEnvLoader.load();

        SpringApplication application = new SpringApplication(ZhizhiAiAgentApplication.class);
        List<String> excludes = new ArrayList<>();
        if (!isEnvTrue("MYSQL_ENABLED")) {
            excludes.add(DataSourceAutoConfiguration.class.getName());
            excludes.add(DataSourceTransactionManagerAutoConfiguration.class.getName());
            System.out.println("[Boot] MYSQL_ENABLED!=true, DataSource auto-config excluded");
        } else {
            System.out.println("[Boot] MYSQL_ENABLED=true, DataSource + MyBatis-Plus auto-config enabled");
        }
        if (!isEnvTrue("REDIS_ENABLED")) {
            excludes.add(RedisAutoConfiguration.class.getName());
            excludes.add(RedisRepositoriesAutoConfiguration.class.getName());
            System.out.println("[Boot] REDIS_ENABLED!=true, Redis auto-config excluded");
        } else {
            System.out.println("[Boot] REDIS_ENABLED=true, Redis auto-config enabled");
        }
        if (!excludes.isEmpty()) {
            Map<String, Object> props = new HashMap<>();
            props.put("spring.autoconfigure.exclude", String.join(",", excludes));
            application.setDefaultProperties(props);
        }
        application.run(args);
    }

    private static boolean isEnvTrue(String key) {
        String value = System.getProperty(key);
        if (value == null || value.isBlank()) {
            value = System.getenv(key);
        }
        return value != null && "true".equalsIgnoreCase(value.trim());
    }

}
