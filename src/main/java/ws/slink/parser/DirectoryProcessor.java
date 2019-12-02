package ws.slink.parser;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ws.slink.model.ProcessingResult;

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

    public ProcessingResult process(String directoryPath) {
        log.trace("processing: {}", directoryPath);
        ProcessingResult result = new ProcessingResult();
        result.merge(processAllFiles(directoryPath));
        result.merge(processAllDirectories(directoryPath));
        return result;
    }

    private ProcessingResult processAllFiles(String directoryPath) {
        ProcessingResult result = new ProcessingResult();
        try {
            Files.list(Paths.get(directoryPath))
                .map(Path::toFile)
                .filter(File::isFile)
                .filter(f -> f.getName().endsWith(".adoc") || f.getName().endsWith(".asciidoc"))
                .map(f -> f.getAbsolutePath())
                .parallel()
                .forEach(f -> result.merge(fileProcessor.process(f)));
        } catch (IOException e) {
            log.error("error processing files in {}: {}", directoryPath, e.getMessage());
            if (log.isTraceEnabled())
                e.printStackTrace();
        }
        return result;
    }

    private ProcessingResult processAllDirectories(String directoryPath) {
        ProcessingResult result = new ProcessingResult();
        try {
            Files.list(Paths.get(directoryPath))
                .map(Path::toFile)
                .filter(f -> f.isDirectory())
                .map(File::toPath)
                .map(Path::toString)
                .parallel()
                .forEach(d -> result.merge(process(d)));
        } catch (IOException e) {
            log.error("error processing directory {}: {}", directoryPath, e.getMessage());
            if (log.isTraceEnabled())
                e.printStackTrace();
        }
        return result;
    }

}
