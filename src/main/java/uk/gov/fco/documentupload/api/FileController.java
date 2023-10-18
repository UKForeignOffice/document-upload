package uk.gov.fco.documentupload.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.async.DeferredResult;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriComponentsBuilder;
import uk.gov.fco.documentupload.service.antivirus.AntiVirusService;
import uk.gov.fco.documentupload.service.merger.Merger;
import uk.gov.fco.documentupload.service.ocr.OCRService;
import uk.gov.fco.documentupload.service.storage.FileUpload;
import uk.gov.fco.documentupload.service.storage.StorageClient;
import uk.gov.fco.documentupload.service.storage.StorageException;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ForkJoinPool;

@RestController
@RequestMapping("/files")
@Slf4j
public class FileController {

    private static final Long REQUEST_TIMEOUT = 120000L; // Allow 2 minute timeout for scanning and storage

    private AntiVirusService antiVirusService;

    private StorageClient storageClient;

    private Collection<Merger> mergers;

    private OCRService ocrService;

    @Autowired
    public FileController(@NonNull AntiVirusService antiVirusService,
                          @NonNull StorageClient storageClient,
                          @NonNull Collection<Merger> mergers,
                          @NonNull OCRService ocrService) {
        this.antiVirusService = antiVirusService;
        this.storageClient = storageClient;
        this.mergers = mergers;
        this.ocrService = ocrService;
    }

    @PostMapping
    @ApiResponses({
            @ApiResponse(
                    responseCode = "400",
                    description = "No file was uploaded"),
            @ApiResponse(
                    responseCode = "422",
                    description = "Uploaded file contains a virus or was too blurry"),
            @ApiResponse(
                    responseCode = "201",
                    description = "File stored successfully",
                    headers = @Header(
                            name = "Location",
                            description = "Location of the uploaded file")),
            @ApiResponse(
                    responseCode = "202",
                    description = "Internal request processing is taking a long time, but the request has been accepted")
    })
    @Operation(
            summary = "Store a file",
            description = "Scans the file for viruses and returns the URL the file can be retrieved from."
    )
    public DeferredResult<ResponseEntity<String>> store(
            @Parameter(
                    name = "files",
                    description = "The files to store, max size 5mb",
                    required = true,
                    content = @Content(
                            schema = @Schema(
                                    type = "string",
                                    format = "binary"
                            )
                    )
            ) List<MultipartFile> files,
            UriComponentsBuilder builder) {
        log.debug("Storing {}", files);

        DeferredResult<ResponseEntity<String>> output = new DeferredResult<>(REQUEST_TIMEOUT);

        if (files == null || files.isEmpty()) {
            output.setResult(ResponseEntity.badRequest().build());
        } else {
            output.onTimeout(() -> {
                log.warn("Timeout waiting for response");
                output.setResult(ResponseEntity.accepted().build());
            });

            ForkJoinPool.commonPool().submit(() -> {
                log.trace("Processing store file in separate thread");
                try {
                    List<FileUpload> uploads = new ArrayList<>();
                    for (MultipartFile file : files) {
                        FileUpload fileUpload = new FileUpload(file);
                        uploads.add(fileUpload);
                    }

                    Merger merger = mergers.stream()
                            .filter(m -> m.supports(uploads))
                            .findFirst()
                            .orElseThrow(NoSupportedMergerException::new);

                    boolean clean = true;
                    for (FileUpload upload : uploads) {
                        if (!antiVirusService.isClean(upload)) {
                            clean = false;
                            break;
                        }
                    }

                    if (!clean) {
                        log.info("Virus scan failed for file");
                        output.setResult(ResponseEntity
                                .status(HttpStatus.UNPROCESSABLE_ENTITY)
                                .body("{\"error\": \"VirusScanError\", \"message\": \"Document contained a virus\"}"));
                    } else {
                        log.info("File is clean, checking image quality");
                        boolean passedQualityCheck = true;
                        for (FileUpload upload : uploads) {
                            if (!ocrService.passesQualityCheck(upload)) {
                                passedQualityCheck = false;
                                break;
                            }
                        }
                        if (!passedQualityCheck) {
                            log.info("Quality check failed for file");
                            output.setResult(ResponseEntity
                                    .status(HttpStatus.UNPROCESSABLE_ENTITY)
                                    .body("{\"error\": \"QualityThresholdError\", \"message\": \"Document did not pass quality threshold\"}"));
                        } else {
                            log.info("File is good quality, uploading file");
                            String id = storageClient.store(merger.merge(uploads));
                            output.setResult(
                                    ResponseEntity
                                            .created(builder.path("/files/{id}").build(id)).build());
                        }
                    }
                } catch (NoSupportedMergerException e) {
                    log.info("No supported merger found", e);
                    output.setResult(ResponseEntity
                            .status(HttpStatus.BAD_REQUEST)
                            .build());
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
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "File retrieved OK",
                    content = @Content(
                            schema = @Schema(
                                    type = "string",
                                    format = "binary"
                            )
                    )),
            @ApiResponse(
                    responseCode = "404",
                    description = "No file was found"),
            @ApiResponse(
                    responseCode = "500",
                    description = "There was an internal error"),
    })
    @Operation(
            summary = "Retrieve a file",
            description = "Returns the file with the given ID.."
    )
    public void retrieve(
            @Parameter(
                    name = "id",
                    description = "The ID of the file to retrieve",
                    required = true,
                    schema = @Schema(type = "string"))
            @PathVariable String id,
            HttpServletResponse response) throws IOException {
        log.debug("Retrieving file with id {}", id);

        try {
            if (!storageClient.exists(id)) {
                log.debug("No file exists for id, returning 404");
                response.setStatus(HttpStatus.NOT_FOUND.value());
            } else {
                log.trace("File exists, setting headers and streaming");
                response.setStatus(HttpStatus.OK.value());
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
    @ApiResponses({
            @ApiResponse(
                    responseCode = "204",
                    description = "File deleted OK"),
            @ApiResponse(
                    responseCode = "500",
                    description = "There was an internal error"),
    })
    @Operation(
            summary = "Delete a file"
    )
    public ResponseEntity<Void> delete(
            @Parameter(
                    name = "id",
                    description = "The ID of the file to retrieve",
                    required = true,
                    schema = @Schema(type = "string"))
            @PathVariable String id) {
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
