package com.probuild.platform;

import io.camunda.client.annotation.Deployment;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * ProBuild Operations Platform — orchestration entry point.
 * Auto-deploys every process, decision table and form on startup.
 */
@SpringBootApplication(scanBasePackages = "com.probuild.platform")
@EntityScan(basePackages = "com.probuild.platform.data.entity")
@EnableJpaRepositories(basePackages = "com.probuild.platform.data.repo")
@Deployment(resources = {
        "classpath*:processes/*.bpmn",
        "classpath*:dmn/*.dmn",
        "classpath*:forms/*.form"
})
public class PlatformApplication {
    public static void main(String[] args) {
        SpringApplication.run(PlatformApplication.class, args);
    }
}
