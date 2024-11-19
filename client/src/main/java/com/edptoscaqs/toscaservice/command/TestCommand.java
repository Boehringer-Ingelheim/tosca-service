package com.edptoscaqs.toscaservice.command;

import com.edptoscaqs.toscaservice.ToscaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import picocli.CommandLine;

import java.util.HashMap;
import java.util.Map;

@Component
@CommandLine.Command(
        name = "test",
        description = "Runs a test suite with the specified project name and test event.")
public class TestCommand implements Runnable {
    private final ToscaService toscaService;

    @Autowired
    public TestCommand(ToscaService toscaService) {
        this.toscaService = toscaService;
    }

    @CommandLine.Parameters(description = "Root name of the Tosca project containing the test event.")
    protected String projectName;
    @CommandLine.Parameters(description = "Name of the test event to execute.")
    protected String testEvent;
    @CommandLine.Option(names = {"-h", "--help"}, usageHelp = true, description = "[Optional] Show this help message and exit.")
    protected boolean helpRequested;
    @CommandLine.Option(names = {"-o", "--output-path"}, description = "[Optional] Path to save the test results. Default is build/test-results/test.")
    protected String outputPath;
    @CommandLine.Option(names = {"-t", "--test-type"}, description = "[Optional] Type of test to run (acceptance, integration, installation, or all). Refer to the tosca-configuration.json file for configuration details. Default is all.")
    protected String testType;
    @CommandLine.Option(names = {"-r", "--release"}, description = "[Optional] Specify if the execution is part of a release. Default is false.")
    protected boolean releaseExecution;
    @CommandLine.Option(names = {"-g", "--git-parameter"}, description = "[Optional] Git parameters to include in the PDF report.")
    protected Map<String, String> gitParameters;
    @CommandLine.Option(names = {"-s", "--suite-parameter"}, description = "[Optional] Test configuration parameters for the test suite to define environmental settings.")
    protected Map<String, String> suiteParameters;
    @CommandLine.Option(names = {"-c", "--characteristics"}, description = "[Optional] Characteristics to define which Agents should execute the tests.")
    protected Map<String, String> characteristics;

    private TestCommandCallback callback;

    public void setCallback(TestCommandCallback callback) {
        this.callback = callback;
    }

    @Override
    public void run() {
        if (helpRequested) {
            CommandLine.usage(this, System.out);
            return;
        }

        Map<String, String> gitReportParameters = new HashMap<>();
        if (gitParameters != null) {
            gitReportParameters.putAll(gitParameters);
        }
        Map<String, String> eventParameters = new HashMap<>();
        if (suiteParameters != null) {
            eventParameters.putAll(suiteParameters);
        }
        Map<String, String> executionCharacteristics = new HashMap<>();
        if (characteristics != null) {
            executionCharacteristics.putAll(characteristics);
        }
        if (testType == null) {
            testType = "all";
        }
        if (outputPath == null) {
            outputPath = "build/test-results/test";
        }
        try {
            boolean allTestsPassed = toscaService.executeTestCases(projectName, testEvent, gitReportParameters, eventParameters, executionCharacteristics, releaseExecution, testType, outputPath);
            if (callback != null) {
                callback.onTestCommandResult(allTestsPassed);
            }
        } catch (Exception e) {
            if (callback != null) {
                callback.onTestCommandException();
            }
            throw new RuntimeException(e);
        }
    }
}
