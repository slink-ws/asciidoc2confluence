package ws.slink;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;
import ws.slink.atlassian.Confluence;
import ws.slink.config.CommandLineArguments;
import ws.slink.parser.DirectoryProcessor;
import ws.slink.parser.FileProcessor;

@Slf4j
@Component
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class DocProcessorApplicationRunner implements CommandLineRunner, ApplicationContextAware {

    private final @NonNull CommandLineArguments commandLineArguments;
    private final @NonNull DirectoryProcessor directoryProcessor;
    private final @NonNull FileProcessor fileProcessor;
    private final @NonNull Confluence confluence;

    private ConfigurableApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = (ConfigurableApplicationContext) applicationContext;
    }

    @Override
    public void run(String... args) {

        // cleanup all the needed spaces
        if(!commandLineArguments.cleanupSpaces().isEmpty()) {
            System.out.println("Cleaning up following space(s): " + commandLineArguments.cleanupSpaces());
            commandLineArguments.cleanupSpaces()
                .stream()
                .forEach(s -> System.out.println("Removed " + confluence.cleanSpace(s) + " page(s) from " + s));
        }

        // process documentation sources
        if (StringUtils.isNotBlank(commandLineArguments.directoryPath()))
            directoryProcessor.process(commandLineArguments.directoryPath());
        else if (StringUtils.isNotBlank(commandLineArguments.inputFilename()))
            fileProcessor.process(commandLineArguments.inputFilename());

        // close up
        applicationContext.close();
        System.exit(0);
    }

}
