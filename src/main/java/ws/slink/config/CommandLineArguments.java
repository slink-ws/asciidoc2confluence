package ws.slink.config;

import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Getter
@Accessors(fluent = true)
public class CommandLineArguments {

    private String inputFilename       = null;
    private String directoryPath       = null;
    private String tagsFilename        = null;
    private String confluenceUrl       = null;
    private String confluenceUser      = null;
    private String confluencePassword  = null;
    private String confluenceSpaceKey  = null;

    @Autowired
    public CommandLineArguments(ApplicationArguments args) {
        if (args.getOptionNames().isEmpty()
         || args.containsOption("h")
         || args.containsOption("help")
         || (!args.containsOption("input") && (!args.containsOption("dir")))
        ) {
            printUsage();
        } else {
            if (args.containsOption("input")) {
                this.inputFilename = args.getOptionValues("input").get(0);
            }
            if (args.containsOption("dir")) {
                this.directoryPath = args.getOptionValues("dir").get(0);
            }
            if (args.containsOption("tags")) {
                this.tagsFilename = args.getOptionValues("tags").get(0);
            }
            if (args.containsOption("url")) {
                this.confluenceUrl = args.getOptionValues("url").get(0);
            }
            if (args.containsOption("user")) {
                this.confluenceUser = args.getOptionValues("user").get(0);
            }
            if (args.containsOption("pass")) {
                this.confluencePassword = args.getOptionValues("pass").get(0);
            }
            if (args.containsOption("space")) {
                this.confluenceSpaceKey = args.getOptionValues("space").get(0);
            }
            checkArguments();
        }
    }

    private void printUsage() {
        System.out.println("Usage: ");

        System.out.println("  java -jar asciidoc2confluence.jar {--input=<asciidoc filename> | --dir=<path/to/directory>} [--url=<confluence url> --user=<login> --pass=<password>] [--space=<confluence space key>]");
        System.out.println("\t--input\t\tInput AsciiDoc filename to generate documentation from");
        System.out.println("\t--dir\t\tDirectory to process asciidoc files recursively");
        System.out.println("\t--url\t\tConfluence server base URL (e.g. http://localhost:8090)");
        System.out.println("\t--user\t\tConfluence user with publish rights");
        System.out.println("\t--pass\t\tConfluence user password");
        System.out.println("\t--space\t\tConfluence space key override");
        System.out.println("\nNote: if (--url & --user & --pass) not set, conversion output will be redirected to STDOUT");
        System.exit(1);
    }

    private void checkArguments() {
        if ( StringUtils.isNotBlank(confluenceUrl) && (StringUtils.isBlank(confluenceUser) || StringUtils.isBlank(confluencePassword))
          || StringUtils.isNotBlank(confluenceUser) && (StringUtils.isBlank(confluenceUrl) || StringUtils.isBlank(confluencePassword))
          || StringUtils.isNotBlank(confluencePassword) && (StringUtils.isBlank(confluenceUser) || StringUtils.isBlank(confluenceUrl)))
        printUsage();
    }
}
