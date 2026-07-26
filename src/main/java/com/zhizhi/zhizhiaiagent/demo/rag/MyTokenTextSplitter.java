package com.zhizhi.zhizhiaiagent.demo.rag;

import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 自定义分词器，TokenTextSplitter 是基于 token 的文本分割器，
 * 可以根据 token 数量来分割文本，适用于需要精确控制文本长度的场景。
 */
@Component
class MyTokenTextSplitter {

    public List<Document> splitDocuments(List<Document> documents) {
        TokenTextSplitter splitter = new TokenTextSplitter();
        return splitter.apply(documents);
    }

    public List<Document> splitCustomized(List<Document> documents) {
        TokenTextSplitter splitter = new TokenTextSplitter(100, 400, 10, 5000, true);
        return splitter.apply(documents);
    }
}
