package uk.gov.fco.documentupload.service.storage;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.InputStream;

@Service
@ConditionalOnProperty(name = "storage.engine", havingValue = "s3")
public class S3StorageClient extends StorageClient {

    @Override
    public String store(FileUpload file) throws StorageException {
        return null;
    }

    @Override
    public int getSize(String id) {
        return 0;
    }

    @Override
    public String getContentType(String id) {
        return null;
    }

    @Override
    public InputStream get(String id) {
        return null;
    }

    @Override
    public boolean exists(String id) {
        return false;
    }
}
