package ws.slink.parser;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.asciidoctor.Asciidoctor;
import org.asciidoctor.OptionsBuilder;
import org.asciidoctor.SafeMode;
import org.asciidoctor.log.LogHandler;
import org.asciidoctor.log.LogRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ws.slink.atlassian.Confluence;
import ws.slink.config.CommandLineArguments;
import ws.slink.model.Document;
import ws.slink.processor.*;

import javax.annotation.PostConstruct;
import javax.swing.plaf.basic.BasicColorChooserUI;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
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

    @Value("${asciidoc.template.tags}")
    private String tagsTemplate;

    private final CommandLineArguments commandLineArguments;
    private final Confluence confluence;

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
    private void init() {
        disableAccessWarnings();
    }

    /**
     * we need to initialize AsciiDoctor for each document being processed, as we need to know document's space
     * to create correct inter-document links
     *
     * @param document
     */
    private Asciidoctor initializeAsciidoctor(Document document) {

        Asciidoctor asciidoctor = Asciidoctor.Factory.create();

        // register preprocessors
        asciidoctor.javaExtensionRegistry().preprocessor(CodeBlockPreProcessor.class);
        asciidoctor.javaExtensionRegistry().preprocessor(ConfluenceLinkMacroPreProcessor.class);

        // register block processors
        asciidoctor.javaExtensionRegistry().block(CodeBlockProcessor.class);

        // register (inline) macro processors
        asciidoctor.javaExtensionRegistry().inlineMacro(new ConfluenceLinkInlineMacroProcessor(document.space()));

        // register postprocessors
        asciidoctor.javaExtensionRegistry().postprocessor(CodeBlockPostProcessor.class);
        asciidoctor.javaExtensionRegistry().postprocessor(NoticeBlockPostProcessor.class);
        asciidoctor.javaExtensionRegistry().postprocessor(TOCBlockPostProcessor.class);

        return asciidoctor;
    }

    public void process(String inputFilename) {
        if (StringUtils.isNotBlank(inputFilename)) {
            read(inputFilename).ifPresent(d ->
                convert(d).ifPresent(cd ->
                    publishOrPrint(d, cd)));
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
                .space(getDocumentParam(lines, spaceKeyTemplate, commandLineArguments.confluenceSpaceKey()))
                .title(getDocumentParam(lines, titleTemplate, null))
                .oldtitle(getDocumentParam(lines, titleOldTemplate, null))
                .parent(getDocumentParam(lines, parentTemplate, null))
                .inputFilename(inputFilename)
                .contents(lines.stream().collect(Collectors.joining("\n")))
                .tags(
                    Arrays.asList(getDocumentParam(lines, tagsTemplate, null).split(","))
                    .stream()
                    .filter(s -> StringUtils.isNotBlank(s))
                    .map(s -> s.trim())
                    .collect(Collectors.toList())
                )
        );
    }
    public Optional<String> convert(Document document) {
        Asciidoctor asciidoctor = initializeAsciidoctor(document);
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
        } finally {
            asciidoctor.shutdown();
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
                    // delete page
                    confluence.getPageId(document.space(), document.title()).ifPresent(confluence::deletePage);
                    // delete old page in case of renaming
                    if (StringUtils.isNotBlank(document.oldtitle()))
                        confluence.getPageId(document.space(), document.oldtitle()).ifPresent(confluence::deletePage);
                    // publish to confluence
                    if (confluence.publishPage(document.space(), document.title(), document.parent(), convertedDocument)) {
                        System.out.println(
                            String.format(
                                "Published document to confluence: %s/display/%s/%s"
                                ,commandLineArguments.confluenceUrl()
                                ,document.space()
                                ,document.title().replaceAll(" ", "+")
                            )
                        );
                        if (confluence.tagPage(document.space(), document.title(), document.tags())
                        ) {
                            System.out.println(
                                String.format(
                                    "Labeled document with tags: %s"
                                    ,document.tags()
                                )
                            );
                        }
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

    private String getDocumentParam(List<String> lines, String key, String override) {
        return (StringUtils.isNotBlank(override))
            ? override
            : lines
            .stream()
            .filter(s -> s.contains(key))
            .findFirst()
            .orElse("")
            .replace(key, "")
            .replace("/", "")
            .trim()
        ;
    }
}
