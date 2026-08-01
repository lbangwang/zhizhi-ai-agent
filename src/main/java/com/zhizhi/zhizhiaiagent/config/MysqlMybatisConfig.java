package com.zhizhi.zhizhiaiagent.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * MySQL + MyBatis-Plus 按需启用（扫描 Mapper / persistence 包）。
 * <p>
 * 数据源与 MyBatis-Plus 由 Spring Boot 自动配置提供；
 * 仅在 {@code MYSQL_ENABLED=true}（即 app.datasource.enabled=true）时生效。
 */
@Configuration
@ConditionalOnProperty(prefix = "app.datasource", name = "enabled", havingValue = "true")
@MapperScan("com.zhizhi.zhizhiaiagent.persistence.mapper")
@ComponentScan(basePackages = "com.zhizhi.zhizhiaiagent.persistence")
public class MysqlMybatisConfig {
}
