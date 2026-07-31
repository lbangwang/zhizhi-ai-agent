package com.zhizhi.zhizhiaiagent.config;

import com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.transaction.TransactionAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * MySQL + MyBatis-Plus 按需启用。
 * <p>
 * 启用方式：.env 中设置 {@code MYSQL_ENABLED=true}，填写连接信息，并执行 {@code db/schema.sql}。
 */
@Configuration
@ConditionalOnProperty(prefix = "app.datasource", name = "enabled", havingValue = "true")
@MapperScan("com.zhizhi.zhizhiaiagent.persistence.mapper")
@ComponentScan(basePackages = "com.zhizhi.zhizhiaiagent.persistence")
@ImportAutoConfiguration({
        DataSourceAutoConfiguration.class,
        DataSourceTransactionManagerAutoConfiguration.class,
        TransactionAutoConfiguration.class,
        MybatisPlusAutoConfiguration.class
})
public class MysqlMybatisConfig {
}
