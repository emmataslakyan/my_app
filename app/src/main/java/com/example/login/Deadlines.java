package com.example.login;

import java.util.Calendar;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extracts a registration deadline (UTC millis at end-of-day) from a free-form program
 * description, mirroring the heuristics greenwich.am's frontend uses.
 *
 * Strategy:
 *  1. Look for a date *after* a deadline anchor word ({@code վերջնաժամկետ} / {@code deadline}).
 *     This avoids confusing the registration deadline with the event date that often
 *     appears later in the description.
 *  2. If the deadline section explicitly says "no deadline" / "not specified" /
 *     "registration not required" in Armenian or English, return 0 (no deadline).
 *  3. If anchored search fails, fall back to the first parseable date anywhere in
 *     the description.
 *
 * Supported date forms (matches what Greenwich's programs actually use):
 *   - Armenian month name + day + year   — {@code մայիսի 20, 2026թ․}
 *   - English month name + day + year    — {@code May 20, 2026}
 *   - Numeric d/m/y or m/d/y             — {@code 20/05/2026}
 */
public final class Deadlines {

    private Deadlines() {}

    /** Search window (characters) after the deadline anchor word. */
    private static final int ANCHOR_WINDOW = 200;

    /** Anchor: any case for English, exact for Armenian (Armenian has no case). */
    private static final Pattern DEADLINE_ANCHOR = Pattern.compile(
            "վերջնաժամկետ|deadline", Pattern.CASE_INSENSITIVE);

    /** Armenian month names (genitive form, optional `-ի` suffix to also catch nominative). */
    private static final Pattern ARMENIAN_DATE = Pattern.compile(
            "(հունվար|փետրվար|մարտ|ապրիլ|մայիս|հունիս|հուլիս|" +
                    "օգոստոս|սեպտեմբեր|հոկտեմբեր|նոյեմբեր|դեկտեմբեր)ի?" +
                    "\\s+(\\d{1,2}),?\\s+(20\\d{2})(?!\\d)");

    /** English month names (Jan / January, etc.). */
    private static final Pattern ENGLISH_DATE = Pattern.compile(
            "\\b(jan(?:uary)?|feb(?:ruary)?|mar(?:ch)?|apr(?:il)?|may|jun(?:e)?|jul(?:y)?|" +
                    "aug(?:ust)?|sep(?:tember)?|sept|oct(?:ober)?|nov(?:ember)?|dec(?:ember)?)\\.?" +
                    "\\s+(\\d{1,2}),?\\s+(20\\d{2})\\b",
            Pattern.CASE_INSENSITIVE);

    /** dd/mm/yyyy or mm/dd/yyyy with /, -, or . separators. */
    private static final Pattern NUMERIC_DATE = Pattern.compile(
            "\\b(\\d{1,2})[\\/.\\-](\\d{1,2})[\\/.\\-]((?:20)?\\d{2})\\b");

    /** Phrases (case-insensitive substring) that mean "this program has no real deadline". */
    private static final String[] NO_DEADLINE_PHRASES = {
            // English
            "no deadline", "deadline not specified", "no application deadline",
            "no fixed deadline", "registration not required", "no registration required",
            "until the event day", "until event day", "until meeting day",
            // Armenian — match what Greenwich's own bundle treats as no-deadline
            "վերջնաժամկետ չկա",
            "վերջնաժամկետ նշված չէ",
            "գրանցում չի պահանջվում",
            "մինչև հանդիպման օրը",
            "մինչեւ հանդիպման օրը",
            "չի նշվում",
            "նշված չէ"
    };

    /** Returns the end-of-day UTC millis of the registration deadline, or 0 if not found. */
    public static long extract(String text) {
        if (text == null || text.isEmpty()) return 0L;

        Matcher anchor = DEADLINE_ANCHOR.matcher(text);
        if (anchor.find()) {
            int start = anchor.end();
            int end = Math.min(text.length(), start + ANCHOR_WINDOW);
            String window = text.substring(start, end);
            if (hasNoDeadlinePhrase(window)) return 0L;
            long t = parseFirstDate(window);
            if (t > 0L) return t;
            // Anchor found but no date nearby — fall through to whole-text scan.
        }
        return parseFirstDate(text);
    }

    private static long parseFirstDate(String text) {
        Matcher m = ARMENIAN_DATE.matcher(text);
        if (m.find()) {
            int month = armenianMonthIndex(m.group(1));
            int day   = parseIntSafe(m.group(2));
            int year  = parseIntSafe(m.group(3));
            return buildEndOfDay(year, month, day);
        }

        m = ENGLISH_DATE.matcher(text);
        if (m.find()) {
            int month = englishMonthIndex(m.group(1));
            int day   = parseIntSafe(m.group(2));
            int year  = parseIntSafe(m.group(3));
            return buildEndOfDay(year, month, day);
        }

        m = NUMERIC_DATE.matcher(text);
        if (m.find()) {
            // Day-first convention (Armenia/Europe). If the first token can't be a day
            // but the second could be a month, swap.
            int a = parseIntSafe(m.group(1));
            int b = parseIntSafe(m.group(2));
            int year = parseIntSafe(m.group(3));
            if (year < 100) year += 2000;
            int day, month;
            if (a > 12 && b <= 12)      { day = a; month = b - 1; }
            else if (a <= 12 && b > 12) { day = b; month = a - 1; }
            else                         { day = a; month = b - 1; }
            return buildEndOfDay(year, month, day);
        }
        return 0L;
    }

    private static boolean hasNoDeadlinePhrase(String text) {
        String lower = text.toLowerCase();
        for (String phrase : NO_DEADLINE_PHRASES) {
            if (lower.contains(phrase.toLowerCase())) return true;
        }
        return false;
    }

    private static int armenianMonthIndex(String name) {
        // Strip optional `ի` genitive suffix.
        String n = name.endsWith("ի") ? name.substring(0, name.length() - 1) : name;
        switch (n) {
            case "հունվար":   return 0;
            case "փետրվար":   return 1;
            case "մարտ":      return 2;
            case "ապրիլ":     return 3;
            case "մայիս":     return 4;
            case "հունիս":    return 5;
            case "հուլիս":    return 6;
            case "օգոստոս":   return 7;
            case "սեպտեմբեր": return 8;
            case "հոկտեմբեր": return 9;
            case "նոյեմբեր":  return 10;
            case "դեկտեմբեր": return 11;
            default: return -1;
        }
    }

    private static int englishMonthIndex(String name) {
        String n = name.toLowerCase();
        if (n.startsWith("jan")) return 0;
        if (n.startsWith("feb")) return 1;
        if (n.startsWith("mar")) return 2;
        if (n.startsWith("apr")) return 3;
        if (n.equals("may"))     return 4;
        if (n.startsWith("jun")) return 5;
        if (n.startsWith("jul")) return 6;
        if (n.startsWith("aug")) return 7;
        if (n.startsWith("sep")) return 8;
        if (n.startsWith("oct")) return 9;
        if (n.startsWith("nov")) return 10;
        if (n.startsWith("dec")) return 11;
        return -1;
    }

    private static int parseIntSafe(String s) {
        try { return Integer.parseInt(s); } catch (NumberFormatException e) { return 0; }
    }

    private static long buildEndOfDay(int year, int month, int day) {
        if (year < 1970 || month < 0 || month > 11 || day < 1 || day > 31) return 0L;
        Calendar c = Calendar.getInstance();
        c.clear();
        c.set(year, month, day, 23, 59, 59);
        return c.getTimeInMillis();
    }
}
