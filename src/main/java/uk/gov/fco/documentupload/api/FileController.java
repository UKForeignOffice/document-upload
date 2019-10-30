package uk.gov.fco.documentupload.api;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.async.DeferredResult;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriComponentsBuilder;
import uk.gov.fco.documentupload.service.storage.StorageClient;
import uk.gov.fco.documentupload.service.storage.StorageException;

import java.util.concurrent.ForkJoinPool;

@RestController
@RequestMapping("/files")
@Slf4j
public class FileController {

    private static final Long REQUEST_TIMEOUT = 120000L; // Allow 2 minute timeout for scanning and storage

    private StorageClient storageClient;

    @Autowired
    public FileController(@NonNull StorageClient storageClient) {
        this.storageClient = storageClient;
    }

    @PostMapping
    public DeferredResult<ResponseEntity<?>> store(MultipartFile file, UriComponentsBuilder builder) {
        log.debug("Storing {}", file);

        DeferredResult<ResponseEntity<?>> output = new DeferredResult<>(REQUEST_TIMEOUT);

        output.onTimeout(() -> {
            log.warn("Timeout waiting for response");
            output.setResult(ResponseEntity.accepted().build());
        });

        // TODO: change this to ExecutorService configured via Spring and limited to X concurrent tasks
        ForkJoinPool.commonPool().submit(() -> {
            log.trace("Processing store file in separate thread");
            // TODO: scan for viruses

            try {
                String id = storageClient.store(file);
                output.setResult(
                        ResponseEntity
                                .created(builder.path("/files/{id}").build(id))
                                .build());
            } catch (StorageException e) {
                log.warn("Error storing file", e);
                output.setResult(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .build());
            }
        });

        log.trace("Returning deferred result");
        return output;
    }

    @GetMapping("/{id}")
    public ResponseEntity<byte[]> retrieve(@PathVariable String id) {
        log.debug("Retrieving file with id {}", id);
        return null;
    }
}
