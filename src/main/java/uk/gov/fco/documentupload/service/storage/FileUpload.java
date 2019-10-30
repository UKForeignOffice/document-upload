package uk.gov.fco.documentupload.service.storage;

import lombok.NonNull;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

public class FileUpload {

    private String id = UUID.randomUUID().toString();

    private long size;

    private String name;

    private String contentType;

    private Path path;

    public FileUpload(@NonNull MultipartFile file) throws IOException {
        this.size = file.getSize();
        this.name = file.getOriginalFilename();
        this.contentType = file.getContentType();

        path = Files.createTempFile(id, ".upload");

        file.transferTo(path);
    }

    public String getId() {
        return id;
    }

    public long getSize() {
        return size;
    }

    public String getName() {
        return name;
    }

    public String getContentType() {
        return contentType;
    }

    public InputStream getInputStream() throws IOException {
        return new BufferedInputStream(Files.newInputStream(path));
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("id", id)
                .append("size", size)
                .append("name", name)
                .append("contentType", contentType)
                .toString();
    }
}
