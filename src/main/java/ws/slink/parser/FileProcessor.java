package ws.slink.parser;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.asciidoctor.Asciidoctor;
import org.asciidoctor.OptionsBuilder;
import org.asciidoctor.SafeMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ws.slink.atlassian.Confluence;
import ws.slink.config.CommandLineArguments;
import ws.slink.model.Document;
import ws.slink.processor.*;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class FileProcessor {

    @Value("${asciidoc.template.space-key}")
    private String spaceKeyTemplate;

    @Value("${asciidoc.template.title}")
    private String titleTemplate;

    @Value("${asciidoc.template.title-old}")
    private String titleOldTemplate;

    @Value("${asciidoc.template.parent}")
    private String parentTemplate;

    private final CommandLineArguments commandLineArguments;
    private final Confluence confluence;

    private Asciidoctor asciidoctor;

    @SuppressWarnings("unchecked")
    private void disableAccessWarnings() {
        try {
            Class unsafeClass = Class.forName("sun.misc.Unsafe");
            Field field = unsafeClass.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            Object unsafe = field.get(null);

            Method putObjectVolatile = unsafeClass.getDeclaredMethod("putObjectVolatile", Object.class, long.class, Object.class);
            Method staticFieldOffset = unsafeClass.getDeclaredMethod("staticFieldOffset", Field.class);

            Class loggerClass = Class.forName("jdk.internal.module.IllegalAccessLogger");
            Field loggerField = loggerClass.getDeclaredField("logger");
            Long offset = (Long) staticFieldOffset.invoke(unsafe, loggerField);
            putObjectVolatile.invoke(unsafe, loggerClass, offset, null);
        } catch (Exception ignored) {
        }
    }

    @PostConstruct
    private void initializeAsciidoctor() {
        disableAccessWarnings();
        this.asciidoctor = Asciidoctor.Factory.create();

        // register preprocessors
        asciidoctor.javaExtensionRegistry().preprocessor(CodeBlockPreProcessor.class);

        // register block processors
        asciidoctor.javaExtensionRegistry().block(CodeBlockProcessor.class);

        // register postprocessors
        asciidoctor.javaExtensionRegistry().postprocessor(CodeBlockPostProcessor.class);
        asciidoctor.javaExtensionRegistry().postprocessor(NoticeBlockPostProcessor.class);
    }

    public void process(String inputFilename) {
        if (StringUtils.isNotBlank(inputFilename)) {
            read(inputFilename).ifPresent(d ->
                convert(d).ifPresent(cd -> publishOrPrint(d, cd))
            );
        }
    }

    public Optional<Document> read(String inputFilename) {
        List<String> lines = null;
        try {
            lines = FileUtils.readLines(new File(inputFilename), "utf-8");
        } catch (IOException e) {
            log.error("error reading file: {}", e.getMessage());
            return Optional.empty();
        }
        return Optional.of(
            new Document()
                .space(lines
                    .stream()
                    .filter(s -> s.contains(spaceKeyTemplate))
                    .findFirst()
                    .orElse("")
                    .replace(spaceKeyTemplate, "")
                    .replace("/", "")
                    .trim()
                )
                .title(lines
                    .stream()
                    .filter(s -> s.contains(titleTemplate))
                    .findFirst()
                    .orElse("")
                    .replace(titleTemplate, "")
                    .replace("/", "")
                    .trim()
                )
                .parent(lines
                    .stream()
                    .filter(s -> s.contains(parentTemplate))
                    .findFirst()
                    .orElse("")
                    .replace(parentTemplate, "")
                    .replace("/", "")
                    .trim()
                )
                .inputFilename(inputFilename)
                .contents(lines.stream().collect(Collectors.joining("\n")))
        );
    }

    public Optional<String> convert(Document document) {
        try {
            String result = asciidoctor
            .convertFile(
                new File(document.inputFilename()),
                OptionsBuilder.options()
                    .backend("xhtml5")
                    .toFile(false)
                    .safe(SafeMode.UNSAFE)
            );
            return Optional.ofNullable(result);
        } catch (Exception e) {
            log.warn("error converting file: {}", e.getMessage());
            return Optional.empty();
        }
    }

    public void publishOrPrint(Document document, String convertedDocument) {
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
