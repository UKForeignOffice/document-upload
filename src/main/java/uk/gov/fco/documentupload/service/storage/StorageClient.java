package uk.gov.fco.documentupload.service.storage;

import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.UUID;

public abstract class StorageClient {

    protected String toId(MultipartFile file) {
        String fileName = file.getOriginalFilename();
        String id = UUID.randomUUID().toString();
        String extension = "";

        if (fileName != null) {
            int i = fileName.lastIndexOf('.');
            if (i > 0) {
                extension = fileName.substring(i);
            }
        }
        return id + extension;
    }

    public abstract String store(MultipartFile file) throws StorageException;
}
