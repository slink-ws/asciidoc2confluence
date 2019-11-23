package ws.slink;

import ws.slink.atlassian.Confluence;
import ws.slink.config.CommandLineArguments;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;

@Slf4j
@SpringBootApplication
public class DocProcessorApplication {

    private CommandLineArguments commandLineArguments;

    @EventListener(classes = {ContextRefreshedEvent.class})
    public void handleContextStartedEvent(ContextRefreshedEvent ctxStartEvt) {
    }

    @EventListener(classes = {ContextClosedEvent.class})
    public void handleContextClosedEvent(ContextClosedEvent ctxCloseEvt) {
    }

    @Autowired
    public DocProcessorApplication(CommandLineArguments commandLineArguments) {
        this.commandLineArguments = commandLineArguments;
    }

    @Bean
    public CommandLineRunner commandLineRunner() {
        return new DocProcessorApplicationRunner();
    }

    @Bean
    public Confluence confluenceInstance() {
        return new Confluence(
            commandLineArguments.getConfluenceUrl(),
            commandLineArguments.getConfluenceUser(),
            commandLineArguments.getConfluencePassword()
        );
    }

    public static void main(String[] args) {
        SpringApplication.run(DocProcessorApplication.class, args);
    }
}
