package ws.slink.parser;

import ws.slink.config.CommandLineArguments;
import ws.slink.model.Document;
import ws.slink.processor.CodeBlockPostProcessor;
import ws.slink.processor.CodeBlockPreProcessor;
import ws.slink.processor.CodeBlockProcessor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.asciidoctor.Asciidoctor;
import org.asciidoctor.OptionsBuilder;
import org.asciidoctor.SafeMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

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
public class FileProcessor {

    private static final String SPACE_KEY_TEMPLATE = ":DOCUMENT-SPACE-KEY:";
    private static final String TITLE_TEMPLATE = ":DOCUMENT-TITLE:";
    private static final String PARENT_TEMPLATE = ":DOCUMENT-PARENT:";

    @Autowired
    private CommandLineArguments commandLineArguments;

    private Asciidoctor asciidoctor;

    @SuppressWarnings("unchecked")
    private void disableAccessWarnings() {
        try {
            Class unsafeClass = Class.forName("sun.misc.Unsafe");
            Field field = unsafeClass.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            Object unsafe = field.get(null);

            Method putObjectVolatile =
                unsafeClass.getDeclaredMethod("putObjectVolatile", Object.class, long.class, Object.class);
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
        asciidoctor = Asciidoctor.Factory.create();

        // register pre processors
        asciidoctor.javaExtensionRegistry().preprocessor(CodeBlockPreProcessor.class);

        // register block processors
        asciidoctor.javaExtensionRegistry().block(CodeBlockProcessor.class);

        // register post processors
        asciidoctor.javaExtensionRegistry().postprocessor(CodeBlockPostProcessor.class);
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
                    .filter(s -> s.contains(SPACE_KEY_TEMPLATE))
                    .findFirst()
                    .orElse("")
                    .replace(SPACE_KEY_TEMPLATE, "")
                    .replace("/", "")
                    .trim()
                )
                .title(lines
                    .stream()
                    .filter(s -> s.contains(TITLE_TEMPLATE))
                    .findFirst()
                    .orElse("")
                    .replace(TITLE_TEMPLATE, "")
                    .replace("/", "")
                    .trim()
                )
                .parent(lines
                    .stream()
                    .filter(s -> s.contains(PARENT_TEMPLATE))
                    .findFirst()
                    .orElse("")
                    .replace(PARENT_TEMPLATE, "")
                    .replace("/", "")
                    .trim()
                )
                .inputFilename(inputFilename)
                .outputFilename(commandLineArguments.outputFilename)
                .contents(String.join("\n", lines))
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
}
