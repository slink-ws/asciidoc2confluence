package com.dxfeed.parser;

import com.dxfeed.config.CommandLineArguments;
import com.dxfeed.model.Document;
import com.dxfeed.processor.CodeBlockPostProcessor;
import com.dxfeed.processor.CodeBlockPreProcessor;
import com.dxfeed.processor.CodeBlockProcessor;
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
        asciidoctor = Asciidoctor.Factory.create();

        // register preprocessors
        asciidoctor.javaExtensionRegistry().preprocessor(CodeBlockPreProcessor.class);

        // register block processors
        asciidoctor.javaExtensionRegistry().block(CodeBlockProcessor.class);

        // register postprocessors
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
                    .filter(s -> s.contains(":DOCUMENT-SPACE:"))
                    .findFirst()
                    .orElse("")
                    .replace(":DOCUMENT-SPACE:", "")
                    .replace("/", "")
                    .trim()
                )
                .title(lines
                    .stream()
                    .filter(s -> s.contains(":DOCUMENT-TITLE:"))
                    .findFirst()
                    .orElse("")
                    .replace(":DOCUMENT-TITLE:", "")
                    .replace("/", "")
                    .trim()
                )
                .inputFilename(inputFilename)
                .outputFilename(commandLineArguments.outputFilename)
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
}
