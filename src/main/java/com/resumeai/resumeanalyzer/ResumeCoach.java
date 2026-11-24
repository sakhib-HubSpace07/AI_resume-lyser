package com.resumeai.resumeanalyzer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

/**
 * The AI resume coach: rewrites weak bullet points (the ones without numbers).
 * It must never invent facts, so it uses placeholders such as [X%] where a number would help.
 */
@Service
public class ResumeCoach {

    public record Suggestion(String original, String improved) {}

    private static final Pattern NUMBERED = Pattern.compile("^\\s*(\\d+)[.)]\\s*(.+)$");

    private final ChatClient chatClient;

    public ResumeCoach(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    public List<Suggestion> improve(String role, List<String> bullets) {
        List<String> clean = bullets.stream().map(String::trim).filter(b -> !b.isEmpty()).limit(3).toList();
        if (clean.isEmpty()) {
            return List.of();
        }

        StringBuilder numbered = new StringBuilder();
        for (int i = 0; i < clean.size(); i++) {
            numbered.append(i + 1).append(". ").append(clean.get(i)).append('\n');
        }

        String answer = chatClient.prompt()
                .system("You are a resume coach for students applying for " + role + " roles. "
                      + "Rewrite each bullet point so it starts with a strong action verb, names the technology "
                      + "and shows the result. NEVER invent numbers, tools or facts that are not in the original. "
                      + "If a number would help, use a placeholder such as [X%] or [N users]. "
                      + "Reply with exactly one rewritten line per bullet, using the same numbering. "
                      + "No extra text.")
                .user("Rewrite these bullet points:\n" + numbered)
                .call()
                .content();

        Map<Integer, String> parsed = new HashMap<>();
        if (answer != null) {
            for (String line : answer.split("\\R")) {
                Matcher m = NUMBERED.matcher(line);
                if (m.find()) {
                    parsed.put(Integer.parseInt(m.group(1)), m.group(2).trim().replaceAll("^\"|\"$", ""));
                }
            }
        }

        List<Suggestion> out = new java.util.ArrayList<>();
        for (int i = 0; i < clean.size(); i++) {
            String improved = parsed.getOrDefault(i + 1, "The AI did not return a suggestion for this bullet point.");
            out.add(new Suggestion(clean.get(i), improved));
        }
        return out;
    }
}