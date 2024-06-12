package uk.gov.fco.documentupload.service.storage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.mockito.MockitoAnnotations.initMocks;

@ExtendWith(SpringExtension.class)
@SpringBootTest
public class FileStorageClientTest {

    private FileStorageClient fileStorageClient;

    private Path storageLocation;

    @BeforeEach
    public void setup() throws IOException {
        initMocks(this);
        storageLocation = Files.createTempDirectory("documentupload");
        fileStorageClient = new FileStorageClient(storageLocation);
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
