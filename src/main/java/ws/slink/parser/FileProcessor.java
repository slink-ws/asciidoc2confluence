package ws.slink.parser;

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
import ws.slink.atlassian.Confluence;
import ws.slink.config.AppConfig;
import ws.slink.model.Document;
import ws.slink.model.ProcessingResult;
import ws.slink.processor.*;
import ws.slink.service.TrackingService;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static ws.slink.model.ProcessingResult.ResultType.*;

@Slf4j
@Component
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class FileProcessor {

    @Value("${asciidoc.template.title}")
    private String titleTemplate;

    @Value("${asciidoc.template.title-old}")
    private String titleOldTemplate;

    @Value("${asciidoc.template.parent}")
    private String parentTemplate;

    @Value("${asciidoc.template.hidden}")
    private String hiddenTemplate;

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
        asciidoctor.javaExtensionRegistry().preprocessor(ChildrenMacroPreProcessor.class);

        // register block processors
        asciidoctor.javaExtensionRegistry().block(CodeBlockProcessor.class);

        // register (inline) macro processors
        asciidoctor.javaExtensionRegistry().inlineMacro(new ConfluenceLinkInlineMacroProcessor(document.space()));
        asciidoctor.javaExtensionRegistry().blockMacro(TreeMacroProcessor.class);
        asciidoctor.javaExtensionRegistry().blockMacro(ChildrenMacroProcessor.class);

        // register postprocessors
        asciidoctor.javaExtensionRegistry().postprocessor(CodeBlockPostProcessor.class);
        asciidoctor.javaExtensionRegistry().postprocessor(NoticeBlockPostProcessor.class);
        asciidoctor.javaExtensionRegistry().postprocessor(TOCBlockPostProcessor.class);
        asciidoctor.javaExtensionRegistry().postprocessor(ImageBlockPostProcessor.class);

        return asciidoctor;
    }

    public ProcessingResult process(String inputFilename) {
        ProcessingResult result = new ProcessingResult();
        if (StringUtils.isNotBlank(inputFilename))
            read(inputFilename).ifPresent(d -> convert(d).ifPresent(cd -> result.merge(publishOrPrint(d, cd))));
        return result;
    }
    public Optional<Document> read(String inputFilename) {
        return read(inputFilename, false);
    }
    public Optional<Document> read(String inputFilename, boolean trackDuplicates) {
        List<String> lines;
        try {
            lines = FileUtils.readLines(new File(inputFilename), "utf-8");
        } catch (IOException e) {
            log.error("error reading file: {}", e.getMessage());
            return Optional.empty();
        }
        Document document =
            new Document()
                .space(appConfig.getSpace()/*getDocumentParam(lines, spaceKeyTemplate, appConfig.getSpace())*/)
                .title(getDocumentParam(lines, titleTemplate, null))
                .oldTitle(getDocumentParam(lines, titleOldTemplate, null))
                .parent(getDocumentParam(lines, parentTemplate, null))
                .hidden(getDocumentBooleanParam(lines, hiddenTemplate))
                .inputFilename(inputFilename)
                .contents(lines.stream().collect(Collectors.joining("\n")))
                .tags(
                    Arrays.asList(getDocumentParam(lines, tagsTemplate, null).split(","))
                    .stream()
                    .filter(s -> StringUtils.isNotBlank(s))
                    .map(s -> s.trim())
                    .collect(Collectors.toList())
        );

        if (trackDuplicates) {
            if (trackingService.contains(document.title()))
                log.warn("document '{}' already processed in this batch; suspected document title repeating", document.title());
            trackingService.add(document.title());
        }
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
                log.warn("can't publish document '" + document.inputFilename() + "' to confluence: not all confluence parameters are set (url, login, password)");
                return new ProcessingResult(RT_PUB_FAILURE);
            } else {
                if (!document.canPublish()) {
                    log.warn("can't publish document '" + document.inputFilename() + "' to confluence: not all document parameters are set (title, spaceKey)");
                    return new ProcessingResult(RT_PUB_FAILURE);
                } else {

                    Optional<String> pageIdOpt = Optional.empty();
                    if (StringUtils.isNotBlank(document.oldTitle())) {
                        log.trace("searching for page with old title: '{}'", document.oldTitle());
                        pageIdOpt = confluence.getPageId(document.space(), document.oldTitle());
                    }

                    if (pageIdOpt.isPresent()) {
                        // update old-titled document
                        log.trace("found page (with old title) '{}'", document.oldTitle());
                        return updateDocument(pageIdOpt.get(), document, convertedDocument);
                    } else {
                        log.trace("searching for page with title: '{}'", document.title());
                        pageIdOpt = confluence.getPageId(document.space(), document.title());
                        if (pageIdOpt.isPresent()) {
                            // update new-tiled document
                            log.trace("found page (with 'new' title)'{}'", document.title());
                            return updateDocument(pageIdOpt.get(), document, convertedDocument);
                        } else {
                            // publish document
                            log.trace("not found page with title '{}'; publishing new one", document.title());
                            return publishDocument(document, convertedDocument);
                        }
                    }
                }
            }
        } else {
            // or print to stdout
            System.out.println(convertedDocument);
            return new ProcessingResult(RT_FILE_SUCCESS);
        }
    }

    private ProcessingResult updateDocument(String pageId, Document document, String convertedDocument) {
        // if found, update page
        if (document.hidden()) {
            if (confluence.deletePage(pageId, document.title()) > 0) {
                log.info("document '{}' removed from server", document.title());
                return new ProcessingResult(RT_DEL_SUCCESS);
            } else {
                log.warn("could not remove document '{}' from server", document.title());
                return new ProcessingResult(RT_DEL_FAILURE);
            }
        } else {
            if (confluence.updatePage(pageId, document.title(), "current", convertedDocument)) {
                log.info(
                    String.format(
                        "Updated document in confluence: %s/display/%s/%s"
                        , appConfig.getUrl()
                        , document.space()
                        , document.title().replaceAll(" ", "+")
                    )
                );

                // remove stale tags from confluence document
                Collection<String> serverTags = confluence.getTags(pageId);
                serverTags.removeAll(document.tags());
                confluence.removeTags(pageId, serverTags);
                log.trace("removed page labels: {}", serverTags);

                // add new tags to confluence document
                document.tags().removeAll(serverTags);
                confluence.tagPage(pageId, document.tags());
                log.trace("added page labels: {}", document.tags());

                return new ProcessingResult(RT_UPD_SUCCESS);
            } else {
                log.warn(
                    String.format(
                        "Could not update document '%s' in confluence"
                        , document.title()
                    )
                );
                if (appConfig.isDebug())
                    System.out.println(convertedDocument);
                return new ProcessingResult(RT_UPD_FAILURE);
            }
        }
    }
    private ProcessingResult publishDocument(Document document, String convertedDocument) {
        if (!document.hidden()) {
            // publish page
            if (confluence.publishPage(document.space(), document.title(), document.parent(), "current", convertedDocument)) {
                log.info(
                    String.format(
                        "Published document to confluence: %s/display/%s/%s"
                        , appConfig.getUrl()
                        , document.space()
                        , document.title().replaceAll(" ", "+")
                    )
                );
                if (confluence.tagPage(document.space(), document.title(), document.tags())) {
                    log.info(
                        String.format(
                            "Labeled document with tags: %s"
                            , document.tags()
                        )
                    );
                }
                return new ProcessingResult(RT_PUB_SUCCESS);
            } else {
                log.warn(
                    String.format(
                        "Could not publish document '%s' to confluence server"
                        , document.title()
                    )
                );
                if (appConfig.isDebug())
                    System.out.println(convertedDocument);
                return new ProcessingResult(RT_PUB_FAILURE);
            }
        } else {
            log.warn("document '{}' is hidden, skip publishing", document.title());
            return new ProcessingResult(RT_SKP_HIDDEN);
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
    private boolean getDocumentBooleanParam(List<String> lines, String key) {
        Optional<String> argument =
            lines.stream()
                .filter(s -> s.startsWith("//") && s.contains(key))
                .findFirst();
        if (!argument.isPresent()) {
            return false;
        } else {
            String value = argument.get()
                .replaceAll("/", "")
                .replace(key, "")
                .trim();
            log.trace("hidden value: '{}'", value);
            return (StringUtils.isBlank(value)) ? true : Boolean.parseBoolean(value);
        }
    }
}
