package com.dxfeed.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Slf4j
@Component
public class CommandLineArguments {

    @Autowired
    public CommandLineArguments(ApplicationArguments args, AppConfig appConfig) {
        if (args.containsOption("input")) {
            appConfig.setInput(args.getOptionValues("input").get(0));
        }
        if (args.containsOption("dir")) {
            appConfig.setDir(args.getOptionValues("dir").get(0));
        }
        if (args.containsOption("clean")) {
            appConfig.getClean().addAll(Arrays.asList(args.getOptionValues("clean").get(0).split(",")));
        }
        if (args.containsOption("force")) {
            appConfig.setForce(true);
        }
        if (args.containsOption("url")) {
            appConfig.setUrl(args.getOptionValues("url").get(0));
        }
        if (args.containsOption("user")) {
            appConfig.setUser(args.getOptionValues("user").get(0));
        }
        if (args.containsOption("pass")) {
            appConfig.setPass(args.getOptionValues("pass").get(0));
        }
        if (args.containsOption("space")) {
            appConfig.setSpace(args.getOptionValues("space").get(0));
        }
        if (args.containsOption("dbg")) {
            appConfig.setDebug(true);
        }
    }




}
