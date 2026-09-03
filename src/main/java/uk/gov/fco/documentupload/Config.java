package uk.gov.fco.documentupload;

import fi.solita.clamav.ClamAVClient;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.apache.tika.Tika;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import software.amazon.awssdk.auth.credentials.WebIdentityTokenFileCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.rekognition.RekognitionClient;

@Configuration
public class Config {

    public static boolean isDevelopment(final Environment e) {
        return e != null && e.matchesProfiles("development", "dev", "test");
    }

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info().title("Document Upload")
                        .version(getClass().getPackage().getImplementationVersion())
                        .description("File storage service with the ability to scan files for viruses as they are uploaded.")
                        .license(new License()
                                .name("MIT")
                                .url("https://raw.githubusercontent.com/CautionYourBlast/document-upload/master/LICENSE")));
    }

    @Bean
    public ClamAVClient clamAVClient(final AntiVirusProperties properties) {
        return new ClamAVClient(properties.host(), properties.port(), properties.timeout());
    }

    @Bean
    public Tika tika() {
        return new Tika();
    }

    @Bean
    public RekognitionClient rekognitionClient() {
        return RekognitionClient.builder()
                .region(Region.EU_WEST_2)
                .credentialsProvider(WebIdentityTokenFileCredentialsProvider.create())
                .build();
    }
}
