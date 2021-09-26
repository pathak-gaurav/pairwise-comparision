package laurentian.pairwise.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import springfox.documentation.builders.PathSelectors;
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
public class SpringConfig {

    @Scheduled(fixedDelay = 1000)
    public void scheduleFixedDelayTask() {
        File file = Paths.get(".").normalize().toAbsolutePath().toFile();
        File[] matchingFiles = file.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.contains("pairwise_file");
            }
        });
        for (File f : matchingFiles) {
            f.delete();
        }
    }

    @Configuration
    public class SpringFoxConfig {
        @Bean
        public Docket api() {
            return new Docket(DocumentationType.SWAGGER_2)
                    .select()
                    .apis(RequestHandlerSelectors.any())
                    .paths(PathSelectors.any())
                    .build();
        }
    }

}
