package com.autorecon.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * MinIO / S3-compatible storage settings.
 */
@Data
@ConfigurationProperties(prefix = "minio")
public class MinioProperties {

    private String endpoint = "http://localhost:9000";
    private String accessKey = "minioadmin";
    private String secretKey = "minioadmin";
    private String bucketName = "autorecon";
}
