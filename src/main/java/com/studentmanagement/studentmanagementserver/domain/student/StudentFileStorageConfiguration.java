package com.studentmanagement.studentmanagementserver.domain.student;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;

@Configuration
public class StudentFileStorageConfiguration {

    @Bean
    @ConditionalOnProperty(name = "app.storage.type", havingValue = "s3")
    public S3Client studentFileS3Client(
            @Value("${app.storage.s3.region:}") String region
    ) {
        S3ClientBuilder builder = S3Client.builder();
        if (StringUtils.hasText(region)) {
            builder.region(Region.of(region.trim()));
        }
        return builder.build();
    }
}
