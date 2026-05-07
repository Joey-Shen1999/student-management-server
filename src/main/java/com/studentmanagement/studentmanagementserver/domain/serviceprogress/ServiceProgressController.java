package com.studentmanagement.studentmanagementserver.domain.serviceprogress;

import com.studentmanagement.studentmanagementserver.service.ManagementAccessService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/teacher")
public class ServiceProgressController {

    private final ServiceProgressService serviceProgressService;
    private final ManagementAccessService managementAccessService;

    public ServiceProgressController(ServiceProgressService serviceProgressService,
                                     ManagementAccessService managementAccessService) {
        this.serviceProgressService = serviceProgressService;
        this.managementAccessService = managementAccessService;
    }

    @GetMapping("/students/{studentId}/service-progress")
    public ResponseEntity<ServiceProgressStateDto> getStudentServiceProgress(@PathVariable Long studentId,
                                                                             HttpServletRequest request) {
        managementAccessService.requireStudentAccountManagementAccess(request);
        return ResponseEntity.ok(serviceProgressService.getStudentServiceProgress(studentId));
    }

    @PostMapping("/students/{studentId}/service-progress")
    public ResponseEntity<ServiceProgressRecordDto> createRecord(@PathVariable Long studentId,
                                                                 @RequestBody(required = false) ServiceProgressRecordRequestDto requestBody,
                                                                 HttpServletRequest request) {
        managementAccessService.requireStudentAccountManagementAccess(request);
        return ResponseEntity.ok(serviceProgressService.createRecord(studentId, requestBody));
    }

    @PutMapping("/service-progress/{recordId}")
    public ResponseEntity<ServiceProgressRecordDto> updateRecord(@PathVariable Long recordId,
                                                                 @RequestBody(required = false) ServiceProgressRecordRequestDto requestBody,
                                                                 HttpServletRequest request) {
        managementAccessService.requireStudentAccountManagementAccess(request);
        return ResponseEntity.ok(serviceProgressService.updateRecord(recordId, requestBody));
    }

    @DeleteMapping("/service-progress/{recordId}")
    public ResponseEntity<Void> deleteRecord(@PathVariable Long recordId,
                                             HttpServletRequest request) {
        managementAccessService.requireStudentAccountManagementAccess(request);
        serviceProgressService.deleteRecord(recordId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/students/{studentId}/remark")
    public ResponseEntity<ServiceProgressStateDto> updateStudentRemark(@PathVariable Long studentId,
                                                                       @RequestBody(required = false) StudentRemarkUpdateRequestDto requestBody,
                                                                       HttpServletRequest request) {
        managementAccessService.requireStudentAccountManagementAccess(request);
        return ResponseEntity.ok(serviceProgressService.updateStudentRemark(studentId, requestBody));
    }
}
