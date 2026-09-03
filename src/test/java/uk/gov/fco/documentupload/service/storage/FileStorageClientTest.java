package uk.gov.fco.documentupload.service.storage;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import uk.gov.fco.documentupload.TestConfig;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.mockito.MockitoAnnotations.openMocks;

@SpringBootTest(classes = TestConfig.class)
public class FileStorageClientTest {

    private FileStorageClient fileStorageClient;
    private Path storageLocation;

    private AutoCloseable closeable;

    @BeforeEach
    public void open() {
        closeable = openMocks(this);
        try {
            storageLocation = Files.createTempDirectory("documentupload");
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
        fileStorageClient = new FileStorageClient(storageLocation);
    }

    @AfterEach
    public void release() throws Exception {
        closeable.close();
        storageLocation.toFile().delete();
    }

    @Test
    public void shouldStoreFileUpload() throws Exception {
        MultipartFile multipartFile = new MockMultipartFile("cyb.jpg", "This is a test file".getBytes());
        FileUpload upload = new FileUpload(multipartFile);

        String id = fileStorageClient.store(upload);

        assertThat(id, notNullValue());
        assertThat(Files.exists(storageLocation.resolve(id)), is(true));
    }
}
