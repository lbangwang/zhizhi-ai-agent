package com.zhizhi.zhizhiaiagent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;

/**
 * 默认排除数据源自动配置，避免未部署 MySQL 时启动失败。
 * persistence 包由 {@link com.zhizhi.zhizhiaiagent.config.MysqlMybatisConfig} 在 MYSQL_ENABLED=true 时按需扫描。
 */
@SpringBootApplication(exclude = {
        DataSourceAutoConfiguration.class,
        DataSourceTransactionManagerAutoConfiguration.class
})
@ComponentScan(
        basePackages = "com.zhizhi.zhizhiaiagent",
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.REGEX,
                pattern = "com\\.zhizhi\\.zhizhiaiagent\\.persistence\\..*"
        )
)
public class ZhizhiAiAgentApplication {

    public static void main(String[] args) {
        SpringApplication.run(ZhizhiAiAgentApplication.class, args);
    }

}
