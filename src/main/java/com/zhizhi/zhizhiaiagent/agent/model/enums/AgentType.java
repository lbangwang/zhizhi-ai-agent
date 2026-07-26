package com.zhizhi.zhizhiaiagent.agent.model.enums;

public enum AgentType implements BaseEnum<Integer> {

    /**
     * 专业智能体
     */
    PROFESSIONAL(0, "PROFESSIONAL"),

    /**
     * 通用智能体
     */
    COMMON(1, "COMMON");

    private  final Integer code;
    private  final String desc;

    // 构造方法私有化
    AgentType(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }
    @Override
    public Integer getCode() {
        return code;
    }

    @Override
    public String getDesc() {
        return desc;
    }
}
