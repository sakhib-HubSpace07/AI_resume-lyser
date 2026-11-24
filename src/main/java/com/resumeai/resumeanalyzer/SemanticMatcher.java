package com.resumeai.resumeanalyzer;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Uses the embedding model (nomic-embed-text) to find skills you already have
 * that are close in meaning to a skill you are missing, for example PyTorch and TensorFlow.
 * This is only a hint. It never changes the score.
 */
@Service
public class SemanticMatcher {

    private final EmbeddingModel embeddingModel;
    private final double threshold;

    public SemanticMatcher(EmbeddingModel embeddingModel,
                           @Value("${app.semantic.threshold:0.72}") double threshold) {
        this.embeddingModel = embeddingModel;
        this.threshold = threshold;
    }

    public Map<String, ResumeScorer.Related> relatedSkills(List<String> missingSkills, List<String> resumePhrases) {
        Map<String, ResumeScorer.Related> out = new LinkedHashMap<>();
        List<String> phrases = resumePhrases.stream().distinct().limit(60).toList();
        if (missingSkills.isEmpty() || phrases.isEmpty()) {
            return out;
        }
        try {
            Map<String, float[]> phraseVectors = new HashMap<>();
            for (String p : phrases) {
                phraseVectors.put(p, embeddingModel.embed(p));
            }
            for (String missing : missingSkills.stream().limit(30).toList()) {
                float[] mv = embeddingModel.embed(missing);
                String best = null;
                double bestSim = -1;
                for (Map.Entry<String, float[]> e : phraseVectors.entrySet()) {
                    if (e.getKey().equalsIgnoreCase(missing)) {
                        continue;
                    }
                    double sim = cosine(mv, e.getValue());
                    if (sim > bestSim) {
                        bestSim = sim;
                        best = e.getKey();
                    }
                }
                if (best != null && bestSim >= threshold) {
                    out.put(missing, new ResumeScorer.Related(best, (int) Math.round(bestSim * 100)));
                }
            }
        } catch (Exception e) {
            // The hints are optional. If the embedding model fails, the analysis still works.
        }
        return out;
    }

    static double cosine(float[] a, float[] b) {
        double dot = 0;
        double na = 0;
        double nb = 0;
        int n = Math.min(a.length, b.length);
        for (int i = 0; i < n; i++) {
            dot += a[i] * b[i];
            na += a[i] * a[i];
            nb += b[i] * b[i];
        }
        return (na == 0 || nb == 0) ? 0 : dot / (Math.sqrt(na) * Math.sqrt(nb));
    }
}