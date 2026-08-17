package com.portfolio.chaosstream.exception;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerAutoConfigurationTest {

    private final WebApplicationContextRunner servletContextRunner = new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(GlobalExceptionHandlerAutoConfiguration.class));

    private final ApplicationContextRunner nonWebContextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(GlobalExceptionHandlerAutoConfiguration.class));

    @Test
    void registersGlobalExceptionHandlerInServletWebApplications() {
        servletContextRunner.run(context ->
                assertThat(context).hasSingleBean(GlobalExceptionHandler.class));
    }

    @Test
    void backsOffWhenUserAlreadyDefinesTheBean() {
        servletContextRunner.withUserConfiguration(CustomHandlerConfig.class)
                .run(context -> assertThat(context.getBean(GlobalExceptionHandler.class))
                        .isSameAs(CustomHandlerConfig.CUSTOM_HANDLER));
    }

    @Test
    void doesNotRegisterOutsideServletWebApplications() {
        nonWebContextRunner.run(context ->
                assertThat(context).doesNotHaveBean(GlobalExceptionHandler.class));
    }

    @Configuration
    static class CustomHandlerConfig {

        static final GlobalExceptionHandler CUSTOM_HANDLER = new GlobalExceptionHandler();

        @Bean
        GlobalExceptionHandler globalExceptionHandler() {
            return CUSTOM_HANDLER;
        }
    }
}
