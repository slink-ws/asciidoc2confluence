package ws.slink;

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
import ws.slink.model.Document;
import ws.slink.parser.FileProcessor;

@Slf4j
@Component
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class DocProcessorApplicationRunner implements CommandLineRunner, ApplicationContextAware {

    private final CommandLineArguments commandLineArguments;
    private final FileProcessor fileProcessor;
    private final Confluence confluence;

    private ConfigurableApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = (ConfigurableApplicationContext) applicationContext;
    }

    @Override
    public void run(String... args) {
        if (StringUtils.isNotBlank(commandLineArguments.inputFilename())) {
            fileProcessor.read(commandLineArguments.inputFilename()).ifPresent(d ->
                fileProcessor.convert(d).ifPresent(cd -> process(d, cd))
            );
        }
        applicationContext.close();
        System.exit(0);
    }

    private void process(Document document, String convertedDocument) {
        if (StringUtils.isNotBlank(commandLineArguments.confluenceUrl())) {
            if (!confluence.canPublish()) {
                System.err.println("can't publish document '" + document.inputFilename() + "' to confluence: not all confluence parameters are set (url, login, password)");
            } else {
                if (!document.canPublish()) {
                    System.err.println("can't publish document '" + document.inputFilename() + "' to confluence: not all document parameters are set (title, spaceKey)");
                } else {
                    // publish to confluence
                    confluence.getPageId(document.space(), document.title()).ifPresent(confluence::deletePage);
                    if (confluence.publishPage(
                        document.space(),
                        document.title(),
                        document.parent(),
                        convertedDocument
                    )) {
                        System.out.println(
                            String.format(
                                "Published document to confluence: %s/display/%s/%s"
                                ,commandLineArguments.confluenceUrl()
                                ,document.space()
                                ,document.title().replaceAll(" ", "+")
                            )
                        );
                    } else {
                        System.out.println(
                            String.format(
                                 "Could not publish document '%s' to confluence server"
                                ,document.title()
                            )
                        );
                    }
                }
            }
        } else {
            // or print to stdout
            System.out.println(convertedDocument);
        }
    }

}
