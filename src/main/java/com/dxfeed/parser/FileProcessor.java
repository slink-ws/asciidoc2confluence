package com.dxfeed.parser;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.asciidoctor.Asciidoctor;
import org.asciidoctor.OptionsBuilder;
import org.asciidoctor.SafeMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import com.dxfeed.atlassian.Confluence;
import com.dxfeed.config.AppConfig;
import com.dxfeed.config.CommandLineArguments;
import com.dxfeed.model.Document;
import com.dxfeed.model.ProcessingResult;
import com.dxfeed.processor.*;
import com.dxfeed.service.TrackingService;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
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

    private final AppConfig appConfig;
    private final Confluence confluence;
    private final TrackingService trackingService;


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
        asciidoctor.javaExtensionRegistry().preprocessor(TreeMacroPreProcessor.class);

        // register block processors
        asciidoctor.javaExtensionRegistry().block(CodeBlockProcessor.class);

        // register (inline) macro processors
        asciidoctor.javaExtensionRegistry().inlineMacro(new ConfluenceLinkInlineMacroProcessor(document.space()));
        asciidoctor.javaExtensionRegistry().blockMacro(TreeMacroProcessor.class);

        // register postprocessors
        asciidoctor.javaExtensionRegistry().postprocessor(CodeBlockPostProcessor.class);
        asciidoctor.javaExtensionRegistry().postprocessor(NoticeBlockPostProcessor.class);
        asciidoctor.javaExtensionRegistry().postprocessor(TOCBlockPostProcessor.class);

        return asciidoctor;
    }

    public ProcessingResult process(String inputFilename) {
        ProcessingResult result = new ProcessingResult();
        if (StringUtils.isNotBlank(inputFilename))
            read(inputFilename).ifPresent(d -> convert(d).ifPresent(cd -> result.merge(publishOrPrint(d, cd))));
        return result;
    }
    public Optional<Document> read(String inputFilename) {
        List<String> lines;
        try {
            lines = FileUtils.readLines(new File(inputFilename), "utf-8");
        } catch (IOException e) {
            log.error("error reading file: {}", e.getMessage());
            return Optional.empty();
        }
        Document document =
            new Document()
                .space(getDocumentParam(lines, spaceKeyTemplate, appConfig.getSpace()))
                .title(getDocumentParam(lines, titleTemplate, null))
                .oldTitle(getDocumentParam(lines, titleOldTemplate, null))
                .parent(getDocumentParam(lines, parentTemplate, null))
                .inputFilename(inputFilename)
                .contents(lines.stream().collect(Collectors.joining("\n")))
                .tags(
                    Arrays.asList(getDocumentParam(lines, tagsTemplate, null).split(","))
                    .stream()
                    .filter(s -> StringUtils.isNotBlank(s))
                    .map(s -> s.trim())
                    .collect(Collectors.toList())
        );

        if (trackingService.contains(document.title()))
            log.warn("document '{}' already processed in this batch; suspected document title repeating", document.title());
        trackingService.add(document.title());

        if (!FilenameUtils.getBaseName(inputFilename)
            .replaceAll(" ", "_")
            .equalsIgnoreCase(document.title().replaceAll(" ", "_")))
            log.warn("document title does not match with file name: '{}' - '{}'",
                document.title(),
                FilenameUtils.getName(inputFilename));

        return Optional.of(document);
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
    public ProcessingResult publishOrPrint(Document document, String convertedDocument) {
        if (StringUtils.isNotBlank(appConfig.getUrl())) {
            if (!confluence.canPublish()) {
                System.err.println("can't publish document '" + document.inputFilename() + "' to confluence: not all confluence parameters are set (url, login, password)");
                return ProcessingResult.FAILURE;
            } else {
                if (!document.canPublish()) {
                    System.err.println("can't publish document '" + document.inputFilename() + "' to confluence: not all document parameters are set (title, spaceKey)");
                    return ProcessingResult.FAILURE;
                } else {
                    // delete page
                    confluence.getPageId(document.space(), document.title()).ifPresent(id -> confluence.deletePage(id, document.title()));
                    // delete old page in case of renaming
                    if (StringUtils.isNotBlank(document.oldTitle()))
                        confluence.getPageId(document.space(), document.oldTitle()).ifPresent(id -> confluence.deletePage(id, document.oldTitle()));
                    // publish to confluence
                    if (confluence.publishPage(document.space(), document.title(), document.parent(), convertedDocument)) {
                        log.info(
                            String.format(
                                "Published document to confluence: %s/display/%s/%s"
                                ,appConfig.getUrl()
                                ,document.space()
                                ,document.title().replaceAll(" ", "+")
                            )
                        );
                        if (confluence.tagPage(document.space(), document.title(), document.tags())
                        ) {
                            log.info(
                                String.format(
                                    "Labeled document with tags: %s"
                                    ,document.tags()
                                )
                            );
                        }
                        return ProcessingResult.SUCCESS;
                    } else {
                        log.info(
                            String.format(
                                "Could not publish document '%s' to confluence server"
                                ,document.title()
                            )
                        );
                        if (appConfig.isDebug())
                            System.out.println(convertedDocument);
                        return ProcessingResult.FAILURE;
                    }
                }
            }
        } else {
            // or print to stdout
            System.out.println(convertedDocument);
            return ProcessingResult.SUCCESS;
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
