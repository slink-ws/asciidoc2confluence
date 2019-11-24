package ws.slink.parser;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Slf4j
@Component
@RequiredArgsConstructor (onConstructor = @__(@Autowired))
public class DirectoryProcessor {

    private final FileProcessor fileProcessor;

    public void process(String directoryPath) {
        log.trace("processing: {}", directoryPath);
        processAllFiles(directoryPath);
        processAllDirectories(directoryPath);
    }

    private void processAllFiles(String directoryPath) {
        try {
            Files.list(Paths.get(directoryPath))
                .map(Path::toFile)
                .filter(File::isFile)
                .filter(f -> f.getName().endsWith(".adoc") || f.getName().endsWith(".asciidoc"))
                .map(f -> f.getAbsolutePath())
                .parallel()
                .forEach(fileProcessor::process);
        } catch (IOException e) {
            log.error("error processing files in {}: {}", directoryPath, e.getMessage());
            if (log.isTraceEnabled())
                e.printStackTrace();
        }
    }

    private void processAllDirectories(String directoryPath) {
        try {
            Files.list(Paths.get(directoryPath))
                .map(Path::toFile)
                .filter(f -> f.isDirectory())
                .map(File::toPath)
                .map(Path::toString)
                .parallel()
                .forEach(this::process);
        } catch (IOException e) {
            log.error("error processing directory {}: {}", directoryPath, e.getMessage());
            if (log.isTraceEnabled())
                e.printStackTrace();
        }
    }

}
