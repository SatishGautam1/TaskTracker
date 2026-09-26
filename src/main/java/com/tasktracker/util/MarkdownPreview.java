package com.tasktracker.util;

import java.util.regex.Pattern;

/**
 * Converts a Markdown task body into a short, readable plain-text preview
 * for use inside a {@code ListView} cell.
 * <p>
 * A full {@code WebView}-based render per row would be expensive (a WebView
 * spins up a real browser engine instance) and is unnecessary for a
 * two-or-three-line preview, so this does a lightweight textual conversion
 * instead: headings/emphasis markers are stripped, bullets become "•",
 * links show their label, and the result is truncated to a sensible length.
 * The full {@code MarkdownEditor} preview (a real WebView) is unaffected and
 * still does full Markdown rendering.
 */
public final class MarkdownPreview {

    private static final Pattern HEADING = Pattern.compile("^\\s{0,3}#{1,6}\\s*");
    private static final Pattern BULLET = Pattern.compile("^\\s*[-*+]\\s+");
    private static final Pattern ORDERED_LIST = Pattern.compile("^\\s*\\d+[.)]\\s+");
    private static final Pattern CHECKBOX = Pattern.compile("^\\s*\\[[ xX]]\\s*");
    private static final Pattern BOLD_ITALIC = Pattern.compile("(\\*\\*\\*|___)(.*?)\\1");
    private static final Pattern BOLD = Pattern.compile("(\\*\\*|__)(.*?)\\1");
    private static final Pattern ITALIC = Pattern.compile("(?<!\\*)\\*(?!\\*)(.*?)\\*(?!\\*)|_(.*?)_");
    private static final Pattern INLINE_CODE = Pattern.compile("`([^`]*)`");
    private static final Pattern LINK = Pattern.compile("\\[([^]]*)]\\(([^)]*)\\)");
    private static final Pattern IMAGE = Pattern.compile("!\\[([^]]*)]\\(([^)]*)\\)");
    private static final Pattern CODE_FENCE = Pattern.compile("^```.*$");
    private static final Pattern BLOCKQUOTE = Pattern.compile("^\\s{0,3}>\\s?");

    private MarkdownPreview() {
    }

    /**
     * @param markdown  raw markdown source (may be null)
     * @param maxLength maximum length of the returned preview, excluding the
     *                  trailing ellipsis
     */
    public static String toPreviewText(String markdown, int maxLength) {

        if (markdown == null || markdown.isBlank()) {
            return "";
        }

        StringBuilder result = new StringBuilder();
        boolean inCodeFence = false;

        for (String rawLine : markdown.split("\n")) {

            if (CODE_FENCE.matcher(rawLine.trim()).matches()) {
                inCodeFence = !inCodeFence;
                continue;
            }

            String line = rawLine;

            if (!inCodeFence) {
                line = BLOCKQUOTE.matcher(line).replaceFirst("");
                line = CHECKBOX.matcher(line).replaceFirst("");
                line = BULLET.matcher(line).replaceFirst("• ");
                line = ORDERED_LIST.matcher(line).replaceFirst("");
                line = HEADING.matcher(line).replaceFirst("");
            }

            line = IMAGE.matcher(line).replaceAll("$1");
            line = LINK.matcher(line).replaceAll("$1");
            line = INLINE_CODE.matcher(line).replaceAll("$1");
            line = BOLD_ITALIC.matcher(line).replaceAll("$2");
            line = BOLD.matcher(line).replaceAll("$2");
            line = ITALIC.matcher(line).replaceAll(matchResult ->
                    matchResult.group(1) != null ? matchResult.group(1) : matchResult.group(2)
            );

            line = line.strip();

            if (line.isEmpty()) {
                continue;
            }

            if (!result.isEmpty()) {
                result.append("  ");
            }

            result.append(line);

            if (result.length() >= maxLength) {
                break;
            }
        }

        String preview = result.toString();

        if (preview.length() > maxLength) {
            preview = preview.substring(0, maxLength).stripTrailing() + "…";
        }

        return preview;
    }

    public static String toPreviewText(String markdown) {
        return toPreviewText(markdown, 180);
    }
}
