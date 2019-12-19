package com.dxfeed.parser;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DurationFormatUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.dxfeed.atlassian.Confluence;
import com.dxfeed.config.AppConfig;
import com.dxfeed.model.ProcessingResult;
import com.dxfeed.service.TrackingService;

import java.time.Instant;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class Processor {

    private final @NonNull AppConfig appConfig;
    private final @NonNull DirectoryProcessor directoryProcessor;
    private final @NonNull FileProcessor fileProcessor;
    private final @NonNull Confluence confluence;
    private final @NonNull TrackingService trackingService;

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
        if (StringUtils.isNotBlank(appConfig.getDir()))
            result.merge(directoryProcessor.process(appConfig.getDir()));
        else if (StringUtils.isNotBlank(appConfig.getInput()))
            result.merge(fileProcessor.process(appConfig.getInput()));

        long timeC = Instant.now().toEpochMilli();

        return new StringBuilder()
            .append("-------------------------------------------------------------").append("\n")
            .append("total time taken      : " + DurationFormatUtils.formatDuration( timeC - timeA, "HH:mm:ss")).append("\n")
            .append("clean up time         : " + DurationFormatUtils.formatDuration( timeB - timeA, "HH:mm:ss")).append("\n")
            .append("publishing time       : " + DurationFormatUtils.formatDuration( timeC - timeB, "HH:mm:ss")).append("\n")
            .append("successfully processed: " + result.successful().get()).append("\n")
            .append("processing failures   : " + result.failed().get()).append("\n")
            .append("duplicate titles      : ").append("\n")
            .append(trackingService
                .get()
                .entrySet()
                .stream()
                .map(e -> "                        " + e.getKey() + " x " + e.getValue())
                .collect(Collectors.joining("\n")))
            .toString();

    }

}
