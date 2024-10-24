package uk.gov.fco.documentupload.service.merger;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import uk.gov.fco.documentupload.api.MergeUnavailableException;
import uk.gov.fco.documentupload.service.storage.FileUpload;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class MergeUnavailableMergerTest {
    private MergeUnavailableMerger merger = new MergeUnavailableMerger();


    @Test
    public void shouldThrowIfThereIsMoreThanOneFile() throws Exception {
        List<FileUpload> uploads = new ArrayList<>();
        MultipartFile multipartFile = new MockMultipartFile("test.odt", "This is a test file".getBytes());
        FileUpload upload = new FileUpload(multipartFile);
        uploads.add(upload);
        uploads.add(upload);

        assertThrows(MergeUnavailableException.class, () -> {
            merger.merge(uploads);
        });
    }

    @Test
    public void doesNotThrowIfThereIsOneFile() throws Exception {
        List<FileUpload> uploads = new ArrayList<>();
        MultipartFile multipartFile = new MockMultipartFile("test.odt", "This is a test file".getBytes());
        FileUpload upload = new FileUpload(multipartFile);
        uploads.add(upload);

        assertThat(merger.merge(uploads), is(upload));

    }
}
