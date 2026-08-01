package com.zhizhi.zhizhiaiagent;

import com.zhizhi.zhizhiaiagent.config.DotEnvLoader;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;

import java.util.HashMap;
import java.util.Map;

/**
 * persistence 包由 {@link com.zhizhi.zhizhiaiagent.config.MysqlMybatisConfig} 在 MYSQL_ENABLED=true 时按需扫描。
 * 未启用 MySQL 时动态排除数据源自动配置，避免启动失败。
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
        if (!isMysqlEnabled()) {
            Map<String, Object> props = new HashMap<>();
            props.put("spring.autoconfigure.exclude", String.join(",",
                    DataSourceAutoConfiguration.class.getName(),
                    DataSourceTransactionManagerAutoConfiguration.class.getName()
            ));
            application.setDefaultProperties(props);
            System.out.println("[Boot] MYSQL_ENABLED!=true, DataSource auto-config excluded");
        } else {
            System.out.println("[Boot] MYSQL_ENABLED=true, DataSource + MyBatis-Plus auto-config enabled");
        }
        application.run(args);
    }

    private static boolean isMysqlEnabled() {
        String value = System.getProperty("MYSQL_ENABLED");
        if (value == null || value.isBlank()) {
            value = System.getenv("MYSQL_ENABLED");
        }
        return value != null && "true".equalsIgnoreCase(value.trim());
    }

}
