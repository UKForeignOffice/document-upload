package uk.gov.fco.documentupload;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.ComponentScan;

@TestConfiguration
@ComponentScan(basePackageClasses = Application.class)
@Slf4j
public class TestConfig {
}
