package com.edptoscaqs.toscaservice.configuration;

import com.edptoscaqs.toscaservice.utilities.Utilities;

public class ConfigFile {

    private long executionWaitTimeOut;
    private long statusSleepTime;
    private long reportCreationTimeOut;
    private String pdfReportName;

    public long getExecutionWaitTimeOut() {
        return executionWaitTimeOut;
    }
    public long getStatusSleepTime() {
        return statusSleepTime;
    }
    public long getReportCreationTimeOut() {
        return reportCreationTimeOut;
    }
    public String getPdfReportName() {
        return pdfReportName;
    }

}
