package com.edptoscaqs.toscaservice.command;

import com.edptoscaqs.toscaservice.ToscaService;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import picocli.CommandLine;

import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;
import java.io.ByteArrayOutputStream;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

public class TestCommandTest {
    @Mock
    private ToscaService toscaService;
    @InjectMocks
    private TestCommand testCommand;

    private ByteArrayOutputStream outContent;

    @Before
    public void init(){
        MockitoAnnotations.initMocks(this);
        outContent = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outContent));
    }

    @Test
    public void testWhenTestCommandIsCalledShouldRunTestCases() throws Exception {

        // Arrange
        testCommand.projectName = "project";
        testCommand.testEvent = "event";
        testCommand.gitParameters  = Map.of("param1", "value1", "param2", "value2");
        testCommand.suiteParameters  = Map.of("param1", "value1", "param2", "value2");
        testCommand.characteristics  = Map.of("param1", "value1", "param2", "value2");
        testCommand.releaseExecution = true;

        Map<String, String> expectedGitParameters = new HashMap<>(testCommand.gitParameters);
        Map<String, String> expectedSuiteParameters = new HashMap<>(testCommand.suiteParameters);
        Map<String, String> expectedCharacteristics = new HashMap<>(testCommand.characteristics);
        when(toscaService.executeTestCases(anyString(), anyString(), anyMap(), anyMap(), anyMap(), anyBoolean(), anyString(), anyString())).thenReturn(false);

        // Act
        testCommand.run();

        // Assert
        verify(toscaService, times(1)).executeTestCases(testCommand.projectName, testCommand.testEvent,expectedGitParameters, expectedSuiteParameters, expectedCharacteristics, testCommand.releaseExecution, testCommand.testType, testCommand.outputPath);
    }

    @Test
    public void testWhenTestCommandIsCalledWithFalseReleaseStatusShouldRunTestCases() throws Exception {

        // Arrange
        testCommand.projectName = "project";
        testCommand.testEvent = "event";
        testCommand.gitParameters  = Map.of("param1", "value1", "param2", "value2");
        testCommand.characteristics  = Map.of("param1", "value1", "param2", "value2");
        testCommand.suiteParameters  = Map.of("param1", "value1", "param2", "value2");
        testCommand.releaseExecution = false;

        Map<String, String> expectedGitParameters = new HashMap<>(testCommand.gitParameters);
        Map<String, String> expectedSuiteParameters = new HashMap<>(testCommand.suiteParameters);
        Map<String, String> expectedCharacteristics = new HashMap<>(testCommand.characteristics);
        when(toscaService.executeTestCases(anyString(), anyString(), anyMap(), anyMap(), anyMap(), anyBoolean(), anyString(), anyString())).thenReturn(false);

        // Act
        testCommand.run();

        // Assert
        verify(toscaService, times(1)).executeTestCases(testCommand.projectName, testCommand.testEvent, expectedGitParameters, expectedSuiteParameters, expectedCharacteristics, testCommand.releaseExecution, testCommand.testType, testCommand.outputPath);
    }

    @Test
    public void testWhenRunningWithNoGitParametersShouldWorkFine() throws Exception {
        // Arrange
        testCommand.projectName = "project";
        testCommand.testEvent = "event";
        testCommand.characteristics  = Map.of("param1", "value1", "param2", "value2");
        testCommand.suiteParameters  = Map.of("param1", "value1", "param2", "value2");;
        testCommand.releaseExecution = true;

        Map<String, String> expectedGitParameters = new HashMap<>();
        Map<String, String> expectedSuiteParameters = new HashMap<>(testCommand.suiteParameters);
        Map<String, String> expectedCharacteristics = new HashMap<>(testCommand.characteristics);
        when(toscaService.executeTestCases(anyString(), anyString(), anyMap(), anyMap(), anyMap(), anyBoolean(), anyString(), anyString())).thenReturn(false);

        // Act
        testCommand.run();

        // Assert
        verify(toscaService, times(1)).executeTestCases(testCommand.projectName, testCommand.testEvent, expectedGitParameters, expectedSuiteParameters, expectedCharacteristics, testCommand.releaseExecution, testCommand.testType, testCommand.outputPath);
    }

    @Test
    public void testWhenRunningWithNoSuiteParametersShouldWorkFine() throws Exception {
        // Arrange
        testCommand.projectName = "project";
        testCommand.testEvent = "event";
        testCommand.gitParameters  = Map.of("param1", "value1", "param2", "value2");
        testCommand.suiteParameters  = null;
        testCommand.characteristics  = Map.of("param1", "value1", "param2", "value2");
        testCommand.releaseExecution = true;

        Map<String, String> expectedGitParameters = new HashMap<>(testCommand.gitParameters);
        Map<String, String> expectedSuiteParameters = new HashMap<>();
        Map<String, String> expectedCharacteristics = new HashMap<>(testCommand.characteristics);
        when(toscaService.executeTestCases(anyString(), anyString(), anyMap(), anyMap(), anyMap(), anyBoolean(), anyString(), anyString())).thenReturn(false);

        // Act
        testCommand.run();

        // Assert
        verify(toscaService, times(1)).executeTestCases(testCommand.projectName, testCommand.testEvent, expectedGitParameters, expectedSuiteParameters, expectedCharacteristics, testCommand.releaseExecution, testCommand.testType, testCommand.outputPath);
    }

    @Test
    public void testWhenRunningWithNoCharacteristicsShouldWorkFine() throws Exception {
        // Arrange
        testCommand.projectName = "project";
        testCommand.testEvent = "event";
        testCommand.gitParameters  = Map.of("param1", "value1", "param2", "value2");
        testCommand.suiteParameters  = Map.of("param1", "value1", "param2", "value2");
        testCommand.characteristics  = null;
        testCommand.releaseExecution = true;

        Map<String, String> expectedGitParameters = new HashMap<>(testCommand.gitParameters);
        Map<String, String> expectedCharacteristics = new HashMap<>();
        Map<String, String> expectedSuiteParameters = new HashMap<>(testCommand.suiteParameters);
        when(toscaService.executeTestCases(anyString(), anyString(), anyMap(), anyMap(), anyMap(), anyBoolean(), anyString(), anyString())).thenReturn(false);

        // Act
        testCommand.run();

        // Assert
        verify(toscaService, times(1)).executeTestCases(testCommand.projectName, testCommand.testEvent, expectedGitParameters, expectedSuiteParameters, expectedCharacteristics, testCommand.releaseExecution, testCommand.testType, testCommand.outputPath);
    }

    @Test
    public void testWhenTestCommandIsCalledWithEventExecutionExceptionShouldReturnError() throws Exception {

        // Arrange
        testCommand.projectName = "project";
        testCommand.testEvent = "event";
        testCommand.gitParameters  = Map.of("param1", "value1", "param2", "value2");
        testCommand.suiteParameters  = Map.of("param1", "value1", "param2", "value2");
        testCommand.characteristics  = Map.of("param1", "value1", "param2", "value2");
        testCommand.releaseExecution = false;
        testCommand.testType = "all";
        testCommand.outputPath = "build/test-results";

        Map<String, String> expectedGitParameters = new HashMap<>(testCommand.gitParameters);
        Map<String, String> expectedSuiteParameters = new HashMap<>(testCommand.suiteParameters);
        Map<String, String> expectedCharacteristics = new HashMap<>(testCommand.characteristics);

        doThrow(new Exception()).when(toscaService).executeTestCases(testCommand.projectName, testCommand.testEvent, expectedGitParameters, expectedSuiteParameters, expectedCharacteristics, testCommand.releaseExecution, testCommand.testType, testCommand.outputPath);

        // Act & Assert
        assertThatThrownBy(() -> testCommand.run())
                .isInstanceOf(RuntimeException.class)
                .hasRootCauseInstanceOf(Exception.class);
    }

    @Test
    public void testWhenRunningWithHelpFlagShouldOutputUsage() {

        // Arrange
        testCommand.projectName = "project";
        testCommand.testEvent = "event";
        testCommand.helpRequested = true;

        // Act
        testCommand.run();

        // Assert
        String expectedOutput = new CommandLine(testCommand).getUsageMessage();
        assertThat(expectedOutput).isEqualTo(outContent.toString());
    }
}