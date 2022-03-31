package laurentian.pairwise.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

import java.io.File;
import java.io.FilenameFilter;
import java.nio.file.Paths;

@Configuration
@EnableScheduling
@EnableSwagger2
@EnableWebMvc
public class SpringConfig implements WebMvcConfigurer {

    /**
     * We cannot keep the files on the server once it was uploaded by the user, there should be a way to delete the file.
     * This scheduler will detect the file which is a csv and delete it from the server. This scheduler runs in every 5 seconds
     * */
    @Scheduled(fixedDelay = 5000)
    public void scheduleFixedDelayTask() {
        File file = Paths.get(".").normalize().toAbsolutePath().toFile();
        File[] matchingFiles = file.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.contains(".csv") && !name.contains("example");
            }
        });
        for (File f : matchingFiles) {
            f.delete();
        }
    }


    /**
     * Swagger Standard Configuration, Choosing Controller folder to show in dashboard as this folder.
     * Only contains the API.
     * */
    @Bean
    public Docket productApi() {
        return new Docket(DocumentationType.SWAGGER_2)
                .select()
                .apis(RequestHandlerSelectors.basePackage("laurentian.pairwise.controller"))
                .build();
    }


    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**").allowedOrigins("http://localhost:4200");
    }

}
