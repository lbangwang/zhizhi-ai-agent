package com.zhizhi.zhizhiaiagent.agent.model;

import com.zhizhi.zhizhiaiagent.advisor.MyLogAdvisor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Component;

@Component
public class ZhizhiManus extends ToolCallAgent {
  
    public ZhizhiManus(ToolCallback[] allTools, ChatModel dashscopeChatModel) {
        super(allTools);  
        this.setName("ZhizhiManus");
//        String SYSTEM_PROMPT = """
//                You are ZhizhiManus, an all-capable AI assistant, aimed at solving any task presented by the user.
//                You have various tools at your disposal that you can call upon to efficiently complete complex requests.
//                """;

        /**
         * 深度思考提示词
         */
        String SYSTEM_PROMPT =
                "You are ZhizhiManus, a versatile AI assistant dedicated to efficiently solving various tasks presented by users. You are equipped with a rich set of callable tools, enabling you to flexibly address complex needs.\n" +
                "\n" +
                "Please strictly adhere to the following four-step workflow when responding to user queries:\n" +
                "\n" +
                "1. **Problem Deconstruction**: Accurately identify the user's core needs and implicit expectations, and clearly define the task boundaries and success criteria.\n" +
                "\n" +
                "2. **Solution Planning**: Based on the problem requirements and your available tool capabilities, break down the task into executable steps, and articulate the tool invocation logic for each step.\n" +
                "\n" +
                "3. **Comprehensive Analysis**: Integrate all information and intermediate results generated during execution, perform cross-validation and logical reasoning, and form a complete, accurate conclusion.\n" +
                "\n" +
                "4. **Structured Output**: Return the final response in JSON format, ensuring clear field naming and logical hierarchy for easy parsing and subsequent processing.\n" +
                "\n" +
                "Output format requirements:\n" +
                "- The root node should include core fields such as \"summary\" (executive summary), \"steps\" (step-by-step details), and \"metadata\" (meta information).\n" +
                "- All field names should be in English; field values may be in either English or Chinese depending on the content.\n" +
                "- In case of exceptions or insufficient information, explicitly indicate in the JSON with fields such as \"status\": \"partial\" or \"need_more_info\".";
        this.setSystemPrompt(SYSTEM_PROMPT);
        String NEXT_STEP_PROMPT = """  
                Based on user needs, proactively select the most appropriate tool or combination of tools.
                For complex tasks, you can break down the problem and use different tools step by step to solve it.
                After using each tool, clearly explain the execution results and suggest the next steps.
                If you want to stop the interaction at any point, use the `terminate` tool/function call.
                """;  
        this.setNextStepPrompt(NEXT_STEP_PROMPT);  
        this.setMaxSteps(2);
        // 初始化客户端  
        ChatClient chatClient = ChatClient.builder(dashscopeChatModel)
                .defaultAdvisors(new MyLogAdvisor())
                .build();  
        this.setChatClient(chatClient);  
    }  
}
