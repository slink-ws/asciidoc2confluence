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
import ws.slink.config.AppConfig;
import ws.slink.parser.Processor;

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

        int exitCode = 0;

        if (!checkConfiguration()) {
            printUsage();
            exitCode = 1;
        } else {
            System.out.println(processor.process());
        }

        // close up
        applicationContext.close();
        System.exit(exitCode);
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
