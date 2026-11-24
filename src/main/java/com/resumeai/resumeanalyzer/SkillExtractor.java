package com.resumeai.resumeanalyzer;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

/**
 * Finds skills in two ways and keeps them apart:
 *   listed   written by the candidate in the skills section (read by rules, 100% accurate)
 *   aiFound  skills the local Llama model spotted in the summary, experience and projects
 */
@Service
public class SkillExtractor {

    public record Extracted(List<String> listed, List<String> aiFound) {
        public List<String> all() {
            List<String> all = new ArrayList<>(listed);
            all.addAll(aiFound);
            return all;
        }
    }

    private final ChatClient chatClient;

    public SkillExtractor(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    public Extracted extract(ResumeParser.ParsedResume resume, String rawText) {
        // 1. Skills written in the skills section
        Map<String, String> listed = new LinkedHashMap<>();
        for (ResumeParser.Section s : resume.sections()) {
            for (ResumeParser.SkillGroup g : s.skillGroups()) {
                for (String k : g.skills()) {
                    listed.putIfAbsent(k.toLowerCase(), k);
                }
            }
        }

        // 2. Ask the AI about the parts of the resume that describe real work
        String input = aiInput(resume);
        if (input.isBlank()) {
            input = rawText;
        }
        if (input.length() > 5000) {
            input = input.substring(0, 5000);
        }
        String answer;
        try {
            answer = chatClient.prompt()
                    .system("You extract technical skills, tools and technologies from resumes. "
                          + "Only list things that clearly appear in the text. Do not guess. "
                          + "Reply with ONLY a comma-separated list. "
                          + "No sentences, no numbering, no explanation.")
                    .user("Resume text:\n" + input)
                    .call()
                    .content();
        } catch (Exception e) {
            // Preserve deterministic skills parsed from the resume if the local chat model is unavailable.
            return new Extracted(new ArrayList<>(listed.values()), List.of());
        }

        Map<String, String> aiFound = new LinkedHashMap<>();
        if (answer != null) {
            for (String piece : answer.split("[,\\n]")) {
                String skill = piece.replaceFirst("^\\s*(?:[-*\\u2022]|\\d+[.)])\\s*", "").trim();
                skill = skill.replaceAll("[.]+$", "");
                if (skill.length() < 2 || skill.length() > 40 || skill.split("\\s+").length > 5) {
                    continue;
                }
                String key = skill.toLowerCase();
                if (!listed.containsKey(key)) {
                    aiFound.putIfAbsent(key, skill);
                }
            }
        }
        return new Extracted(new ArrayList<>(listed.values()), new ArrayList<>(aiFound.values()));
    }

    // Summary, experience and projects as plain text
    private static String aiInput(ResumeParser.ParsedResume resume) {
        StringBuilder sb = new StringBuilder();
        for (ResumeParser.Section s : resume.sections()) {
            if (s.kind().equals("skills")) {
                continue;
            }
            if (!s.kind().equals("summary") && !s.kind().equals("experience") && !s.kind().equals("projects")) {
                continue;
            }
            sb.append(s.title()).append('\n');
            s.paragraphs().forEach(p -> sb.append(p).append('\n'));
            s.bullets().forEach(b -> sb.append("- ").append(b).append('\n'));
            for (ResumeParser.Block b : s.blocks()) {
                sb.append(b.title()).append(' ').append(b.org()).append('\n');
                b.bullets().forEach(x -> sb.append("- ").append(x).append('\n'));
            }
        }
        return sb.toString().trim();
    }
}