package ws.slink.config;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class CommandLineArguments {
    @Getter
    private String inputFilename = null;
    @Getter
    private String outputFilename = null;
    @Getter
    private String confluenceUrl = null;
    @Getter
    private String confluenceUser = null;
    @Getter
    private String confluencePassword = null;

    @Autowired
    public CommandLineArguments(ApplicationArguments args) {
        if (args.getOptionNames().isEmpty() || args.containsOption("h") || args.containsOption("help") ||
            !args.containsOption("input"))
        {
            printUsage();
        } else {
            if (args.containsOption("input")) {
                this.inputFilename = args.getOptionValues("input").get(0);
            }
            if (args.containsOption("output")) {
                this.outputFilename = args.getOptionValues("output").get(0);
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
            checkArguments();
        }
    }

    private void printUsage() {
        System.out.println("Usage: ");

        System.out.println(
            "  java -jar asciidoc2confluence.jar --input=<asciidoc filename> [--output=<html filename>] [--url=<confluence url> --user=<login> --pass=<password>]");
        System.out.println("\t--input\t\tInput AsciiDoc filename to generate documentation from [mandatory argument]");
        System.out
            .println("\t--output\tIf set, conversion output will be written to this file (overwriting existing files)");
        System.out.println("\t--url\t\tConfluence server base URL (e.g. http://localhost:8090)");
        System.out.println("\t--user\t\tConfluence user with publish rights");
        System.out.println("\t--pass\t\tConfluence user password");
        System.out.println(
            "\nNote: if none of --output or (--url & --user & --pass) is set, conversion output will be redirected to STDOUT");
        System.exit(1);
    }

    private void checkArguments() {
        if (StringUtils.isNotBlank(confluenceUrl) &&
            (StringUtils.isBlank(confluenceUser) || StringUtils.isBlank(confluencePassword))
            || StringUtils.isNotBlank(confluenceUser) &&
            (StringUtils.isBlank(confluenceUrl) || StringUtils.isBlank(confluencePassword))
            || StringUtils.isNotBlank(confluencePassword) &&
            (StringUtils.isBlank(confluenceUser) || StringUtils.isBlank(confluenceUrl)))
        {
            printUsage();
        }
    }
}
