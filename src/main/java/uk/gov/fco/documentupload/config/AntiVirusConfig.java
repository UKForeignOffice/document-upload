package uk.gov.fco.documentupload.config;

import fi.solita.clamav.ClamAVClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AntiVirusConfig {

    @Autowired
    private AntiVirusProperties properties;

    @Bean
    public ClamAVClient clamAVClient() {
        return new ClamAVClient(properties.getHost(), properties.getPort(), properties.getTimeout());
    }
}
