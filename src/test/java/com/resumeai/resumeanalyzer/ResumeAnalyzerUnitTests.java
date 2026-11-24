package com.resumeai.resumeanalyzer;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;

class ResumeAnalyzerUnitTests {

    @Test
    void roleKnowledgeBaseParsesCoreAndBonusSkills() {
        RoleKnowledgeBase.Role role = RoleKnowledgeBase.parse(List.of(
                "Java Developer",
                "@icon ☕",
                "@tagline Build reliable backend services",
                "@order 1",
                "[Backend]",
                "Java|core java",
                "+Spring Boot|spring"
        ));

        assertNotNull(role);
        assertEquals("Java Developer", role.info().name());
        assertEquals(1, role.order());
        assertEquals(2, role.skills().size());
        assertTrue(role.skills().get(0).core());
        assertTrue(role.skills().get(1).names().contains("spring"));
        assertFalse(role.skills().get(1).core());
    }

    @Test
    void resumeParserExtractsContactAndSections() {
        String text = """
                Jane Doe
                Java Developer
                jane@example.com | github.com/janedoe
                SUMMARY
                Backend developer building reliable services.
                SKILLS
                Java, Spring Boot, PostgreSQL
                EXPERIENCE
                Software Intern - Example Co  Jan 2024 - Jun 2024
                - Built a REST API used by 120 users.
                """;

        ResumeParser.ParsedResume resume = new ResumeParser().parse(text, 1);

        assertEquals("Jane Doe", resume.name());
        assertTrue(resume.contacts().stream().anyMatch(c -> c.type().equals("email")));
        assertTrue(resume.sections().stream().anyMatch(s -> s.kind().equals("skills")));
        assertTrue(resume.sections().stream().anyMatch(s -> s.kind().equals("experience")));
        assertEquals(1, resume.bulletTotal());
        assertEquals(1, resume.bulletsWithNumbers());
    }

    @Test
    void scoreLabelsUseExpectedThresholds() {
        assertEquals("high", ResumeScorer.level(70));
        assertEquals("mid", ResumeScorer.level(50));
        assertEquals("low", ResumeScorer.level(49));
        assertEquals("Excellent match", ResumeScorer.verdict(85));
    }
}
