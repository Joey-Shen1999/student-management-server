package com.studentmanagement.studentmanagementserver.domain.teacher;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.studentmanagement.studentmanagementserver.domain.enums.UserRole;
import com.studentmanagement.studentmanagementserver.domain.user.User;
import com.studentmanagement.studentmanagementserver.repo.TeacherPagePreferenceRepository;
import com.studentmanagement.studentmanagementserver.repo.TeacherRepository;
import com.studentmanagement.studentmanagementserver.service.ApiRequestException;
import com.studentmanagement.studentmanagementserver.service.AuthSessionService;
import com.studentmanagement.studentmanagementserver.service.TeacherBindingRequiredException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;

@Service
public class TeacherPagePreferenceService {

    private static final int PAGE_KEY_MAX_LENGTH = 160;
    private static final int VERSION_MAX_LENGTH = 32;

    private final AuthSessionService authSessionService;
    private final TeacherRepository teacherRepository;
    private final TeacherPagePreferenceRepository teacherPagePreferenceRepository;
    private final ObjectMapper objectMapper;

    public TeacherPagePreferenceService(AuthSessionService authSessionService,
                                        TeacherRepository teacherRepository,
                                        TeacherPagePreferenceRepository teacherPagePreferenceRepository,
                                        ObjectMapper objectMapper) {
        this.authSessionService = authSessionService;
        this.teacherRepository = teacherRepository;
        this.teacherPagePreferenceRepository = teacherPagePreferenceRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public TeacherPagePreferenceResponseDto getPreference(String pageKeyRaw, HttpServletRequest request) {
        String pageKey = requirePageKey(pageKeyRaw);
        Teacher teacher = resolveTeacherScope(request, false);
        if (teacher == null || teacher.getId() == null) {
            return emptyResponse(pageKey);
        }

        TeacherPagePreference preference = teacherPagePreferenceRepository
                .findByTeacher_IdAndPageKey(teacher.getId(), pageKey)
                .orElse(null);
        if (preference == null) {
            return emptyResponse(pageKey);
        }
        return toResponse(preference);
    }

    @Transactional
    public TeacherPagePreferenceResponseDto upsertPreference(String pageKeyRaw,
                                                             TeacherPagePreferencePutRequestDto requestBody,
                                                             HttpServletRequest request) {
        if (requestBody == null) {
            throw badRequest("request body is required");
        }

        String pageKey = requirePageKey(pageKeyRaw);
        Teacher teacher = resolveTeacherScope(request, true);

        TeacherPagePreference preference = teacherPagePreferenceRepository
                .findByTeacher_IdAndPageKey(teacher.getId(), pageKey)
                .orElse(null);
        if (preference == null) {
            preference = new TeacherPagePreference(teacher, pageKey);
        }

        List<String> visibleColumnKeys = requestBody.getVisibleColumnKeys();
        if (visibleColumnKeys == null) {
            if (preference.getVisibleColumnKeysJson() == null) {
                throw badRequest("visibleColumnKeys is required");
            }
        } else {
            preference.setVisibleColumnKeysJson(serializeColumnKeys(normalizeColumnKeys(visibleColumnKeys)));
        }

        if (requestBody.getOrderedColumnKeys() != null) {
            List<String> orderedColumnKeys = normalizeColumnKeys(requestBody.getOrderedColumnKeys());
            preference.setOrderedColumnKeysJson(serializeColumnKeys(orderedColumnKeys));
        }

        String normalizedVersion = normalizeVersion(requestBody.getVersion());
        if (normalizedVersion != null || preference.getVersion() == null) {
            preference.setVersion(normalizedVersion);
        }

        TeacherPagePreference saved = teacherPagePreferenceRepository.save(preference);
        return toResponse(saved);
    }

    private TeacherPagePreferenceResponseDto emptyResponse(String pageKey) {
        return new TeacherPagePreferenceResponseDto(
                pageKey,
                null,
                Collections.<String>emptyList(),
                null,
                null
        );
    }

    private TeacherPagePreferenceResponseDto toResponse(TeacherPagePreference preference) {
        List<String> visibleColumnKeys = deserializeColumnKeys(preference.getVisibleColumnKeysJson());
        List<String> orderedColumnKeys = preference.getOrderedColumnKeysJson() == null
                ? null
                : deserializeColumnKeys(preference.getOrderedColumnKeysJson());
        LocalDateTime updatedAt = preference.getUpdatedAt();

        return new TeacherPagePreferenceResponseDto(
                preference.getPageKey(),
                preference.getVersion(),
                visibleColumnKeys,
                orderedColumnKeys,
                updatedAt == null ? null : updatedAt.toString()
        );
    }

    private Teacher resolveTeacherScope(HttpServletRequest request, boolean createIfMissing) {
        User operator = requireTeacherPortalUser(request);
        Teacher teacher = teacherRepository.findByUser_Id(operator.getId()).orElse(null);
        if (teacher != null) {
            return teacher;
        }
        if (operator.getRole() == UserRole.TEACHER) {
            throw new TeacherBindingRequiredException();
        }
        if (!createIfMissing) {
            return null;
        }
        String fallbackName = trimToNull(operator.getUsername());
        if (fallbackName == null) {
            fallbackName = "Admin #" + operator.getId();
        }
        return teacherRepository.save(new Teacher(operator, fallbackName));
    }

    private User requireTeacherPortalUser(HttpServletRequest request) {
        User operator = authSessionService.requireAuthenticatedUser(request);
        UserRole role = operator.getRole();
        if (role != UserRole.TEACHER && role != UserRole.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden: teacher/admin role required.");
        }
        return operator;
    }

    private String requirePageKey(String pageKeyRaw) {
        String pageKey = trimToNull(pageKeyRaw);
        if (pageKey == null) {
            throw badRequest("pageKey is required");
        }
        if (pageKey.length() > PAGE_KEY_MAX_LENGTH) {
            throw badRequest("pageKey too long");
        }
        return pageKey;
    }

    private String normalizeVersion(String versionRaw) {
        String version = trimToNull(versionRaw);
        if (version == null) {
            return null;
        }
        if (version.length() > VERSION_MAX_LENGTH) {
            throw badRequest("version too long");
        }
        return version;
    }

    private List<String> normalizeColumnKeys(List<String> rawColumnKeys) {
        LinkedHashSet<String> deduplicated = new LinkedHashSet<String>();
        for (String rawColumnKey : rawColumnKeys) {
            String normalized = trimToNull(rawColumnKey);
            if (normalized != null) {
                deduplicated.add(normalized);
            }
        }
        return new ArrayList<String>(deduplicated);
    }

    private String serializeColumnKeys(List<String> columnKeys) {
        try {
            return objectMapper.writeValueAsString(
                    columnKeys == null ? Collections.<String>emptyList() : columnKeys
            );
        } catch (JsonProcessingException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to serialize preference data.");
        }
    }

    private List<String> deserializeColumnKeys(String rawColumnKeysJson) {
        String raw = trimToNull(rawColumnKeysJson);
        if (raw == null) {
            return Collections.emptyList();
        }

        try {
            JsonNode root = objectMapper.readTree(raw);
            if (root == null || root.isNull()) {
                return Collections.emptyList();
            }
            if (!root.isArray()) {
                return normalizeCommaSeparatedKeys(raw);
            }

            List<String> values = new ArrayList<String>();
            for (JsonNode item : root) {
                if (item == null || item.isNull()) {
                    continue;
                }
                String text = item.asText(null);
                if (text != null) {
                    values.add(text);
                }
            }
            return normalizeColumnKeys(values);
        } catch (Exception ex) {
            return normalizeCommaSeparatedKeys(raw);
        }
    }

    private List<String> normalizeCommaSeparatedKeys(String raw) {
        String[] segments = raw.split(",");
        List<String> values = new ArrayList<String>(segments.length);
        for (String segment : segments) {
            values.add(segment);
        }
        return normalizeColumnKeys(values);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private ApiRequestException badRequest(String message) {
        return new ApiRequestException(HttpStatus.BAD_REQUEST, "BAD_REQUEST", message);
    }
}
