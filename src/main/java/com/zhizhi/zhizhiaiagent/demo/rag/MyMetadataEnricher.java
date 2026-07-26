package com.zhizhi.zhizhiaiagent.demo.rag;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.KeywordMetadataEnricher;
import org.springframework.ai.transformer.SummaryMetadataEnricher;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/**
 * 自定义 MetadataEnricher，可以添加各种不同的 MetadataEnricher 来丰富文档的元信息
 */
@Component
public class MyMetadataEnricher {
    private final ChatModel chatModel;

    public MyMetadataEnricher(ChatModel dashscopeChatModel) {
        this.chatModel = dashscopeChatModel;
    }

    /**
     * 使用 KeywordMetadataEnricher 来提取关键词并添加到文档的 metadata 中
     * @param documents
     * @return
     */
    public List<Document> keywordMetadataEnricher(List<Document> documents){
        KeywordMetadataEnricher keywordMetadataEnricher = new KeywordMetadataEnricher(chatModel,3);
        List<Document> keywordsDocuments = keywordMetadataEnricher.apply(documents);
        return keywordsDocuments;
    }

    /**
     * 使用 summaryMetadataEnricher 来提取摘要并添加到文档的 metadata 中
     * @param documents
     * @return
     */
    public List<Document> summaryMetadataEnricher(List<Document> documents){
        SummaryMetadataEnricher summaryMetadataEnricher = new SummaryMetadataEnricher(chatModel,
                Arrays.asList(SummaryMetadataEnricher.SummaryType.PREVIOUS,
                SummaryMetadataEnricher.SummaryType.CURRENT, SummaryMetadataEnricher.SummaryType.NEXT));
        List<Document> keywordsDocuments = summaryMetadataEnricher.apply(documents);
        return keywordsDocuments;
    }
}
