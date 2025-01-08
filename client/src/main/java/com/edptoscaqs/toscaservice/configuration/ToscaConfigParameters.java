package com.edptoscaqs.toscaservice.configuration;

import com.edptoscaqs.toscaservice.utilities.Utilities;
import org.springframework.stereotype.Component;

@Component
public class ToscaConfigParameters {

    private String edpLockGroupName;
    private String toscaServerGateway;
    private int toscaServerPort;
    private String toscaClientId;
    private String toscaClientSecret;

    private String projectName;
    private String nonAOSWorkspace;
    private String testEventName;

    private String outputPath;
    private String testType;
    private ConfigFile all;
    private ConfigFile acceptance;
    private ConfigFile installation;
    private ConfigFile integration;

    public String getOutputPath() { return outputPath; }
    public void setOutputPath(String outputPath) { this.outputPath = Utilities.normalizePath(outputPath); }

    public String getTestType() { return testType; }
    public void setTestType(String testType) { this.testType = testType; }

    public String getEdpLockGroupName() { return edpLockGroupName; }
    public void setEdpLockGroupName(String edpLockGroupName) { this.edpLockGroupName = edpLockGroupName; }

    public String getToscaServerGateway() { return toscaServerGateway; }
    public void setToscaServerGateway(String toscaServerGateway) { this.toscaServerGateway = toscaServerGateway; }

    public int getToscaServerPort() { return toscaServerPort; }
    public void setToscaServerPort(int toscaServerPort) { this.toscaServerPort = toscaServerPort; }

    public String getToscaClientId() { return toscaClientId; }
    public void setToscaClientId(String toscaClientId) { this.toscaClientId = toscaClientId; }

    public String getToscaClientSecret() { return toscaClientSecret; }
    public void setToscaClientSecret(String toscaClientSecret) { this.toscaClientSecret = toscaClientSecret; }

    public String getProjectName() { return projectName; }
    public void setProjectName(String projectName) { this.projectName = projectName; }

    public String getNonAOSWorkspace() { return nonAOSWorkspace; }
    public void setNonAOSWorkspace(String nonAOSWorkspace) { this.nonAOSWorkspace = nonAOSWorkspace; }

    public String getTestEventName() { return testEventName; }
    public void setTestEventName(String testEventName) { this.testEventName = testEventName; }

    public long getExecutionWaitTimeOut() { return getConfig().getExecutionWaitTimeOut(); }
    public long getStatusSleepTime() { return getConfig().getStatusSleepTime(); }
    public long getReportCreationTimeOut() { return getConfig().getReportCreationTimeOut(); }
    public String getPdfReportName() { return getConfig().getPdfReportName(); }

    public void setAll(ConfigFile all) { this.all = all; }
    public void setAcceptance(ConfigFile acceptance) { this.acceptance = acceptance; }
    public void setInstallation(ConfigFile installation) { this.installation = installation; }
    public void setIntegration(ConfigFile integration) { this.integration = integration; }

    private ConfigFile getConfig() {
        switch (getTestType()) {
            case "acceptance": if (acceptance != null) return acceptance;
            case "installation": if (installation != null) return installation;
            case "integration": if (integration != null) return integration;
        }
        return all;
    }
}