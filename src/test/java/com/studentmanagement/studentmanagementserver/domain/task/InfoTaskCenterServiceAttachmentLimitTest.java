package com.studentmanagement.studentmanagementserver.domain.task;

import com.studentmanagement.studentmanagementserver.domain.notification.EmailService;
import com.studentmanagement.studentmanagementserver.repo.InfoTaskAttachmentRepository;
import com.studentmanagement.studentmanagementserver.repo.InfoTaskRecipientRepository;
import com.studentmanagement.studentmanagementserver.repo.InfoTaskRepository;
import com.studentmanagement.studentmanagementserver.repo.InfoVolunteerTaskItemRepository;
import com.studentmanagement.studentmanagementserver.repo.StudentProfileRepository;
import com.studentmanagement.studentmanagementserver.repo.StudentRepository;
import com.studentmanagement.studentmanagementserver.repo.TeacherRepository;
import com.studentmanagement.studentmanagementserver.repo.TeacherStudentRepository;
import com.studentmanagement.studentmanagementserver.service.ApiRequestException;
import com.studentmanagement.studentmanagementserver.service.AuthSessionService;
import org.junit.jupiter.api.Test;
import org.springframework.core.task.TaskExecutor;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class InfoTaskCenterServiceAttachmentLimitTest {

    private static final long MB = 1024L * 1024L;

    @Test
    void acceptsAttachmentsAtPerFileAndCombinedLimits() {
        InfoTaskCenterService service = createService();

        assertDoesNotThrow(() -> service.assertAttachmentsWithinLimits(Arrays.asList(
                file("one.bin", 30L * MB),
                file("two.bin", 30L * MB),
                file("three.bin", 30L * MB),
                file("four.bin", 10L * MB)
        )));
    }

    @Test
    void rejectsAttachmentLargerThanThirtyMb() {
        InfoTaskCenterService service = createService();

        assertThrows(ApiRequestException.class, () ->
                service.assertAttachmentsWithinLimits(Arrays.asList(file("large.bin", 30L * MB + 1L))));
    }

    @Test
    void rejectsAttachmentsLargerThanOneHundredMbCombined() {
        InfoTaskCenterService service = createService();

        assertThrows(ApiRequestException.class, () -> service.assertAttachmentsWithinLimits(Arrays.asList(
                file("one.bin", 30L * MB),
                file("two.bin", 30L * MB),
                file("three.bin", 30L * MB),
                file("four.bin", 10L * MB + 1L)
        )));
    }

    @Test
    void acceptsNewAttachmentsWhenExistingAndNewTotalIsAtLimit() {
        InfoTaskCenterService service = createService();

        assertDoesNotThrow(() ->
                service.assertAttachmentsWithinLimits(70L * MB, Arrays.asList(file("new.bin", 30L * MB))));
    }

    @Test
    void rejectsNewAttachmentsWhenExistingAndNewTotalExceedsLimit() {
        InfoTaskCenterService service = createService();

        assertThrows(ApiRequestException.class, () ->
                service.assertAttachmentsWithinLimits(70L * MB, Arrays.asList(file("new.bin", 30L * MB + 1L))));
    }

    private MultipartFile file(String name, long size) {
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getName()).thenReturn(name);
        when(file.getSize()).thenReturn(size);
        return file;
    }

    private InfoTaskCenterService createService() {
        return new InfoTaskCenterService(
                mock(AuthSessionService.class),
                mock(InfoTaskRepository.class),
                mock(InfoTaskRecipientRepository.class),
                mock(InfoTaskAttachmentRepository.class),
                mock(InfoVolunteerTaskItemRepository.class),
                mock(StudentRepository.class),
                mock(StudentProfileRepository.class),
                mock(TeacherRepository.class),
                mock(TeacherStudentRepository.class),
                mock(EmailService.class),
                mock(InfoTaskAttachmentStorageService.class),
                mock(TaskExecutor.class),
                false
        );
    }
}
