package com.edptoscaqs.toscaservice.configuration;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.Map;

@Component
public class ConfigLoader {

    public void loadConfiguration(String projectName, String testEventName, String testType, String outputPath, ToscaConfigParameters toscaConfiguration) throws RuntimeException {
        try {
            toscaConfiguration.setProjectName(projectName);
            toscaConfiguration.setNonAOSWorkspace(projectName + "_NonAOS");
            toscaConfiguration.setTestEventName(testEventName);
            toscaConfiguration.setEdpLockGroupName(System.getenv("EDP_LOCK_GROUP_NAME"));
            toscaConfiguration.setToscaServerGateway(System.getenv("TOSCA_SERVER"));
            toscaConfiguration.setToscaServerPort(Integer.parseInt(System.getenv("TOSCA_SERVER_PORT")));
            toscaConfiguration.setToscaClientId(System.getenv("TOSCA_SERVER_CLIENT_ID"));
            toscaConfiguration.setToscaClientSecret(System.getenv("TOSCA_SERVER_CLIENT_SECRET"));
            toscaConfiguration.setTestType(testType);
            toscaConfiguration.setOutputPath(outputPath);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public ToscaConfigParameters loadConfigurationFromJSON(String configFilePath) throws RuntimeException {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            Map<String, ConfigFile> configMap = objectMapper.readValue(new File(configFilePath), new TypeReference<Map<String, ConfigFile>>() {});

            ToscaConfigParameters toscaConfigParameters = new ToscaConfigParameters();
            if (configMap.containsKey("all")) { toscaConfigParameters.setAll(configMap.get("all")); }
            if (configMap.containsKey("acceptance")) { toscaConfigParameters.setAcceptance(configMap.get("acceptance")); }
            if (configMap.containsKey("installation")) { toscaConfigParameters.setInstallation(configMap.get("installation")); }
            if (configMap.containsKey("integration")) { toscaConfigParameters.setIntegration(configMap.get("integration")); }

            return toscaConfigParameters;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
