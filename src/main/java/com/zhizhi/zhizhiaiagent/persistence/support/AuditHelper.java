package com.zhizhi.zhizhiaiagent.persistence.support;

import com.alibaba.nacos.common.utils.UuidUtils;
import com.zhizhi.zhizhiaiagent.persistence.entity.BaseEntity;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Date;

/**
 * 写入公共审计字段。鉴权接入前 createBy/updateBy 可用 "system"。
 */
public final class AuditHelper {

    public static final String SYSTEM_USER = "system";
    public static final String ENTERPRISE_ID = UuidUtils.generateUuid();

    private AuditHelper() {
    }

    public static void fillOnCreate(BaseEntity entity, String operator, String enterpriseId) {
        Date now = new Date(System.currentTimeMillis());
        String by = StringUtils.hasText(operator) ? operator.trim() : SYSTEM_USER;
        entity.setCreateDate(now);
        entity.setCreateBy(by);
        entity.setUpdateDate(now);
        entity.setUpdateBy(by);
        entity.setIsDel(0);
        entity.setEnterpriseId(ENTERPRISE_ID);
    }

    public static void fillOnUpdate(BaseEntity entity, String operator) {
        entity.setUpdateDate(new Date(System.currentTimeMillis()));
        entity.setUpdateBy(StringUtils.hasText(operator) ? operator.trim() : SYSTEM_USER);
    }

}
