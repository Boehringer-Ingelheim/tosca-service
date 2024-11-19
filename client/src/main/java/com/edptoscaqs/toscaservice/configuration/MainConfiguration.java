package com.edptoscaqs.toscaservice.configuration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MainConfiguration {
    @Value("${CONFIGURATION_FILE_PATH}")
    protected String configurationFilePath;

    private final ConfigLoader configLoader;

    @Autowired
    public MainConfiguration(ConfigLoader configLoader) {
        this.configLoader = configLoader;
    }

    @Bean
    public ToscaConfigParameters toscaConfiguration() {
        return configLoader.loadConfigurationFromJSON(configurationFilePath);
    }
}