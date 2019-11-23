package ws.slink;

import org.springframework.stereotype.Component;
import ws.slink.atlassian.Confluence;
import ws.slink.config.CommandLineArguments;
import ws.slink.model.Document;
import ws.slink.parser.FileProcessor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.ConfigurableApplicationContext;

import java.io.IOException;

@Slf4j
@Component
public class DocProcessorApplicationRunner implements CommandLineRunner {

    @Autowired
    private ConfigurableApplicationContext ctx;

    @Autowired
    private CommandLineArguments commandLineArguments;

    @Autowired
    private FileProcessor fileProcessor;

    @Autowired
    private Confluence confluence;


    @Override
    public void run(String... args) {
        if (StringUtils.isNotBlank(commandLineArguments.getInputFilename())) {
            fileProcessor.read(commandLineArguments.getInputFilename()).ifPresent(d ->
                fileProcessor.convert(d).ifPresent(cd -> process(d, cd))
            );
        }
        ctx.close();
        System.exit(0);
    }

    private void process(Document document, String convertedDocument) {
        if (StringUtils.isNotBlank(commandLineArguments.getConfluenceUrl())) {
            if (!confluence.canPublish()) {
                System.err.println("can't publish document '" + document.inputFilename() +
                    "' to confluence: not all confluence parameters are set (url, login, password)");
            } else {
                if (!document.canPublish()) {
                    System.err.println("can't publish document '" + document.inputFilename() +
                        "' to confluence: not all document parameters are set (title, spaceKey)");
                } else {
                    // publish to confluence
                    confluence.getPageId(document.space(), document.title()).ifPresent(confluence::deletePage);
                    if (confluence.publishPage(
                        document.space(),
                        document.title(),
                        document.parent(),
                        convertedDocument
                    ))
                    {
                        System.out.println(
                            String.format(
                                "Published document to confluence: %s/display/%s/%s"
                                , commandLineArguments.getConfluenceUrl()
                                , document.space()
                                , document.title().replaceAll(" ", "+")
                            )
                        );
                    } else {
                        System.out.println(
                            String.format(
                                "Could not publish document '%s' to confluence server"
                                , document.title()
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
