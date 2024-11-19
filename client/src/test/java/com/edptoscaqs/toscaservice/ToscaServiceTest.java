package com.edptoscaqs.toscaservice;

import com.edptoscaqs.toscaservice.configuration.ConfigLoader;
import com.edptoscaqs.toscaservice.configuration.ToscaConfigParameters;
import com.edptoscaqs.toscaservice.execution.ExecutionManager;
import com.edptoscaqs.toscaservice.execution.FreezeHandler;
import com.edptoscaqs.toscaservice.execution.ResultsHandler;
import com.edptoscaqs.toscaservice.logging.LoggerHelper;
import com.edptoscaqs.toscaservice.utilities.Utilities;
import net.minidev.json.JSONObject;
import org.junit.Before;

import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

public class ToscaServiceTest {
    @Mock
    private FreezeHandler freezeHandler;
    @Mock
    private ExecutionManager executionManager;
    @Mock
    private ResultsHandler resultsHandler;
    @Mock
    private ToscaConfigParameters toscaConfiguration;
    @Mock
    private ConfigLoader configLoader;
    @Mock
    private Utilities utilities;
    @Mock
    private LoggerHelper loggerHelper;
    @InjectMocks
    private ToscaService toscaService;

    @Before
    public void init() {
        MockitoAnnotations.initMocks(this);
        when(toscaConfiguration.getOutputPath()).thenReturn("outputPath");
        doNothing().when(utilities).createDirectory(anyString(), any(LoggerHelper.class));
        doNothing().when(utilities).writeGitParametersFile(any(), anyString(), any(LoggerHelper.class));
        doNothing().when(configLoader).loadConfiguration(anyString(), anyString(), anyString(), anyString(), any(ToscaConfigParameters.class));
        doNothing().when(loggerHelper).logInfo(anyString());
        doNothing().when(loggerHelper).logWarning(anyString());
        doNothing().when(loggerHelper).logError(anyString());
        doNothing().when(loggerHelper).logDebug(anyString());
        doNothing().when(loggerHelper).logException(new Exception());
    }

    @Test
    public void testExecuteTestCases() throws Exception {
        // Arrange
        String projectName = "SampleProject";
        String testEventName = "SampleTestEvent";
        String executionId = "123";
        Map<String, String> gitParameters = new HashMap<>();
        gitParameters.put("gitParam1", "value1");
        gitParameters.put("gitParam2", "value2");
        Map<String, String> eventParameters = new HashMap<>();
        eventParameters.put("param1", "value1");
        eventParameters.put("param2", "value2");
        Map<String, String> characteristics = new HashMap<>();
        characteristics.put("key1", "value1");
        characteristics.put("key2", "value2");
        boolean releaseExecution = true;
        String testType = "all";
        String outputPath = "outputPath";

        JSONObject executionResponse = new JSONObject();
        executionResponse.put("ExecutionId", executionId);
        executionResponse.put("CreatedAt", "2023-10-01T10:00:00Z");

        when(executionManager.triggerExecution(eventParameters, characteristics)).thenReturn(executionResponse);
        when(resultsHandler.handleExecutionResults(executionId)).thenReturn(false);

        doNothing().when(freezeHandler).freezeTestEvent(releaseExecution);
        doNothing().when(executionManager).getExecutionStatus(executionId, 0);
        doNothing().when(resultsHandler).savePDFReport(executionId);

        // Act
        boolean failedTestCases = toscaService.executeTestCases(projectName, testEventName, gitParameters, eventParameters, characteristics, releaseExecution, testType, outputPath);

        // Assert
        verify(freezeHandler, times(1)).freezeTestEvent(releaseExecution);
        verify(executionManager, times(1)).triggerExecution(eventParameters, characteristics);
        verify(executionManager, times(1)).getExecutionStatus(executionId, 0);
        verify(resultsHandler, times(1)).handleExecutionResults(executionId);
        verify(resultsHandler, times(1)).savePDFReport(executionId);
        assertThat(failedTestCases).isFalse();
    }

    @Test
    public void testExecuteTestCasesWithFreezeTestEventException() throws Exception {
        // Arrange
        String projectName = "SampleProject";
        String testEventName = "SampleTestEvent";
        Map<String, String> gitParameters = new HashMap<>();
        gitParameters.put("gitParam1", "value1");
        gitParameters.put("gitParam2", "value2");
        Map<String, String> eventParameters = new HashMap<>();
        eventParameters.put("param1", "value1");
        eventParameters.put("param2", "value2");
        Map<String, String> characteristics = new HashMap<>();
        characteristics.put("key1", "value1");
        characteristics.put("key2", "value2");
        boolean releaseExecution = true;
        String testType = "all";
        String outputPath = "outputPath";

        doThrow(new Exception("An error occurred while freezing the test event"))
                .when(freezeHandler)
                .freezeTestEvent(releaseExecution);

        // Act & Assert
        assertThatThrownBy(() -> toscaService.executeTestCases(projectName, testEventName, gitParameters, eventParameters, characteristics, releaseExecution, testType, outputPath))
                .isInstanceOf(Exception.class)
                .hasMessage("An error occurred while freezing the test event");
    }

    @Test
    public void testExecuteTestCasesWithTriggerExecutionException() throws Exception {
        // Arrange
        String projectName = "SampleProject";
        String testEventName = "SampleTestEvent";
        Map<String, String> gitParameters = new HashMap<>();
        gitParameters.put("gitParam1", "value1");
        gitParameters.put("gitParam2", "value2");
        Map<String, String> eventParameters = new HashMap<>();
        eventParameters.put("param1", "value1");
        eventParameters.put("param2", "value2");
        Map<String, String> characteristics = new HashMap<>();
        characteristics.put("key1", "value1");
        characteristics.put("key2", "value2");
        boolean releaseExecution = true;
        String testType = "all";
        String outputPath = "outputPath";

        when(executionManager.triggerExecution(eventParameters, characteristics)).thenThrow(new Exception("Trigger execution failed"));

        doNothing().when(freezeHandler).freezeTestEvent(releaseExecution);

        // Act & Assert
        assertThatThrownBy(() -> toscaService.executeTestCases(projectName, testEventName, gitParameters, eventParameters, characteristics, releaseExecution, testType, outputPath))
                .isInstanceOf(Exception.class)
                .hasMessage("Trigger execution failed");
    }

    @Test
    public void testExecuteTestCasesWithGetExecutionStatusException() throws Exception {
        // Arrange
        String projectName = "SampleProject";
        String testEventName = "SampleTestEvent";
        String executionId = "123";
        Map<String, String> gitParameters = new HashMap<>();
        gitParameters.put("gitParam1", "value1");
        gitParameters.put("gitParam2", "value2");
        Map<String, String> eventParameters = new HashMap<>();
        eventParameters.put("param1", "value1");
        eventParameters.put("param2", "value2");
        Map<String, String> characteristics = new HashMap<>();
        characteristics.put("key1", "value1");
        characteristics.put("key2", "value2");
        boolean releaseExecution = true;
        String testType = "all";
        String outputPath = "outputPath";

        JSONObject executionResponse = new JSONObject();
        executionResponse.put("ExecutionId", executionId);
        executionResponse.put("CreatedAt", "2023-10-01T10:00:00Z");

        when(executionManager.triggerExecution(eventParameters, characteristics)).thenReturn(executionResponse);
        doThrow(new Exception("An error occurred while waiting for execution status to finish"))
                .when(executionManager)
                .getExecutionStatus(executionId, 0);

        doNothing().when(freezeHandler).freezeTestEvent(releaseExecution);

        // Act & Assert
        assertThatThrownBy(() -> toscaService.executeTestCases(projectName, testEventName, gitParameters, eventParameters, characteristics, releaseExecution, testType, outputPath))
                .isInstanceOf(Exception.class)
                .hasMessage("An error occurred while waiting for execution status to finish");
    }

    @Test
    public void testExecuteTestCasesWithHandleExecutionResultsException() throws Exception {
        // Arrange
        String projectName = "SampleProject";
        String testEventName = "SampleTestEvent";
        String executionId = "123";
        Map<String, String> gitParameters = new HashMap<>();
        gitParameters.put("gitParam1", "value1");
        gitParameters.put("gitParam2", "value2");
        Map<String, String> eventParameters = new HashMap<>();
        eventParameters.put("param1", "value1");
        eventParameters.put("param2", "value2");
        Map<String, String> characteristics = new HashMap<>();
        characteristics.put("key1", "value1");
        characteristics.put("key2", "value2");
        boolean releaseExecution = true;
        String testType = "all";
        String outputPath = "outputPath";

        JSONObject executionResponse = new JSONObject();
        executionResponse.put("ExecutionId", executionId);
        executionResponse.put("CreatedAt", "2023-10-01T10:00:00Z");

        when(executionManager.triggerExecution(eventParameters, characteristics)).thenReturn(executionResponse);
        doThrow(new Exception("Handling of execution results has failed"))
                .when(resultsHandler)
                .handleExecutionResults(executionId);

        doNothing().when(freezeHandler).freezeTestEvent(releaseExecution);
        doNothing().when(executionManager).getExecutionStatus(executionId, 0);

        // Act & Assert
        assertThatThrownBy(() -> toscaService.executeTestCases(projectName, testEventName, gitParameters, eventParameters, characteristics, releaseExecution, testType, outputPath))
                .isInstanceOf(Exception.class)
                .hasMessage("Handling of execution results has failed");
    }

}