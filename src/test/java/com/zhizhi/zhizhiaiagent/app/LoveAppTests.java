package com.zhizhi.zhizhiaiagent.app;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.UUID;


@SpringBootTest
class LoveAppTests {

    @Resource
    private LoveApp loveApp;

    @Test
    void testChat() {
        String chatId = UUID.randomUUID().toString();
        // 第一轮
        String message = "你好，我是程序员小李";
        String answer = loveApp.doChat(message, chatId);
        Assertions.assertNotNull(answer);
        // 第二轮
        message = "我想让另一半（小苌）更爱我";
        answer = loveApp.doChat(message, chatId);
        Assertions.assertNotNull(answer);
        // 第三轮
        message = "我的另一半叫什么来着？刚跟你说过，帮我回忆一下";
        answer = loveApp.doChat(message, chatId);
        Assertions.assertNotNull(answer);
    }



    @Test
    void testLogChat() {
        String chatId = UUID.randomUUID().toString();
        String message = "你好，你是谁";
        String answer = loveApp.doChat(message, chatId);
        Assertions.assertNotNull(answer);

    }

    @Test
    void testdoChatWithReport() {
        String chatId = UUID.randomUUID().toString();
        String message = "你好，我是小李，现在正处于恋爱中，请给我一些促进关系发展的建议";
        LoveApp.LoveReport loveReport = loveApp.doChatWithReport(message, chatId);
        Assertions.assertNotNull(loveReport);

    }

//    @Test
//    void doRag() {
//        String chatId = UUID.randomUUID().toString();
//        String message = "我已经结婚了，但是婚后关系不太亲密，怎么办？";
//        ChatResponse chatResponse = loveApp.doChatRag(message, chatId);
//        System.out.println(chatResponse.getResult().getOutput().getText());
//        Assertions.assertNotNull(chatResponse);
//    }

    @Test
    void doChatCloudWithRag() {
        String chatId = UUID.randomUUID().toString();
        String message = "我已经结婚了，但是婚后关系不太亲密，怎么办？";
        String content = loveApp.doChatWithRag(message, chatId);
        Assertions.assertNotNull(content);
    }

    @Test
    void doChatWithTools() {
        // 测试联网搜索问题的答案
        testMessage("周末想带女朋友去上海约会，推荐几个适合情侣的小众打卡地？");

        // 测试网页抓取：恋爱案例分析
        testMessage("最近和对象吵架了，看看编程导航网站（codefather.cn）的其他情侣是怎么解决矛盾的？");

        // 测试资源下载：图片下载
        testMessage("直接下载一张适合做手机壁纸的星空情侣图片为文件");

        // 测试终端操作：执行代码
        testMessage("执行 Python3 脚本来生成数据分析报告");

        // 测试文件操作：保存用户档案
        testMessage("保存我的恋爱档案为文件");

        // 测试 PDF 生成
        testMessage("生成一份‘七夕约会计划’PDF，包含餐厅预订、活动流程和礼物清单");
    }

    private void testMessage(String message) {
        String chatId = UUID.randomUUID().toString();
        String answer = loveApp.doChatWithRag(message, chatId);
        Assertions.assertNotNull(answer);
    }


    @Test
    void doChatWithMcp() {
        String chatId = UUID.randomUUID().toString();
        // 测试地图 MCP
        String message = "我的另一半居住在西安曲江区，请帮我找到 10 公里内合适的约会地点";
        String answer =  loveApp.doChatWithMcp(message, chatId);
    }


    @Test
    void doChatWithMcpTest() {
        String chatId = UUID.randomUUID().toString();
        // 测试地图 MCP
        String message = "我的另一半居住在西安曲江区，请帮我找到 10 公里内合适的约会地点";
        String answer =  loveApp.doChatWithMcpTest(message, chatId);
    }

    @Test
    void doChatWithMcpImage() {
        String chatId = UUID.randomUUID().toString();
        // 测试图片搜索 MCP
        String message = "帮我搜索一些哄另一半开心的图片并把图片地址返回给我";
        String answer =  loveApp.doChatWithMcp(message, chatId);
        Assertions.assertNotNull(answer);
    }


}
