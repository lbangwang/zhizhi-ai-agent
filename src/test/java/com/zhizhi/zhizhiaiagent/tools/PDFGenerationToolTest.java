package com.zhizhi.zhizhiaiagent.tools;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
public class PDFGenerationToolTest {

    @Test
    public void testGeneratePDF() {
        PDFGenerationTool tool = new PDFGenerationTool();
        String fileName = "小A.pdf";
        String content = "边境牧羊犬（英文名：Border Collie）是犬科犬属的哺乳动物，别名边境柯利犬。";
        String result = tool.generatePDF(fileName, content);
        assertNotNull(result);
    }
}
