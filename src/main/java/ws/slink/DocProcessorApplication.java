package ws.slink;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import ws.slink.config.AppConfig;

@Slf4j
@Configuration
@SpringBootApplication
@EnableConfigurationProperties(AppConfig.class)
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class DocProcessorApplication {

	public static void main(String[] args) {
		SpringApplication.run(DocProcessorApplication.class, args);
	}

}
