package com.edptoscaqs.toscaservice.execution;

import com.edptoscaqs.toscaservice.ToscaExecutionAPIHandler;
import com.edptoscaqs.toscaservice.configuration.ToscaConfigParameters;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

public class ExecutionManagerTest {
    @Mock
    private ToscaExecutionAPIHandler toscaExecutionAPIHandler;
    @Mock
    private ToscaConfigParameters toscaConfiguration;
    @Mock
    private Utilities utilities;
    @InjectMocks
    private ExecutionManager executionManager;
    @Mock
    private LoggerHelper loggerHelper;

    @Before
    public void init() {
        MockitoAnnotations.initMocks(this);
        doNothing().when(loggerHelper).logInfo(anyString());
        doNothing().when(loggerHelper).logWarning(anyString());
        doNothing().when(loggerHelper).logError(anyString());
        doNothing().when(loggerHelper).logDebug(anyString());
        doNothing().when(loggerHelper).logException(new Exception());
        doNothing().when(utilities).sleepWithInterruptHandling(anyLong(), any(LoggerHelper.class));
        when(toscaConfiguration.getProjectName()).thenReturn("SampleProject");
        when(toscaConfiguration.getTestEventName()).thenReturn("SampleTestEvent");
        when(toscaConfiguration.getStatusSleepTime()).thenReturn(5000L);
        when(toscaConfiguration.getExecutionWaitTimeOut()).thenReturn(1L);
    }

    @Test
    public void testTriggerExecutionSuccess() throws Exception {
        // Arrange
        Map<String, String> eventParameters = new HashMap<>();
        eventParameters.put("key1", "value1");
        eventParameters.put("key2", "value2");
        Map<String, String> characteristics = new HashMap<>();
        characteristics.put("key1", "value1");
        characteristics.put("key2", "value2");
        JSONObject mockExecution = new JSONObject();
        mockExecution.put("ExecutionId", "12345");
        when(toscaExecutionAPIHandler.triggerEventExecution(anyString(), anyString(), anyString(), anyMap(), anyMap())).thenReturn(mockExecution);

        // Act
        JSONObject result = executionManager.triggerExecution(eventParameters, characteristics);

        // Assert
        assertThat(result.getAsString("ExecutionId")).isEqualTo("12345");
        verify(toscaExecutionAPIHandler).triggerEventExecution(toscaConfiguration.getProjectName(), "Dex", toscaConfiguration.getTestEventName(), eventParameters, characteristics);
    }

    @Test
    public void testTriggerExecutionFailure() throws Exception {
        // Arrange
        Map <String, String> eventParameters = new HashMap<>();
        eventParameters.put("key1", "value1");
        eventParameters.put("key2", "value2");
        Map<String, String> characteristics = new HashMap<>();
        characteristics.put("key1", "value1");
        characteristics.put("key2", "value2");
        when(toscaExecutionAPIHandler.triggerEventExecution(anyString(), anyString(), anyString(), anyMap(), anyMap())).thenThrow(new RuntimeException("API error"));

        // Act and Assert
        assertThatThrownBy(() -> executionManager.triggerExecution(eventParameters, characteristics))
                .isInstanceOf(Exception.class);
    }

    @Test
    public void testGetExecutionStatusCompleted() throws Exception {
        // Arrange
        when(toscaExecutionAPIHandler.getEventExecutionStatus(anyString())).thenReturn("Completed");

        // Act
        executionManager.getExecutionStatus("testExecutionId", 1);

        // Assert
        verify(loggerHelper).logDebug(String.format("[GetExecutionStatus] Process starts - Execution Id: %s - Timeout: %d", "testExecutionId", 1));
        verify(loggerHelper).logDebug("[HandleStatus] Process ends successfully - All test cases completed successfully.");
    }

    @Test
    public void testGetExecutionStatusError() throws Exception {
        // Arrange
        when(toscaExecutionAPIHandler.getEventExecutionStatus(anyString())).thenReturn("Error");

        // Act & Assert
        assertThatThrownBy(() -> executionManager.getExecutionStatus("testExecutionId", 1))
                .isInstanceOf(Exception.class);

    }

    @Test
    public void testGetExecutionStatusCancelled() throws Exception {
        // Arrange
        when(toscaExecutionAPIHandler.getEventExecutionStatus(anyString())).thenReturn("Cancelled");

        // Act & Assert
        assertThatThrownBy(() -> executionManager.getExecutionStatus("testExecutionId", 1))
                .isInstanceOf(Exception.class);
    }

    @Test
    public void testGetExecutionStatusInProgressTimeout() throws Exception {
        // Arrange
        when(toscaConfiguration.getExecutionWaitTimeOut()).thenReturn(0L);
        when(toscaExecutionAPIHandler.getEventExecutionStatus(anyString())).thenReturn("InProgress");

        // Act & Assert
        assertThatThrownBy(() -> executionManager.getExecutionStatus("testExecutionId", 1))
                .isInstanceOf(Exception.class);
    }

    @Test
    public void testGetExecutionStatusCompletedWithErrors() throws Exception {
        // Arrange
        when(toscaExecutionAPIHandler.getEventExecutionStatus(anyString())).thenReturn("CompletedWithErrors");

        // Act & Assert
        assertThatThrownBy(() -> executionManager.getExecutionStatus("testExecutionId", 1))
                .isInstanceOf(Exception.class);
    }

    @Test
    public void testGetExecutionStatusCompletedWithCancellations() throws Exception {
        // Arrange
        when(toscaExecutionAPIHandler.getEventExecutionStatus(anyString())).thenReturn("CompletedWithCancellations");

        // Act & Assert
        assertThatThrownBy(() -> executionManager.getExecutionStatus("testExecutionId", 1))
                .isInstanceOf(Exception.class);
    }

    @Test
    public void testGetExecutionStatusUnhandledStatus() throws Exception {
        // Arrange
        when(toscaExecutionAPIHandler.getEventExecutionStatus(anyString())).thenReturn("Unhandled");

        // Act & Assert
        assertThatThrownBy(() -> executionManager.getExecutionStatus("testExecutionId", 1))
                .isInstanceOf(Exception.class);
    }

    @Test
    public void testGetExecutionStatusErrorGettingStatus() throws Exception {
        // Arrange
        when(toscaExecutionAPIHandler.getEventExecutionStatus(anyString())).thenThrow(new Exception("Error getting the execution status value"));

        // Act & Assert
        assertThatThrownBy(() -> executionManager.getExecutionStatus("testExecutionId", 1))
                .isInstanceOf(Exception.class);
    }

    @Test
    public void testAppendPartialResultsWithValidData() throws Exception {
        // Arrange
        String executionId = "testExecutionId";
        int totalTestCases = 10;
        StringBuilder statusLog = new StringBuilder();
        String xmlString = "<results><TotalTestCases>5</TotalTestCases><Passed>3</Passed><Failed>2</Failed></results>";
        Map<String, Integer> partialResults = new HashMap<>();
        partialResults.put("Total Test Cases", 5);
        partialResults.put("Passed", 3);
        partialResults.put("Failed", 2);

        when(toscaExecutionAPIHandler.getPartialExecutionResults(executionId)).thenReturn(xmlString);
        when(utilities.extractPartialTestResults(xmlString)).thenReturn(partialResults);

        // Act
        executionManager.appendPartialResults(executionId, totalTestCases, statusLog);

        // Assert
        assertThat(statusLog.toString()).contains("Total Test Cases = 5/10");
        assertThat(statusLog.toString()).contains("   - Passed = 3");
        assertThat(statusLog.toString()).contains("   - Failed = 2");
    }

    @Test
    public void testAppendPartialResultsWithEmptyData() throws Exception {
        // Arrange
        String executionId = "testExecutionId";
        int totalTestCases = 10;
        StringBuilder statusLog = new StringBuilder();
        String xmlString = "";

        when(toscaExecutionAPIHandler.getPartialExecutionResults(executionId)).thenReturn(xmlString);

        // Act
        executionManager.appendPartialResults(executionId, totalTestCases, statusLog);

        // Assert
        assertThat(statusLog.toString()).contains("Progress: No data");
    }

    @Test
    public void testAppendPartialResultsWithNullData() throws Exception {
        // Arrange
        String executionId = "testExecutionId";
        int totalTestCases = 10;
        StringBuilder statusLog = new StringBuilder();
        String xmlString = null;

        when(toscaExecutionAPIHandler.getPartialExecutionResults(executionId)).thenReturn(xmlString);

        // Act
        executionManager.appendPartialResults(executionId, totalTestCases, statusLog);

        // Assert
        assertThat(statusLog.toString()).contains("Progress: No data");
    }
}
