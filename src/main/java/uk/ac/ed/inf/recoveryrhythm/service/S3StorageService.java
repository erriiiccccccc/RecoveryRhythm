package uk.ac.ed.inf.recoveryrhythm.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class S3StorageService {

    private final S3Client s3Client;

    @Value("${recovery-rhythm.s3.bucket:recovery-rhythm-evidence}")
    private String bucketName;

    public String putEvidenceObject(UUID userId, String fileName, String contentType, byte[] content) {
        String safeName = fileName == null ? "evidence.jpg" : fileName.replaceAll("[^a-zA-Z0-9._-]", "_");
        String objectKey = "evidence/" + userId + "/" + UUID.randomUUID() + "-" + safeName;

        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(objectKey)
                .contentType(contentType)
                .build();
        s3Client.putObject(request, RequestBody.fromBytes(content));
        return objectKey;
    }

    public String putProfilePhoto(UUID userId, String fileName, String contentType, byte[] content) {
        String safeName = fileName == null ? "profile.jpg" : fileName.replaceAll("[^a-zA-Z0-9._-]", "_");
        String objectKey = "profiles/" + userId + "/" + UUID.randomUUID() + "-" + safeName;
        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(objectKey)
                .contentType(contentType)
                .build();
        s3Client.putObject(request, RequestBody.fromBytes(content));
        return objectKey;
    }

    public byte[] getEvidenceObject(String objectKey) {
        GetObjectRequest request = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(objectKey)
                .build();
        ResponseBytes<GetObjectResponse> objectBytes = s3Client.getObjectAsBytes(request);
        return objectBytes.asByteArray();
    }

    public void ensureBucketExists() {
        try {
            s3Client.headBucket(HeadBucketRequest.builder().bucket(bucketName).build());
        } catch (S3Exception ex) {
            s3Client.createBucket(CreateBucketRequest.builder().bucket(bucketName).build());
            log.info("Created S3 bucket {}", bucketName);
        }
    }
}
