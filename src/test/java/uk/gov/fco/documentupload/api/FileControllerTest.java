package uk.gov.fco.documentupload.api;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.fco.documentupload.TestConfig;

import static org.mockito.MockitoAnnotations.openMocks;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = TestConfig.class)
@AutoConfigureMockMvc
@Slf4j
public class FileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private AutoCloseable closeable;

    @BeforeEach
    public void open() {
        closeable = openMocks(this);
    }

    @AfterEach
    public void release() throws Exception {
        closeable.close();
    }

    @Test
    public void shouldReturn404ForMissingFile() throws Exception {
        mockMvc.perform(get("/files/cyb.jpg"))
                .andDo(print())
                .andExpect(status().isNotFound());
    }
}
