package uk.gov.fco.documentupload.service.storage;

import lombok.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;

@Service
@ConditionalOnProperty(name = "storage.engine", havingValue = "file")
public class FileStorageClient extends StorageClient {

    private Path storageLocation;

    public FileStorageClient(@Value("${storage.file.location}") @NonNull Path storageLocation) {
        this.storageLocation = storageLocation;
    }

    @Override
    public String store(FileUpload file) throws StorageException {
        try {
            String id = toId(file);
            Files.copy(file.getInputStream(), getPath(id));
            return id;
        } catch (IOException e) {
            throw new StorageException("Error copying file to destination", e);
        }
    }

    private Path getPath(String id) {
        return storageLocation.resolve(id);
    }

    @Override
    public long getSize(String id) throws StorageException {
        try {
            return Files.size(getPath(id));
        } catch (IOException e) {
            throw new StorageException("Error reading file size", e);
        }
    }

    @Override
    public String getContentType(String id) {
        return null;
    }

    @Override
    public InputStream get(String id) throws StorageException {
        try {
            return Files.newInputStream(getPath(id));
        } catch (IOException e) {
            throw new StorageException("Error opening file", e);
        }
    }

    @Override
    public boolean exists(String id) {
        return Files.exists(getPath(id));
    }

    @Override
    public void delete(String id) throws StorageException {
        try {
            Files.delete(getPath(id));
        } catch (NoSuchFileException e) {
            // Ignore
        } catch (IOException e) {
            throw new StorageException("Error deleting file", e);
        }
    }
}
