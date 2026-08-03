package com.zhizhi.zhizhiaiagent.rag;

import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

/**
 * 从知识库上传文件中提取纯文本（支持 md/txt/docx/doc）。
 */
@Component
public class DocumentTextExtractor {

    public String extract(Path file, String originalFilename) {
        String lower = originalFilename == null
                ? ""
                : originalFilename.toLowerCase(Locale.ROOT);
        try {
            if (lower.endsWith(".docx")) {
                return extractDocx(file);
            }
            if (lower.endsWith(".doc")) {
                return extractDoc(file);
            }
            // .md / .markdown / .txt 等按 UTF-8 文本读取
            return Files.readString(file, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("无法解析文档内容: " + e.getMessage());
        }
    }

    private static String extractDocx(Path file) throws Exception {
        try (InputStream in = Files.newInputStream(file);
             XWPFDocument document = new XWPFDocument(in);
             XWPFWordExtractor extractor = new XWPFWordExtractor(document)) {
            return requireText(extractor.getText());
        }
    }

    private static String extractDoc(Path file) throws Exception {
        try (InputStream in = Files.newInputStream(file);
             HWPFDocument document = new HWPFDocument(in);
             WordExtractor extractor = new WordExtractor(document)) {
            return requireText(extractor.getText());
        }
    }

    private static String requireText(String text) {
        if (!StringUtils.hasText(text)) {
            throw new IllegalArgumentException("文件内容为空");
        }
        return text.trim();
    }
}
