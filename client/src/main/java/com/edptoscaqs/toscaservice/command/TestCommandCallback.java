package com.edptoscaqs.toscaservice.command;

public interface TestCommandCallback {
    void onTestCommandResult(boolean failedTestCases);
    void onTestCommandException();
}
