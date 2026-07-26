package com.zhizhi.zhizhiaiagent.tools;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest
public class FileOperationToolTest {

    @Test
    public void testReadFile() {
        FileOperationTool tool = new FileOperationTool();
        String fileName = "小李恋爱大师.txt";
        String result = tool.readFile(fileName);
        assertNotNull(result);
    }

    @Test
    public void testWriteFile() {
        FileOperationTool tool = new FileOperationTool();
        String fileName = "小李恋爱大师.txt";
        String content = "hello 这是小李恋爱大师的测试内容。";
        String result = tool.writeFile(fileName, content);
        assertNotNull(result);
    }
}
