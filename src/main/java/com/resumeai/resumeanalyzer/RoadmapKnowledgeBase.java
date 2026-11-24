package com.resumeai.resumeanalyzer;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;

@Service
public class RoadmapKnowledgeBase {

    // One line of a roadmap file: a title, optional keywords, and a description
    public record Entry(String title, List<String> keywords, String detail) {}

    // role -> section name ("advanced", "projects", "career") -> entries
    private final Map<String, Map<String, List<Entry>>> roadmaps = new HashMap<>();

    public RoadmapKnowledgeBase() throws Exception {
        Resource[] files = new PathMatchingResourcePatternResolver()
                .getResources("classpath:roadmaps/*.txt");

        for (Resource file : files) {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {

                String role = reader.readLine();            // first line = role name
                if (role == null || role.isBlank()) {
                    continue;
                }

                Map<String, List<Entry>> sections = new HashMap<>();
                String current = null;
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (line.isEmpty()) {
                        continue;
                    }
                    // A line like [projects] starts a new section
                    if (line.startsWith("[") && line.endsWith("]")) {
                        current = line.substring(1, line.length() - 1).toLowerCase();
                        sections.put(current, new ArrayList<>());
                        continue;
                    }
                    int colon = line.indexOf(':');
                    if (current == null || colon < 0) {
                        continue;
                    }
                    // Left of the colon: "Title | keyword | keyword". Right: description.
                    String[] left = line.substring(0, colon).split("\\|");
                    List<String> keywords = new ArrayList<>();
                    for (int i = 1; i < left.length; i++) {
                        keywords.add(left[i].trim().toLowerCase());
                    }
                    sections.get(current).add(new Entry(
                            left[0].trim(), keywords, line.substring(colon + 1).trim()));
                }
                roadmaps.put(role.trim(), sections);
            }
        }
    }

    public List<Entry> section(String role, String section) {
        return roadmaps.getOrDefault(role, Map.of()).getOrDefault(section, List.of());
    }
}