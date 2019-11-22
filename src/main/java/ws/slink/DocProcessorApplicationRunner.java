package ws.slink;

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
public class DocProcessorApplicationRunner implements CommandLineRunner {

    @Autowired
    private ConfigurableApplicationContext ctx;

    @Autowired
    private CommandLineArguments commandLineArguments;

    @Autowired
    private FileProcessor fileProcessor;

    @Autowired
    Confluence confluence;

    @Override
    public void run(String... args) throws IOException {
        if (StringUtils.isNotBlank(commandLineArguments.inputFilename)) {
            fileProcessor.read(commandLineArguments.inputFilename).ifPresent(d ->
                fileProcessor.convert(d).ifPresent(cd -> process(d, cd))
            );
        }
        ctx.close();
        System.exit(0);
    }

    private void process(Document document, String convertedDocument) {
        if (StringUtils.isNotBlank(commandLineArguments.confluenceUrl)
            && StringUtils.isNotBlank(document.space())
            && StringUtils.isNotBlank(document.title())
        ) {
            // publish to confluence
            confluence.getPageId(document.space(), document.title()).ifPresent(confluence::deletePage);
            confluence.publishPage(document.space(), document.title(), convertedDocument);
        } else {
            // or print to stdout
            System.out.println(convertedDocument);
        }
    }
}
