package jadx.core.utils;

import jadx.api.IJadxArgs;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StringUtils {

    private final boolean escapeUnicode;

    public StringUtils(IJadxArgs args) {
        this.escapeUnicode = args.escapeUnicode();
    }

    public String unescapeString(String str) {
        int len = str.length();
        if (len == 0) {
            return "\"\"";
        }
        StringBuilder res = new StringBuilder();
        for (int i = 0; i < len; i++) {
            int c = str.charAt(i) & 0xFFFF;
            processChar(c, res);
        }
        return '"' + res.toString() + '"';
    }

    public String unescapeChar(char ch) {
        if (ch == '\'') {
            return "'\\\''";
        }
        StringBuilder res = new StringBuilder();
        processChar(ch, res);
        return '\'' + res.toString() + '\'';
    }

    private void processChar(int c, StringBuilder res) {
        switch (c) {
            case '\n':
                res.append("\\n");
                break;
            case '\r':
                res.append("\\r");
                break;
            case '\t':
                res.append("\\t");
                break;
            case '\b':
                res.append("\\b");
                break;
            case '\f':
                res.append("\\f");
                break;
            case '\'':
                res.append('\'');
                break;
            case '"':
                res.append("\\\"");
                break;
            case '\\':
                res.append("\\\\");
                break;

            default:
                if (c < 32 || c >= 127 && escapeUnicode) {
                    res.append("\\u").append(String.format("%04x", c));
                } else {
                    res.append((char) c);
                }
                break;
        }
    }

    public static String escape(String str) {
        int len = str.length();
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) {
            char c = str.charAt(i);
            switch (c) {
                case '.':
                case '/':
                case ';':
                case '$':
                case ' ':
                case ',':
                case '<':
                    sb.append('_');
                    break;

                case '[':
                    sb.append('A');
                    break;

                case ']':
                case '>':
                case '?':
                case '*':
                    break;

                default:
                    sb.append(c);
                    break;
            }
        }
        return sb.toString();
    }

    public static String escapeXML(String str) {
        int len = str.length();
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) {
            char c = str.charAt(i);
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
                    sb.append("&apos;");
                    break;
                default:
                    sb.append(c);
                    break;
            }
        }
        return sb.toString();
    }

    public static String escapeResValue(String str) {
        int len = str.length();
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) {
            char c = str.charAt(i);
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
                    sb.append("&apos;");
                    break;

                case '\n':
                    sb.append("\\n");
                    break;
                case '\r':
                    sb.append("\\r");
                    break;
                case '\t':
                    sb.append("\\t");
                    break;
                case '\b':
                    sb.append("\\b");
                    break;
                case '\f':
                    sb.append("\\f");
                    break;
                default:
                    sb.append(c);
                    break;
            }
        }
        return sb.toString();
    }

    public static String escapeResStrValue(String str) {
        int len = str.length();
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) {
            char c = str.charAt(i);
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
                    sb.append("\\\"");
                    break;
                case '\'':
                    sb.append("\\'");
                    break;

                case '\n':
                    sb.append("\\n");
                    break;
                case '\r':
                    sb.append("\\r");
                    break;
                case '\t':
                    sb.append("\\t");
                    break;
                case '\b':
                    sb.append("\\b");
                    break;
                case '\f':
                    sb.append("\\f");
                    break;
                default:
                    sb.append(c);
                    break;
            }
        }
        return sb.toString();
    }

    private static final Pattern P0 = Pattern.compile("^[0oO]+$");
    private static final Pattern P1 = Pattern.compile("^[iI]+$");
    private static final Pattern P2 = Pattern.compile("^[uv]+$");
    private static final Pattern P3 = Pattern.compile("[0-9]");

    public static boolean deobfuseName(String name) {
        return name != null && !"".equals(name) && (P0.matcher(name).matches() || P1.matcher(name).matches() || P2.matcher(name.toLowerCase()).matches());
    }

    public static boolean isContainNumber(String clsName) {
        Matcher m = P3.matcher(clsName);
        return m.find();
    }
}
