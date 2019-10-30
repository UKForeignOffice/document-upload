package uk.gov.fco.documentupload.service.storage;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@ConditionalOnProperty(name = "storage.engine", havingValue = "s3")
public class S3StorageClient extends StorageClient {

    @Override
    public String store(MultipartFile file) throws StorageException {
        return null;
    }
}
