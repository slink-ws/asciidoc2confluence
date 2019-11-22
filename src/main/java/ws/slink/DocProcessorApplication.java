package com.dxfeed;

import com.dxfeed.atlassian.Confluence;
import com.dxfeed.config.CommandLineArguments;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;

@Slf4j
@SpringBootApplication
@EnableAutoConfiguration
public class DocProcessorApplication {

	@EventListener(classes = {ContextRefreshedEvent.class})
	private void handleContextStartedEvent(ContextRefreshedEvent ctxStartEvt) {
	}

	@EventListener(classes = {ContextClosedEvent.class})
	private void handleContextClosedEvent(ContextClosedEvent ctxCloseEvt) {
	}

	@Bean
	public CommandLineRunner commandLineRunner() {
		return new DocProcessorApplicationRunner();
	}

	@Autowired
	private CommandLineArguments commandLineArguments;

	@Bean
	public Confluence confluenceInstance() {
		return new Confluence(
			commandLineArguments.confluenceUrl,
			commandLineArguments.confluenceUser,
			commandLineArguments.confluencePassword);
	}

	public static void main(String[] args) {
		SpringApplication.run(DocProcessorApplication.class, args);
	}

}
