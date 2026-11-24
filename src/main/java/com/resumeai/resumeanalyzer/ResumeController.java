package com.resumeai.resumeanalyzer;

import java.util.List;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

@Controller
public class ResumeController {

    private final ResumeParser resumeParser;
    private final SkillExtractor skillExtractor;
    private final RoleKnowledgeBase roleKb;
    private final ResumeScorer scorer;
    private final SemanticMatcher semanticMatcher;
    private final RoadmapGenerator roadmapGenerator;
    private final ResumeCoach resumeCoach;

    public ResumeController(ResumeParser resumeParser,
                            SkillExtractor skillExtractor,
                            RoleKnowledgeBase roleKb,
                            ResumeScorer scorer,
                            SemanticMatcher semanticMatcher,
                            RoadmapGenerator roadmapGenerator,
                            ResumeCoach coach) {
        this.resumeParser = resumeParser;
        this.skillExtractor = skillExtractor;
        this.roleKb = roleKb;
        this.scorer = scorer;
        this.semanticMatcher = semanticMatcher;
        this.roadmapGenerator = roadmapGenerator;
        this.resumeCoach = coach;
    }

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("roles", roleKb.roleInfos());
        return "index";
    }

    // Render a complete results page after analysis.
    @PostMapping({"/analyze", "/upload"})
    public String analyze(@RequestParam("resume") MultipartFile file,
                          @RequestParam("role") String role,
                          Model model) {

        String view = "index";
        model.addAttribute("roles", roleKb.roleInfos());
        model.addAttribute("selectedRole", role);

        if (roleKb.role(role) == null) {
            model.addAttribute("error", "Please choose one of the target roles.");
            return view;
        }
        if (file.isEmpty()) {
            model.addAttribute("error", "Please choose a PDF file.");
            return view;
        }
        if (file.getSize() > 5L * 1024 * 1024) {
            model.addAttribute("error", "The PDF must be 5 MB or smaller.");
            return view;
        }
        String originalName = file.getOriginalFilename();
        if (originalName == null || !originalName.toLowerCase(java.util.Locale.ROOT).endsWith(".pdf")) {
            model.addAttribute("error", "Please upload a file with a .pdf extension.");
            return view;
        }

        try (PDDocument document = Loader.loadPDF(file.getBytes())) {
            String text = new PDFTextStripper().getText(document);
            int pages = document.getNumberOfPages();

            // 1. Structure the resume with rules
            ResumeParser.ParsedResume resume = resumeParser.parse(text, pages);

            // 2. Skills: written by the candidate + found by the local AI
            SkillExtractor.Extracted extracted = skillExtractor.extract(resume, text);

            // 3. Compare with the role (evidence levels and weighted score)
            ResumeScorer.Analysis analysis = scorer.analyze(role, resume, text, extracted.aiFound());

            // 4. Semantic hints: missing skills that are close to skills you already have
            List<String> missingNames = analysis.skills().stream()
                    .filter(s -> s.status().equals("missing")).map(ResumeScorer.SkillResult::name).toList();
            analysis = analysis.withRelated(semanticMatcher.relatedSkills(missingNames, extracted.all()));

            // 5. Roadmap (RAG plus a short AI summary)
            RoadmapGenerator.Roadmap roadmap = roadmapGenerator.generate(role, analysis);

            // 6. Weak bullet points for the AI coach tab
            List<String> weak = ResumeParser.weakBullets(resume, 3);

            List<ResumeScorer.SkillResult> matched = analysis.skills().stream()
                    .filter(skill -> !skill.status().equals("missing")).toList();
            List<ResumeScorer.SkillResult> missing = analysis.skills().stream()
                    .filter(skill -> skill.status().equals("missing")).toList();

            model.addAttribute("fileName", originalName.replace("\r", "").replace("\n", ""));
            model.addAttribute("score", analysis.overall());
            model.addAttribute("level", analysis.level());
            model.addAttribute("verdict", analysis.verdict());
            model.addAttribute("matched", matched);
            model.addAttribute("missing", missing);
            model.addAttribute("skills", extracted.all());
            model.addAttribute("resume", resume);
            model.addAttribute("extracted", extracted);
            model.addAttribute("analysis", analysis);
            model.addAttribute("roadmap", roadmap);
            model.addAttribute("weakBullets", weak);
            model.addAttribute("weakBulletsText", String.join("\n", weak));
            // Private-use bullet symbols show as empty boxes, so swap them for a normal bullet
            model.addAttribute("text", text.replaceAll("[\\uE000-\\uF8FF]", "\u2022"));
        } catch (Exception e) {
            // Keep internal parser/model details out of the page; log the exception server-side.
            org.slf4j.LoggerFactory.getLogger(ResumeController.class)
                    .error("Resume analysis failed", e);
            model.addAttribute("error", "We could not analyze this PDF. Check that it is a valid, text-readable PDF and try again.");
        }
        return view;
    }

    // Called asynchronously by the AI coach panel. Nothing is stored.
    @PostMapping("/coach")
    public String coach(@RequestParam("bullets") String bullets,
                        @RequestParam(value = "role", required = false) String role,
                        Model model) {
        String roleName = roleKb.role(role) != null ? role : "your target";
        if (bullets == null || bullets.length() > 5000) {
            model.addAttribute("suggestions", List.of());
            model.addAttribute("coachError", "Please provide up to 5,000 characters of bullet points.");
            return "index :: coach";
        }
        List<String> list = List.of(bullets.split("\\R"));
        try {
            model.addAttribute("suggestions", resumeCoach.improve(roleName, list));
        } catch (Exception e) {
            model.addAttribute("suggestions", List.of());
            org.slf4j.LoggerFactory.getLogger(ResumeController.class)
                    .error("Resume coaching failed", e);
            model.addAttribute("coachError", "The AI coach could not complete this request. Please try again.");
        }
        return "index :: coach";
    }
}