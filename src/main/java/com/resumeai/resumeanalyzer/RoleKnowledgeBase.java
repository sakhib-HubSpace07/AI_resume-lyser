package com.resumeai.resumeanalyzer;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;

/**
 * Reads every file in resources/roles at startup.
 *
 * File format:
 *   line 1        role name
 *   @icon, @tagline, @order   optional details for the role card
 *   [Category]    starts a group of skills
 *   skill|alias|alias   a required (core) skill
 *   +skill|alias        a nice-to-have (bonus) skill
 */
@Service
public class RoleKnowledgeBase {

    public record RoleInfo(String name, String icon, String tagline) {}

    // names = every lowercase spelling we search for in a resume (display name + aliases)
    public record RoleSkill(String name, List<String> names, String category, boolean core) {}

    public record Role(RoleInfo info, int order, List<RoleSkill> skills) {}

    private final Map<String, Role> roles = new LinkedHashMap<>();

    public RoleKnowledgeBase() throws Exception {
        Resource[] files = new PathMatchingResourcePatternResolver().getResources("classpath:roles/*.txt");
        List<Role> loaded = new ArrayList<>();
        for (Resource file : files) {
            List<String> lines = new ArrayList<>();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    lines.add(line);
                }
            }
            Role role = parse(lines);
            if (role != null) {
                loaded.add(role);
            }
        }
        loaded.sort(Comparator.comparingInt(Role::order));
        for (Role r : loaded) {
            roles.put(r.info().name(), r);
        }
    }

    static Role parse(List<String> lines) {
        String name = null;
        String icon = "";
        String tagline = "";
        int order = 999;
        String category = "General";
        List<RoleSkill> skills = new ArrayList<>();

        for (String raw : lines) {
            String line = raw.replace("\uFEFF", "").trim();
            if (line.isEmpty()) {
                continue;
            }
            if (name == null) {
                name = line;
            } else if (line.startsWith("@icon ")) {
                icon = line.substring(6).trim();
            } else if (line.startsWith("@tagline ")) {
                tagline = line.substring(9).trim();
            } else if (line.startsWith("@order ")) {
                try {
                    order = Integer.parseInt(line.substring(7).trim());
                } catch (NumberFormatException e) {
                    order = 999;
                }
            } else if (line.startsWith("[") && line.endsWith("]")) {
                category = line.substring(1, line.length() - 1).trim();
            } else {
                boolean core = true;
                if (line.startsWith("+")) {
                    core = false;
                    line = line.substring(1).trim();
                }
                String[] parts = line.split("\\|");
                String display = parts[0].trim();
                List<String> names = new ArrayList<>();
                for (String p : parts) {
                    String t = p.trim().toLowerCase();
                    if (!t.isEmpty() && !names.contains(t)) {
                        names.add(t);
                    }
                }
                skills.add(new RoleSkill(display, names, category, core));
            }
        }
        return name == null ? null : new Role(new RoleInfo(name, icon, tagline), order, skills);
    }

    public List<RoleInfo> roleInfos() {
        List<RoleInfo> out = new ArrayList<>();
        for (Role r : roles.values()) {
            out.add(r.info());
        }
        return out;
    }

    public Role role(String name) {
        return name == null ? null : roles.get(name);
    }

    public List<RoleSkill> skillsFor(String name) {
        Role r = role(name);
        return r == null ? List.of() : r.skills();
    }
}