package com.resumeai.resumeanalyzer;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

/**
 * The RAG knowledge base: one learning resource per skill, stored as embeddings
 * in an in-memory vector store (nothing is written to disk).
 *
 * Retrieval is hybrid: a vector search finds the closest resources by meaning,
 * then an exact key match picks the best one, so "Docker" never returns a Kubernetes lesson.
 */
@Service
public class LearningKnowledgeBase {

    private final VectorStore vectorStore;
    private final Map<String, String> byKey = new HashMap<>();
    private final int size;

    public LearningKnowledgeBase(EmbeddingModel embeddingModel) throws Exception {
        this.vectorStore = SimpleVectorStore.builder(embeddingModel).build();

        List<Document> documents = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                new ClassPathResource("learning/resources.txt").getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) {
                    continue;
                }
                documents.add(new Document(line));
                for (String key : keyTokens(line)) {
                    byKey.putIfAbsent(key, line);
                }
            }
        }
        try {
            vectorStore.add(documents);
        } catch (Exception e) {
            // The exact-key map remains usable when Ollama embeddings are temporarily unavailable.
            org.slf4j.LoggerFactory.getLogger(LearningKnowledgeBase.class)
                    .warn("Could not index learning resources with embeddings; using exact-key lookup", e);
        }
        this.size = documents.size();
    }

    public int size() {
        return size;
    }

    // Returns the best "key: advice" line for a skill, or null if there is none
    public String find(String skill, List<String> names) {
        List<String> candidates = new ArrayList<>();
        try {
            for (Document d : vectorStore.similaritySearch(SearchRequest.builder().query(skill).topK(8).build())) {
                if (d.getText() != null) {
                    candidates.add(d.getText());
                }
            }
        } catch (Exception e) {
            // fall back to the exact key lookup below
        }
        return pickBest(candidates, names, byKey);
    }

    // names.get(0) is the skill's own name, the rest are aliases
    static String pickBest(List<String> candidates, List<String> names, Map<String, String> byKey) {
        String own = names.get(0).toLowerCase();
        // 1. a search result whose key is exactly the skill name
        for (String line : candidates) {
            if (keyTokens(line).contains(own)) {
                return line;
            }
        }
        // 2. direct lookup by the skill name
        if (byKey.containsKey(own)) {
            return byKey.get(own);
        }
        // 3. a search result that matches one of the aliases
        for (String line : candidates) {
            for (String key : keyTokens(line)) {
                if (names.contains(key)) {
                    return line;
                }
            }
        }
        // 4. direct lookup by alias
        for (String n : names) {
            String hit = byKey.get(n.toLowerCase());
            if (hit != null) {
                return hit;
            }
        }
        // 5. the closest search result
        return candidates.isEmpty() ? null : candidates.get(0);
    }

    // "Scikit-learn | sklearn: advice" gives ["scikit-learn", "sklearn"]
    static List<String> keyTokens(String line) {
        int colon = line.indexOf(':');
        List<String> out = new ArrayList<>();
        if (colon > 0) {
            for (String k : line.substring(0, colon).split("\\|")) {
                out.add(k.trim().toLowerCase());
            }
        }
        return out;
    }
}