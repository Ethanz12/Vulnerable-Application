package campus.util;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class HtmlSanitizer {

    private static final Set<String> ALLOWED_TAGS = new HashSet<>(Arrays.asList(
            "p", "h1", "h2", "h3", "strong", "em", "b", "i", "u",
            "ul", "ol", "li", "br", "a"
    ));

    private static final Pattern TAG_PATTERN = Pattern.compile("<(/?)([a-zA-Z][a-zA-Z0-9]*)\\s*([^>]*)/?>");
    private static final Pattern ATTR_PATTERN = Pattern.compile("\\s+([a-zA-Z_:][\\w:.-]*)(?:\\s*=\\s*(?:\"[^\"]*\"|'[^']*'|\\S+))?");
    private static final Pattern SCRIPT_TAG_PATTERN = Pattern.compile("<\\s*/?\\s*script[^>]*>", Pattern.CASE_INSENSITIVE);

    private HtmlSanitizer() {
        
    }

    
    public static String sanitize(String html) {
        if (html == null || html.isEmpty()) {
            return "";
        }

        
        String result = SCRIPT_TAG_PATTERN.matcher(html).replaceAll("");

        
        result = processTags(result);

        return result;
    }

    private static String processTags(String html) {
        StringBuilder sb = new StringBuilder();
        Matcher matcher = TAG_PATTERN.matcher(html);
        int lastEnd = 0;

        while (matcher.find()) {
            
            sb.append(html.substring(lastEnd, matcher.start()));

            String slash = matcher.group(1);
            String tagName = matcher.group(2).toLowerCase();
            String attrs = matcher.group(3);

            if (ALLOWED_TAGS.contains(tagName)) {
                String cleanAttrs = cleanAttributes(attrs, tagName);
                if ("/".equals(slash)) {
                    sb.append("</").append(tagName).append(">");
                } else {
                    sb.append("<").append(tagName);
                    if (!cleanAttrs.isEmpty()) {
                        sb.append(" ").append(cleanAttrs.trim());
                    }
                    sb.append(">");
                }
            }

            lastEnd = matcher.end();
        }

        sb.append(html.substring(lastEnd));
        return sb.toString();
    }

    private static String cleanAttributes(String attrs, String tagName) {
        if (attrs == null || attrs.trim().isEmpty()) {
            return "";
        }

        if ("a".equals(tagName)) {
            return cleanAnchorAttributes(attrs);
        }

        
        return "";
    }

    private static String cleanAnchorAttributes(String attrs) {
        StringBuilder sb = new StringBuilder();
        Matcher attrMatcher = ATTR_PATTERN.matcher(attrs);

        while (attrMatcher.find()) {
            String fullAttr = attrMatcher.group(0);
            String attrName = attrMatcher.group(1).toLowerCase();

            if ("href".equals(attrName)) {
                String value = extractAttributeValue(fullAttr);
                if (value != null && !isDangerousUri(value)) {
                    sb.append(" href=\"").append(escapeHtmlAttribute(value)).append("\"");
                }
            }
        }

        return sb.toString();
    }

    private static String extractAttributeValue(String attrWithEquals) {
        int eqIndex = attrWithEquals.indexOf('=');
        if (eqIndex < 0) {
            return null;
        }
        String valuePart = attrWithEquals.substring(eqIndex + 1).trim();

        if ((valuePart.startsWith("\"") && valuePart.endsWith("\""))
                || (valuePart.startsWith("'") && valuePart.endsWith("'"))) {
            valuePart = valuePart.substring(1, valuePart.length() - 1);
        }

        return valuePart;
    }

    private static boolean isDangerousUri(String uri) {
        if (uri == null) {
            return true;
        }
        String normalized = uri.replaceAll("\\s+", "").toLowerCase();
        return normalized.startsWith("javascript:")
                || normalized.startsWith("vbscript:")
                || normalized.startsWith("data:");
    }

    private static String escapeHtmlAttribute(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;")
                    .replace("\"", "&quot;")
                    .replace("'", "&#x27;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;");
    }
}
