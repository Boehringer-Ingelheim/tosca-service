package com.edptoscaqs.toscaservice.execution;

import com.edptoscaqs.toscaservice.ToscaExecutionAPIHandler;
import com.edptoscaqs.toscaservice.configuration.ToscaConfigParameters;
import com.edptoscaqs.toscaservice.enums.ExecutionStatusEnum;
import com.edptoscaqs.toscaservice.logging.LoggerHelper;
import com.edptoscaqs.toscaservice.utilities.Utilities;
import net.minidev.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.TimeoutException;
import static com.edptoscaqs.toscaservice.configuration.Constants.*;

@Service
public class ExecutionManager {
    @Autowired
    private ToscaConfigParameters toscaConfiguration;

    @Autowired
    private LoggerHelper loggerHelper;

    private final ToscaExecutionAPIHandler toscaExecutionAPIHandler;
    private final Utilities utilities;

    @Autowired
    public ExecutionManager(ToscaExecutionAPIHandler toscaExecutionAPIHandler,
                            Utilities utilities, ToscaConfigParameters toscaConfiguration, LoggerHelper loggerHelper) {
        this.toscaExecutionAPIHandler = toscaExecutionAPIHandler;
        this.toscaConfiguration = toscaConfiguration;
        this.utilities = utilities;
        this.loggerHelper = loggerHelper;
    }

    public JSONObject triggerExecution(Map<String, String> eventParameters, Map<String, String> executionCharacteristics) throws Exception {
        loggerHelper.logDebug(String.format("[TriggerExecution] Process starts - Project Name: %s - Test Event Name: %s", toscaConfiguration.getProjectName(), toscaConfiguration.getTestEventName()));
        try {
            JSONObject execution = toscaExecutionAPIHandler.triggerEventExecution(toscaConfiguration.getProjectName(), "Dex", toscaConfiguration.getTestEventName(), eventParameters, executionCharacteristics);
            if (execution.getAsString("ExecutionId") == null || execution.getAsString("ExecutionId").isEmpty()) {
                throw new IllegalArgumentException(String.format("[TriggerExecution] Process ends with an error: Execution ID is null or empty - Project Name: %s - Test Event Name: %s", toscaConfiguration.getProjectName(), toscaConfiguration.getTestEventName()));
            }
            loggerHelper.logDebug(String.format("[TriggerExecution] Process ends successfully - Project Name: %s - Test Event Name: %s", toscaConfiguration.getProjectName(), toscaConfiguration.getTestEventName()));
            return execution;
        } catch (Exception e) {
            throw e;
        }
    }

    public void getExecutionStatus(String executionId, int totalTestCases) throws Exception {
        loggerHelper.logDebug(String.format("[GetExecutionStatus] Process starts - Execution Id: %s - Timeout: %d", executionId, toscaConfiguration.getExecutionWaitTimeOut()));
        StringBuilder statusLog = new StringBuilder();
        long startTime = System.currentTimeMillis();
        long endTime = startTime + toscaConfiguration.getExecutionWaitTimeOut() * 60000L;
        boolean continueCheckingStatus = true;
        try {
            while (continueCheckingStatus) {
                ExecutionStatusEnum executionStatus = ExecutionStatusEnum.get(toscaExecutionAPIHandler.getEventExecutionStatus(executionId));
                continueCheckingStatus = executionStatus.handleStatus(loggerHelper);
                if (System.currentTimeMillis() > endTime) {
                    loggerHelper.logInfo(String.format("Execution timeout exceeded - Execution Id: %s - Timeout: %s", executionId, toscaConfiguration.getExecutionWaitTimeOut()));
                    toscaExecutionAPIHandler.CancelExecution(executionId);
                    throw new TimeoutException(String.format("[GetExecutionStatus] Process ends with an error: Timeout exceeded - Execution Id: %s - Timeout: %s", executionId, toscaConfiguration.getExecutionWaitTimeOut()));
                }
                statusLog.append(String.format("%sElapsed time [min]: %s", NEW_LINE, (System.currentTimeMillis() - startTime) / 1000 / 60));
                appendPartialResults(executionId, totalTestCases, statusLog);
                loggerHelper.logInfo(statusLog.toString());
                statusLog.setLength(0);
                utilities.sleepWithInterruptHandling(toscaConfiguration.getStatusSleepTime(), loggerHelper);
            }
        } catch (Exception e) {
            throw e;
        }
    }

    protected void appendPartialResults(String executionId, int totalTestCases, StringBuilder statusLog) throws Exception {
        String xmlString = toscaExecutionAPIHandler.getPartialExecutionResults(executionId);
        if (xmlString != null && !xmlString.isEmpty()) {
            Map<String, Integer> partialResults = utilities.extractPartialTestResults(xmlString);
            for (Map.Entry<String, Integer> entry : partialResults.entrySet().stream()
                    .sorted(Map.Entry.<String, Integer>comparingByKey().reversed())
                    .toList()) {
                if (entry.getKey().equals("Total Test Cases")) {
                    statusLog.append(String.format("%s%s = %s/%d", NEW_LINE, entry.getKey(), entry.getValue(), totalTestCases));
                } else {
                    statusLog.append(String.format("%s   - %s = %s", NEW_LINE, entry.getKey(), entry.getValue()));
                }
            }
        } else {
            statusLog.append(String.format("%sProgress: %s", NEW_LINE, "No data"));
        }
    }

}
