package ws.slink;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DurationFormatUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;
import ws.slink.atlassian.Confluence;
import ws.slink.config.CommandLineArguments;
import ws.slink.model.ProcessingResult;
import ws.slink.parser.DirectoryProcessor;
import ws.slink.parser.FileProcessor;
import ws.slink.service.TrackingService;

import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class DocProcessorApplicationRunner implements CommandLineRunner, ApplicationContextAware {

    private final @NonNull CommandLineArguments commandLineArguments;
    private final @NonNull DirectoryProcessor directoryProcessor;
    private final @NonNull FileProcessor fileProcessor;
    private final @NonNull Confluence confluence;
    private final @NonNull TrackingService trackingService;

    private ConfigurableApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = (ConfigurableApplicationContext) applicationContext;
    }

    @Override
    public void run(String... args) {

        long timeA = Instant.now().toEpochMilli();

        // cleanup all the needed spaces
        if(!commandLineArguments.cleanupSpaces().isEmpty()) {
            System.out.println("Cleaning up following space(s): " + commandLineArguments.cleanupSpaces());
            commandLineArguments.cleanupSpaces()
                .stream()
                .forEach(s -> System.out.println("Removed " + confluence.cleanSpace(s) + " page(s) from " + s));
        }

        long timeB = Instant.now().toEpochMilli();

        // process documentation sources
        ProcessingResult result = new ProcessingResult();
        if (StringUtils.isNotBlank(commandLineArguments.directoryPath()))
            result.merge(directoryProcessor.process(commandLineArguments.directoryPath()));
        else if (StringUtils.isNotBlank(commandLineArguments.inputFilename()))
            result.merge(fileProcessor.process(commandLineArguments.inputFilename()));

        long timeC = Instant.now().toEpochMilli();

        System.out.println("-------------------------------------------------------------");
        System.out.println("total time taken      : " + DurationFormatUtils.formatDurationHMS( timeC - timeA));
        System.out.println("clean up time         : " + DurationFormatUtils.formatDurationHMS( timeB - timeA));
        System.out.println("publishing time       : " + DurationFormatUtils.formatDurationHMS( timeC - timeB));
        System.out.println("successfully processed: " + result.successful().get());
        System.out.println("processing failures   : " + result.failed().get());
        System.out.println("duplicate titles      : ");

        trackingService
            .get()
            .entrySet()
            .stream()
            .map(e -> "                        " + e.getKey() + " x " + e.getValue())
            .forEach(System.out::println);

        // close up
        applicationContext.close();
        System.exit(0);
    }

}
