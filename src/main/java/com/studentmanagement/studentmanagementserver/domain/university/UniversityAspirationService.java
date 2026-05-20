package com.studentmanagement.studentmanagementserver.domain.university;

import com.studentmanagement.studentmanagementserver.domain.enums.UserRole;
import com.studentmanagement.studentmanagementserver.domain.student.Student;
import com.studentmanagement.studentmanagementserver.domain.user.User;
import com.studentmanagement.studentmanagementserver.repo.StudentRepository;
import com.studentmanagement.studentmanagementserver.repo.UniversityAspirationRepository;
import com.studentmanagement.studentmanagementserver.repo.UniversityProgramRepository;
import com.studentmanagement.studentmanagementserver.repo.UniversityRepository;
import com.studentmanagement.studentmanagementserver.service.AuthSessionService;
import com.studentmanagement.studentmanagementserver.service.MustChangePasswordRequiredException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class UniversityAspirationService {

    private static final int MAX_NOTES_LENGTH = 5000;

    private final UniversityAspirationRepository aspirationRepository;
    private final UniversityRepository universityRepository;
    private final UniversityProgramRepository programRepository;
    private final StudentRepository studentRepository;
    private final AuthSessionService authSessionService;

    public UniversityAspirationService(UniversityAspirationRepository aspirationRepository,
                                       UniversityRepository universityRepository,
                                       UniversityProgramRepository programRepository,
                                       StudentRepository studentRepository,
                                       AuthSessionService authSessionService) {
        this.aspirationRepository = aspirationRepository;
        this.universityRepository = universityRepository;
        this.programRepository = programRepository;
        this.studentRepository = studentRepository;
        this.authSessionService = authSessionService;
    }

    @Transactional(readOnly = true)
    public List<UniversityAspirationDto> listByStudent(Long studentId, HttpServletRequest request) {
        Student student = requireStudent(studentId);
        requireStudentAccess(student.getId(), request);
        return toDtos(aspirationRepository.findByStudent_IdOrderBySortOrderAscIdAsc(student.getId()));
    }

    @Transactional
    public UniversityAspirationDto create(Long studentId,
                                          UniversityAspirationRequest requestBody,
                                          HttpServletRequest request) {
        Student student = requireStudent(studentId);
        requireStudentAccess(student.getId(), request);
        University university = requireActiveUniversity(requestBody == null ? null : requestBody.getUniversityId());
        UniversityProgram program = requireActiveProgram(requestBody == null ? null : requestBody.getProgramId());
        ensureProgramBelongsToUniversity(program, university);

        Integer maxSortOrder = aspirationRepository.findMaxSortOrderByStudentId(student.getId());
        int nextSortOrder = maxSortOrder == null ? 1 : maxSortOrder.intValue() + 1;
        UniversityAspiration aspiration = new UniversityAspiration(student, university, program, nextSortOrder);
        aspiration.setNotes(normalizeNotes(requestBody == null ? null : requestBody.getNotes()));
        return toDto(aspirationRepository.save(aspiration));
    }

    @Transactional
    public UniversityAspirationDto update(Long aspirationId,
                                          UniversityAspirationRequest requestBody,
                                          HttpServletRequest request) {
        UniversityAspiration aspiration = requireAspiration(aspirationId);
        Long studentId = aspiration.getStudent() == null ? null : aspiration.getStudent().getId();
        requireStudentAccess(studentId, request);

        University university = requireActiveUniversity(requestBody == null ? null : requestBody.getUniversityId());
        UniversityProgram program = requireActiveProgram(requestBody == null ? null : requestBody.getProgramId());
        ensureProgramBelongsToUniversity(program, university);

        aspiration.setUniversity(university);
        aspiration.setProgram(program);
        aspiration.setNotes(normalizeNotes(requestBody == null ? null : requestBody.getNotes()));
        return toDto(aspirationRepository.save(aspiration));
    }

    @Transactional
    public void delete(Long aspirationId, HttpServletRequest request) {
        UniversityAspiration aspiration = requireAspiration(aspirationId);
        Long studentId = aspiration.getStudent() == null ? null : aspiration.getStudent().getId();
        requireStudentAccess(studentId, request);
        aspirationRepository.delete(aspiration);
        aspirationRepository.flush();
        normalizeSortOrders(studentId);
    }

    @Transactional
    public List<UniversityAspirationDto> reorder(Long studentId,
                                                 List<UniversityAspirationReorderRequest> requestBody,
                                                 HttpServletRequest request) {
        Student student = requireStudent(studentId);
        requireStudentAccess(student.getId(), request);

        List<UniversityAspiration> existing =
                aspirationRepository.findByStudent_IdOrderBySortOrderAscIdAsc(student.getId());
        if (existing.isEmpty()) {
            return new ArrayList<UniversityAspirationDto>();
        }
        if (requestBody == null || requestBody.isEmpty()) {
            throw new IllegalArgumentException("reorder request is required");
        }

        Map<Long, UniversityAspiration> existingById = new HashMap<Long, UniversityAspiration>();
        for (UniversityAspiration aspiration : existing) {
            existingById.put(aspiration.getId(), aspiration);
        }

        List<UniversityAspirationReorderRequest> normalizedRequest =
                new ArrayList<UniversityAspirationReorderRequest>(requestBody);
        Collections.sort(normalizedRequest, new Comparator<UniversityAspirationReorderRequest>() {
            @Override
            public int compare(UniversityAspirationReorderRequest left, UniversityAspirationReorderRequest right) {
                int leftOrder = left == null || left.getSortOrder() == null ? Integer.MAX_VALUE : left.getSortOrder();
                int rightOrder = right == null || right.getSortOrder() == null ? Integer.MAX_VALUE : right.getSortOrder();
                if (leftOrder != rightOrder) {
                    return leftOrder < rightOrder ? -1 : 1;
                }
                Long leftId = left == null ? null : left.getId();
                Long rightId = right == null ? null : right.getId();
                if (leftId == null && rightId == null) return 0;
                if (leftId == null) return 1;
                if (rightId == null) return -1;
                return leftId.compareTo(rightId);
            }
        });

        Set<Long> seenIds = new HashSet<Long>();
        int nextOrder = 1;
        for (UniversityAspirationReorderRequest item : normalizedRequest) {
            Long id = item == null ? null : item.getId();
            if (id == null || !existingById.containsKey(id)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "All reordered aspirations must belong to the student.");
            }
            if (!seenIds.add(id)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Duplicate aspiration id in reorder request: " + id);
            }
            existingById.get(id).setSortOrder(nextOrder++);
        }
        if (seenIds.size() != existing.size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Reorder request must include all aspirations for the student.");
        }

        aspirationRepository.saveAll(existing);
        return toDtos(aspirationRepository.findByStudent_IdOrderBySortOrderAscIdAsc(student.getId()));
    }

    private void normalizeSortOrders(Long studentId) {
        if (studentId == null) {
            return;
        }
        List<UniversityAspiration> aspirations =
                aspirationRepository.findByStudent_IdOrderBySortOrderAscIdAsc(studentId);
        int sortOrder = 1;
        for (UniversityAspiration aspiration : aspirations) {
            aspiration.setSortOrder(sortOrder++);
        }
        aspirationRepository.saveAll(aspirations);
    }

    private Student requireStudent(Long studentId) {
        if (studentId == null || studentId.longValue() <= 0L) {
            throw new IllegalArgumentException("studentId must be positive");
        }
        return studentRepository.findById(studentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found: " + studentId));
    }

    private UniversityAspiration requireAspiration(Long aspirationId) {
        if (aspirationId == null || aspirationId.longValue() <= 0L) {
            throw new IllegalArgumentException("aspirationId must be positive");
        }
        return aspirationRepository.findById(aspirationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "University aspiration not found: " + aspirationId));
    }

    private University requireActiveUniversity(Long universityId) {
        if (universityId == null || universityId.longValue() <= 0L) {
            throw new IllegalArgumentException("universityId is required");
        }
        University university = universityRepository.findById(universityId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "University not found: " + universityId));
        if (!university.isActive()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "University is not active.");
        }
        return university;
    }

    private UniversityProgram requireActiveProgram(Long programId) {
        if (programId == null || programId.longValue() <= 0L) {
            throw new IllegalArgumentException("programId is required");
        }
        UniversityProgram program = programRepository.findById(programId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "University program not found: " + programId));
        if (!program.isActive()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "University program is not active.");
        }
        return program;
    }

    private void ensureProgramBelongsToUniversity(UniversityProgram program, University university) {
        Long programUniversityId = program.getUniversity() == null ? null : program.getUniversity().getId();
        if (programUniversityId == null || !programUniversityId.equals(university.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "programId does not belong to universityId.");
        }
    }

    private void requireStudentAccess(Long studentId, HttpServletRequest request) {
        if (studentId == null || studentId.longValue() <= 0L) {
            throw new IllegalArgumentException("studentId must be positive");
        }
        User operator = authSessionService.requireAuthenticatedUser(request);
        if (operator.isMustChangePassword()) {
            throw new MustChangePasswordRequiredException();
        }

        if (operator.getRole() == UserRole.ADMIN) {
            return;
        }
        if (operator.getRole() == UserRole.STUDENT) {
            Student currentStudent = studentRepository.findByUser_Id(operator.getId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden: student binding required."));
            if (studentId.equals(currentStudent.getId())) {
                return;
            }
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden: student can only access own aspirations.");
        }
        if (operator.getRole() == UserRole.TEACHER) {
            return;
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden: teacher/admin/student role required.");
    }

    private String normalizeNotes(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        if (normalized.length() > MAX_NOTES_LENGTH) {
            throw new IllegalArgumentException("notes must be at most " + MAX_NOTES_LENGTH + " characters");
        }
        return normalized;
    }

    private List<UniversityAspirationDto> toDtos(List<UniversityAspiration> aspirations) {
        List<UniversityAspirationDto> dtos = new ArrayList<UniversityAspirationDto>(aspirations.size());
        for (UniversityAspiration aspiration : aspirations) {
            dtos.add(toDto(aspiration));
        }
        return dtos;
    }

    private UniversityAspirationDto toDto(UniversityAspiration aspiration) {
        University university = aspiration.getUniversity();
        UniversityProgram program = aspiration.getProgram();

        UniversityAspirationDto dto = new UniversityAspirationDto();
        dto.setAspirationId(aspiration.getId());
        dto.setStudentId(aspiration.getStudent() == null ? null : aspiration.getStudent().getId());
        dto.setUniversityId(university == null ? null : university.getId());
        dto.setUniversityName(university == null ? null : university.getName());
        dto.setProgramId(program == null ? null : program.getId());
        dto.setProgramName(program == null ? null : program.getProgramName());
        dto.setFacultyName(program == null ? null : program.getFacultyName());
        dto.setDegreeType(program == null ? null : program.getDegreeType());
        dto.setNotes(aspiration.getNotes());
        dto.setSortOrder(aspiration.getSortOrder());
        dto.setCreatedAt(aspiration.getCreatedAt());
        dto.setUpdatedAt(aspiration.getUpdatedAt());
        return dto;
    }
}
