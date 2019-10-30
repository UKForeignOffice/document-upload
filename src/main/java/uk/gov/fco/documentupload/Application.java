package uk.gov.fco.documentupload;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info().title("Document Upload")
                        .version("X.X.X")
                        .description("Service to check files for viruses nad store them.")
                        .license(new License()
                                .name("MIT")
                                .url("https://raw.githubusercontent.com/CautionYourBlast/document-uploaa/master/LICENSE")));
    }
}
