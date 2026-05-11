package com.studentmanagement.studentmanagementserver.domain.university;

import com.studentmanagement.studentmanagementserver.repo.UniversityProgramRepository;
import com.studentmanagement.studentmanagementserver.repo.UniversityRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Component
@Profile("!test")
public class UniversityCatalogSeedService {

    private static final Logger log = LoggerFactory.getLogger(UniversityCatalogSeedService.class);
    private static final String UNIVERSITY_SEED_FILE = "universities.seed.csv";
    private static final String PROGRAM_SEED_FILE = "university-programs.seed.csv";

    private final UniversityRepository universityRepository;
    private final UniversityProgramRepository universityProgramRepository;

    public UniversityCatalogSeedService(UniversityRepository universityRepository,
                                        UniversityProgramRepository universityProgramRepository) {
        this.universityRepository = universityRepository;
        this.universityProgramRepository = universityProgramRepository;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void seedUniversityCatalog() {
        int universities = seedUniversities();
        int programs = seedPrograms();
        log.info("University catalog seed complete. universitiesUpserted={}, programsInserted={}", universities, programs);
    }

    private int seedUniversities() {
        ClassPathResource resource = new ClassPathResource(UNIVERSITY_SEED_FILE);
        if (!resource.exists()) {
            log.warn("University seed file {} not found.", UNIVERSITY_SEED_FILE);
            return 0;
        }

        int upserted = 0;
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
            String line = reader.readLine();
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) {
                    continue;
                }
                List<String> fields = parseCsvLine(line);
                if (fields.size() < 5) {
                    continue;
                }

                String name = normalize(fields.get(0));
                if (name == null) {
                    continue;
                }
                String province = normalize(fields.get(1));
                String city = normalize(fields.get(2));
                String country = normalize(fields.get(3));
                String website = normalize(fields.get(4));

                University university = universityRepository.findFirstByNameIgnoreCase(name).orElse(null);
                if (university == null) {
                    university = findLegacyUniversityForSeedName(name, website);
                }
                if (university == null) {
                    universityRepository.save(new University(name, province, city, country, website));
                    upserted++;
                    continue;
                }

                boolean changed = false;
                if (!same(university.getName(), name)) {
                    university.setName(name);
                    changed = true;
                }
                if (!same(university.getProvince(), province)) {
                    university.setProvince(province);
                    changed = true;
                }
                if (!same(university.getCity(), city)) {
                    university.setCity(city);
                    changed = true;
                }
                if (!same(university.getCountry(), country == null ? "Canada" : country)) {
                    university.setCountry(country);
                    changed = true;
                }
                if (!same(university.getWebsite(), website)) {
                    university.setWebsite(website);
                    changed = true;
                }
                if (!university.isActive()) {
                    university.setActive(true);
                    changed = true;
                }
                if (changed) {
                    universityRepository.save(university);
                    upserted++;
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load " + UNIVERSITY_SEED_FILE, e);
        }
        return upserted;
    }

    private University findLegacyUniversityForSeedName(String name, String website) {
        if (!sameKey(name, "University of Toronto – St. George Campus")) {
            return null;
        }
        University legacy = universityRepository.findFirstByNameIgnoreCase("University of Toronto").orElse(null);
        if (legacy == null) {
            return null;
        }
        String legacyWebsite = legacy.getWebsite() == null ? "" : legacy.getWebsite().trim().toLowerCase(Locale.ROOT);
        String seedWebsite = website == null ? "" : website.trim().toLowerCase(Locale.ROOT);
        if (legacyWebsite.contains("toronto-st-george") || seedWebsite.contains("toronto-st-george")) {
            return legacy;
        }
        return null;
    }

    private int seedPrograms() {
        ClassPathResource resource = new ClassPathResource(PROGRAM_SEED_FILE);
        if (!resource.exists()) {
            log.warn("University program seed file {} not found.", PROGRAM_SEED_FILE);
            return 0;
        }

        int inserted = 0;
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
            String line = reader.readLine();
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) {
                    continue;
                }
                List<String> fields = parseCsvLine(line);
                if (fields.size() < 4) {
                    continue;
                }

                String universityName = normalize(fields.get(0));
                String programName = normalize(fields.get(1));
                if (universityName == null || programName == null) {
                    continue;
                }
                String facultyName = normalize(fields.get(2));
                String degreeType = normalize(fields.get(3));

                University university = universityRepository.findFirstByNameIgnoreCase(universityName).orElse(null);
                if (university == null) {
                    log.warn("Skipping program seed because university was not found: {}", universityName);
                    continue;
                }
                if (programExists(university, programName, facultyName, degreeType)) {
                    continue;
                }
                universityProgramRepository.save(new UniversityProgram(university, programName, facultyName, degreeType));
                inserted++;
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load " + PROGRAM_SEED_FILE, e);
        }
        return inserted;
    }

    private boolean programExists(University university, String programName, String facultyName, String degreeType) {
        List<UniversityProgram> existing =
                universityProgramRepository.findByUniversity_IdOrderByProgramNameAscFacultyNameAscDegreeTypeAsc(university.getId());
        for (UniversityProgram program : existing) {
            boolean sameProgramName = sameKey(program.getProgramName(), programName);
            boolean incomingHasDetails = normalize(facultyName) != null || normalize(degreeType) != null;
            if (sameProgramName && !incomingHasDetails) {
                return true;
            }
            if (sameProgramName
                    && sameKey(program.getFacultyName(), facultyName)
                    && sameKey(program.getDegreeType(), degreeType)) {
                return true;
            }
        }
        return false;
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

    private String normalize(String value) {
        String normalized = value == null ? "" : value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private boolean same(String left, String right) {
        String normalizedLeft = left == null ? "" : left.trim();
        String normalizedRight = right == null ? "" : right.trim();
        return normalizedLeft.equals(normalizedRight);
    }

    private boolean sameKey(String left, String right) {
        String normalizedLeft = left == null ? "" : left.trim().toLowerCase(Locale.ROOT);
        String normalizedRight = right == null ? "" : right.trim().toLowerCase(Locale.ROOT);
        return normalizedLeft.equals(normalizedRight);
    }
}
