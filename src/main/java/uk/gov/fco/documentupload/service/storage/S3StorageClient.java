package uk.gov.fco.documentupload.service.storage;

import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3Object;
import lombok.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import uk.gov.fco.documentupload.config.EnvironmentUtil;

import java.io.InputStream;

@Service
@ConditionalOnProperty(name = "storage.engine", havingValue = "s3")
public class S3StorageClient extends StorageClient {

    private static final Logger log = LoggerFactory.getLogger(S3StorageClient.class);

    private String bucket;

    private AmazonS3 amazonS3;

    public S3StorageClient(@Value("${storage.s3.bucket}") @NonNull String bucket, Environment environment) {
        this.bucket = bucket;

        if (EnvironmentUtil.isDevelopment(environment)) {
            log.info("Using S3 storage in development mode");
            String endpoint = System.getenv("AWS_ENDPOINT");
            String region = System.getenv("AWS_REGION");

            amazonS3 = AmazonS3ClientBuilder.standard()
                    .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(endpoint, region))
                    .withPathStyleAccessEnabled(true)
                    .build();
        } else {
            log.info("Using S3 storage in non-development mode");
            amazonS3 = AmazonS3ClientBuilder.defaultClient();
        }
    }

    @Override
    public String store(FileUpload file) throws StorageException {
        String id = toId(file);

        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(file.getSize());
        metadata.setContentType(file.getContentType());

        try {
            amazonS3.putObject(bucket, id, file.getInputStream(), metadata);
            return id;
        } catch (Exception e) {
            throw new StorageException("Error uploading file to S3", e);
        }
    }

    @Override
    public long getSize(String id) throws StorageException {
        try {
            ObjectMetadata metadata = amazonS3.getObjectMetadata(bucket, id);
            return metadata.getContentLength();
        } catch (Exception e) {
            throw new StorageException("Error retrieving file size from S3", e);
        }
    }

    @Override
    public String getContentType(String id) throws StorageException {
        try {
            ObjectMetadata metadata = amazonS3.getObjectMetadata(bucket, id);
            return metadata.getContentType();
        } catch (Exception e) {
            throw new StorageException("Error retrieving file content type from S3", e);
        }
    }

    @Override
    public InputStream get(String id) throws StorageException {
        try {
            S3Object object = amazonS3.getObject(bucket, id);
            return object.getObjectContent();
        } catch (Exception e) {
            throw new StorageException("Error retrieving file from S3", e);
        }
    }

    @Override
    public boolean exists(String id) throws StorageException {
        try {
            return amazonS3.doesObjectExist(bucket, id);
        } catch (Exception e) {
            throw new StorageException("Error checking file exists in S3", e);
        }
    }

    @Override
    public void delete(String id) throws StorageException {
        try {
            amazonS3.deleteObject(bucket, id);
        } catch (Exception e) {
            throw new StorageException("Error deleting file from S3", e);
        }
    }
}
