package ws.slink.parser;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DurationFormatUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ws.slink.atlassian.Confluence;
import ws.slink.config.AppConfig;
import ws.slink.model.Document;
import ws.slink.model.Page;
import ws.slink.model.ProcessingResult;
import ws.slink.service.TrackingService;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static ws.slink.model.ProcessingResult.ResultType.*;

@Slf4j
@Component
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class Processor {

    private final @NonNull AppConfig appConfig;
    private final @NonNull DirectoryProcessor directoryProcessor;
    private final @NonNull FileProcessor fileProcessor;
    private final @NonNull Confluence confluence;
    private final @NonNull TrackingService trackingService;

    @Value("${confluence.protected.label:}")
    private List<String> protectedLabels;

    public String process() {

        long timeA = Instant.now().toEpochMilli();

        // cleanup all the needed spaces
        if(!appConfig.getClean().isEmpty()) {
            log.info("Cleaning up following space(s): " + appConfig.getClean());
            appConfig.getClean()
                .stream()
                .forEach(s -> log.info("Removed " + confluence.cleanSpace(s) + " page(s) from " + s));
        }

        long timeB = Instant.now().toEpochMilli();

        // process documentation sources
        ProcessingResult result = new ProcessingResult();
        if (StringUtils.isNotBlank(appConfig.getDir())) {
            result.merge(directoryProcessor.process(appConfig.getDir()));
            result.merge(removeStaleArticles(appConfig.getDir()));
        }
        else if (StringUtils.isNotBlank(appConfig.getInput()))
            result.merge(fileProcessor.process(appConfig.getInput()));

        long timeC = Instant.now().toEpochMilli();

        return new StringBuilder()
            .append("-------------------------------------------------------------").append("\n")
            .append("total time taken   : " + DurationFormatUtils.formatDuration( timeC - timeA, "HH:mm:ss")).append("\n")
            .append("clean up time      : " + DurationFormatUtils.formatDuration( timeB - timeA, "HH:mm:ss")).append("\n")
            .append("publishing time    : " + DurationFormatUtils.formatDuration( timeC - timeB, "HH:mm:ss")).append("\n")
            .append("-------------------------------------------------------------").append("\n")
            .append(String.format("%20s %10s %10s\n", "", "SUCCESS", "FAILURE"))
            .append(getStatStr("published          :", result.get(RT_PUB_SUCCESS).get(), result.get(RT_PUB_FAILURE).get(), "\n"))
            .append(getStatStr("updated            :", result.get(RT_UPD_SUCCESS).get(), result.get(RT_UPD_FAILURE).get(), "\n"))
            .append(getStatStr("removed            :", result.get(RT_DEL_SUCCESS).get(), result.get(RT_DEL_FAILURE).get(), "\n"))
            .append(getStatStr("skipped hidden     :", result.get(RT_SKP_HIDDEN).get(), null, "\n"))
            .append(String.format("%20s", "duplicate titles   :")).append("\n")
            .append(trackingService
                .get()
                .entrySet()
                .stream()
                .map(e -> "                        " + e.getKey() + " x " + e.getValue())
                .collect(Collectors.joining("\n")))
            .toString();

    }

    private ProcessingResult removeStaleArticles(String directoryPath) {

        ProcessingResult result = new ProcessingResult();

        if (confluence.canPublish()) {
            Collection<Document> documents = getRepositoryDocuments(directoryPath);
            List<String> repoTitles = documents.stream().map(d -> d.title()).collect(Collectors.toList());

            Collection<Page> pages = confluence.getPages(appConfig.getSpace());

            List<String> serverTitles = pages
                .stream()
                .filter(p -> p.labels().stream().noneMatch(protectedLabels::contains))
                .map(d -> d.title()).collect(Collectors.toList());
//            List<String> serverTitles = pages.stream().map(d -> d.title()).collect(Collectors.toList());

            serverTitles.removeAll(repoTitles);
            if (serverTitles.size() > 0) {
                log.info("--- to be removed -------------------------------------------------");
                serverTitles.stream().forEach(log::info);
                log.info("-------------------------------------------------------------------");
            }

            serverTitles
                .stream()
                .forEach(title -> {
                    confluence
                        .getPageId(appConfig.getSpace(), title)
                        .ifPresent(pageId -> {
                            log.trace("removing page #{}", pageId);
                            if (confluence.deletePage(pageId, title) > 0)
                                result.add(RT_DEL_SUCCESS);
                            else
                                result.add(RT_DEL_FAILURE);
                        });
                })
            ;
        }

        return result;
    }

    private Collection<Document> getRepositoryDocuments(String directoryPath) {
        List<Document> documents = new ArrayList<>();
        try {
            Files.list(Paths.get(directoryPath))
                    .map(Path::toFile)
                    .filter(f -> f.isDirectory())
                    .map(File::toPath)
                    .map(Path::toString)
                    .parallel()
                    .forEach(d -> documents.addAll(getRepositoryDocuments(d)))
            ;
            Files.list(Paths.get(directoryPath))
                    .map(Path::toFile)
                    .filter(File::isFile)
                    .filter(f -> f.getName().endsWith(".adoc") || f.getName().endsWith(".asciidoc"))
                    .map(f -> f.getAbsolutePath())
                    .parallel()
                    .map(f -> fileProcessor.read(f, false))
                    .filter(d -> d.isPresent())
                    .forEach(d -> documents.add(d.get()))
            ;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return documents;
    }

    private String getStatStr(String prefix, Integer success, Integer failures, String suffix) {
        if (null == failures)
            return String.format("%20s %10d %10s%s", prefix, success, "-", suffix);
        else
            return String.format("%20s %10d %10d%s", prefix, success, failures, suffix);
    }

}
