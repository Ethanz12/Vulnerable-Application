package campus.util;

/**
 * Simple HTML escaping utility to prevent reflected XSS.
 * Escapes the five critical characters that can break out of HTML context.
 */
public final class HtmlEscape {

    private HtmlEscape() {
        // utility class
    }

    /**
     * Escapes special HTML characters in the given string.
     * Converts: & -> &amp;, < -> &lt;, > -> &gt;, " -> &quot;, ' -> &#x27;
     *
     * @param input the raw string to escape
     * @return the escaped string safe for inclusion in HTML body content, or empty string if null
     */
    public static String escape(String input) {
        if (input == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder(input.length() + 16);
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            switch (c) {
                case '&':
                    sb.append("&amp;");
                    break;
                case '<':
                    sb.append("&lt;");
                    break;
                case '>':
                    sb.append("&gt;");
                    break;
                case '"':
                    sb.append("&quot;");
                    break;
                case '\'':
                    sb.append("&#x27;");
                    break;
                default:
                    sb.append(c);
            }
        }
        return sb.toString();
    }
}
