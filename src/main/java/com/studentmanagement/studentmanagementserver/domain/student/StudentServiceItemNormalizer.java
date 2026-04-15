package com.studentmanagement.studentmanagementserver.domain.student;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

final class StudentServiceItemNormalizer {

    private static final Pattern LEGACY_PREFIX_PATTERN = Pattern.compile("^[A-Za-z]\\s*[:：]\\s*");

    private static final Set<String> CANONICAL_VALUES = new LinkedHashSet<String>(Arrays.asList(
            "面试辅导",
            "雅思A类全科班",
            "SAT全科班",
            "数学竞赛类班课",
            "3U&4U阅写及文学素养",
            "雅思VIP 20小时包",
            "雅思VIP 50小时包",
            "学科VIP 20小时包",
            "学科VIP 50小时包",
            "AP/IB/数学竞赛VIP 50小时包",
            "一对一辅导"
    ));

    private StudentServiceItemNormalizer() {
    }

    static List<String> normalizeIncoming(List<String> serviceItems, List<String> serviceProjects) {
        if (serviceItems == null && serviceProjects == null) {
            return null;
        }
        List<String> merged = new ArrayList<String>();
        if (serviceItems != null) {
            merged.addAll(serviceItems);
        }
        if (serviceProjects != null) {
            merged.addAll(serviceProjects);
        }
        return normalizeList(merged);
    }

    static List<String> normalizeStored(List<String> storedItems) {
        if (storedItems == null || storedItems.isEmpty()) {
            return new ArrayList<String>();
        }
        return normalizeList(storedItems);
    }

    private static List<String> normalizeList(List<String> rawItems) {
        LinkedHashSet<String> deduplicated = new LinkedHashSet<String>();
        if (rawItems == null || rawItems.isEmpty()) {
            return new ArrayList<String>();
        }
        for (String rawItem : rawItems) {
            String normalized = normalizeOne(rawItem);
            if (normalized != null) {
                deduplicated.add(normalized);
            }
        }
        return new ArrayList<String>(deduplicated);
    }

    private static String normalizeOne(String rawItem) {
        String value = trimToNull(rawItem);
        if (value == null) {
            return null;
        }
        String withoutPrefix = LEGACY_PREFIX_PATTERN.matcher(value).replaceFirst("");
        String stripped = trimToNull(withoutPrefix);
        if (stripped == null) {
            return null;
        }
        if (CANONICAL_VALUES.contains(stripped)) {
            return stripped;
        }
        return stripped;
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
