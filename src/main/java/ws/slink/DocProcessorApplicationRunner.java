package com.dxfeed;

import com.dxfeed.atlassian.Confluence;
import com.dxfeed.config.CommandLineArguments;
import com.dxfeed.model.Document;
import com.dxfeed.parser.FileProcessor;
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
        if (StringUtils.isNotBlank(commandLineArguments.confluenceUrl)) {
            // publish to confluence
            confluence.getPageId(document.space(), document.title()).ifPresent(confluence::deletePage);
            confluence.publishPage(document.space(), document.title(), convertedDocument);
        } else {
            // or print to stdout
            System.out.println(convertedDocument);
        }
    }
}
