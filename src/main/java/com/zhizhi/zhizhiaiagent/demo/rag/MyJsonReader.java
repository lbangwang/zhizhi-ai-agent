package com.zhizhi.zhizhiaiagent.demo.rag;

import org.springframework.ai.document.Document;
import org.springframework.ai.reader.JsonReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 文档抽取，采用JsonReader读取json文件
 */
@Component
class MyJsonReader {

    private final Resource resource;

    MyJsonReader(@Value("classpath:/file/user.json") Resource resource) {
        this.resource = resource;
    }

    // 基本用法
    List<Document> loadBasicJsonDocuments() {

        JsonReader jsonReader = new JsonReader(this.resource);
        return jsonReader.get();
    }

    // 指定使用哪些 JSON 字段作为文档内容
    List<Document> loadJsonWithSpecificFields() {
        JsonReader jsonReader = new JsonReader(this.resource, "id", "name","email","age");
        return jsonReader.get();
    }

    // 使用 JSON 指针精确提取文档内容
    List<Document> loadJsonWithPointer() {
        JsonReader jsonReader = new JsonReader(this.resource);
        return jsonReader.get("/profile"); // 提取 profile 数组内的内容
    }

}
