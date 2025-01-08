package com.edptoscaqs.toscaservice;

import com.edptoscaqs.toscaservice.configuration.ConfigLoader;
import com.edptoscaqs.toscaservice.configuration.ToscaConfigParameters;
import com.edptoscaqs.toscaservice.execution.ExecutionManager;
import com.edptoscaqs.toscaservice.execution.FreezeHandler;
import com.edptoscaqs.toscaservice.execution.ResultsHandler;
import com.edptoscaqs.toscaservice.logging.LoggerHelper;
import com.edptoscaqs.toscaservice.utilities.Utilities;
import net.minidev.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.Map;
import java.time.format.DateTimeFormatter;
import java.time.ZonedDateTime;
import static com.edptoscaqs.toscaservice.configuration.Constants.*;

@Service
public class ToscaService {
    @Autowired
    private ToscaConfigParameters toscaConfiguration;

    @Autowired
    private LoggerHelper loggerHelper;
    
    private final ConfigLoader configLoader;
    private final FreezeHandler freezeHandler;
    private final ExecutionManager executionManager;
    private final ResultsHandler resultsHandler;
    private final Utilities utilities;

    @Autowired
    public ToscaService(FreezeHandler freezeHandler,
                        ExecutionManager executionManager,
                        ResultsHandler resultsHandler,
                        ToscaConfigParameters toscaConfiguration,
                        ConfigLoader configLoader,
                        Utilities utilities,
                        LoggerHelper loggerHelper) {
        this.freezeHandler = freezeHandler;
        this.executionManager = executionManager;
        this.resultsHandler = resultsHandler;
        this.toscaConfiguration = toscaConfiguration;
        this.configLoader = configLoader;
        this.utilities = utilities;
        this.loggerHelper = loggerHelper;
    }

    public boolean executeTestCases(String projectName, String testEventName, Map<String, String> gitReportParameters, Map<String, String> eventParameters,
                                    Map<String, String> executionCharacteristics, boolean releaseExecution, String testType, String outputPath) throws Exception {
        try {
            configLoader.loadConfiguration(projectName, testEventName, testType, outputPath, toscaConfiguration);

            StringBuilder logText = new StringBuilder();
            logText.append(String.format("%sPARAMETERS FOR THE EXECUTION", NEW_LINE));
            logText.append(String.format("%s  - Project name:    %s", NEW_LINE, projectName));
            logText.append(String.format("%s  - Test event name: %s", NEW_LINE, testEventName));
            logText.append(String.format("%s  - Release mode:    %s", NEW_LINE, releaseExecution));
            logText.append(String.format("%s  - Test type:       %s", NEW_LINE, testType));
            logText.append(String.format("%s  - Output path:     %s", NEW_LINE, outputPath));
            loggerHelper.logInfo(logText.toString());

            logText.setLength(0);
            logText.append(String.format("%s[ExecuteTestCases] Tosca Configuration", NEW_LINE));
            logText.append(String.format("%s  - Name: ProjectName         - Value: %s", NEW_LINE, toscaConfiguration.getProjectName()));
            logText.append(String.format("%s  - Name: NonAOSWorkspace     - Value: %s", NEW_LINE, toscaConfiguration.getNonAOSWorkspace()));
            logText.append(String.format("%s  - Name: TestEventName       - Value: %s", NEW_LINE, toscaConfiguration.getTestEventName()));
            logText.append(String.format("%s  - Name: EDPLockGroupName    - Value: %s", NEW_LINE, toscaConfiguration.getEdpLockGroupName()));
            logText.append(String.format("%s  - Name: ToscaServerGateway  - Value: %s", NEW_LINE, toscaConfiguration.getToscaServerGateway()));
            logText.append(String.format("%s  - Name: ToscaServerPort     - Value: %s", NEW_LINE, toscaConfiguration.getToscaServerPort()));
            logText.append(String.format("%s  - Name: ToscaClientId       - Value: %s", NEW_LINE, toscaConfiguration.getToscaClientId()));
            logText.append(String.format("%s  - Name: ClientSecret        - Value: %s", NEW_LINE, toscaConfiguration.getToscaClientSecret()));
            logText.append(String.format("%s-----------------------------------", NEW_LINE));
            loggerHelper.logDebug(logText.toString());

            logText.setLength(0);
            logText.append(String.format("%s[ExecuteTestCases] Config file", NEW_LINE));
            logText.append(String.format("%s  - Name: Execution time out      - Value: %s", NEW_LINE, toscaConfiguration.getExecutionWaitTimeOut()));
            logText.append(String.format("%s  - Name: Check status sleep time - Value: %s", NEW_LINE, toscaConfiguration.getStatusSleepTime()));
            logText.append(String.format("%s  - Name: Report creation timeout - Value: %s", NEW_LINE, toscaConfiguration.getReportCreationTimeOut()));
            logText.append(String.format("%s  - Name: PDF Report name         - Value: %s", NEW_LINE, toscaConfiguration.getPdfReportName()));
            loggerHelper.logInfo(logText.toString());logText.setLength(0);

            StringBuilder gitParameters = new StringBuilder();
            if (!gitReportParameters.isEmpty()) {
                logText.append(String.format("%s[ExecuteTestCases] Git parameters", NEW_LINE));
                gitReportParameters.forEach((key, value) -> logText.append(String.format("%s  - Name: %s - Value: %s", NEW_LINE, key, value)));
                gitReportParameters.forEach((key, value) -> gitParameters.append(String.format("%s: %s%s", key, value, NEW_LINE)));
            }
            if (!eventParameters.isEmpty()) {
                logText.append(String.format("%s[ExecuteTestCases] Event parameters", NEW_LINE));
                eventParameters.forEach((key, value) -> logText.append(String.format("%s  - Name: %s - Value: %s", NEW_LINE, key, value)));
            }
            if (!executionCharacteristics.isEmpty()) {
                logText.append(String.format("%s[ExecuteTestCases] Execution characteristics", NEW_LINE));
                executionCharacteristics.forEach((key, value) -> logText.append(String.format("%s  - Name: %s - Value: %s", NEW_LINE, key, value)));
            }
            if (!logText.isEmpty()){
                loggerHelper.logInfo(logText.toString());
                logText.setLength(0);
            }
            utilities.removeGitParametersFile(toscaConfiguration.getOutputPath(), loggerHelper);
            if (!gitParameters.isEmpty())
                utilities.writeGitParametersFile(gitParameters, toscaConfiguration.getOutputPath(), loggerHelper);
            gitParameters.setLength(0);
            freezeHandler.freezeTestEvent(releaseExecution);
            loggerHelper.logInfo("STARTING EXECUTION");
            JSONObject execution = executionManager.triggerExecution(eventParameters, executionCharacteristics);
            String executionId = execution.getAsString("ExecutionId");
            String startTime = execution.getAsString("CreatedAt");
            ZonedDateTime parsedDate = ZonedDateTime.parse(startTime);
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            String formattedDate = parsedDate.toLocalDateTime().format(formatter);
            loggerHelper.logInfo(String.format("START TIME: %s", formattedDate));
            loggerHelper.logInfo(String.format("EXECUTION ID: %s", executionId));
            executionManager.getExecutionStatus(executionId, freezeHandler.getTestCaseCounter());
            utilities.createDirectory(toscaConfiguration.getOutputPath(), loggerHelper);
            loggerHelper.logInfo("GETTING EXECUTION XML RESULTS");
            boolean allTestsPassed = resultsHandler.handleExecutionResults(executionId);
            loggerHelper.logInfo("GETTING PDF REPORT");
            resultsHandler.savePDFReport(executionId);
            loggerHelper.logDebug(String.format("[ExecuteTestCases] Process ends successfully - Project Name: %s - Test Event Name: %s", projectName, testEventName));
            loggerHelper.logInfo("FINISHING EXECUTION");
            loggerHelper.closeAndCopyLogFile(toscaConfiguration.getOutputPath());
            return allTestsPassed;
        } catch (Exception e) {
            loggerHelper.logException(e);
            loggerHelper.closeAndCopyLogFile(toscaConfiguration.getOutputPath());
            throw e;
        }
    }
}
