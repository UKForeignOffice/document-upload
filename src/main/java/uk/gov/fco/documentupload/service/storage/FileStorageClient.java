package uk.gov.fco.documentupload.service.storage;

import lombok.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;

@Service
@ConditionalOnProperty(name = "storage.engine", havingValue = "file")
public class FileStorageClient extends StorageClient {

    private Path storageLocation;

    public FileStorageClient(@Value("${storage.file.location}") @NonNull Path storageLocation) {
        this.storageLocation = storageLocation;
    }

    @Override
    public String store(MultipartFile file) throws StorageException {
        try {
            String id = toId(file);
            file.transferTo(storageLocation.resolve(id));
            return id;
        } catch (IOException e) {
            throw new StorageException("Error copying file to destination", e);
        }
    }
}
