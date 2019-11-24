package ws.slink;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import ws.slink.atlassian.Confluence;
import ws.slink.config.CommandLineArguments;
import ws.slink.parser.FileProcessor;

@Slf4j
@SpringBootApplication
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class DocProcessorApplication {

	private final @NonNull CommandLineArguments commandLineArguments;
	private final @NonNull FileProcessor fileProcessor;

	@Bean
	public CommandLineRunner commandLineRunner() {
		return new DocProcessorApplicationRunner(commandLineArguments, fileProcessor);
	}

	public static void main(String[] args) {
		SpringApplication.run(DocProcessorApplication.class, args);
	}

}
