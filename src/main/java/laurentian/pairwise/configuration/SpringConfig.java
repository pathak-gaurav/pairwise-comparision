package laurentian.pairwise.configuration;

import lombok.extern.slf4j.Slf4j;
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
@Slf4j
public class SpringConfig implements WebMvcConfigurer {

    /**
     * We cannot keep the files on the server once it was uploaded by the user, there should be a way to delete the file.
     * This scheduler will detect the file which is a csv and delete it from the server. This scheduler runs in every 5 seconds
     * @implNote : "fixedDelay" : waits for X millis from the end of previous execution before starting next execution.
     * */
    @Scheduled(fixedDelay = 5000)
    public void scheduleFixedDelayTask() {
        //Fetching the location of the Project Home Path
        File file = Paths.get(".").normalize().toAbsolutePath().toFile();
        // Finding all the files with the name of all the files which are '.csv' and does not have a name of 'example'
        // 'example' is the file which the user download as a sample.
        File[] matchingFiles = file.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.contains(".csv") && !name.contains("example");
            }
        });
        //After finding all the files based on above condition we will delete the file.
        for (File f : matchingFiles) {
            f.delete();
            log.info("File Deleted", f.getName());
        }
    }


    /**
     * Swagger Standard Configuration, Choosing Controller folder to show in dashboard as this folder.
     * Only contains the API.
     *
     * @return Docket: A builder which is intended to be the primary interface into the swagger-springmvc framework.
     *                  Provides sensible defaults and convenience methods for configuration.
     * */
    @Bean
    public Docket productApi() {
        return new Docket(DocumentationType.SWAGGER_2)
                .select()
                //Providing the base package name under which we have controller class for which we want to generate document.
                .apis(RequestHandlerSelectors.basePackage("laurentian.pairwise.controller"))
                .build();
    }


    /***
     * This is method comes from the interface WebMvcConfigurer, which is used to allow the URL from where you can
     * access the API's, The below URL (http://localhost:4200) is the default URL of frontend Angular Application.
     *
     * @param registry : Assists with the registration of global, URL pattern based CorsConfiguration mappings.
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**").allowedOrigins("http://localhost:4200");
    }

}
