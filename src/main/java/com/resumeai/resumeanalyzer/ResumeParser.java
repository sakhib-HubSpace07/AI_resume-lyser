package com.resumeai.resumeanalyzer;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

/**
 * Turns the raw text from a PDF into a clean structure:
 * name, contacts, sections (summary, skills, experience...) and a health check.
 * It uses simple rules (no AI), so the result is fast and never invented.
 */
@Service
public class ResumeParser {

    // ---------- Small boxes that carry the data to the web page ----------
    public record Contact(String type, String text, String href) {}
    public record SkillGroup(String label, List<String> skills) {}
    public record Block(String title, String org, String date, List<String> bullets) {}
    public record Section(String title, String kind, List<String> paragraphs, List<String> bullets,
                          List<Block> blocks, List<SkillGroup> skillGroups) {}
    public record Check(String label, boolean ok, String note) {}
    public record ParsedResume(String name, String initials, String headline, List<Contact> contacts,
                               List<Section> sections, List<Check> checks, int checksPassed, int wordCount,
                               int bulletTotal, int bulletsWithNumbers) {}

    // ---------- Patterns ----------
    private static final Pattern EMAIL =
            Pattern.compile("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}");
    private static final Pattern LINKEDIN =
            Pattern.compile("(?i)(?:https?://)?(?:www\\.)?linkedin\\.com/[A-Za-z0-9_/%.-]+");
    private static final Pattern GITHUB =
            Pattern.compile("(?i)(?:https?://)?(?:www\\.)?github\\.com/[A-Za-z0-9_.-]+");
    private static final Pattern PHONE = Pattern.compile("\\+?\\(?\\d[\\d\\s().-]{8,}\\d");
    private static final Pattern YEAR = Pattern.compile("\\b(?:19|20)\\d{2}\\b");
    private static final String MONTH =
            "\\b(?:jan|feb|mar|apr|may|jun|jul|aug|sep|sept|oct|nov|dec)[a-z]*\\.?";
    // A date (or date range) at the END of a line, like "Jan 2026 - Jun 2026" or "2021 - Present"
    private static final Pattern DATE_END = Pattern.compile("(?i)((?:" + MONTH + "\\s+)?\\b(?:19|20)\\d{2}"
            + "(?:\\s*[\\u2013\\u2014-]\\s*(?:(?:" + MONTH + "\\s+)?\\b(?:19|20)\\d{2}|present|current|ongoing))?)\\s*$");
    // A spaced dash or pipe between a title and an organisation
    private static final Pattern SEPARATOR = Pattern.compile("\\s[\\u2014\\u2013|-]\\s");
    private static final Pattern SKILL_LINE = Pattern.compile("^([^:]{2,40}):\\s*(.+)$");

    private static final String[] HEADING_WORDS = {
        "summary", "objective", "profile", "skills", "experience", "employment", "internship",
        "projects", "education", "certification", "achievement", "award", "publication", "language",
        "interests", "activities", "volunteer", "leadership", "courses", "training", "references",
        "declaration", "technologies", "about me", "extracurricular", "hobbies"
    };

    // ---------- Main method ----------
    public ParsedResume parse(String rawText, int pages) {
        List<String> lines = new ArrayList<>();
        for (String l : rawText.split("\\R")) {
            String t = l.replace('\u00A0', ' ').replaceAll("\\s+", " ").trim();
            if (!t.isEmpty()) {
                lines.add(t);
            }
        }

        // 1. The header is everything before the first section heading
        int firstHeading = lines.size();
        for (int i = 1; i < lines.size(); i++) {
            if (isHeading(lines.get(i))) {
                firstHeading = i;
                break;
            }
        }
        boolean foundHeadings = firstHeading < lines.size();
        List<String> header = lines.subList(0, foundHeadings ? firstHeading : Math.min(6, lines.size()));

        String name = "";
        String headline = "";
        int next = 0;
        if (!header.isEmpty() && !header.get(0).contains("@") && header.get(0).length() <= 60) {
            name = isAllUpper(header.get(0)) ? titleCase(header.get(0)) : header.get(0);
            next = 1;
        }
        if (header.size() > next) {
            String h = header.get(next);
            if (!h.contains("@") && !h.contains("|") && digitCount(h) < 6 && h.length() <= 80) {
                headline = h;
                next++;
            }
        }
        List<Contact> contacts = extractContacts(header.subList(next, header.size()));

        // 2. Split the rest into sections
        List<Section> sections = new ArrayList<>();
        int i = firstHeading;
        while (i < lines.size()) {
            String title = lines.get(i).replaceAll("[:\\s]+$", "");
            title = isAllUpper(title) ? titleCase(title) : title;
            int j = i + 1;
            while (j < lines.size() && !isHeading(lines.get(j))) {
                j++;
            }
            sections.add(buildSection(title, lines.subList(i + 1, j)));
            i = j;
        }
        if (sections.isEmpty() && !lines.isEmpty()) {
            // No headings found: keep every line so nothing is lost
            sections.add(new Section("Resume content", "other", new ArrayList<>(lines),
                    new ArrayList<>(), new ArrayList<>(), new ArrayList<>()));
        }

        List<Check> checks = buildChecks(contacts, sections, pages);
        int passed = (int) checks.stream().filter(Check::ok).count();
        String trimmed = rawText.trim();
        int words = trimmed.isEmpty() ? 0 : trimmed.split("\\s+").length;

        int[] stats = bulletStats(sections);
        return new ParsedResume(name, initials(name), headline, contacts, sections, checks, passed, words,
                stats[0], stats[1]);
    }

    // ---------- Contacts ----------
    private static List<Contact> extractContacts(List<String> headerLines) {
        String joined = String.join(" | ", headerLines);
        List<Contact> out = new ArrayList<>();

        // location = the first piece that is not an email, phone or link
        for (String piece : joined.split("\\s*[|\\u2022\\u00B7]\\s*")) {
            String p = piece.trim();
            if (p.isEmpty() || p.length() > 60 || p.contains("@") || digitCount(p) >= 7
                    || LINKEDIN.matcher(p).find() || GITHUB.matcher(p).find()) {
                continue;
            }
            out.add(new Contact("location", p, ""));
            break;
        }

        Matcher m = EMAIL.matcher(joined);
        if (m.find()) {
            out.add(new Contact("email", m.group(), "mailto:" + m.group()));
        }
        m = PHONE.matcher(joined);
        while (m.find()) {
            int d = digitCount(m.group());
            if (d >= 10 && d <= 15) {
                out.add(new Contact("phone", m.group().trim(), "tel:" + m.group().replaceAll("[^+\\d]", "")));
                break;
            }
        }
        m = LINKEDIN.matcher(joined);
        if (m.find()) {
            out.add(new Contact("linkedin", shorten(m.group()), toUrl(m.group())));
        }
        m = GITHUB.matcher(joined);
        if (m.find()) {
            out.add(new Contact("github", shorten(m.group()), toUrl(m.group())));
        }
        return out;
    }

    private static String cleanLink(String s) {
        return s.replaceAll("[.\\-/]+$", "");
    }

    private static String toUrl(String s) {
        String c = cleanLink(s);
        return c.toLowerCase().startsWith("http") ? c : "https://" + c;
    }

    private static String shorten(String s) {
        return cleanLink(s).replaceFirst("(?i)^https?://", "").replaceFirst("(?i)^www\\.", "");
    }

    // ---------- Headings ----------
    private static boolean isHeading(String line) {
        String t = line.replaceAll("[:\\s]+$", "");
        if (t.length() < 3 || t.length() > 40 || isBullet(line)) {
            return false;
        }
        if (t.endsWith(".") || t.contains("|") || t.contains("@") || t.contains(":") || t.contains(",")) {
            return false;
        }
        String[] words = t.split("\\s+");
        if (words.length > 4 || !(isAllUpper(t) || isTitleCase(words))) {
            return false;
        }
        String low = t.toLowerCase();
        for (String k : HEADING_WORDS) {
            if (low.contains(k)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isTitleCase(String[] words) {
        for (String w : words) {
            String lw = w.toLowerCase();
            if (lw.equals("and") || lw.equals("of") || lw.equals("&")) {
                continue;
            }
            if (!Character.isUpperCase(w.charAt(0))) {
                return false;
            }
        }
        return true;
    }

    private static String kindOf(String title) {
        String t = title.toLowerCase();
        if (t.contains("summary") || t.contains("objective") || t.contains("profile") || t.contains("about")) {
            return "summary";
        }
        if (t.contains("skill") || t.contains("technolog")) {
            return "skills";
        }
        if (t.contains("project")) {
            return "projects";
        }
        if (t.contains("experience") || t.contains("employment") || t.contains("internship")) {
            return "experience";
        }
        if (t.contains("education")) {
            return "education";
        }
        if (t.contains("certif")) {
            return "certifications";
        }
        return "other";
    }

    // ---------- Bullets ----------
    private static boolean isBullet(String line) {
        if (line.isEmpty()) {
            return false;
        }
        char c = line.charAt(0);
        if ("\u25CF\u2022\u25AA\u25A0\u25E6\u25CB\u2023\u2043\u25AB\u2713\u2714\u27A2\u2192".indexOf(c) >= 0
                || (c >= '\uE000' && c <= '\uF8FF')) {
            return true;
        }
        return (c == '-' || c == '*') && line.length() > 1 && line.charAt(1) == ' ';
    }

    private static String stripBullet(String line) {
        return line.substring(1).trim();
    }

    // ---------- Sections ----------
    private Section buildSection(String title, List<String> body) {
        String kind = kindOf(title);
        int maxLen = 0;
        for (String l : body) {
            maxLen = Math.max(maxLen, l.length());
        }

        // Step A: group the lines into items. H = block heading, B = bullet, P = paragraph
        record Item(char type, StringBuilder text) {}
        List<Item> items = new ArrayList<>();
        String prev = "";
        for (String line : body) {
            Item last = items.isEmpty() ? null : items.get(items.size() - 1);
            if (isBullet(line)) {
                items.add(new Item('B', new StringBuilder(stripBullet(line))));
            } else if (looksLikeBlockHeading(line)) {
                items.add(new Item('H', new StringBuilder(line)));
            } else if (last == null) {
                items.add(new Item('P', new StringBuilder(line)));
            } else if (last.type() == 'H') {
                items.add(new Item('B', new StringBuilder(line)));
            } else if (continues(prev, line, maxLen)) {
                joinInto(last.text(), line);
            } else {
                items.add(new Item('P', new StringBuilder(line)));
            }
            prev = line;
        }

        // Step B: turn the items into the section content
        List<String> paragraphs = new ArrayList<>();
        List<String> loose = new ArrayList<>();
        List<Block> blocks = new ArrayList<>();
        List<SkillGroup> groups = new ArrayList<>();
        List<String> current = null;
        for (Item it : items) {
            String text = it.text().toString().trim();
            if (it.type() == 'H') {
                Block b = makeBlock(text);
                blocks.add(b);
                current = b.bullets();
            } else if (kind.equals("skills")) {
                addSkillLine(groups, text);
            } else if (current != null) {
                current.add(text);
            } else if (it.type() == 'B') {
                loose.add(text);
            } else {
                paragraphs.add(text);
            }
        }
        return new Section(title, kind, paragraphs, loose, blocks, groups);
    }

    // A line like "Role - Company, City  Jan 2025 - Jun 2025" starts a new block
    private static boolean looksLikeBlockHeading(String line) {
        if (isBullet(line) || line.length() > 140) {
            return false;
        }
        boolean hasSeparator = SEPARATOR.matcher(line).find();
        boolean hasDate = YEAR.matcher(line).find() || line.toLowerCase().contains("present");
        return hasSeparator && hasDate;
    }

    // Is this line just the wrapped second half of the previous line?
    private static boolean continues(String prev, String line, int maxLen) {
        char c = line.charAt(0);
        if (Character.isLowerCase(c) || Character.isDigit(c) || "%(&+/,)".indexOf(c) >= 0) {
            return true;
        }
        boolean prevWasLong = prev.length() >= 0.7 * maxLen;
        boolean prevEnded = prev.endsWith(".") || prev.endsWith("!") || prev.endsWith("?");
        return prevWasLong && !prevEnded;
    }

    private static void joinInto(StringBuilder sb, String line) {
        if (sb.length() > 0 && sb.charAt(sb.length() - 1) == '-') {
            sb.append(line);                 // "data-" + "driven" = "data-driven"
        } else {
            sb.append(' ').append(line);
        }
    }

    private static Block makeBlock(String text) {
        String date = "";
        String rest = text;
        Matcher m = DATE_END.matcher(text);
        if (m.find()) {
            date = m.group(1).trim();
            rest = text.substring(0, m.start()).replaceAll("[\\s|,\\u2013\\u2014-]+$", "").trim();
        }
        String title = rest;
        String org = "";
        Matcher s = SEPARATOR.matcher(rest);
        if (s.find()) {
            title = rest.substring(0, s.start()).trim();
            org = rest.substring(s.end()).trim();
        }
        return new Block(title, org, date, new ArrayList<>());
    }

    // ---------- Skills ----------
    private static void addSkillLine(List<SkillGroup> groups, String text) {
        String label = "Skills";
        String values = text;
        Matcher m = SKILL_LINE.matcher(text);
        if (m.find()) {
            label = m.group(1).trim();
            values = m.group(2);
        }
        List<String> skills = new ArrayList<>();
        for (String s : splitTopLevel(values)) {
            String t = s.trim().replaceAll("[.]+$", "");
            if (!t.isEmpty() && t.length() <= 50) {
                skills.add(t);
            }
        }
        if (!skills.isEmpty()) {
            groups.add(new SkillGroup(label, skills));
        }
    }

    // Split on commas, but not inside brackets: "AWS (EC2, S3), Docker" -> 2 items
    private static List<String> splitTopLevel(String s) {
        List<String> out = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        int depth = 0;
        for (char c : s.toCharArray()) {
            if (c == '(' || c == '[') {
                depth++;
            }
            if ((c == ')' || c == ']') && depth > 0) {
                depth--;
            }
            if ((c == ',' || c == ';' || c == '|') && depth == 0) {
                out.add(cur.toString());
                cur.setLength(0);
            } else {
                cur.append(c);
            }
        }
        out.add(cur.toString());
        return out;
    }

    // ---------- Health check ----------
    private static List<Check> buildChecks(List<Contact> contacts, List<Section> sections, int pages) {
        List<Check> c = new ArrayList<>();
        c.add(check("Email address", hasContact(contacts, "email"), "Found",
                "Add a professional email address at the top."));
        c.add(check("LinkedIn link", hasContact(contacts, "linkedin"), "Found",
                "Add your LinkedIn profile link."));
        c.add(check("GitHub link", hasContact(contacts, "github"), "Found",
                "Add your GitHub link so recruiters can see your code."));
        c.add(check("Professional summary", hasKind(sections, "summary"), "Found",
                "Add a 2 to 3 line summary that names your target role."));
        c.add(check("Skills section", hasKind(sections, "skills"), "Found",
                "Add a skills section grouped by category."));
        c.add(check("Experience or internships", hasKind(sections, "experience"), "Found",
                "Add internships, part-time work or freelance work."));
        c.add(check("Projects", hasKind(sections, "projects"), "Found",
                "Add 2 or 3 projects: what you built, with which tools, and the result."));
        c.add(check("Education", hasKind(sections, "education"), "Found",
                "Add your degree, college and years."));

        int[] stats = bulletStats(sections);
        int total = stats[0];
        int withNumbers = stats[1];
        boolean quantified = total > 0 && (withNumbers >= 3 || withNumbers * 2 >= total);
        String badNote = total == 0
                ? "Use bullet points under experience and projects, and include numbers."
                : "Only " + withNumbers + " of " + total + " bullet points include numbers. "
                  + "Add percentages, counts or time saved.";
        c.add(check("Measurable results", quantified,
                withNumbers + " of " + total + " bullet points include numbers", badNote));

        c.add(check("Length", pages <= 2, pages + (pages == 1 ? " page" : " pages"),
                pages + " pages is long. Aim for 1 or 2 pages."));
        return c;
    }

    private static Check check(String label, boolean ok, String okNote, String badNote) {
        return new Check(label, ok, ok ? okNote : badNote);
    }

    private static boolean hasContact(List<Contact> contacts, String type) {
        return contacts.stream().anyMatch(c -> c.type().equals(type));
    }

    private static boolean hasKind(List<Section> sections, String kind) {
        return sections.stream().anyMatch(s -> s.kind().equals(kind));
    }

    // ---------- Bullet statistics and weak bullets (used by the AI coach) ----------
    private static List<String> impactBullets(List<Section> sections) {
        List<String> all = new ArrayList<>();
        for (Section s : sections) {
            if (!s.kind().equals("experience") && !s.kind().equals("projects")) {
                continue;
            }
            all.addAll(s.bullets());
            for (Block b : s.blocks()) {
                all.addAll(b.bullets());
            }
        }
        return all;
    }

    // returns {total bullets, bullets that contain a number}
    private static int[] bulletStats(List<Section> sections) {
        int total = 0;
        int withNumbers = 0;
        for (String x : impactBullets(sections)) {
            total++;
            if (x.matches(".*\\d.*")) {
                withNumbers++;
            }
        }
        return new int[] {total, withNumbers};
    }

    // Bullets in experience and projects that contain no numbers
    public static List<String> weakBullets(ParsedResume resume, int max) {
        List<String> out = new ArrayList<>();
        for (String x : impactBullets(resume.sections())) {
            if (out.size() >= max) {
                break;
            }
            if (x.length() >= 25 && !x.matches(".*\\d.*")) {
                out.add(x);
            }
        }
        return out;
    }

    // ---------- Tiny helpers ----------
    private static int digitCount(String s) {
        int n = 0;
        for (char c : s.toCharArray()) {
            if (Character.isDigit(c)) {
                n++;
            }
        }
        return n;
    }

    private static boolean isAllUpper(String s) {
        return s.equals(s.toUpperCase()) && !s.equals(s.toLowerCase());
    }

    private static String titleCase(String s) {
        StringBuilder sb = new StringBuilder();
        for (String w : s.toLowerCase().split("\\s+")) {
            if (w.isEmpty()) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(Character.toUpperCase(w.charAt(0))).append(w.substring(1));
        }
        return sb.toString();
    }

    private static String initials(String name) {
        String[] w = name.trim().split("\\s+");
        if (name.isBlank()) {
            return "CV";
        }
        String first = w[0].substring(0, 1);
        String last = w.length > 1 ? w[w.length - 1].substring(0, 1) : "";
        return (first + last).toUpperCase();
    }
}