package uk.gov.fco.documentupload.service.storage;

import lombok.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import uk.gov.fco.documentupload.config.EnvironmentUtil;

import java.io.InputStream;
import java.net.URI;

@Service
@ConditionalOnProperty(name = "storage.engine", havingValue = "s3")
public class S3StorageClient extends StorageClient {

    private static final Logger log = LoggerFactory.getLogger(S3StorageClient.class);

    private String bucket;

    private S3Client s3Client;

    public S3StorageClient(@Value("${storage.s3.bucket}") @NonNull String bucket, Environment environment) {
        this.bucket = bucket;

        if (EnvironmentUtil.isDevelopment(environment)) {
            log.info("Using S3 storage in development mode");
            String endpoint = System.getenv("AWS_ENDPOINT");
            String region = System.getenv("AWS_REGION");

            s3Client = S3Client.builder()
                    .endpointOverride(URI.create(endpoint))
                    .forcePathStyle(true)
                    .region(Region.of(region))
                    .build();
        } else {
            log.info("Using S3 storage in non-development mode");
            s3Client = S3Client.create();
        }
    }

    @Override
    public String store(FileUpload file) throws StorageException {
        String id = toId(file);

        try {
            s3Client.putObject(
                    PutObjectRequest.builder()
                            .bucket(bucket)
                            .key(id)
                            .contentType(file.getContentType())
                            .contentLength(file.getSize())
                            .build(),
                    RequestBody.fromInputStream(file.getInputStream(), file.getSize())
            );
            return id;
        } catch (Exception e) {
            throw new StorageException("Error uploading file to S3", e);
        }
    }

    @Override
    public long getSize(String id) throws StorageException {
        try {
            HeadObjectResponse response = s3Client.headObject(
                    HeadObjectRequest.builder().bucket(bucket).key(id).build());
            return response.contentLength();
        } catch (Exception e) {
            throw new StorageException("Error retrieving file size from S3", e);
        }
    }

    @Override
    public String getContentType(String id) throws StorageException {
        try {
            HeadObjectResponse response = s3Client.headObject(
                    HeadObjectRequest.builder().bucket(bucket).key(id).build());
            return response.contentType();
        } catch (Exception e) {
            throw new StorageException("Error retrieving file content type from S3", e);
        }
    }

    @Override
    public InputStream get(String id) throws StorageException {
        try {
            return s3Client.getObject(
                    GetObjectRequest.builder().bucket(bucket).key(id).build());
        } catch (Exception e) {
            throw new StorageException("Error retrieving file from S3", e);
        }
    }

    @Override
    public boolean exists(String id) throws StorageException {
        try {
            s3Client.headObject(HeadObjectRequest.builder().bucket(bucket).key(id).build());
            return true;
        } catch (NoSuchKeyException e) {
            return false;
        } catch (Exception e) {
            throw new StorageException("Error checking file exists in S3", e);
        }
    }

    @Override
    public void delete(String id) throws StorageException {
        try {
            s3Client.deleteObject(
                    DeleteObjectRequest.builder().bucket(bucket).key(id).build());
        } catch (Exception e) {
            throw new StorageException("Error deleting file from S3", e);
        }
    }
}
