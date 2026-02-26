package com.studentmanagement.studentmanagementserver.domain.reference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class OntarioCourseProviderReferenceService {

    private static final Logger log = LoggerFactory.getLogger(OntarioCourseProviderReferenceService.class);

    private static final String SEED_FILE = "ontario-course-providers.seed.csv";
    private static final String TARGET_PROVINCE = "ontario";
    private static final int DEFAULT_LIMIT = 10;
    private static final int MAX_LIMIT = 50;
    private static final double MIN_SCORE = 0.20d;

    private final List<ProviderEntry> providers;

    public OntarioCourseProviderReferenceService() {
        this.providers = Collections.unmodifiableList(loadProviders());
        log.info("Loaded {} Ontario course provider entries from {}", providers.size(), SEED_FILE);
    }

    public List<OntarioCourseProviderReferenceDto> search(String queryRaw, Integer limitRaw) {
        int limit = normalizeLimit(limitRaw);
        String normalizedQuery = normalizeForSearch(queryRaw);
        if (normalizedQuery == null) {
            return toDtos(providers.subList(0, Math.min(limit, providers.size())));
        }

        List<String> queryTokens = tokenize(normalizedQuery);
        String compactQuery = normalizedQuery.replace(" ", "");
        List<ScoredProvider> scored = new ArrayList<ScoredProvider>();
        for (ProviderEntry provider : providers) {
            double score = scoreProvider(provider, normalizedQuery, compactQuery, queryTokens);
            if (score >= MIN_SCORE) {
                scored.add(new ScoredProvider(provider, score));
            }
        }

        Collections.sort(scored, new Comparator<ScoredProvider>() {
            @Override
            public int compare(ScoredProvider left, ScoredProvider right) {
                int scoreCompare = Double.compare(right.score, left.score);
                if (scoreCompare != 0) {
                    return scoreCompare;
                }
                int nameCompare = left.provider.name.compareToIgnoreCase(right.provider.name);
                if (nameCompare != 0) {
                    return nameCompare;
                }
                return left.provider.city.compareToIgnoreCase(right.provider.city);
            }
        });

        List<OntarioCourseProviderReferenceDto> results = new ArrayList<OntarioCourseProviderReferenceDto>();
        for (int i = 0; i < scored.size() && i < limit; i++) {
            results.add(scored.get(i).provider.toDto());
        }
        return results;
    }

    private int normalizeLimit(Integer limitRaw) {
        if (limitRaw == null) {
            return DEFAULT_LIMIT;
        }
        if (limitRaw.intValue() <= 0) {
            return DEFAULT_LIMIT;
        }
        return Math.min(limitRaw.intValue(), MAX_LIMIT);
    }

    private List<ProviderEntry> loadProviders() {
        ClassPathResource resource = new ClassPathResource(SEED_FILE);
        if (!resource.exists()) {
            log.warn("Seed file {} not found on classpath; search endpoint will return empty list", SEED_FILE);
            return new ArrayList<ProviderEntry>();
        }

        LinkedHashMap<String, ProviderEntry> deduped = new LinkedHashMap<String, ProviderEntry>();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
            String line = reader.readLine(); // header
            if (line == null) {
                return new ArrayList<ProviderEntry>();
            }

            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) {
                    continue;
                }
                List<String> fields = parseCsvLine(line);
                if (fields.size() < 9) {
                    continue;
                }
                ProviderEntry entry = new ProviderEntry(
                        trimToEmpty(fields.get(0)),
                        trimToEmpty(fields.get(1)),
                        trimToEmpty(fields.get(2)),
                        trimToEmpty(fields.get(3)),
                        trimToEmpty(fields.get(4)),
                        trimToEmpty(fields.get(5)),
                        trimToEmpty(fields.get(6)),
                        trimToEmpty(fields.get(7)),
                        trimToEmpty(fields.get(8))
                );
                if (!isTargetProvince(entry.state) || !isCanada(entry.country)) {
                    continue;
                }
                String dedupeKey = entry.normalizedName
                        + "|" + entry.normalizedCity
                        + "|" + entry.normalizedState
                        + "|" + entry.normalizedStreet
                        + "|" + entry.normalizedPostal
                        + "|" + entry.normalizedBoardName
                        + "|" + entry.normalizedSpecialConditions;
                if (!deduped.containsKey(dedupeKey)) {
                    deduped.put(dedupeKey, entry);
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load " + SEED_FILE, e);
        }

        ArrayList<ProviderEntry> loaded = new ArrayList<ProviderEntry>(deduped.values());
        Collections.sort(loaded, new Comparator<ProviderEntry>() {
            @Override
            public int compare(ProviderEntry left, ProviderEntry right) {
                int nameCompare = left.name.compareToIgnoreCase(right.name);
                if (nameCompare != 0) {
                    return nameCompare;
                }
                int cityCompare = left.city.compareToIgnoreCase(right.city);
                if (cityCompare != 0) {
                    return cityCompare;
                }
                return left.boardName.compareToIgnoreCase(right.boardName);
            }
        });
        return loaded;
    }

    private List<String> parseCsvLine(String line) {
        List<String> out = new ArrayList<String>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (ch == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
                continue;
            }

            if (ch == ',' && !inQuotes) {
                out.add(current.toString());
                current.setLength(0);
                continue;
            }

            current.append(ch);
        }
        out.add(current.toString());
        return out;
    }

    private List<OntarioCourseProviderReferenceDto> toDtos(List<ProviderEntry> entries) {
        List<OntarioCourseProviderReferenceDto> out = new ArrayList<OntarioCourseProviderReferenceDto>();
        for (ProviderEntry entry : entries) {
            out.add(entry.toDto());
        }
        return out;
    }

    private double scoreProvider(ProviderEntry provider,
                                 String normalizedQuery,
                                 String compactQuery,
                                 List<String> queryTokens) {
        if (provider.normalizedName.equals(normalizedQuery)) {
            return 1.50d;
        }
        if (!provider.normalizedAcronym.isEmpty() && provider.normalizedAcronym.equals(compactQuery)) {
            return 1.35d;
        }

        double score = 0d;
        if (provider.normalizedName.startsWith(normalizedQuery)) {
            score += 0.95d;
        } else if (provider.normalizedName.contains(normalizedQuery)) {
            score += 0.80d;
        } else if (provider.normalizedSearchText.contains(normalizedQuery)) {
            score += 0.45d;
        }

        if (!compactQuery.isEmpty()) {
            if (provider.normalizedCompactName.startsWith(compactQuery)) {
                score += 0.50d;
            } else if (provider.normalizedCompactName.contains(compactQuery)) {
                score += 0.35d;
            }
            if (!provider.normalizedAcronym.isEmpty()) {
                if (provider.normalizedAcronym.startsWith(compactQuery)) {
                    score += 0.85d;
                } else if (provider.normalizedAcronym.contains(compactQuery)) {
                    score += 0.45d;
                }
            }
        }

        score += 0.45d * diceCoefficient(provider.normalizedName, normalizedQuery);
        score += 0.25d * diceCoefficient(provider.normalizedSearchText, normalizedQuery);
        score += 0.30d * tokenCoverage(queryTokens, provider.normalizedTokens);
        if (!provider.normalizedAcronym.isEmpty() && !compactQuery.isEmpty()) {
            score += 0.35d * diceCoefficient(provider.normalizedAcronym, compactQuery);
        }
        return score;
    }

    private double tokenCoverage(List<String> queryTokens, List<String> providerTokens) {
        if (queryTokens.isEmpty() || providerTokens.isEmpty()) {
            return 0d;
        }
        int matched = 0;
        for (String queryToken : queryTokens) {
            if (providerTokens.contains(queryToken)) {
                matched++;
                continue;
            }
            boolean fuzzyMatched = false;
            for (String providerToken : providerTokens) {
                if (providerToken.contains(queryToken) || queryToken.contains(providerToken)) {
                    fuzzyMatched = true;
                    break;
                }
                if (diceCoefficient(providerToken, queryToken) >= 0.55d) {
                    fuzzyMatched = true;
                    break;
                }
            }
            if (fuzzyMatched) {
                matched++;
            }
        }
        return ((double) matched) / ((double) queryTokens.size());
    }

    private double diceCoefficient(String left, String right) {
        if (left == null || right == null) {
            return 0d;
        }
        if (left.equals(right)) {
            return 1d;
        }
        if (left.length() < 2 || right.length() < 2) {
            return 0d;
        }

        Map<String, Integer> leftBigrams = bigramCounts(left);
        Map<String, Integer> rightBigrams = bigramCounts(right);
        int intersection = 0;
        int leftTotal = 0;
        int rightTotal = 0;
        for (Integer count : leftBigrams.values()) {
            leftTotal += count.intValue();
        }
        for (Integer count : rightBigrams.values()) {
            rightTotal += count.intValue();
        }

        for (Map.Entry<String, Integer> entry : leftBigrams.entrySet()) {
            Integer rightCount = rightBigrams.get(entry.getKey());
            if (rightCount != null) {
                intersection += Math.min(entry.getValue().intValue(), rightCount.intValue());
            }
        }
        return (2d * intersection) / (leftTotal + rightTotal);
    }

    private Map<String, Integer> bigramCounts(String value) {
        Map<String, Integer> counts = new HashMap<String, Integer>();
        for (int i = 0; i < value.length() - 1; i++) {
            String bigram = value.substring(i, i + 2);
            Integer current = counts.get(bigram);
            counts.put(bigram, current == null ? 1 : current.intValue() + 1);
        }
        return counts;
    }

    private List<String> tokenize(String normalized) {
        if (normalized == null || normalized.isEmpty()) {
            return new ArrayList<String>();
        }
        return Arrays.asList(normalized.split(" "));
    }

    private String normalizeForSearch(String raw) {
        if (raw == null) {
            return null;
        }

        String ascii = Normalizer.normalize(raw, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "");
        String cleaned = ascii.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", " ")
                .trim()
                .replaceAll("\\s+", " ");
        return cleaned.isEmpty() ? null : cleaned;
    }

    private String trimToEmpty(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.trim();
    }

    private boolean isTargetProvince(String state) {
        String normalized = normalizeForSearch(state);
        if (normalized == null) {
            return false;
        }
        if (TARGET_PROVINCE.equals(normalized)) {
            return true;
        }
        return "on".equals(normalized);
    }

    private boolean isCanada(String country) {
        String normalized = normalizeForSearch(country);
        if (normalized == null) {
            return false;
        }
        return "canada".equals(normalized);
    }

    private class ProviderEntry {
        private final String id;
        private final String name;
        private final String boardName;
        private final String schoolSpecialConditions;
        private final String streetAddress;
        private final String city;
        private final String state;
        private final String country;
        private final String postal;
        private final String normalizedName;
        private final String normalizedCity;
        private final String normalizedState;
        private final String normalizedStreet;
        private final String normalizedPostal;
        private final String normalizedBoardName;
        private final String normalizedSpecialConditions;
        private final String normalizedCompactName;
        private final String normalizedAcronym;
        private final String normalizedSearchText;
        private final List<String> normalizedTokens;

        private ProviderEntry(String id,
                              String name,
                              String boardName,
                              String schoolSpecialConditions,
                              String streetAddress,
                              String city,
                              String state,
                              String country,
                              String postal) {
            this.id = id;
            this.name = name;
            this.boardName = boardName;
            this.schoolSpecialConditions = schoolSpecialConditions;
            this.streetAddress = streetAddress;
            this.city = city;
            this.state = state;
            this.country = country;
            this.postal = postal;
            this.normalizedName = normalizeOrEmpty(name);
            this.normalizedCity = normalizeOrEmpty(city);
            this.normalizedState = normalizeOrEmpty(state);
            this.normalizedStreet = normalizeOrEmpty(streetAddress);
            this.normalizedPostal = normalizeOrEmpty(postal);
            this.normalizedBoardName = normalizeOrEmpty(boardName);
            this.normalizedSpecialConditions = normalizeOrEmpty(schoolSpecialConditions);
            this.normalizedCompactName = this.normalizedName.replace(" ", "");
            this.normalizedAcronym = buildAcronym(this.normalizedName);

            StringBuilder searchBuilder = new StringBuilder();
            appendSearchPart(searchBuilder, this.normalizedName);
            appendSearchPart(searchBuilder, this.normalizedBoardName);
            appendSearchPart(searchBuilder, this.normalizedSpecialConditions);
            appendSearchPart(searchBuilder, this.normalizedCity);
            appendSearchPart(searchBuilder, this.normalizedState);
            appendSearchPart(searchBuilder, this.normalizedStreet);
            appendSearchPart(searchBuilder, this.normalizedPostal);
            appendSearchPart(searchBuilder, this.normalizedAcronym);
            this.normalizedSearchText = searchBuilder.toString().trim();
            this.normalizedTokens = tokenize(this.normalizedSearchText);
        }

        private OntarioCourseProviderReferenceDto toDto() {
            return new OntarioCourseProviderReferenceDto(
                    id,
                    name,
                    boardName,
                    schoolSpecialConditions,
                    streetAddress,
                    city,
                    state,
                    country,
                    postal
            );
        }

        private void appendSearchPart(StringBuilder builder, String part) {
            if (part == null || part.isEmpty()) {
                return;
            }
            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append(part);
        }

        private String normalizeOrEmpty(String value) {
            String normalized = normalizeForSearch(value);
            return normalized == null ? "" : normalized;
        }

        private String buildAcronym(String normalizedNameValue) {
            if (normalizedNameValue == null || normalizedNameValue.isEmpty()) {
                return "";
            }
            String[] tokens = normalizedNameValue.split(" ");
            StringBuilder acronym = new StringBuilder();
            for (String token : tokens) {
                if (token.isEmpty()) {
                    continue;
                }
                acronym.append(token.charAt(0));
            }
            return acronym.toString();
        }
    }

    private static class ScoredProvider {
        private final ProviderEntry provider;
        private final double score;

        private ScoredProvider(ProviderEntry provider, double score) {
            this.provider = provider;
            this.score = score;
        }
    }
}
