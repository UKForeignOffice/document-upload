package uk.gov.fco.documentupload.api;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.glassfish.jersey.server.monitoring.ResponseMXBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.async.DeferredResult;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriComponentsBuilder;
import uk.gov.fco.documentupload.service.antivirus.AntiVirusService;
import uk.gov.fco.documentupload.service.storage.FileUpload;
import uk.gov.fco.documentupload.service.storage.StorageClient;
import uk.gov.fco.documentupload.service.storage.StorageException;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.concurrent.ForkJoinPool;

@RestController
@RequestMapping("/files")
@Slf4j
public class FileController {

    private static final Long REQUEST_TIMEOUT = 120000L; // Allow 2 minute timeout for scanning and storage

    private AntiVirusService antiVirusService;

    private StorageClient storageClient;

    @Autowired
    public FileController(@NonNull AntiVirusService antiVirusService, @NonNull StorageClient storageClient) {
        this.antiVirusService = antiVirusService;
        this.storageClient = storageClient;
    }

    @PostMapping
    public DeferredResult<ResponseEntity<?>> store(MultipartFile file, UriComponentsBuilder builder) {
        log.debug("Storing {}", file);

        DeferredResult<ResponseEntity<?>> output = new DeferredResult<>(REQUEST_TIMEOUT);

        if (file == null) {
            output.setResult(ResponseEntity.badRequest().build());
        } else {
            output.onTimeout(() -> {
                log.warn("Timeout waiting for response");
                output.setResult(ResponseEntity.accepted().build());
            });

            // TODO: change this to ExecutorService configured via Spring and limited to X concurrent tasks
            ForkJoinPool.commonPool().submit(() -> {
                log.trace("Processing store file in separate thread");
                try {
                    FileUpload upload = new FileUpload(file);
                    if (!antiVirusService.isClean(upload)) {
                        log.trace("Virus scan failed for file");
                        // TODO: is there a better status code to use for "there's a virus"
                        output.setResult(ResponseEntity
                                .badRequest()
                                .build());
                    } else {
                        log.trace("File is clean, storing");
                        String id = storageClient.store(upload);
                        output.setResult(
                                ResponseEntity
                                        .created(builder.path("/files/{id}").build(id))
                                        .build());
                    }
                } catch (StorageException | IOException e) {
                    log.error("Error storing file", e);
                    output.setResult(ResponseEntity
                            .status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .build());
                }
            });
        }

        log.trace("Returning deferred result");
        return output;
    }

    @GetMapping("/{id:.+}")
    public void retrieve(@PathVariable String id, HttpServletResponse response) throws IOException {
        log.debug("Retrieving file with id {}", id);

        try {
            if (!storageClient.exists(id)) {
                log.debug("No file exists for id, returning 404");
                response.setStatus(HttpStatus.NOT_FOUND.value());
            } else {
                log.trace("File exists, setting headers and streaming");
                response.setContentLength((int) storageClient.getSize(id));
                response.setContentType(storageClient.getContentType(id));
                IOUtils.copy(storageClient.get(id), response.getOutputStream());
            }
        } catch (StorageException e) {
            log.error("Error retrieving file", e);
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
        }

        log.trace("Flushing response buffer");
        response.flushBuffer();
    }

    @DeleteMapping("/{id:.+}")
    public ResponseEntity<?> delete(@PathVariable String id) {
        log.debug("Deleting file with id {}", id);

        try {
            storageClient.delete(id);
            return ResponseEntity.noContent().build();
        } catch (StorageException e) {
            log.error("Error deleting file", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
