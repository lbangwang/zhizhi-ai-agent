package com.zhizhi.zhizhiaiagent.tools;

import cn.hutool.core.io.FileUtil;
import com.itextpdf.io.font.PdfEncodings;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.zhizhi.zhizhiaiagent.constants.FileConstant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * PDF 生成工具类
 * 使用 iText 库生成 PDF；中文依赖 font-asian 或本机 TTF 回退。
 */
@Slf4j
public class PDFGenerationTool {

    @Tool(description = "Generate a PDF file with given content")
    public String generatePDF(
            @ToolParam(description = "Name of the file to save the generated PDF") String fileName,
            @ToolParam(description = "Content to be included in the PDF") String content) {
        String safeName = StringUtils.hasText(fileName) ? fileName.trim() : "output.pdf";
        if (!safeName.toLowerCase().endsWith(".pdf")) {
            safeName = safeName + ".pdf";
        }
        String fileDir = FileConstant.FILE_SAVE_DIR + "/pdf";
        String filePath = fileDir + "/" + safeName;
        Path path = Path.of(filePath);
        try {
            // 先解析字体，避免写出空/损坏 PDF
            PdfFont font = resolveChineseFont();
            FileUtil.mkdir(fileDir);
            try (PdfWriter writer = new PdfWriter(filePath);
                 PdfDocument pdf = new PdfDocument(writer);
                 Document document = new Document(pdf)) {
                document.setFont(font);
                // CJK 内置字体编码器不接受 BMP 以外码点（如部分 emoji）
                String body = stripNonBmp(content == null ? "" : content);
                for (String line : body.split("\\R", -1)) {
                    document.add(new Paragraph(line.isEmpty() ? " " : line));
                }
            }
            return "PDF generated successfully to: " + path.toAbsolutePath().normalize();
        } catch (Exception e) {
            log.error("generatePDF failed: {}", e.getMessage());
            try {
                Files.deleteIfExists(path);
            } catch (IOException ignored) {
                // ignore cleanup failure
            }
            return "Error generating PDF: " + e.getMessage();
        }
    }

    /**
     * 优先用 iText font-asian 内置 CJK；失败则回退本机常见中文字体。
     */
    private static PdfFont resolveChineseFont() throws IOException {
        try {
            return PdfFontFactory.createFont("STSongStd-Light", "UniGB-UCS2-H");
        } catch (Exception e) {
            log.warn("CJK font STSongStd-Light unavailable ({}), fallback to local TTF", e.getMessage());
        }

        List<String> candidates = new ArrayList<>();
        candidates.add("/System/Library/Fonts/Supplemental/Songti.ttc");
        candidates.add("/System/Library/Fonts/Supplemental/Arial Unicode.ttf");
        candidates.add("/Library/Fonts/Arial Unicode.ttf");
        candidates.add("/System/Library/Fonts/STHeiti Light.ttc");
        candidates.add("C:/Windows/Fonts/msyh.ttc");
        candidates.add("C:/Windows/Fonts/simsun.ttc");
        candidates.add("/usr/share/fonts/truetype/noto/NotoSansCJK-Regular.ttc");
        candidates.add("/usr/share/fonts/opentype/noto/NotoSansCJK-Regular.ttc");

        for (String candidate : candidates) {
            Path p = Path.of(candidate);
            if (!Files.isRegularFile(p)) {
                continue;
            }
            try {
                // ttc 可能需 index；先试默认
                return PdfFontFactory.createFont(candidate, PdfEncodings.IDENTITY_H,
                        PdfFontFactory.EmbeddingStrategy.PREFER_EMBEDDED);
            } catch (Exception ex) {
                log.debug("skip font {}: {}", candidate, ex.getMessage());
            }
        }
        throw new IOException("无法加载中文字体：请确认依赖 font-asian 已加入运行时 classpath，或本机存在 Songti/微软雅黑等字体");
    }

    private static String stripNonBmp(String text) {
        StringBuilder sb = new StringBuilder(text.length());
        text.codePoints().forEach(cp -> {
            if (cp <= 0xFFFF) {
                sb.appendCodePoint(cp);
            } else {
                sb.append(' ');
            }
        });
        return sb.toString();
    }
}
