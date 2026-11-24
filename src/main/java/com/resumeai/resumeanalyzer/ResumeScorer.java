package com.resumeai.resumeanalyzer;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/**
 * Compares a resume with a target role and explains the result.
 *
 * Every role skill gets one of four statuses:
 *   proven   found in an experience or project (best evidence)
 *   listed   found in the resume, but only in a skills list or summary
 *   ai       not found in the text, but the AI inferred it from the experience
 *   missing  no evidence at all
 */
@Service
public class ResumeScorer {

    public record Related(String skill, int pct) {}

    public record SkillResult(String name, String category, boolean core, String status,
                              String evidence, String related, int relatedPct) {}

    public record CategoryScore(String name, int percent, int have, int total) {}

    public record Dimension(String label, int percent, int weight, String note) {}

    public record Analysis(int overall, String level, String verdict, List<SkillResult> skills,
                           List<CategoryScore> categories, List<Dimension> dimensions,
                           int proven, int listed, int aiInferred, int missing) {

        public String radarLabels() {
            return categories.stream().map(CategoryScore::name).collect(Collectors.joining("|"));
        }

        public String radarValues() {
            return categories.stream().map(c -> String.valueOf(c.percent())).collect(Collectors.joining("|"));
        }

        public List<SkillResult> missingCore() {
            return skills.stream().filter(s -> s.status().equals("missing") && s.core()).toList();
        }

        public List<SkillResult> missingBonus() {
            return skills.stream().filter(s -> s.status().equals("missing") && !s.core()).toList();
        }

        // skills that appear in the resume but have no proof in a project or job
        public List<SkillResult> notProven() {
            return skills.stream().filter(s -> s.status().equals("listed") || s.status().equals("ai")).toList();
        }

        public int total() {
            return skills.size();
        }

        // Adds the "related to a skill you already have" hints found by the embedding model
        public Analysis withRelated(Map<String, Related> hints) {
            List<SkillResult> updated = new ArrayList<>();
            for (SkillResult s : skills) {
                Related r = hints.get(s.name());
                if (r != null) {
                    updated.add(new SkillResult(s.name(), s.category(), s.core(), s.status(),
                            s.evidence(), r.skill(), r.pct()));
                } else {
                    updated.add(s);
                }
            }
            return new Analysis(overall, level, verdict, updated, categories, dimensions,
                    proven, listed, aiInferred, missing);
        }
    }

    private record Doc(String label, String text) {}

    private final RoleKnowledgeBase knowledgeBase;
    private final Map<String, Pattern> patterns = new ConcurrentHashMap<>();

    public ResumeScorer(RoleKnowledgeBase knowledgeBase) {
        this.knowledgeBase = knowledgeBase;
    }

    public Analysis analyze(String roleName, ResumeParser.ParsedResume resume,
                            String fullText, List<String> aiSkills) {
        List<RoleKnowledgeBase.RoleSkill> required = knowledgeBase.skillsFor(roleName);
        String all = fullText.toLowerCase();
        String ai = String.join(" ; ", aiSkills).toLowerCase();
        List<Doc> docs = evidenceDocs(resume);

        List<SkillResult> results = new ArrayList<>();
        int proven = 0;
        int listed = 0;
        int aiOnly = 0;
        int missing = 0;
        double earned = 0;
        double possible = 0;

        for (RoleKnowledgeBase.RoleSkill s : required) {
            String status;
            String evidence = "";
            if (matches(all, s.names())) {
                evidence = findEvidence(docs, s.names());
                status = evidence.isEmpty() ? "listed" : "proven";
            } else if (matches(ai, s.names())) {
                status = "ai";
            } else {
                status = "missing";
            }
            switch (status) {
                case "proven" -> proven++;
                case "listed" -> listed++;
                case "ai" -> aiOnly++;
                default -> missing++;
            }
            double weight = s.core() ? 2.0 : 1.0;
            earned += weight * points(status);
            possible += weight;
            results.add(new SkillResult(s.name(), s.category(), s.core(), status, evidence, "", 0));
        }

        int skillsPct = possible == 0 ? 0 : (int) Math.round(earned * 100 / possible);
        int found = proven + listed;
        int evidencePct = found == 0 ? 0 : Math.round(proven * 100f / found);
        int impactPct = resume.bulletTotal() == 0 ? 0
                : Math.min(100, Math.round(resume.bulletsWithNumbers() * 100f / resume.bulletTotal() / 0.6f));
        int structurePct = resume.checks().isEmpty() ? 0
                : Math.round(resume.checksPassed() * 100f / resume.checks().size());

        int overall = (int) Math.round(0.55 * skillsPct + 0.20 * evidencePct + 0.15 * impactPct + 0.10 * structurePct);
        if (required.isEmpty()) {
            overall = 0;
        }

        List<Dimension> dimensions = List.of(
                new Dimension("Skills match", skillsPct, 55,
                        proven + " proven, " + listed + " listed, " + aiOnly + " AI-inferred, " + missing + " missing"),
                new Dimension("Evidence in experience", evidencePct, 20,
                        proven + " of " + found + " matched skills appear in your experience or projects"),
                new Dimension("Impact and numbers", impactPct, 15,
                        resume.bulletsWithNumbers() + " of " + resume.bulletTotal() + " bullet points include numbers"),
                new Dimension("Resume structure", structurePct, 10,
                        resume.checksPassed() + " of " + resume.checks().size() + " health checks passed"));

        return new Analysis(overall, level(overall), verdict(overall), results,
                categories(results), dimensions, proven, listed, aiOnly, missing);
    }

    // ---------- helpers ----------
    private static double points(String status) {
        return switch (status) {
            case "proven" -> 1.0;
            case "listed" -> 0.6;
            case "ai" -> 0.4;
            default -> 0.0;
        };
    }

    static String level(int overall) {
        return overall >= 70 ? "high" : overall >= 50 ? "mid" : "low";
    }

    static String verdict(int overall) {
        if (overall >= 85) {
            return "Excellent match";
        }
        if (overall >= 70) {
            return "Strong match";
        }
        if (overall >= 50) {
            return "Fair match, with some gaps";
        }
        return "Needs work for this role";
    }

    private static List<CategoryScore> categories(List<SkillResult> results) {
        Map<String, double[]> sums = new LinkedHashMap<>();   // {earned, possible, have, total}
        for (SkillResult r : results) {
            double[] a = sums.computeIfAbsent(r.category(), k -> new double[4]);
            double w = r.core() ? 2.0 : 1.0;
            a[0] += w * points(r.status());
            a[1] += w;
            a[2] += r.status().equals("missing") ? 0 : 1;
            a[3] += 1;
        }
        List<CategoryScore> out = new ArrayList<>();
        for (Map.Entry<String, double[]> e : sums.entrySet()) {
            double[] a = e.getValue();
            out.add(new CategoryScore(e.getKey(), (int) Math.round(a[0] * 100 / a[1]), (int) a[2], (int) a[3]));
        }
        return out;
    }

    // Experience and project blocks, used to prove a skill with real work
    private static List<Doc> evidenceDocs(ResumeParser.ParsedResume resume) {
        List<Doc> docs = new ArrayList<>();
        for (ResumeParser.Section s : resume.sections()) {
            if (!s.kind().equals("experience") && !s.kind().equals("projects")) {
                continue;
            }
            for (ResumeParser.Block b : s.blocks()) {
                String label = b.title() + (b.org().isBlank() ? "" : " \u00B7 " + b.org());
                String text = (b.title() + " " + b.org() + " " + String.join(" ", b.bullets())).toLowerCase();
                docs.add(new Doc(label, text));
            }
            String loose = (String.join(" ", s.paragraphs()) + " " + String.join(" ", s.bullets())).trim();
            if (!loose.isEmpty()) {
                docs.add(new Doc(s.title(), loose.toLowerCase()));
            }
        }
        return docs;
    }

    private String findEvidence(List<Doc> docs, List<String> names) {
        for (Doc d : docs) {
            if (matches(d.text(), names)) {
                return d.label();
            }
        }
        return "";
    }

    // Whole-word match, so "java" does not match "javascript" and "sql" does not match "mysql"
    private boolean matches(String haystack, List<String> names) {
        for (String name : names) {
            Pattern p = patterns.computeIfAbsent(name,
                    n -> Pattern.compile("(?<![a-z0-9+#])" + Pattern.quote(n) + "s?(?![a-z0-9+#])"));
            if (p.matcher(haystack).find()) {
                return true;
            }
        }
        return false;
    }
}