package uk.gov.fco.documentupload.service.merger;

import uk.gov.fco.documentupload.service.storage.FileUpload;

import java.io.IOException;
import java.util.List;

public interface Merger {

    boolean supports(List<FileUpload> uploads);

    FileUpload merge(List<FileUpload> uploads) throws IOException;
}
