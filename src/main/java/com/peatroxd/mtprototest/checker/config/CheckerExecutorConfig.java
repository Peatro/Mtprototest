package com.peatroxd.mtprototest.checker.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
public class CheckerExecutorConfig {

    @Bean(destroyMethod = "shutdown")
    public ExecutorService proxyCheckerExecutor() {
        return Executors.newFixedThreadPool(32);
    }
}
