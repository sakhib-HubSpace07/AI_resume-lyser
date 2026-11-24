package com.resumeai.resumeanalyzer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

/**
 * Builds the 4-phase roadmap.
 *   Phase 1  missing core skills            (RAG: best learning resource for each)
 *   Phase 2  missing bonus skills           (RAG)
 *   Phase 3  prove your skills + projects   (project ideas from the roadmap file)
 *   Phase 4  get job-ready                  (career steps from the roadmap file)
 * The AI only writes a short friendly summary, so it cannot invent courses.
 */
@Service
public class RoadmapGenerator {

    public record Step(String title, String detail, String time) {}
    public record Phase(String name, String goal, List<Step> steps) {}
    public record Roadmap(String summary, List<Phase> phases, int learningWeeks) {}

    private static final Pattern TIME = Pattern.compile("\\s*About ([^.]+)\\.\\s*$");
    private static final Pattern TIME_NUMBER = Pattern.compile("(\\d+)\\s*(day|week|month)");
    private static final int MAX_GAP_STEPS = 6;
    private static final int MAX_BONUS_STEPS = 4;

    private final ChatClient chatClient;
    private final LearningKnowledgeBase learningKb;
    private final RoadmapKnowledgeBase roadmapKb;
    private final RoleKnowledgeBase roleKb;

    public RoadmapGenerator(ChatClient.Builder builder,
                            LearningKnowledgeBase learningKb,
                            RoadmapKnowledgeBase roadmapKb,
                            RoleKnowledgeBase roleKb) {
        this.chatClient = builder.build();
        this.learningKb = learningKb;
        this.roadmapKb = roadmapKb;
        this.roleKb = roleKb;
    }

    public Roadmap generate(String role, ResumeScorer.Analysis analysis) {
        Map<String, RoleKnowledgeBase.RoleSkill> byName = new HashMap<>();
        for (RoleKnowledgeBase.RoleSkill s : roleKb.skillsFor(role)) {
            byName.put(s.name(), s);
        }

        // Phase 1: core gaps
        List<ResumeScorer.SkillResult> core = analysis.missingCore();
        List<Step> gapSteps = new ArrayList<>();
        for (ResumeScorer.SkillResult s : core.stream().limit(MAX_GAP_STEPS).toList()) {
            gapSteps.add(skillStep(s, byName.get(s.name())));
        }
        String gapGoal = core.size() > MAX_GAP_STEPS
                ? "Showing the first " + MAX_GAP_STEPS + " of " + core.size()
                  + " missing core skills. The Skill matrix tab lists all of them."
                : "Learn the core skills this role needs that your resume does not show yet.";
        if (gapSteps.isEmpty()) {
            gapSteps.add(new Step("No core gaps found",
                    "Your resume shows every core skill we check for this role. Great work.", ""));
        }

        // Phase 2: bonus skills
        List<Step> bonusSteps = new ArrayList<>();
        for (ResumeScorer.SkillResult s : analysis.missingBonus().stream().limit(MAX_BONUS_STEPS).toList()) {
            bonusSteps.add(skillStep(s, byName.get(s.name())));
        }
        if (bonusSteps.isEmpty()) {
            bonusSteps.add(new Step("You already cover the advanced skills we track",
                    "Go deeper: contribute to an open-source project or read recent research in your area.", ""));
        }

        // Phase 3: prove skills, then projects
        List<Step> projectSteps = new ArrayList<>();
        List<ResumeScorer.SkillResult> notProven = analysis.notProven();
        if (!notProven.isEmpty()) {
            String names = String.join(", ", notProven.stream().limit(4).map(ResumeScorer.SkillResult::name).toList());
            projectSteps.add(new Step("Prove the skills you list",
                    "These skills appear in your resume without proof in a project or job: " + names
                    + ". Use each one in a project and add a bullet point that shows the result.", "1 week"));
        }
        for (RoadmapKnowledgeBase.Entry e : roadmapKb.section(role, "projects")) {
            projectSteps.add(toStep(e.title(), e.detail()));
        }

        // Phase 4: career steps
        List<Step> careerSteps = new ArrayList<>();
        for (RoadmapKnowledgeBase.Entry e : roadmapKb.section(role, "career")) {
            careerSteps.add(toStep(e.title(), e.detail()));
        }

        List<Phase> phases = new ArrayList<>();
        phases.add(new Phase("Close your skill gaps", gapGoal, gapSteps));
        phases.add(new Phase("Level up with advanced skills",
                "Skills that make you stand out from other candidates.", bonusSteps));
        phases.add(new Phase("Prove it with projects",
                "Show recruiters real work they can open on GitHub.", projectSteps));
        phases.add(new Phase("Get job-ready", "Turn your skills into interviews and offers.", careerSteps));
        phases.removeIf(p -> p.steps().isEmpty());

        double weeks = 0;
        for (Step s : gapSteps) {
            weeks += weeksOf(s.time());
        }
        for (Step s : bonusSteps) {
            weeks += weeksOf(s.time());
        }
        return new Roadmap(writeSummary(role, analysis), phases, (int) Math.ceil(weeks));
    }

    // One step for one skill, using the best resource from the vector store
    private Step skillStep(ResumeScorer.SkillResult skill, RoleKnowledgeBase.RoleSkill roleSkill) {
        List<String> names = roleSkill == null ? List.of(skill.name().toLowerCase()) : roleSkill.names();
        String line = learningKb.find(skill.name(), names);

        Step step;
        if (line == null) {
            step = new Step("Learn " + skill.name(),
                    "Search for the official documentation or a beginner tutorial for " + skill.name()
                    + ", then use it in one small project and add it to your resume.", "2 weeks");
        } else {
            int colon = line.indexOf(':');
            step = toStep("Learn " + skill.name(), colon >= 0 ? line.substring(colon + 1).trim() : line);
        }
        if (!skill.related().isEmpty()) {
            step = new Step(step.title(), step.detail() + " You already know " + skill.related()
                    + ", so this should go faster.", step.time());
        }
        return step;
    }

    private static Step toStep(String title, String text) {
        Matcher m = TIME.matcher(text);
        if (m.find()) {
            return new Step(title, text.substring(0, m.start()).trim(), m.group(1).trim());
        }
        return new Step(title, text, "");
    }

    static double weeksOf(String time) {
        Matcher m = TIME_NUMBER.matcher(time == null ? "" : time);
        if (!m.find()) {
            return 0;
        }
        int n = Integer.parseInt(m.group(1));
        return switch (m.group(2)) {
            case "day" -> n / 7.0;
            case "month" -> n * 4.0;
            default -> n;
        };
    }

    // The AI writes only a short, friendly summary (no courses, books or links)
    private String writeSummary(String role, ResumeScorer.Analysis a) {
        List<String> top = a.missingCore().stream().limit(3).map(ResumeScorer.SkillResult::name).toList();
        try {
            String summary = chatClient.prompt()
                    .system("You are a friendly career mentor for students. "
                          + "Write 2 or 3 short, encouraging sentences in simple English. "
                          + "Do NOT mention any course, book, website or link. "
                          + "Plain text only.")
                    .user("A student wants to become a " + role + ". "
                        + "Their readiness score is " + a.overall() + " out of 100. "
                        + a.proven() + " role skills are proven in their experience, "
                        + a.missing() + " are missing. "
                        + "The most important missing skills: " + (top.isEmpty() ? "none" : String.join(", ", top)) + ".")
                    .call()
                    .content();
            if (summary != null && !summary.isBlank()) {
                return summary.trim();
            }
        } catch (Exception e) {
            org.slf4j.LoggerFactory.getLogger(RoadmapGenerator.class)
                    .warn("Could not generate the AI roadmap summary; using the local fallback", e);
        }
        if (top.isEmpty()) {
            return "Your resume covers the core skills tracked for " + role
                    + ". Focus on demonstrating your experience through projects and measurable outcomes.";
        }
        return "You are building toward " + role + ". Prioritize " + String.join(", ", top)
                + ", then demonstrate your progress with practical projects and measurable results.";
    }
}