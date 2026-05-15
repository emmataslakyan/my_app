package com.example.login;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Minimal Mustache-style renderer supporting:
 *   {{var}}        — HTML-escaped substitution
 *   {{{var}}}      — raw (unescaped) substitution
 *   {{#section}} … {{/section}}    — iterate a List of Maps (each item is a context),
 *                                    render once with parent context if value is a non-empty
 *                                    String, render zero times if empty/null/false.
 *   {{^section}} … {{/section}}    — inverted: render only when value is empty/null/false.
 *   {{.}}                          — current item (when iterating a List<String>).
 *
 * Lookups walk the context stack (current item → … → root) so child sections can read
 * fields from the parent resume.
 */
public final class MustacheRenderer {

    private static final Pattern TAG =
            Pattern.compile("\\{\\{(#|\\^|/|&|!|\\{)?\\s*([\\w.]+)\\s*\\}?\\}\\}");

    private MustacheRenderer() {}

    public static String render(String template, Map<String, Object> rootContext) {
        if (template == null) return "";
        Deque<Object> stack = new ArrayDeque<>();
        stack.push(rootContext != null ? rootContext : java.util.Collections.emptyMap());
        StringBuilder out = new StringBuilder(template.length() + 256);
        renderInto(template, 0, template.length(), stack, out);
        return out.toString();
    }

    private static int renderInto(String tpl, int start, int end,
                                  Deque<Object> stack, StringBuilder out) {
        Matcher m = TAG.matcher(tpl);
        m.region(start, end);
        int cursor = start;
        while (m.find()) {
            out.append(tpl, cursor, m.start());
            String sigil = m.group(1);
            String key = m.group(2);
            cursor = m.end();

            if (sigil == null || "&".equals(sigil) || "{".equals(sigil)) {
                Object val = lookup(stack, key);
                String s = val == null ? "" : String.valueOf(val);
                if (sigil == null) {
                    out.append(htmlEscape(s));
                } else {
                    out.append(s);
                }
            } else if ("!".equals(sigil)) {
                // comment — drop
            } else if ("#".equals(sigil) || "^".equals(sigil)) {
                int sectionStart = cursor;
                int sectionEnd = findClose(tpl, key, sectionStart, end);
                if (sectionEnd < 0) {
                    out.append(tpl, m.start(), end);
                    return end;
                }
                Object val = lookup(stack, key);
                boolean truthy = isTruthy(val);
                if ("#".equals(sigil)) {
                    if (truthy) {
                        if (val instanceof Collection) {
                            for (Object item : (Collection<?>) val) {
                                stack.push(item);
                                renderInto(tpl, sectionStart, sectionEnd, stack, out);
                                stack.pop();
                            }
                        } else {
                            // Non-collection truthy value — render once with same context.
                            renderInto(tpl, sectionStart, sectionEnd, stack, out);
                        }
                    }
                } else { // ^
                    if (!truthy) {
                        renderInto(tpl, sectionStart, sectionEnd, stack, out);
                    }
                }
                // Skip past the {{/key}} close tag.
                cursor = skipCloseTag(tpl, key, sectionEnd);
                m.region(cursor, end);
            } else if ("/".equals(sigil)) {
                // Stray close — render literally (or stop). We just emit nothing.
            }
        }
        out.append(tpl, cursor, end);
        return end;
    }

    private static int findClose(String tpl, String key, int from, int end) {
        Pattern open  = Pattern.compile("\\{\\{[#^]\\s*" + Pattern.quote(key) + "\\s*\\}\\}");
        Pattern close = Pattern.compile("\\{\\{/\\s*"   + Pattern.quote(key) + "\\s*\\}\\}");
        int depth = 1;
        int i = from;
        while (i < end) {
            Matcher oc = open.matcher(tpl).region(i, end);
            Matcher cc = close.matcher(tpl).region(i, end);
            boolean oFound = oc.find();
            boolean cFound = cc.find();
            if (!cFound) return -1;
            if (oFound && oc.start() < cc.start()) {
                depth++;
                i = oc.end();
            } else {
                depth--;
                if (depth == 0) return cc.start();
                i = cc.end();
            }
        }
        return -1;
    }

    private static int skipCloseTag(String tpl, String key, int closeStart) {
        Matcher cc = Pattern.compile("\\{\\{/\\s*" + Pattern.quote(key) + "\\s*\\}\\}")
                .matcher(tpl).region(closeStart, tpl.length());
        return cc.find() ? cc.end() : closeStart;
    }

    @SuppressWarnings("unchecked")
    private static Object lookup(Deque<Object> stack, String key) {
        if (".".equals(key)) return stack.peek();
        for (Object frame : stack) {
            if (frame instanceof Map) {
                Map<String, Object> map = (Map<String, Object>) frame;
                if (map.containsKey(key)) return map.get(key);
            }
        }
        return null;
    }

    private static boolean isTruthy(Object v) {
        if (v == null) return false;
        if (v instanceof Boolean) return (Boolean) v;
        if (v instanceof Collection) return !((Collection<?>) v).isEmpty();
        if (v instanceof CharSequence) return ((CharSequence) v).length() > 0;
        if (v instanceof Number) return ((Number) v).doubleValue() != 0;
        return true;
    }

    private static String htmlEscape(String s) {
        StringBuilder sb = new StringBuilder(s.length() + 16);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '&': sb.append("&amp;"); break;
                case '<': sb.append("&lt;"); break;
                case '>': sb.append("&gt;"); break;
                case '"': sb.append("&quot;"); break;
                case '\'': sb.append("&#39;"); break;
                default:
                    if (c == '\n') sb.append("<br>");
                    else sb.append(c);
            }
        }
        return sb.toString();
    }

    public static List<String> splitLines(String raw) {
        List<String> out = new ArrayList<>();
        if (raw == null) return out;
        for (String line : raw.split("\\r?\\n")) {
            String t = line.trim();
            if (t.isEmpty()) continue;
            if (t.startsWith("•") || t.startsWith("-")) t = t.substring(1).trim();
            if (!t.isEmpty()) out.add(t);
        }
        return out;
    }
}
