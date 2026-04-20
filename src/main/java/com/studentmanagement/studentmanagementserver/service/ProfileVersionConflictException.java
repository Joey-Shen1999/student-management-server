package com.studentmanagement.studentmanagementserver.service;

public class ProfileVersionConflictException extends RuntimeException {

    private final Long currentVersion;

    public ProfileVersionConflictException(Long currentVersion) {
        super("Profile version conflict.");
        this.currentVersion = currentVersion == null ? 0L : currentVersion;
    }

    public Long getCurrentVersion() {
        return currentVersion;
    }
}
