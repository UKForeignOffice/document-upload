package uk.gov.fco.documentupload.service.storage;

import java.io.InputStream;

public abstract class StorageClient {

    String toId(FileUpload file) {
        String fileName = file.getName();
        String extension = "";

        if (fileName != null) {
            int i = fileName.lastIndexOf('.');
            if (i > 0) {
                extension = fileName.substring(i);
            }
        }
        return file.getId() + extension;
    }

    public abstract String store(FileUpload file) throws StorageException;

    public abstract long getSize(String id) throws StorageException;

    public abstract String getContentType(String id) throws StorageException;

    public abstract InputStream get(String id) throws StorageException;

    public abstract boolean exists(String id) throws StorageException;

    public abstract void delete(String id) throws StorageException;
}
