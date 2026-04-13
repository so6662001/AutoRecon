package com.autorecon.service;

import com.autorecon.common.config.AutoReconProperties;
import com.autorecon.common.config.MinioProperties;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Object storage: MinIO in production, local filesystem in demo mode.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class OssStorageService {

    private final AutoReconProperties autoReconProperties;
    private final MinioProperties minioProperties;

    /**
     * Upload UTF-8 text content to storage.
     *
     * @param path    logical path (no leading slash), e.g. agreements/user-agreement/v1.0_20260321.html
     * @param content file body
     * @return accessible URL or null on failure
     */
    public String upload(String path, String content) {
        if (content == null) {
            content = "";
        }
        if (autoReconProperties.isDemoMode()) {
            return uploadLocal(path, content);
        }
        return uploadMinio(path, content);
    }

    private String uploadLocal(String path, String content) {
        try {
            Path filePath = Path.of("/tmp/autorecon-oss", path);
            Files.createDirectories(filePath.getParent());
            Files.writeString(filePath, content);
            String url = "/oss/" + path;
            log.info("Uploaded to local OSS: {}", url);
            return url;
        } catch (Exception e) {
            log.error("OSS upload failed: {}", e.getMessage());
            return null;
        }
    }

    private String uploadMinio(String path, String content) {
        try {
            String endpoint = minioProperties.getEndpoint().replaceAll("/$", "");
            MinioClient client = MinioClient.builder()
                    .endpoint(endpoint)
                    .credentials(minioProperties.getAccessKey(), minioProperties.getSecretKey())
                    .build();
            String bucket = minioProperties.getBucketName();
            if (!client.bucketExists(BucketExistsArgs.builder().bucket(bucket).build())) {
                client.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
            }
            byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
            client.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucket)
                            .object(path)
                            .stream(new ByteArrayInputStream(bytes), bytes.length, -1)
                            .contentType("text/html; charset=utf-8")
                            .build());
            String url = endpoint + "/" + bucket + "/" + path;
            log.info("Uploaded to MinIO: {}", url);
            return url;
        } catch (Exception e) {
            log.error("MinIO upload failed: {}", e.getMessage());
            return null;
        }
    }

    public String getContent(String url) {
        if (url == null) {
            return null;
        }
        if (url.startsWith("/oss/")) {
            try {
                String path = url.replace("/oss/", "");
                return Files.readString(Path.of("/tmp/autorecon-oss", path));
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }
}
