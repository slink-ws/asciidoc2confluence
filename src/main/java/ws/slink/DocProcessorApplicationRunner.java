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
import ws.slink.config.AppConfig;
import ws.slink.parser.DirectoryProcessor;
import ws.slink.parser.FileProcessor;
import ws.slink.parser.Processor;
import ws.slink.service.TrackingService;

@Slf4j
@Component
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class DocProcessorApplicationRunner implements CommandLineRunner, ApplicationContextAware {

    private final @NonNull AppConfig appConfig;
    private final @NonNull Processor processor;

    private ConfigurableApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = (ConfigurableApplicationContext) applicationContext;
    }

    @Override
    public void run(String... args) {

        if (!checkConfiguration()) {
            printUsage();
            applicationContext.close();
            System.exit(1);
        } else {
            System.out.println(processor.process());
        }

        // close up
        applicationContext.close();
        System.exit(0);
    }

    private boolean checkConfiguration() {
        return !( StringUtils.isNotBlank(appConfig.getUrl()) && (StringUtils.isBlank(appConfig.getUser()) || StringUtils.isBlank(appConfig.getPass()))
        || StringUtils.isNotBlank(appConfig.getUser()) && (StringUtils.isBlank(appConfig.getUrl()) || StringUtils.isBlank(appConfig.getPass()))
        || StringUtils.isNotBlank(appConfig.getPass()) && (StringUtils.isBlank(appConfig.getUser()) || StringUtils.isBlank(appConfig.getUrl()))
        || (appConfig.getClean().isEmpty() && StringUtils.isBlank(appConfig.getInput()) && StringUtils.isBlank(appConfig.getDir()))
        )
        ;
    }

    public void printUsage() {
        System.out.println("Usage: ");
        System.out.println("  java -jar asciidoc2confluence.jar {--input=<asciidoc filename> | --dir=<path/to/directory>} [--url=<confluence url> --user=<login> --pass=<password>] [--space=<confluence space key>]");
        System.out.println("\t--input\t\tInput AsciiDoc filename to generate documentation from");
        System.out.println("\t--dir\t\tDirectory to process asciidoc files recursively");
        System.out.println("\t--clean\t\tSpace keys list for spaces to be cleaned up (remove all pages, besides pages tagged with protected labels)");
        System.out.println("\t--force\t\tForce removal of protected pages (if used with --clean)");
        System.out.println("\t--dbg\t\tOutput converted document to STDOUT in case of publishing error");
        System.out.println("\t--url\t\tConfluence server base URL (e.g. http://localhost:8090)");
        System.out.println("\t--user\t\tConfluence user with publish rights");
        System.out.println("\t--pass\t\tConfluence user password");
        System.out.println("\t--space\t\tConfluence space key override");
        System.out.println("\nNote: if (--url & --user & --pass) not set, conversion output will be redirected to STDOUT");
        System.exit(1);
    }

}


//        System.err.println("url  : " + appConfig.getUrl());
//        System.err.println("user : " + appConfig.getUser());
//        System.err.println("pass : " + appConfig.getPass());
//        System.err.println("input: " + appConfig.getInput());
//        System.err.println("dir  : " + appConfig.getDir());
//        System.err.println("space: " + appConfig.getSpace());
//        System.err.println("clean: " + appConfig.getClean());
//        System.err.println("debug: " + appConfig.isDebug());
//        System.err.println("force: " + appConfig.isForce());

/*
        long timeA = Instant.now().toEpochMilli();

        // cleanup all the needed spaces
        if(!appConfig.clean().isEmpty()) {
            log.info("Cleaning up following space(s): " + appConfig.clean());
            appConfig.clean()
                .stream()
                .forEach(s -> log.info("Removed " + confluence.cleanSpace(s) + " page(s) from " + s));
        }

        long timeB = Instant.now().toEpochMilli();

        // process documentation sources
        ProcessingResult result = new ProcessingResult();
        if (StringUtils.isNotBlank(appConfig.dir()))
            result.merge(directoryProcessor.process(appConfig.dir()));
        else if (StringUtils.isNotBlank(appConfig.input()))
            result.merge(fileProcessor.process(appConfig.input()));

        long timeC = Instant.now().toEpochMilli();

        System.out.println("-------------------------------------------------------------");
        System.out.println("total time taken      : " + DurationFormatUtils.formatDuration( timeC - timeA, "HH:mm:ss"));
        System.out.println("clean up time         : " + DurationFormatUtils.formatDuration( timeB - timeA, "HH:mm:ss"));
        System.out.println("publishing time       : " + DurationFormatUtils.formatDuration( timeC - timeB, "HH:mm:ss"));
        System.out.println("successfully processed: " + result.successful().get());
        System.out.println("processing failures   : " + result.failed().get());
        System.out.println("duplicate titles      : ");
        trackingService
            .get()
            .entrySet()
            .stream()
            .map(e -> "                        " + e.getKey() + " x " + e.getValue())
            .forEach(System.out::println);
*/

