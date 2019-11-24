package ws.slink;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import ws.slink.config.CommandLineArguments;
import ws.slink.parser.DirectoryProcessor;
import ws.slink.parser.FileProcessor;

@Slf4j
@SpringBootApplication
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class DocProcessorApplication {

	private final @NonNull CommandLineArguments commandLineArguments;
	private final @NonNull FileProcessor fileProcessor;
	private final @NonNull DirectoryProcessor directoryProcessor;

	@Bean
	public CommandLineRunner commandLineRunner() {
		return new DocProcessorApplicationRunner(commandLineArguments, directoryProcessor, fileProcessor);
	}

	public static void main(String[] args) {
		SpringApplication.run(DocProcessorApplication.class, args);
	}

}
