package uk.gov.fco.documentupload.api;

import net.bytebuddy.dynamic.scaffold.MethodGraph;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.fco.documentupload.service.antivirus.AntiVirusService;
import uk.gov.fco.documentupload.service.fileCheck.FileCheckService;
import uk.gov.fco.documentupload.service.merger.Merger;
import uk.gov.fco.documentupload.service.ocr.OCRService;
import uk.gov.fco.documentupload.service.storage.FileStorageClient;

import java.util.Collection;

import static org.mockito.MockitoAnnotations.initMocks;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

@RunWith(SpringRunner.class)
@SpringBootTest
public class FileControllerTest {

    private MockMvc mockMvc;

    @InjectMocks
    private FileController controller;

    @Mock
    private AntiVirusService antiVirusService;

    @Mock
    private FileStorageClient storageClient;

    @Mock
    private Collection<Merger> mergers;

    @Mock
    private OCRService ocrService;

    @Mock
    private FileCheckService fileCheckService;

    @Before
    public void setup() {
        initMocks(this);
        mockMvc = standaloneSetup(controller)
                .build();
    }

    @Test
    public void shouldReturn404ForMissingFile() throws Exception {
        mockMvc.perform(get("/files/cyb.jpg"))
                .andDo(print())
                .andExpect(status().isNotFound());
    }
}
