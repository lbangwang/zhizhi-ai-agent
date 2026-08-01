package com.zhizhi.zhizhiaiagent.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("app_user")
public class UserEntity extends BaseEntity {

    /** 32 位字符串，代码生成，不自增 */
    @TableId(type = IdType.INPUT)
    private String id;

    private String username;

    /** D4 接入鉴权前可先存占位；正式环境应存哈希 */
    private String passwordHash;

    private String nickname;

    /** 1=正常 0=禁用 */
    private Integer status;
}
