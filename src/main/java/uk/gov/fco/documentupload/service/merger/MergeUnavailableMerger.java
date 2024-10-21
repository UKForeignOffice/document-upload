package uk.gov.fco.documentupload.service.merger;

import org.springframework.stereotype.Service;
import uk.gov.fco.documentupload.api.MergeUnavailableException;
import uk.gov.fco.documentupload.service.storage.FileUpload;

import java.util.HashSet;
import java.util.List;
import java.util.Set;


/**
 * Use this class for file types that cannot be merged. It will only accept one file.
 */
@Service
public class MergeUnavailableMerger implements Merger {

    private static final Set<String> SUPPORTED_CONTENT_TYPES = new HashSet<String>() {{
        add("application/vnd.oasis.opendocument.text");
    }};

    @Override
    public boolean supports(List<FileUpload> uploads) {
        for (FileUpload upload : uploads) {
            if (!SUPPORTED_CONTENT_TYPES.contains(upload.getContentType())) {
                return false;
            }
        }
        return true;
    }

    @Override
    public FileUpload merge(List<FileUpload> uploads) throws MergeUnavailableException {
        if (uploads.size() == 1) {
            return uploads.get(0);
        }
        throw new MergeUnavailableException();
    }
}
